# Agent notes for spice-plugin-api

## Tests shared with Surveyor

[Surveyor](https://github.com/spice-labs-inc/surveyor) assembles the `spice` CLI and runs
black-box integration tests against it that reuse **this repository's own test data**. It
does not copy the data into its tree: `build-surveyor expectations` reads `test-fixtures.json`
at the root of this repository, at the exact commit the shipped jar was built from (the
commit recorded in `META-INF/git/<artifactId>.properties`), and exports what that file
declares. Every unit test here and every Surveyor case derived from it share one ID:

```
spice-plugin-api/<qualified suite or class>#<test name>     code tests
spice-plugin-api/<data path>[#<case id>]                    data-driven cases: the file's `id` field
```

Surveyor's `scripts/compare-test-ids.py` diffs those IDs against `tests/coverage/spice-plugin-api.tsv`
and fails its CI on orphans, so keep these rules when you write or change tests:

- **Put expectations in data, not in code.** A new fixture-driven expectation belongs in a
  data file the unit test reads (see below for this repository's format); Surveyor can then
  evaluate the same file through `spice` without a second copy.
- **Every data file gets an `id`**, derived from its path exactly as the existing files do,
  and the unit test that reads it must require it. Never hand-pick ids.
- **Declare new data in `test-fixtures.json`**: an `expectations[]` entry (glob, format,
  `idField`, how the fixture path derives) and a `fixtures[]` entry with a tier (0 committed
  and small, 1 downloadable for a nightly run, 2 full corpora). Undeclared data is invisible
  to Surveyor.
- **Keep tier-0 fixtures small** (kilobytes to a few megabytes); anything large is a download
  (`downloads[]` with a sha256) or LFS, at tier 1 or 2.
- **Renaming a test or data file changes its ID.** That is allowed, but Surveyor's coverage
  manifest will report an orphan; mention it in the PR so the manifest is regenerated.
- **Keep the provenance plugin.** The jar must record its commit in
  `META-INF/git/<artifactId>.properties`; without it Surveyor falls back to the release tag.

### Here

- No tests yet. If any are added, list their source root under `tests[]` in
  `test-fixtures.json`. The jar records its commit in `META-INF/git/spice-plugin-api.properties`.
- This SPI says what a plugin cannot work out for itself. A capability is not that: whether a
  plugin's command exists is already answered by whether the plugin is on the classpath, and a
  plugin can see which of the tools it drives are installed. Resist adding capability flags.
