class Parcheggio {
  final String id;
  final String nome;
  final String area;
  final int postiTotali;
  final int postiDisponibili;
  final int numPiani;
  final double latitudine;
  final double longitudine;
  final bool inEmergenza;

  const Parcheggio({
    required this.id,
    required this.nome,
    required this.area,
    required this.postiTotali,
    required this.postiDisponibili,
    required this.numPiani,
    required this.latitudine,
    required this.longitudine,
    required this.inEmergenza,
  });

  factory Parcheggio.fromJson(Map<String, dynamic> json) {
    return Parcheggio(
      id: (json['id'] ?? json['_id'] ?? '').toString(),
      nome: json['nome']?.toString() ?? 'N/D',
      area: json['area']?.toString() ?? 'N/D',
      postiTotali: (json['postiTotali'] as num?)?.toInt() ?? 0,
      postiDisponibili: (json['postiDisponibili'] as num?)?.toInt() ?? 0,
      numPiani: (json['numPiani'] as num?)?.toInt() ?? 0,
      latitudine: (json['latitudine'] as num?)?.toDouble() ?? 0.0,
      longitudine: (json['longitudine'] as num?)?.toDouble() ?? 0.0,
      inEmergenza: json['inEmergenza'] as bool? ?? false,
    );
  }
}
