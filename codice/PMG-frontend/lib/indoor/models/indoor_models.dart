import '../slot_map.dart';

enum IndoorSlotType {
  normal,
  disabled,
  pregnant;

  static IndoorSlotType fromJson(Map<String, dynamic> json) {
    final raw = json['tipo']?.toString().toUpperCase();

    if (raw == 'DISABILI' || raw == 'DISABILE') {
      return IndoorSlotType.disabled;
    }

    if (raw == 'INCINTA' || raw == 'DONNE_INCINTA') {
      return IndoorSlotType.pregnant;
    }

    if (json['riservatoDisabili'] == true) {
      return IndoorSlotType.disabled;
    }

    if (json['riservatoIncinta'] == true) {
      return IndoorSlotType.pregnant;
    }

    return IndoorSlotType.normal;
  }
}

enum IndoorSlotStatus {
  free,
  reserved,
  occupied;

  static IndoorSlotStatus fromJson(Map<String, dynamic> json) {
    final raw = json['stato']?.toString().toUpperCase();

    if (raw == 'PRENOTATO' || raw == 'RISERVATO') {
      return IndoorSlotStatus.reserved;
    }

    if (raw == 'OCCUPATO') {
      return IndoorSlotStatus.occupied;
    }

    final available = json['disponibile'];
    if (available is bool && !available) {
      return IndoorSlotStatus.occupied;
    }

    return IndoorSlotStatus.free;
  }
}

class IndoorSlotData {
  final String slotId;
  final int number;
  final String name;
  final IndoorSlotType type;
  final IndoorSlotStatus status;
  final bool outOfService;

  const IndoorSlotData({
    required this.slotId,
    required this.number,
    required this.name,
    required this.type,
    required this.status,
    required this.outOfService,
  });

  factory IndoorSlotData.generated({
    required int floor,
    required int number,
  }) {
    final formatted = number.toString().padLeft(2, '0');

    return IndoorSlotData(
      slotId: '$floor-$formatted',
      number: number,
      name: 'P$floor-$formatted',
      type: IndoorSlotType.normal,
      status: IndoorSlotStatus.free,
      outOfService: false,
    );
  }

  factory IndoorSlotData.fromJson(
    Map<String, dynamic> json, {
    required int floor,
    required int fallbackNumber,
  }) {
    final numberValue = json['numero'];
    final number = numberValue is num
        ? numberValue.toInt()
        : int.tryParse(numberValue?.toString() ?? '') ?? fallbackNumber;

    final formatted = number.toString().padLeft(2, '0');

    return IndoorSlotData(
      slotId: (json['slotId'] ?? '$floor-$formatted').toString(),
      number: number,
      name: (json['nome'] ?? 'P$floor-$formatted').toString(),
      type: IndoorSlotType.fromJson(json),
      status: IndoorSlotStatus.fromJson(json),
      outOfService:
          json['fuoriServizio'] as bool? ?? json['disabilitato'] as bool? ?? false,
    );
  }
}

class IndoorFloorData {
  final int floor;
  final int slotCount;
  final List<IndoorSlotData> slots;

  const IndoorFloorData({
    required this.floor,
    required this.slotCount,
    required this.slots,
  });

  factory IndoorFloorData.fromJson(Map<String, dynamic> json) {
    final floor = (json['piano'] as num).toInt();
    final rawSlots = (json['posti'] as List<dynamic>?) ?? const [];

    final parsedSlots = <int, IndoorSlotData>{};
    int maxNumber = 0;

    for (int index = 0; index < rawSlots.length; index++) {
      final raw = rawSlots[index] as Map<String, dynamic>;
      final parsed = IndoorSlotData.fromJson(
        raw,
        floor: floor,
        fallbackNumber: index + 1,
      );

      parsedSlots[parsed.number] = parsed;
      if (parsed.number > maxNumber) {
        maxNumber = parsed.number;
      }
    }

    final declaredCount = (json['numeroPosti'] as num?)?.toInt() ?? 0;
    final slotCount = declaredCount > maxNumber ? declaredCount : maxNumber;

    final slots = List<IndoorSlotData>.generate(
      slotCount,
      (index) {
        final number = index + 1;
        return parsedSlots[number] ??
            IndoorSlotData.generated(
              floor: floor,
              number: number,
            );
      },
    );

    return IndoorFloorData(
      floor: floor,
      slotCount: slotCount,
      slots: slots,
    );
  }
}

class IndoorParkingData {
  final String parkingId;
  final String name;
  final int floorCount;
  final int totalSlots;
  final List<IndoorFloorData> floors;

  const IndoorParkingData({
    required this.parkingId,
    required this.name,
    required this.floorCount,
    required this.totalSlots,
    required this.floors,
  });

  factory IndoorParkingData.fromJson(Map<String, dynamic> json) {
    final floorJson =
        (json['configurazionePiani'] ?? json['piani']) as List<dynamic>? ??
            const [];

    final floors = floorJson
        .map(
          (item) => IndoorFloorData.fromJson(
            item as Map<String, dynamic>,
          ),
        )
        .toList()
      ..sort((a, b) => a.floor.compareTo(b.floor));

    final calculatedTotal = floors.fold<int>(
      0,
      (sum, floor) => sum + floor.slotCount,
    );

    return IndoorParkingData(
      parkingId: (json['id'] ?? json['_id'] ?? json['parcheggioId'] ?? '')
          .toString(),
      name: (json['nome'] ?? '').toString(),
      floorCount: (json['numPiani'] as num?)?.toInt() ?? floors.length,
      totalSlots:
          (json['postiTotali'] as num?)?.toInt() ?? calculatedTotal,
      floors: floors,
    );
  }

  IndoorFloorData floorByNumber(int floorNumber) {
    return floors.firstWhere(
      (floor) => floor.floor == floorNumber,
      orElse: () => throw StateError(
        'Piano $floorNumber non presente nel parcheggio $parkingId',
      ),
    );
  }
}

class GeneratedIndoorSlot {
  final IndoorSlotData data;
  final GridArea area;
  final SlotCell2D approachCell;

  const GeneratedIndoorSlot({
    required this.data,
    required this.area,
    required this.approachCell,
  });
}

class IndoorFloorLayout {
  final int floor;
  final int cols;
  final int rows;
  final GridArea floorArea;
  final GridArea laneArea;
  final SlotCell2D entryCell;
  final SlotCell2D rampCell;
  final List<GeneratedIndoorSlot> slots;
  final Set<SlotCell2D> walkableCells;

  const IndoorFloorLayout({
    required this.floor,
    required this.cols,
    required this.rows,
    required this.floorArea,
    required this.laneArea,
    required this.entryCell,
    required this.rampCell,
    required this.slots,
    required this.walkableCells,
  });

  GeneratedIndoorSlot? tryGetSlotById(String slotId) {
    for (final slot in slots) {
      if (slot.data.slotId == slotId) {
        return slot;
      }
    }
    return null;
  }

  GeneratedIndoorSlot? tryGetSlotByNumber(int number) {
    for (final slot in slots) {
      if (slot.data.number == number) {
        return slot;
      }
    }
    return null;
  }
}

class IndoorSlotRef {
  final int floor;
  final String slotId;

  const IndoorSlotRef({
    required this.floor,
    required this.slotId,
  });
}

class IndoorAssignment {
  final IndoorSlotRef slot;
  final SlotCell2D approachCell;

  const IndoorAssignment({
    required this.slot,
    required this.approachCell,
  });
}
