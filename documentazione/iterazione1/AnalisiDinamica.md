# Analisi Dinamica 

## 1. Scopo dell’Analisi Dinamica
L’analisi dinamica ha l’obiettivo di verificare, tramite esecuzione controllata del codice, il corretto comportamento delle componenti software principali del sistema, con particolare attenzione ai moduli:

- **Utente**  
- **Operatore**  
- **Connessione** (integrazione con MongoDB)
- **Interfacce funzionali**: `DatiUtenti`, `DatiOperatori`

L’analisi dinamica si affianca all’analisi statica già svolta e permette di individuare malfunzionamenti solo rilevabili in fase di esecuzione reale o semi-reale (interazione con il database, eccezioni, comportamenti inattesi).

---

## 2. Componenti Coinvolte
### 2.1 Modulo Utente
La classe `Utente` gestisce:
- autenticazione (`loginDB`)
- registrazione (`registrazioneDB`)
- eliminazione account (`deleteDB`)
- gestione preferenze (`selezioneDB`, `getPreferenze`)
- accesso credenziali (`controlloCredenziali`)


### 2.2 Modulo Operatore
La classe `Operatore` implementa `DatiOperatori` ed espone il metodo principale:
- `esisteOperatore(Operatore operatore)`

che verifica la presenza di un operatore nel database sulla base di:
- `nomeStruttura`
- `username`

### 2.3 Modulo Connessione
`Connessione` fornisce metodi statici per accedere alle collezioni MongoDB:
- `connessioneUtenti()`
- `connessioneOperatori()`

La correttezza di tali connessioni è fondamentale per l’esito positivo dell’analisi dinamica.

---

## 3. Struttura dei Test Dinamici
Sono stati realizzati test JUnit 5 sulle seguenti classi:

### ✓ `ConnessioneTest`
Verifica che:
- le collezioni restituite non siano `null`
- il database utilizzato sia `PMG`
- le collezioni `utenti` e `operatori` appartengano allo stesso database

### ✓ `UtenteTest`
Testati i seguenti comportamenti:
- registrazione DB di un nuovo utente
- rilevamento di utente duplicato (eccezione)
- login corretto e login scorretto (eccezione)
- eliminazione (`deleteDB`)
- salvataggio e recupero preferenze (`selezioneDB`, `getPreferenze`)
- generazione dello username `nome.cognome`
- robustezza del metodo `logout`

### ✓ `DatiUtentiTest`
Valutazione dell’aderenza dell’implementazione `Utente` al contratto dell’interfaccia:
- comportamento di registrazione
- corretto funzionamento di login/logica DB
- coerenza dei metodi richiesti da DatiUtenti

### ✓ `OperatoreTest`
Senza l’uso di librerie di mocking:
- inserimento controllato di documenti nel DB
- verifica `true` se l’operatore esiste realmente
- verifica `false` se l’operatore non esiste

### ✓ `DatiOperatoriTest`
Verifica che:
- `Operatore` implementi realmente `DatiOperatori`
- `esisteOperatore` si comporti correttamente dal punto di vista dell’interfaccia

---

## 4. Metodologia
L’analisi dinamica è stata effettuata tramite:

- **JUnit 5** per l’esecuzione dei test
- **Interazione reale con MongoDB**  
  Nessuna simulazione o mock del database:  
  → i comportamenti riflettono il funzionamento effettivo del sistema.
- **Inserimento e rimozione temporanea dei dati**  
  Ogni test genera dati unici (basati su `System.nanoTime()`) per evitare collisioni tra esecuzioni.

Questa metodologia garantisce risultati ripetibili e validi anche in presenza di test multipli.

---

## 5. Risultati dell’Analisi Dinamica
Tutti i test progettati hanno confermato che:

- il sistema interagisce correttamente con il database
- la logica applicativa rispetta il comportamento atteso
- eccezioni e condizioni di errore sono correttamente rilevate
- la separazione tra interfacce e implementazioni è coerente
- i metodi critici (`loginDB`, `registrazioneDB`, `esisteOperatore`) reagiscono correttamente sia a input validi che non validi

Non sono emersi malfunzionamenti critici.

---

## 6. Criticità Riscontrate
Durante l’analisi sono emersi i seguenti punti potenzialmente migliorabili:

### 🔸 Dipendenza diretta dal database reale
L’assenza di mock del DB:
- garantisce realismo dell’analisi,
- ma rende i test più sensibili a:
  - indisponibilità del server
  - latenza durante le query
  - cambiamenti schema DB

### 🔸 Assenza di gestione avanzata delle eccezioni
Alcuni metodi effettuano logging senza propagare eccezioni informative.

---

## 7. Possibili Miglioramenti Futuri
- Introduzione di **mock del database** per esecuzioni rapide e indipendenti da componenti esterne.
- Creazione di un livello Service dedicato per separare logica e accesso al DB.
- Estensione dell’interfaccia `DatiOperatori` con ulteriori metodi (registrazione, eliminazione, update).


---

## 8. Conclusioni
L’analisi dinamica ha evidenziato che i moduli Utente, Operatore e Connessione funzionano correttamente e rispettano i requisiti di progetto.

Il comportamento del sistema, osservato tramite esecuzioni reali, conferma la solidità dell’implementazione attuale, pur lasciando spazio a miglioramenti architetturali per una futura evoluzione del software.
