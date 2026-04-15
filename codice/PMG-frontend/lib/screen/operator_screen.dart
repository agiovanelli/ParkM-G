import 'dart:async';
import 'package:flutter/material.dart';
import 'package:park_mg/models/posto.dart';
import 'package:park_mg/screen/home_page.dart';
import 'package:park_mg/utils/theme.dart';
import 'package:park_mg/utils/ui_feedback.dart';
import 'package:park_mg/widgets/operator_parking_image_map.dart';
import '../models/operatore.dart';
import 'qr_scanner_screen.dart';
import 'package:park_mg/api/api_client.dart';
import '../models/prenotazione.dart';

enum LogCategory { allarme, evento, history }

enum LogSeverity {
  critico,
  attenzione,
  controllo,
  pagamento,
  veicolo,
  info,
  risolto,
}

class ParkingLogItem {
  final DateTime timestamp;
  LogCategory category;
  LogSeverity severity;
  final String id;
  final String title;
  final String details;
  final String? source;

  ParkingLogItem({
    required this.id,
    required this.timestamp,
    required this.category,
    required this.severity,
    required this.title,
    required this.details,
    this.source,
  });

  factory ParkingLogItem.fromJson(Map<String, dynamic> json) {
    final DateTime timestamp = DateTime.parse(json['data']);
    final DateTime now = DateTime.now();
    final rawTipo = json['tipo'];
    LogCategory category = LogCategory.history;
    if (rawTipo is String && rawTipo.isNotEmpty) {
      category = LogCategory.values.firstWhere(
        (e) => e.name.toLowerCase() == rawTipo.toLowerCase(),
        orElse: () => LogCategory.history,
      );
    }

    if (category == LogCategory.evento &&
        now.difference(timestamp).inHours >= 24) {
      category = LogCategory.history;
    }

    final rawSeverita = (json['severità'] ?? json['severita']) as String?;
    final severity = rawSeverita != null
        ? LogSeverity.values.firstWhere(
            (e) => e.name.toLowerCase() == rawSeverita.toLowerCase(),
            orElse: () => LogSeverity.info,
          )
        : LogSeverity.info;

    return ParkingLogItem(
      id: json['id'] as String,
      timestamp: timestamp,
      category: category,
      severity: severity,
      title: json['titolo'] ?? '—',
      details: json['descrizione'] ?? '—',
    );
  }
}

class ParkingStats {
  final int totalSpots;
  final int availableSpots;
  final int activeReservations;
  final int inactiveReservations;

  const ParkingStats({
    required this.totalSpots,
    required this.availableSpots,
    required this.activeReservations,
    required this.inactiveReservations,
  });

  int get occupiedSpots => (totalSpots - availableSpots).clamp(0, totalSpots);
  double get occupancyRatio => totalSpots == 0 ? 0 : occupiedSpots / totalSpots;
  int get occupancyPercent => (occupancyRatio * 100).round();
}

class OperatorScreen extends StatefulWidget {
  final Operatore operatore;

  const OperatorScreen({super.key, required this.operatore});

  @override
  State<OperatorScreen> createState() => _OperatorScreenState();
}

class _OperatorScreenState extends State<OperatorScreen> {
  final _scaffoldKey = GlobalKey<ScaffoldState>();
  late ApiClient _apiClient;
  bool _isProcessing = false;
  int _pageIndex = 0;
  String? _selectedSpotId;
  int _selectedFloor = 1;
  LogCategory _selectedCategory = LogCategory.allarme;
  LogSeverity? _severityFilter;
  final _searchController = TextEditingController();
  bool _isRefreshing = false;
  late List<ParkingLogItem> _items;
  List<Posto> _realSpots = [];
  bool _isLoadingSpots = false;
  late ParkingStats _stats;
  Timer? _autoRefreshTimer;
  DateTime? _lastFetchAt;
  String? _analiticaId;

  @override
  void initState() {
    super.initState();
    _apiClient = ApiClient();
    _items = [];
    _loadInitialData();
    _stats = const ParkingStats(
      totalSpots: 0,
      availableSpots: 0,
      activeReservations: 0,
      inactiveReservations: 0,
    );
    _lastFetchAt = DateTime.now();
    _loadParkingStats();
    _loadParkingSpots();

    _autoRefreshTimer = Timer.periodic(const Duration(minutes: 5), (_) {
      if (!mounted) return;
      if (_isRefreshing) return;
      _refresh();
    });
  }

  @override
  void dispose() {
    _autoRefreshTimer?.cancel();
    _searchController.dispose();
    super.dispose();
  }

  Future<void> _loadParkingSpots() async {
    if (!mounted) return;

    setState(() => _isLoadingSpots = true);

    try {
      final spots = await _apiClient.getPostiParcheggio(
        widget.operatore.parcheggioId,
        piano: _selectedFloor,
      );

      if (!mounted) return;

      setState(() {
        _realSpots = spots;
      });
      debugPrint(
        'spots loaded floor=$_selectedFloor -> ${spots.map((s) => '${s.numero}:${s.disponibile}:${s.disabilitato}').join(', ')}',
      );
    } catch (e) {
      UiFeedback.showError(context, 'Errore caricamento posti: $e');
    } finally {
      if (mounted) {
        setState(() => _isLoadingSpots = false);
      }
    }
  }

  Future<void> _resolveAlarm(ParkingLogItem it) async {
    try {
      await _apiClient.updateLogSeverity(
        it.id,
        LogSeverity.risolto.name.toUpperCase(),
      );
      await _apiClient.updateLogCategory(
        it.id,
        LogCategory.history.name.toUpperCase(),
      );

      if (!mounted) return;

      setState(() {
        it.severity = LogSeverity.risolto;
        it.category = LogCategory.history;
      });

      UiFeedback.showSuccess(context, 'Allarme spostato nello storico');
    } catch (e) {
      debugPrint('Errore risoluzione allarme: $e');
      UiFeedback.showError(context, 'Errore aggiornamento allarme: $e');
    }
  }

  Future<void> _loadInitialData() async {
    await _loadAnaliticaId();
    await _loadLogs();
  }

  Future<void> _loadAnaliticaId() async {
    try {
      final analitica = await _apiClient.getAnaliticaByParcheggioId(
        widget.operatore.parcheggioId,
      );

      if (!mounted) return;

      setState(() {
        _analiticaId = analitica['id'] as String?;
      });
    } catch (e) {
      debugPrint('Errore caricamento analiticaId: $e');
      UiFeedback.showError(
        context,
        'Errore caricamento analitica del parcheggio',
      );
    }
  }

  Future<void> _showCreateLogDialog() async {
    if (_analiticaId == null || _analiticaId!.isEmpty) {
      UiFeedback.showError(
        context,
        'Analitica non disponibile per questo parcheggio',
      );
      return;
    }

    final titoloController = TextEditingController();
    final descrizioneController = TextEditingController();

    LogSeverity severita = LogSeverity.critico;
    bool isSaving = false;

    await showDialog<void>(
      context: context,
      barrierDismissible: !isSaving,
      builder: (dialogContext) {
        return StatefulBuilder(
          builder: (context, setDialogState) {
            final allowedSeverities = <LogSeverity>[
              LogSeverity.critico,
              LogSeverity.attenzione,
              LogSeverity.controllo,
            ];

            if (!allowedSeverities.contains(severita)) {
              severita = LogSeverity.critico;
            }

            return AlertDialog(
              backgroundColor: AppColors.bgDark,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(18),
              ),
              title: Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: AppColors.accentCyan.withValues(alpha: 0.12),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: const Icon(
                      Icons.note_add_rounded,
                      color: AppColors.accentCyan,
                      size: 26,
                    ),
                  ),
                  const SizedBox(width: 12),
                  const Expanded(
                    child: Text(
                      'Crea Allarme',
                      style: TextStyle(
                        color: AppColors.textPrimary,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                  ),
                ],
              ),
              content: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Severità',
                      style: TextStyle(
                        color: AppColors.textMuted,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(height: 8),
                    DropdownButtonFormField<LogSeverity>(
                      initialValue: severita,
                      dropdownColor: AppColors.bgDark,
                      style: const TextStyle(color: AppColors.textPrimary),
                      decoration: InputDecoration(
                        filled: true,
                        fillColor: AppColors.bgDark2,
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                          borderSide: const BorderSide(
                            color: AppColors.borderField,
                          ),
                        ),
                        enabledBorder: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                          borderSide: const BorderSide(
                            color: AppColors.borderField,
                          ),
                        ),
                        focusedBorder: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                          borderSide: const BorderSide(
                            color: AppColors.accentCyan,
                            width: 2,
                          ),
                        ),
                      ),
                      items: allowedSeverities.map((s) {
                        return DropdownMenuItem(
                          value: s,
                          child: Row(
                            children: [
                              Container(
                                width: 10,
                                height: 10,
                                decoration: BoxDecoration(
                                  color: _severityColor(s),
                                  borderRadius: BorderRadius.circular(99),
                                ),
                              ),
                              const SizedBox(width: 8),
                              Text(_severityLabel(s)),
                            ],
                          ),
                        );
                      }).toList(),
                      onChanged: isSaving
                          ? null
                          : (value) {
                              if (value == null) return;
                              setDialogState(() {
                                severita = value;
                              });
                            },
                    ),
                    const SizedBox(height: 14),
                    TextField(
                      controller: titoloController,
                      enabled: !isSaving,
                      style: const TextStyle(color: AppColors.textPrimary),
                      decoration: InputDecoration(
                        labelText: 'Titolo',
                        labelStyle: const TextStyle(color: AppColors.textMuted),
                        filled: true,
                        fillColor: AppColors.bgDark2,
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                          borderSide: const BorderSide(
                            color: AppColors.borderField,
                          ),
                        ),
                        enabledBorder: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                          borderSide: const BorderSide(
                            color: AppColors.borderField,
                          ),
                        ),
                        focusedBorder: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                          borderSide: const BorderSide(
                            color: AppColors.accentCyan,
                            width: 2,
                          ),
                        ),
                      ),
                    ),
                    const SizedBox(height: 14),
                    TextField(
                      controller: descrizioneController,
                      enabled: !isSaving,
                      maxLines: 4,
                      style: const TextStyle(color: AppColors.textPrimary),
                      decoration: InputDecoration(
                        labelText: 'Descrizione',
                        labelStyle: const TextStyle(color: AppColors.textMuted),
                        filled: true,
                        fillColor: AppColors.bgDark2,
                        border: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                          borderSide: const BorderSide(
                            color: AppColors.borderField,
                          ),
                        ),
                        enabledBorder: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                          borderSide: const BorderSide(
                            color: AppColors.borderField,
                          ),
                        ),
                        focusedBorder: OutlineInputBorder(
                          borderRadius: BorderRadius.circular(12),
                          borderSide: const BorderSide(
                            color: AppColors.accentCyan,
                            width: 2,
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              actions: [
                TextButton(
                  onPressed: isSaving
                      ? null
                      : () => Navigator.of(dialogContext).pop(),
                  style: TextButton.styleFrom(
                    foregroundColor: AppColors.textMuted,
                  ),
                  child: const Text('Annulla'),
                ),
                ElevatedButton.icon(
                  onPressed: isSaving
                      ? null
                      : () async {
                          final titolo = titoloController.text.trim();
                          final descrizione = descrizioneController.text.trim();

                          if (titolo.isEmpty) {
                            UiFeedback.showError(
                              context,
                              'Inserisci un titolo',
                            );
                            return;
                          }

                          if (descrizione.isEmpty) {
                            UiFeedback.showError(
                              context,
                              'Inserisci una descrizione',
                            );
                            return;
                          }

                          setDialogState(() => isSaving = true);

                          try {
                            await _apiClient.creaLog(
                              analiticaId: _analiticaId!,
                              tipo: LogCategory.allarme.name.toUpperCase(),
                              severita: severita.name.toUpperCase(),
                              titolo: titolo,
                              descrizione: descrizione,
                              data: DateTime.now(),
                            );

                            if (!context.mounted) return;

                            Navigator.of(dialogContext).pop();
                            UiFeedback.showSuccess(
                              context,
                              'Log creato correttamente',
                            );
                            await _refresh();
                          } catch (e) {
                            if (!context.mounted) return;
                            setDialogState(() => isSaving = false);
                            UiFeedback.showError(
                              context,
                              'Errore creazione log: $e',
                            );
                          }
                        },
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppColors.accentCyan,
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(12),
                    ),
                  ),
                  icon: isSaving
                      ? const SizedBox(
                          width: 16,
                          height: 16,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: Colors.white,
                          ),
                        )
                      : const Icon(Icons.save),
                  label: Text(isSaving ? 'Salvataggio...' : 'Crea Allarme'),
                ),
              ],
            );
          },
        );
      },
    );
  }

  Future<List<ParkingLogItem>> _fetchLogItems() async {
    if (_analiticaId == null || _analiticaId!.isEmpty) {
      throw Exception('Analitica non disponibile per questo parcheggio');
    }

    final jsonList = await _apiClient.getLogAnalitiche(_analiticaId!);

    return jsonList.map((json) => ParkingLogItem.fromJson(json)).toList()
      ..sort((a, b) => b.timestamp.compareTo(a.timestamp));
  }

  List<LogSeverity> _allowedSeverities() {
    switch (_selectedCategory) {
      case LogCategory.allarme:
        return [
          LogSeverity.critico,
          LogSeverity.attenzione,
          LogSeverity.controllo,
        ];

      case LogCategory.evento:
        return [LogSeverity.pagamento, LogSeverity.veicolo, LogSeverity.info];

      case LogCategory.history:
        return [
          LogSeverity.pagamento,
          LogSeverity.veicolo,
          LogSeverity.info,
          LogSeverity.risolto,
        ];
    }
  }

  Future<void> _loadParkingStats() async {
    try {
      final parcheggio = await _apiClient.getParcheggioById(
        widget.operatore.parcheggioId,
      );
      debugPrint('parcheggio raw: $parcheggio');

      final prenotazioni = await _apiClient.getPrenotazioniByParcheggio(
        widget.operatore.parcheggioId,
      );
      debugPrint('prenotazioni count: ${prenotazioni.length}');

      int activeReservations = 0;
      int inactiveReservations = 0;

      for (final p in prenotazioni) {
        switch (p.stato) {
          case StatoPrenotazione.attiva:
          case StatoPrenotazione.inCorso:
          case StatoPrenotazione.parcheggiato:
          case StatoPrenotazione.pagato:
            activeReservations++;
            break;

          case StatoPrenotazione.conclusa:
          case StatoPrenotazione.scaduta:
          case StatoPrenotazione.annullata:
            inactiveReservations++;
            break;
        }
      }

      if (!mounted) return;

      final allSpots = await _apiClient.getPostiParcheggio(
        widget.operatore.parcheggioId,
      );
      final totalSpots = allSpots.length;
      final availableSpots = allSpots
          .where((posto) => posto.disponibile && !posto.disabilitato)
          .length;

      if (!mounted) return;
      setState(() {
        _stats = ParkingStats(
          totalSpots: totalSpots,
          availableSpots: availableSpots,
          activeReservations: activeReservations,
          inactiveReservations: inactiveReservations,
        );
      });

      debugPrint(
        'stats loaded -> total: ${_stats.totalSpots}, '
        'available: ${_stats.availableSpots}, '
        'active: ${_stats.activeReservations}, '
        'inactive: ${_stats.inactiveReservations}',
      );
    } catch (e, st) {
      debugPrint('errore _loadParkingStats: $e');
      debugPrintStack(stackTrace: st);
      UiFeedback.showError(context, 'Errore caricamento statistiche: $e');
    }
  }

  Future<void> _logout() async {
    _autoRefreshTimer?.cancel();
    _autoRefreshTimer = null;

    if (!mounted) return;

    setState(() {
      _isProcessing = false;
      _isRefreshing = false;
      _isLoadingSpots = false;
      _selectedSpotId = null;
      _pageIndex = 0;
    });

    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (!mounted) return;
      Navigator.of(context).pushAndRemoveUntil(
        MaterialPageRoute(builder: (_) => HomePage(apiClient: _apiClient)),
        (route) => false,
      );
    });
  }

  void _changeFloor(int floor) {
    setState(() {
      _selectedFloor = floor;
      _selectedSpotId = null;
    });

    _loadParkingSpots();
  }

  Future<void> _confirmLogout() async {
    final res = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        backgroundColor: AppColors.bgDark,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
        title: const Text(
          'Logout',
          style: TextStyle(
            color: AppColors.textPrimary,
            fontWeight: FontWeight.w800,
          ),
        ),
        content: const Text(
          'Vuoi uscire dall’area Operatore?',
          style: TextStyle(color: AppColors.textSecondary),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(false),
            style: TextButton.styleFrom(foregroundColor: AppColors.textMuted),
            child: const Text('Annulla'),
          ),
          TextButton(
            onPressed: () => Navigator.of(context).pop(true),
            style: TextButton.styleFrom(foregroundColor: AppColors.accentCyan),
            child: const Text('Esci'),
          ),
        ],
      ),
    );

    if (res == true && mounted) await _logout();
  }

  void _showStrutturaDialog() {
    showDialog<void>(
      context: context,
      builder: (_) => AlertDialog(
        backgroundColor: AppColors.bgDark,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
        title: const Text(
          'Dettagli Operatore',
          style: TextStyle(
            color: AppColors.textPrimary,
            fontWeight: FontWeight.w800,
          ),
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Username: ${widget.operatore.username}',
              style: const TextStyle(color: AppColors.textSecondary),
            ),
            const SizedBox(height: 8),
            Text(
              'Struttura: ${widget.operatore.nomeStruttura}',
              style: const TextStyle(color: AppColors.textSecondary),
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            style: TextButton.styleFrom(foregroundColor: AppColors.accentCyan),
            child: const Text('OK'),
          ),
        ],
      ),
    );
  }

  void _selectPage(int idx, {required bool closeDrawer}) {
    setState(() => _pageIndex = idx);
    if (closeDrawer) Navigator.of(context).pop();
  }

  Color _severityColor(LogSeverity s) {
    switch (s) {
      case LogSeverity.critico:
        return const Color(0xFFEF4444); 
      case LogSeverity.attenzione:
        return const Color(0xFFF59E0B);
      case LogSeverity.controllo:
        return const Color.fromARGB(255, 11, 245, 73);
      case LogSeverity.pagamento:
        return const Color.fromARGB(255, 237, 118, 223);
      case LogSeverity.veicolo:
        return const Color.fromARGB(255, 132, 118, 237);
      case LogSeverity.info:
        return AppColors.accentCyan;
      case LogSeverity.risolto:
        return const Color.fromARGB(255, 116, 245, 11);
    }
  }

  IconData _categoryIcon(LogCategory c) {
    switch (c) {
      case LogCategory.allarme:
        return Icons.warning_amber_rounded;
      case LogCategory.evento:
        return Icons.bolt_rounded;
      case LogCategory.history:
        return Icons.history_rounded;
    }
  }

  String _categoryLabel(LogCategory c) {
    switch (c) {
      case LogCategory.allarme:
        return 'Allarmi';
      case LogCategory.evento:
        return 'Eventi';
      case LogCategory.history:
        return 'Storico';
    }
  }

  String _severityLabel(LogSeverity s) {
    switch (s) {
      case LogSeverity.critico:
        return 'Critico';
      case LogSeverity.attenzione:
        return 'Attenzione';
      case LogSeverity.controllo:
        return 'Controllo';
      case LogSeverity.pagamento:
        return 'Pagamento';
      case LogSeverity.veicolo:
        return 'Veicolo';
      case LogSeverity.info:
        return 'Info';
      case LogSeverity.risolto:
        return 'Risolto';
    }
  }

  List<ParkingLogItem> get _filteredItems {
    final q = _searchController.text.trim().toLowerCase();
    return _items.where((it) {
      if (it.category != _selectedCategory) return false;
      if (_severityFilter != null && it.severity != _severityFilter) return false;
      if (q.isEmpty) return true;
      return it.title.toLowerCase().contains(q) ||
          it.details.toLowerCase().contains(q) ||
          (it.source?.toLowerCase().contains(q) ?? false);
    }).toList();
  }

  int get _activeAlarmsCount =>
      _items.where((e) => e.category == LogCategory.allarme).length;

  int get _eventsLast24hCount {
    final since = DateTime.now().subtract(const Duration(hours: 24));
    return _items
        .where(
          (e) => e.category == LogCategory.evento && e.timestamp.isAfter(since),
        )
        .length;
  }

  String _formatTime(DateTime dt) {
    String two(int n) => n.toString().padLeft(2, '0');

    return '${two(dt.day)}/${two(dt.month)} ${two(dt.hour)}:${two(dt.minute)}';
  }

  Future<void> _loadLogs() async {
    setState(() => _isRefreshing = true);

    await Future<void>.delayed(const Duration(milliseconds: 700));

    final data = await _fetchLogItems();

    if (!mounted) return;
    setState(() {
      _items = data;
      _isRefreshing = false;
    });
  }

  Future<void> _refresh() async {
    if (_isRefreshing) return;

    setState(() => _isRefreshing = true);

    await Future<void>.delayed(const Duration(milliseconds: 700));

    try {
      final data = await _fetchLogItems();

      if (!mounted) return;
      setState(() {
        _items = data;
        _lastFetchAt = DateTime.now();
        _isRefreshing = false;
      });

      await _loadParkingStats();
      await _loadParkingSpots();
    } catch (e) {
      if (!mounted) return;
      setState(() => _isRefreshing = false);
      UiFeedback.showError(context, 'Errore refresh: $e');
    }
  }

  @override
  Widget build(BuildContext context) {
    final username = widget.operatore.username.trim();
    final isWide =
        MediaQuery.of(context).size.width > 900;

    return Scaffold(
      key: _scaffoldKey,
      backgroundColor: AppColors.bgDark2,
      drawer: isWide
          ? null
          : Drawer(
              backgroundColor: AppColors.bgDark,
              child: SafeArea(child: _sideMenuContent(isDrawer: true)),
            ),
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
            child: Row(
              children: [
                if (isWide) ...[
                  _sideMenuContainer(),
                  const SizedBox(width: 14),
                ],
                Expanded(
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
                            if (!isWide) ...[
                              InkWell(
                                borderRadius: BorderRadius.circular(999),
                                onTap: () =>
                                    _scaffoldKey.currentState?.openDrawer(),
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
                                    Icons.menu_rounded,
                                    color: AppColors.textPrimary,
                                    size: 18,
                                  ),
                                ),
                              ),
                              const SizedBox(width: 12),
                            ],
                            const Text(
                              'Park M&G',
                              style: TextStyle(
                                color: AppColors.textPrimary,
                                fontSize: 22,
                                fontWeight: FontWeight.w800,
                              ),
                            ),
                            const Spacer(),
                            Column(
                              mainAxisAlignment: MainAxisAlignment.center,
                              crossAxisAlignment: CrossAxisAlignment.end,
                              children: [
                                Text(
                                  username.isEmpty ? 'Operatore' : username,
                                  style: const TextStyle(
                                    color: AppColors.textPrimary,
                                    fontWeight: FontWeight.w700,
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(width: 10),
                            InkWell(
                              borderRadius: BorderRadius.circular(999),
                              onTap: _confirmLogout,
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
                                  Icons.logout,
                                  color: AppColors.textPrimary,
                                  size: 18,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 14),
                      Expanded(
                        child: IndexedStack(
                          index: _pageIndex,
                          children: [
                            _dashboardPage(
                              isWide: MediaQuery.of(context).size.width > 700,
                            ),
                            _parkingStatsPage(
                              isWide: MediaQuery.of(context).size.width > 700,
                            ),
                            QrScannerPage(
                              onQrScanned: _handleQrScan,
                              isActive: _pageIndex == 2,
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _sideMenuContainer() {
    return Container(
      width: 240,
      decoration: BoxDecoration(
        color: AppColors.bgDark,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.borderField, width: 1),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.35),
            blurRadius: 22,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: _sideMenuContent(isDrawer: false),
      ),
    );
  }

  Widget _sideMenuContent({required bool isDrawer}) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        const SizedBox(height: 6),
        const Text(
          'Menu',
          style: TextStyle(
            color: AppColors.textPrimary,
            fontSize: 16,
            fontWeight: FontWeight.w900,
          ),
        ),
        const SizedBox(height: 10),

        _navItem(
          icon: Icons.dashboard_rounded,
          label: 'Dashboard',
          selected: _pageIndex == 0,
          onTap: () => _selectPage(0, closeDrawer: isDrawer),
        ),
        const SizedBox(height: 8),
        _navItem(
          icon: Icons.local_parking_rounded,
          label: 'Stato parcheggio',
          selected: _pageIndex == 1,
          onTap: () => _selectPage(1, closeDrawer: isDrawer),
        ),
        const SizedBox(height: 8),
        _navItem(
          icon: Icons.qr_code_scanner,
          label: 'Scansione QR',
          selected: _pageIndex == 2,
          onTap: () => _selectPage(2, closeDrawer: isDrawer),
        ),
        const Spacer(),
        InkWell(
          borderRadius: BorderRadius.circular(16),
          onTap: () {
            if (isDrawer) Navigator.of(context).pop();
            _showStrutturaDialog();
          },
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
            decoration: BoxDecoration(
              color: AppColors.bgDark2.withValues(alpha: 0.25),
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: AppColors.borderField, width: 1),
            ),
            child: Row(
              children: const [
                Icon(Icons.business, size: 18, color: AppColors.textPrimary),
                SizedBox(width: 10),
                Expanded(
                  child: Text(
                    'Struttura',
                    style: TextStyle(
                      color: AppColors.textPrimary,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ),
                Icon(Icons.chevron_right_rounded, color: AppColors.textMuted),
              ],
            ),
          ),
        ),

        const SizedBox(height: 6),
      ],
    );
  }

  Widget _navItem({
    required IconData icon,
    required String label,
    required bool selected,
    required VoidCallback onTap,
  }) {
    return InkWell(
      borderRadius: BorderRadius.circular(16),
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
        decoration: BoxDecoration(
          color: selected
              ? AppColors.brandTop
              : AppColors.bgDark2.withValues(alpha: 0.18),
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: AppColors.borderField, width: 1),
        ),
        child: Row(
          children: [
            Icon(
              icon,
              size: 18,
              color: selected ? AppColors.textPrimary : AppColors.textMuted,
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Text(
                label,
                style: TextStyle(
                  color: selected ? AppColors.textPrimary : AppColors.textMuted,
                  fontWeight: FontWeight.w800,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _dashboardPage({required bool isWide}) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.bgDark,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.borderField, width: 1),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.35),
            blurRadius: 22,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              Expanded(
                child: InkWell(
                  onTap: _triggerEmergenza,
                  child: Container(
                    margin: const EdgeInsets.only(bottom: 16),
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: Colors.red.withValues(alpha: 0.1),
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(color: Colors.red, width: 2),
                    ),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: const [
                        Icon(Icons.warning_amber_rounded, color: Colors.red),
                        SizedBox(width: 12),
                        Flexible(
                          child: Text(
                            "ATTIVA BLOCCO EMERGENZA",
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              color: Colors.red,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: InkWell(
                  onTap: _showCreateLogDialog,
                  child: Container(
                    margin: const EdgeInsets.only(bottom: 16),
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: AppColors.accentCyan.withValues(alpha: 0.10),
                      borderRadius: BorderRadius.circular(16),
                      border: Border.all(color: AppColors.accentCyan, width: 2),
                    ),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: const [
                        Icon(
                          Icons.note_add_rounded,
                          color: AppColors.accentCyan,
                        ),
                        SizedBox(width: 12),
                        Flexible(
                          child: Text(
                            "CREA ALLARME",
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              color: AppColors.accentCyan,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ],
          ),
          isWide
              ? Row(
                  children: [
                    Expanded(
                      child: _kpiCard(
                        'Allarmi attivi',
                        '$_activeAlarmsCount',
                        Icons.report_gmailerrorred_rounded,
                        const Color(0xFFEF4444),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: _kpiCard(
                        'Eventi (24h)',
                        '$_eventsLast24hCount',
                        Icons.bolt_rounded,
                        AppColors.accentCyan,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: _kpiCard(
                        'Ultimo update',
                        _lastFetchAt == null ? '—' : _formatTime(_lastFetchAt!),
                        Icons.schedule_rounded,
                        AppColors.textSecondary,
                        trailing: _kpiRefreshButton(),
                      ),
                    ),
                  ],
                )
              : Column(
                  children: [
                    _kpiCard(
                      'Allarmi attivi',
                      '$_activeAlarmsCount',
                      Icons.report_gmailerrorred_rounded,
                      const Color(0xFFEF4444),
                    ),
                    const SizedBox(height: 12),
                    _kpiCard(
                      'Eventi (24h)',
                      '$_eventsLast24hCount',
                      Icons.bolt_rounded,
                      AppColors.accentCyan,
                    ),
                    const SizedBox(height: 12),
                    _kpiCard(
                      'Ultimo update',
                      _lastFetchAt == null ? '—' : _formatTime(_lastFetchAt!),
                      Icons.schedule_rounded,
                      AppColors.textSecondary,
                      trailing: _kpiRefreshButton(),
                    ),
                  ],
                ),
          const SizedBox(height: 14),
          Container(
            padding: const EdgeInsets.all(4),
            decoration: BoxDecoration(
              color: AppColors.bgDark2.withValues(alpha: 0.35),
              borderRadius: BorderRadius.circular(999),
              border: Border.all(color: AppColors.borderField, width: 1),
            ),
            child: Row(
              children: LogCategory.values.map((c) {
                final selected = _selectedCategory == c;
                return Expanded(
                  child: InkWell(
                    borderRadius: BorderRadius.circular(999),
                    onTap: () {
                      setState(() {
                        _selectedCategory = c;
                        _severityFilter = null;
                      });
                    },
                    child: Container(
                      padding: const EdgeInsets.symmetric(vertical: 10),
                      decoration: BoxDecoration(
                        color: selected
                            ? AppColors.brandTop
                            : Colors.transparent,
                        borderRadius: BorderRadius.circular(999),
                      ),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          Icon(
                            _categoryIcon(c),
                            size: 18,
                            color: selected
                                ? AppColors.textPrimary
                                : AppColors.textMuted,
                          ),
                          const SizedBox(width: 8),
                          Text(
                            _categoryLabel(c),
                            style: TextStyle(
                              color: selected
                                  ? AppColors.textPrimary
                                  : AppColors.textMuted,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                );
              }).toList(),
            ),
          ),
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: TextField(
                  controller: _searchController,
                  style: const TextStyle(color: AppColors.textPrimary),
                  cursorColor: AppColors.accentCyan,
                  decoration: InputDecoration(
                    hintText: 'Cerca…',
                    hintStyle: TextStyle(
                      color: AppColors.textMuted.withValues(alpha: 0.95),
                    ),
                    prefixIcon: const Icon(
                      Icons.search,
                      color: AppColors.textMuted,
                    ),
                    filled: true,
                    fillColor: AppColors.bgDark2.withValues(alpha: 0.35),
                    contentPadding: const EdgeInsets.symmetric(
                      horizontal: 14,
                      vertical: 12,
                    ),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(14),
                      borderSide: const BorderSide(
                        color: AppColors.borderField,
                      ),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(14),
                      borderSide: const BorderSide(color: AppColors.accentCyan),
                    ),
                  ),
                  onChanged: (_) => setState(() {}),
                ),
              ),
              const SizedBox(width: 10),
              _severityDropdown(),
            ],
          ),
          const SizedBox(height: 12),
          Expanded(
            child: RefreshIndicator(onRefresh: _refresh, child: _buildList()),
          ),
        ],
      ),
    );
  }

  Widget _parkingStatsPage({required bool isWide}) {
    final total = _stats.totalSpots;
    final available = _stats.availableSpots;
    final occupied = _stats.occupiedSpots;
    final percent = _stats.occupancyPercent;
    final active = _stats.activeReservations;
    final inactive = _stats.inactiveReservations;

    const occColor = Color(0xFFF59E0B);

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.bgDark,
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.borderField, width: 1),
        boxShadow: [
          BoxShadow(
            color: Colors.black.withValues(alpha: 0.35),
            blurRadius: 22,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: ListView(
        children: [
          Row(
            children: [
              const Expanded(
                child: Text(
                  'Stato parcheggio',
                  style: TextStyle(
                    color: AppColors.textPrimary,
                    fontSize: 18,
                    fontWeight: FontWeight.w900,
                  ),
                ),
              ),
              _kpiRefreshButton(),
            ],
          ),
          const SizedBox(height: 12),
          isWide
              ? Row(
                  children: [
                    Expanded(
                      child: _kpiCard(
                        'Posti totali',
                        '$total',
                        Icons.local_parking_rounded,
                        AppColors.textSecondary,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: _kpiCard(
                        'Posti disponibili',
                        '$available',
                        Icons.check_circle_rounded,
                        AppColors.accentCyan,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: _kpiCard(
                        'Posti occupati',
                        '$occupied ($percent%)',
                        Icons.directions_car_rounded,
                        occColor,
                      ),
                    ),
                  ],
                )
              : Column(
                  children: [
                    _kpiCard(
                      'Posti totali',
                      '$total',
                      Icons.local_parking_rounded,
                      AppColors.textSecondary,
                    ),
                    const SizedBox(height: 12),
                    _kpiCard(
                      'Posti disponibili',
                      '$available',
                      Icons.check_circle_rounded,
                      AppColors.accentCyan,
                    ),
                    const SizedBox(height: 12),
                    _kpiCard(
                      'Posti occupati',
                      '$occupied ($percent%)',
                      Icons.directions_car_rounded,
                      occColor,
                    ),
                  ],
                ),
          const SizedBox(height: 14),
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: AppColors.bgDark2.withValues(alpha: 0.20),
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: AppColors.borderField, width: 1),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Occupazione',
                  style: TextStyle(
                    color: AppColors.textPrimary,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 10),
                ClipRRect(
                  borderRadius: BorderRadius.circular(999),
                  child: LinearProgressIndicator(
                    value: _stats.occupancyRatio,
                    minHeight: 10,
                    backgroundColor: AppColors.bgDark2.withValues(alpha: 0.35),
                    valueColor: const AlwaysStoppedAnimation<Color>(occColor),
                  ),
                ),
                const SizedBox(height: 10),
                Text(
                  '$occupied occupati su $total • $available disponibili',
                  style: TextStyle(
                    color: AppColors.textMuted.withValues(alpha: 0.95),
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 14),
          isWide
              ? Row(
                  children: [
                    Expanded(
                      child: _kpiCard(
                        'Prenotazioni attive',
                        '$active',
                        Icons.event_available_rounded,
                        AppColors.accentCyan,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: _kpiCard(
                        'Prenotazioni non attive',
                        '$inactive',
                        Icons.event_busy_rounded,
                        AppColors.textSecondary,
                      ),
                    ),
                  ],
                )
              : Column(
                  children: [
                    _kpiCard(
                      'Prenotazioni attive',
                      '$active',
                      Icons.event_available_rounded,
                      AppColors.accentCyan,
                    ),
                    const SizedBox(height: 12),
                    _kpiCard(
                      'Prenotazioni non attive',
                      '$inactive',
                      Icons.event_busy_rounded,
                      AppColors.textSecondary,
                    ),
                  ],
                ),
          const SizedBox(height: 24),

          if (_isLoadingSpots)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 24),
              child: Center(
                child: CircularProgressIndicator(color: AppColors.accentCyan),
              ),
            )
          else
            OperatorParkingImageMap(
              key: ValueKey(
                '${_selectedFloor}_${_realSpots.map((s) => '${s.id}:${s.disponibile}:${s.disabilitato}').join('|')}',
              ),
              selectedFloor: _selectedFloor,
              floors: const [1, 2, 3],
              onFloorChanged: _changeFloor,
              spots: _realSpots,
              selectedSpotId: _selectedSpotId,
              onSpotTap: (slotId) {
                final tapped = _realSpots.firstWhere((s) => s.slotId == slotId);

                if (!tapped.disponibile && !tapped.disabilitato) return;

                setState(() {
                  _selectedSpotId = (_selectedSpotId == slotId) ? null : slotId;
                });
              },
              onDisableSpot: (slotId) async {
                try {
                  final selected = _realSpots.firstWhere(
                    (s) => s.slotId == slotId,
                  );

                  await _apiClient.updatePostoDisabilitato(
                    parcheggioId: widget.operatore.parcheggioId,
                    piano: selected.piano,
                    numero: selected.numero,
                    disabilitato: true,
                  );

                  final updatedSpots = await _apiClient.getPostiParcheggio(
                    widget.operatore.parcheggioId,
                    piano: _selectedFloor,
                  );

                  if (!mounted) return;

                  setState(() {
                    _realSpots = updatedSpots;
                    if (_selectedSpotId == slotId) {
                      _selectedSpotId = null;
                    }
                  });

                  UiFeedback.showSuccess(
                    context,
                    'Posto disabilitato correttamente',
                  );
                  await _loadParkingStats();
                } catch (e) {
                  if (!mounted) return;
                  UiFeedback.showError(
                    context,
                    'Errore disabilitazione posto: $e',
                  );
                }
              },
              onEnableSpot: (slotId) async {
                try {
                  final selected = _realSpots.firstWhere(
                    (s) => s.slotId == slotId,
                  );

                  await _apiClient.updatePostoDisabilitato(
                    parcheggioId: widget.operatore.parcheggioId,
                    piano: selected.piano,
                    numero: selected.numero,
                    disabilitato: false,
                  );

                  final updatedSpots = await _apiClient.getPostiParcheggio(
                    widget.operatore.parcheggioId,
                    piano: _selectedFloor,
                  );

                  if (!mounted) return;

                  setState(() {
                    _realSpots = updatedSpots;
                    if (_selectedSpotId == slotId) {
                      _selectedSpotId = null;
                    }
                  });

                  UiFeedback.showSuccess(
                    context,
                    'Posto riabilitato correttamente',
                  );
                  await _loadParkingStats();
                } catch (e) {
                  if (!mounted) return;
                  UiFeedback.showError(
                    context,
                    'Errore riabilitazione posto: $e',
                  );
                }
              },
            ),
        ],
      ),
    );
  }

  Widget _kpiCard(
    String label,
    String value,
    IconData icon,
    Color accent, {
    Widget? trailing,
  }) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.bgDark2.withValues(alpha: 0.25),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.borderField, width: 1),
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: accent.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: accent.withValues(alpha: 0.35), width: 1),
            ),
            child: Icon(icon, color: accent, size: 20),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  label,
                  style: const TextStyle(
                    color: AppColors.textMuted,
                    fontSize: 12,
                    fontWeight: FontWeight.w700,
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  value,
                  style: const TextStyle(
                    color: AppColors.textPrimary,
                    fontSize: 18,
                    fontWeight: FontWeight.w900,
                  ),
                ),
              ],
            ),
          ),
          if (trailing != null) ...[const SizedBox(width: 10), trailing],
        ],
      ),
    );
  }

  Widget _severityDropdown() {
    final allowed = _allowedSeverities();

    if (_severityFilter != null && !allowed.contains(_severityFilter)) {
      _severityFilter = null;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12),
      decoration: BoxDecoration(
        color: AppColors.bgDark2.withValues(alpha: 0.35),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: AppColors.borderField, width: 1),
      ),
      child: DropdownButtonHideUnderline(
        child: DropdownButton<LogSeverity?>(
          value: _severityFilter,
          dropdownColor: AppColors.bgDark,
          iconEnabledColor: AppColors.textMuted,
          style: const TextStyle(
            color: AppColors.textPrimary,
            fontWeight: FontWeight.w700,
          ),
          items: [
            const DropdownMenuItem<LogSeverity?>(
              value: null,
              child: Text('Tutte'),
            ),
            ...allowed.map(
              (s) => DropdownMenuItem<LogSeverity?>(
                value: s,
                child: Row(
                  children: [
                    Container(
                      width: 10,
                      height: 10,
                      decoration: BoxDecoration(
                        color: _severityColor(s),
                        borderRadius: BorderRadius.circular(99),
                      ),
                    ),
                    const SizedBox(width: 8),
                    Text(_severityLabel(s)),
                  ],
                ),
              ),
            ),
          ],
          onChanged: (v) => setState(() => _severityFilter = v),
        ),
      ),
    );
  }

  Widget _buildList() {
    final list = _filteredItems;

    if (_isRefreshing) {
      return ListView(
        children: const [
          SizedBox(height: 22),
          Center(child: CircularProgressIndicator()),
        ],
      );
    }

    if (list.isEmpty) {
      return ListView(
        children: [
          const SizedBox(height: 28),
          Icon(
            Icons.inbox_rounded,
            size: 42,
            color: AppColors.textMuted.withValues(alpha: 0.8),
          ),
          const SizedBox(height: 10),
          const Center(
            child: Text(
              'Nessun elemento trovato con i filtri attuali.',
              style: TextStyle(
                color: AppColors.textMuted,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
        ],
      );
    }

    return ListView.separated(
      physics: const AlwaysScrollableScrollPhysics(),
      itemCount: list.length,
      separatorBuilder: (_, __) => const SizedBox(height: 10),
      itemBuilder: (_, i) => _logCard(list[i]),
    );
  }

  Widget _logCard(ParkingLogItem it) {
    final sevColor = _severityColor(it.severity);

    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.bgDark2.withValues(alpha: 0.20),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.borderField, width: 1),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 4,
            height: 54,
            decoration: BoxDecoration(
              color: sevColor,
              borderRadius: BorderRadius.circular(999),
            ),
          ),
          const SizedBox(width: 12),
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: sevColor.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: sevColor.withValues(alpha: 0.35), width: 1),
            ),
            child: Icon(_categoryIcon(it.category), color: sevColor, size: 20),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(
                      child: Text(
                        it.title,
                        style: const TextStyle(
                          color: AppColors.textPrimary,
                          fontWeight: FontWeight.w800,
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    _badge(_severityLabel(it.severity), sevColor),
                  ],
                ),
                const SizedBox(height: 8),
                Text(
                  it.details,
                  style: TextStyle(
                    color: AppColors.textMuted.withValues(alpha: 0.95),
                    height: 1.2,
                  ),
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Icon(
                      Icons.schedule_rounded,
                      size: 14,
                      color: AppColors.textMuted.withValues(alpha: 0.9),
                    ),
                    const SizedBox(width: 6),
                    Text(
                      _formatTime(it.timestamp),
                      style: const TextStyle(
                        color: AppColors.textMuted,
                        fontWeight: FontWeight.w600,
                        fontSize: 12,
                      ),
                    ),
                    if (it.source != null) ...[
                      const SizedBox(width: 12),
                      Icon(
                        Icons.memory_rounded,
                        size: 14,
                        color: AppColors.textMuted.withValues(alpha: 0.9),
                      ),
                      const SizedBox(width: 6),
                      Expanded(
                        child: Text(
                          it.source!,
                          style: const TextStyle(
                            color: AppColors.textMuted,
                            fontWeight: FontWeight.w600,
                            fontSize: 12,
                          ),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ] else
                      const Spacer(),
                    if (it.category == LogCategory.allarme) ...[
                      if (it.source != null) const SizedBox(width: 12),
                      InkWell(
                        borderRadius: BorderRadius.circular(999),
                        onTap: () => _resolveAlarm(it),
                        child: Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 10,
                            vertical: 6,
                          ),
                          decoration: BoxDecoration(
                            color: const Color(0xFF10B981).withValues(alpha: 0.10),
                            borderRadius: BorderRadius.circular(999),
                            border: Border.all(
                              color: const Color(0xFF10B981).withValues(alpha: 0.35),
                              width: 1,
                            ),
                          ),
                          child: Row(
                            mainAxisSize: MainAxisSize.min,
                            children: const [
                              Text(
                                'Risolvi',
                                style: TextStyle(
                                  color: Color(0xFF10B981),
                                  fontSize: 12,
                                  fontWeight: FontWeight.w700,
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                    ],
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Future<void> updateLogSeverity(
    ParkingLogItem log,
    LogSeverity newSeverity,
  ) async {
    try {
      await _apiClient.updateLogSeverity(
        log.id,
        newSeverity.name.toUpperCase(),
      );

      if (!mounted) return;
      setState(() {
        log.severity = newSeverity;
      });
    } catch (e) {
      debugPrint('Errore aggiornamento severity: $e');
      UiFeedback.showError(context, 'Errore aggiornamento severità');
    }
  }

  Future<void> updateLogCategory(
    ParkingLogItem log,
    LogCategory newCategory,
  ) async {
    try {
      await _apiClient.updateLogCategory(
        log.id,
        newCategory.name.toUpperCase(),
      );

      if (!mounted) return;
      setState(() {
        log.category = newCategory;
      });
    } catch (e) {
      debugPrint('Errore aggiornamento category: $e');
      UiFeedback.showError(context, 'Errore aggiornamento categoria');
    }
  }

  Widget _badge(String text, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.14),
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: color.withValues(alpha: 0.35), width: 1),
      ),
      child: Text(
        text,
        style: TextStyle(
          color: color,
          fontWeight: FontWeight.w900,
          fontSize: 12,
        ),
      ),
    );
  }

  Widget _kpiRefreshButton() {
    if (_isRefreshing) {
      return const SizedBox(
        width: 18,
        height: 18,
        child: CircularProgressIndicator(strokeWidth: 2),
      );
    }

    return InkWell(
      borderRadius: BorderRadius.circular(999),
      onTap: _refresh,
      child: Container(
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(
          color: AppColors.bgDark2.withValues(alpha: 0.25),
          borderRadius: BorderRadius.circular(999),
          border: Border.all(color: AppColors.borderField, width: 1),
        ),
        child: const Icon(
          Icons.refresh_rounded,
          size: 18,
          color: AppColors.textPrimary,
        ),
      ),
    );
  }

  Future<void> _handleQrScan(String qrCode) async {
    if (_isProcessing) return;

    setState(() {
      _isProcessing = true;
    });

    try {
      final prenotazioneData = await _apiClient.getPrenotazioneByQr(qrCode);
      final statoString = prenotazioneData['stato'] as String;
      final stato = StatoPrenotazione.values.firstWhere(
        (e) => e.name == statoString,
      );
      final prenotazioneId = prenotazioneData['id'] as String;

      switch (stato) {
        case StatoPrenotazione.attiva:
          await _handleIngresso(qrCode);
          break;

        case StatoPrenotazione.inCorso:
        case StatoPrenotazione.parcheggiato:
          await _handlePagamento(prenotazioneId, qrCode);
          break;

        case StatoPrenotazione.pagato:
          await _handleUscita(qrCode);
          break;

        case StatoPrenotazione.conclusa:
          _showErrorDialog('Prenotazione già conclusa');
          break;

        case StatoPrenotazione.scaduta:
          _showErrorDialog('Prenotazione scaduta');
          break;

        case StatoPrenotazione.annullata:
          _showErrorDialog('Prenotazione annullata');
          break;
      }
    } catch (e) {
      _showErrorDialog('Errore: ${e.toString()}');
    } finally {
      if (mounted) {
        setState(() {
          _isProcessing = false;
        });
      }
    }
  }

  Future<void> _handleIngresso(String qrCode) async {
    try {
      final response = await _apiClient.validaIngresso(qrCode);

      if (mounted) {
        await showDialog(
          context: context,
          builder: (_) => AlertDialog(
            backgroundColor: AppColors.bgDark,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(18),
            ),
            title: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: const Color(0xFF10B981).withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: const Icon(
                    Icons.check_circle,
                    color: Color(0xFF10B981),
                    size: 28,
                  ),
                ),
                const SizedBox(width: 12),
                const Expanded(
                  child: Text(
                    'Ingresso Validato',
                    style: TextStyle(
                      color: AppColors.textPrimary,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ),
              ],
            ),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _infoRow('Prenotazione', response.id),
                const SizedBox(height: 8),
                _infoRow('Codice QR', response.codiceQr ?? qrCode),
                const SizedBox(height: 8),
                _infoRow('Stato', _formatStato(response.stato)),
                const SizedBox(height: 8),
                _infoRow(
                  'Data ingresso',
                  response.dataIngresso != null
                      ? _formatTime(response.dataIngresso!)
                      : 'Ora',
                ),
              ],
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(context).pop(),
                style: TextButton.styleFrom(
                  foregroundColor: AppColors.accentCyan,
                ),
                child: const Text('OK'),
              ),
            ],
          ),
        );

        _refresh();
      }
    } catch (e) {
      _showErrorDialog('Errore validazione ingresso: ${e.toString()}');
    }
  }

  Future<void> _handlePagamento(String prenotazioneId, String qrCode) async {
    try {
      final importo = await _apiClient.calcolaImporto(prenotazioneId);

      final conferma = await _showPagamentoDialog(importo, prenotazioneId);

      if (conferma == true && mounted) {
        _showSuccessDialog(
          'Pagamento registrato con successo.\n\nScansionare nuovamente il QR per consentire l\'uscita.',
        );
        _refresh();
      }
    } catch (e) {
      _showErrorDialog('Errore durante il pagamento: ${e.toString()}');
    }
  }

  Future<void> _handleUscita(String qrCode) async {
    try {
      final response = await _apiClient.validaUscita(qrCode);

      if (mounted) {
        await showDialog(
          context: context,
          builder: (_) => AlertDialog(
            backgroundColor: AppColors.bgDark,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(18),
            ),
            title: Row(
              children: [
                Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: const Color(0xFF10B981).withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: const Icon(
                    Icons.check_circle,
                    color: Color(0xFF10B981),
                    size: 28,
                  ),
                ),
                const SizedBox(width: 12),
                const Expanded(
                  child: Text(
                    'Uscita Consentita',
                    style: TextStyle(
                      color: AppColors.textPrimary,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ),
              ],
            ),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                _infoRow('Prenotazione', response.id),
                const SizedBox(height: 8),
                _infoRow('Stato', _formatStato(response.stato)),
                const SizedBox(height: 8),
                _infoRow(
                  'Data uscita',
                  response.dataUscita != null
                      ? _formatTime(response.dataUscita!)
                      : 'Ora',
                ),
                const SizedBox(height: 16),
                const Text(
                  'Buon viaggio! 🚗',
                  style: TextStyle(
                    color: AppColors.textSecondary,
                    fontSize: 14,
                  ),
                ),
              ],
            ),
            actions: [
              TextButton(
                onPressed: () => Navigator.of(context).pop(),
                style: TextButton.styleFrom(
                  foregroundColor: AppColors.accentCyan,
                ),
                child: const Text('OK'),
              ),
            ],
          ),
        );

        _refresh();
      }
    } catch (e) {
      _showErrorDialog('Errore validazione uscita: ${e.toString()}');
    }
  }

  Future<bool?> _showPagamentoDialog(
    double importoCalcolato,
    String prenotazioneId,
  ) async {
    final importoController = TextEditingController(
      text: importoCalcolato.toStringAsFixed(2),
    );

    return showDialog<bool>(
      context: context,
      barrierDismissible: false,
      builder: (BuildContext context) {
        return AlertDialog(
          backgroundColor: AppColors.bgDark,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(18),
          ),
          title: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: const Color(0xFFF59E0B).withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Icon(
                  Icons.euro,
                  color: Color(0xFFF59E0B),
                  size: 28,
                ),
              ),
              const SizedBox(width: 12),
              const Expanded(
                child: Text(
                  'Pagamento in Cassa',
                  style: TextStyle(
                    color: AppColors.textPrimary,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
            ],
          ),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'Importo dovuto:',
                style: TextStyle(
                  color: AppColors.textMuted,
                  fontWeight: FontWeight.w600,
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: importoController,
                keyboardType: const TextInputType.numberWithOptions(
                  decimal: true,
                ),
                style: const TextStyle(
                  color: AppColors.textPrimary,
                  fontSize: 18,
                  fontWeight: FontWeight.w800,
                ),
                decoration: InputDecoration(
                  prefixText: '€ ',
                  prefixStyle: const TextStyle(
                    color: AppColors.textPrimary,
                    fontSize: 18,
                    fontWeight: FontWeight.w800,
                  ),
                  filled: true,
                  fillColor: AppColors.bgDark2,
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: const BorderSide(color: AppColors.borderField),
                  ),
                  enabledBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: const BorderSide(color: AppColors.borderField),
                  ),
                  focusedBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(12),
                    borderSide: const BorderSide(
                      color: AppColors.accentCyan,
                      width: 2,
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 12),
              const Text(
                'L\'operatore può modificare l\'importo se necessario.',
                style: TextStyle(fontSize: 12, color: AppColors.textMuted),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(context).pop(false),
              style: TextButton.styleFrom(foregroundColor: AppColors.textMuted),
              child: const Text('Annulla'),
            ),
            ElevatedButton(
              onPressed: () async {
                final importo = double.tryParse(importoController.text);

                if (importo == null || importo <= 0) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('Importo non valido'),
                      backgroundColor: Colors.red,
                    ),
                  );
                  return;
                }

                try {
                  await _apiClient.pagaPrenotazione(prenotazioneId, importo);
                  if(!context.mounted) return;
                  Navigator.of(context).pop(true);
                } catch (e) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text('Errore: ${e.toString()}'),
                      backgroundColor: Colors.red,
                    ),
                  );
                }
              },
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.accentCyan,
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
              child: const Text('Conferma Pagamento'),
            ),
          ],
        );
      },
    );
  }

  void _showSuccessDialog(String message) {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          backgroundColor: AppColors.bgDark,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(18),
          ),
          title: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: const Color(0xFF10B981).withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Icon(
                  Icons.check_circle,
                  color: Color(0xFF10B981),
                  size: 28,
                ),
              ),
              const SizedBox(width: 12),
              const Expanded(
                child: Text(
                  'Operazione completata',
                  style: TextStyle(
                    color: AppColors.textPrimary,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
            ],
          ),
          content: Text(
            message,
            style: const TextStyle(color: AppColors.textSecondary),
          ),
          actions: [
            ElevatedButton(
              onPressed: () => Navigator.of(context).pop(),
              style: ElevatedButton.styleFrom(
                backgroundColor: AppColors.accentCyan,
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
              child: const Text('OK'),
            ),
          ],
        );
      },
    );
  }

  void _showErrorDialog(String message) {
    showDialog(
      context: context,
      builder: (BuildContext context) {
        return AlertDialog(
          backgroundColor: AppColors.bgDark,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(18),
          ),
          title: Row(
            children: [
              Container(
                padding: const EdgeInsets.all(8),
                decoration: BoxDecoration(
                  color: const Color(0xFFEF4444).withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(12),
                ),
                child: const Icon(
                  Icons.error,
                  color: Color(0xFFEF4444),
                  size: 28,
                ),
              ),
              const SizedBox(width: 12),
              const Expanded(
                child: Text(
                  'Errore',
                  style: TextStyle(
                    color: AppColors.textPrimary,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
            ],
          ),
          content: Text(
            message,
            style: const TextStyle(color: AppColors.textSecondary),
          ),
          actions: [
            ElevatedButton(
              onPressed: () => Navigator.of(context).pop(),
              style: ElevatedButton.styleFrom(
                backgroundColor: const Color(0xFFEF4444),
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                ),
              ),
              child: const Text('OK'),
            ),
          ],
        );
      },
    );
  }

  String _formatStato(StatoPrenotazione stato) {
    switch (stato) {
      case StatoPrenotazione.attiva:
        return 'Attiva';
      case StatoPrenotazione.inCorso:
        return 'In Corso';
      case StatoPrenotazione.parcheggiato:
        return 'Parcheggiato';
      case StatoPrenotazione.pagato:
        return 'Pagato';
      case StatoPrenotazione.conclusa:
        return 'Conclusa';
      case StatoPrenotazione.scaduta:
        return 'Scaduta';
      case StatoPrenotazione.annullata:
        return 'Annullata';
    }
  }

  Widget _infoRow(String label, String value) {
    return Row(
      children: [
        Text(
          '$label: ',
          style: const TextStyle(
            color: AppColors.textMuted,
            fontWeight: FontWeight.w600,
          ),
        ),
        Expanded(
          child: Text(
            value,
            style: const TextStyle(
              color: AppColors.textPrimary,
              fontWeight: FontWeight.w800,
            ),
            overflow: TextOverflow.ellipsis,
          ),
        ),
      ],
    );
  }

  Future<void> _triggerEmergenza() async {
    String motivo = "";
    final confirm = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.bgDark,
        title: const Text(
          'ATTIVAZIONE EMERGENZA',
          style: TextStyle(color: Colors.red, fontWeight: FontWeight.bold),
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Text(
              'Il parcheggio verrà chiuso istantaneamente.',
              style: TextStyle(color: Colors.white),
            ),
            const SizedBox(height: 10),
            TextField(
              style: const TextStyle(color: Colors.white),
              decoration: const InputDecoration(
                labelText: 'Motivo (opzionale)',
                labelStyle: TextStyle(color: Colors.grey),
              ),
              onChanged: (v) => motivo = v,
            ),
          ],
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Annulla'),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(backgroundColor: Colors.red),
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('CONFERMA BLOCCO'),
          ),
        ],
      ),
    );

    if (confirm == true) {
      try {
        await _apiClient.impostaEmergenza(
          widget.operatore.parcheggioId,
          true,
          motivo,
        );
        _refresh();
        if(!mounted) return;
        UiFeedback.showSuccess(context, "EMERGENZA ATTIVATA");
      } catch (e) {
        if(!mounted) return;
        UiFeedback.showError(context, "Errore: $e");
      }
    }
  }
}
