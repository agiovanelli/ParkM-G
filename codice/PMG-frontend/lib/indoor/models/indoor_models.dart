import 'package:flutter/material.dart';

class IndoorSlotRef {
  final int floor;  
  final String slotId; 
  const IndoorSlotRef({required this.floor, required this.slotId});
}

class IndoorPoint {
  final double x; 
  final double y; 
  const IndoorPoint(this.x, this.y);

  Offset toOffset() => Offset(x, y);
}

class IndoorAssignment {
  final IndoorSlotRef slot;
  final IndoorPoint slotPoint;
  const IndoorAssignment({required this.slot, required this.slotPoint});
}