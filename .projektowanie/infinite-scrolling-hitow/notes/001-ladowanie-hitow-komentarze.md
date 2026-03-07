# Ładowanie hitów - komentarze.

Diagram przedstawia pełny flow infinite scrolling w `HitsPresenter`:

1. **Start** - `HitsFragment.onViewCreated()` wywołuje `loadData()` z `currentPage = null`
2. **Wybór endpointu** - na podstawie `currentScreen` (day/week/month/year)
3. **Wariant auth** - zalogowany użytkownik wysyła JWT token, API zwraca stan głosu (`voted`); niezalogowany nie ma tokena, `voted=null`
4. **Token refresh** - `UserTokenRefresher` automatycznie odświeża wygasły token
5. **Obsługa odpowiedzi** - jeśli `totalCount > 0`: zapisz `nextPage` i dodaj items; jeśli 0: `disableLoading()` kończy paginację
6. **Infinite scroll** - `EndlessScrollListener` wykrywa scroll 2 itemy przed końcem i triggeruje `loadMore()`
7. **Głosowanie** - zalogowany może wykopać/zakopać; niezalogowany dostanie błąd API

---
*ID: 1*
*Created: 2026-03-07T22:07:45.255Z*
*Updated: 2026-03-07T22:07:45.255Z*
