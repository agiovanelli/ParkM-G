import 'layout/parking_layout_generator.dart';
import 'models/indoor_models.dart';

class IndoorMapDefinition {
  final IndoorParkingData data;
  final Map<int, IndoorFloorLayout> layouts;

  const IndoorMapDefinition({
    required this.data,
    required this.layouts,
  });

  factory IndoorMapDefinition.fromJson(Map<String, dynamic> json) {
    final data = IndoorParkingData.fromJson(json);
    final layouts = const ParkingLayoutGenerator().generateParking(data);

    return IndoorMapDefinition(
      data: data,
      layouts: layouts,
    );
  }

  int get floors => data.floorCount;

  List<int> get floorNumbers {
    final numbers = layouts.keys.toList()..sort();
    return numbers;
  }

  IndoorFloorLayout layoutForFloor(int floor) {
    final layout = layouts[floor];
    if (layout == null) {
      throw StateError('Layout del piano $floor non disponibile');
    }
    return layout;
  }
}
