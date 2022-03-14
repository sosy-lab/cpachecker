// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg.counterexamples;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathTemplate;

@Options(prefix = "counterexample.export", deprecatedPrefix = "cpa.arg.errorPath")
public final class CEXExportOptions {

  @Option(
      secure = true,
      name = "enabled",
      deprecatedName = "export",
      description = "export counterexample to file, if one is found")
  private boolean exportErrorPath = true;

  @Option(secure = true, name = "file", description = "export counterexample as text file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate errorPathFile = PathTemplate.ofFormatString("Counterexample.%d.txt");

  @Option(secure = true, name = "core", description = "export counterexample core as text file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate errorPathCoreFile =
      PathTemplate.ofFormatString("Counterexample.%d.core.txt");

  @Option(secure = true, name = "source", description = "export counterexample as source file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate errorPathSourceFile = PathTemplate.ofFormatString("Counterexample.%d.c");

  @Option(
      secure = true,
      name = "exportAsSource",
      description = "export counterexample as source file")
  private boolean exportSource = true;

  @Option(
      secure = true,
      name = "graph",
      description = "export counterexample as Dot/Graphviz visualization")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate errorPathDotFile = PathTemplate.ofFormatString("Counterexample.%d.dot");

  @Option(secure = true, name = "automaton", description = "export counterexample as automaton")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate errorPathAutomatonFile =
      PathTemplate.ofFormatString("Counterexample.%d.spc");

  @Option(
      secure = true,
      name = "prefixCoverageFile",
      description =
          "export counterexample coverage information, considering only spec prefix as "
              + "covered (up until reaching __FALSE state in Assumption Automaton).")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  PathTemplate coveragePrefixTemplate =
      PathTemplate.ofFormatString("Counterexample.%d.aa-prefix.coverage-info");

  @Option(
      secure = true,
      name = "exportCounterexampleCoverage",
      description =
          "export coverage information for every witness: "
              + "requires using an Assumption Automaton as part of the specification. "
              + "Lines are considered to be covered only when the path reaching "
              + "the statement does not reach the __FALSE state in the Assumption Automaton.")
  private boolean exportCounterexampleCoverage = false;

  @Option(
      secure = true,
      name = "exportWitness",
      description = "export counterexample as witness/graphml file")
  private boolean exportWitness = true;

  @Option(
      secure = true,
      name = "graphml",
      description = "export counterexample witness as GraphML automaton")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate errorPathWitnessFile =
      PathTemplate.ofFormatString("Counterexample.%d.graphml");

  @Option(
      secure = true,
      name = "witnessGraph",
      description = "export counterexample witness as Dot/Graphviz visualization")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate errorPathWitnessDotFile =
      PathTemplate.ofFormatString("Counterexample.%d.witness.dot");

  @Option(secure = true, description = "Export extended witness in addition to regular witness")
  private boolean exportExtendedWitness = false;

  @Option(secure = true, description = "Extended witness with specific analysis information file")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate extendedWitnessFile =
      PathTemplate.ofFormatString("extendedWitness.%d.graphml");

  @Option(secure = true, name = "exportHarness", description = "export test harness")
  private boolean exportHarness = false;

  @Option(secure = true, name = "harness", description = "export test harness to file as code")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate testHarnessFile = PathTemplate.ofFormatString("Counterexample.%d.harness.c");

  @Option(
      secure = true,
      name = "exportTestCase",
      description =
          "export test case that represents the counterexample. Further options can be"
              + " set with options 'testcase.*'")
  private boolean exportTest = false;

  @Option(
      secure = true,
      name = "exportAllFoundErrorPaths",
      description =
          "export error paths to files immediately after they were found, "
              + "including spurious error-paths before executing a refinement. "
              + "Note that we do not track already exported error-paths and export them "
              + "at every refinement as long as they are not removed from the reached-set. "
              + "Most helpful for debugging refinements.")
  private boolean dumpAllFoundErrorPaths = false;

  @Option(
      secure = true,
      name = "exportImmediately",
      description = "export error paths to files immediately after they were found")
  private boolean dumpErrorPathImmediately = false;

  public CEXExportOptions(Configuration config) throws InvalidConfigurationException {
    config.inject(this);
  }

  public boolean disabledCompletely() {
    return getAutomatonFile() == null
        && getCoreFile() == null
        && getCoveragePrefix() == null
        && getErrorPathFile() == null
        && getDotFile() == null
        && getSourceFile() == null
        && getTestHarnessFile() == null
        && getWitnessFile() == null
        && getExtendedWitnessFile() == null
        && !exportTest;
  }

  @Nullable PathTemplate getAutomatonFile() {
    if (!exportErrorPath) {
      return null;
    }
    return errorPathAutomatonFile;
  }

  @Nullable PathTemplate getCoreFile() {
    if (!exportErrorPath) {
      return null;
    }
    return errorPathCoreFile;
  }

  @Nullable PathTemplate getCoveragePrefix() {
    if (!exportErrorPath) {
      return null;
    }
    return exportCounterexampleCoverage ? coveragePrefixTemplate : null;
  }

  @Nullable PathTemplate getErrorPathFile() {
    if (!exportErrorPath) {
      return null;
    }
    return errorPathFile;
  }

  @Nullable PathTemplate getDotFile() {
    if (!exportErrorPath) {
      return null;
    }
    return errorPathDotFile;
  }

  @Nullable PathTemplate getSourceFile() {
    if (!exportErrorPath) {
      return null;
    }
    return exportSource ? errorPathSourceFile : null;
  }

  @Nullable PathTemplate getTestHarnessFile() {
    if (!exportErrorPath) {
      return null;
    }
    return exportHarness ? testHarnessFile : null;
  }

  @Nullable PathTemplate getWitnessFile() {
    if (!exportErrorPath) {
      return null;
    }
    return exportWitness ? errorPathWitnessFile : null;
  }

  @Nullable PathTemplate getWitnessDotFile() {
    if (!exportErrorPath) {
      return null;
    }
    return exportWitness ? errorPathWitnessDotFile : null;
  }

  @Nullable PathTemplate getExtendedWitnessFile() {
    if (!exportErrorPath) {
      return null;
    }
    return exportExtendedWitness ? extendedWitnessFile : null;
  }

  boolean exportToTest() {
    return exportTest;
  }

  public boolean dumpAllFoundErrorPaths() {
    return dumpAllFoundErrorPaths;
  }

  /** export error paths to files immediately after they were found, or after the whole analysis. */
  public boolean dumpErrorPathImmediately() {
    return dumpErrorPathImmediately;
  }
}
