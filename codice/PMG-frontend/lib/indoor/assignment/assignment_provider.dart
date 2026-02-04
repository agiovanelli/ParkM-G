import 'package:park_mg/indoor/slot_map.dart';

import '../models/indoor_models.dart';
import '../graph/lane_grid_mask.dart';

class IndoorAssignmentProvider {
  const IndoorAssignmentProvider();

  IndoorAssignment fromSlotId(String slotId) {
    final parts = slotId.split('-');
    final floor = int.parse(parts[0]);
    final num = int.parse(parts[1]);

    final cell = slotMap18[num];
    if (cell == null) {
      final p = LaneGridMask.cellCenterToNormalized(
        LaneGridMask.rampCol,
        LaneGridMask.rampRow,
      );
      return IndoorAssignment(
        slot: IndoorSlotRef(floor: floor, slotId: slotId),
        slotPoint: IndoorPoint(p.dx, p.dy),
      );
    }

    final p = LaneGridMask.cellCenterToNormalized(cell.c, cell.r);

    return IndoorAssignment(
      slot: IndoorSlotRef(floor: floor, slotId: slotId),
      slotPoint: IndoorPoint(p.dx, p.dy),
    );
  }
}
