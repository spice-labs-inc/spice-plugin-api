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

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The claims carried by the run's Spice Pass, decoded once by {@code spice} and handed to
 * every plugin — so a plugin never parses a JWT to find out what its pass says, and built-in
 * commands and plugins cannot disagree about the values in force.
 *
 * <p><strong>Why this exists.</strong> These values used to travel as system properties that
 * {@code spice} set on itself before dispatch (plugins load in-process via
 * {@code ServiceLoader}, and a JVM cannot set an environment variable for itself). That made
 * the contract invisible — nothing declared which properties existed, and anyone could set
 * one from the command line and thereby widen a scope the platform had deliberately
 * narrowed. Claims come from the pass and from nowhere else: no flag, no property, no config
 * file supplies them, and the only way to change one is to hold a different pass.
 *
 * <p><strong>The typed/map split follows RFC 7519.</strong> The <em>registered</em> claims
 * ({@code iss}, {@code sub}, {@code aud}, {@code exp}, {@code nbf}, {@code iat},
 * {@code jti}) have semantics owned by the JWT standard and will never change, so they get
 * typed accessors. Everything else — the Spice-specific {@code x-*} claims, and whatever the
 * platform mints next — arrives in {@link #additionalClaims()}, reachable without an API
 * bump. This artifact stays dependency-free ({@code java.*} types only) either way.
 *
 * <p>Every accessor is empty when the pass omits the claim, or when there is no pass at all.
 * A plugin that genuinely requires a pass should report that itself.
 */
public interface SpicePassClaims {

  /** Claims of a run with no pass, for defaults and tests: every accessor is empty. */
  SpicePassClaims EMPTY = new SpicePassClaims() {};

  // ── Registered claims (RFC 7519 §4.1) ────────────────────────────────────────────────

  /** The {@code iss} claim: who issued the pass. */
  default Optional<String> issuer() {
    return Optional.empty();
  }

  /** The {@code sub} claim: who the pass was issued to. */
  default Optional<String> subject() {
    return Optional.empty();
  }

  /** The {@code aud} claim: the pass's intended audience; empty when absent. */
  default List<String> audience() {
    return List.of();
  }

  /** The {@code exp} claim: when the pass itself expires. */
  default Optional<Instant> expiresAt() {
    return Optional.empty();
  }

  /** The {@code nbf} claim: the instant before which the pass is not valid. */
  default Optional<Instant> notBefore() {
    return Optional.empty();
  }

  /** The {@code iat} claim: when the pass was issued. */
  default Optional<Instant> issuedAt() {
    return Optional.empty();
  }

  /** The {@code jti} claim: the pass's unique identifier. */
  default Optional<String> jwtId() {
    return Optional.empty();
  }

  // ── Everything else ──────────────────────────────────────────────────────────────────

  /**
   * Every claim that is not one of the registered claims above, verbatim.
   *
   * <p>Values are the JSON data model in {@code java.*} types, exactly:
   * <ul>
   *   <li>{@link String}, {@link Long}, {@link Double}, {@link Boolean}</li>
   *   <li>{@code List<Object>} for arrays, whose elements are again drawn from this
   *       list</li>
   *   <li>{@code Map<String, Object>} for nested objects</li>
   * </ul>
   * Integral numbers are always {@link Long}, never {@link Integer}. There is no
   * per-claim-name conversion: this map is a faithful transcript of the pass, and
   * interpretation belongs to the consumer. In particular, NumericDate-style claims — such
   * as {@code x-cutoff}, the artifact cutoff constraining what the platform will accept —
   * are epoch-second {@code Long}s, as the pass encodes them.
   *
   * @return the pass's non-registered claims, or an empty map when there is no pass; never
   *     {@code null}
   */
  default Map<String, Object> additionalClaims() {
    return Map.of();
  }
}
