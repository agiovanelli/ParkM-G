import 'dart:async';
import 'package:flutter/material.dart';
import 'package:park_mg/api/api_client.dart';
import 'package:qr_flutter/qr_flutter.dart';
import '../models/prenotazione.dart';
import '../utils/theme.dart';

class PrenotazioneDialog {
  static Future<void> mostra(
    BuildContext context, {
    required PrenotazioneResponse prenotazione,
    required ApiClient apiClient,
    required String utenteId,
    required VoidCallback onCancelled,
  }) {
    return showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (_) => _PrenotazioneDialogContent(
        prenotazione: prenotazione,
        apiClient: apiClient,
        utenteId: utenteId,
        onCancelled: onCancelled,
      ),
    );
  }
}

class _PrenotazioneDialogContent extends StatefulWidget {
  final PrenotazioneResponse prenotazione;
  final ApiClient apiClient;
  final String utenteId;
  final VoidCallback onCancelled;

  const _PrenotazioneDialogContent({
    required this.prenotazione,
    required this.apiClient,
    required this.utenteId,
    required this.onCancelled,
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

  @override
  void initState() {
    super.initState();
    if (widget.prenotazione.stato == StatoPrenotazione.ATTIVA) {
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
    );

    if (ok != true) return;

    setState(() => _isCancelling = true);

    try {
      await widget.apiClient.annullaPrenotazione(
        prenotazioneId: widget.prenotazione.id,
        utenteId: widget.utenteId,
      );

      if (!mounted) return;

      widget.onCancelled();

      Navigator.of(context).pop();

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: const Text("Prenotazione annullata."),
          backgroundColor: Colors.redAccent,
          behavior: SnackBarBehavior.floating,
          margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(14),
          ),
        ),
      );
    } on ApiException catch (e) {
      if (!mounted) return;
      setState(() => _isCancelling = false);

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(e.message),
          backgroundColor: Colors.redAccent,
          behavior: SnackBarBehavior.floating,
          margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(14),
          ),
        ),
      );
    } catch (_) {
      if (!mounted) return;
      setState(() => _isCancelling = false);

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: const Text("Errore inatteso durante l'annullamento."),
          backgroundColor: Colors.redAccent,
          behavior: SnackBarBehavior.floating,
          margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(14),
          ),
        ),
      );
    }
  }

  void _calcolaTempoRimanente() {
    if (widget.prenotazione.dataCreazione == null) {
      _remainingTime = Duration.zero;
      return;
    }

    final scadenza = widget.prenotazione.dataCreazione!.add(
      const Duration(minutes: 10),
    );
    final now = DateTime.now();
    final diff = scadenza.difference(now);

    _remainingTime = diff.isNegative ? Duration.zero : diff;
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Dialog(
      backgroundColor: AppColors.bgDark2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
      child: SingleChildScrollView(
        child: Padding(
          padding: const EdgeInsets.fromLTRB(50, 20, 50, 20),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              // Badge dello stato
              _buildStatoBadge(),
              const SizedBox(height: 16),

              // Timer (solo se prenotazione ATTIVA)
              if (widget.prenotazione.stato == StatoPrenotazione.ATTIVA)
                _buildTimer(),

              // Dettagli prenotazione
              _buildDettaglio(
                Icons.directions_car,
                "Parcheggio",
                widget.prenotazione.parcheggioId,
              ),
              _buildDettaglio(
                Icons.event,
                "Creazione",
                _formatDT(widget.prenotazione.dataCreazione),
              ),
              if (widget.prenotazione.dataIngresso != null)
                _buildDettaglio(
                  Icons.login,
                  "Ingresso",
                  _formatDT(widget.prenotazione.dataIngresso),
                ),
              if (widget.prenotazione.dataUscita != null)
                _buildDettaglio(
                  Icons.logout,
                  "Uscita",
                  _formatDT(widget.prenotazione.dataUscita),
                ),

              const SizedBox(height: 16),

              // QR Code (se disponibile)
              if (widget.prenotazione.codiceQr != null &&
                  widget.prenotazione.codiceQr!.isNotEmpty)
                _buildQrCode(),

              if (widget.prenotazione.codiceQr != null &&
                  widget.prenotazione.codiceQr!.isNotEmpty)
                const SizedBox(height: 16),

              // Bottone chiudi
              Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  if (widget.prenotazione.stato ==
                      StatoPrenotazione.ATTIVA) ...[
                    OutlinedButton(
                      onPressed: _isCancelling ? null : _annullaPrenotazione,
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
                          horizontal: 24,
                          vertical: 12,
                        ),
                      ),
                      child: _isCancelling
                          ? const SizedBox(
                              width: 18,
                              height: 18,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : const Text(
                              "Annulla",
                              style: TextStyle(fontWeight: FontWeight.bold),
                            ),
                    ),
                    const SizedBox(width: 12),
                  ],
                  ElevatedButton(
                    onPressed: _isCancelling
                        ? null
                        : () => Navigator.of(context).pop(),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppColors.accentCyan,
                      foregroundColor: Colors.black,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(12),
                      ),
                      padding: const EdgeInsets.symmetric(
                        horizontal: 32,
                        vertical: 12,
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
    );
  }

  Widget _buildStatoBadge() {
    final stato = widget.prenotazione.stato;
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

  Widget _buildQrCode() {
    return Container(
      padding: const EdgeInsets.all(12),
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
          const SizedBox(height: 8),
          QrImageView(
            data: widget.prenotazione.codiceQr!,
            version: QrVersions.auto,
            size: 180.0,
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
