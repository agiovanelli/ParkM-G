/// Modello Operatore come tornato dal backend Spring Boot.
class Operatore {
  final String id;
  final String nomeStruttura;
  final String username;

  Operatore({
    required this.id,
    required this.nomeStruttura,
    required this.username,
  });

  factory Operatore.fromJson(Map<String, dynamic> json) {
    return Operatore(
      id: json['id'] as String,
      nomeStruttura: json['nomeStruttura'] as String,
      username: json['username'] as String,
    );
  }
}