import 'package:flutter/material.dart';
import 'package:park_mg/utils/theme.dart';
import '../models/operatore.dart';

enum LogCategory { alarms, events, history }
enum LogSeverity { critical, warning, info }

class ParkingLogItem {
  final DateTime timestamp;
  final LogCategory category;
  final LogSeverity severity;
  final String title;
  final String details;
  final String? source;

  const ParkingLogItem({
    required this.timestamp,
    required this.category,
    required this.severity,
    required this.title,
    required this.details,
    this.source,
  });
}

class OperatorScreen extends StatefulWidget {
  final Operatore operatore;

  const OperatorScreen({
    super.key,
    required this.operatore,
  });

  @override
  State<OperatorScreen> createState() => _OperatorScreenState();
}

class _OperatorScreenState extends State<OperatorScreen> {
  final GlobalKey _gearKey = GlobalKey();

  // Dashboard state
  LogCategory _selectedCategory = LogCategory.alarms;
  LogSeverity? _severityFilter; // null = tutte
  final _searchController = TextEditingController();

  bool _isRefreshing = false;

  // Mock data (poi li rimpiazzi con API / websocket / polling)
  late List<ParkingLogItem> _items;

  @override
  void initState() {
    super.initState();
    _items = _buildMockItems();
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  List<ParkingLogItem> _buildMockItems() {
    final now = DateTime.now();
    return [
      ParkingLogItem(
        timestamp: now.subtract(const Duration(minutes: 6)),
        category: LogCategory.alarms,
        severity: LogSeverity.critical,
        title: 'Sbarra ingresso bloccata',
        details: 'Motore in overload. Verificare alimentazione e finecorsa.',
        source: 'Gate #1',
      ),
      ParkingLogItem(
        timestamp: now.subtract(const Duration(minutes: 14)),
        category: LogCategory.alarms,
        severity: LogSeverity.warning,
        title: 'Sensore posto non coerente',
        details: 'Posto A-12: stato oscillante nelle ultime letture.',
        source: 'Sensor A-12',
      ),
      ParkingLogItem(
        timestamp: now.subtract(const Duration(minutes: 30)),
        category: LogCategory.events,
        severity: LogSeverity.info,
        title: 'Pagamento completato',
        details: 'Ticket #4932 pagato con carta. Uscita autorizzata.',
        source: 'Cassa automatica',
      ),
      ParkingLogItem(
        timestamp: now.subtract(const Duration(hours: 2, minutes: 5)),
        category: LogCategory.events,
        severity: LogSeverity.info,
        title: 'Ingresso veicolo',
        details: 'Targa rilevata e associata a ticket.',
        source: 'Camera IN',
      ),
      ParkingLogItem(
        timestamp: now.subtract(const Duration(hours: 5)),
        category: LogCategory.history,
        severity: LogSeverity.info,
        title: 'Cambio tariffa',
        details: 'Tariffa oraria aggiornata da 1.50€ a 2.00€.',
        source: 'Operatore admin',
      ),
      ParkingLogItem(
        timestamp: now.subtract(const Duration(days: 1, hours: 3)),
        category: LogCategory.history,
        severity: LogSeverity.warning,
        title: 'Interruzione rete',
        details: 'Connessione persa per 3 minuti. Recupero automatico ok.',
        source: 'Gateway',
      ),
    ]..sort((a, b) => b.timestamp.compareTo(a.timestamp));
  }

  // ---- Top menu actions ----

  void _logout() => Navigator.of(context).pop();

  void _showStrutturaDialog() {
    showDialog<void>(
      context: context,
      builder: (_) => AlertDialog(
        backgroundColor: AppColors.bgDark,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(18)),
        title: const Text(
          'Dettagli Operatore',
          style: TextStyle(color: AppColors.textPrimary, fontWeight: FontWeight.w800),
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

  Future<void> _openOperatorMenu() async {
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
          value: 'info',
          child: Row(
            children: const [
              Icon(Icons.business, size: 18, color: AppColors.textPrimary),
              SizedBox(width: 10),
              Text(
                'Struttura',
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
    if (selected == 'info') _showStrutturaDialog();
    if (selected == 'logout') _logout();
  }

  // ---- Dashboard logic ----

  Color _severityColor(LogSeverity s) {
    switch (s) {
      case LogSeverity.critical:
        return const Color(0xFFEF4444); // red
      case LogSeverity.warning:
        return const Color(0xFFF59E0B); // amber
      case LogSeverity.info:
        return AppColors.accentCyan;
    }
  }

  IconData _categoryIcon(LogCategory c) {
    switch (c) {
      case LogCategory.alarms:
        return Icons.warning_amber_rounded;
      case LogCategory.events:
        return Icons.bolt_rounded;
      case LogCategory.history:
        return Icons.history_rounded;
    }
  }

  String _categoryLabel(LogCategory c) {
    switch (c) {
      case LogCategory.alarms:
        return 'Allarmi';
      case LogCategory.events:
        return 'Eventi';
      case LogCategory.history:
        return 'Storico';
    }
  }

  String _severityLabel(LogSeverity s) {
    switch (s) {
      case LogSeverity.critical:
        return 'Critico';
      case LogSeverity.warning:
        return 'Warning';
      case LogSeverity.info:
        return 'Info';
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
      _items.where((e) => e.category == LogCategory.alarms).length;

  int get _eventsLast24hCount {
    final since = DateTime.now().subtract(const Duration(hours: 24));
    return _items.where((e) => e.category == LogCategory.events && e.timestamp.isAfter(since)).length;
  }

  DateTime? get _lastUpdate =>
      _items.isEmpty ? null : _items.map((e) => e.timestamp).reduce((a, b) => a.isAfter(b) ? a : b);

  String _formatTime(DateTime dt) {
    final two = (int n) => n.toString().padLeft(2, '0');
    return '${two(dt.day)}/${two(dt.month)} ${two(dt.hour)}:${two(dt.minute)}';
  }

  Future<void> _refresh() async {
    setState(() => _isRefreshing = true);

    // Qui in futuro farai: await api.fetchLogs(...);
    await Future<void>.delayed(const Duration(milliseconds: 700));

    if (!mounted) return;
    setState(() {
      _items = _buildMockItems();
      _isRefreshing = false;
    });
  }

  // ---- UI ----

  @override
  Widget build(BuildContext context) {
    final username = widget.operatore.username.trim();
    final isWide = MediaQuery.of(context).size.width > 700;

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
                // TOP BAR uguale a UserScreen
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
                        key: _gearKey,
                        borderRadius: BorderRadius.circular(999),
                        onTap: _openOperatorMenu,
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

                // DASHBOARD
                Expanded(
                  child: Container(
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
                                  Expanded(child: _kpiCard('Allarmi attivi', '$_activeAlarmsCount', Icons.report_gmailerrorred_rounded, const Color(0xFFEF4444))),
                                  const SizedBox(width: 12),
                                  Expanded(child: _kpiCard('Eventi (24h)', '$_eventsLast24hCount', Icons.bolt_rounded, AppColors.accentCyan)),
                                  const SizedBox(width: 12),
                                  Expanded(child: _kpiCard('Ultimo update', _lastUpdate == null ? '—' : _formatTime(_lastUpdate!), Icons.schedule_rounded, AppColors.textSecondary)),
                                ],
                              )
                            : Column(
                                children: [
                                  _kpiCard('Allarmi attivi', '$_activeAlarmsCount', Icons.report_gmailerrorred_rounded, const Color(0xFFEF4444)),
                                  const SizedBox(height: 12),
                                  _kpiCard('Eventi (24h)', '$_eventsLast24hCount', Icons.bolt_rounded, AppColors.accentCyan),
                                  const SizedBox(height: 12),
                                  _kpiCard('Ultimo update', _lastUpdate == null ? '—' : _formatTime(_lastUpdate!), Icons.schedule_rounded, AppColors.textSecondary),
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
                                  onTap: () => setState(() => _selectedCategory = c),
                                  child: Container(
                                    padding: const EdgeInsets.symmetric(vertical: 10),
                                    decoration: BoxDecoration(
                                      color: selected ? AppColors.brandTop : Colors.transparent,
                                      borderRadius: BorderRadius.circular(999),
                                    ),
                                    child: Row(
                                      mainAxisAlignment: MainAxisAlignment.center,
                                      children: [
                                        Icon(
                                          _categoryIcon(c),
                                          size: 18,
                                          color: selected ? AppColors.textPrimary : AppColors.textMuted,
                                        ),
                                        const SizedBox(width: 8),
                                        Text(
                                          _categoryLabel(c),
                                          style: TextStyle(
                                            color: selected ? AppColors.textPrimary : AppColors.textMuted,
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
                                  hintStyle: TextStyle(color: AppColors.textMuted.withOpacity(0.95)),
                                  prefixIcon: const Icon(Icons.search, color: AppColors.textMuted),
                                  filled: true,
                                  fillColor: AppColors.bgDark2.withOpacity(0.35),
                                  contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 12),
                                  enabledBorder: OutlineInputBorder(
                                    borderRadius: BorderRadius.circular(14),
                                    borderSide: const BorderSide(color: AppColors.borderField),
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
                          child: RefreshIndicator(
                            onRefresh: _refresh,
                            child: _buildList(),
                          ),
                        ),
                      ],
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

  Widget _kpiCard(String label, String value, IconData icon, Color accent) {
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
                Text(label, style: const TextStyle(color: AppColors.textMuted, fontSize: 12, fontWeight: FontWeight.w700)),
                const SizedBox(height: 6),
                Text(value, style: const TextStyle(color: AppColors.textPrimary, fontSize: 18, fontWeight: FontWeight.w900)),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _severityDropdown() {
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
          style: const TextStyle(color: AppColors.textPrimary, fontWeight: FontWeight.w700),
          items: [
            const DropdownMenuItem<LogSeverity?>(
              value: null,
              child: Text('Tutte'),
            ),
            ...LogSeverity.values.map(
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
          Icon(Icons.inbox_rounded, size: 42, color: AppColors.textMuted.withOpacity(0.8)),
          const SizedBox(height: 10),
          const Center(
            child: Text(
              'Nessun elemento trovato con i filtri attuali.',
              style: TextStyle(color: AppColors.textMuted, fontWeight: FontWeight.w600),
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
          // Left accent
          Container(
            width: 4,
            height: 54,
            decoration: BoxDecoration(
              color: sevColor,
              borderRadius: BorderRadius.circular(999),
            ),
          ),
          const SizedBox(width: 12),

          // Icon
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

          // Text
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
                  style: TextStyle(color: AppColors.textMuted.withOpacity(0.95), height: 1.2),
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Icon(Icons.schedule_rounded, size: 14, color: AppColors.textMuted.withOpacity(0.9)),
                    const SizedBox(width: 6),
                    Text(
                      _formatTime(it.timestamp),
                      style: const TextStyle(color: AppColors.textMuted, fontWeight: FontWeight.w600, fontSize: 12),
                    ),
                    if (it.source != null) ...[
                      const SizedBox(width: 12),
                      Icon(Icons.memory_rounded, size: 14, color: AppColors.textMuted.withOpacity(0.9)),
                      const SizedBox(width: 6),
                      Expanded(
                        child: Text(
                          it.source!,
                          style: const TextStyle(color: AppColors.textMuted, fontWeight: FontWeight.w600, fontSize: 12),
                          overflow: TextOverflow.ellipsis,
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
        style: TextStyle(color: color, fontWeight: FontWeight.w900, fontSize: 12),
      ),
    );
  }
}
