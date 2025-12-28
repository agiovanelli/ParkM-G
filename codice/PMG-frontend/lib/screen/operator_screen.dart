import 'dart:async';
import 'package:flutter/material.dart';
import 'package:park_mg/utils/theme.dart';
import 'package:park_mg/widgets/parking_map.dart' as sch;
import '../models/operatore.dart';
import 'dart:convert';
import 'package:http/http.dart' as http;

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
  final LogCategory category;
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
    return ParkingLogItem(
      id: json['id'] as String,
      timestamp: DateTime.parse(json['data']),
      category: LogCategory.values.firstWhere(
        (e) => e.name.toLowerCase() == (json['tipo'] as String).toLowerCase(),
        orElse: () => LogCategory.history,
      ),
      severity: LogSeverity.values.firstWhere(
        (e) =>
            e.name.toLowerCase() == (json['severità'] as String).toLowerCase(),
        orElse: () => LogSeverity.info,
      ),
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

enum ParkingSpotState { available, occupied, reserved, unavailable }

class ParkingSpot {
  final String id;
  final ParkingSpotState state;

  const ParkingSpot(this.id, this.state);
}

List<ParkingSpot> mockSpotsForFloor(int floor) {
  final random = [
    ParkingSpotState.available,
    ParkingSpotState.occupied,
    ParkingSpotState.reserved,
    ParkingSpotState.unavailable,
  ];

  return List.generate(
    44,
    (i) => ParkingSpot(
      'F$floor-${(i + 1).toString().padLeft(3, '0')}',
      random[(i + floor * 2) % random.length],
    ),
  );
}

class OperatorScreen extends StatefulWidget {
  final Operatore operatore;

  const OperatorScreen({super.key, required this.operatore});

  @override
  State<OperatorScreen> createState() => _OperatorScreenState();
}

class _OperatorScreenState extends State<OperatorScreen> {
  final _scaffoldKey = GlobalKey<ScaffoldState>();

  // --- Side menu navigation ---
  int _pageIndex = 0;

  String? _selectedSpotId;

  int _selectedFloor = 1;
  late List<ParkingSpot> _spots;

  // Dashboard state
  LogCategory _selectedCategory = LogCategory.allarme;
  LogSeverity? _severityFilter; // null = tutte

  final _searchController = TextEditingController();

  bool _isRefreshing = false;

  // Mock data (poi li rimpiazzi con API / websocket / polling)
  late List<ParkingLogItem> _items;

  // Nuova pagina: stats
  late ParkingStats _stats;

  // Auto refresh + last fetch
  Timer? _autoRefreshTimer;
  DateTime? _lastFetchAt;

  @override
  void initState() {
    super.initState();
    _items = [];
    _loadLogs();
    _stats = _buildMockStats();
    _lastFetchAt = DateTime.now();
    _spots = mockSpotsForFloor(_selectedFloor);

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

  Future<List<ParkingLogItem>> _fetchLogItems() async {
    final url = Uri.parse(
      'http://localhost:8080/api/analitiche/694aa3eeb7b5590ae69d9379/log',
    );

    final response = await http.get(url);

    if (response.statusCode != 200) {
      throw Exception('Errore nel caricamento dei log');
    }

    final jsonList = jsonDecode(response.body);
    if (jsonList is! List) {
      throw Exception('Il backend non ha restituito una lista JSON');
    }

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
      return [
        LogSeverity.pagamento,
        LogSeverity.veicolo,
        LogSeverity.info,
      ];

    case LogCategory.history:
      return LogSeverity.values;
  }
}

  ParkingStats _buildMockStats() {
    // TODO: rimpiazza con dati reali (API/websocket/polling)
    return const ParkingStats(
      totalSpots: 220,
      availableSpots: 58,
      activeReservations: 12,
      inactiveReservations: 3,
    );
  }

  // ---- Top actions ----

  void _logout() => Navigator.of(context).pop();

  void _changeFloor(int floor) {
    setState(() {
      _selectedFloor = floor;
      _selectedSpotId = null;
      _spots = mockSpotsForFloor(floor);
    });
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

    if (res == true && mounted) _logout();
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

  // ---- Dashboard logic ----

  Color _severityColor(LogSeverity s) {
    switch (s) {
      case LogSeverity.critico:
        return const Color(0xFFEF4444); // red
      case LogSeverity.attenzione:
        return const Color(0xFFF59E0B); // amber
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
      if (_severityFilter != null && it.severity != _severityFilter)
        return false;
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
    final two = (int n) => n.toString().padLeft(2, '0');
    return '${two(dt.day)}/${two(dt.month)} ${two(dt.hour)}:${two(dt.minute)}';
  }

  Future<void> _loadLogs() async {
    setState(() => _isRefreshing = true);

    // Qui in futuro farai: await api.fetchLogs(...);
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

    // Qui in futuro farai: await api.fetchLogs(...);
    await Future<void>.delayed(const Duration(milliseconds: 700));

    final data = await _fetchLogItems();

    if (!mounted) return;
    setState(() {
      _items = data;
      _stats = _buildMockStats();
      _spots = mockSpotsForFloor(_selectedFloor); // aggiorna anche la mappa
      _lastFetchAt = DateTime.now();
      _isRefreshing = false;
    });
  }

  // ---- UI ----

  @override
  Widget build(BuildContext context) {
    final username = widget.operatore.username.trim();
    final isWide =
        MediaQuery.of(context).size.width > 900; // sidebar persistente

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
                      // TOP BAR
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

                      // CONTENT (2 schermate)
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

  // ---- Side menu ----

  Widget _sideMenuContainer() {
    return Container(
      width: 240,
      decoration: BoxDecoration(
        color: AppColors.bgDark,
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

        const Spacer(),

        // Bottone in fondo: Struttura
        InkWell(
          borderRadius: BorderRadius.circular(16),
          onTap: () {
            if (isDrawer) Navigator.of(context).pop();
            _showStrutturaDialog();
          },
          child: Container(
            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
            decoration: BoxDecoration(
              color: AppColors.bgDark2.withOpacity(0.25),
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
              : AppColors.bgDark2.withOpacity(0.18),
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

  // ---- Pages ----

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
            color: Colors.black.withOpacity(0.35),
            blurRadius: 22,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // KPI CARDS
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

          // CATEGORY SEGMENT
          Container(
            padding: const EdgeInsets.all(4),
            decoration: BoxDecoration(
              color: AppColors.bgDark2.withOpacity(0.35),
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
                        _severityFilter = null; // reset consigliato
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

          // FILTER ROW
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
                      color: AppColors.textMuted.withOpacity(0.95),
                    ),
                    prefixIcon: const Icon(
                      Icons.search,
                      color: AppColors.textMuted,
                    ),
                    filled: true,
                    fillColor: AppColors.bgDark2.withOpacity(0.35),
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

          // LIST
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

    const occColor = Color(0xFFF59E0B); // amber

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.bgDark,
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

          // KPI: posti (totali / disponibili / occupati + %)
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

          // Barra di occupazione
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: AppColors.bgDark2.withOpacity(0.20),
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
                    backgroundColor: AppColors.bgDark2.withOpacity(0.35),
                    valueColor: const AlwaysStoppedAnimation<Color>(occColor),
                  ),
                ),
                const SizedBox(height: 10),
                Text(
                  '$occupied occupati su $total • $available disponibili',
                  style: TextStyle(
                    color: AppColors.textMuted.withOpacity(0.95),
                    fontWeight: FontWeight.w700,
                  ),
                ),
              ],
            ),
          ),

          const SizedBox(height: 14),

          // KPI: prenotazioni
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

          // ---- MAPPA PARCHEGGIO (ORA IN FILE SEPARATO) ----
          sch.ParkingSchematicMap(
            selectedFloor: _selectedFloor,
            floors: const [1, 2, 3, 4, 5],
            onFloorChanged: (v) => _changeFloor(v),

            spots: sch.UniformGarageLayout.buildForFloor(
              floor: _selectedFloor,
              selectedId: _selectedSpotId,
              occupiedIds: _occupiedIdsForSelectedFloor(),
              reservedIds: _reservedIdsForSelectedFloor(),
              unavailableIds: _unavailableIdsForSelectedFloor(),
            ),

            onSpotTap: (s) {
              // Blocca tap su occupati/prenotati/non disponibili
              if (s.state != sch.SpotState.free) return;

              setState(() {
                _selectedSpotId = (_selectedSpotId == s.id) ? null : s.id;
              });
            },

            legend: [
              _legendItem(AppColors.accentCyan, 'Disponibile'),
              _legendItem(const Color(0xFFF59E0B), 'Occupato'),
              _legendItem(const Color(0xFF3B82F6), 'Prenotato'),
              _legendItem(const Color(0xFF6B7280), 'Non disponibile'),
            ],
          ),
        ],
      ),
    );
  }

  Set<String> _occupiedIdsForSelectedFloor() {
    return _spots
        .where((s) => s.state == ParkingSpotState.occupied)
        .map((s) => s.id)
        .toSet();
  }

  Set<String> _reservedIdsForSelectedFloor() {
    return _spots
        .where((s) => s.state == ParkingSpotState.reserved)
        .map((s) => s.id)
        .toSet();
  }

  Set<String> _unavailableIdsForSelectedFloor() {
    return _spots
        .where((s) => s.state == ParkingSpotState.unavailable)
        .map((s) => s.id)
        .toSet();
  }

  // ---- Widgets (riusati) ----

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
        color: AppColors.bgDark2.withOpacity(0.25),
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: AppColors.borderField, width: 1),
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: accent.withOpacity(0.12),
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: accent.withOpacity(0.35), width: 1),
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

  Widget _legendItem(Color color, String label) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 12,
          height: 12,
          decoration: BoxDecoration(
            color: color.withOpacity(0.25),
            border: Border.all(color: color, width: 1.5),
            borderRadius: BorderRadius.circular(3),
          ),
        ),
        const SizedBox(width: 6),
        Text(
          label,
          style: const TextStyle(
            color: AppColors.textMuted,
            fontWeight: FontWeight.w700,
            fontSize: 12,
          ),
        ),
      ],
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
        color: AppColors.bgDark2.withOpacity(0.35),
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
            color: AppColors.textMuted.withOpacity(0.8),
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
        color: AppColors.bgDark2.withOpacity(0.20),
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
              color: sevColor.withOpacity(0.12),
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: sevColor.withOpacity(0.35), width: 1),
            ),
            child: Icon(_categoryIcon(it.category), color: sevColor, size: 20),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
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
                const SizedBox(height: 6),
                Text(
                  it.details,
                  style: TextStyle(
                    color: AppColors.textMuted.withOpacity(0.95),
                    height: 1.2,
                  ),
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(
                      child: 
                      Row(
                        children: [
                          Icon(
                            Icons.schedule_rounded,
                            size: 14,
                            color: AppColors.textMuted.withOpacity(0.9),
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
                              color: AppColors.textMuted.withOpacity(0.9),
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
                          ],

                          if(it.category == LogCategory.allarme)...{
                            const SizedBox(width: 12),
                            ElevatedButton(
                              onPressed: () async {
                                final newSeverity = LogSeverity.risolto; 
                                await updateLogSeverity(it, newSeverity); 
                                setState(() {}); 
                              },
                              style: ElevatedButton.styleFrom(
                                backgroundColor: _severityColor(it.severity),
                                minimumSize: const Size(26, 26),
                                padding: EdgeInsets.zero,
                                shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(8),
                                ),
                              ),
                              child: const Icon(
                                Icons.arrow_upward,
                                size: 18,
                                color: Colors.white,
                              ),
                            ),
                          }
                        ],
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),  
        ],
      ),
    );
  }

  Future<void> updateLogSeverity(ParkingLogItem log, LogSeverity newSeverity) async {
    final url = Uri.parse(
      'http://localhost:8080/api/log/${log.id}/severity?severity=${newSeverity.name}'
    );

    final response = await http.put(url);

    if (response.statusCode == 200) {
      // Aggiorno localmente
      setState(() {
        log.severity = newSeverity;
      });
    } else {
      // Gestione errore
      debugPrint('Errore aggiornamento severity: ${response.statusCode}');
    }
  }


  Widget _badge(String text, Color color) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
      decoration: BoxDecoration(
        color: color.withOpacity(0.14),
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: color.withOpacity(0.35), width: 1),
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
          color: AppColors.bgDark2.withOpacity(0.25),
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
}
