import 'dart:async';

import 'package:flutter/material.dart';
import 'package:park_mg/indoor/graph/lane_grid_mask.dart';
import '../models/indoor_models.dart';
import '../parking_map_definition.dart';

class IndoorParkingView extends StatefulWidget {
  final IndoorMapDefinition def;
  final IndoorAssignment assignment;
  final int userFloor;
  final bool showGridDebug;

  const IndoorParkingView({
    super.key,
    required this.def,
    required this.assignment,
    this.userFloor = 1,
    this.showGridDebug = false,
  });

  @override
  State<IndoorParkingView> createState() => _IndoorParkingViewState();
}

class _IndoorParkingViewState extends State<IndoorParkingView>
    with SingleTickerProviderStateMixin {
  late int _floor;
  List<Offset> _path = const [];
  double? _imgAspect;
  int _stepIndex = 0;
  late final AnimationController _targetCtrl;
  late final Animation<double> _targetT;

  List<int> get _steps {
    final targetFloor = widget.assignment.slot.floor;
    final from = widget.userFloor;

    if (targetFloor == from) return [from];
    if (targetFloor > from)
      return [for (int f = from; f <= targetFloor; f++) f];
    return [for (int f = from; f >= targetFloor; f--) f];
  }

  @override
  void initState() {
    super.initState();
    _stepIndex = 0;
    _floor = 1;
    _recompute();

    _targetCtrl = AnimationController(
      vsync: this,
      duration: const Duration(milliseconds: 900),
    )..repeat(reverse: true);

    _targetT = CurvedAnimation(parent: _targetCtrl, curve: Curves.easeInOut);

    WidgetsBinding.instance.addPostFrameCallback((_) async {
      final a = await _loadAssetAspect(widget.def.floorAsset);
      if (!mounted) return;
      setState(() => _imgAspect = a);
    });
  }

  @override
  void dispose() {
    _targetCtrl.dispose();
    super.dispose();
  }

  Future<double?> _loadAssetAspect(String assetPath) async {
    try {
      final img = AssetImage(assetPath);
      final stream = img.resolve(const ImageConfiguration());
      final completer = Completer<ImageInfo>();
      late final ImageStreamListener listener;

      listener = ImageStreamListener(
        (info, _) {
          completer.complete(info);
          stream.removeListener(listener);
        },
        onError: (e, _) {
          stream.removeListener(listener);
          completer.completeError(e);
        },
      );

      stream.addListener(listener);
      final info = await completer.future;
      return info.image.width / info.image.height;
    } catch (_) {
      return null;
    }
  }

  @override
  void didUpdateWidget(covariant IndoorParkingView oldWidget) {
    super.didUpdateWidget(oldWidget);

    final changed =
        oldWidget.assignment.slot.slotId != widget.assignment.slot.slotId ||
        oldWidget.assignment.slot.floor != widget.assignment.slot.floor ||
        oldWidget.userFloor != widget.userFloor;

    if (changed) {
      final slotChanged =
          oldWidget.assignment.slot.slotId != widget.assignment.slot.slotId ||
          oldWidget.assignment.slot.floor != widget.assignment.slot.floor;

      if (slotChanged) {
        _stepIndex = 0;
        _floor = 1;
      }
      _recompute();
    }
  }

  void _recompute() {
    final steps = _steps;
    final shownFloor = steps[_stepIndex];
    _floor = shownFloor;
    final targetFloor = widget.assignment.slot.floor;

    if (shownFloor != targetFloor) {
      final uCells = LaneGridMask.buildUPathCells(
        colLeft: 9,
        colRight: 29,
        rowTop: 0,
        rowBottom: 24,
      );

      final startU = (LaneGridMask.entryCol, LaneGridMask.entryRow);
      int startIdx = uCells.indexWhere(
        (e) => e.$1 == startU.$1 && e.$2 == startU.$2,
      );
      if (startIdx < 0) startIdx = 0;

      final pts = <Offset>[];
      for (int i = startIdx; i < uCells.length; i++) {
        final (c, r) = uCells[i];
        pts.add(LaneGridMask.cellCenterToNormalized(c, r));
      }

      setState(() => _path = pts);
      return;
    }

    final (gc0, gr0) = LaneGridMask.pointToCell(
      widget.assignment.slotPoint.toOffset(),
    );
    final gc = gc0.clamp(0, LaneGridMask.cols - 1);
    final gr = gr0.clamp(0, LaneGridMask.rows - 1);
    final sc = LaneGridMask.entryCol;
    final sr = LaneGridMask.entryRow;
    final pts = LaneGridMask.buildTurnAtGoalRowPathNormalized(
      startC: sc,
      startR: sr,
      goalC: gc,
      goalR: gr,
    );

    setState(() => _path = pts);
  }

  @override
  Widget build(BuildContext context) {
    final slot = widget.assignment.slot;
    final steps = _steps;
    final shownFloor = steps[_stepIndex];
    final totalSteps = steps.length;

    return Column(
      children: [
        SizedBox(
          height: 40,
          child: Stack(
            alignment: Alignment.center,
            children: [
              Align(
                alignment: Alignment.centerLeft,
                child: const Icon(Icons.directions, color: Colors.white),
              ),

              Text(
                'Vai al posto ${slot.slotId} (Piano ${slot.floor})',
                textAlign: TextAlign.center,
                style: const TextStyle(
                  color: Colors.white,
                  fontWeight: FontWeight.w800,
                  fontSize: 16,
                ),
              ),
            ],
          ),
        ),
        Row(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            IconButton(
              onPressed: _stepIndex > 0
                  ? () {
                      setState(() => _stepIndex--);
                      _recompute();
                    }
                  : null,
              icon: const Icon(Icons.chevron_left, color: Colors.white),
            ),
            Text(
              'Step ${_stepIndex + 1}/$totalSteps • Piano $shownFloor',
              style: const TextStyle(
                color: Colors.white70,
                fontWeight: FontWeight.w700,
              ),
            ),
            IconButton(
              onPressed: _stepIndex < totalSteps - 1
                  ? () {
                      setState(() => _stepIndex++);
                      _recompute();
                    }
                  : null,
              icon: const Icon(Icons.chevron_right, color: Colors.white),
            ),
          ],
        ),
        const SizedBox(height: 10),
        Expanded(
          child: ClipRRect(
            borderRadius: BorderRadius.circular(18),
            child: Container(
              color: Colors.white,
              child: Center(
                child: AspectRatio(
                  aspectRatio: _imgAspect ?? (16 / 9),
                  child: InteractiveViewer(
                    minScale: 1,
                    maxScale: 4,
                    boundaryMargin: const EdgeInsets.all(24),
                    child: Stack(
                      fit: StackFit.expand,
                      children: [
                        Image.asset(widget.def.floorAsset, fit: BoxFit.contain),
                        AnimatedBuilder(
                          animation: _targetT,
                          builder: (_, __) {
                            return CustomPaint(
                              painter: _IndoorOverlayPainter(
                                imageAspect: _imgAspect ?? (16 / 9),
                                path: _path,
                                target: widget.assignment.slotPoint.toOffset(),
                                showTarget: _floor == slot.floor,
                                showUser: _floor == widget.userFloor,
                                showGridDebug: widget.showGridDebug,
                                targetAnimT: _targetT.value,
                              ),
                            );
                          },
                        ),
                      ],
                    ),
                  ),
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class _IndoorOverlayPainter extends CustomPainter {
  final List<Offset> path;
  final Offset target;
  final bool showTarget;
  final bool showUser;
  final bool showGridDebug;
  final double imageAspect;
  final double targetAnimT;

  _IndoorOverlayPainter({
    required this.path,
    required this.target,
    required this.showTarget,
    required this.showUser,
    required this.showGridDebug,
    required this.imageAspect,
    required this.targetAnimT,
  });

  Offset _px(Offset n, Size s) {
    final r = _imageRect(s);
    return Offset(r.left + n.dx * r.width, r.top + n.dy * r.height);
  }

  Rect _imageRect(Size size) {
    final dstW = size.width;
    final dstH = size.height;

    final dstAspect = dstW / dstH;
    final srcAspect = imageAspect;

    double w, h;
    if (srcAspect > dstAspect) {
      w = dstW;
      h = w / srcAspect;
    } else {
      h = dstH;
      w = h * srcAspect;
    }

    final left = (dstW - w) / 2.0;
    final top = (dstH - h) / 2.0;
    return Rect.fromLTWH(left, top, w, h);
  }

  @override
  void paint(Canvas canvas, Size size) {
    if (path.length >= 2) {
      final p0 = _px(path.first, size);
      final dp = Path()..moveTo(p0.dx, p0.dy);
      for (int i = 1; i < path.length; i++) {
        final pi = _px(path[i], size);
        dp.lineTo(pi.dx, pi.dy);
      }

      final glow = Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = 12
        ..strokeCap = StrokeCap.round
        ..color = Colors.black.withOpacity(0.30);

      final line = Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = 7
        ..strokeCap = StrokeCap.round
        ..shader = const LinearGradient(
          begin: Alignment.centerLeft,
          end: Alignment.centerRight,
          colors: [Color(0xFF00B0FF), Color(0xFF7C4DFF)],
        ).createShader(Offset.zero & size);

      canvas.drawPath(dp, glow);
      canvas.drawPath(dp, line);
    }

    if (showUser) {
      final entry = LaneGridMask.entryPointNormalized();
      final u = _px(entry, size);

      final fill = Paint()..color = const Color(0xFF00E676);
      final ring = Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = 4
        ..color = Colors.white.withOpacity(0.95);

      canvas.drawCircle(u, 10, fill);
      canvas.drawCircle(u, 14, ring);
    }

    if (showTarget) {
      final (gc, gr) = LaneGridMask.pointToCell(target);

      final polyN = LaneGridMask.cellBlockPolygonNormalized(
        c: gc,
        r: gr,
        halfCols: 2,
        halfRows: 1,
      );

      final p0 = _px(polyN[0], size);
      final p1 = _px(polyN[1], size);
      final p2 = _px(polyN[2], size);
      final p3 = _px(polyN[3], size);
      final dy = (-8.0) * (0.5 - (targetAnimT - 0.5).abs()) * 2.0;

      final pathPoly = Path()
        ..moveTo(p0.dx, p0.dy + dy)
        ..lineTo(p1.dx, p1.dy + dy)
        ..lineTo(p2.dx, p2.dy + dy)
        ..lineTo(p3.dx, p3.dy + dy)
        ..close();

      const green = Color(0xFF00E676);

      // glow verde (al posto del nero)
      final glow = Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = 12
        ..strokeJoin = StrokeJoin.round
        ..color = green.withOpacity(0.28);

      // riempimento verde più deciso
      final fill = Paint()
        ..style = PaintingStyle.fill
        ..color = green.withOpacity(0.35);

      // bordo verde pieno
      final stroke = Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = 4
        ..strokeJoin = StrokeJoin.round
        ..color = green.withOpacity(0.95);

      canvas.drawPath(pathPoly, glow);
      canvas.drawPath(pathPoly, fill);
      canvas.drawPath(pathPoly, stroke);
    }

    if (showGridDebug) {
      _drawGridDebug(canvas, size);
    }
  }

  void _drawGridDebug(Canvas canvas, Size size) {
    final tl = _px(LaneGridMask.gridTL, size);
    final tr = _px(LaneGridMask.gridTR, size);
    final bl = _px(LaneGridMask.gridBL, size);

    final br = tr + (bl - tl);

    final border = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2
      ..color = Colors.black.withOpacity(0.35);

    final quad = Path()
      ..moveTo(tl.dx, tl.dy)
      ..lineTo(tr.dx, tr.dy)
      ..lineTo(br.dx, br.dy)
      ..lineTo(bl.dx, bl.dy)
      ..close();

    canvas.drawPath(quad, border);

    void handle(Offset p, Color c, String label) {
      final fill = Paint()..color = c;
      final ring = Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = 3
        ..color = Colors.white.withOpacity(0.95);

      canvas.drawCircle(p, 7, fill);
      canvas.drawCircle(p, 11, ring);

      final tp = TextPainter(
        text: TextSpan(
          text: label,
          style: TextStyle(
            color: Colors.black.withOpacity(0.75),
            fontSize: 12,
            fontWeight: FontWeight.w800,
          ),
        ),
        textDirection: TextDirection.ltr,
      )..layout();

      tp.paint(canvas, p + const Offset(10, -18));
    }

    handle(tl, Colors.red, 'TL');
    handle(tr, Colors.blue, 'TR');
    handle(bl, Colors.green, 'BL');
    handle(br, Colors.orange, 'BR');

    final u = (tr - tl) / LaneGridMask.cols.toDouble();
    final v = (bl - tl) / LaneGridMask.rows.toDouble();

    Offset pCell(int c, int r) => tl + u * c.toDouble() + v * r.toDouble();

    final fill = Paint()
      ..style = PaintingStyle.fill
      ..color = Colors.purple.withOpacity(0.10);

    for (int r = 0; r < LaneGridMask.rows; r++) {
      for (int c = 0; c < LaneGridMask.cols; c++) {
        final p00 = pCell(c, r);
        final p10 = pCell(c + 1, r);
        final p11 = pCell(c + 1, r + 1);
        final p01 = pCell(c, r + 1);

        final poly = Path()
          ..moveTo(p00.dx, p00.dy)
          ..lineTo(p10.dx, p10.dy)
          ..lineTo(p11.dx, p11.dy)
          ..lineTo(p01.dx, p01.dy)
          ..close();

        canvas.drawPath(poly, fill);
      }
    }

    final gridPaint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 1
      ..color = Colors.black.withOpacity(0.18);

    for (int c = 0; c <= LaneGridMask.cols; c++) {
      final a = pCell(c, 0);
      final b = pCell(c, LaneGridMask.rows);
      canvas.drawLine(a, b, gridPaint);
    }

    for (int r = 0; r <= LaneGridMask.rows; r++) {
      final a = pCell(0, r);
      final b = pCell(LaneGridMask.cols, r);
      canvas.drawLine(a, b, gridPaint);
    }

    final rc = LaneGridMask.rampCol;
    final rr = LaneGridMask.rampRow;

    final center = pCell(rc, rr) + (u + v) * 0.5;

    final rDot = Paint()..color = Colors.orange.withOpacity(0.95);
    canvas.drawCircle(center, 6, rDot);

    final p00 = pCell(rc, rr);
    final p10 = pCell(rc + 1, rr);
    final p11 = pCell(rc + 1, rr + 1);
    final p01 = pCell(rc, rr + 1);

    final rBox = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = 2
      ..color = Colors.orange.withOpacity(0.95);

    final poly = Path()
      ..moveTo(p00.dx, p00.dy)
      ..lineTo(p10.dx, p10.dy)
      ..lineTo(p11.dx, p11.dy)
      ..lineTo(p01.dx, p01.dy)
      ..close();

    canvas.drawPath(poly, rBox);
  }

  @override
  bool shouldRepaint(covariant _IndoorOverlayPainter old) {
    return old.path != path ||
        old.target != target ||
        old.showTarget != showTarget ||
        old.showUser != showUser ||
        old.showGridDebug != showGridDebug ||
        old.targetAnimT != targetAnimT;
  }
}
