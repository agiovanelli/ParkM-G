# Analisi Dinamica

## Obiettivo
L’analisi dinamica del backend è stata svolta tramite una suite di test automatici **JUnit 5** con l’obiettivo di:
- verificare a runtime i comportamenti principali (casi “happy path” e casi di errore);
- validare la corretta gestione delle eccezioni e dei messaggi associati;
- controllare la correttezza degli endpoint REST (status code e gestione payload JSON) isolando il layer web dal resto dell’applicazione.

---

## Strumenti e tecnologie utilizzate
- **JUnit 5 (Jupiter)** per la definizione ed esecuzione dei test.
- **Mockito** per il mocking delle dipendenze nei test di servizio (repository) e nei test web (service).
- **Spring Boot Test – @WebMvcTest + MockMvc** per testare il layer controller senza avviare l’intero contesto applicativo.
- **Jackson ObjectMapper** per serializzare/deserializzare JSON nelle richieste HTTP simulate.

---

## Struttura della suite di test
La suite è organizzata per layer applicativo:

### 1) Test di Service (unit test con MockitoExtension)
**File:**
- `UtenteServiceImplTest.java`
- `OperatoreServiceImplTest.java`

**Caratteristiche:**
- Uso di `@ExtendWith(MockitoExtension.class)` per abilitare Mockito in JUnit 5.
- Dipendenze del service mockate con `@Mock` (es. repository).
- Classe under test iniettata con `@InjectMocks`.
- Verifiche tramite `assertEquals`, `assertThrows`, e stubbing con `when(...).thenReturn(...)`.

**Casi coperti (principali):**
- **UtenteServiceImplTest**
  - `testRegistrazioneSuccess`: registrazione con email non presente → salvataggio utente e ritorno di `UtenteResponse`.
  - `testRegistrazioneEmailGiaEsistente`: registrazione con email già presente → attesa gestione errore (blocco registrazione).
  - `testLoginSuccess`: login valido → attesa risposta coerente.
  - `testGetPreferenzeUtenteNonTrovato`: richiesta preferenze su ID inesistente → attesa eccezione (es. `IllegalArgumentException`).

- **OperatoreServiceImplTest**
  - `testLoginSuccess`: login operatore valido → ritorno risposta prevista.
  - `testLoginFailure_NotFound`: operatore non presente (repository ritorna `Optional.empty()`) → attesa eccezione con messaggio **"Operatore non registrato"**.

---

### 2) Test di Controller (test del layer web con MockMvc)
**File:**
- `UtenteControllerTest.java`
- `OperatoreControllerTest.java`

**Caratteristiche:**
- Uso di `@WebMvcTest(...)` per caricare solo componenti web (controller, filtri/handler web).
- Dipendenze del controller mockate con `@MockBean` (service), così da isolare il controller dalla logica business.
- Simulazione richieste HTTP con `MockMvc` e verifica di status code e (dove applicabile) risposta JSON.

**Endpoint e casi coperti (principali):**
- **UtenteControllerTest**
  - `POST /api/utenti/registrazione` → `testRegistrazioneSuccess` (status **200 OK**).
  - `POST /api/utenti/login` → `testLoginSuccess` (status **200 OK**).
  - `GET /api/utenti/{id}/preferenze` → `testGetPreferenze` (verifica esito positivo e contenuto).
  - `PUT /api/utenti/{id}/preferenze` → `testAggiornaPreferenze` (status **204 No Content**).
  - `DELETE /api/utenti/{id}` → `testDeleteUtente` (status **204 No Content**).

- **OperatoreControllerTest**
  - `POST /api/operatori/login` → `testLoginSuccess` (status **200 OK**).

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
- autenticazione/registrazione (utente e operatore),
- gestione preferenze utente,
- verifica delle risposte HTTP (status code) nel layer REST,
- gestione degli errori tramite eccezioni nei service.

Questa suite fornisce una base di regressione automatica: modifiche future a controller/service possono essere validate rapidamente rieseguendo i test.

---

## Limiti e possibili estensioni
- Inserire test “negative” aggiuntivi sui controller (payload non valido, campi mancanti, status 4xx).
- Aggiungere verifiche più profonde sul body JSON (oltre agli status code), usando `jsonPath(...)`.
- Integrare una misura di copertura (es. **JaCoCo**) per quantificare la coverage (line/branch) e guidare il completamento della suite.
- Integrare test di integrazione end-to-end (`@SpringBootTest`) con database in-memory o Testcontainers per coprire l’intera pipeline repository→service→controller.

