import '../models/indoor_models.dart';
import '../slot_map.dart';

class LaneGrid {
  final IndoorFloorLayout layout;

  const LaneGrid(this.layout);

  bool isInside(SlotCell2D cell) {
    return cell.c >= 0 &&
        cell.c < layout.cols &&
        cell.r >= 0 &&
        cell.r < layout.rows;
  }

  bool isWalkable(SlotCell2D cell) {
    return layout.walkableCells.contains(cell);
  }

  List<SlotCell2D> buildPath({
    required SlotCell2D start,
    required SlotCell2D goal,
  }) {
    if (!isInside(start) ||
        !isInside(goal) ||
        !isWalkable(start) ||
        !isWalkable(goal)) {
      return [start];
    }

    final queue = <SlotCell2D>[start];
    final visited = <SlotCell2D>{start};
    final parent = <SlotCell2D, SlotCell2D?>{start: null};

    const directions = <SlotCell2D>[
      SlotCell2D(c: 1, r: 0),
      SlotCell2D(c: -1, r: 0),
      SlotCell2D(c: 0, r: 1),
      SlotCell2D(c: 0, r: -1),
    ];

    int queueIndex = 0;

    while (queueIndex < queue.length) {
      final current = queue[queueIndex++];

      if (current == goal) {
        break;
      }

      for (final direction in directions) {
        final next = SlotCell2D(
          c: current.c + direction.c,
          r: current.r + direction.r,
        );

        if (!isInside(next)) continue;
        if (!isWalkable(next)) continue;
        if (visited.contains(next)) continue;

        visited.add(next);
        parent[next] = current;
        queue.add(next);
      }
    }

    if (!parent.containsKey(goal)) {
      return [start];
    }

    final reversed = <SlotCell2D>[];
    SlotCell2D? current = goal;

    while (current != null) {
      reversed.add(current);
      current = parent[current];
    }

    return reversed.reversed.toList();
  }
}
