// SPDX-License-Identifier: Apache-2.0
/* Copyright 2025 Spice Labs, Inc. & Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License. */

package io.spicelabs.cli.spi;

import java.util.Map;
import java.util.Optional;

/**
 * Shared services that {@code spice} passes to a {@link SpiceCommandPlugin} when it
 * builds its command, so plugins can behave consistently with built-in commands (same
 * version reporting, same {@code SPICE_PASS} resolution).
 *
 * <p>The surface is intentionally small and <strong>dependency-free</strong> (only
 * {@code java.*} types) so the {@code spice-plugin-api} artifact imposes nothing on
 * plugin authors and can be compiled in any context. It may grow over time (e.g. an
 * upload helper, output-directory conventions, a global {@code --json} flag); any
 * breaking change is signalled by bumping {@link #API_VERSION}.
 *
 * <p>Plugins that want logging should obtain their own logger (e.g. via SLF4J); the
 * context deliberately does not expose one, to keep this contract dependency-free.
 */
public interface SpiceContext {

  /**
   * The SPI contract version. Bumped when {@link SpiceCommandPlugin} or this interface
   * changes incompatibly; {@code spice} only mounts plugins whose
   * {@link SpiceCommandPlugin#apiVersion()} matches this value.
   */
  int API_VERSION = 3;

  /** The running {@code spice} CLI version (e.g. for plugin {@code --version} output). */
  String version();

  /**
   * The resolved Spice Pass (JWT) from the {@code SPICE_PASS} environment variable, or
   * empty if it is not set. This is the raw <em>credential</em>, for plugins that upload to
   * the platform; to find out what the pass <em>says</em>, use {@link #passClaims()} rather
   * than decoding it yourself.
   */
  Optional<String> spicePass();

  /**
   * The claims carried by this run's Spice Pass, decoded once by {@code spice}. A plugin
   * reads what it needs from here instead of parsing the pass or consulting system
   * properties, so built-in commands and plugins always agree on the values in force.
   *
   * @return the pass's claims; never {@code null}, and {@link SpicePassClaims#EMPTY} when
   *     there is no pass
   */
  default SpicePassClaims passClaims() {
    return SpicePassClaims.EMPTY;
  }

  /**
   * This plugin's section of the run's configuration file, already parsed.
   *
   * <p>{@code spice} reads one TOML file per run — named with {@code --config} or discovered
   * in the platform's standard configuration directory — parses it once, and hands each
   * plugin the table at its own command path. A plugin therefore never sees the file, never
   * learns where its table sits in the tree, and never parses TOML that is not its own. The
   * table is in the plugin's <em>own</em> schema, which {@code spice} carries without
   * understanding.
   *
   * <p>The table is a nested map, which is the TOML data model rather than a stand-in for it.
   * Values are exactly:
   * <ul>
   *   <li>{@link String}, {@link Long}, {@link Double}, {@link Boolean}</li>
   *   <li>{@link java.time.OffsetDateTime}, {@link java.time.LocalDateTime},
   *       {@link java.time.LocalDate}, {@link java.time.LocalTime}</li>
   *   <li>{@code List<Object>} for arrays, whose elements are again drawn from this list</li>
   *   <li>{@code Map<String, Object>} for sub-tables</li>
   * </ul>
   * so this API needs no TOML library and imposes none on plugin authors. A plugin that
   * already has one can adapt the map back to its own representation.
   *
   * <p>Deliberately separate from {@link #passClaims()}: the config table is entirely
   * user-authored, whereas pass claims are properties of the credential the platform issued
   * and nothing a user writes may supply or override them. Keeping the two reachable by
   * different methods is what stops that distinction eroding.
   *
   * @return this plugin's table, or an empty map when there is no config file or it has no
   *     table for this command; never {@code null}
   */
  default Map<String, Object> configuration() {
    return Map.of();
  }
}
