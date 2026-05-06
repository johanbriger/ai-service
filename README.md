# FunnyAI - Fullstack AI Platform

En komplett fullstack-applikation som erbjuder en interaktiv chattupplevelse med olika AI-personligheter. Projektet består av en fristående React-frontend och en Spring Boot-backend som fungerar som en säker brygga mot externa LLM-modeller via OpenRouter.

---

## 🌐 Live Demo

Applikationen är fullt driftsatt i molnet och redo att testas direkt i din webbläsare:

* **Testa appen här:** [Klicka här för att öppna FunnyAI](https://funnyai-johanbriger.up.railway.app)

*(Om du vill köra projektet lokalt på din egen maskin, följ instruktionerna för "Lokal utveckling" längre ner).*

---

## Funktioner

### Frontend (React & Vite)
* **Dynamiska personligheter:** Välj mellan "Pappa-humor", "Sarkastiske Simon" eller "Kaos-boten" för att styra AI:ns tonläge.
* **Animerad SVG-logo:** En vektoriserad logotyp med inbäddade CSS-animationer (svävande huvud och roterande laddningsring) för snabb laddningstid och skarp skärpa.
* **Sessioner:** Använder lokalt lagrade UUID:er för att bibehålla konversationen även vid sidomladdning.
* **Smart felvisning:** React-appen läser av specifika felkoder från backenden och visar tydliga, användarvänliga meddelanden (t.ex. vid nätverksproblem eller rate limits).

### Backend (Spring Boot 3.2+)
* **Intelligent Middleware:** Fungerar som en säker brygga som döljer känsliga API-nycklar för klienten.
* **Trådsäker minneshantering:** Sparar konversationshistorik i minnet med en trådsäker `ConcurrentHashMap`.
* **Automatisk städning:** En bakgrunds-scheduler (`@Scheduled`) körs en gång i timmen och rensar bort sessioner som varit inaktiva i mer än 24 timmar för att förhindra minnesläckor.
* **Resiliens & Retry:** Robust felhantering med Spring Retry. Vid tillfälliga nätverksfel eller serverfel (5xx) mot OpenRouter görs upp till 3 automatiska försök med Exponential Backoff (2s, 4s) och slumpmässig variation (Jitter) för att undvika överbelastning.
* **Säker historikhantering:** Om ett anrop misslyckas på grund av ett klientfel (t.ex. 400 Bad Request) rensas det felaktiga meddelandet automatiskt från historiken så att chatten inte låser sig permanent.
* **CORS-säkerhet:** Konfigurerad för kontrollerad och säker kommunikation mellan frontend och backend.

---

## Systemets dataflöde

Här är en översikt av hur en förfrågan rör sig genom systemet och hur felhanteringen agerar som ett skyddande nät:

```text
[ React Frontend ] 
       │
       ▼ (POST /api/v1/chat)
[ ChatController ]
       │
       ▼ (processChat)
[ ChatService ] ──(Hämtar/skapar session i ConcurrentHashMap)
       │
       ▼ (RestClient med defaultStatusHandler)
[ OpenRouter API ]
       │
       ├─► [ OK (200) ] ──► (Svaret sparas i historiken och returneras till React)
       │
       ├─► [ Tillfälligt fel (5xx / Timeout) ]
       │         │
       │         └─► Spring Retry gör upp till 3 försök med Exponential Backoff & Jitter.
       │             Om alla försök misslyckas kastas felet vidare till...
       │
       └─► [ GlobalExceptionHandler ]
                 │
                 └─► Mappar felet till en strukturerad ErrorResponse (JSON)
                     som skickas till React med rätt HTTP-status (t.ex. 429 eller 503).
```

---

## Arkitektur & Teknikstack

* **Frontend:** React 18, Vite, Lucide React (ikoner), React Markdown, CSS3.
* **Backend:** Java 17, Spring Boot 3.2.5, Maven, Spring Retry, Spring Aspects (AOP).
* **API-klient:** Spring Boots synkrona `RestClient` med anpassade felhanterare.
* **Drift:** Railway (PaaS) med fullständig HTTPS-kryptering.
* **CI/CD:** Automatiserad pipeline via GitHub Actions.

---

## Tester & Kvalitetssäkerhet

* **WireMock & Spring Retry-test:** Integrations- och resiliens-tester som simulerar externa API-svar och felscenarier (t.ex. 500 Internal Server Error) för att verifiera att backendens automatiska återförsök fungerar i praktiken.
* **WebMvcTest:** Enhetstester för kontrollernivån (`ChatControllerTest`) som verifierar att API-endpoints returnerar korrekt JSON-struktur.
* **GitHub Actions:** Automatiserad testkörning (inklusive integrationstester) vid varje push till `main`-branchen innan driftsättning till Railway.

---

## Installation & Driftsättning

### Miljövariabler

#### Backend (`/backend/src/main/resources/application.properties` eller miljövariabler i Railway):

* `AI_API_KEY`: Din OpenRouter API-nyckel (Måste anges)
* `AI_BASE_URL`: Sökväg till AI-tjänsten (Standard: `https://openrouter.ai/api/v1`)
* `PORT`: Porten som Spring Boot lyssnar på (Standard: `8080`)

#### Frontend (`/frontend/.env`):

`VITE_API_URL=http://localhost:8080/api/v1`

*I produktion på Railway ändras denna till din live-URL (t.ex. `https://funnyai-backend.railway.app/api/v1`).*

---

### Lokal utveckling

#### 1. Starta Backenden
Öppna projektet i din IDE och kör `AiServiceApplication.java`, eller starta genom att köra följande kommandon i terminalen:

```bash
cd backend
mvn spring-boot:run
```

#### 2. Starta Frontenden
Öppna en ny terminal, installera beroenden och starta Vites lokala utvecklingsserver genom att köra:

```bash
cd frontend
npm install
npm run dev
```

*Obs: Om du gör ändringar i din `.env`-fil måste du starta om utvecklingsservern (`Ctrl + C` och sedan `npm run dev`) för att de nya variablerna ska läsas in.*

---

## API-Dokumentation

När backenden körs lokalt finns interaktiv Swagger-dokumentation tillgänglig på:

* **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
