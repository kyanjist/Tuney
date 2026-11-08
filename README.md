# Tuney 🎵

Tuney è un'applicazione desktop social che permette agli amanti della musica di pubblicare recensioni, interagire con gli altri utenti e scoprire nuovi brani e album.

Il progetto è stato sviluppato per l'esame di **User Interfaces Design** all'Università della Calabria. L'applicazione è già pre-popolata con profili, recensioni, commenti e interazioni per simulare un social network attivo.

## Funzionalità principali

- **Feed & Social**: leggi, commenta e metti "Mi piace" alle recensioni della community.
- **Attività & Notifiche**: visualizza la tua attività e quella degli utenti che segui, monitora follower/seguiti e le interazioni ricevute.
- **Profili personalizzati**: personalizza avatar, biografia e canzoni/album preferiti.
- **Salvati per dopo**: una comoda "watchlist" per salvare album e brani da ascoltare o recensire in seguito.
- **Statistiche**: calcolo automatico delle statistiche basate sui voti assegnati (distribuzione dei voti per profilo e per elemento).
- **Integrazione Spotify & Cache Locale**: sfrutta le API di Spotify per ricavare metadati e copertine. Una volta salvati nel database locale, tutti i dati restano accessibili anche offline.

## Requisiti

- **Java 19** o superiore (consigliato **Java 21 LTS**)
- **Maven** 3.8+

## Esecuzione rapida

L'applicazione è **pronta all'uso**: il database locale `tuney.db` è già incluso con account di prova, recensioni e brani.

Per avviare l'applicazione:

```bash
mvn clean javafx:run
```

All'avvio puoi accedere con gli account di test esistenti oppure **creare un nuovo account** tramite la schermata di registrazione.

## Configurazione Spotify (Opzionale)

L'applicazione funziona autonomamente offline per tutte le funzioni social e per i brani/album già memorizzati nel database.

Le credenziali Spotify for Developers sono richieste **esclusivamente** se desideri cercare nuovi brani/album dal catalogo di Spotify o caricarne le relative tracklist:

1. Vai su [Spotify for Developers Dashboard](https://developer.spotify.com/dashboard) ed esegui il login con il tuo account Spotify
2. Clicca su **Create App** (inserisci un nome e una descrizione a piacere, e come Redirect URI puoi indicare `http://localhost`)
3. Dalle impostazioni dell'app creata (**Settings**), copia il **Client ID** e clicca su **View client secret** per visualizzare il **Client Secret**
4. Nella cartella principale del progetto, crea il file `config.properties` copiando il template di esempio:
   ```bash
   cp config.properties.example config.properties
   ```
5. Inserisci i tuoi valori in `config.properties`:
   ```properties
   spotify.client.id=IL_TUO_CLIENT_ID
   spotify.client.secret=IL_TUO_CLIENT_SECRET
   ```
   *(In alternativa, puoi impostare le variabili d'ambiente `SPOTIFY_CLIENT_ID` e `SPOTIFY_CLIENT_SECRET`).*

> **Nota:** se le credenziali non vengono inserite, l'applicazione continuerà a funzionare regolarmente sfruttando i dati locali memorizzati nel database.

## Tecnologie utilizzate

- **Java 19** & **JavaFX** — Interfaccia grafica (FXML e CSS personalizzato)
- **SQLite** & **JDBC** — Database relazionale locale
- **Spotify Web API** & **Unirest** — Integrazione catalogo musicale
- **Spring Security Crypto (BCrypt)** — Hashing sicuro delle password
- **org.json** — Parsing risposte JSON

## Licenza

Questo progetto è distribuito sotto licenza [GNU General Public License v3.0 (GPL-3.0)](LICENSE).
