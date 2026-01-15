import 'package:flutter/material.dart';
import 'package:qr_flutter/qr_flutter.dart';
import '../models/prenotazione.dart';
import '../utils/theme.dart';

class PrenotazioneDialog {
  static void mostra(BuildContext context, PrenotazioneResponse risposta) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppColors.bgDark,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
        title: const Column(
          children: [
            Icon(Icons.check_circle, color: Colors.green, size: 50),
            SizedBox(height: 10),
            Text(
              "Dettaglio Prenotazione",
              style: TextStyle(color: AppColors.textPrimary, fontWeight: FontWeight.bold),
            ),
          ],
        ),
        content: SizedBox(
          width: 250,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              Text(
                "ID Parcheggio: ${risposta.parcheggioId}",
                style: const TextStyle(color: AppColors.textMuted, fontSize: 13),
              ),
              const SizedBox(height: 20),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(16),
                ),
                child: QrImageView(
                  data: risposta.codiceQr,
                  version: QrVersions.auto,
                  size: 180.0,
                ),
              ),
              const SizedBox(height: 10),
              SelectableText(
                risposta.codiceQr,
                style: const TextStyle(color: AppColors.accentCyan, fontSize: 10),
              ),
            ],
          ),
        ),
        actions: [
          Center(
            child: TextButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: const Text("CHIUDI", style: TextStyle(color: AppColors.textPrimary)),
            ),
          ),
        ],
      ),
    );
  }
}