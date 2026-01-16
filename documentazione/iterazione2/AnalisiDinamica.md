# Analisi Dinamica

## Obiettivo
L’analisi dinamica del backend è stata svolta tramite una suite di test automatici JUnit 5 con l’obiettivo di:
- verificare a runtime i principali comportamenti applicativi (casi “happy path” e casi di errore);
- validare la corretta propagazione/gestione delle eccezioni nei layer di servizio;
- controllare la correttezza degli endpoint REST (status code e gestione payload JSON) isolando il layer web dal resto dell’applicazione.

---

## Strumenti e tecnologie utilizzate
- **JUnit 5 (Jupiter)** per la definizione ed esecuzione dei test.
- **Mockito** per il mocking delle dipendenze nei test di servizio (repository) e nei test web (service).
- **Spring Boot Test – @WebMvcTest + MockMvc** per testare il layer controller senza avviare l’intero contesto applicativo.
- **Jackson ObjectMapper** per serializzare/deserializzare JSON nelle richieste HTTP simulate.
- **ReflectionTestUtils** per impostare campi privati (es. id) sulle entity durante i test, simulando la presenza di ID persistiti.
---

## Struttura della suite di test
La suite è organizzata per layer applicativo:

### 1) Test di Service (unit test con MockitoExtension)
**File:**
- `AnaliticheServiceImplTest.java`
- `LogServiceImplTest.java`
- `ParcheggioServiceImplTest.java`
- `PrenotazioneServiceImplTest.java`

**Caratteristiche:**
- Uso di `@ExtendWith(MockitoExtension.class)` per abilitare Mockito in JUnit 5.
- Dipendenze del service mockate con `@Mock` (es. repository).
- Classe under test iniettata con `@InjectMocks`.
- Verifiche tramite `assertEquals`, `assertThrows`, e stubbing con `when(...).thenReturn(...)`.
- Verifica interazioni con i repository tramite `verify(...)` e `verifyNoMoreInteractions(...)`.

**Casi coperti (principali):**
- **AnaliticheServiceImplTest**
  - `getByIdTest`: recupero analitiche per ID con repository che restituisce `Optional.of(...)`.
  - `getByIdEccezioneTest`: repository `Optional.empty()` → eccezione con messaggio **"Analitiche non trovata"**.
  - `getByOperatoreIdTest`: recupero per `operatoreId` con esito positivo.
  - `getByOperatoreIdEccezioneTest`: `operatoreId` non presente → eccezione con messaggio **"Analitiche non trovata"**.
  - `saveTest`: verifica mapping corretto da `AnaliticheRequest` a entity e corretta chiamata a `repository.save(...)` (con `ArgumentCaptor`).

- **LogServiceImplTest**
  - `getLogByIdTest` / `getLogByIdVuotoTest`: recupero `Optional<Log>` presente o vuoto.
  - `salvaLog1Test`: salvataggio di un `Log` con impostazione della data (non nulla) e persistenza tramite repository.
  - `salvaLogTest`: mapping da `LogRequest` a entity (tipo, severita, titolo, descrizione, data) verificato con `ArgumentCaptor`.
  - `getLogByAnaliticaIdTest`: recupero lista log ordinata per data tramite `findByAnaliticaIdOrderByDataDesc(...)`.
  - `getLogByAnaliticaIdAndTipoTest`: recupero lista log filtrata per `analiticaId` e `tipo`.

- **ParcheggioServiceImplTest**
  - `cercaPerAreaTest`: ricerca parcheggi per area e mapping verso `ParcheggioResponse`.
  - `effettuaPrenotazioneTest`: prenotazione su parcheggio esistente con posti disponibili; verifica chiamate a `ParcheggioRepository.save(...)` e `PrenotazioneRepository.save(...)`.
  - `effettuaPrenotazionePostiEsauritiTest`: posti disponibili = 0 → eccezione `IllegalStateException` e nessun salvataggio effettuato.
  - `effettuaPrenotazioneParcheggioNonTrovatoTest`: parcheggio non presente → eccezione `IllegalArgumentException` e nessun salvataggio effettuato.
  - `cercaViciniTest`: filtro dei parcheggi “vicini” a coordinate date (atteso un solo risultato coerente).

- **PrenotazioneServiceImplTest**
  - `getStoricoUtenteTest`: recupero storico prenotazioni di un utente e mapping verso `PrenotazioneResponse` (ID, utenteId, parcheggioId, orario, codiceQr), con verifica chiamate al repository.
---

### 2) Test di Controller (test del layer web con MockMvc)
**File:**
- `AnaliticheControllerTest.java`
- `LogControllerTest.java`
- `ParcheggioControllerTest.java`
- `PrenotazioneControllerTest.java`

**Caratteristiche:**
- Uso di `@WebMvcTest(...)` per caricare solo componenti web (controller, filtri/handler web).
- Dipendenze del controller mockate con `@MockBean` (service), così da isolare il controller dalla logica business.
- Simulazione richieste HTTP con `MockMvc` e verifica di status code e (dove applicabile) risposta JSON.

**Endpoint e casi coperti (principali):**
- **AnaliticheControllerTest**
  - `GET /api/analitiche/{id}` → `getByIdTest` (verifica campi: id, parcheggioId, nomeParcheggio, operatoreId).
  - `GET /api/analitiche/operatore/{operatoreId}` → `getByOperatoreIdTest`.
  - `POST /api/analitiche` → `creaAnaliticheTest` (verifica risposta e presenza campo log come array vuoto).
  - `GET /api/analitiche/{id}/log` → `getLogByAnaliticaIdTest` (lista log, verifica campi e timestamp).
  - `GET /api/analitiche/{id}/log/{tipo}` → `getLogByAnaliticaIdAndTipoTest` (verifica filtro e verify su logService).

- **LogControllerTest**
  - `POST /api/log` → `creaLogTest` (verifica campi del log creato).
  - `GET /api/log/analitiche/{analiticaId}/log` → `getLogByAnaliticaIdTest`.
  - `GET /api/log/analitiche/{analiticaId}/tipo/{tipo}` → `getLogByAnaliticaIdAndTipoTest`.
  - `PUT /api/log/{id}/severity?severity=...` → `aggiornaSeverityTest` (verifica aggiornamento severità).
  - `PUT /api/log/{id}/category?category=...` → `aggiornaCategoryTest` (verifica aggiornamento categoria/tipo).

- **ParcheggioControllerTest**
  - `GET /api/parcheggi/cerca?area=...` → `cercaTest` (lista parcheggi filtrata per area).
  - `POST /api/parcheggi/prenota` → `prenotaTest` (prenotazione, verifica codiceQr e orario).
  - `GET /api/parcheggi/nearby?lat=...&lng=...&radius=...` → `getNearbyTest` (lista parcheggi vicini).

- **PrenotazioneControllerTest**
  - `GET /api/prenotazioni/utente/{utenteId}` → `getStoricoTest` (lista storico prenotazioni per utente).

---

## Modalità di esecuzione
La suite è eseguibile tramite il normale lifecycle di build del progetto, ad esempio:

- Maven:
  - `mvn test`
- Gradle:
  - `./gradlew test`

I test controller basati su `MockMvc` non richiedono l’avvio completo dell’applicazione: il layer web viene avviato in modo “slice” tramite `@WebMvcTest`, rendendo l’esecuzione più rapida e focalizzata.

---

## Risultato dell’analisi dinamica
L’analisi dinamica copre i flussi essenziali di:
- gestione **Analitiche** (recupero per ID/operatore, creazione e lettura log associati);
- gestione **Log** (creazione, query per analitica/tipo e aggiornamento categoria/severità);
- gestione **Parcheggi** (ricerca per area, ricerca nearby, prenotazione con gestione errori per posti esauriti/parcheggio mancante);
- gestione **Prenotazioni** (storico utente lato service e controller);
- verifica delle risposte HTTP (status code, JSON body) nel layer REST e corrette interazioni con i service mockati.

Questa suite fornisce una base di regressione automatica: modifiche future a controller/service possono essere validate rapidamente rieseguendo i test.

---

## Limiti e possibili estensioni
- Inserire test “negative” aggiuntivi sui controller (payload non valido, campi mancanti, status 4xx).
- Aggiungere verifiche più profonde sul body JSON (oltre agli status code), usando `jsonPath(...)`.
- Integrare una misura di copertura (es. **JaCoCo**) per quantificare la coverage (line/branch) e guidare il completamento della suite.
- Integrare test di integrazione end-to-end (`@SpringBootTest`) con database in-memory o Testcontainers per coprire l’intera pipeline repository→service→controller.

