class SlotCell2D {
  final int c;
  final int r;

  const SlotCell2D({
    required this.c,
    required this.r,
  });

  @override
  bool operator ==(Object other) {
    return other is SlotCell2D && other.c == c && other.r == r;
  }

  @override
  int get hashCode => Object.hash(c, r);

  @override
  String toString() => 'SlotCell2D(c: $c, r: $r)';
}

class GridArea {
  final int c;
  final int r;
  final int width;
  final int height;

  const GridArea({
    required this.c,
    required this.r,
    required this.width,
    required this.height,
  });
}

class ParsedSlotId {
  final int floor;
  final int slotNumber;

  const ParsedSlotId({
    required this.floor,
    required this.slotNumber,
  });

  String get formattedSlotNumber => slotNumber.toString().padLeft(2, '0');

  String get value => '$floor-$formattedSlotNumber';

  factory ParsedSlotId.parse(String raw) {
    final normalized = raw.trim();
    final match = RegExp(r'^(\d+)-(\d+)$').firstMatch(normalized);

    if (match == null) {
      throw FormatException('Slot id non valido: $raw');
    }

    final floor = int.tryParse(match.group(1)!);
    final slotNumber = int.tryParse(match.group(2)!);

    if (floor == null || floor <= 0) {
      throw FormatException('Piano non valido nello slot id: $raw');
    }

    if (slotNumber == null || slotNumber <= 0) {
      throw FormatException('Numero posto non valido nello slot id: $raw');
    }

    return ParsedSlotId(
      floor: floor,
      slotNumber: slotNumber,
    );
  }
}
