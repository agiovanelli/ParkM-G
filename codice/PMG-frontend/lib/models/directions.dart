import 'package:google_maps_flutter/google_maps_flutter.dart';

class DirectionsStep {
  final String htmlInstructions;
  final String distanceText;
  final String durationText;
  final String maneuver;
  final String polyline; 
  final LatLng start;
  final LatLng end;

  DirectionsStep({
    required this.htmlInstructions,
    required this.distanceText,
    required this.durationText,
    required this.maneuver,
    required this.polyline,
    required this.start,
    required this.end,
  });

  factory DirectionsStep.fromJson(Map<String, dynamic> j) => DirectionsStep(
        htmlInstructions: (j['htmlInstructions'] ?? '') as String,
        distanceText: (j['distanceText'] ?? '') as String,
        durationText: (j['durationText'] ?? '') as String,
        maneuver: (j['maneuver'] ?? '') as String,
        polyline: (j['polyline'] ?? '') as String,
        start: LatLng(
          (j['startLat'] as num).toDouble(),
          (j['startLng'] as num).toDouble(),
        ),
        end: LatLng(
          (j['endLat'] as num).toDouble(),
          (j['endLng'] as num).toDouble(),
        ),
      );
}

class DirectionsRoute {
  final String polyline;
  final String summary;
  final String distanceText;
  final String durationText;
  final String durationInTrafficText;
  final List<DirectionsStep> steps;

  DirectionsRoute({
    required this.polyline,
    required this.summary,
    required this.distanceText,
    required this.durationText,
    required this.durationInTrafficText,
    required this.steps,
  });

  factory DirectionsRoute.fromJson(Map<String, dynamic> j) => DirectionsRoute(
        polyline: (j['polyline'] ?? '') as String,
        summary: (j['summary'] ?? '') as String,
        distanceText: (j['distanceText'] ?? '') as String,
        durationText: (j['durationText'] ?? '') as String,
        durationInTrafficText: (j['durationInTrafficText'] ?? '') as String,
        steps: ((j['steps'] ?? []) as List)
            .map((e) => DirectionsStep.fromJson(e as Map<String, dynamic>))
            .toList(),
      );
}
