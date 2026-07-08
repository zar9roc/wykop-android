# CLAUDE.md — konwencje projektu Otwarty Wykop Mobilny

## Ikony
- Ikony muszą być **monochromatyczne** (czarne lub białe) i **tintowalne** — przyjmować kolor przez `android:tint` / atrybut motywu (`?attr/colorControlNormal` itp.), spójnie ze stylem istniejących `ic_*` (vector drawable w `ui/base/android/.../res/drawable`).
- **Nie używać emoji** jako ikon ani w UI.
- Kolorowy akcent (np. żółta „kartka" notatki) uzyskujemy przez `tint` na monochromatycznym vectorze — nie przez wielokolorową grafikę.
