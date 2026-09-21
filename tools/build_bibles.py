#!/usr/bin/env python3
"""Downloads public domain Bible texts and converts them into the per-book JSON assets the app bundles.

Source: https://github.com/scrollmapper/bible_databases (formats/json)
Output: app/src/main/assets/bibles/<id>/<book 01-66>.json  ->  [[verse, verse, ...], ...]  (one inner list per chapter)

Run from the repo root:  python3 tools/build_bibles.py
"""
import json
import pathlib
import urllib.request

BASE = "https://raw.githubusercontent.com/scrollmapper/bible_databases/master/formats/json/{}.json"
OUT = pathlib.Path("app/src/main/assets/bibles")

# id, upstream file, display name, abbreviation, blurb
VERSIONS = [
    ("kjv", "KJV", "King James Version", "KJV", "1769 Oxford edition. The classic English Bible."),
    ("asv", "ASV", "American Standard Version", "ASV", "1901. A literal, word-for-word revision of the KJV."),
    ("nheb", "NHEB", "New Heart English Bible", "NHEB", "A modern-English update of the World English Bible. Public domain."),
]

# Must match BookCatalog.kt
CHAPTER_COUNTS = [50, 40, 27, 36, 34, 24, 21, 4, 31, 24, 22, 25, 29, 36, 10, 13, 10, 42, 150, 31, 12, 8, 66, 52, 5, 48, 12, 14, 3, 9,
                  1, 4, 7, 3, 3, 3, 2, 14, 4, 28, 16, 24, 21, 28, 16, 16, 13, 6, 6, 4, 4, 5, 3, 6, 4, 3, 1, 13, 5, 5, 3, 5, 1, 1, 1, 22]


def main():
    manifest = []
    for vid, upstream, name, abbr, blurb in VERSIONS:
        print(f"{vid}: downloading {upstream}")
        with urllib.request.urlopen(BASE.format(upstream)) as r:
            data = json.load(r)
        books = data["books"]
        assert [len(b["chapters"]) for b in books] == CHAPTER_COUNTS, f"{vid}: chapter counts differ"
        out_dir = OUT / vid
        out_dir.mkdir(parents=True, exist_ok=True)
        for i, book in enumerate(books, start=1):
            chapters = []
            for ch in book["chapters"]:
                nums = [v["verse"] for v in ch["verses"]]
                assert nums == list(range(1, len(nums) + 1)), f"{vid}: non-sequential verses in {book['name']} {ch['chapter']}"
                chapters.append([v["text"].strip() for v in ch["verses"]])
            (out_dir / f"{i:02d}.json").write_text(json.dumps(chapters, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
        manifest.append({"id": vid, "name": name, "abbreviation": abbr, "description": blurb})
    (OUT / "versions.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")
    print("done")


if __name__ == "__main__":
    main()
