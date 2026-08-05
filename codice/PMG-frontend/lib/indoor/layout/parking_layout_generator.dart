import '../models/indoor_models.dart';
import '../slot_map.dart';

class ParkingLayoutGenerator {
  static const int margin = 2;
  static const int slotWidth = 3;
  static const int slotDepth = 5;
  static const int laneHeight = 5;

  const ParkingLayoutGenerator();

  IndoorFloorLayout generateFloor(IndoorFloorData floorData) {
    final sortedSlots = [...floorData.slots]
      ..sort((a, b) => a.number.compareTo(b.number));

    final topCount = (sortedSlots.length / 2).ceil();
    final bottomCount = sortedSlots.length - topCount;
    final longestRow = topCount > bottomCount ? topCount : bottomCount;

    final cols = margin * 2 + longestRow * slotWidth;
    final topRow = margin;
    final laneRow = topRow + slotDepth;
    final bottomRow = laneRow + laneHeight;
    final rows = bottomRow + slotDepth + margin;

    final laneArea = GridArea(
      c: margin,
      r: laneRow,
      width: cols - margin * 2,
      height: laneHeight,
    );

    final generatedSlots = <GeneratedIndoorSlot>[];

    for (int index = 0; index < sortedSlots.length; index++) {
      final isTop = index < topCount;
      final localIndex = isTop ? index : index - topCount;
      final slotCol = margin + localIndex * slotWidth;

      generatedSlots.add(
        GeneratedIndoorSlot(
          data: sortedSlots[index],
          area: GridArea(
            c: slotCol,
            r: isTop ? topRow : bottomRow,
            width: slotWidth,
            height: slotDepth,
          ),
          approachCell: SlotCell2D(
            c: slotCol + slotWidth ~/ 2,
            r: isTop ? laneRow : laneRow + laneHeight - 1,
          ),
        ),
      );
    }

    final walkableCells = <SlotCell2D>{
      for (int r = laneRow; r < laneRow + laneHeight; r++)
        for (int c = margin; c < cols - margin; c++)
          SlotCell2D(c: c, r: r),
    };

    return IndoorFloorLayout(
      floor: floorData.floor,
      cols: cols,
      rows: rows,
      floorArea: GridArea(
        c: 0,
        r: 0,
        width: cols,
        height: rows,
      ),
      laneArea: laneArea,
      entryCell: SlotCell2D(
        c: margin,
        r: laneRow + laneHeight ~/ 2,
      ),
      rampCell: SlotCell2D(
        c: cols - margin - 1,
        r: laneRow + laneHeight ~/ 2,
      ),
      slots: generatedSlots,
      walkableCells: walkableCells,
    );
  }

  Map<int, IndoorFloorLayout> generateParking(
    IndoorParkingData parkingData,
  ) {
    return {
      for (final floor in parkingData.floors)
        floor.floor: generateFloor(floor),
    };
  }
}
