# Releasing spice-plugin-api

This repo publishes a single artifact: `io.spicelabs:spice-plugin-api`. It ships
on its own GitHub Release, tagged `v<x.y.z>`, independently of every consumer.

SNAPSHOT versions are published to GitHub Packages on every push to `main` (see
`.github/workflows/snapshot.yml`); releases go to GitHub Packages **and** Maven
Central (see `.github/workflows/publish.yml`).

| Channel | Trigger | Workflow |
|---|---|---|
| GitHub Packages (SNAPSHOT) | push to `main` | `snapshot.yml` |
| GitHub Packages (release) | GitHub Release `v<x.y.z>` | `publish.yml` |
| Maven Central (release) | GitHub Release `v<x.y.z>` | `publish.yml` |

In-repo, the version is a `-SNAPSHOT` (e.g. `1.0.0-SNAPSHOT`). A release takes
its version from the release tag and publishes a real (non-SNAPSHOT) artifact.

Pushing a bare `v<x.y.z>` git tag does **not** publish anything — the workflow
runs on `release: [published]`, so the release itself is what triggers it.

## Dependency order

`spice-plugin-api` ← `spice-bom` (depends on plugin-api as a normal
dependency) ← `spice-labs-cli` (imports the BOM). Release lower layers first.
Each release workflow **verifies** its inputs are already published
(`dependency:get`) and fails fast otherwise.

1. **plugin-api** — publish a GitHub Release tagged `v<x.y.z>` → `publish.yml`
   publishes it.
2. **BOM** (in `spice-labs-inc/spice-bom`) — the BOM depends on plugin-api as a
   normal dependency, so bumping plugin-api is a BOM **patch** bump (a managed
   version changed). Publish a GitHub Release tagged `v<x.y.z>` in the spice-bom
   repo → its `publish.yml` publishes the BOM.
3. **CLI** — publish a GitHub Release tagged `v<x.y.z>` → `publish.yml` verifies
   that `spice-bom` is published, pins the CLI to it, and deploys.

## Versioning

The SPI is SemVer'd on its Java API. Bump the `<version>`:

- **patch** — bug fix / no API change
- **minor** — additive API change
- **major** — breaking API change (also bump `SpiceContext.API_VERSION`)

`SpiceContext.API_VERSION` is the runtime contract version; `spice` refuses to
mount a plugin whose `apiVersion()` differs from its own.

## Cutting a release

1. Ensure the API is what you want and the `<version>` reflects the intended
   bump.
2. Publish a GitHub Release tagged `v<x.y.z>` against the tip of `main` →
   `publish.yml` publishes to GitHub Packages and Maven Central:

   ```bash
   gh release create v<x.y.z> --target main --generate-notes
   ```

After a release, bump the in-repo `-SNAPSHOT` to the next intended version.
