import 'package:flutter/material.dart';
import 'posto.dart';

class PrenotazioneResponse {
  final String id;
  final String utenteId;
  final String parcheggioId;
  final DateTime? dataCreazione;
  final String? codiceQr;
  final StatoPrenotazione stato;
  final DateTime? dataIngresso;
  final DateTime? dataUscita;
  final Posto? posto;
  final DateTime? scadenzaArrivo;

  PrenotazioneResponse({
    required this.id,
    required this.utenteId,
    required this.parcheggioId,
    required this.dataCreazione,
    required this.codiceQr,
    required this.stato,
    required this.dataIngresso,
    required this.dataUscita,
    required this.posto,
    required this.scadenzaArrivo,
  });

  static DateTime? _parseDT(dynamic v) {
    if (v == null) return null;
    if (v is String && v.isNotEmpty) return DateTime.tryParse(v);
    return null;
  }

  factory PrenotazioneResponse.fromJson(Map<String, dynamic> json) {
    return PrenotazioneResponse(
      id: (json['id'] ?? '') as String,
      utenteId: (json['utenteId'] ?? '') as String,
      parcheggioId: (json['parcheggioId'] ?? '') as String,
      dataCreazione: _parseDT(json['dataCreazione']),
      codiceQr: json['codiceQr'] as String?,
      stato: StatoPrenotazione.values.firstWhere(
        (e) => e.name == (json['stato'] ?? 'ATTIVA'),
        orElse: () => StatoPrenotazione.attiva,
      ),
      dataIngresso: _parseDT(json['dataIngresso']),
      dataUscita: _parseDT(json['dataUscita']),
      posto: json['posto'] != null
          ? Posto.fromJson(json['posto'] as Map<String, dynamic>)
          : null,
      scadenzaArrivo: json['scadenzaArrivo'] != null
          ? DateTime.parse(json['scadenzaArrivo'])
          : null,
    );
  }
}

enum StatoPrenotazione {
  attiva,
  inCorso,
  parcheggiato,
  pagato,
  conclusa,
  scaduta,
  annullata,
}

extension StatoPrenotazioneExtension on StatoPrenotazione {
  String get label {
    switch (this) {
      case StatoPrenotazione.attiva:
        return 'Attiva';
      case StatoPrenotazione.inCorso:
        return 'In corso';
      case StatoPrenotazione.parcheggiato:
        return 'Parcheggiato';
      case StatoPrenotazione.pagato:
        return 'Pagato';
      case StatoPrenotazione.conclusa:
        return 'Conclusa';
      case StatoPrenotazione.scaduta:
        return 'Scaduta';
      case StatoPrenotazione.annullata:
        return 'Annullata';
    }
  }

  Color get color {
    switch (this) {
      case StatoPrenotazione.attiva:
        return Colors.orange;
      case StatoPrenotazione.inCorso:
        return Colors.blue;
      case StatoPrenotazione.parcheggiato:
        return Colors.deepPurple;
      case StatoPrenotazione.pagato:
        return Colors.green;
      case StatoPrenotazione.conclusa:
        return Colors.grey;
      case StatoPrenotazione.scaduta:
        return Colors.red;
      case StatoPrenotazione.annullata:
        return Colors.deepOrange;
    }
  }

  bool get isGestibileDopoParcheggio {
    return this == StatoPrenotazione.parcheggiato ||
        this == StatoPrenotazione.pagato;
  }
}
