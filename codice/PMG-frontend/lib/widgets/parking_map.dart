import 'package:flutter/material.dart';
import 'package:park_mg/utils/theme.dart';

class ParkingMap<T> extends StatelessWidget {
  final int selectedFloor;
  final List<int> floors;
  final ValueChanged<int> onFloorChanged;

  final List<T> items;

  final int crossAxisCount;
  final double crossAxisSpacing;
  final double mainAxisSpacing;

  final String Function(T item) tooltipText;
  final String Function(T item) cellText;
  final Color Function(T item) cellColor;

  /// Legenda già pronta (così puoi riusare il tuo _legendItem senza duplicare)
  final List<Widget> legend;

  const ParkingMap({
    super.key,
    required this.selectedFloor,
    required this.floors,
    required this.onFloorChanged,
    required this.items,
    required this.tooltipText,
    required this.cellText,
    required this.cellColor,
    required this.legend,
    this.crossAxisCount = 6,
    this.crossAxisSpacing = 6,
    this.mainAxisSpacing = 6,
  });

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.bgDark2.withOpacity(0.2),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.borderField, width: 1),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                'Mappa Parcheggio',
                style: TextStyle(
                  color: AppColors.textPrimary,
                  fontWeight: FontWeight.w900,
                  fontSize: 16,
                ),
              ),
              DropdownButtonHideUnderline(
                child: DropdownButton<int>(
                  dropdownColor: AppColors.bgDark,
                  value: selectedFloor,
                  style: const TextStyle(color: AppColors.textPrimary),
                  items: floors
                      .map(
                        (f) => DropdownMenuItem<int>(
                          value: f,
                          child: Text('Piano $f'),
                        ),
                      )
                      .toList(),
                  onChanged: (v) {
                    if (v != null) onFloorChanged(v);
                  },
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),

          // Griglia
          GridView.builder(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            itemCount: items.length,
            gridDelegate: SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: crossAxisCount,
              crossAxisSpacing: crossAxisSpacing,
              mainAxisSpacing: mainAxisSpacing,
            ),
            itemBuilder: (_, i) {
              final it = items[i];
              final color = cellColor(it);
              return Tooltip(
                message: tooltipText(it),
                child: Container(
                  decoration: BoxDecoration(
                    color: color.withOpacity(0.25),
                    border: Border.all(color: color, width: 1.5),
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: Center(
                    child: Text(
                      cellText(it),
                      style: const TextStyle(
                        color: AppColors.textPrimary,
                        fontSize: 11,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ),
                ),
              );
            },
          ),

          const SizedBox(height: 16),

          // Legenda
          Wrap(
            spacing: 16,
            runSpacing: 8,
            children: legend,
          ),
        ],
      ),
    );
  }
}
