import 'package:flutter/material.dart';

class LaneGridMask {
  static const int cols = 40;
  static const int rows = 28;

  static const Offset gridTL = Offset(0.054, 0.536);
  static const Offset gridTR = Offset(0.511, 0.147);
  static const Offset gridBL = Offset(0.491, 0.906);

  static const int rampCol = 29;
  static const int rampRow = 0;
  static const int entryCol = 9;
  static const int entryRow = 0;

  static final Set<(int c, int r)> walkableCells = {
    for (int r = 0; r <= 24; r++) (9, r),
    for (int c = 9; c <= 29; c++) (c, 24),
    for (int r = 0; r <= 24; r++) (29, r),
    for (int c = 3; c <= 9; c++) (c, 3),
    for (int c = 3; c <= 9; c++) (c, 6),
    for (int c = 3; c <= 9; c++) (c, 11),
    for (int c = 3; c <= 9; c++) (c, 14),
    for (int c = 3; c <= 9; c++) (c, 16),
    for (int c = 3; c <= 9; c++) (c, 21),
    for (int c = 3; c <= 9; c++) (c, 24),
    for (int c = 29; c <= 35; c++) (c, 3),
    for (int c = 29; c <= 35; c++) (c, 6),
    for (int c = 29; c <= 35; c++) (c, 11),
    for (int c = 29; c <= 35; c++) (c, 14),
    for (int c = 29; c <= 35; c++) (c, 16),
    for (int c = 29; c <= 35; c++) (c, 21),
    for (int c = 29; c <= 35; c++) (c, 24),
    for (int c = 9; c <= 15; c++) (c, 13),
    for (int c = 22; c <= 29; c++) (c, 13),
    for (int c = 9; c <= 15; c++) (c, 16),
    for (int c = 22; c <= 29; c++) (c, 16),
  };

  static bool isWalkable(int c, int r) {
    return walkableCells.contains((c, r));
  }

  static List<Offset> buildPathNormalized({
    required int startC,
    required int startR,
    required int goalC,
    required int goalR,
  }) {
    final start = (startC, startR);
    final goal = (goalC, goalR);

    if (startC < 0 || startC >= cols || startR < 0 || startR >= rows) {
      return [entryPointNormalized()];
    }

    if (goalC < 0 || goalC >= cols || goalR < 0 || goalR >= rows) {
      return [cellCenterToNormalized(startC, startR)];
    }

    if (!isWalkable(startC, startR)) {
      return [cellCenterToNormalized(startC, startR)];
    }

    if (!isWalkable(goalC, goalR)) {
      return [cellCenterToNormalized(startC, startR)];
    }

    final queue = <(int c, int r)>[start];
    final visited = <(int c, int r)>{start};
    final parent = <(int c, int r), (int c, int r)?>{start: null};

    const dirs = <(int dc, int dr)>[(1, 0), (-1, 0), (0, 1), (0, -1)];

    int qi = 0;

    while (qi < queue.length) {
      final current = queue[qi++];

      if (current == goal) break;

      for (final d in dirs) {
        final nc = current.$1 + d.$1;
        final nr = current.$2 + d.$2;

        if (nc < 0 || nc >= cols || nr < 0 || nr >= rows) continue;
        if (!isWalkable(nc, nr)) continue;

        final next = (nc, nr);
        if (visited.contains(next)) continue;

        visited.add(next);
        parent[next] = current;
        queue.add(next);
      }
    }

    if (!parent.containsKey(goal)) {
      return [cellCenterToNormalized(startC, startR)];
    }

    final cells = <(int c, int r)>[];
    (int c, int r)? cur = goal;

    while (cur != null) {
      cells.add(cur);
      cur = parent[cur];
    }

    final ordered = cells.reversed.toList();
    return ordered.map((e) => cellCenterToNormalized(e.$1, e.$2)).toList();
  }

  static List<(int c, int r)> buildUPathCells({
    required int colLeft,
    required int colRight,
    required int rowTop,
    required int rowBottom,
  }) {
    final cells = <(int, int)>[];

    for (int r = rowTop; r <= rowBottom; r++) {
      cells.add((colLeft, r));
    }

    for (int c = colLeft + 1; c <= colRight; c++) {
      cells.add((c, rowBottom));
    }

    for (int r = rowBottom - 1; r >= rowTop; r--) {
      cells.add((colRight, r));
    }

    return cells;
  }

  static List<Offset> buildTurnAtGoalRowPathNormalized({
    required int startC,
    required int startR,
    required int goalC,
    required int goalR,
  }) {
    return buildPathNormalized(
      startC: startC,
      startR: startR,
      goalC: goalC,
      goalR: goalR,
    );
  }

  static Offset entryPointNormalized() =>
      cellCenterToNormalized(entryCol, entryRow);

  static Offset _u() => (gridTR - gridTL) / cols.toDouble();
  static Offset _v() => (gridBL - gridTL) / rows.toDouble();

  static Offset cellCenterToNormalized(int c, int r) {
    final u = _u();
    final v = _v();
    return gridTL + u * (c + 0.5) + v * (r + 0.5);
  }

  static (int c, int r) pointToCell(Offset p) {
    final u = _u();
    final v = _v();
    final d = p - gridTL;

    final det = u.dx * v.dy - u.dy * v.dx;

    final a = (d.dx * v.dy - d.dy * v.dx) / det;
    final b = (u.dx * d.dy - u.dy * d.dx) / det;

    final cc = a.clamp(0.0, cols - 1e-6);
    final rr = b.clamp(0.0, rows - 1e-6);

    return (cc.floor(), rr.floor());
  }

  static Offset cellCornerToNormalized(int c, int r) {
    final u = _u();
    final v = _v();
    return gridTL + u * c.toDouble() + v * r.toDouble();
  }

  static List<Offset> cellBlockPolygonNormalized({
    required int c,
    required int r,
    required int halfCols,
    required int halfRows,
  }) {
    final c0 = (c - halfCols).clamp(0, cols);
    final c1 = (c + halfCols + 1).clamp(0, cols);
    final r0 = (r - halfRows).clamp(0, rows);
    final r1 = (r + halfRows + 1).clamp(0, rows);

    final tl = cellCornerToNormalized(c0, r0);
    final tr = cellCornerToNormalized(c1, r0);
    final br = cellCornerToNormalized(c1, r1);
    final bl = cellCornerToNormalized(c0, r1);

    return [tl, tr, br, bl];
  }
}
