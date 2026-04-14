import 'package:flutter/material.dart';
import 'package:park_mg/api/api_client.dart';
import 'package:park_mg/models/prenotazione.dart';
import 'package:park_mg/models/utente.dart';
import 'package:park_mg/utils/theme.dart';
import 'package:park_mg/utils/ui_feedback.dart';
import 'package:park_mg/widgets/prenotazione_dialog.dart';

class GestioneSostaInlineView extends StatefulWidget {
  final Utente utente;
  final ApiClient apiClient;
  final Future<void> Function()? onPaymentCompleted;

  const GestioneSostaInlineView({
    super.key,
    required this.utente,
    required this.apiClient,
    this.onPaymentCompleted,
  });

  @override
  State<GestioneSostaInlineView> createState() =>
      _GestioneSostaInlineViewState();
}

class _GestioneSostaInlineViewState extends State<GestioneSostaInlineView> {
  bool _loading = true;
  bool _paying = false;
  List<PrenotazioneResponse> _attive = [];
  final Map<String, Future<double>> _importiFutures = {};
  final Map<String, Future<String>> _parcheggiNomiFutures = {};

  @override
  void initState() {
    super.initState();
    _loadSoste();
  }

  Future<void> _loadSoste() async {
    if (mounted) {
      setState(() => _loading = true);
    }

    try {
      final storico = await widget.apiClient.getStoricoPrenotazioni(
        widget.utente.id,
      );

      final attive = storico
          .where((p) => p.stato == StatoPrenotazione.parcheggiato)
          .toList();

      if (!mounted) return;

      _importiFutures.clear();
      _parcheggiNomiFutures.clear();

      setState(() {
        _attive = attive;
        _loading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() => _loading = false);
      UiFeedback.showError(context, "Impossibile recuperare le soste: $e");
    }
  }

  Future<double> _getImportoFuture(String prenotazioneId) {
    return _importiFutures.putIfAbsent(
      prenotazioneId,
      () => widget.apiClient.calcolaImporto(prenotazioneId),
    );
  }

  Future<String> _getParcheggioNomeFuture(String parcheggioId) {
    return _parcheggiNomiFutures.putIfAbsent(parcheggioId, () async {
      final json = await widget.apiClient.getParcheggioById(parcheggioId);
      final nome = (json['nome'] ?? '').toString().trim();
      return nome.isNotEmpty ? nome : parcheggioId;
    });
  }

  Future<void> _pagaPrenotazione(PrenotazioneResponse p, double importo) async {
    if (_paying) return;

    setState(() => _paying = true);

    try {
      final aggiornata = await widget.apiClient.pagaPrenotazione(p.id, importo);

      if (!mounted) return;

      UiFeedback.showSuccess(
        context,
        "Pagamento riuscito! Hai 10 minuti per uscire.",
      );

      await PrenotazioneDialog.mostra(
        context,
        prenotazione: aggiornata,
        apiClient: widget.apiClient,
        utenteId: widget.utente.id,
        onCancelled: () {},
        onClosed: () async {
          if (widget.onPaymentCompleted != null) {
            await widget.onPaymentCompleted!();
          }
        },
      );

      if (!mounted) return;
      await _loadSoste();
    } catch (e) {
      if (!mounted) return;
      UiFeedback.showError(context, "Errore pagamento: $e");
    } finally {
      if (mounted) {
        setState(() => _paying = false);
      }
    }
  }

  Future<void> _confermaPagamento(
    PrenotazioneResponse p,
    double importo,
  ) async {
    final confermato = await showDialog<bool>(
      context: context,
      barrierDismissible: !_paying,
      builder: (dialogContext) {
        return AlertDialog(
          backgroundColor: AppColors.bgDark,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(20),
          ),
          title: const Row(
            children: [
              Icon(Icons.warning_amber_rounded, color: Colors.orange, size: 28),
              SizedBox(width: 10),
              Expanded(
                child: Text(
                  "Conferma pagamento",
                  style: TextStyle(
                    color: AppColors.textPrimary,
                    fontWeight: FontWeight.w800,
                    fontSize: 20,
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
                "Sei sicuro di voler pagare ora?",
                style: TextStyle(
                  color: AppColors.textPrimary,
                  fontSize: 15,
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(height: 12),
              const Text(
                "Dalla conferma avrai 10 minuti per uscire dal parcheggio.",
                style: TextStyle(
                  color: AppColors.textMuted,
                  fontSize: 14,
                  fontWeight: FontWeight.w500,
                ),
              ),
              const SizedBox(height: 8),
              const Text(
                "Scaduti i 10 minuti, verrà aggiunta una penale.",
                style: TextStyle(
                  color: Colors.orangeAccent,
                  fontSize: 14,
                  fontWeight: FontWeight.w700,
                ),
              ),
              const SizedBox(height: 16),
              Container(
                width: double.infinity,
                padding: const EdgeInsets.symmetric(
                  horizontal: 14,
                  vertical: 12,
                ),
                decoration: BoxDecoration(
                  color: AppColors.bgDark2.withOpacity(0.5),
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(color: AppColors.borderField),
                ),
                child: Text(
                  "Importo attuale: € ${importo.toStringAsFixed(2)}",
                  style: const TextStyle(
                    color: AppColors.accentCyan,
                    fontSize: 16,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ),
            ],
          ),
          actionsPadding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
          actions: [
            SizedBox(
              height: 46,
              child: OutlinedButton(
                style: OutlinedButton.styleFrom(
                  foregroundColor: AppColors.textPrimary,
                  side: BorderSide(color: AppColors.borderField),
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
                onPressed: _paying
                    ? null
                    : () => Navigator.of(dialogContext).pop(false),
                child: const Text(
                  "Annulla",
                  style: TextStyle(fontWeight: FontWeight.w700),
                ),
              ),
            ),
            SizedBox(
              height: 46,
              child: ElevatedButton.icon(
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.green,
                  foregroundColor: Colors.white,
                  elevation: 0,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                ),
                onPressed: _paying
                    ? null
                    : () => Navigator.of(dialogContext).pop(true),
                icon: const Icon(Icons.payment),
                label: const Text(
                  "Conferma",
                  style: TextStyle(fontWeight: FontWeight.w800),
                ),
              ),
            ),
          ],
        );
      },
    );

    if (confermato == true) {
      await _pagaPrenotazione(p, importo);
    }
  }

  String _formatDateTime(DateTime? dt) {
    if (dt == null) return "—";
    final dd = dt.day.toString().padLeft(2, '0');
    final mm = dt.month.toString().padLeft(2, '0');
    final yyyy = dt.year.toString();
    final hh = dt.hour.toString().padLeft(2, '0');
    final min = dt.minute.toString().padLeft(2, '0');
    return "$dd/$mm/$yyyy • $hh:$min";
  }

  List<Widget> _buildPostoValueWidgets(PrenotazioneResponse p) {
    final posto = p.posto;
    if (posto == null) {
      return const [
        Text(
          "—",
          style: TextStyle(
            color: AppColors.textPrimary,
            fontSize: 13,
            fontWeight: FontWeight.w700,
          ),
        ),
      ];
    }

    return [
      Text(
        "Piano ${posto.piano} · Posto ${posto.numero}",
        style: const TextStyle(
          color: AppColors.textPrimary,
          fontSize: 13,
          fontWeight: FontWeight.w700,
        ),
      ),
      if (posto.riservatoDisabili) ...[
        const SizedBox(width: 8),
        const Icon(Icons.accessible, size: 16, color: AppColors.accentCyan),
      ],
      if (posto.riservatoIncinta) ...[
        const SizedBox(width: 8),
        const Icon(Icons.pregnant_woman, size: 16, color: AppColors.accentCyan),
      ],
    ];
  }

  Widget _buildInfoChip(
    IconData icon,
    String label, {
    String? value,
    List<Widget>? trailingWidgets,
  }) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 11),
      decoration: BoxDecoration(
        color: AppColors.bgDark2.withOpacity(0.42),
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: AppColors.borderField),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Icon(icon, color: AppColors.accentCyan, size: 18),
          const SizedBox(width: 10),
          Expanded(
            child: Wrap(
              crossAxisAlignment: WrapCrossAlignment.center,
              spacing: 4,
              runSpacing: 4,
              children: [
                Text(
                  "$label: ",
                  style: const TextStyle(
                    color: AppColors.textMuted,
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                  ),
                ),
                if (value != null)
                  Text(
                    value,
                    style: const TextStyle(
                      color: AppColors.textPrimary,
                      fontSize: 13,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                if (trailingWidgets != null) ...trailingWidgets,
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStatoBadge(StatoPrenotazione stato) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 7),
      decoration: BoxDecoration(
        color: stato.color.withOpacity(0.14),
        borderRadius: BorderRadius.circular(999),
        border: Border.all(color: stato.color),
      ),
      child: Text(
        stato.label,
        style: TextStyle(
          color: stato.color,
          fontWeight: FontWeight.w800,
          fontSize: 12,
        ),
      ),
    );
  }

  Widget _buildImportoBox(PrenotazioneResponse p) {
    return FutureBuilder<double>(
      future: _getImportoFuture(p.id),
      builder: (context, snapshot) {
        if (snapshot.connectionState == ConnectionState.waiting) {
          return Container(
            width: double.infinity,
            padding: const EdgeInsets.all(18),
            decoration: BoxDecoration(
              color: AppColors.bgDark2.withOpacity(0.35),
              borderRadius: BorderRadius.circular(18),
              border: Border.all(color: AppColors.borderField),
            ),
            child: const Center(
              child: CircularProgressIndicator(color: AppColors.accentCyan),
            ),
          );
        }

        if (snapshot.hasError) {
          return Container(
            width: double.infinity,
            padding: const EdgeInsets.all(18),
            decoration: BoxDecoration(
              color: Colors.red.withOpacity(0.08),
              borderRadius: BorderRadius.circular(18),
              border: Border.all(color: Colors.redAccent),
            ),
            child: const Text(
              "Errore nel calcolo della tariffa.",
              textAlign: TextAlign.center,
              style: TextStyle(
                color: Colors.redAccent,
                fontWeight: FontWeight.w700,
              ),
            ),
          );
        }

        final importo = snapshot.data ?? 0.0;

        return Container(
          width: double.infinity,
          padding: const EdgeInsets.all(18),
          decoration: BoxDecoration(
            gradient: LinearGradient(
              colors: [
                Colors.green.withOpacity(0.18),
                AppColors.accentCyan.withOpacity(0.12),
              ],
            ),
            borderRadius: BorderRadius.circular(18),
            border: Border.all(color: AppColors.accentCyan.withOpacity(0.6)),
          ),
          child: Column(
            children: [
              const Text(
                "Importo da pagare",
                style: TextStyle(
                  color: AppColors.textMuted,
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                "€ ${importo.toStringAsFixed(2)}",
                style: const TextStyle(
                  color: AppColors.accentCyan,
                  fontSize: 34,
                  fontWeight: FontWeight.w900,
                ),
              ),
              const SizedBox(height: 6),
              const Text(
                "Tariffa calcolata in tempo reale",
                textAlign: TextAlign.center,
                style: TextStyle(color: Colors.grey, fontSize: 12),
              ),
              const SizedBox(height: 16),
              SizedBox(
                width: double.infinity,
                height: 52,
                child: ElevatedButton.icon(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.green,
                    foregroundColor: Colors.white,
                    elevation: 0,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(14),
                    ),
                  ),
                  onPressed: _paying
                      ? null
                      : () => _confermaPagamento(p, importo),
                  icon: _paying
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: Colors.white,
                          ),
                        )
                      : const Icon(Icons.payment),
                  label: Text(
                    _paying ? "Pagamento..." : "Paga ora",
                    style: const TextStyle(
                      fontWeight: FontWeight.w800,
                      fontSize: 16,
                    ),
                  ),
                ),
              ),
            ],
          ),
        );
      },
    );
  }

  Widget _buildSostaCard(PrenotazioneResponse p) {
    return FutureBuilder<String>(
      future: _getParcheggioNomeFuture(p.parcheggioId),
      builder: (context, snapshot) {
        final nomeParcheggio = snapshot.data?.trim().isNotEmpty == true
            ? snapshot.data!
            : "Caricamento...";

        return Container(
          padding: const EdgeInsets.all(18),
          decoration: BoxDecoration(
            color: AppColors.bgDark,
            borderRadius: BorderRadius.circular(22),
            border: Border.all(color: AppColors.borderField),
            boxShadow: [
              BoxShadow(
                color: Colors.black.withOpacity(0.22),
                blurRadius: 14,
                offset: const Offset(0, 6),
              ),
            ],
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Container(
                    width: 46,
                    height: 46,
                    decoration: BoxDecoration(
                      color: Colors.orange.withOpacity(0.16),
                      shape: BoxShape.circle,
                    ),
                    child: const Icon(
                      Icons.local_parking,
                      color: Colors.orange,
                      size: 24,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      nomeParcheggio,
                      style: const TextStyle(
                        color: AppColors.textPrimary,
                        fontSize: 18,
                        fontWeight: FontWeight.w800,
                      ),
                    ),
                  ),
                  _buildStatoBadge(p.stato),
                ],
              ),
              const SizedBox(height: 16),
              _buildInfoChip(
                Icons.pin_drop_outlined,
                "Posto",
                trailingWidgets: _buildPostoValueWidgets(p),
              ),
              const SizedBox(height: 10),
              _buildInfoChip(
                Icons.event_available_outlined,
                "Creazione",
                value: _formatDateTime(p.dataCreazione),
              ),
              const SizedBox(height: 10),
              _buildInfoChip(
                Icons.login,
                "Ingresso",
                value: _formatDateTime(p.dataIngresso),
              ),
              const SizedBox(height: 16),
              _buildImportoBox(p),
            ],
          ),
        );
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      color: AppColors.bgDark2,
      child: Column(
        children: [
          Container(
            width: double.infinity,
            padding: const EdgeInsets.fromLTRB(18, 18, 18, 14),
            decoration: BoxDecoration(
              color: AppColors.bgDark,
              border: Border(bottom: BorderSide(color: AppColors.borderField)),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: const [
                Text(
                  'Gestione sosta',
                  style: TextStyle(
                    color: AppColors.textPrimary,
                    fontSize: 24,
                    fontWeight: FontWeight.w900,
                  ),
                ),
              ],
            ),
          ),
          Expanded(
            child: _loading
                ? const Center(
                    child: CircularProgressIndicator(
                      color: AppColors.accentCyan,
                    ),
                  )
                : _attive.isEmpty
                ? const Center(
                    child: Padding(
                      padding: EdgeInsets.all(24),
                      child: Text(
                        'Nessuna sosta parcheggiata trovata.',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          color: AppColors.textPrimary,
                          fontSize: 16,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                  )
                : RefreshIndicator(
                    onRefresh: _loadSoste,
                    child: ListView.separated(
                      padding: const EdgeInsets.all(16),
                      itemCount: _attive.length,
                      separatorBuilder: (_, __) => const SizedBox(height: 16),
                      itemBuilder: (context, index) {
                        final p = _attive[index];
                        return _buildSostaCard(p);
                      },
                    ),
                  ),
          ),
        ],
      ),
    );
  }
}
