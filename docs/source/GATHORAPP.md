---
title: ""
author: ""
date: ""
documentclass: report
header-includes: |
  \usepackage{graphicx}
  \usepackage{fancyhdr}
  \renewcommand{\familydefault}{\ttdefault}
---

\begin{titlepage}
\centering
\vspace{2cm}

\includegraphics[width=0.5\textwidth]{source/polimi-logo.png}

\vspace{2cm}

{\Huge\bfseries GathorApp\par}
\vspace{1cm}
{\Large Documentazione Tecnica del Progetto\par}

\vspace{3cm}

{\large
Alessandro Alfano\\[0.5cm]
Matricola: 962485\\[0.3cm]
Codice Persona: 10788970\\[0.5cm]
}

\vfill

{\large Novembre 2025\par}

\vspace{2cm}
\end{titlepage}

\tableofcontents
\newpage

## 1. DESCRIZIONE DELL'AMBITO

### 1.1 Obiettivi del Progetto

GathorApp è un'applicazione web per la gestione di uscite e eventi con caratteristiche avanzate:

- **Organizzazione di uscite:** Gli utenti possono creare uscite (outings) indipendenti oppure associate a eventi
- **Gestione di eventi:** Gli utenti business possono creare eventi
- **Partecipazione:** Gestisce il sistema di approvazione delle partecipazioni, controlla e gestisce la concorrenza
- **Chat in tempo reale:** Implementa la comunicazione tra i partecipanti ad un'uscita tramite WebSocket. La chat si auto-disattiva dopo 7 giorni
- **Notifiche:** Salva le notifiche su database e le invia agli utenti (Pattern Observer)
- **Sistema di voucher:** Genera automaticamente i voucher per utenti premium che organizzano uscite che soddisfano i requisiti
- **Recensioni e valutazioni:** Permette agli utenti di valutare uscite ed eventi

### 1.2 Contesto di Utilizzo

L'applicazione è rivolta a:

- **Utenti standard:** Possono creare fino a 5 uscite al mese con massimo 10 partecipanti
- **Utenti premium:** Possono creare un numero illimitate di uscite associate a eventi e guadagnare voucher
- **Utenti business:** Possono creare eventi e premi
- **Amministratori:** Gestiscono utenti e configurazioni

### 1.3 Requisiti Funzionali Divisi per Ruolo

#### Utente Standard (USER)

- **RF1:** Può visualizzare le uscite e gli eventi nelle vicinanze (ricerca per raggio)
- **RF2:** Può creare uscite indipendenti (max 5 al mese, max 10 partecipanti)
- **RF3:** Può iscriversi a un'uscita e ricevere la notifica di approvazione/rifiuto
- **RF4:** Può inviare messaggi in chat durante l'uscita
- **RF5:** Può lasciare una recensione su un'uscita o un evento (1-5 stelle)
- **RF6:** Può visualizzare notifiche

#### Utente Premium (PREMIUM)

- **RF7:** Può creare uscite illimitate associate a eventi
- **RF8:** Può visualizzare i voucher che ha guadagnato dall'organizzazione di uscite
- **RF9:** Può utilizzare il voucher

#### Utente Business (BUSINESS)

- **RF10:** Può creare un evento e associare dei premi
- **RF11:** Può riscattare i voucher
- **RF12:** Può visualizzare i premi disponibili

#### Amministratore (ADMIN)

- **RF13:** Può gestire i ruoli degli utenti (promozione/retrocessione)
- **RF14:** Può bannare/sbannare gli utenti
- **RF15:** Può visualizzare le statistiche della piattaforma

### 1.4 Requisiti Non Funzionali

- **RNF1:** Interfaccia grafica responsiva (Angular)
- **RNF2:** Multithreading con lock per il controllo della concorrenza nel sistema di partecipazioni
- **RNF3:** Copertura di test >= 80% (JUnit 5 + JaCoCo)
- **RNF4:** Autenticazione JWT con refresh token
- **RNF5:** Documentazione UML completa con diagrammi
- **RNF6:** API documentata con OpenAPI/Swagger

---

## 2. DESCRIZIONE DELLA PROGETTAZIONE

### 2.1 Architettura Generale

L'applicazione segue il **pattern MVC (Model-View-Controller)**:

- **Model:** Entità JPA
  - _backend/src/main/java/com/alfano/gathorapp/\*/\*.java_
- **View:** Componenti Angular standalone
  - _frontend/src/app/\*_
- **Controller:** Spring REST controllers
  - _backend/src/main/java/com/alfano/gathorapp/\*/\*Controller.java_
- **Service:** Logica di business
  - _backend/src/main/java/com/alfano/gathorapp/\*/\*Service.java_
- **Repository:** Accesso ai dati JPA
  - _backend/src/main/java/com/alfano/gathorapp/\*/\*Repository.java_

Ogni package funzionale contiene tutti i componenti correlati (Controller, Service, Repository, Entity, DTO, Mapper) nello stesso package, senza sottocartelle. Ad esempio, il package `outing` contiene:

- `Outing.java` (Entity)
- `OutingController.java` (REST Controller)
- `OutingService.java` (Business Logic)
- `OutingRepository.java` (Data Access)
- `OutingMapper.java` (DTO Mapping)
- `dto/` (DTO classes)

### 2.2 Stack Tecnologico

**Backend:**

- Java 17
- Spring Boot 3.5.6
- Spring Security (JWT)
- Spring Data JPA
- Spring WebSocket (STOMP)
- H2 Database (in-memory/file-based)
- Gradle 8.5

**Frontend:**

- Angular 20
- TypeScript 5.7
- Componenti standalone
- OpenAPI Generator (client auto-generato)
- Leaflet (mappe)
- STOMP.js (WebSocket)

**Testing & Coverage:**

- JUnit 5
- Mockito
- JaCoCo

### 2.3 Package Backend

```
com.alfano.gathorapp
├── admin/           → Gestione amministratori
├── auth/            → Autenticazione JWT
├── chat/            → Sistema di chat
├── config/          → Configurazioni Spring
├── event/           → Gestione eventi
├── map/             → Servizi di geolocalizzazione
├── notification/    → Notifiche
├── outing/          → Gestione uscite
├── participation/   → Approvazione partecipazioni
├── pattern/         → Pattern di design
│   ├── observer/    → Observer Pattern (notifiche)
│   └── strategy/    → Strategy Pattern (limiti utente)
├── report/          → Segnalazioni
├── review/          → Recensioni
├── reward/          → Premi
├── security/        → Configurazioni sicurezza
├── user/            → Gestione utenti
├── voucher/         → Sistema voucher
└── websocket/       → Configurazione WebSocket
```

---

## 3. DESIGN PATTERN IMPLEMENTATI

### 3.1 Strategy Pattern (Limiti Utente)

**Ubicazione:** _backend/src/main/java/com/alfano/gathorapp/pattern/strategy/_

**Problema risolto:**

GathorApp ha 4 tipi di utenti (USER, PREMIUM, BUSINESS, ADMIN) ciascuno ha regole di business differenti per la creazione di uscite. Implementare queste regole con _if-else_ basati su _user.getRole()_ violerebbe l'Open/Closed Principle e renderebbe il codice fragile e difficile da testare.

**Soluzione tramite Strategy Pattern:**

```
UserLimitationStrategy (interfaccia)
├── BaseUserStrategy (max 5 uscite/mese, 10 partecipanti)
├── PremiumUserStrategy (illimitate collegate ad evento, partecipanti illimitati)
└── BusinessUserStrategy (illimitati)

UserStrategyFactory → seleziona strategia per ruolo
```

**Implementazione dettagliata:**

1. **Interfaccia _UserLimitationStrategy_**: definisce il contratto con 3 metodi

   - _canCreateOuting(User user, int monthlyCount)_: verifica se l'utente può creare l'uscita
   - _getMaxParticipants(User user)_: ritorna il limite massimo di partecipanti (10 per USER, illimitato per PREMIUM/BUSINESS)
   - _getMonthlyOutingLimit(User user)_: ritorna il limite massimo mensile di uscite create (5 per USER, illimitato per altri)

2. **Strategie concrete**:

   - _BaseUserStrategy_: implementa le regole per il ruolo USER (5/mese, max 10 partecipanti, solo uscite indipendenti)
   - _PremiumUserStrategy_: uscite illimitate se associate a eventi, partecipanti illimitati
   - _BusinessUserStrategy_: nessun limite (usato anche per ADMIN)

3. **Factory _UserStrategyFactory_**: usa un _Map<Role, UserLimitationStrategy>_ per selezionare la strategia

   ```java
   public UserLimitationStrategy getStrategy(Role role) {
       return strategies.getOrDefault(role, baseUserStrategy);
   }
   ```

4. **Integrazione in _OutingService_**:
   ```java
   UserLimitationStrategy strategy = strategyFactory.getStrategy(user.getRole());
   if (!strategy.canCreateOuting(user, monthlyCount)) {
       throw new LimitExceededException("Monthly limit reached");
   }
   ```

**Benefici architetturali:**

- **Open/Closed Principle**: aggiungere un nuovo ruolo (es. MODERATOR) richiede solo di creare una nuova strategia, senza modificare _OutingService_
- **Single Responsibility**: ogni strategia gestisce solo le regole del proprio ruolo
- **Testabilità**: ogni strategia è testabile indipendentemente con unit test dedicati (vedi _UserLimitationStrategyTest.java_)
- **Polimorfismo**: il client (_OutingService_) lavora con l'interfaccia, non con le implementazioni concrete
- **Eliminazione condizionali**: nessun _if (role == USER)_ sparso nel codice

**Visibilità nel Class Diagram:**

Nel class diagram, il pattern Strategy è rappresentato come sottografo separato con:

- Relazioni di implementazione (_implements_) tra strategie concrete e interfaccia
- Relazione di dipendenza (_creates_) tra factory e interfaccia
- Integrazione con _User_ tramite _Role_ enum

**File chiave:**

- _UserLimitationStrategy.java_ (interfaccia)
- _BaseUserStrategy.java_, _PremiumUserStrategy.java_, _BusinessUserStrategy.java_
- _UserStrategyFactory.java_
- Test: _UserLimitationStrategyTest.java_

### 3.2 Observer Pattern (Notifiche Multi-Canale)

**Ubicazione:** _backend/src/main/java/com/alfano/gathorapp/pattern/observer/_

**Problema risolto:**

Il sistema deve inviare notifiche attraverso **2 canali** quando si verificano eventi importanti (partecipazione approvata, voucher generato, nuovo messaggio):

1. **Database**: persistenza per storico e consultazione offline
2. **WebSocket**: notifica real-time agli utenti connessi

Implementare questa logica direttamente nei service (es. _ParticipationService.approve()_) creerebbe:

- **Tight coupling**: i service dipenderebbero da _NotificationRepository_ e _WebSocketMessagingTemplate_
- **Difficoltà di estensione**: aggiungere un 3° canale (email, SMS) richiederebbe di modificare tutti i service
- **Violazione SRP**: i service gestirebbero sia la business logic sia la notifica

**Soluzione tramite Observer Pattern:**

```
NotificationSubject (interface)
└── NotificationManager (Subject concreto thread-safe)

NotificationObserver (interface)
├── PersistenceNotificationObserver (salva in DB)
└── WebSocketNotificationObserver (invia via WebSocket STOMP)
```

**Implementazione dettagliata:**

1. **Interfaccia _NotificationSubject_**: definisce il contratto del Subject

   - _attach(NotificationObserver observer)_: registra un observer
   - _detach(NotificationObserver observer)_: rimuove un observer
   - _notifyObservers(NotificationEvent event)_: propaga evento a tutti gli observer

2. **Classe _NotificationManager_** (Subject concreto):

   - Mantiene lista observer in _CopyOnWriteArrayList<NotificationObserver>_ (thread-safe)
   - Metodo _notifyObservers()_ usa _parallelStream()_ per eseguire observer in parallelo:

   ```java
   observers.parallelStream().forEach(observer -> {
       try {
           observer.update(event);
       } catch (Exception e) {
           log.error("Error notifying observer", e);
       }
   });
   ```

   - **Gestione errori**: se un observer fallisce, gli altri continuano l'esecuzione

3. **Interfaccia _NotificationObserver_**:

   - Singolo metodo: _update(NotificationEvent event)_

4. **Observer concreti**:

   - _PersistenceNotificationObserver_: crea un'entità _Notification_ e salva nel DB tramite _NotificationRepository_
   - _WebSocketNotificationObserver_: invia un messaggio STOMP a _/topic/notifications/{userId}_ tramite _SimpMessagingTemplate_

5. **Inizializzazione** (_NotificationObserverInitializer_):

   - Bean **@PostConstruct** che registra i 2 observer al manager all'avvio dell'applicazione

6. **Integrazione nei Service**:
   ```java
   // In ParticipationService.approve()
   NotificationEvent event = new NotificationEvent(
       userId,
       NotificationType.PARTICIPATION_APPROVED,
       "La tua richiesta è stata approvata"
   );
   notificationManager.notifyObservers(event);
   ```

**Multithreading e prestazioni:**

- **Thread-safety**: _CopyOnWriteArrayList_ permette letture concorrenti senza lock. Scritture (attach/detach) copiano l'array intero (rare, solo all'avvio)
- **Parallelizzazione**: _parallelStream()_ esegue observer su thread del ForkJoinPool comune
  - Osservatore DB: ~10ms (INSERT query)
  - Osservatore WebSocket: ~50ms (network latency)
  - **Tempo totale sequenziale**: 60ms
  - **Tempo totale parallelo**: ~50ms (il più lento dei due)
  - **Speedup**: 1.2x con 2 observer, scala linearmente con più observer
- **No blocking**: se il DB è lento, il WebSocket non viene bloccato (e viceversa)
- **Fault tolerance**: eccezione in un observer non propaga agli altri (try-catch in _forEach_)

**Benefici architetturali:**

- **Decoupling**: _ParticipationService_ non conosce _NotificationRepository_ né _SimpMessagingTemplate_, dipende solo da _NotificationManager_
- **Estensibilità (Open/Closed)**: aggiungere email/SMS richiede solo:
  1. Creare _EmailNotificationObserver implements NotificationObserver_
  2. Registrare nel _NotificationObserverInitializer_
  3. **Zero modifiche** ai service esistenti
- **Single Responsibility**: ogni observer gestisce 1 solo canale
- **Testabilità**: gli observer sono testabili individualmente, i service sono testabili con mock del manager
- **Concorrenza**: le notifiche inviate in parallelo migliorano performance

**Visibilità nel Class Diagram:**

Nel class diagram, l'Observer Pattern è visualizzato con:

- Relazioni di implementazione tra observer concreti e interfaccia _NotificationObserver_
- Relazione di composizione (_o--_) tra _NotificationManager_ e lista observer
- Annotazione _<<thread-safe>>_ su _NotificationManager_
- Annotazioni _<<concurrent>>_ sui 2 observer concreti
- Nota dedicata che spiega il meccanismo di _parallelStream()_

**Integrazione con altri componenti:**

Il pattern Observer è usato da:

- _ParticipationService_: notifica approvazione/rifiuto partecipazioni
- _VoucherService_: notifica generazione/riscatto voucher
- _ChatService_: notifica nuovi messaggi
- _OutingService_: notifica creazione uscite
- _EventService_: notifica creazione eventi

**File chiave:**

- _NotificationSubject.java_ (interfaccia Subject)
- _NotificationManager.java_ (Subject concreto thread-safe)
- _NotificationObserver.java_ (interfaccia Observer)
- _PersistenceNotificationObserver.java_ (observer DB)
- _WebSocketNotificationObserver.java_ (observer WebSocket)
- _NotificationObserverInitializer.java_ (registrazione observer)
- _NotificationEvent.java_ (evento propagato)
- Test: _NotificationObserverTest.java_ (verifica parallelizzazione e fault tolerance)

---

## 4. MULTITHREADING E SINCRONIZZAZIONE

### 4.1 Meccanismi di Concorrenza

#### A. Pessimistic Locking (ParticipationService)

**Ubicazione:** _participation/ParticipationService.java_

```java
@Transactional(isolation = Isolation.SERIALIZABLE)
public synchronized ParticipationResponse joinOuting(UUID outingId, UUID userId) {
    // ...
    long approvedCount = participationRepository.countApprovedByOuting(outing);
    if (approvedCount >= outing.getMaxParticipants()) {
        throw new RuntimeException("Outing is full");
    }
    // ...
}
```

**Meccanismo:**

- _@Transactional(isolation = Isolation.SERIALIZABLE)_: Isolamento massimo
- _synchronized_: Sincronizzazione a livello di metodo
- _@Lock(LockModeType.PESSIMISTIC_WRITE)_ su repository

**Scopo:** Prevenire race condition quando più utenti cercano di unirsi simultaneamente.

**Resource condivisa:** _Participation_ (conteggio partecipanti approvati)

#### B. Observer Pattern con Parallelizzazione

**Ubicazione:** _NotificationManager.notifyObservers()_

```java
observers.parallelStream().forEach(observer -> {
    try {
        observer.onNotification(notification);
    } catch (Exception e) {
        log.error("Error notifying observer", e);
    }
});
```

**Meccanismo:**

- _CopyOnWriteArrayList_: Thread-safe per accesso concorrente
- _parallelStream()_: Esecuzione parallela di observer
- Gestione errori: un osservatore non blocca gli altri

**Scopo:** Notificare utenti in tempo reale senza blocking.

#### C. Scheduled Tasks

**Ubicazione 1:** _chat/ChatDeactivationScheduler.java_

```java
@Scheduled(cron = "0 0 2 * * *")  // 2:00 ogni giorno
public void deactivateExpiredChats() {
    // Disattiva chat più vecchie di 7 giorni
}
```

**Ubicazione 2:** _voucher/VoucherExpirationScheduler.java_

```java
@Scheduled(cron = "0 0 3 * * *")  // 3:00 ogni giorno
public void expireVouchers() {
    // Scade voucher più vecchi di 60 giorni
}
```

---

## 5. DIAGRAMMI UML

Tutti i diagrammi sono consultabili come file allegati. Nei seguenti paragrafi si specificano i nomi dei file di riferimento.

### 5.1 Class Diagram

**File:** _class-diagram_

Mostra:

- **12 entità JPA**: User, Event, Outing, Participation, Reward, Voucher, Chat, ChatMessage, Notification, Review, RefreshToken, Report
- **10 enumerazioni**: Role, ParticipationStatus, VoucherStatus, NotificationType, ReportType, ReportStatus
- **2 design pattern completi**: Strategy Pattern (limitazioni utente) + Observer Pattern (notifiche multi-canale)
- **Tutti i vincoli e relazioni**: associazioni, molteplicità, dipendenze tra package

**Approccio progettuale:**

Si è scelto un **approccio unificato** con un singolo class diagram comprensivo di tutti i package, anziché creare diagrammi separati per ogni package. Questa scelta è motivata da:

1. **Leggibilità**: visualizzazione immediata delle relazioni cross-package (es. User → Outing → Voucher)
2. **Completezza**: ogni entità è contestualizzata rispetto all'intero sistema
3. **Pattern integration**: i design pattern sono mostrati come sottografi integrati con le entità di dominio, evidenziando come Strategy e Observer interagiscono con il resto del sistema

I package sono comunque identificabili tramite:

- Commenti di sezione (_%% Core Entities_, _%% Design Pattern: Strategy Pattern_)
- Raggruppamento logico delle classi (entità di dominio, enumerazioni, pattern)
- Note esplicative per meccanismi di multithreading

**Discussione:**

Il diagramma delle classi illustra la struttura completa del dominio applicativo. Le entità core rappresentano i concetti principali del sistema: utenti con ruoli differenziati (User + Role), eventi organizzati da business (Event + Reward), uscite sociali (Outing + Participation), comunicazione real-time (Chat + ChatMessage), sistema di incentivi (Voucher), feedback (Review) e moderazione (Report).

Le 12 entità sono interconnesse da relazioni che rappresentano le dipendenze funzionali:

- **User è il fulcro** del sistema: crea eventi, organizza uscite, partecipa, scrive recensioni, guadagna voucher, invia messaggi, riceve notifiche, sottomette e riceve segnalazioni
- **Event e Outing** sono separati ma collegabili: gli utenti PREMIUM possono creare uscite associate a eventi business
- **Report** permette la moderazione della piattaforma: utenti possono segnalare contenuti/utenti inappropriati, gli admin revisionano le segnalazioni

**Design Pattern integrati:**

Il diagramma include due design pattern come sottografi visibili:

1. **Strategy Pattern** (_pattern.strategy_): implementa il polimorfismo delle limitazioni utente

   - Interfaccia _UserLimitationStrategy_ definisce il contratto
   - 3 strategie concrete (_BaseUserStrategy_, _PremiumUserStrategy_, _BusinessUserStrategy_) implementano regole specifiche per ogni ruolo
   - _UserStrategyFactory_ seleziona la strategia appropriata basandosi su _User.role_
   - **Integrazione**: _OutingService_ usa la factory per validare la creazione di uscite in base al ruolo dell'organizzatore

2. **Observer Pattern** (_pattern.observer_): gestisce le notifiche multi-canale in modo disaccoppiato
   - _NotificationSubject_ (interfaccia) definisce attach/detach/notify
   - _NotificationManager_ (Subject concreto) mantiene lista thread-safe di observer tramite _CopyOnWriteArrayList_
   - 2 Observer concreti: _PersistenceNotificationObserver_ (salva nel DB), _WebSocketNotificationObserver_ (invia via STOMP)
   - **Multithreading**: _parallelStream()_ esegue observer in parallelo (no blocking)
   - **Integrazione**: _ParticipationService_, _VoucherService_, _ChatService_ notificano eventi al manager, che propaga a tutti gli observer registrati

**Meccanismi di sincronizzazione:**

Il diagramma evidenzia 3 meccanismi di controllo della concorrenza tramite note:

- **Participation**: _synchronized_ + _@Transactional(SERIALIZABLE)_ + pessimistic locking per prevenire race condition nell'approvazione (scenario critico: 100 utenti per 10 posti)
- **Voucher**: _@Lock(PESSIMISTIC_WRITE)_ per evitare double redemption
- **NotificationManager**: _CopyOnWriteArrayList_ + _parallelStream()_ per notifiche concurrent senza blocking

Questo approccio integrato permette di comprendere sia la struttura statica del sistema sia i meccanismi dinamici di estensibilità (pattern) e concorrenza (multithreading).

### 5.2 Sequence Diagram - Partecipazione

**File:** _sequence-diagram_

Mostra:

- Uno user richiede di unirsi a un'uscita
- Il sistema valida la disponibilità di posti
- Si attiva Observer Pattern per le notifiche
- Le notifiche sono salvate nel DB e inviate via WebSocket in parallelo
- L'organizzatore riceve la notifica in tempo reale

**Discussione:** Il diagramma di sequenza per la partecipazione illustra il flusso completo dall'iscrizione alla ricezione della notifica real-time. L'elemento _par_ (parallelo) mostra come i due osservatori (_PersistenceObserver_ e _WebSocketObserver_) vengono eseguiti in parallelo tramite _parallelStream()_.

### 5.3 Sequence Diagram - Chat WebSocket

**File:** _sequence-diagram-chat-websocket_

Mostra:

- La connessione WebSocket con autenticazione JWT
- L'invio dei messaggi
- La presenza dell'indicatore di digitazione
- L'auto-disattivazione tramite lo scheduler

**Discussione:** Il diagramma della chat WebSocket mostra come i messaggi vengono trasmessi in tempo reale tramite STOMP broker. L'autenticazione è garantita dal JWT token, che viene verificato dall'interceptor del WebSocket. L'auto-disattivazione della chat dopo 7 giorni è gestita da un task scheduler che viene eseguito quotidianamente alle 2:00.

### 5.4 Sequence Diagram - Voucher

**File:** _sequence-diagram-voucher-redemption_

Mostra:

- La creazione automatica voucher dopo approvazione
- Il riscatto tramite QR code
- La gestione della scadenza tramite lo scheduler

**Discussione:** Il flusso del voucher è composto da tre fasi principali: emissione (automatica dopo l'approvazione di 5+ partecipanti), visualizzazione e riscatto. L'emissione automatica è verificata nel metodo _checkAndIssueVoucher()_ del _VoucherService_, che viene chiamato durante l'approvazione di ogni partecipazione. La scadenza è gestita da uno scheduler che viene eseguito quotidianamente alle 3:00.

### 5.5 Use Case Diagram

**File:** _use-case-diagram-1-events-outings_, _use-case-diagram-2-communication-rewards_, _use-case-diagram-3-administration_

Mostra:

- 22 use case
- 4 attori (User, Premium, Business, Admin)
- Dipendenze tra use case

**Discussione:** Il diagramma dei casi d'uso rappresenta tutte le funzionalità del sistema organizzate per attore. I 22 use case coprono le operazioni principali: gestione uscite (UC1-UC5, UC13-UC14), comunicazione (UC6, UC12, UC15), premi e voucher (UC7, UC11, UC16-UC18), recensioni (UC8, UC19) e amministrazione (UC10, UC20-UC22). Le dipendenze mostrano come certi casi d'uso estendono o richiedono altri; ad esempio, UC4 (Join Outing) è necessario per UC6 (Send Chat Message) e UC12 (View Chat History).

---

\newpage

## 6. INTERFACCIA GRAFICA - SCREENSHOT APPLICAZIONE

Questa sezione presenta 18 screenshot dell'applicazione frontend Angular che illustrano le principali funzionalità e il design dell'interfaccia utente. Gli screenshot sono organizzati per flusso funzionale e mostrano l'implementazione concreta dei requisiti descritti nelle sezioni precedenti.

### 6.1 Autenticazione e Navigazione

#### Screenshot 6.1.1: Pagina di Login

![Login](source/screenshots/screenshot-01-login.png){ width=85% }

**Descrizione:** Form di login con autenticazione JWT, validazione client-side.

\newpage

#### Screenshot 6.1.2: Pagina di Registrazione

![Registrazione](source/screenshots/screenshot-02-register.png){ width=85% }

**Descrizione:** Form registrazione con validazione real-time dei campi (nome, email, password, conferma). Redirect a login dopo creazione account.

#### Screenshot 6.1.3: Navbar con Menu Utente

![Navbar](source/screenshots/screenshot-03-navbar.png){ width=85% }

**Descrizione:** Navbar con logo, menu principale (Eventi, Uscite, Mappa, Voucher), icona notifiche, menu utente. Voci differenziate per ruolo (Admin ha pannello).

\newpage

#### Screenshot 6.1.4: Dropdown Notifiche

![Notifiche](source/screenshots/screenshot-04-notifications.png){ width=85% }

**Descrizione:** Lista notifiche, timestamp relativo. Non lette evidenziate.

\newpage

### 6.2 Eventi

#### Screenshot 6.2.1: Lista Eventi

![Lista Eventi](source/screenshots/screenshot-05-event-list.png){ width=85% }

**Descrizione:** Griglia responsive di card eventi con immagine, titolo, location, data. Filtri e ordinamento.

\newpage

\newpage

#### Screenshot 6.2.2: Dettaglio Evento

![Dettaglio Evento](source/screenshots/screenshot-06-event-details.png){ width=85% }

**Descrizione:** Dettaglio con titolo, organizzatore, descrizione, dettagli. Sezione "Ricompense" per utenti premium, sezione per cercare uscite collegate o creare un'uscita collegata. Link per pagina mappa. Possibilità di segnalazione.

\newpage

#### Screenshot 6.2.3: Form Creazione Evento

![Creazione Evento](source/screenshots/screenshot-07-event-creation.png){ width=85% }

**Descrizione:** Form: titolo/descrizione, risoluzione automatica coordinate da indirizzo, datepicker data/ora. Validazione real-time.

\newpage

### 6.3 Uscite

#### Screenshot 6.3.1: Lista Uscite

![Lista Uscite](source/screenshots/screenshot-08-outing-list.png){ width=85% }

**Descrizione:** Griglia card con organizzatore, max posti disponibili, location. Indicazione per uscite collegate ad eventi. Filtri e ordinamento.

\newpage

#### Screenshot 6.3.2: Dettaglio Uscita

![Dettaglio Uscita](source/screenshots/screenshot-09-outing-details.png){ width=85% }

**Descrizione:** Dettagli, lista partecipanti, accesso alla chat, recensioni. Lista partecipanti da approvare per organizzatore. Bottone "Partecipa", "Lascia". Possibilità di segnalazione.

\newpage

#### Screenshot 6.3.3: Form Creazione Uscita

![Creazione Uscita](source/screenshots/screenshot-10-outing-creation.png){ width=85% }

**Descrizione:** Form con dropdown eventi da linkare, risoluzione automatica da indirizzo a coordinate, max partecipanti con limite per ruolo. Datepicker.

\newpage

#### Screenshot 6.3.4: Gestione Partecipanti (Organizzatore)

![Gestione Partecipanti](source/screenshots/screenshot-11-participant-management.png){ width=85% }

**Descrizione:** Lista con partecipanti da approvare. Bottoni Approva/Rifiuta. Notifica real-time al partecipante.

\newpage

### 6.4 Chat Real-Time

#### Screenshot 6.4.1: Chat Attiva

![Chat Attiva](source/screenshots/screenshot-12-chat.png){ width=85% }

**Descrizione:** Chat con bubble differenziati, nome utente, timestamp. Indicatore digitazione "sta scrivendo...". Input testo. WebSocket real-time.

\newpage

### 6.5 Mappa e Geolocalizzazione

#### Screenshot 6.5.1: Vista Mappa con Marker

![Mappa](source/screenshots/screenshot-13-map.png){ width=85% }

**Descrizione:** Mappa Leaflet con segnaposto (colori differenziati tra eventi e ucite). Filtro raggio. Risoluzione automatica indirizzo. Possibilità di utilizzare posizione utente.

\newpage

### 6.6 Voucher

#### Screenshot 6.6.1: Lista Voucher

![Lista Voucher](source/screenshots/screenshot-14-voucher-list.png){ width=85% }

**Descrizione:** Griglia voucher con QR code, premio, entità rilascio, status, data rilascio e data scadenza.

\newpage

### 6.7 Profilo Utente

#### Screenshot 6.7.1: Profilo - Tab Informazioni

![Profilo Info](source/screenshots/screenshot-15-profile-info.png){ width=85% }

**Descrizione:** Header con nome, ruolo. Tab Informazioni: dettagli account.

\newpage

#### Screenshot 6.7.2: Profilo - Tab Modifica

![Profilo Edit](source/screenshots/screenshot-16-profile-edit.png){ width=85% }

**Descrizione:** Form editabile: nome, email password. Validazione modifiche. Possibilità reset prima di salvare.

\newpage

#### Screenshot 6.7.3: Profilo - Tab Impostazioni

![Profilo Settings](source/screenshots/screenshot-17-profile-settings.png){ width=85% }

**Descrizione:** Upgrade / downgrade profilo. Logout.

\newpage

### 6.8 Amministrazione

#### Screenshot 6.8.1: Dashboard Admin

![Admin Dashboard](source/screenshots/screenshot-18-admin-dashboard.png){ width=85% }

**Descrizione:** Dashboard con card statistiche. Nella tab user management é possibile cambiare ruolo agli utenti e bannarli/sbannarli.

---

## 7. PIANO DI TEST

### 7.1 Test di Unità (Unit Tests)

**Ubicazione:** _backend/src/test/java/com/alfano/gathorapp/_

**Copertura richiesta:** >= 80%

**Test principali:**

```
Package            Test Class                      Count  Oggetto
─────────────────────────────────────────────────────────────────────────
pattern/strategy   UserLimitationStrategyTest        30   Limiti per ruolo
pattern/observer   NotificationObserverTest          20   Observer + parallelizz.
participation      ParticipationServiceTest          20   Race condition, locking
outing             OutingServiceTest                 26   CRUD e validazioni
event              EventServiceTest                  14   Gestione eventi
voucher            VoucherServiceTest                19   Generazione voucher
chat               ChatServiceTest                   24   Invio messaggi
chat               ChatDeactivationSchedulerTest     8    Auto-disattivazione
user               UserServiceTest                   19   Gestione utenti
auth               AuthServiceTest                   11   JWT Authentication
notification       NotificationServiceTest           15   Creazione notifiche
review             ReviewServiceTest                 15   Gestione recensioni
```

**Framework:** JUnit 5 + Mockito

**Totale test:** 585 test eseguiti (di cui 221 principali elencati sopra)

**Comando esecuzione:**

```bash
cd backend
gradle test
gradle jacocoTestReport
```

### 7.2 Test di Integrazione (Integration Tests)

Test di controller con MockMvc per verificare l'integrazione tra controller, service e repository:

- _OutingControllerIntegrationTest_ - Test endpoint outings (7 test)
- _ParticipationControllerIntegrationTest_ - Test endpoint partecipazioni (8 test)
- _ChatControllerIntegrationTest_ - Test endpoint chat (6 test)
- _VoucherControllerIntegrationTest_ - Test endpoint voucher (9 test)
- _UserControllerIntegrationTest_ - Test endpoint utenti (3 test)
- _ReviewControllerIntegrationTest_ - Test endpoint recensioni (3 test)

### 7.3 Test Manuale - Chat WebSocket

Procedura di test manuale:

1. Connessione WebSocket con JWT (verificare autenticazione)
2. Invio e ricezione messaggi tra utenti
3. Indicatore di digitazione in tempo reale
4. Auto-disattivazione chat dopo 7 giorni dall'uscita

### 7.4 Report Copertura JaCoCo

**Ubicazione:** _backend/build/reports/jacoco/test/html/index.html_

**Copertura raggiunta:** 88% (verificata con JaCoCo)

**Verifica minima 80%:**

```bash
gradle jacocoTestCoverageVerification
```

**Report:** Analizza la copertura per ogni package, classe e metodo. Le classi critiche (ParticipationService, NotificationManager, VoucherService) hanno una copertura superiore all'85%.

\newpage

**Code Coverage Report**

\footnotesize

| Pkg           | Missed Instr. (Lost/Cov.) | Cov.    | Missed Branches (Lost/Cov.) | Cov.    | Missed (Cxty) | Cxty    | Missed (Lns) | Lns       | Missed (Mthds) | Mthds   | Missed (Clss) | Clss   |
| :------------ | :------------------------ | :------ | :-------------------------- | :------ | :------------ | :------ | :----------- | :-------- | :------------- | :------ | :------------ | :----- |
| **Total**     | **986 of 8,619**          | **88%** | **55 of 347**               | **84%** | **113**       | **627** | **239**      | **2,008** | **71**         | **452** | **1**         | **94** |
| _event_       | 111 / 322                 | 74%     | 4 / 10                      | 71%     | 12            | 32      | 29           | 109       | 8              | 25      | 0             | 4      |
| _admin_       | 110 / 339                 | 75%     | -                           | n/a     | 9             | 23      | 28           | 104       | 9              | 23      | 0             | 2      |
| _websock._    | 102 / 64                  | 38%     | 12 / 0                      | 0%      | 8             | 12      | 25           | 36        | 2              | 6       | 0             | 2      |
| _notific._    | 92 / 292                  | 76%     | 0 / 6                       | 100%    | 6             | 26      | 24           | 100       | 6              | 23      | 0             | 4      |
| _review_      | 91 / 353                  | 79%     | 2 / 38                      | 95%     | 8             | 46      | 25           | 107       | 7              | 26      | 0             | 6      |
| _except._     | 80 / 306                  | 79%     | -                           | n/a     | 5             | 19      | 18           | 80        | 5              | 19      | 0             | 4      |
| _auth_        | 77 / 425                  | 84%     | 2 / 12                      | 85%     | 8             | 33      | 18           | 139       | 6              | 26      | 0             | 4      |
| _reward_      | 70 / 127                  | 64%     | 1 / 5                       | 83%     | 4             | 14      | 21           | 52        | 3              | 11      | 0             | 4      |
| _user_        | 46 / 449                  | 90%     | 11 / 31                     | 73%     | 9             | 47      | 8            | 108       | 1              | 25      | 0             | 5      |
| _report_      | 44 / 489                  | 91%     | 1 / 3                       | 75%     | 9             | 34      | 4            | 121       | 8              | 32      | 1             | 7      |
| _outing_      | 36 / 734                  | 95%     | 4 / 24                      | 85%     | 6             | 57      | 7            | 180       | 3              | 43      | 0             | 4      |
| _voucher_     | 35 / 574                  | 94%     | 6 / 34                      | 85%     | 8             | 48      | 8            | 149       | 2              | 28      | 0             | 7      |
| _partic._     | 23 / 605                  | 96%     | 5 / 19                      | 79%     | 5             | 38      | 3            | 140       | 2              | 26      | 0             | 5      |
| _config_      | 18 / 699                  | 97%     | 1 / 39                      | 97%     | 1             | 41      | 7            | 163       | 0              | 21      | 0             | 4      |
| _secur._      | 18 / 140                  | 88%     | 0 / 8                       | 100%    | 6             | 21      | 5            | 39        | 6              | 17      | 0             | 4      |
| _config seed_ | 17 / 138                  | 89%     | 4 / 4                       | 50%     | 4             | 8       | 3            | 26        | 0              | 4       | 0             | 1      |
| _chat_        | 11 / 686                  | 98%     | 2 / 28                      | 93%     | 4             | 50      | 4            | 170       | 2              | 35      | 0             | 12     |
| _root_        | -                         | 37%     | -                           | n/a     | 1             | 2       | 2            | 3         | 1              | 2       | 0             | 1      |
| _map_         | 0 / 560                   | 100%    | 0 / 14                      | 100%    | 0             | 31      | 0            | 87        | 0              | 24      | 0             | 6      |
| _patt. obs._  | 0 / 211                   | 100%    | 0 / 4                       | 100%    | 0             | 18      | 0            | 62        | 0              | 16      | 0             | 4      |
| _patt. str._  | 0 / 117                   | 100%    | 0 / 13                      | 100%    | 0             | 27      | 0            | 33        | 0              | 20      | 0             | 4      |

\normalsize

---

## 8. DEPLOYMENT E ESECUZIONE

### 8.1 Avvio Backend

**Prerequisiti**: java JDK 17 e gradle installati

```bash
cd backend

# Build
gradle clean build

# Esecuzione con H2
gradle bootRun
```

**Backend disponibile:** _http://localhost:8080_  
**Swagger UI:** _http://localhost:8080/swagger-ui.html_

### 8.2 Avvio Frontend

**Prerequisiti**: NodeJS installato

```bash
cd frontend
npm install
npm start
```

**Frontend disponibile:** _http://localhost:4200_

### 8.3 Database Seeding

Il _DataSeeder_ crea automaticamente:

- Admin user (admin@gathorapp.com / admin123)
- Utenti business con eventi e premi
- Utenti premium
- Utenti standard
- Uscite

---

## 9. CONSIDERAZIONI SULLA PROGETTAZIONE

### 9.1 Scelta del linguaggio

- Richiesta del corso: Java è il linguaggio consigliato
- Programmazione a oggetti: sfrutta appieno le potenzialità OOP di Java
- Framework maturo: Spring Boot è ben documentato e largamente utilizzato
- Performance: Java è performante per applicazioni web
- Type safety: Type system forte previene bug

### 9.2 Gestione della Concorrenza

La gestione della concorrenza è fondamentale nel sistema di partecipazioni:

**Scenario critico:** 100 utenti cercano di unirsi a un'uscita con 10 posti disponibili.

**Soluzione implementata:**

1. **SERIALIZABLE isolation:** Impedisce "phantom reads"
2. **Synchronized method:** Lock a livello di thread JVM
3. **Pessimistic locking:** Database-level lock (SELECT FOR UPDATE)

Questo triplo livello garantisce che solo 10 transazioni avranno successo.

### 9.3 Pattern Observer e Prestazioni

Con _parallelStream()_, le notifiche vengono inviate in parallelo:

- Osservatore database: salva su DB (~10ms)
- Osservatore WebSocket: invia messaggio real-time (~50ms)

Senza parallelizzazione: ~60ms sequenziale  
Con parallelizzazione: ~50ms parallelo (tempo del più lento)

### 9.4 Scelta delle Tecnologie Frontend

Angular 20 è stato scelto per:

- Componenti standalone (nessun NgModule)
- Type safety con TypeScript
- OpenAPI Generator per client auto-generato
- Supporto WebSocket nativo

---

## 10. QUALITÀ DEL CODICE

- **Javadoc completo:** Ogni classe e metodo documentato
- **Logging:** SLF4J con livelli DEBUG, INFO, WARN, ERROR
- **Exception handling:** Eccezioni custom e messaggi specifici
- **Naming conventions:** Seguono Java standard
- **DRY principle:** Riduzione della duplicazione di codice
- **SOLID principles:** Applicati nel design

---

## 11. CONFORMITÀ AI REQUISITI DELL'ESAME

✓ **Architettura client/server:** Backend Spring + Frontend Angular  
✓ **Pattern MVC:** Controller → Service → Repository  
✓ **Pattern aggiuntivi:** Strategy + Observer (documentati e testati)  
✓ **OOP:** Uso estensivo di classi, ereditarietà, polimorfismo, incapsulamento  
✓ **Unit test >= 80%:** JUnit 5 + JaCoCo (88% copertura raggiunta)  
✓ **Interfaccia grafica:** Angular 20 con componenti standalone  
✓ **Multithreading:** Observer pattern con _parallelStream()_ + pessimistic locking con _synchronized_  
✓ **Documentazione:** UML completo + Javadoc + README  
✓ **Resource condivisa:** _Participation_ entity con controllo concorrenza  
✓ **Lock/Semafori:** SERIALIZABLE isolation + pessimistic locking + synchronized

---

## RIFERIMENTI

- Repository GitHub: https://github.com/lysandre995/gathorapp
- Spring Boot Documentation: https://docs.spring.io/spring-boot/
- Angular Documentation: https://angular.io
- OpenAPI/Swagger: http://localhost:8080/swagger-ui.html
- Design Patterns: https://refactoring.guru/design-patterns
- Java Concurrency: https://docs.oracle.com/javase/tutorial/essential/concurrency/

## ALLEGATI

- Class Diagram (class-diagram.pdf)
- Use Case Diagram: Eventi e Uscite (use-case-diagram-1-events-outings.pdf)
- Use Case Diagram: Comunicazione e Ricompense (use-case-diagram-2-communication-rewards.pdf)
- Use Case Diagram: Amministrazione (use-case-diagram-3-administration.pdf)
- Sequence Diagram: Outing Participation Flow (sequence-diagram.pdf)
- Sequence Diagram: Chat Websocket (sequence-diagram-chat-websocket.pdf)
- Sequence Diagram: Voucher Redemption (sequence-diagram-voucher-redemption.pdf)
