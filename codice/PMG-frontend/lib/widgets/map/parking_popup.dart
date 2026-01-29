import 'package:flutter/material.dart';
import 'package:park_mg/utils/theme.dart';

class ParkingPopup extends StatelessWidget {
  final Map<String, dynamic> parking;
  final VoidCallback onClose;
  final VoidCallback? onBook;
  final bool canBook;
  final bool isLoading;

  const ParkingPopup({
    super.key,
    required this.parking,
    required this.onClose,
    required this.onBook,
    required this.canBook,
    required this.isLoading,
  });

  @override
  Widget build(BuildContext context) {
    final bool inEmergenza = parking['inEmergenza'] == true;
    final bool disabled = inEmergenza || !canBook || isLoading;

    String label;
    if (inEmergenza) {
      label = 'CHIUSO PER EMERGENZA';
    } else if (isLoading) {
      label = 'Prenotazione...';
    } else {
      label = canBook ? 'Prenota parcheggio' : 'Nessun posto disponibile';
    }

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
                parking['nome'] ?? 'Parcheggio',
                style: const TextStyle(
                  color: AppColors.textPrimary,
                  fontSize: 18,
                  fontWeight: FontWeight.bold,
                ),
              ),
              IconButton(
                icon: const Icon(Icons.close, color: AppColors.textPrimary),
                onPressed: onClose,
              ),
            ],
          ),
          const SizedBox(height: 4),
          Text(
            'Area: ${parking['area'] ?? 'N/D'}',
            style: const TextStyle(color: AppColors.textMuted),
          ),
          Text(
            'Posti disponibili: ${parking['postiDisponibili']}/${parking['postiTotali']}',
            style: const TextStyle(color: AppColors.textMuted),
          ),
          const SizedBox(height: 8),

          ElevatedButton.icon(
            onPressed: disabled ? null : onBook,

            // ICONA: se loading -> spinner, altrimenti QR
            icon: isLoading
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.qr_code),

            label: Text(label),

            style: ElevatedButton.styleFrom(
              backgroundColor: inEmergenza
                  ? Colors.redAccent
                  : (disabled
                        ? AppColors.accentCyan.withOpacity(0.55)
                        : AppColors.accentCyan),
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
}
