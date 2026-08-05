import 'package:flutter/material.dart';

enum _FeedbackType {
  success,
  error,
  warning,
  info,
}

class UiFeedback {
  static void showSuccess(BuildContext context, String message) {
    _show(
      context,
      message: message,
      type: _FeedbackType.success,
      duration: const Duration(seconds: 3),
    );
  }

  static void showError(BuildContext context, String message) {
    _show(
      context,
      message: message,
      type: _FeedbackType.error,
      duration: const Duration(seconds: 5),
    );
  }

  static void showWarning(BuildContext context, String message) {
    _show(
      context,
      message: message,
      type: _FeedbackType.warning,
      duration: const Duration(seconds: 4),
    );
  }

  static void showInfo(BuildContext context, String message) {
    _show(
      context,
      message: message,
      type: _FeedbackType.info,
      duration: const Duration(seconds: 3),
    );
  }

  /// Alias mantenuto per compatibilità con il codice esistente.
  static void showToast(BuildContext context, String message) {
    showInfo(context, message);
  }

  static void _show(
    BuildContext context, {
    required String message,
    required _FeedbackType type,
    required Duration duration,
  }) {
    final config = _configFor(type);

    final snackBar = SnackBar(
      behavior: SnackBarBehavior.floating,
      margin: const EdgeInsets.fromLTRB(16, 12, 16, 18),
      padding: EdgeInsets.zero,
      elevation: 18,
      dismissDirection: DismissDirection.horizontal,
      backgroundColor: Colors.white,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: BorderSide(
          color: config.color.withValues(alpha: 0.75),
          width: 1.3,
        ),
      ),
      duration: duration,
      content: IntrinsicHeight(
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Container(
              width: 5,
              decoration: BoxDecoration(
                color: config.color,
                borderRadius: const BorderRadius.horizontal(
                  left: Radius.circular(16),
                ),
              ),
            ),
            Expanded(
              child: Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: 14,
                  vertical: 13,
                ),
                child: Row(
                  children: [
                    Icon(
                      config.icon,
                      color: config.color,
                      size: 24,
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Text(
                        message,
                        style: const TextStyle(
                          color: Color(0xFF111827),
                          fontSize: 14,
                          fontWeight: FontWeight.w700,
                          height: 1.25,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );

    final messenger = ScaffoldMessenger.of(context);
    messenger
      ..hideCurrentSnackBar()
      ..showSnackBar(snackBar);
  }

  static _FeedbackConfig _configFor(_FeedbackType type) {
    switch (type) {
      case _FeedbackType.success:
        return const _FeedbackConfig(
          color: Color(0xFF16A34A),
          icon: Icons.check_circle_outline_rounded,
        );

      case _FeedbackType.error:
        return const _FeedbackConfig(
          color: Color(0xFFDC2626),
          icon: Icons.error_outline_rounded,
        );

      case _FeedbackType.warning:
        return const _FeedbackConfig(
          color: Color(0xFFD97706),
          icon: Icons.warning_amber_rounded,
        );

      case _FeedbackType.info:
        return const _FeedbackConfig(
          color: Color(0xFF0284C7),
          icon: Icons.info_outline_rounded,
        );
    }
  }
}

class _FeedbackConfig {
  final Color color;
  final IconData icon;

  const _FeedbackConfig({
    required this.color,
    required this.icon,
  });
}
