enum TipoPosto {
  normale,
  disabili,
  incinta;

  static TipoPosto fromJson(dynamic value) {
    switch (value?.toString().toUpperCase()) {
      case 'DISABILI':
      case 'DISABILE':
        return TipoPosto.disabili;
      case 'INCINTA':
      case 'DONNE_INCINTA':
        return TipoPosto.incinta;
      case 'NORMALE':
      default:
        return TipoPosto.normale;
    }
  }
}

enum StatoPosto {
  libero,
  prenotato,
  occupato;

  static StatoPosto fromJson(
    dynamic value, {
    bool? disponibile,
  }) {
    switch (value?.toString().toUpperCase()) {
      case 'PRENOTATO':
      case 'RISERVATO':
        return StatoPosto.prenotato;
      case 'OCCUPATO':
        return StatoPosto.occupato;
      case 'LIBERO':
        return StatoPosto.libero;
      default:
        return disponibile == false
            ? StatoPosto.prenotato
            : StatoPosto.libero;
    }
  }
}

/// Rappresenta lo snapshot di un posto restituito dal backend.
///
/// Il posto non è più una risorsa MongoDB autonoma: viene salvato embedded
/// dentro `Parcheggio.configurazionePiani[].posti` e può essere incluso anche
/// nella risposta di una prenotazione.
class Posto {
  final String id;
  final String slotId;
  final String parcheggioId;
  final int numero;
  final String nome;
  final int piano;
  final TipoPosto tipo;
  final StatoPosto stato;
  final bool fuoriServizio;
  final int distanzaUscita;

  const Posto({
    required this.id,
    required this.slotId,
    required this.parcheggioId,
    required this.numero,
    required this.nome,
    required this.piano,
    required this.tipo,
    required this.stato,
    required this.fuoriServizio,
    required this.distanzaUscita,
  });

  factory Posto.fromJson(Map<String, dynamic> json) {
    final piano = (json['piano'] as num?)?.toInt() ?? 0;
    final numero = (json['numero'] as num?)?.toInt() ?? 0;
    final fallbackSlotId =
        '$piano-${numero.toString().padLeft(2, '0')}';

    final rawSlotId = (json['slotId'] ?? json['id'])?.toString().trim();
    final slotId = rawSlotId == null || rawSlotId.isEmpty
        ? fallbackSlotId
        : rawSlotId;

    final fuoriServizio =
        json['fuoriServizio'] as bool? ??
        json['disabilitato'] as bool? ??
        false;

    return Posto(
      id: (json['id'] ?? slotId).toString(),
      slotId: slotId,
      parcheggioId: json['parcheggioId']?.toString() ?? '',
      numero: numero,
      nome: (json['nome'] ?? 'P$piano-${numero.toString().padLeft(2, '0')}')
          .toString(),
      piano: piano,
      tipo: TipoPosto.fromJson(json['tipo']),
      stato: StatoPosto.fromJson(
        json['stato'],
        disponibile: json['disponibile'] as bool?,
      ),
      fuoriServizio: fuoriServizio,
      distanzaUscita: (json['distanzaUscita'] as num?)?.toInt() ?? 0,
    );
  }

  /// Compatibilità con le parti del frontend che usavano il vecchio DTO.
  bool get disponibile => stato == StatoPosto.libero && !fuoriServizio;

  /// Alias legacy: nel nuovo backend equivale a `fuoriServizio`.
  bool get disabilitato => fuoriServizio;

  bool get riservatoDisabili => tipo == TipoPosto.disabili;

  bool get riservatoIncinta => tipo == TipoPosto.incinta;

  int get floor => piano;

  String get slotNumber => numero.toString().padLeft(2, '0');
}
