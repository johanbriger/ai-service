# FunnyAI - Fullstack AI Platform

En komplett fullstack-applikation som erbjuder en interaktiv chattupplevelse med olika AI-personligheter. Projektet består av en fristående React-frontend och en Spring Boot-backend som kommunicerar säkert med externa LLM-modeller via OpenRouter.

---

## 🌐 Live Demo

Applikationen är fullt driftsatt i molnet och redo att testas direkt i din webbläsare:

* **Testa appen här:** [Klicka här för att öppna FunnyAI](https://din-frontend-länk-på-railway.app)

*(Om du vill köra projektet lokalt på din egen maskin, följ instruktionerna för "Lokal utveckling" längre ner).*

---

## Funktioner

### Frontend (React & Vite)
* **Dynamiska personligheter:** Välj mellan "Pappa-humor", "Sarkastiske Simon" eller "Kaos-boten" för att styra AI:ns tonläge.
* **Animerad SVG-logo:** En vektoriserad logotyp med inbäddade CSS-animationer (svävande huvud och roterande laddningsring) för snabb laddningstid och skarp skärpa.
* **Sessioner:** Använder lokalt lagrade UUID:er för att bibehålla konversationen även vid sidomladdning.

### Backend (Spring Boot 3.2+)
* **Intelligent Middleware:** Fungerar som en säker brygga som döljer känsliga API-nycklar för klienten.
* **Trådsäker minneshantering:** Sparar konversationshistorik i minnet med en trådsäker `ConcurrentHashMap`.
* **Automatisk städning:** En bakgrunds-scheduler (`@Scheduled`) körs en gång i timmen och rensar bort sessioner som varit inaktiva i mer än 24 timmar för att förhindra minnesläckor.
* **Resiliens & Retry:** Inbyggd retry-logik med Exponential Backoff (2s, 4s, 8s) för att hantera temporära nätverksfel mot externa API:er.
* **CORS-säkerhet:** Konfigurerad för kontrollerad och säker kommunikation mellan frontend och backend.

---

## Arkitektur & Teknikstack

* **Frontend:** React 18, Vite, Lucide React (ikoner), CSS3.
* **Backend:** Java 21, Spring Boot 3.2+, Maven.
* **API-klient:** Spring Boots synkrona `RestClient`.
* **Drift:** Railway (PaaS) med fullständig HTTPS-kryptering.
* **CI/CD:** Automatiserad pipeline via GitHub Actions.

---

## Tester & Kvalitetssäkerhet

* **WireMock:** Simulerar externa API-svar och felscenarier (t.ex. 503 Service Unavailable) för att verifiera backendens retry-logik.
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

*Obs: I produktion på Railway ändras denna till din live-URL (t.ex. `https://din-backend.railway.app/api/v1`).*

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

* **Swagger UI:** http://localhost:8080/swagger-ui.html
