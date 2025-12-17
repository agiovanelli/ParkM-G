# 📄 Report di Analisi Statica del Codice (Iterazione 1)

## Introduzione

Per garantire l'alta qualità, la manutenibilità e la sicurezza del codice sviluppato durante l'Iterazione 1 del progetto, è stata eseguita un'analisi statica integrata.

Il tool scelto è **SonarLint** (plugin di SonarQube), utilizzato in Eclipse. SonarLint ha identificato e guidato la risoluzione di *Code Smells*, potenziali *Bug* e violazioni delle *best practice* di programmazione.

Di seguito sono riepilogate le modifiche eseguite, suddivise per package e file.

***

## 1. Package `grafica`

### File `HomeController.java`

Le correzioni in questo controller si sono focalizzate sulla pulizia della logica e sulla gestione degli output.

* **Pulizia della Logica e Refactoring:**
    * È stato rimosso il campo privato `o` ( di tipo `Operatore`) e, se necessario, è stato dichiarato come variabile locale all'interno dei metodi per migliorare l'incapsulamento.
    * La funzione di validazione della password è stata modificata: i molti `if/return false` sono stati eliminati concatenando le condizioni in un'unica espressione booleana nel `return`, migliorando la leggibilità.
* **Gestione degli Output:**
    * Tutte le chiamate a **`System.out.println`** sono state sostituite con l'uso del **Logger SLF4J** (`LOGGER.info`, ecc.).
* **Nomenclatura:**
    * È stato eseguito il **Refactoring** del package da `Grafica` a **`grafica`** per aderire alle convenzioni di nomenclatura Java standard.

***

## 2. Package `operatore`

### File `Operatore.java`

* **Nomenclatura Package:**
    * È stato eseguito il **Refactoring** del package da `Operatore` a **`operatore`**.
* **Gestione degli Output:**
    * Tutte le chiamate a **`System.out.println`** sono state sostituite con l'uso del **Logger SLF4J** (`LOGGER.info`, ecc.).

***

## 3. Package `utente`

### File `GestioneUtenti.java` (Interfaccia)

* **Gestione Errori (Eccezioni):**
    * La clausola generica **`throws Exception`** è stata sostituita con eccezioni specifiche nei metodi `registrazione()` e `login()`.
    * Sono state introdotte `IOException`, `IllegalArgumentException` e `IllegalStateException` per rendere esplicito il tipo di errore (es. problemi di connessione, dati non validi, o utente già registrato).

### File `Utente.java` (Implementazione)

* **Duplicazione Dati (Costanti):**
    * Le stringhe letterali (es. `"password"`, `"email"`, ecc.) utilizzate per le chiavi MongoDB sono state sostituite con **costanti** `private static final String` (es. `FIELD_PASSWORD`).
* **Gestione Output e Logica:**
    * Tutte le stampe **`System.out.println`** sono state sostituite con il **Logger SLF4J**.
    * Nel metodo `selezioneDB()`, la variabile `UpdateResult update` ora viene utilizzata per controllare l'esito dell'operazione, restituendo `update.getModifiedCount() == 1;`.
* **Accesso Statico e Interfaccia:**
    * È stato corretto l'accesso al metodo `connessioneUtenti()` eliminando l'istanza `new Connessione()`.
    * È stata assicurata la compatibilità con le interfacce `DatiUtenti` e `GestioneUtenti` aggiornando le clausole `throws` e risolvendo errori di sintassi.
* **Assegnazione:**
    * È stata eliminata la combinazione dell'operazione di assegnazione (`=`) con altre espressioni logiche.

***

## 4. Package `database`

### File `Connessione.java`

* **Chiusura Risorse Critiche:**
    * È stato implementato il blocco **`try-with-resources`** nei metodi `connessioneUtenti()` e `connessioneOperatori()`. Questo garantisce la chiusura automatica del **`MongoClient`** e previene *memory leak*.
* **Design di Classe Utility:**
    * Il costruttore `public Connessione()` è stato reso **privato** per impedire l'istanza della classe.
* **Semplificazione Logica:**
    * Le variabili temporanee sono state rimosse e il risultato del database è stato restituito immediatamente.
* **Accesso Statico:**
    * L'accesso ai metodi è stato corretto chiamandoli direttamente sulla classe.
* **Gestione URI:**
    * Il metodo `uri.isBlank()` è stato sostituito con l'alternativa compatibile **`uri.trim().isEmpty()`** per risolvere un errore di sintassi.