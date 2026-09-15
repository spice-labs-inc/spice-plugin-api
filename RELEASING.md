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

This artifact has no dependencies of its own, so nothing needs releasing before
it. Consumers pin it directly by version — `spice-labs-cli` and `allspice` each
in a `spice-plugin-api.version` property — so the order is: release this, wait
for it to appear on Maven Central, then bump those pins.

`spice-bom` used to sit in between, and this page used to describe releasing it
as a middle step. The BOM was retired and every component pins its versions
inline, so there is no middle step; nothing imports `spice-bom` any more. Neither
this workflow nor the CLI's verifies its inputs with `dependency:get`.

## Versioning

The SPI is SemVer'd on its Java API. The release tag carries the version, so this
is a decision about the tag rather than about anything in the repository:

- **patch** — bug fix / no API change
- **minor** — additive API change
- **major** — breaking API change (also bump `SpiceContext.API_VERSION`)

`SpiceContext.API_VERSION` is the runtime contract version; `spice` refuses to
mount a plugin whose `apiVersion()` differs from its own.

## Cutting a release

1. Ensure `main` holds the API you want. The `<version>` in `pom.xml` is *not*
   what ships: `publish.yml` sets the version from the tag.
2. Check the number is a natural successor to the last release. The workflow runs
   `.github/scripts/check-release-tag.py` for you and you can run it first:

   ```bash
   git fetch --tags && git tag --list > /tmp/tags.txt
   python3 .github/scripts/check-release-tag.py v<x.y.z> /tmp/tags.txt
   ```

   A tag that skips versions still releases, but `auto_publish` comes out false
   and the deployment waits in the Central Portal for someone to publish or drop
   it, because a published Central version can never be replaced.
3. Publish a GitHub Release tagged `v<x.y.z>` against the tip of `main` →
   `publish.yml` publishes to GitHub Packages and Maven Central:

   ```bash
   gh release create v<x.y.z> --target main --generate-notes
   ```

## The in-repo version

`pom.xml` has said `1.0.0-SNAPSHOT` since before 2.0.0, and nothing bumps it,
because releases take their version from the tag. `snapshot.yml` does not
override it either, so every push to `main` publishes a `1.0.0-SNAPSHOT` to
GitHub Packages carrying whatever the API is now — two majors ahead of its own
name. Treat a snapshot's version as meaningless and depend on a release. Bumping
the property after each release would fix it, if anyone wants to.

## If a release fails part-way

`publish.yml` deploys to GitHub Packages first and Maven Central second, and
GitHub Packages refuses to overwrite a release version. A run that uploads the
POM and then stops — cancelled, or failing later — therefore leaves that version
**present in GitHub Packages and absent from Central**. Re-running it returns
`409 Conflict` on the POM and never reaches Central.

Two ways out:

- Delete the version from GitHub Packages (org admin only) and re-run the
  release; or
- burn the number and release the next patch instead. Nothing can be depending on
  the half-published version, because it never reached Central.

This happened to 2.1.0 on 15 September 2026, which is why `SpiceContext.airgapped()`
shipped as 2.1.1.
