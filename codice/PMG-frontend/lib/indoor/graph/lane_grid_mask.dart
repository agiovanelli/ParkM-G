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

  static List<Offset> buildTurnAtGoalRowPathNormalized({
    required int startC,
    required int startR,
    required int goalC,
    required int goalR,
  }) {
    final pts = <Offset>[];

    final stepR = (goalR >= startR) ? 1 : -1;
    for (int r = startR; r != goalR; r += stepR) {
      pts.add(cellCenterToNormalized(startC, r));
    }
    pts.add(cellCenterToNormalized(startC, goalR));

    final stepC = (goalC >= startC) ? 1 : -1;
    for (int c = startC + stepC; c != goalC; c += stepC) {
      pts.add(cellCenterToNormalized(c, goalR));
    }
    if (goalC != startC) {
      pts.add(cellCenterToNormalized(goalC, goalR));
    }

    return pts;
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

  // corner della cella (c,r) in coordinate normalized (0..1)
  static Offset cellCornerToNormalized(int c, int r) {
    final u = _u();
    final v = _v();
    return gridTL + u * c.toDouble() + v * r.toDouble();
  }

  // poligono (4 punti) del blocco di celle centrato in (c,r)
  // halfCols=2 => 2 celle a sx e 2 a dx (tot 5)
  // halfRows=1 => 1 sopra e 1 sotto (tot 3)
  static List<Offset> cellBlockPolygonNormalized({
    required int c,
    required int r,
    required int halfCols,
    required int halfRows,
  }) {
    final c0 = (c - halfCols).clamp(0, cols);
    final c1 = (c + halfCols + 1).clamp(0, cols); // +1 perché corner destro
    final r0 = (r - halfRows).clamp(0, rows);
    final r1 = (r + halfRows + 1).clamp(0, rows);

    final tl = cellCornerToNormalized(c0, r0);
    final tr = cellCornerToNormalized(c1, r0);
    final br = cellCornerToNormalized(c1, r1);
    final bl = cellCornerToNormalized(c0, r1);

    return [tl, tr, br, bl];
  }
}
