// lib/widgets/active_booking_banner.dart
import 'package:flutter/material.dart';
import 'package:park_mg/models/prenotazione.dart';
import 'package:park_mg/utils/theme.dart';

class ActiveBookingBanner extends StatelessWidget {
  const ActiveBookingBanner({
    super.key,
    required this.booking,
    required this.onCheck,
    required this.onDetails,
  });

  final PrenotazioneResponse? booking;
  final VoidCallback onCheck;
  final VoidCallback onDetails;

  @override
  Widget build(BuildContext context) {
    if (booking == null) return const SizedBox.shrink();

    return SafeArea(
      child: Align(
        alignment: Alignment.topCenter,
        child: Padding(
          padding: const EdgeInsets.only(top: 8),
          child: Container(
            constraints: const BoxConstraints(maxWidth: 420),
            height: 56,
            padding: const EdgeInsets.symmetric(horizontal: 14),
            decoration: BoxDecoration(
              color: AppColors.bgDark.withOpacity(0.85),
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: AppColors.borderField),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.25),
                  blurRadius: 12,
                  offset: const Offset(0, 6),
                ),
              ],
            ),
            child: Row(
              children: [
                const Icon(
                  Icons.local_parking,
                  size: 20,
                  color: AppColors.accentCyan,
                ),
                const SizedBox(width: 10),

                const Expanded(
                  child: Text(
                    'Prenotazione attiva',
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      color: AppColors.textPrimary,
                      fontWeight: FontWeight.w700,
                      fontSize: 14,
                    ),
                  ),
                ),

                TextButton(
                  onPressed: onCheck,
                  style: TextButton.styleFrom(
                    foregroundColor: AppColors.accentCyan,
                    padding: const EdgeInsets.symmetric(horizontal: 8),
                  ),
                  child: const Text(
                    'Controlla',
                    style: TextStyle(fontWeight: FontWeight.w700),
                  ),
                ),

                IconButton(
                  onPressed: onDetails,
                  icon: const Icon(Icons.open_in_new),
                  color: AppColors.textMuted,
                  tooltip: 'Dettagli prenotazione',
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
