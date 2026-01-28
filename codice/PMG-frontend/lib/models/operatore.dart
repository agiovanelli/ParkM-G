class Operatore {
  final String id;
  final String username;
  final String nomeStruttura;
  final String parcheggioId; 

  Operatore({
    required this.id,
    required this.username,
    required this.nomeStruttura,
    required this.parcheggioId,
  });

  factory Operatore.fromJson(Map<String, dynamic> json) {
  return Operatore(
    id: json['id'] as String,
    username: json['username'] as String,
    nomeStruttura: json['nomeStruttura'] as String,
    parcheggioId: json['parcheggioId'] as String, 
  );
}
}