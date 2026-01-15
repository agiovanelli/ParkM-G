class Analitiche {
  final String id;
  final String parcheggioId;
  final String nomeParcheggio;
  final String operatoreId;

  Analitiche({
    required this.id,
    required this.parcheggioId,
    required this.nomeParcheggio,
    required this.operatoreId,
  });

  factory Analitiche.fromJson(Map<String, dynamic> json) {
    return Analitiche(
      id: json['id'] as String,
      parcheggioId: json['parcheggioId'] as String,
      nomeParcheggio: json['nomeParcheggio'] as String,
      operatoreId: json['operatoreId'] as String,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'parcheggioId': parcheggioId,
      'nomeParcheggio': nomeParcheggio,
      'operatoreId': operatoreId,
    };
  }
}
