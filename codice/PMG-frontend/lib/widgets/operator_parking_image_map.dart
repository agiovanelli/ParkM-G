import 'dart:async';
import 'package:flutter/material.dart';
import 'package:park_mg/indoor/graph/lane_grid_mask.dart';
import 'package:park_mg/models/posto.dart';
import 'package:park_mg/indoor/slot_map.dart';
import 'package:park_mg/utils/theme.dart';

class OperatorParkingImageMap extends StatefulWidget {
  final int selectedFloor;
  final List<int> floors;
  final ValueChanged<int> onFloorChanged;
  final List<Posto> spots;
  final String? selectedSpotId;
  final ValueChanged<String> onSpotTap;
  final ValueChanged<String> onDisableSpot;
  final String assetPath;

  const OperatorParkingImageMap({
    super.key,
    required this.selectedFloor,
    required this.floors,
    required this.onFloorChanged,
    required this.spots,
    required this.selectedSpotId,
    required this.onSpotTap,
    required this.onDisableSpot,
    this.assetPath = 'assets/parking/floor.png',
  });

  @override
  State<OperatorParkingImageMap> createState() =>
      _OperatorParkingImageMapState();
}

class _OperatorParkingImageMapState extends State<OperatorParkingImageMap> {
  double? _imgAspect;

  @override
  void initState() {
    super.initState();
    _loadAspect();
  }

  Future<void> _loadAspect() async {
    try {
      final img = AssetImage(widget.assetPath);
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
      if (!mounted) return;
      setState(() {
        _imgAspect = info.image.width / info.image.height;
      });
    } catch (_) {}
  }

  Rect _imageRect(Size size, double imageAspect) {
    final dstAspect = size.width / size.height;

    double w, h;
    if (imageAspect > dstAspect) {
      w = size.width;
      h = w / imageAspect;
    } else {
      h = size.height;
      w = h * imageAspect;
    }

    final left = (size.width - w) / 2.0;
    final top = (size.height - h) / 2.0;
    return Rect.fromLTWH(left, top, w, h);
  }

  Offset _pxFromNormalized(Offset n, Size size) {
    final r = _imageRect(size, _imgAspect ?? (16 / 9));
    return Offset(r.left + n.dx * r.width, r.top + n.dy * r.height);
  }

  bool _isDisabled(Posto posto) {
    return posto.disabilitato;
  }

  Color _spotColor(Posto posto) {
    if (_isDisabled(posto)) {
      return const Color(0xFF9CA3AF);
    }

    if (widget.selectedSpotId == posto.slotId) {
      return const Color(0xFFFACC15);
    }

    if (!posto.disponibile) {
      return const Color(0xFFEF4444);
    }

    if (posto.riservatoDisabili) {
      return AppColors.accentCyan;
    }

    if (posto.riservatoIncinta) {
      return const Color(0xFFF9A8D4);
    }

    return const Color(0xFF22C55E);
  }

  String _spotLabel(Posto posto) {
    return posto.slotNumber.toString();
  }

  List<Offset> _slotPolygonNormalized(int c, int r) {
    return LaneGridMask.cellBlockPolygonNormalized(
      c: c,
      r: r,
      halfCols: 2,
      halfRows: 1,
    );
  }

  Rect _slotTapRect(List<Offset> poly, Size size) {
    final pts = poly.map((e) => _pxFromNormalized(e, size)).toList();

    final minX = pts.map((e) => e.dx).reduce((a, b) => a < b ? a : b);
    final maxX = pts.map((e) => e.dx).reduce((a, b) => a > b ? a : b);
    final minY = pts.map((e) => e.dy).reduce((a, b) => a < b ? a : b);
    final maxY = pts.map((e) => e.dy).reduce((a, b) => a > b ? a : b);

    return Rect.fromLTRB(minX, minY, maxX, maxY);
  }

  String _spotStatusLabel(Posto posto) {
    if (_isDisabled(posto)) {
      return 'Disabilitato';
    }

    if (!posto.disponibile) {
      return 'Occupato';
    }

    if (posto.riservatoDisabili) {
      return 'Libero - Disabili';
    }

    if (posto.riservatoIncinta) {
      return 'Libero - Donna incinta';
    }

    return 'Libero';
  }

  @override
  Widget build(BuildContext context) {
    Posto? selectedSpot;
    if (widget.selectedSpotId != null) {
      try {
        selectedSpot = widget.spots.firstWhere(
          (s) => s.slotId == widget.selectedSpotId,
        );
      } catch (_) {
        selectedSpot = null;
      }
    }

    final canDisableSelected =
        selectedSpot != null &&
        selectedSpot.disponibile &&
        !selectedSpot.disabilitato;

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
          Row(
            children: [
              DropdownButtonHideUnderline(
                child: DropdownButton<int>(
                  dropdownColor: AppColors.bgDark,
                  value: widget.selectedFloor,
                  style: const TextStyle(color: AppColors.textPrimary),
                  items: widget.floors
                      .map(
                        (f) => DropdownMenuItem<int>(
                          value: f,
                          child: Text('Piano $f'),
                        ),
                      )
                      .toList(),
                  onChanged: (v) {
                    if (v != null) widget.onFloorChanged(v);
                  },
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Center(
            child: FractionallySizedBox(
              widthFactor: 0.82,
              child: AspectRatio(
                aspectRatio: _imgAspect ?? (16 / 9),
                child: LayoutBuilder(
                  builder: (context, constraints) {
                    final size = Size(
                      constraints.maxWidth,
                      constraints.maxHeight,
                    );

                    return Stack(
                      fit: StackFit.expand,
                      children: [
                        ClipRRect(
                          borderRadius: BorderRadius.circular(14),
                          child: Image.asset(
                            widget.assetPath,
                            fit: BoxFit.contain,
                          ),
                        ),
                        CustomPaint(
                          painter: _OperatorParkingSlotsPainter(
                            spots: widget.spots,
                            selectedSpotId: widget.selectedSpotId,
                            imageAspect: _imgAspect ?? (16 / 9),
                            pxFromNormalized: _pxFromNormalized,
                            spotColor: _spotColor,
                            spotLabel: _spotLabel,
                          ),
                        ),
                        ...widget.spots.map((posto) {
                          final cell = baseSlotMap[posto.slotNumber];
                          if (cell == null) return const SizedBox.shrink();

                          final poly = _slotPolygonNormalized(cell.c, cell.r);
                          final tapRect = _slotTapRect(poly, size);

                          return Positioned(
                            left: tapRect.left,
                            top: tapRect.top,
                            width: tapRect.width,
                            height: tapRect.height,
                            child: Tooltip(
                              message:
                                  'Posto ${posto.slotNumber} • ${_spotStatusLabel(posto)}',
                              child: GestureDetector(
                                behavior: HitTestBehavior.translucent,
                                onTap: posto.disabilitato
                                    ? null
                                    : () => widget.onSpotTap(posto.slotId),
                                child: const SizedBox.expand(),
                              ),
                            ),
                          );
                        }),
                      ],
                    );
                  },
                ),
              ),
            ),
          ),
          const SizedBox(height: 16),
          if (canDisableSelected)
            Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: ElevatedButton.icon(
                onPressed: () {
                  widget.onDisableSpot(widget.selectedSpotId!);
                },
                icon: const Icon(Icons.block),
                label: const Text('Disabilita posto selezionato'),
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
          Wrap(
            spacing: 16,
            runSpacing: 8,
            children: [
              _legendItem(const Color(0xFF22C55E), 'Disponibile'),
              _legendItem(AppColors.accentCyan, 'Disabili'),
              _legendItem(const Color(0xFFF9A8D4), 'Donna incinta'),
              _legendItem(const Color(0xFFEF4444), 'Occupato'),
              _legendItem(const Color(0xFFFACC15), 'Selezionato'),
              _legendItem(const Color(0xFF9CA3AF), 'Disabilitato'),
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
            color: color.withOpacity(0.25),
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

class _OperatorParkingSlotsPainter extends CustomPainter {
  final List<Posto> spots;
  final String? selectedSpotId;
  final double imageAspect;
  final Offset Function(Offset n, Size size) pxFromNormalized;
  final Color Function(Posto posto) spotColor;
  final String Function(Posto posto) spotLabel;

  _OperatorParkingSlotsPainter({
    required this.spots,
    required this.selectedSpotId,
    required this.imageAspect,
    required this.pxFromNormalized,
    required this.spotColor,
    required this.spotLabel,
  });

  @override
  void paint(Canvas canvas, Size size) {
    for (final posto in spots) {
      final cell = baseSlotMap[posto.slotNumber];
      if (cell == null) continue;

      final polyN = LaneGridMask.cellBlockPolygonNormalized(
        c: cell.c,
        r: cell.r,
        halfCols: 2,
        halfRows: 1,
      );

      final pts = polyN.map((p) => pxFromNormalized(p, size)).toList();
      if (pts.length < 4) continue;

      final path = Path()
        ..moveTo(pts[0].dx, pts[0].dy)
        ..lineTo(pts[1].dx, pts[1].dy)
        ..lineTo(pts[2].dx, pts[2].dy)
        ..lineTo(pts[3].dx, pts[3].dy)
        ..close();

      final color = spotColor(posto);

      final glow = Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = 8
        ..strokeJoin = StrokeJoin.round
        ..color = color.withOpacity(0.22);

      final fill = Paint()
        ..style = PaintingStyle.fill
        ..color = color.withOpacity(0.35);

      final stroke = Paint()
        ..style = PaintingStyle.stroke
        ..strokeWidth = 2.2
        ..strokeJoin = StrokeJoin.round
        ..color = color.withOpacity(0.95);

      canvas.drawPath(path, glow);
      canvas.drawPath(path, fill);
      canvas.drawPath(path, stroke);

      final center = Offset(
        (pts[0].dx + pts[1].dx + pts[2].dx + pts[3].dx) / 4,
        (pts[0].dy + pts[1].dy + pts[2].dy + pts[3].dy) / 4,
      );

      final textPainter = TextPainter(
        text: TextSpan(
          text: spotLabel(posto),
          style: const TextStyle(
            color: Colors.white,
            fontSize: 13,
            fontWeight: FontWeight.w800,
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
  }

  @override
  bool shouldRepaint(covariant _OperatorParkingSlotsPainter oldDelegate) {
    return oldDelegate.spots != spots ||
        oldDelegate.selectedSpotId != selectedSpotId ||
        oldDelegate.imageAspect != imageAspect;
  }
}