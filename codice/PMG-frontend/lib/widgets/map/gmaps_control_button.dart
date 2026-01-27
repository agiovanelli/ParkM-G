import 'package:flutter/material.dart';

class GMapsControlButton extends StatelessWidget {
  final VoidCallback? onPressed;
  final IconData icon;
  final String? tooltip;
  final bool selected;

  const GMapsControlButton({
    super.key,
    required this.icon,
    required this.onPressed,
    this.tooltip,
    this.selected = false,
  });

  @override
  Widget build(BuildContext context) {
    final Color iconColor = onPressed == null
        ? const Color(0xFFB0B0B0)
        : (selected ? const Color(0xFF4285F4) : const Color(0xFF666666));

    final child = Material(
      color: Colors.white,
      elevation: 2.5,
      shadowColor: Colors.black.withOpacity(0.22),
      shape: CircleBorder(
        side: BorderSide(
          color: selected ? const Color(0x334285F4) : const Color(0x1F000000),
          width: 1,
        ),
      ),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: onPressed,
        child: SizedBox(
          width: 44,
          height: 44,
          child: Icon(icon, size: 22, color: iconColor),
        ),
      ),
    );

    if (tooltip == null) return child;
    return Tooltip(message: tooltip!, child: child);
  }
}
