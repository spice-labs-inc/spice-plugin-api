# spice-plugin-api

Stable, dependency-free service-provider interface (SPI) for contributing
top-level `spice` subcommands as plugins.

A plugin implements `io.spicelabs.cli.spi.SpiceCommandPlugin`, registers it in
`META-INF/services/io.spicelabs.cli.spi.SpiceCommandPlugin`, and is discovered at
runtime by the `spice` CLI via `java.util.ServiceLoader`. `spice` has no
compile-time knowledge of any plugin — a plugin is mounted purely by being on
the classpath.

```xml
<dependency>
  <groupId>io.spicelabs</groupId>
  <artifactId>spice-plugin-api</artifactId>
  <version>1.0.0-SNAPSHOT</version> <!-- pin a released version for production -->
</dependency>
```

## Where it's published

| Channel | Version kind | How |
|---|---|---|
| GitHub Packages (`maven.pkg.github.com/spice-labs-inc/spice-plugin-api`) | `-SNAPSHOT` (every push to `main`) and releases (`v<x.y.z>`) | `publish.yml` (on tag) + `snapshot.yml` (on push) |
| Maven Central | releases only (`v<x.y.z>`) | `publish.yml` |

GitHub Packages requires auth even for public repos, so every consumer needs a
`~/.m2/settings.xml` carrying a `github` server entry with a
`read:packages`-scoped token (see [RELEASING.md](RELEASING.md)).

## Dependency order

`spice-plugin-api` ← `spice-bom` (depends on plugin-api transitively) ←
`spice-labs-cli` (imports the BOM). Release lower layers first. See
[RELEASING.md](RELEASING.md).

## Versioning

The SPI is SemVer'd on its Java API. Bump the `<version>`:

- **patch** — bug fix / no API change
- **minor** — additive API change
- **major** — breaking API change (also bump `SpiceContext.API_VERSION`)

`SpiceContext.API_VERSION` is the runtime contract version; `spice` refuses to
mount a plugin whose `apiVersion()` differs from its own.
