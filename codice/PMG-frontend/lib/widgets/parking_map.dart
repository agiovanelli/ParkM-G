import 'package:flutter/material.dart';
import 'package:park_mg/utils/theme.dart';

enum SpotType { standard, reserved, disabled }

enum SpotState { free, occupied, reserved, unavailable, selected }

class ParkingSpot {
  final String id; // es: F0-001
  final String label; // testo dentro il posto (P / d / 001)
  final SpotType type;
  final SpotState state;

  /// Rettangolo normalizzato (0..1)
  final Rect rect;

  /// true = aperto verso il basso (come riga in alto), false = aperto verso l'alto
  final bool openDown;

  const ParkingSpot({
    required this.id,
    required this.label,
    required this.type,
    required this.state,
    required this.rect,
    required this.openDown,
  });

  String tooltip() {
    final t = switch (type) {
      SpotType.standard => 'Standard',
      SpotType.reserved => 'Riservato',
      SpotType.disabled => 'Disabili',
    };
    final s = switch (state) {
      SpotState.free => 'Libero',
      SpotState.occupied => 'Occupato',
      SpotState.selected => 'Selezionato',
      SpotState.reserved => 'Riservato',
      SpotState.unavailable => 'Non Disponibile',
    };
    return '$id · $t · $s';
  }
}

class SpotTemplate {
  final int index1;
  final SpotType type;
  final Rect rect;
  final bool openDown;

  const SpotTemplate({
    required this.index1,
    required this.type,
    required this.rect,
    required this.openDown,
  });
}

class UniformGarageLayout {
  /// 5 piani * 44 = 220 (uniforme)
  static const int floorsCount = 5;
  static const int spotsPerFloor = 44;

  /// 44 = 4 righe * 11 colonne
  static const int rows = 4;
  static const int cols = 11;

  /// Template (coordinate uguali per ogni piano)
  static final List<SpotTemplate> templates = _buildTemplates();

  static List<int> defaultFloors() => List.generate(floorsCount, (i) => i);

  /// Genera i 44 posti per un piano, applicando la stessa geometria.
  /// - selectedId: (opzionale) id selezionato su quel piano
  /// - occupiedIds: (opzionale) ids occupati su quel piano
  static List<ParkingSpot> buildForFloor({
    required int floor,
    String? selectedId,
    Set<String> occupiedIds = const {},
    Set<String> reservedIds = const {},
    Set<String> unavailableIds = const {},
  }) {
    return templates
        .map((t) {
          final id = 'F$floor-${t.index1.toString().padLeft(3, '0')}';

          final label = switch (t.type) {
            SpotType.reserved => 'P',
            SpotType.disabled => 'd',
            SpotType.standard => t.index1.toString().padLeft(3, '0'),
          };

          final SpotState state = (id == selectedId)
              ? SpotState.selected
              : (unavailableIds.contains(id))
              ? SpotState.unavailable
              : (reservedIds.contains(id))
              ? SpotState.reserved
              : (occupiedIds.contains(id))
              ? SpotState.occupied
              : SpotState.free;

          return ParkingSpot(
            id: id,
            label: label,
            type: t.type,
            state: state,
            rect: t.rect,
            openDown: t.openDown,
          );
        })
        .toList(growable: false);
  }

  static List<SpotTemplate> _buildTemplates() {
    // Parametri in coordinate normalizzate (0..1) per imitare lo screenshot
    const leftPad = 0.18; // spazio a sinistra per frecce / aiuole
    const rightPad = 0.06;
    const topBand = 0.12; // fascia verde sopra

    const colGap = 0.018; // spazio fra posti
    const stallH = 0.115; // altezza posto

    // Spazi verticali: righe + corsie fra coppie di righe
    // Row0 (openDown) e Row1 (openUp) condividono una corsia.
    // Row2 (openDown) e Row3 (openUp) condividono una corsia.
    const topInner = 0.06;
    const betweenPairs = 0.10; // distanza fra (row0/1) e (row2/3)
    const inPairAisle = 0.12; // corsia fra le due righe della coppia

    final availW = 1.0 - leftPad - rightPad;
    final stallW = (availW - (cols - 1) * colGap) / cols;

    // Calcolo Y righe
    final yRow0 = topBand + topInner; // openDown
    final yRow1 = yRow0 + stallH + inPairAisle; // openUp
    final yRow2 = yRow1 + stallH + betweenPairs; // openDown
    final yRow3 = yRow2 + stallH + inPairAisle; // openUp

    // Nota: bottomBand è già garantito perché abbiamo scelto valori “comodi”.
    // Se cambi i parametri, controlla che yRow3 + stallH < 1 - bottomBand.

    SpotType typeFor(int row, int col, int index1) {
      // Distribuzione “sensata” e identica su tutti i piani:
      // - Riservati (P) in alto
      // - Disabili (d) in basso vicino al lato sinistro
      final reserved =
          (row == 0 && (col == 1 || col == 2)) || (row == 1 && col == 1);
      final disabled =
          (row == 3 && (col == 0 || col == 1)) || (row == 2 && col == 0);

      if (reserved) return SpotType.reserved;
      if (disabled) return SpotType.disabled;
      return SpotType.standard;
    }

    Rect rectFor(int row, int col) {
      final x = leftPad + col * (stallW + colGap);
      final y = switch (row) {
        0 => yRow0,
        1 => yRow1,
        2 => yRow2,
        _ => yRow3,
      };
      return Rect.fromLTWH(x, y, stallW, stallH);
    }

    final out = <SpotTemplate>[];
    var idx = 1;
    for (var r = 0; r < rows; r++) {
      final openDown = (r % 2 == 0); // 0 e 2 down, 1 e 3 up
      for (var c = 0; c < cols; c++) {
        out.add(
          SpotTemplate(
            index1: idx,
            type: typeFor(r, c, idx),
            rect: rectFor(r, c),
            openDown: openDown,
          ),
        );
        idx++;
      }
    }
    return out;
  }
}

class ParkingBackgroundPainter extends CustomPainter {
  final List<ParkingSpot> spots;

  final Color asphaltColor;
  final Color lineColor;
  final Color greenColor;

  const ParkingBackgroundPainter({
    required this.spots,
    this.asphaltColor = const Color(0xFF5E5F66),
    this.lineColor = const Color(0xFFEDEDED),
    this.greenColor = const Color(0xFF7CB342),
  });

  @override
  void paint(Canvas canvas, Size size) {
    // Fondo asfalto
    final bg = Paint()..color = asphaltColor;
    canvas.drawRRect(
      RRect.fromRectAndRadius(Offset.zero & size, const Radius.circular(14)),
      bg,
    );

    // Fasce verdi sopra/sotto
    final bandH = size.height * 0.12;
    final green = Paint()..color = greenColor;
    canvas.drawRect(Rect.fromLTWH(0, 0, size.width, bandH), green);
    canvas.drawRect(
      Rect.fromLTWH(0, size.height - bandH, size.width, bandH),
      green,
    );

    // “Aiuole” sinistra (similissime allo screenshot)
    final island = Paint()..color = greenColor.withOpacity(0.95);
    final islandSize = Size(size.width * 0.14, size.height * 0.18);
    canvas.drawRRect(
      RRect.fromRectAndRadius(
        Rect.fromLTWH(0, 0, islandSize.width, islandSize.height),
        const Radius.circular(10),
      ),
      island,
    );
    canvas.drawRRect(
      RRect.fromRectAndRadius(
        Rect.fromLTWH(
          0,
          size.height - islandSize.height,
          islandSize.width,
          islandSize.height,
        ),
        const Radius.circular(10),
      ),
      island,
    );

    // Linee stalli (a U, aperti verso corsia)
    final p = Paint()
      ..color = lineColor.withOpacity(0.95)
      ..strokeWidth = 2
      ..style = PaintingStyle.stroke;

    for (final s in spots) {
      final r = Rect.fromLTWH(
        s.rect.left * size.width,
        s.rect.top * size.height,
        s.rect.width * size.width,
        s.rect.height * size.height,
      );

      final path = Path();
      if (s.openDown) {
        // U aperta verso il basso
        path.moveTo(r.left, r.top);
        path.lineTo(r.left, r.bottom);
        path.lineTo(r.right, r.bottom);
        path.lineTo(r.right, r.top);
      } else {
        // U aperta verso l'alto
        path.moveTo(r.left, r.bottom);
        path.lineTo(r.left, r.top);
        path.lineTo(r.right, r.top);
        path.lineTo(r.right, r.bottom);
      }
      canvas.drawPath(path, p);
    }

    // Frecce a sinistra
    _drawArrow(canvas, size, Offset(size.width * 0.08, size.height * 0.48));
    _drawArrow(canvas, size, Offset(size.width * 0.08, size.height * 0.52));
  }

  void _drawArrow(Canvas canvas, Size size, Offset center) {
    final arrowPaint = Paint()
      ..color = lineColor.withOpacity(0.95)
      ..style = PaintingStyle.stroke
      ..strokeWidth = 3
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round;

    final w = size.width * 0.05;
    final h = size.height * 0.035;

    final path = Path()
      ..moveTo(center.dx + w, center.dy)
      ..lineTo(center.dx - w, center.dy)
      ..moveTo(center.dx - w * 0.2, center.dy - h)
      ..lineTo(center.dx - w, center.dy)
      ..lineTo(center.dx - w * 0.2, center.dy + h);

    canvas.drawPath(path, arrowPaint);
  }

  @override
  bool shouldRepaint(covariant ParkingBackgroundPainter oldDelegate) {
    return oldDelegate.spots != spots;
  }
}

class ParkingSchematicMap extends StatelessWidget {
  final int selectedFloor;
  final List<int> floors;
  final ValueChanged<int> onFloorChanged;

  final List<ParkingSpot> spots;
  final ValueChanged<ParkingSpot> onSpotTap;

  final List<Widget> legend;
  final double aspectRatio;

  const ParkingSchematicMap({
    super.key,
    required this.selectedFloor,
    required this.floors,
    required this.onFloorChanged,
    required this.spots,
    required this.onSpotTap,
    required this.legend,
    this.aspectRatio = 1.30, // più largo per 11 colonne
  });

  Color _stroke(ParkingSpot s) {
    switch (s.state) {
      case SpotState.free:
        return AppColors.accentCyan;
      case SpotState.occupied:
        return const Color(0xFFF59E0B);
      case SpotState.reserved:
        return const Color(0xFF3B82F6);
      case SpotState.unavailable:
        return const Color(0xFF6B7280);
      case SpotState.selected:
        return const Color(0xFF22C55E); // oppure AppColors.accentCyan
    }
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
                  onChanged: (v) => v == null ? null : onFloorChanged(v),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),

          AspectRatio(
            aspectRatio: aspectRatio,
            child: ClipRRect(
              borderRadius: BorderRadius.circular(14),
              child: LayoutBuilder(
                builder: (context, c) {
                  final w = c.maxWidth;
                  final h = c.maxHeight;

                  return Stack(
                    children: [
                      CustomPaint(
                        size: Size(w, h),
                        painter: ParkingBackgroundPainter(spots: spots),
                      ),

                      for (final s in spots)
                        Positioned(
                          left: s.rect.left * w,
                          top: s.rect.top * h,
                          width: s.rect.width * w,
                          height: s.rect.height * h,
                          child: Tooltip(
                            message: s.tooltip(),
                            child: Material(
                              color: Colors.transparent,
                              child: InkWell(
                                borderRadius: BorderRadius.circular(6),
                                onTap: () => onSpotTap(s),
                                child: Container(
                                  decoration: BoxDecoration(
                                    color: _stroke(s).withOpacity(0.22),
                                    border: Border.all(
                                      color: _stroke(s),
                                      width: 1.4,
                                    ),
                                    borderRadius: BorderRadius.circular(6),
                                  ),
                                  alignment: Alignment.center,
                                  child: FittedBox(
                                    fit: BoxFit.scaleDown,
                                    child: Text(
                                      s.label,
                                      style: const TextStyle(
                                        color: AppColors.textPrimary,
                                        fontWeight: FontWeight.w800,
                                      ),
                                    ),
                                  ),
                                ),
                              ),
                            ),
                          ),
                        ),
                    ],
                  );
                },
              ),
            ),
          ),

          const SizedBox(height: 16),

          Wrap(spacing: 16, runSpacing: 8, children: legend),
        ],
      ),
    );
  }
}

class ParkingPage extends StatefulWidget {
  const ParkingPage({super.key});

  @override
  State<ParkingPage> createState() => _ParkingPageState();
}

class _ParkingPageState extends State<ParkingPage> {
  final floors = UniformGarageLayout.defaultFloors(); // [0..4]
  int selectedFloor = 0;

  String? selectedSpotId;

  // Esempio: occupati “finti” per piano (puoi sostituire con dati backend)
  final Map<int, Set<String>> occupiedByFloor = {
    0: {'F0-003', 'F0-044'},
    1: {'F1-010'},
    2: {'F2-021', 'F2-022'},
    3: {},
    4: {'F4-001'},
  };

  @override
  Widget build(BuildContext context) {
    final spots = UniformGarageLayout.buildForFloor(
      floor: selectedFloor,
      selectedId: selectedSpotId,
      occupiedIds: occupiedByFloor[selectedFloor] ?? {},
    );

    return ParkingSchematicMap(
      selectedFloor: selectedFloor,
      floors: floors,
      onFloorChanged: (f) {
        setState(() {
          selectedFloor = f;
          selectedSpotId = null; // opzionale: reset selezione cambiando piano
        });
      },
      spots: spots,
      onSpotTap: (s) {
        if (s.state == SpotState.occupied)
          return; // opzionale: blocca tap su occupati
        setState(() {
          selectedSpotId = (selectedSpotId == s.id) ? null : s.id;
        });
      },
      legend: [
        _legendItem('Libero', const Color(0xFF81C784)),
        _legendItem('Occupato', const Color(0xFFE57373)),
        _legendItem('Selezionato', const Color(0xFF42A5F5)),
        _legendItem('Riservato', Colors.white),
        _legendItem('Disabili', Colors.white),
      ],
    );
  }

  Widget _legendItem(String label, Color c) {
    return Row(
      mainAxisSize: MainAxisSize.min,
      children: [
        Container(
          width: 12,
          height: 12,
          decoration: BoxDecoration(
            color: c.withOpacity(0.25),
            border: Border.all(color: c, width: 1.4),
            borderRadius: BorderRadius.circular(3),
          ),
        ),
        const SizedBox(width: 6),
        Text(
          label,
          style: const TextStyle(color: AppColors.textPrimary, fontSize: 12),
        ),
      ],
    );
  }
}
