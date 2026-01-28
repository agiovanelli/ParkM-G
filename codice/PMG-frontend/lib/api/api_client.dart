import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:park_mg/models/log.dart';
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
}
