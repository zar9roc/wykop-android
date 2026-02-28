# Wykop Android - Project Memory

## API v3 OpenAPI Specification
- **File**: `docs/wykop_api_v3_openapi.yaml` - kompletny plik OpenAPI 3.0.3 dla Wykop API v3
- **RULE**: Zawsze sprawdzaj ten plik jako jedyne źródło prawdy o API v3 przy:
  - Tworzeniu interfejsów Retrofit
  - Przebudowie endpointów
  - Tworzeniu modeli request/response
  - Weryfikacji dostępnych endpointów i ich parametrów
- Zawiera: 145 endpointów, 35 schematów, 18 parametrów, JWT bearer auth
- Źródło: Złożony z https://doc.wykop.pl/openapi.yaml + ~145 plików zasobów + ~55 plików schematów

## Project Stack
- Kotlin, Android, Retrofit + Moshi (codegen), Dagger 2, RxJava 2
- Migracja z API v1/v2 na v3 (w toku)
- JWT token storage via JwtTokenStorage (migracja z UserSession)
- Response models: `io.github.wykopmobilny.api.responses.v3`
- Moshi: `@JsonClass(generateAdapter = true)`
