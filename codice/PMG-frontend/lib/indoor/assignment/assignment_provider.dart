import 'package:flutter/foundation.dart';
import 'package:park_mg/indoor/slot_map.dart';

import '../models/indoor_models.dart';
import '../graph/lane_grid_mask.dart';

class IndoorAssignmentProvider {
  const IndoorAssignmentProvider();

  IndoorAssignment fromSlotId(String rawSlotId) {
    late final ParsedSlotId parsed;

    try {
      parsed = ParsedSlotId.parse(rawSlotId);
    } catch (e) {
      debugPrint('Formato slot non valido: $rawSlotId - $e');

      final fallbackPoint = LaneGridMask.cellCenterToNormalized(
        LaneGridMask.rampCol,
        LaneGridMask.rampRow,
      );

      return IndoorAssignment(
        slot: IndoorSlotRef(
          floor: 1,
          slotId: rawSlotId,
        ),
        slotPoint: IndoorPoint(fallbackPoint.dx, fallbackPoint.dy),
      );
    }

    final cell = baseSlotMap[parsed.slotNumber];

    if (cell == null) {
      debugPrint('Slot non mappato graficamente: ${parsed.value}');

      final fallbackPoint = LaneGridMask.cellCenterToNormalized(
        LaneGridMask.rampCol,
        LaneGridMask.rampRow,
      );

      return IndoorAssignment(
        slot: IndoorSlotRef(
          floor: parsed.floor,
          slotId: parsed.value,
        ),
        slotPoint: IndoorPoint(fallbackPoint.dx, fallbackPoint.dy),
      );
    }

    final point = LaneGridMask.cellCenterToNormalized(cell.c, cell.r);

    return IndoorAssignment(
      slot: IndoorSlotRef(
        floor: parsed.floor,
        slotId: parsed.value,
      ),
      slotPoint: IndoorPoint(point.dx, point.dy),
    );
  }
}