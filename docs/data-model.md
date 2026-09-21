# YourBiblePace data model

This is the contract between the app (Room, on the phone) and the cloud (Firestore). Firestore is schemaless, so
**adding** fields is always safe. **Renaming, removing or re-typing** a field is what hurts once real people have data,
so treat everything below as permanent.

## Principles
1. **Local first.** Room on the phone is the source of truth. The app never needs the network to read or mark chapters.
2. **Never lose progress.** Merges only ever prefer keeping a read. Nothing is physically deleted (see tombstones).
3. **Additive evolution only.** New fields are optional and readers ignore fields they don't know. `schemaVersion` exists
   so a real migration is possible, but the plan is never to need one.
4. **Version independent.** A chapter is identified by canonical book number (1-66, Protestant order) and chapter number.
   Never by book name or Bible version, so reading John 3 in the KJV counts for the ASV too.

## Identifiers
`chapterId = "BBB_CCC"`, zero padded: Genesis 1 = `001_001`, John 3 = `043_003`, Psalm 119 = `019_119`, Revelation 22 = `066_022`.

## Firestore
```
users/{uid}                          (profile, reserved; not written yet)
    schemaVersion   number   1
    createdAt       number   epoch millis (UTC)

users/{uid}/reads/{chapterId}        (one document per chapter, at most 1,189 per user)
    book            number   1-66
    chapter         number   1-based
    readAt          number?  epoch millis (UTC) when the user marked it read. null = imported, date unknown
    readDay         string?  the user's LOCAL calendar day, "2026-09-21". Keeps late-night reading on the right day
    version         string?  Bible version being read, e.g. "kjv". Informational only
    updatedAt       number   epoch millis (UTC) of the last change to this document. Drives conflict resolution
    serverUpdatedAt timestamp set by Firestore (server clock) on every write. Only used as the sync cursor, never for conflicts
    deleted         boolean  true = the user un-marked it (a tombstone)
    schemaVersion   number   1
```
`{uid}` is the Firebase Auth user id (one per Google account).

### Why these choices
- **One document per chapter, not one big list.** Two devices can edit different chapters without overwriting each other.
- **Tombstones (`deleted: true`) instead of deleting documents.** Otherwise a device that was offline would re-upload a
  chapter you had un-marked and it would come back.
- **`readDay` is stored, not derived.** The local day can't be reconstructed later from a UTC time without knowing the
  time zone. Future history and pace features need it.
- **`readAt` is nullable** because progress imported from the pre-database version has no date. We don't invent one.
- **Client timestamps** (epoch millis). Good enough for two people. A phone with a badly wrong clock could win a conflict
  it shouldn't; acceptable for now.

### Sync
`ProgressRepository.sync`, one pass, run on app start, after every tap, on sign-in and on "Sync now":
1. **Pull** documents with `serverUpdatedAt` after the saved cursor (minus a 5 second overlap), so a normal sync reads only what changed
   instead of all 1,189 documents. First sync on a device reads everything once.
2. **Merge** each pulled document into Room with the rule below.
3. **Push** every row still flagged `dirty`, then clear the flag only if the row hasn't changed in the meantime.
Offline or failed: nothing is lost, rows stay dirty and the next pass retries. Firestore's own offline disk cache is switched off
on purpose; Room is the offline store, so a stale write can't be replayed later over newer data.

Known, accepted race: two devices changing the very same chapter within the seconds between one device's pull and push can lose the
older change. For a personal app this is negligible. The fix if it ever matters is to push inside a Firestore transaction.

### Signing out
Sign-out first syncs, refuses if anything is still unsynced, then wipes the local copy. This stops the next person to sign in on the same
phone from inheriting (and uploading) someone else's progress. Progress made while signed out is treated as the first account's: it merges
into whichever account signs in first.

### Merge rule (same chapter on two devices)
Higher `updatedAt` wins. On an exact tie, the non-deleted (read) copy wins. See `ChapterMerge.kt`.

### Security rules (to paste into the Firebase console)
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{uid}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == uid;
    }
  }
}
```

### Account deletion
Google Play requires apps that let people create an account to also let them delete it. Before publishing, add an in-app "Delete my data"
that deletes `users/{uid}/reads/*` and the Firebase Auth user. Not built yet.

### Rules for changing this later
- Adding a field: add it as optional, no migration.
- Adding a feature with new data (notes, plans): add a new subcollection, e.g. `users/{uid}/plans`. Don't bend `reads`.
- Wanting to track re-reads: add a `passes` subcollection. `reads` keeps meaning "have I read this at least once".
- Only bump `schemaVersion` for a change old data can't satisfy, and write a migration then.

## Room (on device)
Table `chapter_reads`: the same fields as the Firestore document, keyed by `id` (= chapterId), plus a local-only
`dirty` flag meaning "changed here, not yet uploaded". Room schemas are exported to `app/schemas/` and committed so every
future migration can be checked against them.

Preferences (`ReaderPrefs`) keep only UI state: chosen version and current book/chapter.
