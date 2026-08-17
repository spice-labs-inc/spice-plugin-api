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

/**
 * Service-provider interface for contributing a top-level {@code spice} subcommand.
 *
 * <p>Implementations are discovered at runtime via {@link java.util.ServiceLoader};
 * register the implementation in
 * {@code META-INF/services/io.spicelabs.cli.spi.SpiceCommandPlugin}. A plugin is
 * included in a {@code spice} distribution purely by being present on the classpath —
 * {@code spice} has no compile-time knowledge of any plugin.
 *
 * <p>The plugin <em>fully defines</em> its subcommand (name, description, options,
 * parameters, nested subcommands, and execution) through the object returned by
 * {@link #command(SpiceContext)}; {@code spice} only discovers it and mounts it.
 *
 * <p>This interface is published as the {@code io.spicelabs:spice-plugin-api} artifact
 * so that third parties can author plugins against a stable, dependency-free contract.
 */
public interface SpiceCommandPlugin {

  /**
   * Build the subcommand to mount under {@code spice}.
   *
   * <p>The returned value may be any picocli command:
   * <ul>
   *   <li>a {@code @picocli.CommandLine.Command}-annotated object that is
   *       {@link Runnable} or {@link java.util.concurrent.Callable},</li>
   *   <li>a {@code picocli.CommandLine} instance, or</li>
   *   <li>a {@code picocli.CommandLine.Model.CommandSpec}.</li>
   * </ul>
   * picocli reads the command name, description, options, parameters and nested
   * subcommands from it, so {@code spice} needs no compile-time knowledge of the
   * returned type. The return type is declared as {@link Object} precisely so this
   * API artifact carries no dependency on picocli.
   *
   * @param context shared services (CLI version, resolved {@code SPICE_PASS}) so a
   *                plugin can behave consistently with built-in commands
   * @return the picocli command definition; returning {@code null} skips the plugin
   */
  Object command(SpiceContext context);

  /**
   * A stable identifier for this plugin, used for ordering and for diagnostics when a
   * plugin is skipped (load failure, name clash, incompatible API version). Should be
   * unique and human-recognizable, e.g. {@code "allspice-registry"}.
   */
  String id();

  /**
   * The SPI contract version this plugin was built against. {@code spice} refuses to
   * mount a plugin whose version differs from {@link SpiceContext#API_VERSION}, rather
   * than risk a {@link LinkageError} at command-execution time.
   */
  default int apiVersion() {
    return SpiceContext.API_VERSION;
  }

  /**
   * Parent command name under which this plugin should be mounted, or an empty string
   * to mount as a top-level subcommand directly under {@code spice}.
   *
   * <p>For example, returning {@code "survey"} mounts the plugin's command as
   * {@code spice survey <command-name>}. Returning {@code ""} mounts it as
   * {@code spice <command-name>}.
   */
  default String parent() {
    return "";
  }

  /**
   * The configuration groups this plugin reads.
   *
   * <p>A group is a section of the user's configuration file named for a job rather than for
   * whoever does it — {@code analysis}, {@code upload}, {@code pipeline}. Usually a table of
   * settings; sometimes a list of things, such as an array of repositories.
   *
   * <p><strong>A group may not share a name with a command.</strong> At the root of a file a
   * table is either a group or a command's scope, so {@code [registry.analysis]} can only have
   * one reading if nothing called {@code registry} is also a group. Groups are
   * <em>shared</em>: a setting written once under {@code [analysis]} reaches every command
   * that claims {@code analysis}, whether built-in or from a plugin, so a user configures
   * the job and not each component that happens to perform part of it.
   *
   * <p>Claiming is what makes that safe. {@code spice} hands a plugin exactly the groups it
   * names here and nothing else, so a plugin cannot read settings intended for another, and
   * a table no command claims can be reported as a probable typo rather than silently doing
   * nothing. Several commands claiming the same group is the normal case — that overlap is
   * the sharing mechanism, not a conflict.
   *
   * <p>Within a claimed group {@code g}, {@code spice} resolves the shared {@code [g]} table
   * overlaid by a {@code [<command path>.g]} table, so a user can set a value globally and
   * override it for one command. The plugin receives the result and never learns which
   * layer supplied what.
   *
   * <p>Returning an empty list — the default — means this plugin reads no configuration
   * file settings at all, and {@link SpiceContext#configuration()} will be empty.
   *
   * @return the group names this plugin reads, e.g. {@code List.of("analysis", "pipeline")}
   */
  default java.util.List<String> configurationGroups() {
    return java.util.List.of();
  }

  /**
   * The keys, among this plugin's claimed groups, whose values name filesystem paths.
   *
   * <p>Dotted, group first: {@code "pipeline.staging_dir"}, {@code "paths.report_cli"}.
   *
   * <p>This exists because of Docker. The {@code spice} wrapper runs on the host and mounts
   * the paths it can see — which, until now, meant the paths named on the command line,
   * because that is all a shell script can read without understanding configuration. A path
   * written in a config file was invisible to it, so a run would either fail or, worse,
   * write inside the container and lose the result when it exited.
   *
   * <p>Declaring them here lets the host resolve those values and mount them, without the
   * wrapper parsing TOML and without anybody maintaining a second copy of a schema they do
   * not own. A plugin that names no paths in its configuration returns nothing, which is the
   * default.
   *
   * <p>A key that is absent from the user's configuration is simply skipped — this is a
   * statement about which keys <em>would</em> be paths, not a claim that they are set.
   *
   * @return dotted {@code group.key} names, e.g. {@code List.of("pipeline.staging_dir")}
   */
  default java.util.List<String> configurationPathKeys() {
    return java.util.List.of();
  }

  /**
   * A PowerShell tab-completion fragment contributing completions for this plugin's
   * command tree, or an empty string for none (the default).
   *
   * <p>Why only PowerShell: {@code spice} generates bash/zsh completion automatically
   * from the live picocli command model (which already includes this plugin's command),
   * so no fragment is needed there. picocli cannot generate PowerShell, so plugins supply
   * that here; {@code spice} splices every plugin's fragment into the PowerShell
   * completion script it emits.
   *
   * <p>The fragment runs after {@code spice} has defined a {@code $SpiceCompletions}
   * hashtable (pre-populated with the built-in commands) and before the argument
   * completer is registered. It must add one entry keyed by the plugin's top-level
   * command name, whose value maps {@code __sub} to the subcommand names and each
   * subcommand name to its option/flag list, e.g.:
   *
   * <pre>{@code
   * $SpiceCompletions['registry'] = @{
   *   __sub    = @('discover', 'run', 'status')
   *   discover = @('--config', '--output', '--max', '--json')
   *   run      = @('--config', '--discovery', '--json')
   *   status   = @('--config', '--json')
   * }
   * }</pre>
   */
  default String powershellCompletion() {
    return "";
  }
}
