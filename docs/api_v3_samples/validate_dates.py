#!/usr/bin/env python3
"""
Walidacja formatów dat w przykładach API v3
Sprawdza czy wszystkie daty używają formatu: YYYY-MM-DD HH:MM:SS
"""

import json
import re
from pathlib import Path

DATE_PATTERN = re.compile(r'^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$')

def extract_dates(obj, path=""):
    """Rekurencyjnie wyciąga wszystkie wartości z kluczem 'created_at'"""
    dates = []

    if isinstance(obj, dict):
        for key, value in obj.items():
            new_path = f"{path}.{key}" if path else key
            if key == "created_at" and isinstance(value, str):
                dates.append((new_path, value))
            else:
                dates.extend(extract_dates(value, new_path))
    elif isinstance(obj, list):
        for i, item in enumerate(obj):
            dates.extend(extract_dates(item, f"{path}[{i}]"))

    return dates

def main():
    samples_dir = Path(__file__).parent
    json_files = sorted(samples_dir.glob("*.json"))

    print("=" * 70)
    print("Walidacja Formatów Dat - Wykop API v3")
    print("=" * 70)
    print()

    all_dates = []
    valid_count = 0
    invalid_count = 0

    for json_file in json_files:
        with open(json_file, 'r', encoding='utf-8') as f:
            data = json.load(f)

        dates = extract_dates(data)

        if dates:
            print(f"Plik: {json_file.name}")
            for path, date_value in dates:
                is_valid = DATE_PATTERN.match(date_value)
                status = "OK" if is_valid else "BŁĄD"
                print(f"  [{status}] {path}: {date_value}")

                if is_valid:
                    valid_count += 1
                    all_dates.append(date_value)
                else:
                    invalid_count += 1
            print()

    print("=" * 70)
    print("Podsumowanie:")
    print("=" * 70)
    print(f"Poprawne daty: {valid_count}")
    print(f"Niepoprawne daty: {invalid_count}")
    print()

    if all_dates:
        print("Unikalne daty znalezione:")
        for date in sorted(set(all_dates)):
            print(f"  - {date}")
        print()

    print("Weryfikacja formatu:")
    print(f"  Wzorzec: YYYY-MM-DD HH:MM:SS")
    print(f"  Przykład: 2026-02-27 14:30:45")
    print()

    if invalid_count > 0:
        print("UWAGA: Znaleziono niepoprawne formaty dat!")
        return 1
    else:
        print("Wszystkie daty używają poprawnego formatu!")
        return 0

if __name__ == "__main__":
    exit(main())
