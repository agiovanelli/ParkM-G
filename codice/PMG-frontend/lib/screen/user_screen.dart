import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';
import 'package:http/http.dart' as http;

import 'package:park_mg/utils/theme.dart';
import '../api/api_client.dart';
import '../models/utente.dart';

class UserScreen extends StatefulWidget {
  final Utente utente;
  final ApiClient apiClient;

  const UserScreen({
    super.key,
    required this.utente,
    required this.apiClient,
  });

  @override
  State<UserScreen> createState() => _UserScreenState();
}

class _UserScreenState extends State<UserScreen> {
  final _searchController = TextEditingController();
  final GlobalKey _gearKey = GlobalKey();

  GoogleMapController? _mapController;
  final Set<Marker> _markers = {};

  // Metti la tua posizione di default (qui Bergamo)
  static const CameraPosition _initialCamera = CameraPosition(
    target: LatLng(45.6983, 9.6773),
    zoom: 12,
  );

  // Per la SEARCH (Geocoding API) ti conviene usare --dart-define per non hardcodare
  static const String _geocodingKey =
      String.fromEnvironment('GOOGLE_GEOCODING_KEY', defaultValue: 'AIzaSyCRAbggpHBwIhmP8iNExxc98UBkrDo_OGY');

  @override
  void initState() {
    super.initState();

    if (widget.utente.preferenze == null || widget.utente.preferenze!.isEmpty) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        _showPreferenzeDialog();
      });
    }
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _showPreferenzeDialog() async {
    final updatedPrefs = await showDialog<Map<String, String>>(
      context: context,
      barrierDismissible: false,
      builder: (_) => PreferenzeDialog(
        utente: widget.utente,
        apiClient: widget.apiClient,
      ),
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
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
          content: Text(msg, style: const TextStyle(color: AppColors.textPrimary)),
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
                style: TextStyle(color: AppColors.textPrimary, fontWeight: FontWeight.w600),
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
                style: TextStyle(color: AppColors.textPrimary, fontWeight: FontWeight.w600),
              ),
            ],
          ),
        ),
      ],
    );

    if (!mounted) return;

    if (selected == 'prefs') {
      _showPreferenzeDialog();
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
      final uri = Uri.https(
        'maps.googleapis.com',
        '/maps/api/geocode/json',
        {'address': q, 'key': _geocodingKey},
      );

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

      setState(() {
        _markers
          ..removeWhere((m) => m.markerId.value == 'search')
          ..add(
            Marker(
              markerId: const MarkerId('search'),
              position: target,
              infoWindow: InfoWindow(title: q),
            ),
          );
      });
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
                            border: Border.all(color: AppColors.borderField, width: 1),
                          ),
                          child: const Icon(Icons.settings, color: AppColors.textPrimary, size: 18),
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

                // MAP AREA
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
                      child: GoogleMap(
                        initialCameraPosition: _initialCamera,
                        onMapCreated: (c) => _mapController = c,
                        markers: _markers,
                        myLocationButtonEnabled: false,
                        zoomControlsEnabled: false,
                        mapToolbarEnabled: false,
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
            e is ApiException ? e.message : 'Errore nel salvataggio delle preferenze',
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
        style: TextStyle(color: AppColors.textPrimary, fontWeight: FontWeight.w800),
      ),
      content: SingleChildScrollView(
        child: Theme(
          // Colori coerenti per radio/checkbox/slider/dropdown
          data: Theme.of(context).copyWith(
            radioTheme: RadioThemeData(
              fillColor: WidgetStateProperty.all(AppColors.accentCyan),
            ),
            checkboxTheme: CheckboxThemeData(
              fillColor: WidgetStateProperty.resolveWith((states) {
                if (states.contains(WidgetState.selected)) return AppColors.accentCyan;
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
              valueIndicatorTextStyle: const TextStyle(color: AppColors.textPrimary),
            ),
          ),
          child: DefaultTextStyle(
            style: const TextStyle(color: AppColors.textPrimary),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text('Età', style: TextStyle(fontWeight: FontWeight.w700)),
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

                const Text('Piano', style: TextStyle(fontWeight: FontWeight.w700)),
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

                const Text('Distanza massima (m)', style: TextStyle(fontWeight: FontWeight.w700)),
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
                  style: TextStyle(color: AppColors.textMuted.withOpacity(0.95)),
                ),
                const SizedBox(height: 10),

                const Text('Condizioni speciali', style: TextStyle(fontWeight: FontWeight.w700)),
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

                const Text('Occupazione', style: TextStyle(fontWeight: FontWeight.w700)),
                DropdownButtonFormField<String>(
                  value: _occupazione,
                  dropdownColor: AppColors.bgDark,
                  decoration: InputDecoration(
                    filled: true,
                    fillColor: AppColors.bgDark2.withOpacity(0.35),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: const BorderSide(color: AppColors.borderField),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: const BorderSide(color: AppColors.accentCyan),
                    ),
                  ),
                  items: _occupazioni
                      .map((o) => DropdownMenuItem<String>(value: o, child: Text(o)))
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
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          ),
          child: _isSaving
              ? const SizedBox(
                  width: 16,
                  height: 16,
                  child: CircularProgressIndicator(strokeWidth: 2, color: AppColors.textPrimary),
                )
              : const Text('Salva', style: TextStyle(fontWeight: FontWeight.w800)),
        ),
      ],
    );
  }
}
