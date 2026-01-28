import 'package:flutter/material.dart';

class PrenotazioneResponse {
  final String id;
  final String utenteId;
  final String parcheggioId;

  final DateTime? dataCreazione;
  final String? codiceQr;
  final StatoPrenotazione stato;

  final DateTime? dataIngresso;
  final DateTime? dataUscita;

  PrenotazioneResponse({
    required this.id,
    required this.utenteId,
    required this.parcheggioId,
    required this.dataCreazione,
    required this.codiceQr,
    required this.stato,
    required this.dataIngresso,
    required this.dataUscita,
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
        orElse: () => StatoPrenotazione.ATTIVA,
      ),

      dataIngresso: _parseDT(json['dataIngresso']),
      dataUscita: _parseDT(json['dataUscita']),
    );
  }
}

enum StatoPrenotazione {
  ATTIVA, // Prenotata (attesa entro 10 min)
  IN_CORSO, // Utente entrato (timer avviato)
  PAGATO, // Saldo effettuato (pronto per uscire)
  CONCLUSA, // Utente uscito (posto liberato)
  SCADUTA, // Tempo per l'ingresso esaurito
  ANNULLATA, // Cancellata dall'utente
}

extension StatoPrenotazioneExtension on StatoPrenotazione {
  String get label {
    switch (this) {
      case StatoPrenotazione.ATTIVA:
        return 'Attiva';
      case StatoPrenotazione.IN_CORSO:
        return 'In Corso';
      case StatoPrenotazione.PAGATO:
        return 'Pagato';
      case StatoPrenotazione.CONCLUSA:
        return 'Conclusa';
      case StatoPrenotazione.SCADUTA:
        return 'Scaduta';
      case StatoPrenotazione.ANNULLATA:
        return 'Annullata';
    }
  }

  Color get color {
    switch (this) {
      case StatoPrenotazione.ATTIVA:
        return Colors.orange;
      case StatoPrenotazione.IN_CORSO:
        return Colors.blue;
      case StatoPrenotazione.PAGATO:
        return Colors.green;
      case StatoPrenotazione.CONCLUSA:
        return Colors.grey;
      case StatoPrenotazione.SCADUTA:
        return Colors.red;
      case StatoPrenotazione.ANNULLATA:
        return Colors.deepOrange;
    }
  }
}
