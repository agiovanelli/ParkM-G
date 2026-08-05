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
  final double? importoPagato;
  final Posto? posto;
  final DateTime? scadenzaArrivo;

  const PrenotazioneResponse({
    required this.id,
    required this.utenteId,
    required this.parcheggioId,
    required this.dataCreazione,
    required this.codiceQr,
    required this.stato,
    required this.dataIngresso,
    required this.dataUscita,
    this.importoPagato,
    required this.posto,
    required this.scadenzaArrivo,
  });

  static DateTime? _parseDateTime(dynamic value) {
    if (value == null) return null;
    if (value is DateTime) return value;
    final raw = value.toString().trim();
    if (raw.isEmpty) return null;
    return DateTime.tryParse(raw);
  }

  factory PrenotazioneResponse.fromJson(Map<String, dynamic> json) {
    final rawState = json['stato']?.toString() ?? 'attiva';

    return PrenotazioneResponse(
      id: (json['id'] ?? '').toString(),
      utenteId: (json['utenteId'] ?? '').toString(),
      parcheggioId: (json['parcheggioId'] ?? '').toString(),
      dataCreazione: _parseDateTime(json['dataCreazione']),
      codiceQr: json['codiceQr']?.toString(),
      stato: StatoPrenotazione.values.firstWhere(
        (state) => state.name == rawState,
        orElse: () => StatoPrenotazione.attiva,
      ),
      dataIngresso: _parseDateTime(json['dataIngresso']),
      dataUscita: _parseDateTime(json['dataUscita']),
      importoPagato: (json['importoPagato'] as num?)?.toDouble(),
      posto: json['posto'] is Map<String, dynamic>
          ? Posto.fromJson(json['posto'] as Map<String, dynamic>)
          : json['posto'] is Map
              ? Posto.fromJson(
                  Map<String, dynamic>.from(json['posto'] as Map),
                )
              : null,
      scadenzaArrivo: _parseDateTime(json['scadenzaArrivo']),
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
