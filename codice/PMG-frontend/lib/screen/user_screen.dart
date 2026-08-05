import 'dart:async';
import 'dart:convert';
import 'dart:ui' as ui;
import 'dart:html' as html;

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:geolocator/geolocator.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:http/http.dart' as http;
import 'package:park_mg/indoor/assignment/assignment_provider.dart';
import 'package:park_mg/indoor/parking_map_definition.dart';
import 'package:park_mg/indoor/models/indoor_models.dart';
import 'package:park_mg/indoor/ui/indoor_parking_view.dart';
import 'package:park_mg/models/prenotazione.dart';
import 'package:park_mg/screen/home_page.dart';
import 'package:park_mg/utils/ui_feedback.dart';
import 'package:park_mg/widgets/gestione_sosta_inline_view.dart';
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
  Timer? _bookingStatusTimer;
  Timer? _fakeArrivalTimer;
  bool _fakeArrivalTriggered = false;
  bool _showGestioneSostaView = false;
  bool _waitingPaymentDialogClose = false;
  final GlobalKey<IndoorParkingViewState> _indoorKey =
      GlobalKey<IndoorParkingViewState>();

  Timer? _parkedConfirmTimer;
  bool _parkingConfirmVisible = false;
  bool _forceHideIndoorMap = false;
  IndoorMapDefinition? _indoorDef;
  IndoorAssignment? _indoorAssignment;
  bool _isLoadingIndoorMap = false;
  String? _indoorMapError;
  String? _loadedIndoorParkingId;
  String? _loadedIndoorSlotId;

  bool get _showIndoorMap {
    final b = _activeBooking;
    if (_forceHideIndoorMap) return false;
    if (b == null) return false;
    return b.stato == StatoPrenotazione.inCorso;
  }

  bool get _showMainMapView => !_showIndoorMap && !_showGestioneSostaView;

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

  void _startBookingStatusPolling() {
    _bookingStatusTimer?.cancel();

    _bookingStatusTimer = Timer.periodic(const Duration(seconds: 2), (_) async {
      await _refreshActiveBookingAndMaybeEnterIndoor();
    });
  }

  void _stopBookingStatusPolling() {
    _bookingStatusTimer?.cancel();
    _bookingStatusTimer = null;
  }

  void _handleIndoorArrivedToSlot() {
    if (!mounted) return;
    if (_parkingConfirmVisible) return;

    _parkedConfirmTimer?.cancel();
    _parkedConfirmTimer = Timer(const Duration(seconds: 2), () async {
      if (!mounted) return;
      if (_parkingConfirmVisible) return;

      _parkingConfirmVisible = true;
      await _showParkedConfirmationPopup();
      _parkingConfirmVisible = false;
    });
  }

  Future<void> _openGestioneSostaInline() async {
    if (!mounted || _activeBooking == null) return;

    try {
      final aggiornata = await widget.apiClient.confermaParcheggio(
        _activeBooking!.id,
      );

      if (!mounted) return;

      setState(() {
        _activeBooking = aggiornata;
        _forceHideIndoorMap = true;
        _showGestioneSostaView = true;
        _blockMapInteractions = false;
        _lockMapGestures = false;
      });
    } catch (e) {
      if (!mounted) return;
      UiFeedback.showError(context, 'Errore conferma parcheggio: $e');
    }
  }

  Future<void> _resetMapAfterPayment() async {
    if (!mounted) return;

    setState(() {
      _activeBooking = null;
      _activeParkingLatLng = null;
      _arrivalHandled = false;

      _bookedParkingMarkerId = null;
      _bookedMarkerLocked = false;

      _selectedParkingData = null;
      _selectedParkingMarkerId = null;

      _showParkings = false;
      _showGestioneSostaView = false;
      _forceHideIndoorMap = false;

      _distanceToParkingM = null;
      _returnOverlay = false;
      _arrivalUiDone = false;
      _externalNavOpened = false;

      _indoorDef = null;
      _indoorAssignment = null;
      _indoorMapError = null;
      _loadedIndoorParkingId = null;
      _loadedIndoorSlotId = null;

      _markers.removeWhere((m) => m.markerId.value.startsWith('p_'));
    });

    await _bootstrapMyLocation();
  }

  Future<void> _showParkedConfirmationPopup() async {
    final result = await showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.bgDark,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: const Text(
          'Hai parcheggiato?',
          style: TextStyle(
            color: AppColors.textPrimary,
            fontWeight: FontWeight.w800,
          ),
        ),
        content: const Text(
          'Se hai completato il parcheggio, prosegui alla gestione della sosta.',
          style: TextStyle(color: AppColors.textPrimary),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text(
              'No',
              style: TextStyle(color: Colors.orangeAccent),
            ),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.accentCyan,
              foregroundColor: Colors.black,
            ),
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('Sì'),
          ),
        ],
      ),
    );

    if (!mounted) return;

    if (result == true) {
      await _openGestioneSostaInline();
    } else {
      _restartIndoorAnimation();
    }
  }

  void _restartIndoorAnimation() {
    _parkedConfirmTimer?.cancel();
    _indoorKey.currentState?.restartRouteAnimation();
  }

  String _formatDistance(double? meters) {
    if (meters == null) return 'Calcolo distanza…';

    if (meters >= 1000) {
      final km = meters / 1000;
      final hasDecimal = (km * 10) % 10 != 0;
      return 'Distanza: ${hasDecimal ? km.toStringAsFixed(1) : km.toStringAsFixed(0)} km';
    }

    return 'Distanza: ${meters.toStringAsFixed(0)} m';
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
          : () => _selectParking(p),
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

  Future<Map<String, dynamic>> _enrichParkingWithSpotStats(
    Map<String, dynamic> parking,
  ) async {
    final enriched = Map<String, dynamic>.from(parking);
    final floorConfigs =
        parking['configurazionePiani'] as List<dynamic>? ?? const [];

    if (floorConfigs.isEmpty) {
      return enriched;
    }

    int total = 0;
    int available = 0;
    bool hasDetailedSlots = false;

    for (final floorRaw in floorConfigs) {
      final floor = floorRaw as Map<String, dynamic>;
      final slotCount = (floor['numeroPosti'] as num?)?.toInt() ?? 0;
      final slots = floor['posti'] as List<dynamic>? ?? const [];

      total += slotCount;

      if (slots.isNotEmpty) {
        hasDetailedSlots = true;
        final generatedFreeSlots = slotCount > slots.length
            ? slotCount - slots.length
            : 0;

        available += generatedFreeSlots;
        available += slots.where((raw) {
          final slot = raw as Map<String, dynamic>;
          final outOfService =
              slot['fuoriServizio'] as bool? ??
              slot['disabilitato'] as bool? ??
              false;
          final status = slot['stato']?.toString().toUpperCase();
          final isAvailable = slot['disponibile'] as bool?;

          if (outOfService) return false;
          if (status != null) return status == 'LIBERO';
          return isAvailable ?? true;
        }).length;
      }
    }

    enriched['postiTotali'] = total;
    enriched['postiDisponibili'] = hasDetailedSlots
        ? available
        : (parking['postiDisponibili'] as num?)?.toInt() ?? total;

    return enriched;
  }

  Future<void> _selectParking(Map<String, dynamic> parking) async {
    setState(() {
      _selectedParkingMarkerId = 'p_${parking['id']}';
      _selectedParkingData = parking;
    });

    final enrichedParking = await _enrichParkingWithSpotStats(parking);
    if (!mounted) return;

    setState(() {
      _selectedParkingData = enrichedParking;
    });
  }

  // -------------------- utils --------------------

  Future<void> _prepareIndoorMap() async {
    final booking = _activeBooking;
    final assignedSlotId = booking?.posto?.slotId;

    if (booking == null || assignedSlotId == null || assignedSlotId.isEmpty) {
      if (!mounted) return;
      setState(() {
        _indoorMapError = 'La prenotazione non contiene uno slot assegnato.';
        _isLoadingIndoorMap = false;
      });
      return;
    }

    final sameMapAlreadyLoaded =
        _loadedIndoorParkingId == booking.parcheggioId &&
        _loadedIndoorSlotId == assignedSlotId &&
        _indoorDef != null &&
        _indoorAssignment != null;

    if (sameMapAlreadyLoaded || _isLoadingIndoorMap) {
      return;
    }

    setState(() {
      _isLoadingIndoorMap = true;
      _indoorMapError = null;
      _indoorDef = null;
      _indoorAssignment = null;
    });

    try {
      final definition = await widget.apiClient.getIndoorParkingMap(
        booking.parcheggioId,
      );

      final assignment = IndoorAssignmentProvider(
        definition: definition,
      ).fromSlotId(assignedSlotId);

      if (!mounted) return;

      setState(() {
        _indoorDef = definition;
        _indoorAssignment = assignment;
        _loadedIndoorParkingId = booking.parcheggioId;
        _loadedIndoorSlotId = assignedSlotId;
      });
    } catch (error) {
      if (!mounted) return;

      setState(() {
        _indoorMapError = 'Impossibile generare la mappa indoor: $error';
      });
    } finally {
      if (mounted) {
        setState(() {
          _isLoadingIndoorMap = false;
        });
      }
    }
  }

  Widget _buildIndoorMapContent() {
    if (_isLoadingIndoorMap) {
      return const Center(
        child: CircularProgressIndicator(
          color: AppColors.accentCyan,
        ),
      );
    }

    final error = _indoorMapError;
    if (error != null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(
                Icons.error_outline,
                color: Colors.orangeAccent,
                size: 42,
              ),
              const SizedBox(height: 12),
              Text(
                error,
                textAlign: TextAlign.center,
                style: const TextStyle(color: Colors.white),
              ),
              const SizedBox(height: 16),
              ElevatedButton(
                onPressed: _prepareIndoorMap,
                child: const Text('Riprova'),
              ),
            ],
          ),
        ),
      );
    }

    final definition = _indoorDef;
    final assignment = _indoorAssignment;

    if (definition == null || assignment == null) {
      return const Center(
        child: Text(
          'Preparazione della mappa indoor…',
          style: TextStyle(color: Colors.white70),
        ),
      );
    }

    return IndoorParkingView(
      key: _indoorKey,
      def: definition,
      assignment: assignment,
      userFloor: 1,
      showGridDebug: false,
      onArrivedToSlot: _handleIndoorArrivedToSlot,
    );
  }

  void _enterIndoorModeIfNeeded() {
    debugPrint('ENTER INDOOR called, showIndoor=$_showIndoorMap');
    if (!mounted) return;
    if (!_showIndoorMap) return;

    _stopReturnOverlay();
    _prepareIndoorMap();

    final nav = Navigator.of(context);
    if (nav.canPop()) nav.pop();

    setState(() {
      _selectedParkingData = null;
      _selectedParkingMarkerId = null;
      _showParkings = false;
      _returnOverlay = false;
      _arrivalUiDone = false;
    });
  }

  Future<void> _showQrOnLoginIfNeeded() async {
    if (!mounted) return;
    if (_qrShownOnLogin) return;
    if (_activeBooking == null) return;
    if (_activeParkingLatLng == null) return;
    if (_activeBooking!.stato == StatoPrenotazione.parcheggiato ||
        _activeBooking!.stato == StatoPrenotazione.pagato) {
      return;
    }

    _qrShownOnLogin = true;
    _openingQrDialog = true;

    try {
      final arrived = await _isArrivedToActiveParking();
      _autoNavAfterQrClose = !arrived;

      await _showMapLockedDialog<void>(
        show: () async {
          final updated = await widget.apiClient.getPrenotazioneByIdFromStorico(
            widget.utente.id,
            _activeBooking!.id,
          );

          if (!mounted) return;

          if (updated != null) {
            setState(() => _activeBooking = updated);
            _enterIndoorModeIfNeeded();
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
                          locationSettings: LocationSettings(
                            accuracy: LocationAccuracy.high,
                          )
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
            locationSettings: LocationSettings(
              accuracy: LocationAccuracy.high,
            )
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

  Future<void> _restoreActiveBookingFromBackend() async {
    try {
      final storico = await widget.apiClient.getStoricoPrenotazioni(
        widget.utente.id,
      );

      final attive = storico
          .where(
            (p) =>
                p.stato == StatoPrenotazione.attiva ||
                p.stato == StatoPrenotazione.inCorso ||
                p.stato == StatoPrenotazione.parcheggiato ||
                p.stato == StatoPrenotazione.pagato,
          )
          .toList();

      if (attive.isEmpty) {
        if (!mounted) return;

        if (_waitingPaymentDialogClose) {
          return;
        }

        setState(() {
          _activeBooking = null;
          _activeParkingLatLng = null;
          _arrivalHandled = false;
          _bookedParkingMarkerId = null;
          _bookedMarkerLocked = false;
          _externalNavOpened = false;
          _forceHideIndoorMap = false;
          _showGestioneSostaView = false;
          _indoorDef = null;
          _indoorAssignment = null;
          _indoorMapError = null;
          _loadedIndoorParkingId = null;
          _loadedIndoorSlotId = null;
        });
        return;
      }

      attive.sort((a, b) {
        final da = a.dataCreazione ?? DateTime.fromMillisecondsSinceEpoch(0);
        final db = b.dataCreazione ?? DateTime.fromMillisecondsSinceEpoch(0);
        return db.compareTo(da);
      });

      final booking = attive.first;

      final park = await widget.apiClient.getParcheggioById(
        booking.parcheggioId,
      );

      final lat = (park['latitudine'] as num).toDouble();
      final lng = (park['longitudine'] as num).toDouble();

      if (!mounted) return;

      final bool showGestione =
          booking.stato == StatoPrenotazione.parcheggiato ||
          booking.stato == StatoPrenotazione.pagato;

      setState(() {
        _activeBooking = booking;
        _activeParkingLatLng = LatLng(lat, lng);
        _arrivalHandled = showGestione;
        _externalNavOpened = false;
        _distanceToParkingM = null;
        _returnOverlay = false;
        _bookedMarkerLocked = true;
        _forceHideIndoorMap = showGestione;
        _showGestioneSostaView = showGestione;
      });

      if (booking.stato == StatoPrenotazione.inCorso) {
        await _prepareIndoorMap();
      }

      _enterIndoorModeIfNeeded();
      _showOnlyBookedParking(park);
    } catch (_) {}
  }

  void _startReturnOverlay() {
    if (!mounted) return;
    if (_openingQrDialog) return;
    if (_showIndoorMap) return;
    if (_activeBooking == null || _activeParkingLatLng == null) return;
    if (_arrivalHandled) return;

    final stato = _activeBooking!.stato;
    if (stato == StatoPrenotazione.parcheggiato ||
        stato == StatoPrenotazione.pagato) {
      return;
    }

    _fakeArrivalTimer?.cancel();
    _fakeArrivalTriggered = false;

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

    // TEST: se resta troppo tempo su "calcolo distanza", forza l'arrivo
    if (_distanceToParkingM == null && !_fakeArrivalTriggered) {
      _fakeArrivalTriggered = true;
      _fakeArrivalTimer?.cancel();
      _fakeArrivalTimer = Timer(const Duration(seconds: 5), () async {
        if (!mounted) return;
        if (_arrivalHandled || _arrivalUiDone || _openingQrDialog) return;

        _openingQrDialog = true;
        setState(() {
          _arrivalUiDone = true;
          _distanceToParkingM = 0;
        });

        await Future.delayed(const Duration(milliseconds: 800));
        if (!mounted) return;

        _arrivalHandled = true;
        _stopReturnOverlay();

        _startBookingStatusPolling();

        await _showMapLockedDialog<void>(
          show: () async {
            final updated = await widget.apiClient
                .getPrenotazioneByIdFromStorico(
                  widget.utente.id,
                  _activeBooking!.id,
                );

            if (!mounted) return;

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

        await _refreshActiveBookingAndMaybeEnterIndoor();
        _stopBookingStatusPolling();
        _openingQrDialog = false;
      });
    }

    try {
      final pos = await Geolocator.getCurrentPosition(
        locationSettings: LocationSettings(
          accuracy: LocationAccuracy.high,
        ),
      ).timeout(const Duration(seconds: 6));
      final me = LatLng(pos.latitude, pos.longitude);
      _lastMe = me;
      final dest = _activeParkingLatLng!;

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

      _fakeArrivalTimer?.cancel();

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

        _startBookingStatusPolling();

        await _showMapLockedDialog<void>(
          show: () async {
            final updated = await widget.apiClient
                .getPrenotazioneByIdFromStorico(
                  widget.utente.id,
                  _activeBooking!.id,
                );

            if (!mounted) return;

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

        await _refreshActiveBookingAndMaybeEnterIndoor();
        _stopBookingStatusPolling();
        _openingQrDialog = false;
      }
    } catch (_) {}
  }

  Future<void> _refreshActiveBookingAndMaybeEnterIndoor() async {
    if (!mounted || _activeBooking == null) return;

    try {
      final updated = await widget.apiClient.getPrenotazioneByIdFromStorico(
        widget.utente.id,
        _activeBooking!.id,
      );

      if (!mounted || updated == null) return;

      final changed = _activeBooking!.stato != updated.stato;

      setState(() {
        _activeBooking = updated;
      });

      if (!changed) return;

      if (updated.stato == StatoPrenotazione.inCorso) {
        setState(() {
          _showGestioneSostaView = false;
          _forceHideIndoorMap = false;
          _arrivalHandled = false;
        });
        _stopBookingStatusPolling();
        await _prepareIndoorMap();
        _enterIndoorModeIfNeeded();
        return;
      }

      if (updated.stato == StatoPrenotazione.parcheggiato) {
        setState(() {
          _showGestioneSostaView = true;
          _forceHideIndoorMap = true;
          _arrivalHandled = true;
        });
        _stopReturnOverlay();
        _stopBookingStatusPolling();
        return;
      }

      if (updated.stato == StatoPrenotazione.pagato) {
        setState(() {
          _showGestioneSostaView = true;
          _forceHideIndoorMap = true;
          _arrivalHandled = true;
          _waitingPaymentDialogClose = true;
        });
        _stopReturnOverlay();
        _stopBookingStatusPolling();
        return;
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
      if (!mounted) return;
      if (_openingQrDialog) return;
      if (_showGestioneSostaView) return;
      if (_showIndoorMap) return;
      if (_waitingPaymentDialogClose) return;
      if (_blockMapInteractions) return;

      Future.microtask(() async {
        await _restoreActiveBookingFromBackend();
        _enterIndoorModeIfNeeded();
        _startReturnOverlay();
      });
    }
  }

  void _updatePulseCenter(LatLng me) {
    _lastMe = me;

    setState(() {
      _circles.removeWhere((c) => c.circleId.value == 'pulse');
      _circles.add(
        Circle(
          circleId: const CircleId('pulse'),
          center: me,
          radius: _pulseAnimation.value,
          fillColor: Colors.blue.withValues(alpha: 0.25),
          strokeColor: Colors.blue.withValues(alpha: 0.1),
          strokeWidth: 1,
          zIndex: 900,
        ),
      );

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

      _forceHideIndoorMap = false;
      _showGestioneSostaView = false;

      _indoorDef = null;
      _indoorAssignment = null;
      _indoorMapError = null;
      _loadedIndoorParkingId = null;
      _loadedIndoorSlotId = null;

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
        if (_showGestioneSostaView) return;
        if (_showIndoorMap) return;
        if (_waitingPaymentDialogClose) return;
        if (_blockMapInteractions) return;

        Future.microtask(() async {
          await _restoreActiveBookingFromBackend();
          _enterIndoorModeIfNeeded();
          _startReturnOverlay();
        });
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
      if (_showMainMapView) {
        await _showQrOnLoginIfNeeded();
      }
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
                fillColor: Colors.blue.withValues(alpha: 0.25),
                strokeColor: Colors.blue.withValues(alpha: 0.1),
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
    _bookingStatusTimer?.cancel();
    _parkedConfirmTimer?.cancel();
    _fakeArrivalTimer?.cancel();
    super.dispose();
  }

  Future<T> retry<T>(
  Future<T> Function() fn, {
  int retries = 3,
  Duration delay = const Duration(seconds: 2),
}) async {
  int attempt = 0;

  while (true) {
    try {
      return await fn();
    } on TimeoutException catch (_) {
      attempt++;

      if (attempt >= retries) {
        throw ApiException('Timeout dopo $retries tentativi');
      }

      // Aspetta prima di riprovare
      await Future.delayed(delay);
    }
  }
}

  // -------------------- location / icons --------------------
Future<void> _bootstrapMyLocation() async {
  if (_isLocating) return;
  final int token = _sessionToken;

  if (context.mounted) setState(() => _isLocating = true);

  try {
    final serviceEnabled = await Geolocator.isLocationServiceEnabled()
        .timeout(const Duration(seconds: 3));

    if (!serviceEnabled && !kIsWeb) {
      if (!mounted) return;
      UiFeedback.showError(context, 'Servizi di localizzazione disattivati.');
      return;
    }

    LocationPermission perm = await Geolocator.checkPermission()
        .timeout(const Duration(seconds: 3));

    if (perm == LocationPermission.denied) {
      perm = await Geolocator.requestPermission()
          .timeout(const Duration(seconds: 8));
    }

    if (perm == LocationPermission.denied ||
        perm == LocationPermission.deniedForever) {
      if (token != _sessionToken) return;
      if (!mounted) return;

      if (context.mounted) setState(() => _locationGranted = false);
      UiFeedback.showError(context, 'Permesso posizione negato.');
      return;
    }

    final pos = await retry(() async {
      return await Geolocator.getCurrentPosition(
        locationSettings: const LocationSettings(
          accuracy: LocationAccuracy.high,
        ),
      ).timeout(const Duration(seconds: 8));
    });

    if (token != _sessionToken) return;
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
            fillColor: Colors.blue.withValues(alpha: 0.25),
            strokeColor: Colors.blue.withValues(alpha: 0.1),
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
        CameraUpdate.newCameraPosition(
          CameraPosition(target: me, zoom: 16),
        ),
      );
    } else {
      _pendingCenter = me;
    }
  } on TimeoutException {
    if (token != _sessionToken || !mounted) return;
    UiFeedback.showError(context, 'Posizione non disponibile, riprova.');
  } catch (_) {
    if (token != _sessionToken || !mounted) return;
    UiFeedback.showError(context, 'Impossibile ottenere la posizione.');
  } finally {
    if (token == _sessionToken && mounted) {
      setState(() => _isLocating = false);
    }
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

  // -------------------- parkings --------------------

  Future<void> _toggleParkings() async {
    if (_bookedParkingMarkerId != null) {
      UiFeedback.showInfo(
        context,
        'Hai già una prenotazione attiva: viene mostrato solo il parcheggio prenotato.',
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

    final res = await retry(() async {
      return await http.get(uri).timeout(const Duration(seconds: 8));
    });

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
    UiFeedback.showError(context, 'Connessione lenta. Riprova.');
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
            _selectParking(p);

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
      final pos = await retry(() async {
        return await Geolocator.getCurrentPosition(
          locationSettings: const LocationSettings(
            accuracy: LocationAccuracy.high,
          ),
        ).timeout(const Duration(seconds: 5));
      });

      origin = LatLng(pos.latitude, pos.longitude);
    } catch (_) {
      // silent fallback
    }
  }

  try {
    final risposta = await retry(() async {
      return await widget.apiClient
          .prenotaParcheggio(
            utenteId: widget.utente.id,
            parcheggioId: parcheggioId,
            dataCreazione: DateTime.now().toIso8601String(),
            originLat: origin?.latitude,
            originLng: origin?.longitude,
          )
          .timeout(const Duration(seconds: 12));
    });

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
          if (_bookingCancelledInDialog) return;

          setState(() {
            _activeBooking = risposta;
            _activeParkingLatLng = LatLng(destLat, destLng);
            _arrivalHandled = false;
          });

          setState(() {
            _distanceToParkingM = null;
            _returnOverlay = false;
          });

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
    UiFeedback.showError(context, 'Connessione lenta. Riprova.');
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
                'Preferenze',
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
        if(!mounted) return;
        UiFeedback.showInfo(context, 'Nessun risultato trovato.');
        return;
      }

      final first = results.first;
      final lat = (first['lat'] as num).toDouble();
      final lng = (first['lng'] as num).toDouble();

      final target = LatLng(lat, lng);

      setState(() {
        _showParkings = false;
        _selectedParkingData = null;
        _selectedParkingMarkerId = null;
        _lastParkingsJson = null;

        _markers.removeWhere(
          (m) =>
              m.markerId.value.startsWith('p_') &&
              m.markerId.value != _bookedParkingMarkerId,
        );
      });

      await _mapController?.animateCamera(
        CameraUpdate.newCameraPosition(
          CameraPosition(target: target, zoom: 15),
        ),
      );

      _searchController.clear();
      if(!mounted) return;
      FocusScope.of(context).unfocus();
    } catch (_) {
      if (!mounted) return;
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
                        color: Colors.black.withValues(alpha: 0.35),
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

                if (_showMainMapView) ...[
                  TextField(
                    controller: _searchController,
                    style: const TextStyle(color: AppColors.textPrimary),
                    cursorColor: AppColors.accentCyan,
                    decoration: InputDecoration(
                      hintText: 'Search your Park',
                      hintStyle: TextStyle(
                        color: AppColors.textMuted.withValues(alpha: 0.9),
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
                      fillColor: AppColors.bgDark2.withValues(alpha: 0.35),
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
                ],

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
                          color: Colors.black.withValues(alpha: 0.35),
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
                            if (_showGestioneSostaView)
                              GestioneSostaInlineView(
                                utente: widget.utente,
                                apiClient: widget.apiClient,
                                onPaymentCompleted: () async {
                                  if (!mounted) return;

                                  setState(() {
                                    _waitingPaymentDialogClose = false;
                                  });

                                  await _resetMapAfterPayment();
                                },
                              )
                            else if (_showIndoorMap)
                              _buildIndoorMapContent()
                            else
                              GoogleMap(
                                onCameraMove: (pos) =>
                                    _cameraTarget = pos.target,
                                initialCameraPosition: _initialCamera,
                                style: _mapStyleNoPoi,
                                onMapCreated: (controller) async {
                                  _mapController = controller;
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
                            if (_showMainMapView)
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

                            if (_showMainMapView &&
                                _selectedParkingData != null)
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
                                                : _formatDistance(
                                                    _distanceToParkingM,
                                                  ),
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
                                                color: Colors.green.withValues(
                                                  alpha: 0.18,
                                                ),
                                                shape: BoxShape.circle,
                                                border: Border.all(
                                                  color: Colors.greenAccent
                                                      .withValues(alpha: 0.6),
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
