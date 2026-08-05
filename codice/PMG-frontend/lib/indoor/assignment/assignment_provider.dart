import '../models/indoor_models.dart';
import '../parking_map_definition.dart';
import '../slot_map.dart';

class IndoorAssignmentProvider {
  final IndoorMapDefinition definition;

  const IndoorAssignmentProvider({
    required this.definition,
  });

  IndoorAssignment fromSlotId(String rawSlotId) {
    final parsed = ParsedSlotId.parse(rawSlotId);
    final layout = definition.layouts[parsed.floor];

    if (layout == null) {
      throw StateError(
        'Piano ${parsed.floor} non presente nella mappa '
        '${definition.data.parkingId}',
      );
    }

    final generatedSlot = layout.tryGetSlotById(rawSlotId) ??
        layout.tryGetSlotById(parsed.value) ??
        layout.tryGetSlotByNumber(parsed.slotNumber);

    if (generatedSlot == null) {
      throw StateError(
        'Posto ${parsed.value} non presente nel piano ${parsed.floor}',
      );
    }

    return IndoorAssignment(
      slot: IndoorSlotRef(
        floor: parsed.floor,
        slotId: generatedSlot.data.slotId,
      ),
      approachCell: generatedSlot.approachCell,
    );
  }
}
