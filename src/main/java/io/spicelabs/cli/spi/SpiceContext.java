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
  int API_VERSION = 5;

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
   * The configuration groups this plugin claimed, resolved.
   *
   * <p>Keyed by group name, so {@code configuration().get("analysis")} is that group's
   * settings in the group's own vocabulary. A plugin receives exactly the groups it named in
   * {@link SpiceCommandPlugin#configurationGroups()} — never the file, never another
   * command's settings, and never a group it did not ask for.
   *
   * <p>{@code spice} has already done the layering: for each claimed group it read the
   * shared {@code [group]} table, overlaid the command-scoped {@code [<command>.group]}
   * table, then any {@code SPICE_GROUP_KEY} environment variables, so a value written once
   * at the top level reaches every command that claims the group. What arrives here is the
   * result. Which layer supplied a given value is deliberately not part of this API: it is
   * reported by {@code spice} as it resolves, and printed by {@code spice config explain}.
   *
   * <p>{@code spice} still does not understand what any of it means. The values are in the
   * group's schema, which the component that owns the group parses and validates. That is
   * what keeps this command free of every plugin's flag list — the previous attempt at
   * cross-program configuration failed exactly there.
   *
   * <p>A group is usually a table of settings, and arrives as a {@code Map}. A group that
   * names a list of things rather than a set of settings — an array of tables — arrives as a
   * {@code List}, because there are no keys to layer: a later source replaces it whole or
   * leaves it alone. That is why the value type here is {@code Object} rather than a map.
   *
   * <p>Values are the TOML data model, exactly:
   * <ul>
   *   <li>{@link String}, {@link Long}, {@link Double}, {@link Boolean}</li>
   *   <li>{@link java.time.OffsetDateTime}, {@link java.time.LocalDateTime},
   *       {@link java.time.LocalDate}, {@link java.time.LocalTime}</li>
   *   <li>{@code List<Object>} for arrays, whose elements are again drawn from this list</li>
   *   <li>{@code Map<String, Object>} for sub-tables</li>
   * </ul>
   * with one exception: a value that came from an environment variable is a {@link String},
   * because the environment has no types and guessing one here would be worse than letting
   * the component that knows the schema coerce it.
   *
   * <p>Deliberately separate from {@link #passClaims()}: these settings are entirely
   * user-authored, whereas pass claims are properties of the credential the platform issued
   * and nothing a user writes may supply or override them. Keeping the two reachable by
   * different methods is what stops that distinction eroding.
   *
   * @return group name to that group's settings; never {@code null}, and empty for a plugin
   *     that claimed no groups or a run with no configuration file
   */
  default Map<String, Object> configuration() {
    return Map.of();
  }
}
