import 'dart:async';
import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:park_mg/models/log.dart';
import 'package:park_mg/models/posto.dart';
import '../models/utente.dart';
import '../models/operatore.dart';
import '../models/prenotazione.dart';

/// Eccezione generica per gli errori API.
class ApiException implements Exception {
  final String message;
  final int? statusCode;

  ApiException(this.message, [this.statusCode]);

  @override
  String toString() => 'ApiException($statusCode): $message';
}

class ApiClient {
  static const String _baseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://localhost:8080/api',
  );

  final http.Client _client;

  ApiClient({http.Client? client}) : _client = client ?? http.Client();

  // -------------------- UTENTI --------------------

  /// Login utente: POST /api/utenti/login
  Future<Utente> loginUtente(String email, String password) async {
    final uri = Uri.parse('$_baseUrl/utenti/login');
    final body = jsonEncode({'email': email, 'password': password});

    final resp = await _client.post(
      uri,
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: body,
    );

    if (resp.statusCode == 200 || resp.statusCode == 201) {
      final json = jsonDecode(resp.body) as Map<String, dynamic>;
      return Utente.fromJson(json);
    }

    if (resp.statusCode == 400 ||
        resp.statusCode == 401 ||
        resp.statusCode == 500) {
      throw ApiException('Credenziali non valide', resp.statusCode);
    }

    throw ApiException(
      'Errore backend utenti: HTTP ${resp.statusCode}',
      resp.statusCode,
    );
  }

  /// Registrazione utente: POST /api/utenti/registrazione
  Future<Utente> registraUtente(
    String nome,
    String cognome,
    String email,
    String password,
  ) async {
    final uri = Uri.parse('$_baseUrl/utenti/registrazione');
    final body = jsonEncode({
      'nome': nome,
      'cognome': cognome,
      'email': email,
      'password': password,
    });

    final resp = await _client.post(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: body,
    );

    if (resp.statusCode == 200 || resp.statusCode == 201) {
      final json = jsonDecode(resp.body) as Map<String, dynamic>;
      return Utente.fromJson(json);
    } else if (resp.statusCode == 400 || resp.statusCode == 409) {
      // usato lato backend per "Email già registrata"
      throw ApiException('Email già registrata', resp.statusCode);
    } else {
      throw ApiException(
        'Errore backend utenti: HTTP ${resp.statusCode}',
        resp.statusCode,
      );
    }
  }

  /// Aggiorna le preferenze: PUT /api/utenti/{id}/preferenze
  Future<void> aggiornaPreferenze(
    String utenteId,
    Map<String, String> preferenze,
  ) async {
    final uri = Uri.parse('$_baseUrl/utenti/$utenteId/preferenze');
    final body = jsonEncode(preferenze);

    final resp = await _client.put(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: body,
    );

    if (resp.statusCode != 204) {
      throw ApiException(
        'Errore aggiornamento preferenze: HTTP ${resp.statusCode}',
        resp.statusCode,
      );
    }
  }

  Future<PrenotazioneResponse> annullaPrenotazione({
    required String prenotazioneId,
    required String utenteId,
  }) async {
    final uri = Uri.parse(
      '$_baseUrl/prenotazioni/$prenotazioneId/utente/$utenteId',
    );

    final resp = await _client.delete(
      uri,
      headers: {'Accept': 'application/json'},
    );

    if (resp.statusCode == 200) {
      return PrenotazioneResponse.fromJson(jsonDecode(resp.body));
    }

    String msg = 'Errore annullamento (HTTP ${resp.statusCode})';
    if (resp.body.trim().isNotEmpty) {
      msg = resp.body.trim();
    }
    throw ApiException(msg, resp.statusCode);
  }

  // -------------------- OPERATORI --------------------

  /// Login operatore: POST /api/operatori/login
  Future<Operatore> loginOperatore(
    String nomeStruttura,
    String username,
  ) async {
    final uri = Uri.parse('$_baseUrl/operatori/login');
    final body = jsonEncode({
      'nomeStruttura': nomeStruttura,
      'username': username,
    });

    final resp = await _client.post(
      uri,
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
      },
      body: body,
    );

    if (resp.statusCode == 200 || resp.statusCode == 201) {
      final json = jsonDecode(resp.body) as Map<String, dynamic>;
      return Operatore.fromJson(json);
    }

    if (resp.statusCode == 400 ||
        resp.statusCode == 401 ||
        resp.statusCode == 403 ||
        resp.statusCode == 404 ||
        resp.statusCode == 500) {
      throw ApiException(
        'Operatore non registrato o credenziali errate',
        resp.statusCode,
      );
    }

    throw ApiException(
      'Errore backend operatori: HTTP ${resp.statusCode}',
      resp.statusCode,
    );
  }

  Future<List<Log>> getLogByAnaliticaId(String analiticaId) async {
    final uri = Uri.parse('$_baseUrl/$analiticaId');

    final resp = await _client.get(uri);

    if (resp.statusCode == 200 || resp.statusCode == 201) {
      final List<dynamic> jsonList = jsonDecode(resp.body) as List<dynamic>;
      return jsonList
          .map((json) => Log.fromJson(json as Map<String, dynamic>))
          .toList();
    } else {
      throw ApiException(
        'Errore recupero log: HTTP ${resp.statusCode}',
        resp.statusCode,
      );
    }
  }

  //BLOCCO DI EMERGENZA PARCHEGGIO
  Future<void> impostaEmergenza(
    String parcheggioId,
    bool attiva,
    String motivo,
  ) async {
    final url = Uri.parse(
      '$_baseUrl/parcheggi/$parcheggioId/emergenza',
    ).replace(queryParameters: {'attiva': attiva.toString(), 'motivo': motivo});

    final resp = await _client.patch(
      url,
      headers: {
        // Rimuovi Content-Type se non mandi un JSON nel body, lascia solo Accept
        'Accept': 'application/json',
      },
    );

    if (resp.statusCode != 200) {
      throw ApiException('Errore attivazione emergenza', resp.statusCode);
    }
  }

  //-------------------- PRENOTAZIONI --------------------
  /// Prenota un parcheggio: POST /api/parcheggi/prenota
  Future<PrenotazioneResponse> prenotaParcheggio(
    String utenteId,
    String parcheggioId,
    String dataCreazione,
  ) async {
    final uri = Uri.parse('$_baseUrl/parcheggi/prenota');
    final body = jsonEncode({
      'utenteId': utenteId,
      'parcheggioId': parcheggioId,
      'dataCreazione': dataCreazione,
    });

    final resp = await _client.post(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: body,
    );

    if (resp.statusCode == 200 || resp.statusCode == 201) {
      final json = jsonDecode(resp.body) as Map<String, dynamic>;
      return PrenotazioneResponse.fromJson(json);
    } else if (resp.statusCode == 400) {
      // Ad esempio: parcheggio già occupato o dati mancanti
      throw ApiException(
        'Dati prenotazione non validi o parcheggio non disponibile',
        resp.statusCode,
      );
    } else if (resp.statusCode == 404) {
      throw ApiException('Utente o parcheggio non trovato', resp.statusCode);
    } else {
      throw ApiException(
        'Errore durante la prenotazione: HTTP ${resp.statusCode}',
        resp.statusCode,
      );
    }
  }

  //API recupero storico prenotazioni per utente
  // GET /api/prenotazioni/utente/{utenteId}

  Future<List<PrenotazioneResponse>> getStoricoPrenotazioni(
    String utenteId,
  ) async {
    final uri = Uri.parse('$_baseUrl/prenotazioni/utente/$utenteId');

    final resp = await _client.get(
      uri,
      headers: {'Content-Type': 'application/json'},
    );

    if (resp.statusCode == 200 || resp.statusCode == 201) {
      final List<dynamic> jsonList = jsonDecode(resp.body);
      // Trasformiamo ogni elemento della lista JSON in un oggetto PrenotazioneResponse
      return jsonList
          .map((json) => PrenotazioneResponse.fromJson(json))
          .toList();
    } else if (resp.statusCode == 204) {
      // Se il backend restituisce 204 No Content, restituiamo una lista vuota
      return [];
    } else {
      throw ApiException(
        'Errore nel recupero dello storico: HTTP ${resp.statusCode}',
        resp.statusCode,
      );
    }
  }

  // Valida codice QR: POST /api/prenotazioni/valida-ingresso/{codiceQr}
  Future<PrenotazioneResponse> validaIngresso(String codiceQr) async {
    final resp = await _client.post(
      Uri.parse('$_baseUrl/prenotazioni/valida-ingresso/$codiceQr'),
      headers: {'Accept': 'application/json'},
    );

    if (resp.statusCode == 200) {
      return PrenotazioneResponse.fromJson(jsonDecode(resp.body));
    }

    String msg = 'Errore validazione ingresso (HTTP ${resp.statusCode})';
    try {
      final decoded = jsonDecode(resp.body);
      if (decoded is Map && decoded['message'] is String) {
        msg = decoded['message'] as String;
      } else if (decoded is String && decoded.trim().isNotEmpty) {
        msg = decoded.trim();
      }
    } catch (_) {
      if (resp.body.trim().isNotEmpty) msg = resp.body.trim();
    }

    throw ApiException(msg, resp.statusCode);
  }

  /// Recupera la prenotazione tramite QR Code senza modificarne lo stato
  Future<Map<String, dynamic>> getPrenotazioneByQr(String codiceQr) async {
    final url = Uri.parse('$_baseUrl/prenotazioni/qr/$codiceQr');

    try {
      final response = await http.get(
        url,
        headers: {'Content-Type': 'application/json'},
      );

      if (response.statusCode == 200) {
        return json.decode(response.body);
      } else {
        final errorMsg = response.body.isNotEmpty
            ? response.body
            : 'Prenotazione non trovata';
        throw Exception(errorMsg);
      }
    } catch (e) {
      throw Exception('Errore di connessione: $e');
    }
  }

  // Calcola importo da pagare: GET /api/prenotazioni/{id}/calcola-importo
  Future<double> calcolaImporto(String id) async {
    final response = await http.get(
      Uri.parse('$_baseUrl/prenotazioni/$id/calcola-importo'),
    );
    if (response.statusCode == 200) {
      return double.parse(response.body);
    }
    throw ApiException('Errore calcolo importo: ${response.body}');
  }

  // Paga prenotazione: POST /api/prenotazioni/{id}/paga
  Future<PrenotazioneResponse> pagaPrenotazione(
    String id,
    double importo,
  ) async {
    final response = await http.post(
      Uri.parse('$_baseUrl/prenotazioni/$id/paga'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'importo': importo}),
    );
    if (response.statusCode == 200) {
      return PrenotazioneResponse.fromJson(jsonDecode(response.body));
    }
    throw ApiException('Errore pagamento: ${response.body}');
  }

  // Valida uscita: POST /api/prenotazioni/valida-uscita/{codiceQr}
  Future<PrenotazioneResponse> validaUscita(String qr) async {
    final response = await http.post(
      Uri.parse('$_baseUrl/prenotazioni/valida-uscita/$qr'),
    );
    if (response.statusCode == 200) {
      return PrenotazioneResponse.fromJson(jsonDecode(response.body));
    }
    throw ApiException(
      response.body,
    ); // Passa il messaggio di errore (es. "Devi pagare")
  }

  // RECUPERO DIREZIONI E PERCORSO
  Future<Map<String, dynamic>> getDirections({
    required double oLat,
    required double oLng,
    required double dLat,
    required double dLng,
  }) async {
    final uri = Uri.parse(
      '$_baseUrl/maps/directions'
      '?oLat=$oLat&oLng=$oLng&dLat=$dLat&dLng=$dLng',
    );

    final resp = await _client.get(uri);

    if (resp.statusCode == 200) {
      return jsonDecode(resp.body) as Map<String, dynamic>;
    }
    throw ApiException(
      'Errore directions: HTTP ${resp.statusCode}',
      resp.statusCode,
    );
  }

  Future<int?> getRoadDistanceMeters({
    required double oLat,
    required double oLng,
    required double dLat,
    required double dLng,
  }) async {
    final data = await getDirections(
      oLat: oLat,
      oLng: oLng,
      dLat: dLat,
      dLng: dLng,
    );

    final routes = (data['routes'] as List?) ?? const [];
    if (routes.isEmpty) return null;

    final first = routes.first as Map<String, dynamic>;
    final m = first['distanceMeters'];
    if (m is num) return m.round();

    return null;
  }

  // Recupera una prenotazione specifica dallo storico dell'utente
  Future<PrenotazioneResponse?> getPrenotazioneByIdFromStorico(
    String utenteId,
    String prenotazioneId,
  ) async {
    final list = await getStoricoPrenotazioni(utenteId);
    try {
      return list.firstWhere((p) => p.id == prenotazioneId);
    } catch (_) {
      return null;
    }
  }

  //LOCALIZZAZIONE GPS
  Future<Map<String, dynamic>> geocode({required String address}) async {
    final uri = Uri.parse(
      '$_baseUrl/maps/geocode',
    ).replace(queryParameters: {'address': address});

    final res = await http.get(uri);
    if (res.statusCode != 200) {
      throw ApiException('Geocode failed (${res.statusCode})');
    }
    return jsonDecode(res.body) as Map<String, dynamic>;
  }

  //RECUPERO INFORMAZIONI PARCHEGGIO
  Future<Map<String, dynamic>> getParcheggioById(String parcheggioId) async {
    final uri = Uri.parse('$_baseUrl/parcheggi/$parcheggioId');

    final resp = await _client.get(
      uri,
      headers: {'Accept': 'application/json'},
    );

    if (resp.statusCode == 200) {
      return jsonDecode(resp.body) as Map<String, dynamic>;
    }

    throw ApiException(
      'Errore recupero parcheggio: HTTP ${resp.statusCode}',
      resp.statusCode,
    );
  }

  Future<List<Posto>> getPostiParcheggio(
    String parcheggioId, {
    int? piano,
  }) async {
    final uri = Uri.parse(
      piano == null
          ? '$_baseUrl/parcheggi/$parcheggioId/posti'
          : '$_baseUrl/parcheggi/$parcheggioId/posti?piano=$piano',
    );

    final response = await _client.get(uri);

    if (response.statusCode != 200) {
      throw ApiException(
        'Errore recupero posti parcheggio: HTTP ${response.statusCode}',
        response.statusCode,
      );
    }

    final data = jsonDecode(response.body) as List<dynamic>;
    return data.map((e) => Posto.fromJson(e as Map<String, dynamic>)).toList();
  }

  Future<List<PrenotazioneResponse>> getPrenotazioniByParcheggio(
    String parcheggioId,
  ) async {
    final uri = Uri.parse('$_baseUrl/prenotazioni/parcheggio/$parcheggioId');

    final response = await http.get(uri);

    if (response.statusCode != 200) {
      throw Exception('Errore recupero prenotazioni del parcheggio');
    }

    final data = jsonDecode(response.body) as List<dynamic>;

    return data
        .map((e) => PrenotazioneResponse.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  Future<List<Map<String, dynamic>>> getLogAnalitiche(
    String analiticaId,
  ) async {
    final url = Uri.parse('$_baseUrl/analitiche/$analiticaId/log');
    final response = await http.get(url);

    if (response.statusCode != 200) {
      throw Exception('Errore nel caricamento dei log');
    }

    final jsonList = jsonDecode(response.body);
    if (jsonList is! List) {
      throw Exception('Il backend non ha restituito una lista JSON');
    }

    return jsonList.cast<Map<String, dynamic>>();
  }

  Future<void> updateLogSeverity(String logId, String severity) async {
    final url = Uri.parse('$_baseUrl/log/$logId/severity?severity=$severity');
    final response = await http.put(url);

    if (response.statusCode != 200) {
      throw Exception('Errore aggiornamento severity: ${response.statusCode}');
    }
  }

  Future<void> updateLogCategory(String logId, String category) async {
    final url = Uri.parse('$_baseUrl/log/$logId/category?category=$category');
    final response = await http.put(url);

    if (response.statusCode != 200) {
      throw Exception('Errore aggiornamento category: ${response.statusCode}');
    }
  }

  Future<Map<String, dynamic>> getAnaliticaByParcheggioId(
    String parcheggioId,
  ) async {
    final url = Uri.parse('$_baseUrl/analitiche/parcheggio/$parcheggioId');
    final response = await http.get(url);

    if (response.statusCode != 200) {
      throw Exception('Errore nel caricamento analitica del parcheggio');
    }

    final jsonMap = jsonDecode(response.body);
    if (jsonMap is! Map<String, dynamic>) {
      throw Exception('Il backend non ha restituito un\'analitica valida');
    }

    return jsonMap;
  }

  Future<Posto> updatePostoDisabilitato({
    required String parcheggioId,
    required int piano,
    required int numero,
    required bool disabilitato,
  }) async {
    final uri = Uri.parse(
      '$_baseUrl/posti/parcheggio/$parcheggioId/piano/$piano/numero/$numero/disabilitato',
    ).replace(queryParameters: {'disabilitato': disabilitato.toString()});

    final resp = await _client.patch(
      uri,
      headers: {'Accept': 'application/json'},
    );

    if (resp.statusCode == 200) {
      final json = jsonDecode(resp.body) as Map<String, dynamic>;
      return Posto.fromJson(json);
    }

    throw ApiException(
      'Errore aggiornamento stato disabilitato posto: HTTP ${resp.statusCode}',
      resp.statusCode,
    );
  }

  Future<Posto> updatePostoDisponibilita({
    required String parcheggioId,
    required int piano,
    required int numero,
    required bool disponibile,
  }) async {
    final uri = Uri.parse(
      '$_baseUrl/posti/parcheggio/$parcheggioId/piano/$piano/numero/$numero/disponibilita',
    ).replace(queryParameters: {'disponibile': disponibile.toString()});

    final resp = await _client.patch(
      uri,
      headers: {'Accept': 'application/json'},
    );

    if (resp.statusCode == 200) {
      final json = jsonDecode(resp.body) as Map<String, dynamic>;
      return Posto.fromJson(json);
    }

    throw ApiException(
      'Errore aggiornamento disponibilità posto: HTTP ${resp.statusCode}',
      resp.statusCode,
    );
  }

  Future<PrenotazioneResponse> confermaParcheggio(String prenotazioneId) async {
    final uri = Uri.parse(
      '$_baseUrl/prenotazioni/$prenotazioneId/parcheggiato',
    );

    try {
      final response = await http
          .post(uri, headers: {'Content-Type': 'application/json'})
          .timeout(const Duration(seconds: 10));

      if (response.statusCode != 200) {
        final body = response.body.trim();
        throw ApiException(
          body.isNotEmpty
              ? 'Errore conferma parcheggio: $body'
              : 'Errore conferma parcheggio (${response.statusCode})',
        );
      }

      if (response.body.trim().isEmpty) {
        throw ApiException(
          'Risposta vuota dal server durante la conferma parcheggio.',
        );
      }

      final json = jsonDecode(response.body) as Map<String, dynamic>;
      return PrenotazioneResponse.fromJson(json);
    } on TimeoutException {
      throw ApiException('Timeout durante la conferma del parcheggio.');
    } on FormatException {
      throw ApiException('Risposta non valida dal server.');
    } on ApiException {
      rethrow;
    } catch (e) {
      throw ApiException('Errore conferma parcheggio: $e');
    }
  }
}
