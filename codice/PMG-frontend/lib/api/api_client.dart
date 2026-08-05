import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:park_mg/indoor/parking_map_definition.dart';
import 'package:park_mg/models/log.dart';
import 'package:park_mg/models/posto.dart';

import '../models/operatore.dart';
import '../models/prenotazione.dart';
import '../models/utente.dart';

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

  // =========================================================
  // UTENTI
  // =========================================================

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
      throw ApiException('Email già registrata', resp.statusCode);
    } else {
      throw ApiException(
        'Errore backend utenti: HTTP ${resp.statusCode}',
        resp.statusCode,
      );
    }
  }

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

  // =========================================================
  // OPERATORI
  // =========================================================

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

  // =========================================================
  // PRENOTAZIONI
  // =========================================================

  Future<PrenotazioneResponse> prenotaParcheggio({
    required String utenteId,
    required String parcheggioId,
    required String dataCreazione,
    double? originLat,
    double? originLng,
  }) async {
    final uri = Uri.parse('$_baseUrl/parcheggi/prenota');
    final body = jsonEncode({
      'utenteId': utenteId,
      'parcheggioId': parcheggioId,
      'dataCreazione': dataCreazione,
      'originLat': originLat,
      'originLng': originLng,
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
      return jsonList
          .map((json) => PrenotazioneResponse.fromJson(json))
          .toList();
    } else if (resp.statusCode == 204) {
      return [];
    } else {
      throw ApiException(
        'Errore nel recupero dello storico: HTTP ${resp.statusCode}',
        resp.statusCode,
      );
    }
  }

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

  Future<PrenotazioneResponse> validaUscita(String qr) async {
    final response = await _client.post(
      Uri.parse('$_baseUrl/prenotazioni/valida-uscita/$qr'),
    );
    if (response.statusCode == 200) {
      return PrenotazioneResponse.fromJson(jsonDecode(response.body));
    }
    throw ApiException(response.body);
  }

  Future<PrenotazioneResponse> getPrenotazioneByQr(
    String codiceQr,
  ) async {
    final uri = Uri.parse('$_baseUrl/prenotazioni/qr/$codiceQr');

    final response = await _client.get(
      uri,
      headers: {'Accept': 'application/json'},
    );

    if (response.statusCode == 200) {
      final json = jsonDecode(response.body) as Map<String, dynamic>;
      return PrenotazioneResponse.fromJson(json);
    }

    final body = response.body.trim();
    throw ApiException(
      body.isNotEmpty
          ? body
          : 'Prenotazione non trovata',
      response.statusCode,
    );
  }

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

  Future<double> calcolaImporto(String id) async {
    final response = await _client.get(
      Uri.parse('$_baseUrl/prenotazioni/$id/calcola-importo'),
    );
    if (response.statusCode == 200) {
      return double.parse(response.body);
    }
    throw ApiException('Errore calcolo importo: ${response.body}');
  }

  Future<PrenotazioneResponse> pagaPrenotazione(
    String id,
    double importo,
  ) async {
    final response = await _client.post(
      Uri.parse('$_baseUrl/prenotazioni/$id/paga'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'importo': importo}),
    );
    if (response.statusCode == 200) {
      return PrenotazioneResponse.fromJson(jsonDecode(response.body));
    }
    throw ApiException('Errore pagamento: ${response.body}');
  }

  Future<T> retry<T>(
    Future<T> Function() fn, {
    int retries = 3,
    Duration delay = const Duration(seconds: 2),
  }) async {
    int attempt = 0;

    while (true) {
      try {
        return await fn();
      } on TimeoutException {
        attempt++;

        if (attempt >= retries) {
          throw ApiException('Timeout dopo $retries tentativi');
        }

        await Future.delayed(delay);
      }
    }
  }

  Future<PrenotazioneResponse> confermaParcheggio(
    String prenotazioneId,
  ) async {
    final uri = Uri.parse(
      '$_baseUrl/prenotazioni/$prenotazioneId/parcheggiato',
    );

    return retry(() async {
      final response = await _client
          .post(uri, headers: {'Accept': 'application/json'})
          .timeout(const Duration(seconds: 10));

      if (response.statusCode != 200) {
        final body = response.body.trim();
        throw ApiException(
          body.isNotEmpty
              ? body
              : 'Errore conferma parcheggio',
          response.statusCode,
        );
      }

      final json = jsonDecode(response.body) as Map<String, dynamic>;
      return PrenotazioneResponse.fromJson(json);
    });
  }

  Future<List<PrenotazioneResponse>> getPrenotazioniByParcheggio(
    String parcheggioId,
  ) async {
    final uri = Uri.parse('$_baseUrl/prenotazioni/parcheggio/$parcheggioId');

    final response = await _client.get(uri);

    if (response.statusCode != 200) {
      throw Exception('Errore recupero prenotazioni del parcheggio');
    }

    final data = jsonDecode(response.body) as List<dynamic>;

    return data
        .map((e) => PrenotazioneResponse.fromJson(e as Map<String, dynamic>))
        .toList();
  }

  // =========================================================
  // MAPPE / GEOLOCALIZZAZIONE
  // =========================================================

  Future<Map<String, dynamic>> geocode({required String address}) async {
    final uri = Uri.parse(
      '$_baseUrl/maps/geocode',
    ).replace(queryParameters: {'address': address});

    final res = await _client.get(uri);
    if (res.statusCode != 200) {
      throw ApiException('Geocode failed (${res.statusCode})');
    }
    return jsonDecode(res.body) as Map<String, dynamic>;
  }

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

  // =========================================================
  // PARCHEGGI / POSTI
  // =========================================================

  Future<List<dynamic>> getParcheggiNearby({
    required double lat,
    required double lng,
    required double radius,
  }) async {
    final uri = Uri.parse('$_baseUrl/parcheggi/nearby').replace(
      queryParameters: {
        'lat': lat.toString(),
        'lng': lng.toString(),
        'radius': radius.toString(),
      },
    );

    final resp = await _client
        .get(uri, headers: {'Accept': 'application/json'})
        .timeout(const Duration(seconds: 8));

    if (resp.statusCode == 200) {
      return jsonDecode(resp.body) as List<dynamic>;
    }

    throw ApiException(
      'Errore caricamento parcheggi (${resp.statusCode})',
      resp.statusCode,
    );
  }

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


  Future<IndoorMapDefinition> getIndoorParkingMap(
    String parcheggioId,
  ) async {
    final uri = Uri.parse(
      '$_baseUrl/parcheggi/$parcheggioId/mappa',
    );

    final response = await _client.get(
      uri,
      headers: {'Accept': 'application/json'},
    );

    if (response.statusCode != 200) {
      throw ApiException(
        'Errore recupero mappa parcheggio: HTTP ${response.statusCode}',
        response.statusCode,
      );
    }

    final json = jsonDecode(response.body) as Map<String, dynamic>;
    return IndoorMapDefinition.fromJson(json);
  }

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
      headers: {'Accept': 'application/json'},
    );

    if (resp.statusCode != 200) {
      throw ApiException('Errore attivazione emergenza', resp.statusCode);
    }
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

  Future<Posto> updatePostoFuoriServizio({
    required String parcheggioId,
    required String slotId,
    required bool fuoriServizio,
  }) async {
    final uri = Uri.parse(
      '$_baseUrl/posti/parcheggio/$parcheggioId/slot/$slotId/fuori-servizio',
    ).replace(
      queryParameters: {
        'fuoriServizio': fuoriServizio.toString(),
      },
    );

    final response = await _client.patch(
      uri,
      headers: {'Accept': 'application/json'},
    );

    if (response.statusCode == 200) {
      final json = jsonDecode(response.body) as Map<String, dynamic>;
      return Posto.fromJson(json);
    }

    final body = response.body.trim();
    throw ApiException(
      body.isNotEmpty
          ? body
          : 'Errore aggiornamento fuori servizio del posto',
      response.statusCode,
    );
  }

  Future<Posto> updatePostoStato({
    required String parcheggioId,
    required String slotId,
    required StatoPosto stato,
  }) async {
    final uri = Uri.parse(
      '$_baseUrl/posti/parcheggio/$parcheggioId/slot/$slotId/stato',
    ).replace(
      queryParameters: {
        'stato': stato.name.toUpperCase(),
      },
    );

    final response = await _client.patch(
      uri,
      headers: {'Accept': 'application/json'},
    );

    if (response.statusCode == 200) {
      final json = jsonDecode(response.body) as Map<String, dynamic>;
      return Posto.fromJson(json);
    }

    final body = response.body.trim();
    throw ApiException(
      body.isNotEmpty ? body : 'Errore aggiornamento stato del posto',
      response.statusCode,
    );
  }

  // =========================================================
  // LOG / ANALITICHE
  // =========================================================

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

  Future<List<Map<String, dynamic>>> getLogAnalitiche(
    String analiticaId,
  ) async {
    final url = Uri.parse('$_baseUrl/analitiche/$analiticaId/log');
    final response = await _client.get(url);

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

  Future<Map<String, dynamic>> creaLog({
    required String analiticaId,
    required String tipo,
    required String severita,
    required String titolo,
    required String descrizione,
    required DateTime data,
  }) async {
    final url = Uri.parse('$_baseUrl/log');
    final body = jsonEncode({
      'analiticaId': analiticaId,
      'tipo': tipo,
      'severita': severita,
      'titolo': titolo,
      'descrizione': descrizione,
      'data': data.toIso8601String(),
    });

    final response = await _client.post(
      url,
      headers: {'Content-Type': 'application/json'},
      body: body,
    );

    if (response.statusCode != 200) {
      throw Exception('Errore creazione log: ${response.statusCode}');
    }

    final jsonMap = jsonDecode(response.body);
    if (jsonMap is! Map<String, dynamic>) {
      throw Exception('Il backend non ha restituito un log valido');
    }

    return jsonMap;
  }

  Future<Map<String, dynamic>> getAnaliticaByParcheggioId(
    String parcheggioId,
  ) async {
    final url = Uri.parse('$_baseUrl/analitiche/parcheggio/$parcheggioId');
    final response = await _client.get(url);

    if (response.statusCode != 200) {
      throw Exception('Errore nel caricamento analitica del parcheggio');
    }

    final jsonMap = jsonDecode(response.body);
    if (jsonMap is! Map<String, dynamic>) {
      throw Exception('Il backend non ha restituito un\'analitica valida');
    }

    return jsonMap;
  }
}
