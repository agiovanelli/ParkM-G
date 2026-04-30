class Posto {
  final String id;
  final String parcheggioId;
  final int numero;
  final int piano;
  final int distanzaUscita;
  final bool riservatoDisabili;
  final bool riservatoIncinta;
  final bool disponibile;
  final bool disabilitato;

  const Posto({
    required this.id,
    required this.parcheggioId,
    required this.numero,
    required this.piano,
    required this.distanzaUscita,
    required this.riservatoDisabili,
    required this.riservatoIncinta,
    required this.disponibile,
    required this.disabilitato,
  });

  factory Posto.fromJson(Map<String, dynamic> json) {
    return Posto(
      id: json['id']?.toString() ?? '',
      parcheggioId: json['parcheggioId']?.toString() ?? '',
      numero: (json['numero'] as num?)?.toInt() ?? 0,
      piano: (json['piano'] as num?)?.toInt() ?? 0,
      distanzaUscita: (json['distanzaUscita'] as num?)?.toInt() ?? 0,
      riservatoDisabili: json['riservatoDisabili'] as bool? ?? false,
      riservatoIncinta: json['riservatoIncinta'] as bool? ?? false,
      disponibile: json['disponibile'] as bool? ?? false,
      disabilitato: json['disabilitato'] as bool? ?? false,
    );
  }

  String get slotId => '$piano-${numero.toString().padLeft(2, '0')}';
  int get floor => piano;
  String get slotNumber => numero.toString().padLeft(2, '0');
}