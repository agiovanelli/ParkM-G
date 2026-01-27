import 'dart:async';
import 'dart:convert';
import 'dart:ui' as ui;

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:http/http.dart' as http;
import 'package:park_mg/models/directions.dart';
import 'package:park_mg/widgets/map/nav_math.dart';
import 'package:park_mg/widgets/preferenze_dialog.dart';

import 'package:park_mg/utils/theme.dart';

import '../api/api_client.dart';
import '../models/utente.dart';
import '../widgets/prenotazione_dialog.dart';
import 'history_screen.dart';

import '../widgets/map/gmaps_control_button.dart';
import '../widgets/map/parking_popup.dart';
import '../widgets/map/route_panel.dart';

class UserScreen extends StatefulWidget {
  final Utente utente;
  final ApiClient apiClient;

  const UserScreen({super.key, required this.utente, required this.apiClient});

  @override
  State<UserScreen> createState() => _UserScreenState();
}

class _UserScreenState extends State<UserScreen> with TickerProviderStateMixin {
  final _searchController = TextEditingController();
  final GlobalKey _gearKey = GlobalKey();
  GoogleMapController? _mapController;
  final Set<Marker> _markers = {};
  final Set<Circle> _circles = {};
  final Set<Polyline> _polylines = {};
  bool _locationGranted = false;
  late AnimationController _pulseController;
  late Animation<double> _pulseAnimation;
  LatLng _cameraTarget = _initialCamera.target;
  bool _showParkings = false;
  bool _isLoadingParkings = false;
  Map<String, dynamic>? _selectedParkingData;
  String? _selectedParkingMarkerId;
  List<DirectionsRoute> _routes = [];
  int _selectedRouteIndex = 0;
  bool _isLoadingRoute = false;
  bool _isLocating = false;
  LatLng? _pendingCenter;
  bool _showRoutePanel = false;
  bool _lockMapGestures = false;
  StreamSubscription<Position>? _posSub;
  bool _navActive = false;
  int _navStepIndex = 0;
  List<LatLng>? _navRoutePts;
  int? _navRouteIndexCached;
  String? _bookedParkingMarkerId;
  static const double _stepArriveThresholdM = 25;
  bool _isStartingNav = false;
  bool _gotFirstNavFix = false;
  bool _navPending = false;
  bool _bookedMarkerLocked = false;
  BitmapDescriptor? _parkingIconFull;

  void _setMapGesturesLocked(bool v) {
    if (_lockMapGestures == v) return;
    setState(() => _lockMapGestures = v);
  }

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

  static const Color _baseBlue = Color(0xFF4285F4);

  late final Color _selectedGreen = () {
    final hsl = HSLColor.fromColor(_baseBlue);
    return hsl.withHue(120).toColor();
  }();

  BitmapDescriptor? _parkingIcon;
  BitmapDescriptor? _parkingIconSelected;

  static const String _baseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://localhost:8080/api',
  );

  static const CameraPosition _initialCamera = CameraPosition(
    target: LatLng(41.9028, 12.4964),
    zoom: 6,
  );

  // -------------------- utils --------------------

  void _checkPolylineChars(String poly) {
    for (int i = 0; i < poly.length; i++) {
      final c = poly.codeUnitAt(i);
      if (c < 63 || c > 126) {
        debugPrint('BAD CHAR in polyline at i=$i code=$c char="${poly[i]}"');
        final start = (i - 10).clamp(0, poly.length);
        final end = (i + 10).clamp(0, poly.length);
        debugPrint('CONTEXT: "${poly.substring(start, end)}"');
        return;
      }
    }
    debugPrint('Polyline chars OK (all in 63..126)');
  }

  static const String _mapStyleNoPoi = '''
    [
      { "featureType": "poi", "stylers": [ { "visibility": "off" } ] },
      { "featureType": "transit", "stylers": [ { "visibility": "off" } ] }
    ]
  ''';

  String _formatDistanceKm(String distanceText) {
    final t = distanceText.trim().toLowerCase().replaceAll(',', '.');

    final m = RegExp(r'([\d.]+)\s*(km|m)\b').firstMatch(t);
    if (m == null) return distanceText;

    final value = double.tryParse(m.group(1)!) ?? 0.0;
    final unit = m.group(2)!;

    final km = (unit == 'm') ? (value / 1000.0) : value;

    if (km >= 10) return '${km.toStringAsFixed(0)} km';
    return '${km.toStringAsFixed(1)} km';
  }

  String _formatDurationHm(String durationText) {
    final t = durationText.trim().toLowerCase();

    int hours = 0;
    int mins = 0;

    final hMatch = RegExp(r'(\d+)\s*(h|hour|hours|ora|ore)\b').firstMatch(t);
    if (hMatch != null) hours = int.tryParse(hMatch.group(1)!) ?? 0;

    final mMatch = RegExp(
      r'(\d+)\s*(m|min|mins|minute|minutes|minuto|minuti)\b',
    ).firstMatch(t);
    if (mMatch != null) mins = int.tryParse(mMatch.group(1)!) ?? 0;

    if (hours == 0 && mins == 0) return durationText;

    if (hours == 0) return '$mins min';
    if (mins == 0) return '${hours} h';
    return '${hours} h ${mins} min';
  }

  String _routeMeta(DirectionsRoute r) {
    final dist = _formatDistanceKm(r.distanceText);
    final dur = _formatDurationHm(r.durationText);
    return '$dist • $dur';
  }

  String _stripHtml(String input) {
    final noTags = input.replaceAll(RegExp(r'<[^>]*>'), ' ');
    return noTags
        .replaceAll('&nbsp;', ' ')
        .replaceAll('&amp;', '&')
        .replaceAll('&quot;', '"')
        .replaceAll('&#39;', "'")
        .replaceAll('&lt;', '<')
        .replaceAll('&gt;', '>')
        .replaceAll(RegExp(r'\s+'), ' ')
        .trim();
  }

  void _showToast(String msg) {
    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(
        SnackBar(
          behavior: SnackBarBehavior.floating,
          backgroundColor: AppColors.bgDark,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(14),
          ),
          content: Text(
            msg,
            style: const TextStyle(color: AppColors.textPrimary),
          ),
        ),
      );
  }

  // -------------------- lifecycle --------------------

  @override
  void initState() {
    super.initState();

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

    WidgetsBinding.instance.addPostFrameCallback((_) => _bootstrapMyLocation());

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
          if (_circles.isNotEmpty && mounted) {
            final old = _circles.last.center;
            setState(() {
              _circles.removeWhere((c) => c.circleId.value == 'pulse');
              _circles.add(
                Circle(
                  circleId: const CircleId('pulse'),
                  center: old,
                  radius: _pulseAnimation.value,
                  fillColor: Colors.blue.withOpacity(0.25),
                  strokeColor: Colors.blue.withOpacity(0.1),
                  strokeWidth: 1,
                ),
              );
            });
          }
        });
  }

  @override
  void dispose() {
    _posSub?.cancel();
    _searchController.dispose();
    _pulseController.dispose();
    super.dispose();
  }

  // -------------------- location / icons --------------------

  Future<void> _bootstrapMyLocation() async {
    if (_isLocating) return;
    setState(() => _isLocating = true);

    try {
      final serviceEnabled = await Geolocator.isLocationServiceEnabled();
      if (!serviceEnabled) {
        _showToast('Servizi di localizzazione disattivati.');
        return;
      }

      LocationPermission perm = await Geolocator.checkPermission();
      if (perm == LocationPermission.denied)
        perm = await Geolocator.requestPermission();

      if (perm == LocationPermission.denied ||
          perm == LocationPermission.deniedForever) {
        setState(() => _locationGranted = false);
        _showToast('Permesso posizione negato.');
        return;
      }

      final pos = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
      );
      final me = LatLng(pos.latitude, pos.longitude);

      setState(() {
        _locationGranted = true;
        _circles
          ..clear()
          ..addAll([
            Circle(
              circleId: const CircleId('pulse'),
              center: me,
              radius: 25,
              fillColor: Colors.blue.withOpacity(0.25),
              strokeColor: Colors.blue.withOpacity(0.1),
              strokeWidth: 1,
            ),
            Circle(
              circleId: const CircleId('dot'),
              center: me,
              radius: 6,
              fillColor: const Color(0xFF4285F4),
              strokeColor: Colors.white,
              strokeWidth: 2,
            ),
          ]);
      });

      if (_mapController != null) {
        await _mapController!.animateCamera(
          CameraUpdate.newCameraPosition(CameraPosition(target: me, zoom: 16)),
        );
      } else {
        _pendingCenter = me;
      }
    } catch (_) {
      _showToast('Impossibile ottenere la posizione.');
    } finally {
      if (mounted) setState(() => _isLocating = false);
    }
  }

  Future<void> _startInAppNavigation() async {
    if (_routes.isEmpty) return;
    if (_isStartingNav || _navPending) return; // evita doppi tap

    setState(() {
      _navPending = true;
      _isStartingNav = true;
      _gotFirstNavFix = false;

      _navActive = false; // ✅ IMPORTANTISSIMO: non attivare ancora
      _navStepIndex = 0;
      _lockMapGestures = true;
    });

    _posSub?.cancel();

    const settings = LocationSettings(
      accuracy: LocationAccuracy.bestForNavigation,
      distanceFilter: 3,
    );

    _posSub = Geolocator.getPositionStream(locationSettings: settings).listen(
      (pos) {
        if (!mounted) return;

        if (_navPending && !_gotFirstNavFix) {
          _gotFirstNavFix = true;
          setState(() {
            _navPending = false;
            _isStartingNav = false;
            _navActive = true;
          });
        }

        if (!_navActive) return;

        final me = LatLng(pos.latitude, pos.longitude);

        final routePts = _getNavRoutePts();
        nearestPointOnPolyline(routePts, me);

        final route = _routes[_selectedRouteIndex];
        final step =
            (route.steps.isNotEmpty && _navStepIndex < route.steps.length)
            ? route.steps[_navStepIndex]
            : null;

        final stepDone =
            (step != null) &&
            (distanceMeters(me, step.end) <= _stepArriveThresholdM);

        _followCamera(me, bearing: pos.heading.isFinite ? pos.heading : null);

        if (stepDone) _advanceStep();
      },
      onError: (e) {
        if (!mounted) return;
        setState(() {
          _navPending = false;
          _isStartingNav = false;
          _navActive = false;
        });
        _showToast('Errore avvio navigazione: $e');
      },
    );
  }

  void _followCamera(LatLng me, {double? bearing}) {
    final c = _mapController;
    if (c == null) return;

    c.animateCamera(
      CameraUpdate.newCameraPosition(
        CameraPosition(
          target: me,
          zoom: 18,
          bearing: (bearing ?? 0).clamp(0, 360),
          tilt: 55,
        ),
      ),
    );
  }

  List<LatLng> _getNavRoutePts() {
    final idx = _selectedRouteIndex;
    if (_navRoutePts == null || _navRouteIndexCached != idx) {
      _navRoutePts = decodePolylineToLatLngs(_routes[idx].polyline);
      _navRouteIndexCached = idx;
    }
    return _navRoutePts!;
  }

  String _currentInstruction() {
    if (_routes.isEmpty) return '';
    final steps = _routes[_selectedRouteIndex].steps;
    if (steps.isEmpty) return '';
    if (_navStepIndex < 0 || _navStepIndex >= steps.length) return '';
    return _stripHtml(steps[_navStepIndex].htmlInstructions);
  }

  void _advanceStep() {
    if (_routes.isEmpty) return;
    final route = _routes[_selectedRouteIndex];

    setState(() {
      _navStepIndex++;
      if (_navStepIndex >= route.steps.length) {
        _navStepIndex = route.steps.length - 1;
        _showToast('Sei arrivato a destinazione.');
        _stopInAppNavigation();
      }
    });
  }

  void _stopInAppNavigation() {
    _posSub?.cancel();
    _posSub = null;

    setState(() {
      _navActive = false;
      _navPending = false;
      _isStartingNav = false;
      _gotFirstNavFix = false;

      _lockMapGestures = false;
    });
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

  void _applySelectedRoute() {
    _navRoutePts = null;
    _navRouteIndexCached = null;
    if (_routes.isEmpty) return;
    final route = _routes[_selectedRouteIndex];

    final pts = decodePolylineToLatLngs(route.polyline);

    setState(() {
      _polylines
        ..removeWhere((p) => p.polylineId.value == 'route')
        ..add(
          Polyline(
            polylineId: const PolylineId('route'),
            points: pts,
            width: 6,
            color: AppColors.accentCyan,
            geodesic: true,
          ),
        );
    });

    if (pts.isNotEmpty) _fitCameraToPoints(pts);
  }

  void _showRouteChoiceSheet() {
    if (_routes.isEmpty) return;
    setState(() {
      _showRoutePanel = true;
    });
  }

  void _closeRoutePanel() {
    if (_navActive) _stopInAppNavigation();
    setState(() {
      _showRoutePanel = false;
      _lockMapGestures = false;
    });
  }

  Future<void> _showRouteToParking({
    required LatLng origin,
    required double destLat,
    required double destLng,
  }) async {
    setState(() {
      _isLoadingRoute = true;
    });

    try {
      final destination = LatLng(destLat, destLng);

      final routes = await _fetchDirectionsRoutes(
        origin: origin,
        destination: destination,
      );

      setState(() {
        _routes = routes;
        _selectedRouteIndex = 0;
      });

      _navRoutePts = null;
      _navRouteIndexCached = null;
      _navStepIndex = 0;

      _applySelectedRoute();

      setState(() {
        _showRoutePanel = true;
      });
    } catch (e, st) {
      debugPrint('Errore calcolo percorso: $e');
      debugPrintStack(stackTrace: st);
      _showToast('Errore calcolo percorso.');
    } finally {
      if (mounted) setState(() => _isLoadingRoute = false);
    }
  }

  Future<List<DirectionsRoute>> _fetchDirectionsRoutes({
    required LatLng origin,
    required LatLng destination,
  }) async {
    final data = await widget.apiClient.getDirections(
      oLat: origin.latitude,
      oLng: origin.longitude,
      dLat: destination.latitude,
      dLng: destination.longitude,
    );

    final routesJson = (data['routes'] as List).cast<Map<String, dynamic>>();
    if (routesJson.isEmpty)
      throw Exception('Nessun percorso dalla Directions API');

    return routesJson.map((r) {
      final route = DirectionsRoute.fromJson(r);

      if (route.polyline.isEmpty) {
        throw Exception('Polyline overview vuota dal backend');
      }
      _checkPolylineChars(route.polyline);

      return route;
    }).toList();
  }

  void _fitCameraToPoints(List<LatLng> pts) {
    if (_mapController == null || pts.isEmpty) return;

    double minLat = pts.first.latitude, maxLat = pts.first.latitude;
    double minLng = pts.first.longitude, maxLng = pts.first.longitude;

    for (final p in pts) {
      if (p.latitude < minLat) minLat = p.latitude;
      if (p.latitude > maxLat) maxLat = p.latitude;
      if (p.longitude < minLng) minLng = p.longitude;
      if (p.longitude > maxLng) maxLng = p.longitude;
    }

    _mapController!.animateCamera(
      CameraUpdate.newLatLngBounds(
        LatLngBounds(
          southwest: LatLng(minLat, minLng),
          northeast: LatLng(maxLat, maxLng),
        ),
        70,
      ),
    );
  }

  // -------------------- parkings --------------------

  Future<void> _toggleParkings() async {
    if (_bookedParkingMarkerId != null) {
      _showToast(
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

    await _loadParkingsNearby(_cameraTarget, radiusMeters: 1200);
  }

  Future<void> _loadParkingsNearby(
    LatLng center, {
    double radiusMeters = 1200,
  }) async {
    setState(() => _isLoadingParkings = true);

    try {
      final uri = Uri.parse(
        '$_baseUrl/parcheggi/nearby?lat=${center.latitude}&lng=${center.longitude}&radius=$radiusMeters',
      );

      final res = await http.get(uri);
      if (res.statusCode != 200) {
        _showToast('Errore caricamento parcheggi (${res.statusCode})');
        return;
      }

      final data = jsonDecode(res.body) as List<dynamic>;
      final newMarkers = <Marker>{};

      for (final p in data) {
        final markerId = 'p_${p['id']}';
        final isSelected = _selectedParkingMarkerId == markerId;

        final int postiDisp =
            (p['postiDisponibili'] as num?)?.toInt() ??
            (p['posti_disponibili'] as num?)?.toInt() ??
            0;

        final bool isFull = postiDisp <= 0;

        newMarkers.add(
          Marker(
            markerId: MarkerId(markerId),
            position: LatLng(
              (p['latitudine'] as num).toDouble(),
              (p['longitudine'] as num).toDouble(),
            ),
            icon: isFull
                ? (_parkingIconFull ?? BitmapDescriptor.defaultMarker)
                : (isSelected
                      ? (_parkingIconSelected ??
                            _parkingIcon ??
                            BitmapDescriptor.defaultMarker)
                      : (_parkingIcon ?? BitmapDescriptor.defaultMarker)),

            infoWindow: const InfoWindow(title: ''),
            onTap: () async {
              setState(() {
                _selectedParkingMarkerId = markerId;
                _selectedParkingData = p;
              });
              await _loadParkingsNearby(_cameraTarget, radiusMeters: 1200);
            },
          ),
        );
      }

      setState(() {
        _markers.removeWhere((m) => m.markerId.value.startsWith('p_'));
        _markers.addAll(newMarkers);
      });
    } catch (_) {
      _showToast('Errore durante il caricamento dei parcheggi.');
    } finally {
      if (mounted) setState(() => _isLoadingParkings = false);
    }
  }

  // -------------------- booking --------------------

  Future<void> _effettuaPrenotazione(
    String parcheggioId, {
    required double destLat,
    required double destLng,
    required Map<String, dynamic> parkingData,
  }) async {
    LatLng? origin;
    try {
      LocationPermission perm = await Geolocator.checkPermission();
      if (perm == LocationPermission.denied)
        perm = await Geolocator.requestPermission();

      if (perm != LocationPermission.denied &&
          perm != LocationPermission.deniedForever) {
        final pos = await Geolocator.getCurrentPosition(
          desiredAccuracy: LocationAccuracy.high,
        );
        origin = LatLng(pos.latitude, pos.longitude);
      }
    } catch (_) {}

    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (_) => const Center(
        child: CircularProgressIndicator(color: AppColors.accentCyan),
      ),
    );

    try {
      final risposta = await widget.apiClient.prenotaParcheggio(
        widget.utente.id,
        parcheggioId,
        DateTime.now().toIso8601String(),
      );

      setState(() {});

      if (!mounted) return;

      Navigator.of(context, rootNavigator: true).pop();

      await PrenotazioneDialog.mostra(context, risposta);

      if (origin != null) {
        await _showRouteToParking(
          origin: origin,
          destLat: destLat,
          destLng: destLng,
        );

        if (!mounted) return;
        setState(() => _bookedMarkerLocked = true);
        _showOnlyBookedParking(parkingData);
      } else {
        _showToast(
          'Posizione non disponibile: impossibile calcolare il percorso.',
        );
      }
    } on ApiException catch (e) {
      if (!mounted) return;
      Navigator.of(context, rootNavigator: true).pop();
      _showToast(e.message);
    } catch (_) {
      if (!mounted) return;
      Navigator.of(context, rootNavigator: true).pop();
      _showToast('Errore di connessione o del server');
    }
  }

  // -------------------- menu / navigation --------------------

  Future<void> _showPreferenzeDialog() async {
    final updatedPrefs = await showDialog<Map<String, String>>(
      context: context,
      barrierDismissible: false,
      builder: (_) =>
          PreferenzeDialog(utente: widget.utente, apiClient: widget.apiClient),
    );

    if (updatedPrefs != null) {
      setState(() => widget.utente.preferenze = updatedPrefs);
    }
  }

  void _logout() {
    Navigator.of(context).pop();
  }

  void _vaiAlloStorico() {
    Navigator.push(
      context,
      MaterialPageRoute(
        builder: (context) =>
            HistoryScreen(utente: widget.utente, apiClient: widget.apiClient),
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
      _logout();
    }
  }

  Future<void> _searchAndGo(String query) async {
    final q = query.trim();
    if (q.isEmpty) return;

    try {
      final data = await widget.apiClient.geocode(address: q);

      final results = (data['results'] as List).cast<Map<String, dynamic>>();
      if (results.isEmpty) {
        _showToast('Nessun risultato trovato.');
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
      _showToast('Errore durante la ricerca.');
    }
  }

  // -------------------- UI --------------------

  @override
  Widget build(BuildContext context) {
    final fullName = '${widget.utente.nome} ${widget.utente.cognome}'.trim();
    final int safeIdx = _selectedRouteIndex.clamp(
      0,
      _routes.isEmpty ? 0 : _routes.length - 1,
    );
    final int stepsTotal = _routes.isEmpty ? 0 : _routes[safeIdx].steps.length;
    (_navStepIndex + 1).clamp(1, stepsTotal == 0 ? 1 : stepsTotal);
    final selected = _selectedParkingData;
    final int postiDispSelected =
        (selected?['postiDisponibili'] as num?)?.toInt() ??
        (selected?['posti_disponibili'] as num?)?.toInt() ??
        0;

    final bool isFullSelected = postiDispSelected <= 0;

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
                      child: Stack(
                        children: [
                          GoogleMap(
                            onCameraMove: (pos) => _cameraTarget = pos.target,
                            initialCameraPosition: _initialCamera,
                            onMapCreated: (c) async {
                              _mapController = c;
                              await _mapController!.setMapStyle(_mapStyleNoPoi);

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
                            polylines: _polylines,
                            scrollGesturesEnabled: !_lockMapGestures,
                            zoomGesturesEnabled: !_lockMapGestures,
                            rotateGesturesEnabled: !_lockMapGestures,
                            tiltGesturesEnabled: !_lockMapGestures,
                            onTap: (_) async {
                              setState(() {
                                _selectedParkingData = null;
                                _selectedParkingMarkerId = null;
                              });
                              if (_showParkings &&
                                  _bookedParkingMarkerId == null) {
                                await _loadParkingsNearby(
                                  _cameraTarget,
                                  radiusMeters: 1200,
                                );
                              }
                            },
                          ),

                          if (_isLoadingParkings ||
                              _isLoadingRoute ||
                              _isLocating ||
                              _isStartingNav)
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
                                    onPressed: _toggleParkings,
                                    tooltip: 'Parcheggi vicino',
                                    selected: _showParkings,
                                  ),
                                  const SizedBox(height: 10),
                                  GMapsControlButton(
                                    icon: Icons.route,
                                    onPressed: _routes.isEmpty
                                        ? null
                                        : _showRouteChoiceSheet,
                                    tooltip: 'Percorso / Navigazione',
                                    selected: _showRoutePanel,
                                  ),
                                ],
                              ),
                            ),
                          ),

                          if (_showRoutePanel &&
                              _routes.isNotEmpty &&
                              !_isStartingNav)
                            _navActive
                                ? Align(
                                    alignment: Alignment.bottomCenter,
                                    child: SafeArea(
                                      child: Padding(
                                        padding: const EdgeInsets.only(
                                          bottom: 16,
                                        ),
                                        child: SizedBox(
                                          width: 520,
                                          child: Material(
                                            color: Colors.transparent,
                                            child: Container(
                                              padding:
                                                  const EdgeInsets.fromLTRB(
                                                    16,
                                                    14,
                                                    16,
                                                    16,
                                                  ),
                                              decoration: BoxDecoration(
                                                color: Colors.white,
                                                borderRadius:
                                                    BorderRadius.circular(18),
                                                boxShadow: [
                                                  BoxShadow(
                                                    color: Colors.black
                                                        .withOpacity(0.18),
                                                    blurRadius: 24,
                                                    offset: const Offset(0, 10),
                                                  ),
                                                ],
                                              ),
                                              child: RoutePanel(
                                                compact: true,
                                                currentInstruction:
                                                    _currentInstruction(),
                                                stepNow: (_navStepIndex + 1),
                                                stepsTotal:
                                                    _routes[_selectedRouteIndex]
                                                        .steps
                                                        .length,

                                                routes: _routes
                                                    .map(
                                                      (r) => RoutePanelRoute(
                                                        summary: _routeMeta(r),
                                                        steps: r.steps,
                                                      ),
                                                    )
                                                    .toList(),
                                                selectedIndex:
                                                    _selectedRouteIndex,
                                                onSelectIndex: (i) {
                                                  setState(
                                                    () =>
                                                        _selectedRouteIndex = i,
                                                  );
                                                  _navRoutePts = null;
                                                  _navRouteIndexCached = null;
                                                  _navStepIndex = 0;
                                                  _applySelectedRoute();
                                                },
                                                onClose: _closeRoutePanel,
                                                onStart: _startInAppNavigation,
                                                onStop: _stopInAppNavigation,
                                                navActive: _navActive,
                                                stripHtml: _stripHtml,
                                                setMapGesturesLocked:
                                                    _setMapGesturesLocked,
                                              ),
                                            ),
                                          ),
                                        ),
                                      ),
                                    ),
                                  )
                                : Positioned(
                                    left: 16,
                                    top: 16,
                                    bottom: 16,
                                    child: SafeArea(
                                      child: ConstrainedBox(
                                        constraints: const BoxConstraints(
                                          maxWidth: 420,
                                        ),
                                        child: Material(
                                          color: Colors.transparent,
                                          child: Container(
                                            padding: const EdgeInsets.fromLTRB(
                                              16,
                                              14,
                                              16,
                                              16,
                                            ),
                                            decoration: BoxDecoration(
                                              color: Colors.white,
                                              borderRadius:
                                                  BorderRadius.circular(18),
                                              boxShadow: [
                                                BoxShadow(
                                                  color: Colors.black
                                                      .withOpacity(0.18),
                                                  blurRadius: 24,
                                                  offset: const Offset(0, 10),
                                                ),
                                              ],
                                            ),
                                            child: RoutePanel(
                                              compact: false,
                                              currentInstruction: '',
                                              stepNow: 0,
                                              stepsTotal: 0,

                                              routes: _routes
                                                  .map(
                                                    (r) => RoutePanelRoute(
                                                      summary: _routeMeta(r),
                                                      steps: r.steps,
                                                    ),
                                                  )
                                                  .toList(),
                                              selectedIndex:
                                                  _selectedRouteIndex,
                                              onSelectIndex: (i) {
                                                setState(
                                                  () => _selectedRouteIndex = i,
                                                );
                                                _navRoutePts = null;
                                                _navRouteIndexCached = null;
                                                _navStepIndex = 0;
                                                _applySelectedRoute();
                                              },
                                              onClose: _closeRoutePanel,
                                              onStart: _startInAppNavigation,
                                              onStop: _stopInAppNavigation,
                                              navActive: _navActive,
                                              stripHtml: _stripHtml,
                                              setMapGesturesLocked:
                                                  _setMapGesturesLocked,
                                            ),
                                          ),
                                        ),
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
                                onClose: () async {
                                  setState(() {
                                    _selectedParkingData = null;
                                    _selectedParkingMarkerId = null;
                                  });

                                  if (_showParkings &&
                                      _bookedParkingMarkerId == null) {
                                    await _loadParkingsNearby(
                                      _cameraTarget,
                                      radiusMeters: 1200,
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

                                  _showOnlyBookedParking(p);

                                  _effettuaPrenotazione(
                                    pId,
                                    destLat: destLat,
                                    destLng: destLng,
                                    parkingData: p,
                                  );
                                },
                              ),
                            ),
                        ],
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
