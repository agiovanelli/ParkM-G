class Posto {
  final String id;
  final int piano;
  final int distanzaUscita;
  final String parcheggioId;
  final bool disponibile;
  final bool riservatoDisabili;
  final bool riservatoIncinta;

  Posto({
    required this.id,
    required this.piano,
    required this.distanzaUscita,
    required this.parcheggioId,
    required this.disponibile,
    required this.riservatoDisabili,
    required this.riservatoIncinta,
  });

  factory Posto.fromJson(Map<String, dynamic> json) {
    return Posto(
      id: json['id'],
      piano: json['piano'],
      distanzaUscita: json['distanzaUscita'],
      parcheggioId: json['parcheggioId'],
      disponibile: json['disponibile'],
      riservatoDisabili: json['riservatoDisabili'],
      riservatoIncinta: json['riservatoIncinta'],
    );
  }
}