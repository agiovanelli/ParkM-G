class Log {
  final String id;
  final String analiticheId;
  final String tipo;
  final String descrizione;
  final String titolo;
  final DateTime data;

  Log({
    required this.id,
    required this.analiticheId,
    required this.tipo,
    required this.descrizione,
    required this.titolo,
    required this.data,
  });

  factory Log.fromJson(Map<String, dynamic> json) {
    return Log(
      id: json['id'] as String,
      analiticheId: json['analiticheId'] as String,
      tipo: json['tipo'] as String,
      descrizione: json['descrizione'] as String,
      titolo: json['titolo'] as String,
      data: DateTime.parse(json['data'] as String),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'analiticheId': analiticheId,
      'tipo': tipo,
      'descrizione': descrizione,
      'titolo': titolo,
      'data': data.toIso8601String(),
    };
  }
}
