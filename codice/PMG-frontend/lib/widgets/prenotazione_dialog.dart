import 'dart:async';
import 'package:flutter/material.dart';
import 'package:park_mg/api/api_client.dart';
import 'package:park_mg/utils/ui_feedback.dart';
import 'package:qr_flutter/qr_flutter.dart';
import '../models/prenotazione.dart';
import '../utils/theme.dart';

class PrenotazioneDialog {
  static Future<void> mostra(
    BuildContext context, {
    required PrenotazioneResponse prenotazione,
    required ApiClient apiClient,
    required String utenteId,

    // ✅ rendi opzionali per poterli omettere nel flow "arrivo"
    VoidCallback? onCancelled,
    VoidCallback? onClosed,

    // ✅ nuovo flag: se true, niente bottoni
    bool lockActions = false,
  }) {
    return showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (_) => _PrenotazioneDialogContent(
        prenotazione: prenotazione,
        apiClient: apiClient,
        utenteId: utenteId,
        onCancelled: onCancelled,
        onClosed: onClosed,
        lockActions: lockActions,
      ),
    );
  }
}

class _PrenotazioneDialogContent extends StatefulWidget {
  final PrenotazioneResponse prenotazione;
  final ApiClient apiClient;
  final String utenteId;

  // ✅ ora opzionali
  final VoidCallback? onCancelled;
  final VoidCallback? onClosed;

  // ✅ nuovo
  final bool lockActions;

  const _PrenotazioneDialogContent({
    required this.prenotazione,
    required this.apiClient,
    required this.utenteId,
    this.onCancelled,
    this.onClosed,
    this.lockActions = false,
  });

  @override
  State<_PrenotazioneDialogContent> createState() =>
      _PrenotazioneDialogContentState();
}

class _PrenotazioneDialogContentState
    extends State<_PrenotazioneDialogContent> {
  Timer? _timer;
  Duration _remainingTime = Duration.zero;
  bool _isCancelling = false;
  Timer? _pollTimer;
  late PrenotazioneResponse _current;
  bool _polling = false; // evita doppi start

  @override
  void initState() {
    super.initState();
    _current = widget.prenotazione;
    _startPolling();
    if (_current.stato == StatoPrenotazione.ATTIVA) {
      _calcolaTempoRimanente();
      _timer = Timer.periodic(const Duration(seconds: 1), (timer) {
        setState(() {
          _calcolaTempoRimanente();
        });
        if (_remainingTime.inSeconds <= 0) {
          timer.cancel();
        }
      });
    }
  }

  void _startPolling() {
    if (_polling) return;
    _polling = true;

    _pollTimer = Timer.periodic(const Duration(seconds: 2), (_) async {
      // se dialog non più montato, stop
      if (!mounted) return;

      try {
        final updated = await widget.apiClient.getPrenotazioneByIdFromStorico(
          widget.utenteId,
          _current.id,
        );

        if (!mounted) return;
        if (updated == null) return;

        final changed =
            updated.stato != _current.stato ||
            updated.dataIngresso != _current.dataIngresso ||
            updated.dataUscita != _current.dataUscita;

        if (changed) {
          setState(() => _current = updated);
        }
      } catch (_) {
        // ignora: rete ballerina, ecc.
      }
    });
  }

  void _stopPolling() {
    _pollTimer?.cancel();
    _pollTimer = null;
    _polling = false;
  }

  Future<void> _annullaPrenotazione() async {
    if (_isCancelling) return;

    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.bgDark2,
        title: const Text(
          "Conferma annullamento",
          style: TextStyle(
            color: AppColors.textPrimary,
            fontWeight: FontWeight.bold,
          ),
        ),
        content: const Text(
          "Vuoi annullare questa prenotazione?",
          style: TextStyle(color: AppColors.textPrimary),
        ),
        actions: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 0),
            child: Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                TextButton(
                  onPressed: () => Navigator.of(ctx).pop(false),
                  child: const Text(
                    "No",
                    style: TextStyle(color: AppColors.textMuted),
                  ),
                ),
                ElevatedButton(
                  onPressed: () => Navigator.of(ctx).pop(true),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.redAccent,
                    foregroundColor: Colors.white,
                  ),
                  child: const Text("Sì, annulla"),
                ),
              ],
            ),
          ),
        ],
      ),
    );

    if (ok != true) return;

    setState(() => _isCancelling = true);

    try {
      await widget.apiClient.annullaPrenotazione(
        prenotazioneId: widget.prenotazione.id,
        utenteId: widget.utenteId,
      );
      if (!mounted) return;
      widget.onCancelled?.call();
      Navigator.of(context).pop();
      UiFeedback.showSuccess(context, "Prenotazione annullata.");
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _isCancelling = false);
      UiFeedback.showError(context, e.message);
    } catch (_) {
      if (!mounted) return;
      setState(() => _isCancelling = false);
      UiFeedback.showError(context, "Errore inatteso durante l'annullamento.");
    }
  }

  void _calcolaTempoRimanente() {
    final dc = _current.dataCreazione;
    if (dc == null) {
      _remainingTime = Duration.zero;
      return;
    }

    final scadenza = dc.add(const Duration(minutes: 10));
    final diff = scadenza.difference(DateTime.now());
    _remainingTime = diff.isNegative ? Duration.zero : diff;
  }

  @override
  void dispose() {
    _timer?.cancel();
    _stopPolling();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    const double maxDialogWidth = 300.0;
    final p = _current;

    return Dialog(
      backgroundColor: AppColors.bgDark2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      insetPadding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: maxDialogWidth),
        child: SingleChildScrollView(
          child: Padding(
            padding: const EdgeInsets.fromLTRB(8, 14, 8, 14),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.center,
              children: [
                _buildStatoBadge(p),
                const SizedBox(height: 12),

                if (p.stato == StatoPrenotazione.ATTIVA) _buildTimer(),

                _buildDettaglio(
                  Icons.directions_car,
                  "Parcheggio",
                  p.parcheggioId,
                ),
                _buildDettaglio(
                  Icons.event,
                  "Creazione",
                  _formatDT(p.dataCreazione),
                ),
                if (p.dataIngresso != null)
                  _buildDettaglio(
                    Icons.login,
                    "Ingresso",
                    _formatDT(p.dataIngresso),
                  ),
                if (p.dataUscita != null)
                  _buildDettaglio(
                    Icons.logout,
                    "Uscita",
                    _formatDT(p.dataUscita),
                  ),

                const SizedBox(height: 12),

                if ((p.codiceQr ?? '').isNotEmpty) _buildQrCode(p),

                if ((p.codiceQr ?? '').isNotEmpty) const SizedBox(height: 12),

                if (!widget.lockActions)
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      if (p.stato == StatoPrenotazione.ATTIVA) ...[
                        OutlinedButton(
                          onPressed: _isCancelling
                              ? null
                              : _annullaPrenotazione,
                          style: OutlinedButton.styleFrom(
                            foregroundColor: Colors.redAccent,
                            side: const BorderSide(
                              color: Colors.redAccent,
                              width: 1.6,
                            ),
                            shape: RoundedRectangleBorder(
                              borderRadius: BorderRadius.circular(12),
                            ),
                            padding: const EdgeInsets.symmetric(
                              horizontal: 18,
                              vertical: 10,
                            ),
                          ),
                          child: _isCancelling
                              ? const SizedBox(
                                  width: 18,
                                  height: 18,
                                  child: CircularProgressIndicator(
                                    strokeWidth: 2,
                                  ),
                                )
                              : const Text(
                                  "Annulla",
                                  style: TextStyle(fontWeight: FontWeight.bold),
                                ),
                        ),
                        const SizedBox(width: 10),
                      ],
                      ElevatedButton(
                        onPressed: _isCancelling
                            ? null
                            : () {
                                _timer?.cancel();
                                _stopPolling();
                                Navigator.of(context).pop();
                                widget.onClosed?.call();
                              },
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppColors.accentCyan,
                          foregroundColor: Colors.black,
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12),
                          ),
                          padding: const EdgeInsets.symmetric(
                            horizontal: 22,
                            vertical: 10,
                          ),
                        ),
                        child: const Text(
                          "Chiudi",
                          style: TextStyle(fontWeight: FontWeight.bold),
                        ),
                      ),
                    ],
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildStatoBadge(PrenotazioneResponse p) {
    final stato = p.stato;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      decoration: BoxDecoration(
        color: stato.color.withOpacity(0.15),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: stato.color, width: 2),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(_getStatoIcon(stato), color: stato.color, size: 24),
          const SizedBox(width: 8),
          Text(
            stato.label,
            style: TextStyle(
              color: stato.color,
              fontWeight: FontWeight.bold,
              fontSize: 16,
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTimer() {
    final minutes = _remainingTime.inMinutes;
    final seconds = _remainingTime.inSeconds % 60;
    final isUrgent = _remainingTime.inMinutes < 3;

    return Container(
      margin: const EdgeInsets.only(bottom: 16),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: isUrgent
            ? Colors.red.withOpacity(0.1)
            : AppColors.accentCyan.withOpacity(0.1),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(
          color: isUrgent ? Colors.red : AppColors.accentCyan,
          width: 2,
        ),
      ),
      child: Column(
        children: [
          Text(
            "Tempo rimanente per l'ingresso",
            style: TextStyle(
              color: isUrgent ? Colors.red : AppColors.accentCyan,
              fontSize: 11,
              fontWeight: FontWeight.w600,
            ),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 6),
          Row(
            mainAxisSize: MainAxisSize.min,
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(
                Icons.timer,
                color: isUrgent ? Colors.red : AppColors.accentCyan,
                size: 24,
              ),
              const SizedBox(width: 6),
              Text(
                "${minutes.toString().padLeft(2, '0')}:${seconds.toString().padLeft(2, '0')}",
                style: TextStyle(
                  color: isUrgent ? Colors.red : AppColors.accentCyan,
                  fontSize: 28,
                  fontWeight: FontWeight.bold,
                  fontFeatures: const [FontFeature.tabularFigures()],
                ),
              ),
            ],
          ),
          if (isUrgent && _remainingTime.inSeconds > 0)
            Padding(
              padding: const EdgeInsets.only(top: 6),
              child: Text(
                "Affrettati ad entrare!",
                style: TextStyle(
                  color: Colors.red,
                  fontSize: 11,
                  fontWeight: FontWeight.bold,
                ),
                textAlign: TextAlign.center,
              ),
            ),
          if (_remainingTime.inSeconds <= 0)
            Padding(
              padding: const EdgeInsets.only(top: 6),
              child: Text(
                "Tempo scaduto",
                style: TextStyle(
                  color: Colors.red,
                  fontSize: 11,
                  fontWeight: FontWeight.bold,
                ),
                textAlign: TextAlign.center,
              ),
            ),
        ],
      ),
    );
  }

  Widget _buildDettaglio(IconData icon, String label, String value) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6.0),
      child: Column(
        children: [
          Icon(icon, color: AppColors.accentCyan, size: 18),
          const SizedBox(height: 4),
          Text(
            label,
            style: const TextStyle(color: AppColors.textMuted, fontSize: 11),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 2),
          Text(
            value,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
            style: const TextStyle(
              color: AppColors.textPrimary,
              fontWeight: FontWeight.w600,
              fontSize: 13,
            ),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }

  IconData _getStatoIcon(StatoPrenotazione stato) {
    switch (stato) {
      case StatoPrenotazione.ATTIVA:
        return Icons.access_time;
      case StatoPrenotazione.IN_CORSO:
        return Icons.directions_car;
      case StatoPrenotazione.PAGATO:
        return Icons.payment;
      case StatoPrenotazione.CONCLUSA:
        return Icons.check_circle;
      case StatoPrenotazione.SCADUTA:
        return Icons.event_busy;
      case StatoPrenotazione.ANNULLATA:
        return Icons.cancel;
    }
  }

  Widget _buildQrCode(PrenotazioneResponse p) {
  final code = (p.codiceQr ?? '').trim();

  return Container(
    padding: const EdgeInsets.all(8),
    decoration: BoxDecoration(
      color: Colors.white,
      borderRadius: BorderRadius.circular(12),
      border: Border.all(color: AppColors.accentCyan, width: 2),
    ),
    child: Column(
      children: [
        const Text(
          "Codice QR",
          style: TextStyle(
            color: Colors.black87,
            fontWeight: FontWeight.bold,
            fontSize: 13,
          ),
          textAlign: TextAlign.center,
        ),
        const SizedBox(height: 6),

        GestureDetector(
          onLongPress: () async {
            if (_isCancelling) return;

            final s = p.stato;
            final canCancel =
                s == StatoPrenotazione.ATTIVA ||
                s == StatoPrenotazione.IN_CORSO;

            if (!canCancel) {
              UiFeedback.showError(
                context,
                "Puoi annullare solo se la prenotazione è ATTIVA o IN_CORSO.",
              );
              return;
            }

            await _annullaPrenotazione();
          },
          child: QrImageView(
            data: code, 
            version: QrVersions.auto,
            size: 200.0,
            backgroundColor: Colors.white,
            eyeStyle: const QrEyeStyle(
              eyeShape: QrEyeShape.square,
              color: Colors.black,
            ),
            dataModuleStyle: const QrDataModuleStyle(
              dataModuleShape: QrDataModuleShape.square,
              color: Colors.black,
            ),
          ),
        ),

        const SizedBox(height: 8),

       
        SelectableText(
          code,
          textAlign: TextAlign.center,
          style: const TextStyle(
            fontSize: 11,
            fontFamily: 'monospace',
            color: Colors.black54,
          ),
        ),

        const SizedBox(height: 6),

        const Text(
          "Mostra questo codice all'ingresso",
          style: TextStyle(color: Colors.black54, fontSize: 10),
          textAlign: TextAlign.center,
        ),
      ],
    ),
  );
}


  String _formatDT(DateTime? dt) {
    if (dt == null) return "Data non disponibile";
    final dd = dt.day.toString().padLeft(2, '0');
    final mm = dt.month.toString().padLeft(2, '0');
    final yyyy = dt.year.toString();
    final hh = dt.hour.toString().padLeft(2, '0');
    final min = dt.minute.toString().padLeft(2, '0');
    return "$dd/$mm/$yyyy ore $hh:$min";
  }
}
