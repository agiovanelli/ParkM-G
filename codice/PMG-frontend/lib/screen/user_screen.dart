import 'dart:async';
import 'dart:convert';
import 'dart:ui' as ui;
import 'dart:html' as html;

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:http/http.dart' as http;
import 'package:park_mg/models/prenotazione.dart';
import 'package:park_mg/screen/home_page.dart';
import 'package:park_mg/utils/ui_feedback.dart';
import 'package:park_mg/widgets/preferenze_dialog.dart';

import 'package:park_mg/utils/theme.dart';
import 'package:url_launcher/url_launcher.dart';

import '../api/api_client.dart';
import '../models/utente.dart';
import '../widgets/prenotazione_dialog.dart';
import 'history_screen.dart';

import '../widgets/map/gmaps_control_button.dart';
import '../widgets/map/parking_popup.dart';

class UserScreen extends StatefulWidget {
  final Utente utente;
  final ApiClient apiClient;

  const UserScreen({super.key, required this.utente, required this.apiClient});

  @override
  State<UserScreen> createState() => _UserScreenState();
}

class _UserScreenState extends State<UserScreen>
    with TickerProviderStateMixin, WidgetsBindingObserver {
  final _searchController = TextEditingController();
  final GlobalKey _gearKey = GlobalKey();
  GoogleMapController? _mapController;
  final Set<Marker> _markers = {};
  final Set<Circle> _circles = {};
  bool _locationGranted = false;
  late AnimationController _pulseController;
  late Animation<double> _pulseAnimation;
  LatLng _cameraTarget = _initialCamera.target;
  bool _showParkings = false;
  bool _isLoadingParkings = false;
  Map<String, dynamic>? _selectedParkingData;
  String? _selectedParkingMarkerId;
  bool _isLocating = false;
  LatLng? _pendingCenter;
  bool _lockMapGestures = false;
  String? _bookedParkingMarkerId;
  bool _bookedMarkerLocked = false;
  BitmapDescriptor? _parkingIconFull;
  bool _blockMapInteractions = false;
  bool _bookingCancelledInDialog = false;
  LatLng? _lastMe;
  StreamSubscription<Position>? _trackSub;
  DateTime _lastTrackUiUpdate = DateTime.fromMillisecondsSinceEpoch(0);
  LatLng? _lastTrackUiPos;
  static const Duration _trackUiMinInterval = Duration(milliseconds: 250);
  static const double _trackUiMinMoveMeters = 1.5;
  List<dynamic>? _lastParkingsJson;
  bool _isBooking = false;
  bool _externalNavOpened = false;
  static const double _arriveParkingThresholdM = 150.0;
  PrenotazioneResponse? _activeBooking;
  LatLng? _activeParkingLatLng;
  bool _arrivalHandled = false;
  StreamSubscription<html.Event>? _focusSub;
  static const double _blueDotRadiusM = 5.0;
  bool _returnOverlay = false;
  double? _distanceToParkingM;
  Timer? _returnOverlayTimer;
  bool _arrivalUiDone = false;
  bool _openingQrDialog = false;
  bool _autoNavAfterQrClose = false;
  bool _qrShownOnLogin = false;
  static const Color _baseBlue = Color(0xFF4285F4);
  BitmapDescriptor? _parkingIcon;
  BitmapDescriptor? _parkingIconSelected;
  int _sessionToken = 0;

  static const String _baseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://localhost:8080/api',
  );

  static const CameraPosition _initialCamera = CameraPosition(
    target: LatLng(41.9028, 12.4964),
    zoom: 6,
  );

  late final Color _selectedGreen = () {
    final hsl = HSLColor.fromColor(_baseBlue);
    return hsl.withHue(120).toColor();
  }();

  void _showOnlyBookedParking(Map<String, dynamic> p) {
    final markerId = 'p_${p['id']}';
    final lat = (p['latitudine'] as num).toDouble();
    final lng = (p['longitudine'] as num).toDouble();
    final bookedMarker = Marker(
      markerId: MarkerId(markerId),
      position: LatLng(lat, lng),
      icon:
          _parkingIconSelected ??
          _parkingIcon ??
          BitmapDescriptor.defaultMarker,
      infoWindow: const InfoWindow(title: ''),
      onTap: _bookedMarkerLocked
          ? null
          : () {
              setState(() {
                _selectedParkingMarkerId = markerId;
                _selectedParkingData = p;
              });
            },
      consumeTapEvents: true,
    );
    setState(() {
      _bookedParkingMarkerId = markerId;
      _showParkings = false;
      _selectedParkingData = null;
      _selectedParkingMarkerId = markerId;
      _markers.removeWhere((m) => m.markerId.value.startsWith('p_'));
      _markers.add(bookedMarker);
    });
  }

  // -------------------- utils --------------------

  Future<void> _showQrOnLoginIfNeeded() async {
    if (!mounted) return;
    if (_qrShownOnLogin) return;
    if (_activeBooking == null) return;
    if (_activeParkingLatLng == null) return;

    _qrShownOnLogin = true;
    _openingQrDialog = true;

    try {
      final arrived = await _isArrivedToActiveParking();
      _autoNavAfterQrClose = !arrived;

      await _showMapLockedDialog<void>(
        show: () async {
          // ✅ refresh stato dal backend prima di mostrare
          final updated = await widget.apiClient.getPrenotazioneByIdFromStorico(
            widget.utente.id,
            _activeBooking!.id,
          );

          if (!mounted) return null;

          if (updated != null) {
            setState(() => _activeBooking = updated);
          }

          return PrenotazioneDialog.mostra(
            context,
            prenotazione: updated ?? _activeBooking!,
            apiClient: widget.apiClient,
            utenteId: widget.utente.id,
            lockActions: arrived,
            onCancelled: arrived
                ? null
                : () async {
                    _autoNavAfterQrClose = false;
                    await _handleBookingCancelledAndReload();
                  },
            onClosed: arrived
                ? null
                : () async {
                    if (!_autoNavAfterQrClose) return;
                    _autoNavAfterQrClose = false;
                    final dest = _activeParkingLatLng;
                    if (dest == null) return;
                    if (_externalNavOpened) return;
                    _externalNavOpened = true;

                    LatLng? origin = _lastMe;
                    if (origin == null) {
                      try {
                        final pos = await Geolocator.getCurrentPosition(
                          desiredAccuracy: LocationAccuracy.high,
                        ).timeout(const Duration(seconds: 5));
                        origin = LatLng(pos.latitude, pos.longitude);
                      } catch (_) {}
                    }

                    await _openExternalNavWeb(
                      destLat: dest.latitude,
                      destLng: dest.longitude,
                      origin: origin,
                    );
                  },
          );
        },
      );
    } finally {
      _openingQrDialog = false;
    }
  }

  Future<bool> _isArrivedToActiveParking() async {
    if (_activeBooking == null || _activeParkingLatLng == null) return false;

    LatLng? me = _lastMe;

    if (me == null) {
      try {
        final pos = await Geolocator.getCurrentPosition(
          desiredAccuracy: LocationAccuracy.high,
        ).timeout(const Duration(seconds: 5));
        me = LatLng(pos.latitude, pos.longitude);
        _lastMe = me;
      } catch (_) {
        return false;
      }
    }

    final dest = _activeParkingLatLng!;

    try {
      final roadMeters = await widget.apiClient
          .getRoadDistanceMeters(
            oLat: me.latitude,
            oLng: me.longitude,
            dLat: dest.latitude,
            dLng: dest.longitude,
          )
          .timeout(const Duration(seconds: 6));

      final d = (roadMeters != null)
          ? roadMeters.toDouble()
          : Geolocator.distanceBetween(
              me.latitude,
              me.longitude,
              dest.latitude,
              dest.longitude,
            );

      _distanceToParkingM = d;
      return d <= _arriveParkingThresholdM;
    } catch (_) {
      final d = Geolocator.distanceBetween(
        me.latitude,
        me.longitude,
        dest.latitude,
        dest.longitude,
      );
      _distanceToParkingM = d;
      return d <= _arriveParkingThresholdM;
    }
  }

  bool _isActiveState(StatoPrenotazione s) {
    return s == StatoPrenotazione.ATTIVA ||
        s == StatoPrenotazione.IN_CORSO ||
        s == StatoPrenotazione.PAGATO;
  }

  Future<void> _restoreActiveBookingFromBackend() async {
    try {
      final storico = await widget.apiClient.getStoricoPrenotazioni(
        widget.utente.id,
      );

      final attive = storico.where((p) => _isActiveState(p.stato)).toList();

      if (attive.isEmpty) {
        if (!mounted) return;
        setState(() {
          _activeBooking = null;
          _activeParkingLatLng = null;
          _arrivalHandled = false;
          _bookedParkingMarkerId = null;
          _bookedMarkerLocked = false;
          _externalNavOpened = false;
        });
        return;
      }

      attive.sort((a, b) {
        final da = a.dataCreazione ?? DateTime.fromMillisecondsSinceEpoch(0);
        final db = b.dataCreazione ?? DateTime.fromMillisecondsSinceEpoch(0);
        return db.compareTo(da); // desc
      });

      final booking = attive.first;

      // recupera parcheggio per lat/lng
      final park = await widget.apiClient.getParcheggioById(
        booking.parcheggioId,
      );

      final lat = (park['latitudine'] as num).toDouble();
      final lng = (park['longitudine'] as num).toDouble();

      if (!mounted) return;

      setState(() {
        _activeBooking = booking;
        _activeParkingLatLng = LatLng(lat, lng);
        _arrivalHandled = false;

        _externalNavOpened = false;
        _distanceToParkingM = null;
        _returnOverlay = false;
        _bookedMarkerLocked = true;
      });

      _showOnlyBookedParking(park);
    } catch (_) {}
  }

  void _startReturnOverlay() {
    if (!mounted) return;
    if (_openingQrDialog) return;
    if (_activeBooking == null || _activeParkingLatLng == null) return;
    if (_arrivalHandled) return;

    setState(() {
      _returnOverlay = true;
      _arrivalUiDone = false;
    });
    _returnOverlayTimer?.cancel();
    _updateDistanceAndMaybeArrive();
    _returnOverlayTimer = Timer.periodic(const Duration(milliseconds: 1500), (
      _,
    ) {
      _updateDistanceAndMaybeArrive();
    });
  }

  void _stopReturnOverlay() {
    _returnOverlayTimer?.cancel();
    _returnOverlayTimer = null;
    if (mounted) {
      setState(() {
        _returnOverlay = false;
        _arrivalUiDone = false;
      });
    }
  }

  Future<void> _updateDistanceAndMaybeArrive() async {
    if (!mounted) return;
    if (_activeBooking == null || _activeParkingLatLng == null) return;
    if (_arrivalHandled) {
      _stopReturnOverlay();
      return;
    }

    if (_arrivalUiDone) return;

    try {
      final pos = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
      ).timeout(const Duration(seconds: 6));
      final me = LatLng(pos.latitude, pos.longitude);
      _lastMe = me;
      final dest = _activeParkingLatLng!;

      // distanza su strada (metri) via backend
      final roadMeters = await widget.apiClient.getRoadDistanceMeters(
        oLat: me.latitude,
        oLng: me.longitude,
        dLat: dest.latitude,
        dLng: dest.longitude,
      );

      final d = (roadMeters != null)
          ? roadMeters.toDouble()
          : Geolocator.distanceBetween(
              me.latitude,
              me.longitude,
              dest.latitude,
              dest.longitude,
            );

      if (!mounted) return;
      setState(() => _distanceToParkingM = d);

      if (d <= _arriveParkingThresholdM) {
        if (_openingQrDialog) return;
        _openingQrDialog = true;
        if (mounted) {
          setState(() {
            _arrivalUiDone = true;
          });
        }
        await Future.delayed(const Duration(milliseconds: 800));
        if (!mounted) return;

        _arrivalHandled = true;
        _stopReturnOverlay();

        await _showMapLockedDialog<void>(
          show: () async {
            final updated = await widget.apiClient
                .getPrenotazioneByIdFromStorico(
                  widget.utente.id,
                  _activeBooking!.id,
                );

            if (!mounted) return null;

            if (updated != null) {
              setState(() => _activeBooking = updated);
            }

            return PrenotazioneDialog.mostra(
              context,
              prenotazione: updated ?? _activeBooking!,
              apiClient: widget.apiClient,
              utenteId: widget.utente.id,
              lockActions: true,
            );
          },
        );
        _openingQrDialog = false;
      }
    } catch (_) {}
  }

  Future<void> _openExternalNavWeb({
    required double destLat,
    required double destLng,
    LatLng? origin,
  }) async {
    final url = (origin != null)
        ? 'https://www.google.com/maps/dir/?api=1'
              '&origin=${origin.latitude},${origin.longitude}'
              '&destination=$destLat,$destLng'
              '&travelmode=driving'
        : 'https://www.google.com/maps/dir/?api=1'
              '&destination=$destLat,$destLng'
              '&travelmode=driving';

    final ok = await launchUrl(Uri.parse(url), webOnlyWindowName: '_blank');

    if (!ok && mounted) {
      UiFeedback.showError(context, 'Impossibile aprire Google Maps.');
    }
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _startReturnOverlay();
    }
  }

  void _updatePulseCenter(LatLng me) {
    _lastMe = me;

    setState(() {
      // aggiorna PULSE con il nuovo centro (subito)
      _circles.removeWhere((c) => c.circleId.value == 'pulse');
      _circles.add(
        Circle(
          circleId: const CircleId('pulse'),
          center: me,
          radius: _pulseAnimation.value, // raggio attuale dell'animazione
          fillColor: Colors.blue.withOpacity(0.25),
          strokeColor: Colors.blue.withOpacity(0.1),
          strokeWidth: 1,
          zIndex: 900,
        ),
      );

      // aggiorna PALLINO BLU con lo stesso centro (subito)
      _circles.removeWhere((c) => c.circleId.value == 'blue_dot');
      _circles.add(
        Circle(
          circleId: const CircleId('blue_dot'),
          center: me,
          radius: _blueDotRadiusM,
          fillColor: const Color(0xFF1A73E8),
          strokeWidth: 0,
          strokeColor: Colors.transparent,
          zIndex: 1000,
        ),
      );
    });

    // opzionale: se non ti serve più il marker "me", puoi anche rimuoverlo del tutto
    // _updateMeMarkerThrottled(me);
  }

  static const String _mapStyleNoPoi = '''
    [
      { "featureType": "poi", "stylers": [ { "visibility": "off" } ] },
      { "featureType": "transit", "stylers": [ { "visibility": "off" } ] }
    ]
  ''';

  Future<void> _handleBookingCancelledAndReload([
    Map<String, dynamic>? _,
  ]) async {
    _stopReturnOverlay();
    setState(() {
      _activeBooking = null;
      _activeParkingLatLng = null;
      _arrivalHandled = false;

      _bookedParkingMarkerId = null;
      _bookedMarkerLocked = false;

      _selectedParkingData = null;
      _selectedParkingMarkerId = null;

      _externalNavOpened = false;

      _showParkings = true;
      _lockMapGestures = false;

      _markers.removeWhere((m) => m.markerId.value.startsWith('p_'));
    });

    await _loadParkingsNearby(_cameraTarget, radiusMeters: 2500);
  }

  // -------------------- lifecycle --------------------

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addObserver(this);
    if (kIsWeb) {
      _focusSub = html.window.onFocus.listen((_) {
        if (!mounted) return;
        if (_openingQrDialog) return;
        if (!_qrShownOnLogin && _activeBooking != null) {}
        _startReturnOverlay();
      });
    }

    _bitmapDescriptorFromIcon(
      Icons.local_parking,
      size: 64,
      iconSize: 34,
      backgroundColor: _baseBlue,
    ).then((v) {
      if (mounted) setState(() => _parkingIcon = v);
    });

    _bitmapDescriptorFromIcon(
      Icons.local_parking,
      size: 64,
      iconSize: 34,
      backgroundColor: _selectedGreen,
    ).then((v) {
      if (mounted) setState(() => _parkingIconSelected = v);
    });

    _bitmapDescriptorFromIcon(
      Icons.local_parking,
      size: 64,
      iconSize: 34,
      backgroundColor: Colors.redAccent,
    ).then((v) {
      if (mounted) setState(() => _parkingIconFull = v);
    });

    WidgetsBinding.instance.addPostFrameCallback((_) async {
      await _bootstrapMyLocation();
      await _restoreActiveBookingFromBackend();
      await _showQrOnLoginIfNeeded();
    });

    if (widget.utente.preferenze == null || widget.utente.preferenze!.isEmpty) {
      WidgetsBinding.instance.addPostFrameCallback(
        (_) => _showPreferenzeDialog(),
      );
    }

    _pulseController = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 2),
    )..repeat(reverse: true);

    _pulseAnimation =
        Tween<double>(begin: 15, end: 35).animate(
          CurvedAnimation(parent: _pulseController, curve: Curves.easeInOut),
        )..addListener(() {
          if (!mounted) return;
          final me = _lastMe;
          if (me == null) return;

          setState(() {
            _circles.removeWhere((c) => c.circleId.value == 'pulse');
            _circles.add(
              Circle(
                circleId: const CircleId('pulse'),
                center: me,
                radius: _pulseAnimation.value,
                fillColor: Colors.blue.withOpacity(0.25),
                strokeColor: Colors.blue.withOpacity(0.1),
                strokeWidth: 1,
              ),
            );
          });
        });
  }

  @override
  void dispose() {
    WidgetsBinding.instance.removeObserver(this);
    _searchController.dispose();
    _pulseController.dispose();
    _trackSub?.cancel();
    _focusSub?.cancel();
    _returnOverlayTimer?.cancel();
    super.dispose();
  }

  // -------------------- location / icons --------------------

  Future<void> _bootstrapMyLocation() async {
    if (_isLocating) return;
    final int token = _sessionToken; // ✅ snapshot sessione

    if (mounted) setState(() => _isLocating = true);

    try {
      // (su web questo può essere poco affidabile, ma ok)
      final serviceEnabled = await Geolocator.isLocationServiceEnabled()
          .timeout(const Duration(seconds: 3));

      if (!serviceEnabled && !kIsWeb) {
        UiFeedback.showError(context, 'Servizi di localizzazione disattivati.');
        return;
      }

      LocationPermission perm = await Geolocator.checkPermission().timeout(
        const Duration(seconds: 3),
      );

      if (perm == LocationPermission.denied) {
        perm = await Geolocator.requestPermission().timeout(
          const Duration(seconds: 8),
        );
      }

      if (perm == LocationPermission.denied ||
          perm == LocationPermission.deniedForever) {
        if (token != _sessionToken) return; // ✅ sessione cambiata
        if (mounted) setState(() => _locationGranted = false);
        UiFeedback.showError(context, 'Permesso posizione negato.');
        return;
      }

      // ✅ IL PUNTO CRITICO: su web può pendere -> timeout duro
      final pos = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
      ).timeout(const Duration(seconds: 8));

      if (token != _sessionToken) return; // ✅ logout nel frattempo
      if (!mounted) return;

      final me = LatLng(pos.latitude, pos.longitude);

      setState(() {
        _locationGranted = true;
        _lastMe = me;

        _circles
          ..removeWhere((c) => c.circleId.value == 'pulse')
          ..removeWhere((c) => c.circleId.value == 'blue_dot')
          ..add(
            Circle(
              circleId: const CircleId('pulse'),
              center: me,
              radius: _pulseAnimation.value,
              fillColor: Colors.blue.withOpacity(0.25),
              strokeColor: Colors.blue.withOpacity(0.1),
              strokeWidth: 1,
            ),
          )
          ..add(
            Circle(
              circleId: const CircleId('blue_dot'),
              center: me,
              radius: _blueDotRadiusM,
              fillColor: const Color(0xFF1A73E8),
              strokeColor: Colors.transparent,
              strokeWidth: 0,
              zIndex: 1000,
            ),
          );
      });

      _startTrackingPosition();

      if (_mapController != null) {
        await _mapController!.animateCamera(
          CameraUpdate.newCameraPosition(CameraPosition(target: me, zoom: 16)),
        );
      } else {
        _pendingCenter = me;
      }
    } on TimeoutException {
      if (token != _sessionToken) return;
      if (!mounted) return;
      UiFeedback.showError(context, 'Timeout posizione, riprova.');
    } catch (_) {
      if (token != _sessionToken) return;
      if (!mounted) return;
      UiFeedback.showError(context, 'Impossibile ottenere la posizione.');
    } finally {
      if (token != _sessionToken) return;
      if (mounted) setState(() => _isLocating = false);
    }
  }

  void _startTrackingPosition() {
    if (_trackSub != null) return;

    final LocationSettings settings = kIsWeb
        ? const LocationSettings(
            accuracy: LocationAccuracy.high,
            distanceFilter: 1,
          )
        : (defaultTargetPlatform == TargetPlatform.android)
        ? AndroidSettings(
            accuracy: LocationAccuracy.high,
            distanceFilter: 1,
            intervalDuration: const Duration(milliseconds: 700),
          )
        : AppleSettings(
            accuracy: LocationAccuracy.best,
            distanceFilter: 1,
            activityType: ActivityType.otherNavigation,
            pauseLocationUpdatesAutomatically: false,
          );

    _trackSub = Geolocator.getPositionStream(locationSettings: settings).listen(
      (pos) {
        if (!mounted) return;

        final me = LatLng(pos.latitude, pos.longitude);
        final now = DateTime.now();

        // throttle temporale
        if (now.difference(_lastTrackUiUpdate) < _trackUiMinInterval) return;

        // throttle per spostamento
        if (_lastTrackUiPos != null) {
          final moved = Geolocator.distanceBetween(
            _lastTrackUiPos!.latitude,
            _lastTrackUiPos!.longitude,
            me.latitude,
            me.longitude,
          );
          if (moved < _trackUiMinMoveMeters) return;
        }

        _lastTrackUiUpdate = now;
        _lastTrackUiPos = me;

        // 1 sola chiamata
        _updatePulseCenter(me);
      },
      onError: (e) {
        if (!mounted) return;
        UiFeedback.showError(context, 'Errore tracking posizione: $e');
      },
    );
  }

  Future<BitmapDescriptor> _bitmapDescriptorFromIcon(
    IconData icon, {
    double size = 96,
    double iconSize = 56,
    Color backgroundColor = const Color(0xFF4285F4),
    Color iconColor = Colors.white,
  }) async {
    final recorder = ui.PictureRecorder();
    final canvas = Canvas(recorder);

    final paint = Paint()..color = backgroundColor;
    canvas.drawCircle(Offset(size / 2, size / 2), size / 2, paint);

    final textPainter = TextPainter(
      textDirection: TextDirection.ltr,
      text: TextSpan(
        text: String.fromCharCode(icon.codePoint),
        style: TextStyle(
          fontSize: iconSize,
          fontFamily: icon.fontFamily,
          package: icon.fontPackage,
          color: iconColor,
        ),
      ),
    );

    textPainter.layout();
    final offset = Offset(
      (size - textPainter.width) / 2,
      (size - textPainter.height) / 2,
    );
    textPainter.paint(canvas, offset);

    final picture = recorder.endRecording();
    final img = await picture.toImage(size.toInt(), size.toInt());
    final bytes = await img.toByteData(format: ui.ImageByteFormat.png);
    return BitmapDescriptor.bytes(bytes!.buffer.asUint8List());
  }

  // -------------------- routes --------------------

  // -------------------- parkings --------------------

  Future<void> _toggleParkings() async {
    if (_bookedParkingMarkerId != null) {
      UiFeedback.showError(
        context,
        'Hai già una prenotazione attiva: mostro solo il parcheggio prenotato.',
      );
      return;
    }

    final bool turningOff = _showParkings;

    setState(() {
      _showParkings = !_showParkings;

      if (turningOff) {
        _selectedParkingData = null;
        _selectedParkingMarkerId = null;
      }
    });

    if (turningOff) {
      setState(() {
        _markers.removeWhere((m) => m.markerId.value.startsWith('p_'));
      });
      return;
    }

    await _loadParkingsNearby(_cameraTarget, radiusMeters: 2500);
  }

  Future<void> _loadParkingsNearby(
    LatLng center, {
    double radiusMeters = 2500,
  }) async {
    setState(() => _isLoadingParkings = true);

    try {
      final uri = Uri.parse(
        '$_baseUrl/parcheggi/nearby?lat=${center.latitude}&lng=${center.longitude}&radius=$radiusMeters',
      );

      final res = await http.get(uri).timeout(const Duration(seconds: 8));

      if (!mounted) return;

      if (res.statusCode != 200) {
        UiFeedback.showError(
          context,
          'Errore caricamento parcheggi (${res.statusCode})',
        );
        return;
      }

      final data = jsonDecode(res.body) as List<dynamic>;

      _lastParkingsJson = data;

      _rebuildParkingMarkersFromLastData();
    } on TimeoutException {
      if (!mounted) return;
      UiFeedback.showError(context, 'Timeout caricamento parcheggi.');
    } catch (_) {
      if (!mounted) return;
      UiFeedback.showError(
        context,
        'Errore durante il caricamento dei parcheggi.',
      );
    } finally {
      if (mounted) setState(() => _isLoadingParkings = false);
    }
  }

  void _rebuildParkingMarkersFromLastData() {
    final data = _lastParkingsJson;
    if (data == null) return;

    final newMarkers = <Marker>{};

    for (final raw in data) {
      final p = raw as Map<String, dynamic>;

      final markerId = 'p_${p['id']}';
      final isSelected = _selectedParkingMarkerId == markerId;

      final bool inEmergenza = p['inEmergenza'] as bool? ?? false;
      final int postiDisp = (p['postiDisponibili'] as num?)?.toInt() ?? 0;
      final bool isFull = postiDisp <= 0;

      final icon = (inEmergenza || isFull)
          ? (_parkingIconFull ?? BitmapDescriptor.defaultMarker)
          : (isSelected
                ? (_parkingIconSelected ??
                      _parkingIcon ??
                      BitmapDescriptor.defaultMarker)
                : (_parkingIcon ?? BitmapDescriptor.defaultMarker));

      newMarkers.add(
        Marker(
          markerId: MarkerId(markerId),
          position: LatLng(
            (p['latitudine'] as num).toDouble(),
            (p['longitudine'] as num).toDouble(),
          ),
          icon: icon,
          infoWindow: const InfoWindow(title: ''),
          consumeTapEvents: true,
          onTap: () {
            // NIENTE rete qui
            setState(() {
              _selectedParkingMarkerId = markerId;
              _selectedParkingData = p;
            });

            // aggiorna solo le icone localmente
            _rebuildParkingMarkersFromLastData();
          },
        ),
      );
    }

    setState(() {
      _markers.removeWhere((m) => m.markerId.value.startsWith('p_'));
      _markers.addAll(newMarkers);
    });
  }

  // -------------------- booking --------------------

  Future<void> _effettuaPrenotazione(
    String parcheggioId, {
    required double destLat,
    required double destLng,
    required Map<String, dynamic> parkingData,
  }) async {
    if (_isBooking) return;

    setState(() => _isBooking = true);
    _bookingCancelledInDialog = false;

    LatLng? origin = _lastMe;
    if (origin == null) {
      try {
        final pos = await Geolocator.getCurrentPosition(
          desiredAccuracy: LocationAccuracy.high,
        ).timeout(const Duration(seconds: 5));
        origin = LatLng(pos.latitude, pos.longitude);
      } catch (_) {}
    }

    try {
      final risposta = await widget.apiClient
          .prenotaParcheggio(
            widget.utente.id,
            parcheggioId,
            DateTime.now().toIso8601String(),
          )
          .timeout(const Duration(seconds: 12));

      if (!mounted) return;

      await _showMapLockedDialog<void>(
        show: () => PrenotazioneDialog.mostra(
          context,
          prenotazione: risposta,
          apiClient: widget.apiClient,
          utenteId: widget.utente.id,
          onCancelled: () {
            _bookingCancelledInDialog = true;
            Future.microtask(() async {
              if (!mounted) return;
              await _handleBookingCancelledAndReload(parkingData);
            });
          },
          onClosed: () {
            // Se l'ha annullata nel dialog, non attivare nulla
            if (_bookingCancelledInDialog) return;

            // ✅ ATTIVA la prenotazione "globale" SOLO ORA (dopo Chiudi del QR)
            setState(() {
              _activeBooking = risposta;
              _activeParkingLatLng = LatLng(destLat, destLng);
              _arrivalHandled = false;
            });

            setState(() {
              _distanceToParkingM = null; // ✅ pulisci distanza
              _returnOverlay =
                  false; // ✅ overlay deve comparire solo al ritorno
            });

            // evita doppio open
            if (_externalNavOpened) return;
            _externalNavOpened = true;

            setState(() => _bookedMarkerLocked = true);
            _showOnlyBookedParking(parkingData);

            Future.microtask(() async {
              await _openExternalNavWeb(
                destLat: destLat,
                destLng: destLng,
                origin: origin ?? _lastMe,
              );
            });
          },
        ),
      );

      if (!mounted) return;
      if (_bookingCancelledInDialog) return;
    } on TimeoutException {
      if (!mounted) return;
      UiFeedback.showError(context, 'Timeout prenotazione: riprova.');
    } on ApiException catch (e) {
      if (!mounted) return;
      UiFeedback.showError(context, e.message);
    } catch (e) {
      if (!mounted) return;
      UiFeedback.showError(context, 'Errore di connessione o del server');
    } finally {
      if (mounted) setState(() => _isBooking = false);
    }
  }

  // -------------------- menu / navigation --------------------

  Future<void> _showPreferenzeDialog() async {
    final updatedPrefs = await _showMapLockedDialog<Map<String, String>>(
      show: () => showDialog<Map<String, String>>(
        context: context,
        barrierDismissible: false,
        builder: (_) => PreferenzeDialog(
          utente: widget.utente,
          apiClient: widget.apiClient,
        ),
      ),
    );

    if (!mounted) return;
    if (updatedPrefs != null) {
      setState(() => widget.utente.preferenze = updatedPrefs);
    }
  }

  Future<T?> _showMapLockedDialog<T>({
    required Future<T?> Function() show,
  }) async {
    final prevLock = _lockMapGestures;

    setState(() {
      _blockMapInteractions = true;
      _lockMapGestures = true;
    });

    T? result;
    try {
      result = await show();
    } finally {
      if (mounted) {
        setState(() {
          _blockMapInteractions = false;
          _lockMapGestures = prevLock;
        });
      } else {
        _blockMapInteractions = false;
        _lockMapGestures = prevLock;
      }
    }

    return result;
  }

  Future<void> _logout() async {
    _sessionToken++;
    _stopReturnOverlay();
    await _trackSub?.cancel();
    _trackSub = null;
    await _focusSub?.cancel();
    _focusSub = null;
    _openingQrDialog = false;
    _arrivalHandled = false;
    _autoNavAfterQrClose = false;
    _qrShownOnLogin = false;
    _externalNavOpened = false;

    if (!mounted) return;

    setState(() {
      _blockMapInteractions = false;
      _lockMapGestures = false;
      _isLocating = false;
      _isLoadingParkings = false;
      _returnOverlay = false;
      _arrivalUiDone = false;
    });

    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      Navigator.of(context).pushAndRemoveUntil(
        MaterialPageRoute(
          builder: (_) => HomePage(apiClient: widget.apiClient),
        ),
        (route) => false,
      );
    });
  }

  void _vaiAlloStorico() {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) => HistoryScreen(
          utente: widget.utente,
          apiClient: widget.apiClient,
          onBookingCancelled: () {
            Future.microtask(() async {
              if (!mounted) return;
              await _handleBookingCancelledAndReload();
            });
          },
        ),
      ),
    );
  }

  Future<void> _openUserMenu() async {
    final overlay = Overlay.of(context).context.findRenderObject() as RenderBox;
    final box = _gearKey.currentContext!.findRenderObject() as RenderBox;
    final pos = box.localToGlobal(Offset.zero, ancestor: overlay);

    final selected = await showMenu<String>(
      context: context,
      color: AppColors.bgDark,
      elevation: 16,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      position: RelativeRect.fromRect(
        Rect.fromLTWH(pos.dx, pos.dy + box.size.height, box.size.width, 1),
        Offset.zero & overlay.size,
      ),
      items: [
        PopupMenuItem<String>(
          value: 'prefs',
          child: Row(
            children: const [
              Icon(Icons.tune, size: 18, color: AppColors.textPrimary),
              SizedBox(width: 10),
              Text(
                'Preferences',
                style: TextStyle(
                  color: AppColors.textPrimary,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
        ),
        PopupMenuItem<String>(
          value: 'history',
          child: Row(
            children: const [
              Icon(Icons.history, size: 18, color: AppColors.textPrimary),
              SizedBox(width: 10),
              Text(
                'Le mie prenotazioni',
                style: TextStyle(color: AppColors.textPrimary),
              ),
            ],
          ),
        ),
        const PopupMenuDivider(height: 10),
        PopupMenuItem<String>(
          value: 'logout',
          child: Row(
            children: const [
              Icon(Icons.logout, size: 18, color: AppColors.textPrimary),
              SizedBox(width: 10),
              Text(
                'Logout',
                style: TextStyle(
                  color: AppColors.textPrimary,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
        ),
      ],
    );

    if (!mounted) return;

    if (selected == 'prefs') {
      _showPreferenzeDialog();
    } else if (selected == 'history') {
      _vaiAlloStorico();
    } else if (selected == 'logout') {
      await _logout();
    }
  }

  Future<void> _searchAndGo(String query) async {
    final q = query.trim();
    if (q.isEmpty) return;

    try {
      final data = await widget.apiClient.geocode(address: q);

      final results = (data['results'] as List).cast<Map<String, dynamic>>();
      if (results.isEmpty) {
        UiFeedback.showError(context, 'Nessun risultato trovato.');
        return;
      }

      final first = results.first;
      final lat = (first['lat'] as num).toDouble();
      final lng = (first['lng'] as num).toDouble();

      final target = LatLng(lat, lng);
      _mapController?.animateCamera(
        CameraUpdate.newCameraPosition(
          CameraPosition(target: target, zoom: 15),
        ),
      );
    } catch (_) {
      UiFeedback.showError(context, 'Errore durante la ricerca.');
    }
  }

  // -------------------- UI --------------------

  @override
  Widget build(BuildContext context) {
    final fullName = '${widget.utente.nome} ${widget.utente.cognome}'.trim();

    final selected = _selectedParkingData;
    final int postiDispSelected =
        (selected?['postiDisponibili'] as num?)?.toInt() ??
        (selected?['posti_disponibili'] as num?)?.toInt() ??
        0;

    final bool isFullSelected = postiDispSelected <= 0;
    final bool hasActiveBooking = _bookedParkingMarkerId != null;

    return Scaffold(
      backgroundColor: AppColors.bgDark2,
      body: Container(
        decoration: const BoxDecoration(
          gradient: LinearGradient(
            begin: Alignment.topLeft,
            end: Alignment.bottomRight,
            colors: [AppColors.bgDark2, AppColors.bgDark, AppColors.bgDark2],
          ),
        ),
        child: SafeArea(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              children: [
                Container(
                  height: 64,
                  padding: const EdgeInsets.symmetric(horizontal: 16),
                  decoration: BoxDecoration(
                    color: AppColors.bgDark,
                    borderRadius: BorderRadius.circular(18),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withOpacity(0.35),
                        blurRadius: 18,
                        offset: const Offset(0, 6),
                      ),
                    ],
                  ),
                  child: Row(
                    children: [
                      const Text(
                        'Park M&G',
                        style: TextStyle(
                          color: AppColors.textPrimary,
                          fontSize: 22,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                      const Spacer(),
                      Text(
                        fullName.isEmpty ? 'Utente' : fullName,
                        style: const TextStyle(
                          color: AppColors.textPrimary,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      const SizedBox(width: 10),
                      InkWell(
                        key: _gearKey,
                        borderRadius: BorderRadius.circular(999),
                        onTap: _openUserMenu,
                        child: Container(
                          padding: const EdgeInsets.all(10),
                          decoration: BoxDecoration(
                            color: AppColors.bgDark,
                            borderRadius: BorderRadius.circular(999),
                            border: Border.all(
                              color: AppColors.borderField,
                              width: 1,
                            ),
                          ),
                          child: const Icon(
                            Icons.settings,
                            color: AppColors.textPrimary,
                            size: 18,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 14),

                TextField(
                  controller: _searchController,
                  style: const TextStyle(color: AppColors.textPrimary),
                  cursorColor: AppColors.accentCyan,
                  decoration: InputDecoration(
                    hintText: 'Search your Park',
                    hintStyle: TextStyle(
                      color: AppColors.textMuted.withOpacity(0.9),
                    ),
                    prefixIcon: const Icon(
                      Icons.search,
                      color: AppColors.textMuted,
                    ),
                    suffixIcon: IconButton(
                      icon: const Icon(
                        Icons.arrow_forward,
                        color: AppColors.accentCyan,
                      ),
                      onPressed: () => _searchAndGo(_searchController.text),
                    ),
                    filled: true,
                    fillColor: AppColors.bgDark2.withOpacity(0.35),
                    contentPadding: const EdgeInsets.symmetric(
                      horizontal: 14,
                      vertical: 14,
                    ),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(999),
                      borderSide: const BorderSide(
                        color: AppColors.borderField,
                        width: 1,
                      ),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(999),
                      borderSide: const BorderSide(
                        color: AppColors.accentCyan,
                        width: 1.2,
                      ),
                    ),
                  ),
                  onSubmitted: _searchAndGo,
                ),
                const SizedBox(height: 14),

                Expanded(
                  child: Container(
                    width: double.infinity,
                    decoration: BoxDecoration(
                      borderRadius: BorderRadius.circular(18),
                      border: Border.all(
                        color: AppColors.borderField,
                        width: 1,
                      ),
                      boxShadow: [
                        BoxShadow(
                          color: Colors.black.withOpacity(0.35),
                          blurRadius: 22,
                          offset: const Offset(0, 10),
                        ),
                      ],
                    ),
                    child: ClipRRect(
                      borderRadius: BorderRadius.circular(18),
                      child: AbsorbPointer(
                        absorbing: _blockMapInteractions || _returnOverlay,
                        child: Stack(
                          children: [
                            GoogleMap(
                              onCameraMove: (pos) => _cameraTarget = pos.target,
                              initialCameraPosition: _initialCamera,
                              onMapCreated: (c) async {
                                _mapController = c;
                                await _mapController!.setMapStyle(
                                  _mapStyleNoPoi,
                                );

                                if (_pendingCenter != null) {
                                  final me = _pendingCenter!;
                                  _pendingCenter = null;
                                  await _mapController!.animateCamera(
                                    CameraUpdate.newCameraPosition(
                                      CameraPosition(target: me, zoom: 16),
                                    ),
                                  );
                                }
                              },
                              markers: _markers,
                              circles: _circles,
                              myLocationEnabled: !kIsWeb && _locationGranted,
                              myLocationButtonEnabled:
                                  !kIsWeb && _locationGranted,
                              zoomControlsEnabled: false,
                              mapToolbarEnabled: false,
                              scrollGesturesEnabled:
                                  !_lockMapGestures && !_returnOverlay,
                              zoomGesturesEnabled:
                                  !_lockMapGestures && !_returnOverlay,
                              rotateGesturesEnabled:
                                  !_lockMapGestures && !_returnOverlay,
                              tiltGesturesEnabled:
                                  !_lockMapGestures && !_returnOverlay,
                              onTap: (_) async {
                                setState(() {
                                  _selectedParkingData = null;
                                  _selectedParkingMarkerId = null;
                                });
                                if (_showParkings &&
                                    _bookedParkingMarkerId == null) {
                                  await _loadParkingsNearby(
                                    _cameraTarget,
                                    radiusMeters: 2500,
                                  );
                                }
                              },
                            ),

                            if (_isLoadingParkings || _isLocating)
                              Container(
                                color: Colors.black54,
                                child: const Center(
                                  child: CircularProgressIndicator(
                                    color: AppColors.accentCyan,
                                  ),
                                ),
                              ),

                            Align(
                              alignment: Alignment.topRight,
                              child: Padding(
                                padding: const EdgeInsets.only(
                                  top: 14,
                                  right: 14,
                                ),
                                child: Column(
                                  mainAxisSize: MainAxisSize.min,
                                  children: [
                                    const SizedBox(height: 10),
                                    GMapsControlButton(
                                      icon: Icons.local_parking,
                                      onPressed: hasActiveBooking
                                          ? null
                                          : _toggleParkings,
                                      tooltip: hasActiveBooking
                                          ? 'Hai già una prenotazione attiva'
                                          : 'Parcheggi vicino',
                                      selected: hasActiveBooking
                                          ? false
                                          : _showParkings,
                                    ),
                                  ],
                                ),
                              ),
                            ),

                            if (_selectedParkingData != null)
                              Positioned(
                                bottom: 40,
                                left: 20,
                                right: 20,
                                child: ParkingPopup(
                                  parking: _selectedParkingData!,
                                  canBook: !isFullSelected,
                                  isLoading: _isBooking,
                                  onClose: () async {
                                    setState(() {
                                      _selectedParkingData = null;
                                      _selectedParkingMarkerId = null;
                                    });

                                    if (_showParkings &&
                                        _bookedParkingMarkerId == null) {
                                      await _loadParkingsNearby(
                                        _cameraTarget,
                                        radiusMeters: 2500,
                                      );
                                    }
                                  },
                                  onBook: () {
                                    final p = _selectedParkingData!;
                                    final pId = p['id'].toString();
                                    final destLat = (p['latitudine'] as num)
                                        .toDouble();
                                    final destLng = (p['longitudine'] as num)
                                        .toDouble();

                                    _effettuaPrenotazione(
                                      pId,
                                      destLat: destLat,
                                      destLng: destLng,
                                      parkingData: p,
                                    );
                                  },
                                ),
                              ),

                            if (_returnOverlay)
                              Positioned.fill(
                                child: Container(
                                  color: Colors.black54,
                                  child: Center(
                                    child: Padding(
                                      padding: const EdgeInsets.symmetric(
                                        horizontal: 24,
                                      ),
                                      child: Column(
                                        mainAxisSize: MainAxisSize.min,
                                        children: [
                                          Text(
                                            _arrivalUiDone
                                                ? 'Sei arrivato al parcheggio'
                                                : 'Torna quando sei arrivato al parcheggio',
                                            textAlign: TextAlign.center,
                                            style: TextStyle(
                                              color: Colors.white,
                                              fontSize: 26,
                                              fontWeight: FontWeight.w800,
                                            ),
                                          ),
                                          const SizedBox(height: 14),
                                          Text(
                                            _arrivalUiDone
                                                ? 'Apro il QR…'
                                                : (_distanceToParkingM == null
                                                      ? 'Calcolo distanza…'
                                                      : 'Distanza: ${_distanceToParkingM!.toStringAsFixed(0)} m'),
                                            textAlign: TextAlign.center,
                                            style: const TextStyle(
                                              color: Colors.white70,
                                              fontSize: 18,
                                              fontWeight: FontWeight.w600,
                                            ),
                                          ),

                                          const SizedBox(height: 18),

                                          if (_arrivalUiDone)
                                            Container(
                                              width: 44,
                                              height: 44,
                                              decoration: BoxDecoration(
                                                color: Colors.green.withOpacity(
                                                  0.18,
                                                ),
                                                shape: BoxShape.circle,
                                                border: Border.all(
                                                  color: Colors.greenAccent
                                                      .withOpacity(0.6),
                                                ),
                                              ),
                                              child: const Icon(
                                                Icons.check,
                                                color: Colors.greenAccent,
                                                size: 28,
                                              ),
                                            )
                                          else
                                            const SizedBox(
                                              width: 28,
                                              height: 28,
                                              child: CircularProgressIndicator(
                                                strokeWidth: 3,
                                                color: AppColors.accentCyan,
                                              ),
                                            ),
                                        ],
                                      ),
                                    ),
                                  ),
                                ),
                              ),
                          ],
                        ),
                      ),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
