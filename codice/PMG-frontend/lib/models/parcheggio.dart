
class Parcheggio {
  final String id;
  final String nome;
  final String area;
  final int postiTotali;
  final int postiDisponibili;
  final double latitudine;
  final double longitudine;
  final bool inEmergenza; 

  Parcheggio({
    required this.id,
    required this.nome,
    required this.area,
    required this.postiTotali,
    required this.postiDisponibili,
    required this.latitudine,
    required this.longitudine,
    required this.inEmergenza,
  });

  factory Parcheggio.fromJson(Map<String, dynamic> json) {
    return Parcheggio(
      id: json['id'] as String? ?? '',
      nome: json['nome'] as String? ?? 'N/D',
      area: json['area'] as String? ?? 'N/D',
      postiTotali: (json['postiTotali'] as num?)?.toInt() ?? 0,
      postiDisponibili: (json['postiDisponibili'] as num?)?.toInt() ?? 0,
      latitudine: (json['latitudine'] as num?)?.toDouble() ?? 0.0,
      longitudine: (json['longitudine'] as num?)?.toDouble() ?? 0.0,
      inEmergenza: json['inEmergenza'] as bool? ?? false,
    );
  }
}