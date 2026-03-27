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
  final String assetPath;

  const OperatorParkingImageMap({
    super.key,
    required this.selectedFloor,
    required this.floors,
    required this.onFloorChanged,
    required this.spots,
    required this.selectedSpotId,
    required this.onSpotTap,
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

  Color _spotColor(Posto posto) {
    if (widget.selectedSpotId == posto.slotId) {
      return const Color(0xFF22C55E);
    }
    if (!posto.disponibile) {
      return const Color(0xFFEF4444);
    }
    if (posto.riservatoDisabili || posto.riservatoIncinta) {
      return const Color(0xFF3B82F6);
    }
    return AppColors.accentCyan;
  }

  String _spotLabel(Posto posto) {
    if (!posto.disponibile) return 'X';
    if (posto.riservatoDisabili) return 'D';
    if (posto.riservatoIncinta) return 'P';
    return 'L';
  }

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
          Row(
            children: [
              const Expanded(
                child: Text(
                  'Mappa Parcheggio',
                  style: TextStyle(
                    color: AppColors.textPrimary,
                    fontWeight: FontWeight.w900,
                    fontSize: 16,
                  ),
                ),
              ),
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
          AspectRatio(
            aspectRatio: _imgAspect ?? (16 / 9),
            child: LayoutBuilder(
              builder: (context, constraints) {
                final size = Size(constraints.maxWidth, constraints.maxHeight);

                return Stack(
                  fit: StackFit.expand,
                  children: [
                    ClipRRect(
                      borderRadius: BorderRadius.circular(14),
                      child: Image.asset(widget.assetPath, fit: BoxFit.contain),
                    ),
                    ...widget.spots.map((posto) {
                      final cell = baseSlotMap[posto.slotNumber];
                      if (cell == null) {
                        return const SizedBox.shrink();
                      }

                      final normalized = LaneGridMask.cellCenterToNormalized(
                        cell.c,
                        cell.r,
                      );
                      final p = _pxFromNormalized(normalized, size);

                      return Positioned(
                        left: p.dx - 20,
                        top: p.dy - 20,
                        child: Tooltip(
                          message:
                              '${posto.slotId} • '
                              '${posto.disponibile ? "Libero" : "Occupato"}'
                              '${posto.riservatoDisabili ? " • Disabili" : ""}'
                              '${posto.riservatoIncinta ? " • Incinta" : ""}',
                          child: GestureDetector(
                            onTap: () => widget.onSpotTap(posto.slotId),
                            child: Container(
                              width: 40,
                              height: 40,
                              decoration: BoxDecoration(
                                color: _spotColor(posto).withOpacity(0.90),
                                shape: BoxShape.circle,
                                border: Border.all(
                                  color: Colors.white,
                                  width: 2,
                                ),
                                boxShadow: [
                                  BoxShadow(
                                    color: Colors.black.withOpacity(0.25),
                                    blurRadius: 6,
                                    offset: const Offset(0, 2),
                                  ),
                                ],
                              ),
                              alignment: Alignment.center,
                              child: Text(
                                _spotLabel(posto),
                                style: const TextStyle(
                                  color: Colors.white,
                                  fontWeight: FontWeight.w800,
                                  fontSize: 10,
                                ),
                              ),
                            ),
                          ),
                        ),
                      );
                    }),
                  ],
                );
              },
            ),
          ),
          const SizedBox(height: 16),
          Wrap(
            spacing: 16,
            runSpacing: 8,
            children: [
              _legendItem(AppColors.accentCyan, 'Disponibile'),
              _legendItem(const Color(0xFFF59E0B), 'Occupato'),
              _legendItem(const Color(0xFF3B82F6), 'Riservato'),
              _legendItem(const Color(0xFF22C55E), 'Selezionato'),
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
          width: 12,
          height: 12,
          decoration: BoxDecoration(
            color: color.withOpacity(0.25),
            border: Border.all(color: color, width: 1.5),
            borderRadius: BorderRadius.circular(3),
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
