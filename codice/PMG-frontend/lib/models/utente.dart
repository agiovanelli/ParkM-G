class Utente {
  final String id;
  final String nome;
  final String cognome;
  final String email;
  final String username;
  Map<String, String>? preferenze;

  Utente({
    required this.id,
    required this.nome,
    required this.cognome,
    required this.email,
    required this.username,
    this.preferenze,
  });

  factory Utente.fromJson(Map<String, dynamic> json) {
    final prefsDynamic = json['preferenze'];
    Map<String, String>? prefs;
    if (prefsDynamic != null) {
      prefs = (prefsDynamic as Map).map(
        (key, value) => MapEntry(key.toString(), value.toString()),
      );
    }

    return Utente(
      id: json['id'] as String,
      nome: json['nome'] as String,
      cognome: json['cognome'] as String,
      email: json['email'] as String,
      username: json['username'] as String,
      preferenze: prefs,
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'nome': nome,
        'cognome': cognome,
        'email': email,
        'username': username,
        'preferenze': preferenze,
      };
}
