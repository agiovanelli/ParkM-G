import 'package:flutter/material.dart';
import 'package:park_mg/utils/ui_feedback.dart';
import '../api/api_client.dart';
import '../models/prenotazione.dart';
import '../models/utente.dart';
import '../utils/theme.dart';
import '../widgets/prenotazione_dialog.dart';

class HistoryScreen extends StatefulWidget {
  final Utente utente;
  final ApiClient apiClient;
  final VoidCallback onBookingCancelled;

  const HistoryScreen({
    super.key,
    required this.utente,
    required this.apiClient,
    required this.onBookingCancelled,
  });

  @override
  State<HistoryScreen> createState() => _HistoryScreenState();
}

class _HistoryScreenState extends State<HistoryScreen> {
  late Future<List<PrenotazioneResponse>> _storicoFuture;
  bool _errorFeedbackShown = false;

  @override
  void initState() {
    super.initState();
    _storicoFuture = _caricaEOrdinaStorico();
  }

  void _reloadHistory() {
    setState(() {
      _errorFeedbackShown = false;
      _storicoFuture = _caricaEOrdinaStorico();
    });
  }

  Future<List<PrenotazioneResponse>> _caricaEOrdinaStorico() async {
    final storico = await widget.apiClient.getStoricoPrenotazioni(
      widget.utente.id,
    );

    storico.sort((a, b) {
      if (a.dataCreazione == null && b.dataCreazione == null) return 0;
      if (a.dataCreazione == null) return 1;
      if (b.dataCreazione == null) return -1;
      return b.dataCreazione!.compareTo(a.dataCreazione!);
    });

    return storico;
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppColors.bgDark,
      appBar: AppBar(
        title: const Text(
          "Le mie prenotazioni",
          style: TextStyle(fontWeight: FontWeight.bold),
        ),
        backgroundColor: AppColors.bgDark2,
        foregroundColor: AppColors.textPrimary,
        elevation: 0,
      ),
      body: FutureBuilder<List<PrenotazioneResponse>>(
        future: _storicoFuture,
        builder: (context, snapshot) {
          if (snapshot.connectionState == ConnectionState.waiting) {
            return const Center(
              child: CircularProgressIndicator(color: AppColors.accentCyan),
            );
          } else if (snapshot.hasError) {
            if (!_errorFeedbackShown) {
              _errorFeedbackShown = true;
              WidgetsBinding.instance.addPostFrameCallback((_) {
                if (!mounted) return;
                UiFeedback.showError(
                  context,
                  'Errore nel caricamento dello storico.',
                );
              });
            }

            return Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(
                      Icons.error_outline,
                      color: Color(0xFFEF4444),
                      size: 42,
                    ),
                    const SizedBox(height: 12),
                    const Text(
                      'Errore nel caricamento dello storico.',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        color: AppColors.textPrimary,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    const SizedBox(height: 16),
                    OutlinedButton.icon(
                      onPressed: _reloadHistory,
                      icon: const Icon(Icons.refresh_rounded),
                      label: const Text('Riprova'),
                    ),
                  ],
                ),
              ),
            );
          } else if (!snapshot.hasData || snapshot.data!.isEmpty) {
            return const Center(
              child: Text(
                "Non hai ancora effettuato prenotazioni.",
                style: TextStyle(color: AppColors.textMuted),
              ),
            );
          }

          final storico = snapshot.data!;

          return ListView.builder(
            padding: const EdgeInsets.all(16),
            itemCount: storico.length,
            itemBuilder: (context, index) {
              final p = storico[index];
              return Card(
                color: AppColors.bgDark2,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(15),
                ),
                margin: const EdgeInsets.only(bottom: 12),
                child: ListTile(
                  contentPadding: const EdgeInsets.symmetric(
                    horizontal: 20,
                    vertical: 10,
                  ),
                  leading: const Icon(
                    Icons.directions_car,
                    color: AppColors.accentCyan,
                  ),
                  title: Row(
                    children: [
                      Expanded(
                        child: Text(
                          "Parcheggio: ${p.parcheggioId}",
                          style: const TextStyle(
                            color: AppColors.textPrimary,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                      _coloreStato(p.stato),
                    ],
                  ),
                  subtitle: Text(
                    'Data: ${formatDT(p.dataCreazione)}',
                    style: const TextStyle(color: AppColors.textMuted),
                  ),
                  trailing: const Icon(
                    Icons.chevron_right,
                    color: AppColors.textMuted,
                  ),
                  onTap: () async {
                    try {
                      final updated = await widget.apiClient
                          .getPrenotazioneByIdFromStorico(
                            widget.utente.id,
                            p.id,
                          );

                      if (!context.mounted) return;

                      await PrenotazioneDialog.mostra(
                        context,
                        prenotazione: updated ?? p,
                        apiClient: widget.apiClient,
                        utenteId: widget.utente.id,
                        onCancelled: () {
                          _reloadHistory();
                          widget.onBookingCancelled();
                        },
                        onClosed: () {},
                      );
                    } catch (_) {
                      if (!context.mounted) return;
                      UiFeedback.showError(
                        context,
                        'Impossibile aprire la prenotazione.',
                      );
                    }
                  },
                ),
              );
            },
          );
        },
      ),
    );
  }

  String formatDT(DateTime? dt) {
    if (dt == null) return "Data non disponibile";
    final dd = dt.day.toString().padLeft(2, '0');
    final mm = dt.month.toString().padLeft(2, '0');
    final yyyy = dt.year.toString();
    final hh = dt.hour.toString().padLeft(2, '0');
    final min = dt.minute.toString().padLeft(2, '0');
    return "$dd/$mm/$yyyy ore $hh:$min";
  }

  Widget _coloreStato(StatoPrenotazione stato) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
      decoration: BoxDecoration(
        color: stato.color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(8),
        border: Border.all(color: stato.color),
      ),
      child: Text(
        stato.label,
        style: TextStyle(
          color: stato.color,
          fontWeight: FontWeight.bold,
          fontSize: 12,
        ),
      ),
    );
  }
}
