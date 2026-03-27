class Posto {
  final String slotId;
  final int floor;
  final String slotNumber;
  final int distanzaUscita;
  final bool riservatoDisabili;
  final bool riservatoIncinta;
  final bool disponibile;

  const Posto({
    required this.slotId,
    required this.floor,
    required this.slotNumber,
    required this.distanzaUscita,
    required this.riservatoDisabili,
    required this.riservatoIncinta,
    required this.disponibile,
  });

  factory Posto.fromJson(Map<String, dynamic> json) {
    return Posto(
      slotId: json['slotId']?.toString() ?? '',
      floor: (json['floor'] as num?)?.toInt() ?? 0,
      slotNumber: json['slotNumber']?.toString() ?? '',
      distanzaUscita: (json['distanzaUscita'] as num?)?.toInt() ?? 0,
      riservatoDisabili: json['riservatoDisabili'] as bool? ?? false,
      riservatoIncinta: json['riservatoIncinta'] as bool? ?? false,
      disponibile: json['disponibile'] as bool? ?? false,
    );
  }
}
