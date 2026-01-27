// lib/widgets/map/nav_math.dart
import 'dart:math' as math;

import 'package:flutter_polyline_points/flutter_polyline_points.dart';
import 'package:geolocator/geolocator.dart';
import 'package:google_maps_flutter/google_maps_flutter.dart';

/// Distanza geodetica (WGS84) in metri.
double distanceMeters(LatLng a, LatLng b) => Geolocator.distanceBetween(
      a.latitude,
      a.longitude,
      b.latitude,
      b.longitude,
    );

/// Decodifica una polyline encoded (Google) in lista di LatLng.
List<LatLng> decodePolylineToLatLngs(String encoded) {
  if (encoded.isEmpty) return const [];
  final decoded = PolylinePoints.decodePolyline(encoded);
  return decoded.map((p) => LatLng(p.latitude, p.longitude)).toList();
}

/// Trova il punto più vicino a [p] sulla polyline [poly].
/// Ritorna:
/// - point: punto "snappato" sulla spezzata
/// - distanceMeters: distanza (metri) tra p e point
///
/// Nota: usa una proiezione locale in metri per calcolare la proiezione sul segmento
/// e poi misura la distanza finale con Geolocator (geodetica).
({LatLng point, double distanceMeters}) nearestPointOnPolyline(
  List<LatLng> poly,
  LatLng p,
) {
  if (poly.isEmpty) {
    return (point: p, distanceMeters: double.infinity);
  }
  if (poly.length == 1) {
    return (point: poly.first, distanceMeters: distanceMeters(poly.first, p));
  }

  double bestDist = double.infinity;
  LatLng bestPoint = poly.first;

  // Approx metri locali (sufficiente per snapping a scala urbana)
  double mx(double lon, double lat) =>
      lon * 111320.0 * math.cos(lat * math.pi / 180.0);
  double my(double lat) => lat * 110540.0;

  final px = mx(p.longitude, p.latitude);
  final py = my(p.latitude);

  for (int i = 0; i < poly.length - 1; i++) {
    final a = poly[i];
    final b = poly[i + 1];

    final ax = mx(a.longitude, a.latitude);
    final ay = my(a.latitude);
    final bx = mx(b.longitude, b.latitude);
    final by = my(b.latitude);

    final abx = bx - ax;
    final aby = by - ay;
    final apx = px - ax;
    final apy = py - ay;

    final ab2 = abx * abx + aby * aby;
    final t = ab2 == 0 ? 0.0 : (apx * abx + apy * aby) / ab2;
    final tt = t.clamp(0.0, 1.0);

    final sx = ax + abx * tt;
    final sy = ay + aby * tt;

    final sLat = sy / 110540.0;
    final sLng =
        sx / (111320.0 * math.cos(p.latitude * math.pi / 180.0)); // ok locale

    final sp = LatLng(sLat, sLng);
    final d = distanceMeters(sp, p);

    if (d < bestDist) {
      bestDist = d;
      bestPoint = sp;
    }
  }

  return (point: bestPoint, distanceMeters: bestDist);
}

/// Bearing in gradi (0..360) da [from] a [to], utile per camera/marker.
double bearingDegrees(LatLng from, LatLng to) {
  final lat1 = _degToRad(from.latitude);
  final lat2 = _degToRad(to.latitude);
  final dLon = _degToRad(to.longitude - from.longitude);

  final y = math.sin(dLon) * math.cos(lat2);
  final x =
      math.cos(lat1) * math.sin(lat2) - math.sin(lat1) * math.cos(lat2) * math.cos(dLon);

  final brng = math.atan2(y, x);
  final deg = (_radToDeg(brng) + 360.0) % 360.0;
  return deg;
}

/// Stima della distanza rimanente lungo la polyline [poly] a partire dal punto più vicino a [from].
/// Approccio:
/// 1) trova indice del segmento più vicino (tramite nearest point) facendo anche tracking dell'indice
/// 2) somma: distanza da punto snappato -> fine segmento + tutti i segmenti successivi
///
/// NB: è una stima (buona per MVP). Se vuoi precisione alta, conviene usare anche "distance" per step dal backend.
double remainingDistanceOnPolyline(List<LatLng> poly, LatLng from) {
  if (poly.length < 2) return 0;

  // Trova anche il segmento migliore.
  final snap = _nearestPointWithSegmentIndex(poly, from);
  final snapped = snap.point;
  final segIdx = snap.segmentIndex;

  double total = 0;

  // dal punto snappato alla fine del segmento
  total += distanceMeters(snapped, poly[segIdx + 1]);

  // segmenti successivi
  for (int i = segIdx + 1; i < poly.length - 1; i++) {
    total += distanceMeters(poly[i], poly[i + 1]);
  }

  return total;
}

/// Interno: come nearestPointOnPolyline ma ritorna anche l'indice del segmento migliore.
({LatLng point, double distanceMeters, int segmentIndex}) _nearestPointWithSegmentIndex(
  List<LatLng> poly,
  LatLng p,
) {
  if (poly.length < 2) {
    return (point: poly.isEmpty ? p : poly.first, distanceMeters: double.infinity, segmentIndex: 0);
  }

  double bestDist = double.infinity;
  LatLng bestPoint = poly.first;
  int bestSeg = 0;

  double mx(double lon, double lat) =>
      lon * 111320.0 * math.cos(lat * math.pi / 180.0);
  double my(double lat) => lat * 110540.0;

  final px = mx(p.longitude, p.latitude);
  final py = my(p.latitude);

  for (int i = 0; i < poly.length - 1; i++) {
    final a = poly[i];
    final b = poly[i + 1];

    final ax = mx(a.longitude, a.latitude);
    final ay = my(a.latitude);
    final bx = mx(b.longitude, b.latitude);
    final by = my(b.latitude);

    final abx = bx - ax;
    final aby = by - ay;
    final apx = px - ax;
    final apy = py - ay;

    final ab2 = abx * abx + aby * aby;
    final t = ab2 == 0 ? 0.0 : (apx * abx + apy * aby) / ab2;
    final tt = t.clamp(0.0, 1.0);

    final sx = ax + abx * tt;
    final sy = ay + aby * tt;

    final sLat = sy / 110540.0;
    final sLng = sx / (111320.0 * math.cos(p.latitude * math.pi / 180.0));

    final sp = LatLng(sLat, sLng);
    final d = distanceMeters(sp, p);

    if (d < bestDist) {
      bestDist = d;
      bestPoint = sp;
      bestSeg = i;
    }
  }

  return (point: bestPoint, distanceMeters: bestDist, segmentIndex: bestSeg);
}

double _degToRad(double d) => d * math.pi / 180.0;
double _radToDeg(double r) => r * 180.0 / math.pi;
