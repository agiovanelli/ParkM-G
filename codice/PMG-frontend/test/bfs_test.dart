import 'package:flutter_test/flutter_test.dart';
import 'package:park_mg/indoor/graph/lane_grid_mask.dart';
import 'package:park_mg/indoor/models/indoor_models.dart';
import 'package:park_mg/indoor/slot_map.dart';

void main() {
  group('LaneGrid BFS unit', () {
    const start = SlotCell2D(c: 0, r: 2);
    const goal = SlotCell2D(c: 4, r: 2);

    IndoorFloorLayout buildLayout({
      Set<SlotCell2D>? walkableCells,
    }) {
      return IndoorFloorLayout(
        floor: 1,
        cols: 5,
        rows: 5,
        floorArea: const GridArea(
          c: 0,
          r: 0,
          width: 5,
          height: 5,
        ),
        laneArea: const GridArea(
          c: 0,
          r: 0,
          width: 5,
          height: 5,
        ),
        entryCell: start,
        rampCell: goal,
        slots: const [],
        walkableCells:
            walkableCells ??
            {
              const SlotCell2D(c: 0, r: 2),
              const SlotCell2D(c: 1, r: 2),
              const SlotCell2D(c: 2, r: 2),
              const SlotCell2D(c: 3, r: 2),
              const SlotCell2D(c: 4, r: 2),
              const SlotCell2D(c: 2, r: 1),
              const SlotCell2D(c: 2, r: 0),
            },
      );
    }

    bool areAdjacent(SlotCell2D a, SlotCell2D b) {
      final dc = (a.c - b.c).abs();
      final dr = (a.r - b.r).abs();

      return dc + dr == 1;
    }

    test('finds a valid shortest path between two walkable cells', () {
      final layout = buildLayout();
      final grid = LaneGrid(layout);

      final path = grid.buildPath(
        start: start,
        goal: goal,
      );

      expect(path, isNotEmpty);
      expect(path.first, start);
      expect(path.last, goal);

      // Il percorso è una linea orizzontale di 5 celle.
      expect(path, hasLength(5));

      for (var i = 0; i < path.length; i++) {
        expect(
          grid.isWalkable(path[i]),
          isTrue,
          reason: 'La cella ${path[i]} deve essere percorribile.',
        );

        if (i > 0) {
          expect(
            areAdjacent(path[i - 1], path[i]),
            isTrue,
            reason: 'Il percorso deve avanzare di una cella al passo $i.',
          );
        }
      }
    });

    test('returns the start cell when the goal is not walkable', () {
      final grid = LaneGrid(buildLayout());
      const invalidGoal = SlotCell2D(c: 0, r: 0);

      final path = grid.buildPath(
        start: start,
        goal: invalidGoal,
      );

      expect(path, equals([start]));
    });

    test('returns the supplied start cell when the start is out of bounds', () {
      final grid = LaneGrid(buildLayout());
      const invalidStart = SlotCell2D(c: -1, r: 0);

      final path = grid.buildPath(
        start: invalidStart,
        goal: goal,
      );

      expect(path, equals([invalidStart]));
    });

    test('returns the start cell when the start cell is not walkable', () {
      final grid = LaneGrid(buildLayout());
      const invalidStart = SlotCell2D(c: 0, r: 1);

      final path = grid.buildPath(
        start: invalidStart,
        goal: goal,
      );

      expect(path, equals([invalidStart]));
    });

    test('returns a single cell when start and goal are the same', () {
      final grid = LaneGrid(buildLayout());
      const singleCell = SlotCell2D(c: 2, r: 2);

      final path = grid.buildPath(
        start: singleCell,
        goal: singleCell,
      );

      expect(path, equals([singleCell]));
    });

    test('returns the start cell when no path connects start and goal', () {
      final layout = buildLayout(
        walkableCells: {
          start,
          goal,
        },
      );

      final grid = LaneGrid(layout);

      final path = grid.buildPath(
        start: start,
        goal: goal,
      );

      expect(path, equals([start]));
    });
  });
}