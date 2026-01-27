import 'dart:convert';
import 'dart:ui' as ui;

import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_polyline_points/flutter_polyline_points.dart';
import 'package:geolocator/geolocator.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:http/http.dart' as http;
import 'package:park_mg/widgets/preferenze_dialog.dart';
import 'package:url_launcher/url_launcher.dart';

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

  List<_DirectionsRoute> _routes = [];
  int _selectedRouteIndex = 0;

  bool _isLoadingRoute = false;
  String? _routeSummary;

  bool _isLocating = false;
  LatLng? _pendingCenter;

  LatLng? _activeDestination;
  LatLng? _activeOriginAtBooking;

  bool _routeSheetOpen = false;
  bool _showRoutePanel = false;

  bool _lockMapGestures = false;

  void _setMapGesturesLocked(bool v) {
    if (_lockMapGestures == v) return;
    setState(() => _lockMapGestures = v);
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
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
          content: Text(msg, style: const TextStyle(color: AppColors.textPrimary)),
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

    WidgetsBinding.instance.addPostFrameCallback((_) => _bootstrapMyLocation());

    if (widget.utente.preferenze == null || widget.utente.preferenze!.isEmpty) {
      WidgetsBinding.instance.addPostFrameCallback((_) => _showPreferenzeDialog());
    }

    _pulseController = AnimationController(
      vsync: this,
      duration: const Duration(seconds: 2),
    )..repeat(reverse: true);

    _pulseAnimation = Tween<double>(begin: 15, end: 35).animate(
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
      if (perm == LocationPermission.denied) perm = await Geolocator.requestPermission();

      if (perm == LocationPermission.denied || perm == LocationPermission.deniedForever) {
        setState(() => _locationGranted = false);
        _showToast('Permesso posizione negato.');
        return;
      }

      final pos = await Geolocator.getCurrentPosition(desiredAccuracy: LocationAccuracy.high);
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
    final offset = Offset((size - textPainter.width) / 2, (size - textPainter.height) / 2);
    textPainter.paint(canvas, offset);

    final picture = recorder.endRecording();
    final img = await picture.toImage(size.toInt(), size.toInt());
    final bytes = await img.toByteData(format: ui.ImageByteFormat.png);
    return BitmapDescriptor.bytes(bytes!.buffer.asUint8List());
  }

  // -------------------- routes --------------------

  void _applySelectedRoute() {
    if (_routes.isEmpty) return;
    final route = _routes[_selectedRouteIndex];

    setState(() {
      _polylines
        ..removeWhere((p) => p.polylineId.value == 'route')
        ..add(
          Polyline(
            polylineId: const PolylineId('route'),
            points: route.points,
            width: 6,
            color: AppColors.accentCyan,
            geodesic: true,
          ),
        );

      _routeSummary = route.summary;
    });

    if (route.points.isNotEmpty) _fitCameraToPoints(route.points);
  }

  void _showRouteChoiceSheet() {
    if (_routes.isEmpty) return;
    setState(() {
      _showRoutePanel = true;
      _routeSheetOpen = true;
    });
  }

  void _closeRoutePanel() {
    setState(() {
      _showRoutePanel = false;
      _routeSheetOpen = false;
      _lockMapGestures = false;
    });
  }

  Future<void> _startExternalNavigation() async {
    final dest = _activeDestination;
    if (dest == null) {
      _showToast('Destinazione non disponibile.');
      return;
    }

    final origin = _activeOriginAtBooking;

    final qp = <String, String>{
      'api': '1',
      'destination': '${dest.latitude},${dest.longitude}',
      'travelmode': 'driving',
    };

    if (origin != null) qp['origin'] = '${origin.latitude},${origin.longitude}';

    final uri = Uri.https('www.google.com', '/maps/dir/', qp);

    final ok = await launchUrl(uri, mode: LaunchMode.externalApplication);
    if (!ok) _showToast('Impossibile aprire il navigatore.');
  }

  Future<void> _showRouteToParking({
    required LatLng origin,
    required double destLat,
    required double destLng,
  }) async {
    setState(() {
      _isLoadingRoute = true;
      _routeSummary = null;
    });

    try {
      final destination = LatLng(destLat, destLng);

      final routes = await _fetchDirectionsRoutes(origin: origin, destination: destination);

      setState(() {
        _routes = routes;
        _selectedRouteIndex = 0;
      });

      _applySelectedRoute();

      setState(() {
        _showRoutePanel = true;
        _routeSheetOpen = true;
      });
    } catch (e, st) {
      debugPrint('Errore calcolo percorso: $e');
      debugPrintStack(stackTrace: st);
      _showToast('Errore calcolo percorso.');
    } finally {
      if (mounted) setState(() => _isLoadingRoute = false);
    }
  }

  Future<List<_DirectionsRoute>> _fetchDirectionsRoutes({
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
    if (routesJson.isEmpty) throw Exception('Nessun percorso dalla Directions API');

    return routesJson.map((r) {
      final poly = (r['polyline'] ?? '') as String;
      if (poly.isEmpty) throw Exception('Polyline vuota dal backend');

      _checkPolylineChars(poly);

      final decoded = PolylinePoints.decodePolyline(poly);
      final points = decoded.map((p) => LatLng(p.latitude, p.longitude)).toList();

      final dist = (r['distanceText'] ?? '') as String;
      final durTraffic = (r['durationInTrafficText'] ?? '') as String;
      final dur = (r['durationText'] ?? '') as String;

      final summary = (durTraffic.isNotEmpty ? durTraffic : dur);
      final summaryFull = (summary.isNotEmpty && dist.isNotEmpty)
          ? '$summary • $dist'
          : ((r['summary'] ?? 'Percorso pronto') as String);

      final steps = (r['steps'] ?? []) as List<dynamic>;

      return _DirectionsRoute(points: points, summary: summaryFull, steps: steps);
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
    setState(() => _showParkings = !_showParkings);

    if (!_showParkings) {
      setState(() => _markers.removeWhere((m) => m.markerId.value.startsWith('p_')));
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

        newMarkers.add(
          Marker(
            markerId: MarkerId(markerId),
            position: LatLng(
              (p['latitudine'] as num).toDouble(),
              (p['longitudine'] as num).toDouble(),
            ),
            icon: isSelected
                ? (_parkingIconSelected ?? _parkingIcon ?? BitmapDescriptor.defaultMarker)
                : (_parkingIcon ?? BitmapDescriptor.defaultMarker),
            infoWindow: const InfoWindow(title: ''),
            onTap: () {
              setState(() {
                _selectedParkingMarkerId = markerId;
                _selectedParkingData = p;
              });
              _loadParkingsNearby(_cameraTarget, radiusMeters: 1200);
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
  }) async {
    LatLng? origin;
    try {
      LocationPermission perm = await Geolocator.checkPermission();
      if (perm == LocationPermission.denied) perm = await Geolocator.requestPermission();

      if (perm != LocationPermission.denied && perm != LocationPermission.deniedForever) {
        final pos = await Geolocator.getCurrentPosition(desiredAccuracy: LocationAccuracy.high);
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

      setState(() {
        _activeDestination = LatLng(destLat, destLng);
        _activeOriginAtBooking = origin;
      });

      if (!mounted) return;

      Navigator.of(context, rootNavigator: true).pop();

      await PrenotazioneDialog.mostra(context, risposta);

      if (origin != null) {
        await _showRouteToParking(origin: origin, destLat: destLat, destLng: destLng);
      } else {
        _showToast('Posizione non disponibile: impossibile calcolare il percorso.');
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

    _loadParkingsNearby(_cameraTarget);
  }

  // -------------------- menu / navigation --------------------

  Future<void> _showPreferenzeDialog() async {
    final updatedPrefs = await showDialog<Map<String, String>>(
      context: context,
      barrierDismissible: false,
      builder: (_) => PreferenzeDialog(utente: widget.utente, apiClient: widget.apiClient),
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
        builder: (context) => HistoryScreen(utente: widget.utente, apiClient: widget.apiClient),
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
              Text('Preferences',
                  style: TextStyle(color: AppColors.textPrimary, fontWeight: FontWeight.w600)),
            ],
          ),
        ),
        PopupMenuItem<String>(
          value: 'history',
          child: Row(
            children: const [
              Icon(Icons.history, size: 18, color: AppColors.textPrimary),
              SizedBox(width: 10),
              Text('Le mie prenotazioni', style: TextStyle(color: AppColors.textPrimary)),
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
              Text('Logout',
                  style: TextStyle(color: AppColors.textPrimary, fontWeight: FontWeight.w600)),
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
        CameraUpdate.newCameraPosition(CameraPosition(target: target, zoom: 15)),
      );
    } catch (_) {
      _showToast('Errore durante la ricerca.');
    }
  }

  // -------------------- UI --------------------

  @override
  Widget build(BuildContext context) {
    final fullName = '${widget.utente.nome} ${widget.utente.cognome}'.trim();

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
                            border: Border.all(color: AppColors.borderField, width: 1),
                          ),
                          child: const Icon(Icons.settings, color: AppColors.textPrimary, size: 18),
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
                    hintStyle: TextStyle(color: AppColors.textMuted.withOpacity(0.9)),
                    prefixIcon: const Icon(Icons.search, color: AppColors.textMuted),
                    suffixIcon: IconButton(
                      icon: const Icon(Icons.arrow_forward, color: AppColors.accentCyan),
                      onPressed: () => _searchAndGo(_searchController.text),
                    ),
                    filled: true,
                    fillColor: AppColors.bgDark2.withOpacity(0.35),
                    contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(999),
                      borderSide: const BorderSide(color: AppColors.borderField, width: 1),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(999),
                      borderSide: const BorderSide(color: AppColors.accentCyan, width: 1.2),
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
                      border: Border.all(color: AppColors.borderField, width: 1),
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
                            myLocationButtonEnabled: !kIsWeb && _locationGranted,
                            zoomControlsEnabled: false,
                            mapToolbarEnabled: false,
                            polylines: _polylines,
                            scrollGesturesEnabled: !_lockMapGestures,
                            zoomGesturesEnabled: !_lockMapGestures,
                            rotateGesturesEnabled: !_lockMapGestures,
                            tiltGesturesEnabled: !_lockMapGestures,
                          ),

                          if (_isLoadingParkings || _isLoadingRoute || _isLocating)
                            Container(
                              color: Colors.black54,
                              child: const Center(
                                child: CircularProgressIndicator(color: AppColors.accentCyan),
                              ),
                            ),

                          Align(
                            alignment: Alignment.topRight,
                            child: Padding(
                              padding: const EdgeInsets.only(top: 14, right: 14),
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
                                    onPressed: _routes.isEmpty ? null : _showRouteChoiceSheet,
                                    tooltip: 'Percorso / Navigazione',
                                    selected: _showRoutePanel,
                                  ),
                                ],
                              ),
                            ),
                          ),

                          if (_showRoutePanel && _routes.isNotEmpty)
                            Positioned(
                              left: 16,
                              top: 16,
                              bottom: 16,
                              child: SafeArea(
                                child: ConstrainedBox(
                                  constraints: const BoxConstraints(maxWidth: 420),
                                  child: Material(
                                    color: Colors.transparent,
                                    child: Container(
                                      padding: const EdgeInsets.fromLTRB(16, 14, 16, 16),
                                      decoration: BoxDecoration(
                                        color: Colors.white,
                                        borderRadius: BorderRadius.circular(18),
                                        boxShadow: [
                                          BoxShadow(
                                            color: Colors.black.withOpacity(0.18),
                                            blurRadius: 24,
                                            offset: const Offset(0, 10),
                                          ),
                                        ],
                                      ),
                                      // ✅ widget esterno
                                      child: RoutePanel(
                                        routes: _routes
                                            .map((r) => RoutePanelRoute(
                                                  summary: r.summary,
                                                  steps: r.steps,
                                                ))
                                            .toList(),
                                        selectedIndex: _selectedRouteIndex,
                                        onSelectIndex: (i) {
                                          setState(() => _selectedRouteIndex = i);
                                          _applySelectedRoute();
                                        },
                                        onClose: _closeRoutePanel,
                                        onStart: _startExternalNavigation,
                                        stripHtml: _stripHtml,
                                        setMapGesturesLocked: _setMapGesturesLocked,
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
                              // ✅ widget esterno
                              child: ParkingPopup(
                                parking: _selectedParkingData!,
                                onClose: () => setState(() {
                                  _selectedParkingData = null;
                                  _selectedParkingMarkerId = null;
                                }),
                                onBook: () {
                                  final p = _selectedParkingData!;
                                  final pId = p['id'].toString();
                                  final destLat = (p['latitudine'] as num).toDouble();
                                  final destLng = (p['longitudine'] as num).toDouble();

                                  setState(() => _selectedParkingData = null);
                                  _effettuaPrenotazione(pId, destLat: destLat, destLng: destLng);
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

class _DirectionsRoute {
  final List<LatLng> points;
  final String summary;
  final List<dynamic> steps;

  _DirectionsRoute({
    required this.points,
    required this.summary,
    required this.steps,
  });
}
