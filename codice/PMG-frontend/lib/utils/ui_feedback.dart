import 'package:flutter/material.dart';
import 'package:park_mg/utils/theme.dart';

class UiFeedback {
  static void showError(BuildContext context, String msg) {
    _showSnack(
      context,
      msg: msg,
      background: Colors.white,
      textColor: Colors.black,
      leading: const Icon(Icons.error_outline, color: Colors.red, size: 20),
      duration: const Duration(seconds: 4),
    );
  }

  static void showToast(BuildContext context, String msg) {
    _showSnack(
      context,
      msg: msg,
      background: AppColors.bgDark,
      textColor: AppColors.textPrimary,
      leading: const Icon(
        Icons.info_outline,
        color: AppColors.textPrimary,
        size: 18,
      ),
      duration: const Duration(seconds: 3),
    );
  }

  static void showSuccess(BuildContext context, String msg) {
    _showSnack(
      context,
      msg: msg,
      background: AppColors.bgDark,
      textColor: AppColors.textPrimary,
      leading: const Icon(
        Icons.check_circle_outline,
        color: AppColors.accentCyan,
        size: 18,
      ),
      duration: const Duration(seconds: 3),
    );
  }

  static void _showSnack(
    BuildContext context, {
    required String msg,
    required Color background,
    required Color textColor,
    required Widget leading,
    required Duration duration,
  }) {
    final snackBar = SnackBar(
      behavior: SnackBarBehavior.floating,
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
      elevation: 10,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
      backgroundColor: background,
      content: Row(
        children: [
          leading,
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              msg,
              style: TextStyle(color: textColor, fontWeight: FontWeight.w600),
            ),
          ),
        ],
      ),
      duration: duration,
    );

    ScaffoldMessenger.of(context)
      ..hideCurrentSnackBar()
      ..showSnackBar(snackBar);
  }
}
