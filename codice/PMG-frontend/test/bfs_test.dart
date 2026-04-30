import 'package:flutter_test/flutter_test.dart';
import 'package:park_mg/indoor/graph/lane_grid_mask.dart';

void main() {
  group('LaneGridMask BFS unit', () {
    List<(int c, int r)> toCellPath(List<Offset> path) => path.map(LaneGridMask.pointToCell).toList();

    bool areAdjacent((int c, int r) a, (int c, int r) b) {
      final dc = (a.$1 - b.$1).abs();
      final dr = (a.$2 - b.$2).abs();
      return dc + dr == 1;
    }

    test('finds a valid shortest path between two walkable cells', () {
      const start = (9, 0);
      const goal = (29, 24);

      final path = LaneGridMask.buildPathNormalized(
        startC: start.$1,
        startR: start.$2,
        goalC: goal.$1,
        goalR: goal.$2,
      );

      expect(path, isNotEmpty);

      final cells = toCellPath(path);
      expect(cells.first, start);
      expect(cells.last, goal);
      expect(cells.length, greaterThan(1));

      for (var i = 1; i < cells.length; i++) {
        expect(areAdjacent(cells[i - 1], cells[i]), isTrue,
            reason: 'Path must move to an adjacent cell at step $i.');
      }
    });

    test('returns the start cell when the goal is not walkable', () {
      const start = (9, 0);
      const invalidGoal = (0, 0);

      final path = LaneGridMask.buildPathNormalized(
        startC: start.$1,
        startR: start.$2,
        goalC: invalidGoal.$1,
        goalR: invalidGoal.$2,
      );

      expect(path, hasLength(1));
      expect(toCellPath(path).first, start);
    });

    test('returns entry point when the start is out of bounds', () {
      const invalidStart = (-1, 0);
      const goal = (9, 0);

      final path = LaneGridMask.buildPathNormalized(
        startC: invalidStart.$1,
        startR: invalidStart.$2,
        goalC: goal.$1,
        goalR: goal.$2,
      );

      expect(path, hasLength(1));
      expect(path.first, LaneGridMask.entryPointNormalized());
    });

    test('returns the start cell when the start cell is not walkable', () {
      const invalidStart = (0, 1);
      const goal = (9, 0);

      final path = LaneGridMask.buildPathNormalized(
        startC: invalidStart.$1,
        startR: invalidStart.$2,
        goalC: goal.$1,
        goalR: goal.$2,
      );

      expect(path, hasLength(1));
      expect(toCellPath(path).first, invalidStart);
    });

    test('returns a single cell when start and goal are the same', () {
      const singleCell = (9, 13);

      final path = LaneGridMask.buildPathNormalized(
        startC: singleCell.$1,
        startR: singleCell.$2,
        goalC: singleCell.$1,
        goalR: singleCell.$2,
      );

      expect(path, hasLength(1));
      expect(toCellPath(path).first, singleCell);
    });
  });
}