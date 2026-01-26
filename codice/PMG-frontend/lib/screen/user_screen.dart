import 'dart:convert';
import 'dart:ui' as ui;
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_polyline_points/flutter_polyline_points.dart';
import 'package:geolocator/geolocator.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:http/http.dart' as http;

import 'package:park_mg/utils/theme.dart';
import '../api/api_client.dart';
import '../models/utente.dart';
import '../widgets/prenotazione_dialog.dart';
import 'history_screen.dart';

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
  final Set<Circle> _circles = {};
  bool _locationGranted = false;
  late AnimationController _pulseController;
  late Animation<double> _pulseAnimation;

  GoogleMapController? _mapController;
  final Set<Marker> _markers = {};

  LatLng _cameraTarget = _initialCamera.target;
  bool _showParkings = false;
  bool _isLoadingParkings = false;

  Map<String, dynamic>? _selectedParkingData;

  List<_DirectionsRoute> _routes = [];
  int _selectedRouteIndex = 0;

  final Set<Polyline> _polylines = {};
  bool _isLoadingRoute = false;
  String? _routeSummary;

  bool _isLocating = false;
  LatLng? _pendingCenter;

  static const Color _baseBlue = Color(0xFF4285F4);

  late final Color _selectedGreen = () {
    final hsl = HSLColor.fromColor(_baseBlue);
    return hsl.withHue(120).toColor();
  }();

  BitmapDescriptor? _parkingIcon;
  BitmapDescriptor? _parkingIconSelected;
  String? _selectedParkingMarkerId;

  static const String _baseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://localhost:8080/api',
  );

  static const CameraPosition _initialCamera = CameraPosition(
    target: LatLng(41.9028, 12.4964),
    zoom: 6,
  );

  static const String _geocodingKey = String.fromEnvironment(
    'GOOGLE_GEOCODING_KEY',
    defaultValue: 'AIzaSyCRAbggpHBwIhmP8iNExxc98UBkrDo_OGY',
  );

  void _checkPolylineChars(String poly) {
    for (int i = 0; i < poly.length; i++) {
      final c = poly.codeUnitAt(i);
      if (c < 63 || c > 126) {
        debugPrint('BAD CHAR in polyline at i=$i code=$c char="${poly[i]}"');
        // opzionale: stampa un intorno
        final start = (i - 10).clamp(0, poly.length);
        final end = (i + 10).clamp(0, poly.length);
        debugPrint('CONTEXT: "${poly.substring(start, end)}"');
        return;
      }
    }
    debugPrint('Polyline chars OK (all in 63..126)');
  }

  String _stripHtml(String input) {
    // rimuove tag HTML e decodifica le entità più comuni
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

  void _showStepsSheet() {
    if (_routes.isEmpty) return;
    final steps = _routes[_selectedRouteIndex].steps;

    showModalBottomSheet(
      context: context,
      backgroundColor: AppColors.bgDark,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(18)),
      ),
      builder: (_) => ListView.separated(
        padding: const EdgeInsets.all(16),
        itemCount: steps.length,
        separatorBuilder: (_, __) =>
            Divider(color: AppColors.borderField.withOpacity(0.6)),
        itemBuilder: (_, i) {
          final s = steps[i] as Map<String, dynamic>;
          final html = (s['htmlInstructions'] ?? '') as String;
          final dist = (s['distanceText'] ?? '') as String;
          final dur = (s['durationText'] ?? '') as String;

          return ListTile(
            title: Text(
              _stripHtml(html),
              style: const TextStyle(color: AppColors.textPrimary),
            ),
            subtitle: Text(
              '$dist • $dur',
              style: const TextStyle(color: AppColors.textMuted),
            ),
          );
        },
      ),
    );
  }

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

    WidgetsBinding.instance.addPostFrameCallback((_) {
      _bootstrapMyLocation();
    });

    if (widget.utente.preferenze == null || widget.utente.preferenze!.isEmpty) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        _showPreferenzeDialog();
      });
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
    _searchController.dispose();
    _pulseController.dispose();
    super.dispose();
  }

  Future<void> _bootstrapMyLocation() async {
    if (_isLocating) return;

    setState(() => _isLocating = true);

    try {
      // Nota: su Web spesso serve un gesto utente per far comparire il prompt.
      // Qui ci proviamo comunque: se il browser lo blocca, mostriamo un toast.
      final serviceEnabled = await Geolocator.isLocationServiceEnabled();
      if (!serviceEnabled) {
        _showToast('Servizi di localizzazione disattivati.');
        return;
      }

      LocationPermission perm = await Geolocator.checkPermission();
      if (perm == LocationPermission.denied) {
        perm = await Geolocator.requestPermission();
      }

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

        // Cerchi: pulse + dot (attenzione all’ordine: dot deve essere l’ultimo)
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

      // centra la camera: se la mappa non è pronta, lo facciamo dopo in onMapCreated
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

    if (route.points.isNotEmpty) {
      _fitCameraToPoints(route.points);
    }
  }

  Future<void> _showPreferenzeDialog() async {
    final updatedPrefs = await showDialog<Map<String, String>>(
      context: context,
      barrierDismissible: false,
      builder: (_) =>
          PreferenzeDialog(utente: widget.utente, apiClient: widget.apiClient),
    );

    if (updatedPrefs != null) {
      setState(() {
        widget.utente.preferenze = updatedPrefs;
      });
    }
  }

  void _logout() {
    Navigator.of(context).pop();
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
        // STORICO
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

    if (_geocodingKey.isEmpty) {
      _showToast('Manca GOOGLE_GEOCODING_KEY (usa --dart-define).');
      return;
    }

    try {
      final uri = Uri.https('maps.googleapis.com', '/maps/api/geocode/json', {
        'address': q,
        'key': _geocodingKey,
      });

      final res = await http.get(uri);
      if (res.statusCode != 200) {
        _showToast('Errore Geocoding (${res.statusCode}).');
        return;
      }

      final data = jsonDecode(res.body) as Map<String, dynamic>;
      final status = (data['status'] ?? '') as String;

      if (status != 'OK') {
        final err = (data['error_message'] ?? 'Nessun risultato') as String;
        _showToast(err);
        return;
      }

      final results = (data['results'] as List).cast<Map<String, dynamic>>();
      if (results.isEmpty) {
        _showToast('Nessun risultato trovato.');
        return;
      }

      final loc = results.first['geometry']['location'] as Map<String, dynamic>;
      final lat = (loc['lat'] as num).toDouble();
      final lng = (loc['lng'] as num).toDouble();
      final target = LatLng(lat, lng);

      _mapController?.animateCamera(
        CameraUpdate.newCameraPosition(
          CameraPosition(target: target, zoom: 15),
        ),
      );

      // Non aggiungo marker, solo sposto la camera
      _mapController?.animateCamera(
        CameraUpdate.newCameraPosition(
          CameraPosition(target: target, zoom: 15),
        ),
      );
    } catch (_) {
      _showToast('Errore durante la ricerca.');
    }
  }

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

                // SEARCH BAR
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

                // MAP AREA
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
                          ),
                          if (_routeSummary != null)
                            Positioned(
                              top: 14,
                              left: 16,
                              right: 16,
                              child: Center(
                                child: GestureDetector(
                                  onTap: _showStepsSheet, // <-- aggiunto
                                  child: Container(
                                    padding: const EdgeInsets.symmetric(
                                      horizontal: 12,
                                      vertical: 10,
                                    ),
                                    decoration: BoxDecoration(
                                      color: Colors.black.withOpacity(0.70),
                                      borderRadius: BorderRadius.circular(999),
                                      border: Border.all(
                                        color: AppColors.accentCyan.withOpacity(
                                          0.9,
                                        ),
                                      ),
                                    ),
                                    child: Row(
                                      mainAxisSize: MainAxisSize.min,
                                      children: [
                                        const Icon(
                                          Icons.directions_car,
                                          size: 18,
                                          color: Colors.white,
                                        ),
                                        const SizedBox(width: 8),
                                        Text(
                                          _routeSummary!,
                                          style: const TextStyle(
                                            color: Colors.white,
                                            fontWeight: FontWeight.w700,
                                          ),
                                        ),
                                        const SizedBox(width: 8),
                                        const Icon(
                                          Icons.list_alt,
                                          size: 18,
                                          color: Colors.white,
                                        ),
                                      ],
                                    ),
                                  ),
                                ),
                              ),
                            ),

                          if (_routes.length > 1)
                            Positioned(
                              top: 60,
                              left: 16,
                              right: 16,
                              child: Wrap(
                                spacing: 8,
                                children: List.generate(_routes.length, (i) {
                                  final selected = i == _selectedRouteIndex;
                                  return ChoiceChip(
                                    label: Text('Route ${i + 1}'),
                                    selected: selected,
                                    onSelected: (_) {
                                      setState(() => _selectedRouteIndex = i);
                                      _applySelectedRoute();
                                    },
                                  );
                                }),
                              ),
                            ),

                          if (_isLoadingParkings)
                            Container(
                              color: Colors.black54,
                              child: const Center(
                                child: CircularProgressIndicator(
                                  color: AppColors.accentCyan,
                                ),
                              ),
                            ),
                          if (_isLoadingRoute)
                            Container(
                              color: Colors.black54,
                              child: const Center(
                                child: CircularProgressIndicator(
                                  color: AppColors.accentCyan,
                                ),
                              ),
                            ),
                          if (_isLocating)
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
                                ],
                              ),
                            ),
                          ),
                          if (_selectedParkingData != null)
                            Positioned(
                              bottom: 40,
                              left: 20,
                              right: 20,
                              child: _buildParkingPopup(_selectedParkingData!),
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

  Widget _buildParkingPopup(Map<String, dynamic> p) {
    return Container(
      key: const ValueKey('popup'),
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.bgDark,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.accentCyan, width: 1.2),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.4),
            blurRadius: 18,
            offset: const Offset(0, 6),
          ),
        ],
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                p['nome'] ?? 'Parcheggio',
                style: const TextStyle(
                  color: AppColors.textPrimary,
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                ),
              ),
              IconButton(
                icon: const Icon(Icons.close, color: AppColors.textPrimary),
                onPressed: () => setState(() {
                  _selectedParkingData = null;
                  _selectedParkingMarkerId = null;
                }),
              ),
            ],
          ),
          const SizedBox(height: 4),
          Text(
            'Area: ${p['area'] ?? 'N/D'}',
            style: const TextStyle(color: AppColors.textMuted),
          ),
          Text(
            'Posti disponibili: ${p['postiDisponibili']}/${p['postiTotali']}',
            style: const TextStyle(color: AppColors.textMuted),
          ),
          const SizedBox(height: 8),
          ElevatedButton.icon(
            onPressed: () {
              final pId = p['id'].toString();
              final destLat = (p['latitudine'] as num).toDouble();
              final destLng = (p['longitudine'] as num).toDouble();

              setState(() => _selectedParkingData = null);

              _effettuaPrenotazione(pId, destLat: destLat, destLng: destLng);
            },

            icon: const Icon(Icons.qr_code),
            label: const Text('Prenota parcheggio'),
            style: ElevatedButton.styleFrom(
              backgroundColor: AppColors.accentCyan,
              foregroundColor: AppColors.textPrimary,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(12),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _toggleParkings() async {
    setState(() => _showParkings = !_showParkings);

    if (!_showParkings) {
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
        '$_baseUrl/parcheggi/nearby'
        '?lat=${center.latitude}&lng=${center.longitude}&radius=$radiusMeters',
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
                ? (_parkingIconSelected ??
                      _parkingIcon ??
                      BitmapDescriptor.defaultMarker)
                : (_parkingIcon ?? BitmapDescriptor.defaultMarker),

            // se vuoi eliminare del tutto la “finestrina” bianca nativa
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
    } catch (e) {
      _showToast('Errore durante il caricamento dei parcheggi.');
    } finally {
      setState(() => _isLoadingParkings = false);
    }
  }

  Future<void> _showRouteToParking({
    required double destLat,
    required double destLng,
  }) async {
    setState(() {
      _isLoadingRoute = true;
      _routeSummary = null;
    });

    try {
      // Permessi + posizione attuale
      LocationPermission perm = await Geolocator.checkPermission();
      if (perm == LocationPermission.denied) {
        perm = await Geolocator.requestPermission();
      }
      if (perm == LocationPermission.denied ||
          perm == LocationPermission.deniedForever) {
        _showToast(
          'Permesso posizione negato: impossibile calcolare il percorso.',
        );
        return;
      }

      final pos = await Geolocator.getCurrentPosition(
        desiredAccuracy: LocationAccuracy.high,
      );

      final origin = LatLng(pos.latitude, pos.longitude);
      final destination = LatLng(destLat, destLng);

      final routes = await _fetchDirectionsRoutes(
        origin: origin,
        destination: destination,
      );

      setState(() {
        _routes = routes;
        _selectedRouteIndex = 0;
      });

      _applySelectedRoute();
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
    if (routesJson.isEmpty)
      throw Exception('Nessun percorso dalla Directions API');

    return routesJson.map((r) {
      final poly = (r['polyline'] ?? '') as String;
      if (poly.isEmpty) throw Exception('Polyline vuota dal backend');

      _checkPolylineChars(poly);

      final decoded = PolylinePoints.decodePolyline(poly);
      final points = decoded
          .map((p) => LatLng(p.latitude, p.longitude))
          .toList();

      final dist = (r['distanceText'] ?? '') as String;
      final durTraffic = (r['durationInTrafficText'] ?? '') as String;
      final dur = (r['durationText'] ?? '') as String;

      // Preferisci traffico se presente
      final summary = (durTraffic.isNotEmpty ? durTraffic : dur);
      final summaryFull = (summary.isNotEmpty && dist.isNotEmpty)
          ? '$summary • $dist'
          : ((r['summary'] ?? 'Percorso pronto') as String);

      final steps = (r['steps'] ?? []) as List<dynamic>;

      return _DirectionsRoute(
        points: points,
        summary: summaryFull,
        steps: steps,
      );
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

  Future<void> _effettuaPrenotazione(
    String parcheggioId, {
    required double destLat,
    required double destLng,
  }) async {
    // 0) prendi posizione subito
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

      // 1) disegna route se ho origin
      if (origin != null) {
        await _showRouteToParking(destLat: destLat, destLng: destLng);
        // oppure crea una versione che usa "origin" senza richiamare geolocator
      }

      if (!mounted) return;
      Navigator.of(context).pop(); // chiude loader

      PrenotazioneDialog.mostra(context, risposta);
    } on ApiException catch (e) {
      if (!mounted) return;
      Navigator.of(context).pop();
      _showToast(e.message);
    } catch (_) {
      if (!mounted) return;
      Navigator.of(context).pop();
      _showToast("Errore di connessione o del server");
    }

    _loadParkingsNearby(_cameraTarget);
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
}

class PreferenzeDialog extends StatefulWidget {
  final Utente utente;
  final ApiClient apiClient;

  const PreferenzeDialog({
    super.key,
    required this.utente,
    required this.apiClient,
  });

  @override
  State<PreferenzeDialog> createState() => _PreferenzeDialogState();
}

class _PreferenzeDialogState extends State<PreferenzeDialog> {
  String _eta = 'under30';
  String _piano = 'piano_terra';
  double _distanza = 30;
  bool _disabile = false;
  bool _donnaIncinta = false;
  String _occupazione = 'Studente';

  bool _isSaving = false;

  final List<String> _occupazioni = const [
    'Studente',
    'Lavoratore dipendente',
    'Libero professionista',
  ];

  @override
  void initState() {
    super.initState();

    final prefs = widget.utente.preferenze;
    if (prefs != null && prefs.isNotEmpty) {
      _eta = prefs['età'] ?? _eta;
      _piano = prefs['piano'] ?? _piano;
      _distanza = double.tryParse(prefs['distanza'] ?? '') ?? _distanza;
      _disabile = (prefs['disabile'] ?? 'No') == 'Sì';
      _donnaIncinta = (prefs['donnaIncinta'] ?? 'No') == 'Sì';
      _occupazione = prefs['occupazione'] ?? _occupazione;
      if (!_occupazioni.contains(_occupazione)) _occupazione = 'Studente';
    }
  }

  Future<void> _onSalva() async {
    final prefs = <String, String>{
      'età': _eta,
      'piano': _piano,
      'distanza': _distanza.toStringAsFixed(2),
      'disabile': _disabile ? 'Sì' : 'No',
      'donnaIncinta': _donnaIncinta ? 'Sì' : 'No',
      'occupazione': _occupazione,
    };

    setState(() => _isSaving = true);
    try {
      await widget.apiClient.aggiornaPreferenze(widget.utente.id, prefs);
      if (!mounted) return;
      Navigator.of(context).pop(prefs);
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          behavior: SnackBarBehavior.floating,
          backgroundColor: AppColors.bgDark,
          content: Text(
            e is ApiException
                ? e.message
                : 'Errore nel salvataggio delle preferenze',
            style: const TextStyle(color: AppColors.textPrimary),
          ),
        ),
      );
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  void _onAnnulla() => Navigator.of(context).pop();

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      backgroundColor: AppColors.bgDark,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
      title: const Text(
        'Imposta preferenze',
        style: TextStyle(
          color: AppColors.textPrimary,
          fontWeight: FontWeight.w800,
        ),
      ),
      content: SingleChildScrollView(
        child: Theme(
          data: Theme.of(context).copyWith(
            radioTheme: RadioThemeData(
              fillColor: WidgetStateProperty.all(AppColors.accentCyan),
            ),
            checkboxTheme: CheckboxThemeData(
              fillColor: WidgetStateProperty.resolveWith((states) {
                if (states.contains(WidgetState.selected))
                  return AppColors.accentCyan;
                return AppColors.borderField;
              }),
              checkColor: WidgetStateProperty.all(AppColors.textPrimary),
            ),
            sliderTheme: Theme.of(context).sliderTheme.copyWith(
              activeTrackColor: AppColors.accentCyan,
              thumbColor: AppColors.accentCyan,
              overlayColor: AppColors.accentCyan.withOpacity(0.15),
              inactiveTrackColor: AppColors.borderField,
              valueIndicatorColor: AppColors.brandTop,
              valueIndicatorTextStyle: const TextStyle(
                color: AppColors.textPrimary,
              ),
            ),
          ),
          child: DefaultTextStyle(
            style: const TextStyle(color: AppColors.textPrimary),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Età',
                  style: TextStyle(fontWeight: FontWeight.w700),
                ),
                Row(
                  children: [
                    Expanded(
                      child: RadioListTile<String>(
                        title: const Text('Under 30'),
                        value: 'under30',
                        groupValue: _eta,
                        onChanged: (v) => setState(() => _eta = v!),
                        dense: true,
                        contentPadding: EdgeInsets.zero,
                      ),
                    ),
                    Expanded(
                      child: RadioListTile<String>(
                        title: const Text('Over 60'),
                        value: 'over60',
                        groupValue: _eta,
                        onChanged: (v) => setState(() => _eta = v!),
                        dense: true,
                        contentPadding: EdgeInsets.zero,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 10),

                const Text(
                  'Piano',
                  style: TextStyle(fontWeight: FontWeight.w700),
                ),
                Row(
                  children: [
                    Expanded(
                      child: RadioListTile<String>(
                        title: const Text('Piano terra'),
                        value: 'piano_terra',
                        groupValue: _piano,
                        onChanged: (v) => setState(() => _piano = v!),
                        dense: true,
                        contentPadding: EdgeInsets.zero,
                      ),
                    ),
                    Expanded(
                      child: RadioListTile<String>(
                        title: const Text('Altri piani'),
                        value: 'altri_piani',
                        groupValue: _piano,
                        onChanged: (v) => setState(() => _piano = v!),
                        dense: true,
                        contentPadding: EdgeInsets.zero,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 10),

                const Text(
                  'Distanza massima (m)',
                  style: TextStyle(fontWeight: FontWeight.w700),
                ),
                Slider(
                  value: _distanza,
                  min: 0,
                  max: 200,
                  divisions: 40,
                  label: _distanza.toStringAsFixed(0),
                  onChanged: (v) => setState(() => _distanza = v),
                ),
                Text(
                  'Valore: ${_distanza.toStringAsFixed(0)} m',
                  style: TextStyle(
                    color: AppColors.textMuted.withOpacity(0.95),
                  ),
                ),
                const SizedBox(height: 10),

                const Text(
                  'Condizioni speciali',
                  style: TextStyle(fontWeight: FontWeight.w700),
                ),
                CheckboxListTile(
                  value: _disabile,
                  onChanged: (v) => setState(() => _disabile = v ?? false),
                  title: const Text('Disabile'),
                  dense: true,
                  controlAffinity: ListTileControlAffinity.leading,
                  contentPadding: EdgeInsets.zero,
                ),
                CheckboxListTile(
                  value: _donnaIncinta,
                  onChanged: (v) => setState(() => _donnaIncinta = v ?? false),
                  title: const Text('Donna incinta'),
                  dense: true,
                  controlAffinity: ListTileControlAffinity.leading,
                  contentPadding: EdgeInsets.zero,
                ),
                const SizedBox(height: 10),

                const Text(
                  'Occupazione',
                  style: TextStyle(fontWeight: FontWeight.w700),
                ),
                DropdownButtonFormField<String>(
                  value: _occupazione,
                  dropdownColor: AppColors.bgDark,
                  decoration: InputDecoration(
                    filled: true,
                    fillColor: AppColors.bgDark2.withOpacity(0.35),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: const BorderSide(
                        color: AppColors.borderField,
                      ),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: const BorderSide(color: AppColors.accentCyan),
                    ),
                  ),
                  items: _occupazioni
                      .map(
                        (o) =>
                            DropdownMenuItem<String>(value: o, child: Text(o)),
                      )
                      .toList(),
                  onChanged: (v) {
                    if (v != null) setState(() => _occupazione = v);
                  },
                ),
              ],
            ),
          ),
        ),
      ),
      actionsPadding: const EdgeInsets.fromLTRB(16, 0, 16, 14),
      actions: [
        TextButton(
          onPressed: _isSaving ? null : _onAnnulla,
          style: TextButton.styleFrom(foregroundColor: AppColors.textMuted),
          child: const Text('Annulla'),
        ),
        ElevatedButton(
          onPressed: _isSaving ? null : _onSalva,
          style: ElevatedButton.styleFrom(
            backgroundColor: AppColors.accentCyan,
            foregroundColor: AppColors.textPrimary,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(12),
            ),
          ),
          child: _isSaving
              ? const SizedBox(
                  width: 16,
                  height: 16,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: AppColors.textPrimary,
                  ),
                )
              : const Text(
                  'Salva',
                  style: TextStyle(fontWeight: FontWeight.w800),
                ),
        ),
      ],
    );
  }
}

class GMapsControlButton extends StatelessWidget {
  final VoidCallback? onPressed;
  final IconData icon;
  final String? tooltip;
  final bool selected;

  const GMapsControlButton({
    super.key,
    required this.icon,
    required this.onPressed,
    this.tooltip,
    this.selected = false,
  });

  @override
  Widget build(BuildContext context) {
    final Color iconColor = onPressed == null
        ? const Color(0xFFB0B0B0)
        : (selected ? const Color(0xFF4285F4) : const Color(0xFF666666));

    final child = Material(
      color: Colors.white,
      elevation: 2.5,
      shadowColor: Colors.black.withOpacity(0.22),
      shape: CircleBorder(
        side: BorderSide(
          color: selected ? const Color(0x334285F4) : const Color(0x1F000000),
          width: 1,
        ),
      ),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onPressed,
        child: SizedBox(
          width: 44,
          height: 44,
          child: Icon(icon, size: 22, color: iconColor),
        ),
      ),
    );

    if (tooltip == null) return child;
    return Tooltip(message: tooltip!, child: child);
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
