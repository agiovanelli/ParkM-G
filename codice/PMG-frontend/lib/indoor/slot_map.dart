class SlotCell2D {
  final int c;
  final int r;

  const SlotCell2D({
    required this.c,
    required this.r,
  });
}

class ParsedSlotId {
  final int floor;
  final String slotNumber;

  const ParsedSlotId({
    required this.floor,
    required this.slotNumber,
  });

  String get value => '$floor-$slotNumber';

  factory ParsedSlotId.parse(String raw) {
    final parts = raw.split('-');

    if (parts.length != 2) {
      throw FormatException('Slot id non valido: $raw');
    }

    final floor = int.tryParse(parts[0]);
    if (floor == null || floor <= 0) {
      throw FormatException('Piano non valido nello slot id: $raw');
    }

    final slotNum = int.tryParse(parts[1]);
    if (slotNum == null || slotNum <= 0) {
      throw FormatException('Numero posto non valido nello slot id: $raw');
    }

    return ParsedSlotId(
      floor: floor,
      slotNumber: slotNum.toString().padLeft(2, '0'),
    );
  }
}

const Map<String, SlotCell2D> baseSlotMap = {
  '01': SlotCell2D(c: 3, r: 3),
  '02': SlotCell2D(c: 3, r: 6),
  '03': SlotCell2D(c: 3, r: 11),
  '04': SlotCell2D(c: 3, r: 14),
  '05': SlotCell2D(c: 3, r: 16),
  '06': SlotCell2D(c: 3, r: 21),
  '07': SlotCell2D(c: 3, r: 24),

  '08': SlotCell2D(c: 35, r: 3),
  '09': SlotCell2D(c: 35, r: 6),
  '10': SlotCell2D(c: 35, r: 11),
  '11': SlotCell2D(c: 35, r: 14),
  '12': SlotCell2D(c: 35, r: 16),
  '13': SlotCell2D(c: 35, r: 21),
  '14': SlotCell2D(c: 35, r: 24),

  '15': SlotCell2D(c: 22, r: 13),
  '16': SlotCell2D(c: 22, r: 16),

  '17': SlotCell2D(c: 15, r: 13),
  '18': SlotCell2D(c: 15, r: 16),
};

SlotCell2D? tryGetSlotCellFromSlotId(String rawSlotId) {
  try {
    final parsed = ParsedSlotId.parse(rawSlotId);
    return baseSlotMap[parsed.slotNumber];
  } catch (_) {
    return null;
  }
}