class IndoorMapDefinition {
  final String floorAsset;
  final int floors;

  const IndoorMapDefinition({required this.floorAsset, required this.floors});
}

IndoorMapDefinition buildDefaultIndoorMapDefinition() {
  return IndoorMapDefinition(floorAsset: 'assets/parking/floor.png', floors: 5);
}
