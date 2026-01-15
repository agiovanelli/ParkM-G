class PrenotazioneResponse {
  final String id;
  final String utenteId;
  final String parcheggioId;
  final String orario;
  final String codiceQr;

  PrenotazioneResponse({
    required this.id,
    required this.utenteId,
    required this.parcheggioId,
    required this.orario,
    required this.codiceQr,
  });

  factory PrenotazioneResponse.fromJson(Map<String, dynamic> json) {
    return PrenotazioneResponse(
      id: json['id'] as String,
      utenteId: json['utenteId'] as String,
      parcheggioId: json['parcheggioId'] as String,
      orario: json['orario'] as String,
      codiceQr: json['codiceQr'] as String,
    );
  }
}