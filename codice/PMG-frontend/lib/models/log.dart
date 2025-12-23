class Log {
  final String id;
  final String analiticheId;
  final String tipo;        // Evento | Allarme
  final String descrizione;
  final DateTime data;

  Log({
    required this.id,
    required this.analiticheId,
    required this.tipo,
    required this.descrizione,
    required this.data,
  });

  factory Log.fromJson(Map<String, dynamic> json) {
    return Log(
      id: json['id'] as String,
      analiticheId: json['analiticheId'] as String,
      tipo: json['tipo'] as String,
      descrizione: json['descrizione'] as String,
      data: DateTime.parse(json['data'] as String),
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'analiticheId': analiticheId,
      'tipo': tipo,
      'descrizione': descrizione,
      'data': data.toIso8601String(),
    };
  }
}
