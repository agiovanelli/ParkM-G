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
  /// Per sviluppo locale (Flutter web + backend sullo stesso PC)
  static const String _baseUrl = 'http://localhost:8080/api';

  final http.Client _client;

  ApiClient({http.Client? client}) : _client = client ?? http.Client();

  // -------------------- UTENTI --------------------

  /// Login utente: POST /api/utenti/login
  Future<Utente> loginUtente(String email, String password) async {
    final uri = Uri.parse('$_baseUrl/utenti/login');
    final body = jsonEncode({
      'email': email,
      'password': password,
    });

    final resp = await _client.post(
      uri,
      headers: {'Content-Type': 'application/json'},
      body: body,
    );

    if (resp.statusCode == 200) {
      final json = jsonDecode(resp.body) as Map<String, dynamic>;
      return Utente.fromJson(json);
    } else if (resp.statusCode == 400 || resp.statusCode == 401) {
      // mappa ai tuoi IllegalArgumentException lato backend
      throw ApiException('Credenziali non valide', resp.statusCode);
    } else {
      throw ApiException(
        'Errore backend utenti: HTTP ${resp.statusCode}',
        resp.statusCode,
      );
    }
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

    if (resp.statusCode == 200) {
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
      headers: {'Content-Type': 'application/json'},
      body: body,
    );

    if (resp.statusCode == 200) {
      final json = jsonDecode(resp.body) as Map<String, dynamic>;
      return Operatore.fromJson(json);
    } else if (resp.statusCode == 400 || resp.statusCode == 401 || resp.statusCode == 404) {
      throw ApiException('Operatore non registrato o credenziali errate', resp.statusCode);
    } else {
      throw ApiException(
        'Errore backend operatori: HTTP ${resp.statusCode}',
        resp.statusCode,
      );
    }
  }

  Future<List<Log>> getLogByAnaliticaId(String analiticaId) async {
    final uri = Uri.parse('$_baseUrl/$analiticaId');

    final resp = await _client.get(uri);

    if (resp.statusCode == 200) {
      final List<dynamic> jsonList = jsonDecode(resp.body) as List<dynamic>;
      return jsonList.map((json) => Log.fromJson(json as Map<String, dynamic>)).toList();
    } else {
      throw ApiException(
        'Errore recupero log: HTTP ${resp.statusCode}',
        resp.statusCode,
      );
    }
  }

//-------------------- PRENOTAZIONI --------------------

  Future<PrenotazioneResponse> prenotaParcheggio(
  String utenteId,
  String parcheggioId,
  String orario,
) async {
  final uri = Uri.parse('$_baseUrl/parcheggi/prenota');
  final body = jsonEncode({
    'utenteId': utenteId,
    'parcheggioId': parcheggioId,
    'orario': orario,
  });

  final resp = await _client.post(
    uri,
    headers: {'Content-Type': 'application/json'},
    body: body,
  );

  if (resp.statusCode == 200) {
    final json = jsonDecode(resp.body) as Map<String, dynamic>;
    return PrenotazioneResponse.fromJson(json);
  } else if (resp.statusCode == 400) {
    // Ad esempio: parcheggio già occupato o dati mancanti
    throw ApiException('Dati prenotazione non validi o parcheggio non disponibile', resp.statusCode);
  } else if (resp.statusCode == 404) {
    throw ApiException('Utente o parcheggio non trovato', resp.statusCode);
  } else {
    throw ApiException(
      'Errore durante la prenotazione: HTTP ${resp.statusCode}',
      resp.statusCode,
    );
  }
}
}