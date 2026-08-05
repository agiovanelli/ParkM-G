import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:park_mg/indoor/models/indoor_models.dart';
import 'package:park_mg/indoor/parking_map_definition.dart';
import 'package:park_mg/indoor/slot_map.dart';
import 'package:park_mg/utils/theme.dart';

class OperatorParkingImageMap extends StatefulWidget {
  final IndoorMapDefinition definition;
  final int selectedFloor;
  final ValueChanged<int> onFloorChanged;
  final String? selectedSpotId;
  final ValueChanged<String> onSpotTap;
  final ValueChanged<String> onDisableSpot;
  final ValueChanged<String> onEnableSpot;
  final bool showGridDebug;

  const OperatorParkingImageMap({
    super.key,
    required this.definition,
    required this.selectedFloor,
    required this.onFloorChanged,
    required this.selectedSpotId,
    required this.onSpotTap,
    required this.onDisableSpot,
    required this.onEnableSpot,
    this.showGridDebug = false,
  });

  @override
  State<OperatorParkingImageMap> createState() =>
      _OperatorParkingImageMapState();
}

class _OperatorParkingImageMapState extends State<OperatorParkingImageMap> {
  String? _hoveredSpotId;

  @override
  void didUpdateWidget(covariant OperatorParkingImageMap oldWidget) {
    super.didUpdateWidget(oldWidget);

    if (oldWidget.selectedFloor != widget.selectedFloor ||
        oldWidget.definition != widget.definition) {
      _hoveredSpotId = null;
    }
  }

  GeneratedIndoorSlot? _findSlot(
    IndoorFloorLayout layout,
    String? slotId,
  ) {
    if (slotId == null) return null;
    return layout.tryGetSlotById(slotId);
  }

  GeneratedIndoorSlot? _hitTestSlot({
    required Offset position,
    required Size size,
    required IndoorFloorLayout layout,
  }) {
    final projection = _OperatorIsometricProjection(
      layout: layout,
      size: size,
    );

    for (final slot in layout.slots.reversed) {
      if (projection.areaPath(slot.area).contains(position)) {
        return slot;
      }
    }

    return null;
  }

  String _slotStatusLabel(IndoorSlotData slot) {
    if (slot.outOfService) {
      return 'Fuori servizio';
    }

    final status = switch (slot.status) {
      IndoorSlotStatus.free => 'Libero',
      IndoorSlotStatus.reserved => 'Prenotato',
      IndoorSlotStatus.occupied => 'Occupato',
    };

    final type = switch (slot.type) {
      IndoorSlotType.normal => '',
      IndoorSlotType.disabled => ' · Disabili',
      IndoorSlotType.pregnant => ' · Donna incinta',
    };

    return '$status$type';
  }

  @override
  Widget build(BuildContext context) {
    final floorNumbers = widget.definition.floorNumbers;

    if (floorNumbers.isEmpty) {
      return const Center(
        child: Text(
          'Nessun piano disponibile',
          style: TextStyle(color: AppColors.textMuted),
        ),
      );
    }

    final effectiveFloor = floorNumbers.contains(widget.selectedFloor)
        ? widget.selectedFloor
        : floorNumbers.first;

    final layout = widget.definition.layoutForFloor(effectiveFloor);
    final selectedSlot = _findSlot(layout, widget.selectedSpotId);

    final canDisableSelected =
        selectedSlot != null &&
        selectedSlot.data.status == IndoorSlotStatus.free &&
        !selectedSlot.data.outOfService;

    final canEnableSelected =
        selectedSlot != null && selectedSlot.data.outOfService;

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppColors.bgDark2.withValues(alpha: 0.2),
        borderRadius: BorderRadius.circular(18),
        border: Border.all(color: AppColors.borderField, width: 1),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              DropdownButtonHideUnderline(
                child: DropdownButton<int>(
                  dropdownColor: AppColors.bgDark,
                  value: effectiveFloor,
                  style: const TextStyle(color: AppColors.textPrimary),
                  items: floorNumbers
                      .map(
                        (floor) => DropdownMenuItem<int>(
                          value: floor,
                          child: Text('Piano $floor'),
                        ),
                      )
                      .toList(),
                  onChanged: (value) {
                    if (value == null || value == effectiveFloor) return;
                    setState(() => _hoveredSpotId = null);
                    widget.onFloorChanged(value);
                  },
                ),
              ),
              const Spacer(),
              Text(
                '${layout.slots.length} posti',
                style: const TextStyle(
                  color: AppColors.textMuted,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Center(
            child: FractionallySizedBox(
              widthFactor: 0.92,
              child: AspectRatio(
                aspectRatio: 16 / 9,
                child: ClipRRect(
                  borderRadius: BorderRadius.circular(14),
                  child: Container(
                    color: const Color(0xFFF3F4F6),
                    child: LayoutBuilder(
                      builder: (context, constraints) {
                        final size = Size(
                          constraints.maxWidth,
                          constraints.maxHeight,
                        );

                        final hoveredSlot = _findSlot(
                          layout,
                          _hoveredSpotId,
                        );

                        return InteractiveViewer(
                          minScale: 0.8,
                          maxScale: 4,
                          boundaryMargin: const EdgeInsets.all(120),
                          child: SizedBox(
                            width: size.width,
                            height: size.height,
                            child: MouseRegion(
                              cursor: SystemMouseCursors.click,
                              onExit: (_) {
                                if (_hoveredSpotId == null) return;
                                setState(() => _hoveredSpotId = null);
                              },
                              onHover: (event) {
                                final hit = _hitTestSlot(
                                  position: event.localPosition,
                                  size: size,
                                  layout: layout,
                                );

                                final nextId = hit?.data.slotId;
                                if (nextId == _hoveredSpotId) return;

                                setState(() => _hoveredSpotId = nextId);
                              },
                              child: GestureDetector(
                                behavior: HitTestBehavior.opaque,
                                onTapUp: (details) {
                                  final hit = _hitTestSlot(
                                    position: details.localPosition,
                                    size: size,
                                    layout: layout,
                                  );

                                  if (hit != null) {
                                    widget.onSpotTap(hit.data.slotId);
                                  }
                                },
                                child: Stack(
                                  fit: StackFit.expand,
                                  children: [
                                    CustomPaint(
                                      painter: _OperatorDynamicParkingPainter(
                                        layout: layout,
                                        selectedSpotId: widget.selectedSpotId,
                                        hoveredSpotId: _hoveredSpotId,
                                        showGridDebug: widget.showGridDebug,
                                      ),
                                    ),
                                    if (hoveredSlot != null)
                                      Positioned(
                                        left: 12,
                                        top: 12,
                                        child: IgnorePointer(
                                          child: Container(
                                            padding: const EdgeInsets.symmetric(
                                              horizontal: 10,
                                              vertical: 7,
                                            ),
                                            decoration: BoxDecoration(
                                              color: Colors.black.withValues(
                                                alpha: 0.78,
                                              ),
                                              borderRadius:
                                                  BorderRadius.circular(8),
                                            ),
                                            child: Text(
                                              '${hoveredSlot.data.name} · '
                                              '${_slotStatusLabel(hoveredSlot.data)}',
                                              style: const TextStyle(
                                                color: Colors.white,
                                                fontSize: 12,
                                                fontWeight: FontWeight.w700,
                                              ),
                                            ),
                                          ),
                                        ),
                                      ),
                                  ],
                                ),
                              ),
                            ),
                          ),
                        );
                      },
                    ),
                  ),
                ),
              ),
            ),
          ),
          const SizedBox(height: 14),
          if (selectedSlot != null) ...[
            Container(
              width: double.infinity,
              padding: const EdgeInsets.symmetric(
                horizontal: 14,
                vertical: 12,
              ),
              decoration: BoxDecoration(
                color: AppColors.bgDark.withValues(alpha: 0.72),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppColors.borderField),
              ),
              child: Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          selectedSlot.data.name,
                          style: const TextStyle(
                            color: AppColors.textPrimary,
                            fontSize: 15,
                            fontWeight: FontWeight.w800,
                          ),
                        ),
                        const SizedBox(height: 3),
                        Text(
                          _slotStatusLabel(selectedSlot.data),
                          style: const TextStyle(
                            color: AppColors.textMuted,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ],
                    ),
                  ),
                  Text(
                    selectedSlot.data.slotId,
                    style: const TextStyle(
                      color: AppColors.accentCyan,
                      fontWeight: FontWeight.w800,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 12),
          ],
          if (canDisableSelected)
            Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: ElevatedButton.icon(
                onPressed: () {
                  widget.onDisableSpot(selectedSlot!.data.slotId);
                },
                icon: const Icon(Icons.block),
                label: const Text('Metti fuori servizio'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFF6B7280),
                  foregroundColor: Colors.white,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                  padding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 12,
                  ),
                ),
              ),
            ),
          if (canEnableSelected)
            Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: ElevatedButton.icon(
                onPressed: () {
                  widget.onEnableSpot(selectedSlot!.data.slotId);
                },
                icon: const Icon(Icons.check_circle_outline),
                label: const Text('Rimetti in servizio'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: const Color(0xFF10B981),
                  foregroundColor: Colors.white,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                  ),
                  padding: const EdgeInsets.symmetric(
                    horizontal: 16,
                    vertical: 12,
                  ),
                ),
              ),
            ),
          Wrap(
            spacing: 16,
            runSpacing: 8,
            children: [
              _legendItem(const Color(0xFF22C55E), 'Libero'),
              _legendItem(AppColors.accentCyan, 'Disabili'),
              _legendItem(const Color(0xFFF9A8D4), 'Donna incinta'),
              _legendItem(const Color(0xFFF59E0B), 'Prenotato'),
              _legendItem(const Color(0xFFEF4444), 'Occupato'),
              _legendItem(const Color(0xFFFACC15), 'Selezionato'),
              _legendItem(const Color(0xFF9CA3AF), 'Fuori servizio'),
            ],
          ),
        ],
      ),
    );
  }

  Widget _legendItem(Color color, String label) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 18,
          height: 10,
          decoration: BoxDecoration(
            color: color.withValues(alpha: 0.25),
            border: Border.all(color: color, width: 1.5),
            borderRadius: BorderRadius.circular(2),
          ),
        ),
        const SizedBox(width: 6),
        Text(
          label,
          style: const TextStyle(
            color: AppColors.textMuted,
            fontWeight: FontWeight.w700,
            fontSize: 12,
          ),
        ),
      ],
    );
  }
}

class _OperatorDynamicParkingPainter extends CustomPainter {
  final IndoorFloorLayout layout;
  final String? selectedSpotId;
  final String? hoveredSpotId;
  final bool showGridDebug;

  const _OperatorDynamicParkingPainter({
    required this.layout,
    required this.selectedSpotId,
    required this.hoveredSpotId,
    required this.showGridDebug,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final projection = _OperatorIsometricProjection(
      layout: layout,
      size: size,
    );

    _drawFloor(canvas, projection);
    _drawLane(canvas, projection);
    _drawSlots(canvas, projection);
    _drawEntryAndRamp(canvas, projection);

    if (showGridDebug) {
      _drawGrid(canvas, projection);
    }
  }

  void _drawFloor(
    Canvas canvas,
    _OperatorIsometricProjection projection,
  ) {
    final floorPath = projection.areaPath(layout.floorArea);

    canvas.save();
    canvas.translate(14, 18);
    canvas.drawPath(
      floorPath,
      Paint()..color = Colors.black.withValues(alpha: 0.22),
    );
    canvas.restore();

    canvas.drawPath(
      floorPath,
      Paint()..color = const Color(0xFF5D6065),
    );

    canvas.drawPath(
      floorPath,
      Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = 6
        ..strokeJoin = StrokeJoin.round
        ..color = const Color(0xFFD7B128),
    );
  }

  void _drawLane(
    Canvas canvas,
    _OperatorIsometricProjection projection,
  ) {
    final lanePath = projection.areaPath(layout.laneArea);

    canvas.drawPath(
      lanePath,
      Paint()..color = const Color(0xFFCC7B48),
    );

    final laneCenterR = layout.laneArea.r + layout.laneArea.height / 2;
    final start = projection.point(
      layout.laneArea.c + 0.8,
      laneCenterR,
    );
    final end = projection.point(
      layout.laneArea.c + layout.laneArea.width - 0.8,
      laneCenterR,
    );

    canvas.drawLine(
      start,
      end,
      Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2
        ..color = Colors.white.withValues(alpha: 0.55),
    );
  }

  void _drawSlots(
    Canvas canvas,
    _OperatorIsometricProjection projection,
  ) {
    for (final slot in layout.slots) {
      final selected = slot.data.slotId == selectedSpotId;
      final hovered = slot.data.slotId == hoveredSpotId;
      final slotPath = projection.areaPath(slot.area);
      final baseColor = _slotColor(slot.data);

      canvas.drawPath(
        slotPath,
        Paint()
          ..style = PaintingStyle.fill
          ..color = selected
              ? const Color(0xFFFACC15).withValues(alpha: 0.72)
              : baseColor.withValues(alpha: 0.78),
      );

      if (hovered || selected) {
        canvas.drawPath(
          slotPath,
          Paint()
            ..style = PaintingStyle.stroke
            ..strokeWidth = selected ? 9 : 6
            ..strokeJoin = StrokeJoin.round
            ..color = (selected ? const Color(0xFFFACC15) : Colors.white)
                .withValues(alpha: 0.24),
        );
      }

      canvas.drawPath(
        slotPath,
        Paint()
          ..style = PaintingStyle.stroke
          ..strokeWidth = selected ? 3.4 : (hovered ? 2.8 : 1.8)
          ..strokeJoin = StrokeJoin.round
          ..color = selected
              ? const Color(0xFFFACC15)
              : hovered
              ? Colors.white
              : baseColor,
      );

      if (slot.data.status == IndoorSlotStatus.occupied) {
        _drawCar(canvas, projection, slot.area);
      }

      if (slot.data.outOfService) {
        _drawOutOfServiceMark(canvas, projection, slot.area);
      }

      _drawSlotLabel(canvas, projection, slot);
    }
  }

  Color _slotColor(IndoorSlotData slot) {
    if (slot.outOfService) {
      return const Color(0xFF9CA3AF);
    }

    if (slot.status == IndoorSlotStatus.occupied) {
      return const Color(0xFFEF4444);
    }

    if (slot.status == IndoorSlotStatus.reserved) {
      return const Color(0xFFF59E0B);
    }

    return switch (slot.type) {
      IndoorSlotType.disabled => AppColors.accentCyan,
      IndoorSlotType.pregnant => const Color(0xFFF9A8D4),
      IndoorSlotType.normal => const Color(0xFF22C55E),
    };
  }

  void _drawCar(
    Canvas canvas,
    _OperatorIsometricProjection projection,
    GridArea area,
  ) {
    final carPath = projection.areaPathDouble(
      c: area.c + area.width * 0.18,
      r: area.r + area.height * 0.18,
      width: area.width * 0.64,
      height: area.height * 0.64,
    );

    canvas.drawPath(
      carPath,
      Paint()..color = const Color(0xFF263238),
    );

    canvas.drawPath(
      carPath,
      Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.4
        ..color = Colors.white.withValues(alpha: 0.75),
    );
  }

  void _drawOutOfServiceMark(
    Canvas canvas,
    _OperatorIsometricProjection projection,
    GridArea area,
  ) {
    final a = projection.point(area.c + 0.35, area.r + 0.35);
    final b = projection.point(
      area.c + area.width - 0.35,
      area.r + area.height - 0.35,
    );
    final c = projection.point(
      area.c + area.width - 0.35,
      area.r + 0.35,
    );
    final d = projection.point(
      area.c + 0.35,
      area.r + area.height - 0.35,
    );

    final paint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2.4
      ..strokeCap = StrokeCap.round
      ..color = Colors.white.withValues(alpha: 0.9);

    canvas.drawLine(a, b, paint);
    canvas.drawLine(c, d, paint);
  }

  void _drawSlotLabel(
    Canvas canvas,
    _OperatorIsometricProjection projection,
    GeneratedIndoorSlot slot,
  ) {
    final center = projection.point(
      slot.area.c + slot.area.width / 2,
      slot.area.r + slot.area.height / 2,
    );

    final numberPainter = TextPainter(
      text: TextSpan(
        text: slot.data.number.toString(),
        style: const TextStyle(
          color: Colors.white,
          fontSize: 11,
          fontWeight: FontWeight.w900,
        ),
      ),
      textDirection: TextDirection.ltr,
    )..layout();

    numberPainter.paint(
      canvas,
      Offset(
        center.dx - numberPainter.width / 2,
        center.dy - numberPainter.height / 2,
      ),
    );

    final typeSymbol = switch (slot.data.type) {
      IndoorSlotType.normal => null,
      IndoorSlotType.disabled => '♿',
      IndoorSlotType.pregnant => 'M',
    };

    if (typeSymbol == null) return;

    final typePainter = TextPainter(
      text: TextSpan(
        text: typeSymbol,
        style: const TextStyle(
          color: Colors.white,
          fontSize: 8,
          fontWeight: FontWeight.w900,
        ),
      ),
      textDirection: TextDirection.ltr,
    )..layout();

    typePainter.paint(
      canvas,
      Offset(
        center.dx - typePainter.width / 2,
        center.dy + numberPainter.height / 2 - 1,
      ),
    );
  }

  void _drawEntryAndRamp(
    Canvas canvas,
    _OperatorIsometricProjection projection,
  ) {
    _drawMarker(
      canvas,
      projection.cellCenter(layout.entryCell),
      'IN',
      const Color(0xFF66BB6A),
    );

    _drawMarker(
      canvas,
      projection.cellCenter(layout.rampCell),
      'R',
      const Color(0xFFFFA726),
    );
  }

  void _drawMarker(
    Canvas canvas,
    Offset center,
    String label,
    Color color,
  ) {
    canvas.drawCircle(center, 10, Paint()..color = color);
    canvas.drawCircle(
      center,
      13,
      Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = 3
        ..color = Colors.white,
    );

    final textPainter = TextPainter(
      text: TextSpan(
        text: label,
        style: const TextStyle(
          color: Colors.white,
          fontSize: 8,
          fontWeight: FontWeight.w900,
        ),
      ),
      textDirection: TextDirection.ltr,
    )..layout();

    textPainter.paint(
      canvas,
      Offset(
        center.dx - textPainter.width / 2,
        center.dy - textPainter.height / 2,
      ),
    );
  }

  void _drawGrid(
    Canvas canvas,
    _OperatorIsometricProjection projection,
  ) {
    final paint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 0.8
      ..color = Colors.black.withValues(alpha: 0.18);

    for (int c = 0; c <= layout.cols; c++) {
      canvas.drawLine(
        projection.point(c.toDouble(), 0),
        projection.point(c.toDouble(), layout.rows.toDouble()),
        paint,
      );
    }

    for (int r = 0; r <= layout.rows; r++) {
      canvas.drawLine(
        projection.point(0, r.toDouble()),
        projection.point(layout.cols.toDouble(), r.toDouble()),
        paint,
      );
    }
  }

  @override
  bool shouldRepaint(covariant _OperatorDynamicParkingPainter oldDelegate) {
    return oldDelegate.layout != layout ||
        oldDelegate.selectedSpotId != selectedSpotId ||
        oldDelegate.hoveredSpotId != hoveredSpotId ||
        oldDelegate.showGridDebug != showGridDebug;
  }
}

class _OperatorIsometricProjection {
  final IndoorFloorLayout layout;
  final Size size;

  late final double tileWidth;
  late final double tileHeight;
  late final double originX;
  late final double originY;

  _OperatorIsometricProjection({
    required this.layout,
    required this.size,
  }) {
    const padding = 28.0;
    final sum = (layout.cols + layout.rows).toDouble();

    final widthTile = ((size.width - padding * 2) * 2) / sum;
    final heightTile = ((size.height - padding * 2) * 4) / sum;

    tileWidth = math.min(widthTile, heightTile).clamp(4.0, 38.0).toDouble();
    tileHeight = tileWidth / 2;

    final projectedWidth = sum * tileWidth / 2;
    final projectedHeight = sum * tileHeight / 2;

    final left = (size.width - projectedWidth) / 2;
    final top = (size.height - projectedHeight) / 2;

    originX = left + layout.rows * tileWidth / 2;
    originY = top;
  }

  Offset point(double c, double r) {
    return Offset(
      originX + (c - r) * tileWidth / 2,
      originY + (c + r) * tileHeight / 2,
    );
  }

  Offset cellCenter(SlotCell2D cell) {
    return point(cell.c + 0.5, cell.r + 0.5);
  }

  Path areaPath(GridArea area) {
    return areaPathDouble(
      c: area.c.toDouble(),
      r: area.r.toDouble(),
      width: area.width.toDouble(),
      height: area.height.toDouble(),
    );
  }

  Path areaPathDouble({
    required double c,
    required double r,
    required double width,
    required double height,
  }) {
    final topLeft = point(c, r);
    final topRight = point(c + width, r);
    final bottomRight = point(c + width, r + height);
    final bottomLeft = point(c, r + height);

    return Path()
      ..moveTo(topLeft.dx, topLeft.dy)
      ..lineTo(topRight.dx, topRight.dy)
      ..lineTo(bottomRight.dx, bottomRight.dy)
      ..lineTo(bottomLeft.dx, bottomLeft.dy)
      ..close();
  }
}
