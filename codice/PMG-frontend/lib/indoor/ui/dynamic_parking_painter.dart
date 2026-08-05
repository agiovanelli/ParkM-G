import 'dart:math' as math;

import 'package:flutter/material.dart';

import '../models/indoor_models.dart';
import '../slot_map.dart';

class DynamicParkingPainter extends CustomPainter {
  final IndoorFloorLayout layout;
  final List<SlotCell2D> path;
  final String? targetSlotId;
  final double targetAnimation;
  final double userAnimation;
  final bool showGridDebug;

  DynamicParkingPainter({
    required this.layout,
    required this.path,
    required this.targetSlotId,
    required this.targetAnimation,
    required this.userAnimation,
    required this.showGridDebug,
  });

  @override
  void paint(Canvas canvas, Size size) {
    final projection = _IsometricProjection(
      layout: layout,
      size: size,
    );

    _drawFloor(canvas, projection);
    _drawLane(canvas, projection);
    _drawSlots(canvas, projection);
    _drawEntryAndRamp(canvas, projection);
    _drawRoute(canvas, projection);
    _drawUser(canvas, projection);

    if (showGridDebug) {
      _drawGrid(canvas, projection);
    }
  }

  void _drawFloor(Canvas canvas, _IsometricProjection projection) {
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

  void _drawLane(Canvas canvas, _IsometricProjection projection) {
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

  void _drawSlots(Canvas canvas, _IsometricProjection projection) {
    for (final slot in layout.slots) {
      final isTarget = slot.data.slotId == targetSlotId;
      final slotPath = projection.areaPath(slot.area);

      canvas.drawPath(
        slotPath,
        Paint()..color = _slotFill(slot.data),
      );

      final pulse = isTarget ? 2.0 + targetAnimation * 3.0 : 0.0;

      canvas.drawPath(
        slotPath,
        Paint()
          ..style = PaintingStyle.stroke
          ..strokeWidth = isTarget ? 3.5 + pulse : 1.8
          ..strokeJoin = StrokeJoin.round
          ..color = isTarget
              ? const Color(0xFF00E676)
              : Colors.white.withValues(alpha: 0.92),
      );

      if (slot.data.status == IndoorSlotStatus.occupied) {
        _drawCar(canvas, projection, slot.area);
      }

      _drawSlotLabel(canvas, projection, slot);
    }
  }

  Color _slotFill(IndoorSlotData slot) {
    if (slot.outOfService) {
      return const Color(0xFF616161);
    }

    if (slot.status == IndoorSlotStatus.occupied) {
      return const Color(0xFFB63D32);
    }

    if (slot.status == IndoorSlotStatus.reserved) {
      return const Color(0xFFFFB74D);
    }

    return switch (slot.type) {
      IndoorSlotType.disabled => const Color(0xFF42A5F5),
      IndoorSlotType.pregnant => const Color(0xFFEC7EB7),
      IndoorSlotType.normal => const Color(0xFF69B9A9),
    };
  }

  void _drawCar(
    Canvas canvas,
    _IsometricProjection projection,
    GridArea slotArea,
  ) {
    final insetC = slotArea.width * 0.18;
    final insetR = slotArea.height * 0.18;

    final carPath = projection.areaPathDouble(
      c: slotArea.c + insetC,
      r: slotArea.r + insetR,
      width: slotArea.width - insetC * 2,
      height: slotArea.height - insetR * 2,
    );

    canvas.drawPath(
      carPath,
      Paint()..color = const Color(0xFF263238),
    );

    canvas.drawPath(
      carPath,
      Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = 1.5
        ..color = Colors.white.withValues(alpha: 0.75),
    );
  }

  void _drawSlotLabel(
    Canvas canvas,
    _IsometricProjection projection,
    GeneratedIndoorSlot slot,
  ) {
    final center = projection.point(
      slot.area.c + slot.area.width / 2,
      slot.area.r + slot.area.height / 2,
    );

    final symbol = switch (slot.data.type) {
      IndoorSlotType.disabled => '♿',
      IndoorSlotType.pregnant => 'M',
      IndoorSlotType.normal => slot.data.number.toString(),
    };

    final textPainter = TextPainter(
      text: TextSpan(
        text: symbol,
        style: const TextStyle(
          color: Colors.white,
          fontSize: 11,
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

  void _drawEntryAndRamp(
    Canvas canvas,
    _IsometricProjection projection,
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
    canvas.drawCircle(
      center,
      11,
      Paint()..color = color,
    );

    canvas.drawCircle(
      center,
      14,
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
          fontSize: 9,
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

  void _drawRoute(Canvas canvas, _IsometricProjection projection) {
    if (path.length < 2) return;

    final first = projection.cellCenter(path.first);
    final route = Path()..moveTo(first.dx, first.dy);

    for (final cell in path.skip(1)) {
      final point = projection.cellCenter(cell);
      route.lineTo(point.dx, point.dy);
    }

    canvas.drawPath(
      route,
      Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = 11
        ..strokeCap = StrokeCap.round
        ..strokeJoin = StrokeJoin.round
        ..color = Colors.black.withValues(alpha: 0.30),
    );

    canvas.drawPath(
      route,
      Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = 6
        ..strokeCap = StrokeCap.round
        ..strokeJoin = StrokeJoin.round
        ..shader = const LinearGradient(
          colors: [Color(0xFF00B0FF), Color(0xFF7C4DFF)],
        ).createShader(Offset.zero & projection.size),
    );
  }

  void _drawUser(Canvas canvas, _IsometricProjection projection) {
    if (path.isEmpty) return;

    final position = _pointOnPath(
      projection: projection,
      t: userAnimation,
    );

    canvas.drawCircle(
      position,
      10,
      Paint()..color = const Color(0xFF00E676),
    );

    canvas.drawCircle(
      position,
      14,
      Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = 4
        ..color = Colors.white,
    );
  }

  Offset _pointOnPath({
    required _IsometricProjection projection,
    required double t,
  }) {
    if (path.length == 1) {
      return projection.cellCenter(path.first);
    }

    final points = path.map(projection.cellCenter).toList();
    double total = 0;

    for (int index = 1; index < points.length; index++) {
      total += (points[index] - points[index - 1]).distance;
    }

    if (total <= 0) {
      return points.first;
    }

    double remaining = t.clamp(0.0, 1.0).toDouble() * total;

    for (int index = 1; index < points.length; index++) {
      final a = points[index - 1];
      final b = points[index];
      final segment = (b - a).distance;

      if (segment <= 0) continue;

      if (remaining <= segment) {
        final localT = remaining / segment;
        return Offset.lerp(a, b, localT)!;
      }

      remaining -= segment;
    }

    return points.last;
  }

  void _drawGrid(Canvas canvas, _IsometricProjection projection) {
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
  bool shouldRepaint(covariant DynamicParkingPainter oldDelegate) {
    return oldDelegate.layout != layout ||
        oldDelegate.path != path ||
        oldDelegate.targetSlotId != targetSlotId ||
        oldDelegate.targetAnimation != targetAnimation ||
        oldDelegate.userAnimation != userAnimation ||
        oldDelegate.showGridDebug != showGridDebug;
  }
}

class _IsometricProjection {
  final IndoorFloorLayout layout;
  final Size size;

  late final double tileWidth;
  late final double tileHeight;
  late final double originX;
  late final double originY;

  _IsometricProjection({
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
