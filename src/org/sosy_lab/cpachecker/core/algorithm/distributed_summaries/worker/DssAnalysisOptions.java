// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import java.nio.file.Path;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis.DssBlockAnalysisType;

@Options(prefix = "distributedSummaries")
public class DssAnalysisOptions {

  @Option(
      name = "logging.reportFiles",
      description = "output file for visualizing message exchange")
  @FileOption(Type.OUTPUT_DIRECTORY)
  private Path reportFiles = Path.of("block_analysis/messages");

  @Option(
      name = "logging.blockCFAFile",
      description = "output file for visualizing the block graph")
  @FileOption(Type.OUTPUT_FILE)
  private Path blockCFAFile = Path.of("block_analysis/blocks.json");

  @Option(
      name = "debug",
      description =
          "Whether to enable debug mode of block-summary analysis. This creates visual output for"
              + " debugging and exports additional metadata.Creating this information consumes"
              + " resources and should not be used for benchmarks.",
      secure = true)
  private boolean debug = false;

  @Option(
      name = "worker.forwardConfiguration",
      description = "Configuration for forward analysis in computation of distributed summaries",
      secure = true)
  @FileOption(Type.OPTIONAL_INPUT_FILE)
  private Path forwardConfiguration =
      Path.of("config/distributed-summary-synthesis/dss-block-analysis.properties");

  @Option(
      name = "worker.logDirectory",
      description =
          "Destination directory for the logfiles of all DssWorkers. The logfiles have the"
              + " same name as the ID of the worker.",
      secure = true)
  @FileOption(Type.OUTPUT_DIRECTORY)
  private Path logDirectory = Path.of("block_analysis/logfiles");

  @Option(
      name = "debug.readableFormulas",
      description =
          "Whether the messages of a debug run carry every predicate formula a second time in the"
              + " notation of the solver. That notation is not the one the message is built from,"
              + " so it has to be rendered separately, which costs more than the whole rest of a"
              + " block analysis. Has no effect unless debug mode is enabled.",
      secure = true)
  private boolean readableFormulas = false;

  @Option(
      name = "worker.logLevel",
      description =
          "Level of the per-worker logfiles. The block analyses log their SMT formulas at ALL, and"
              + " rendering a formula as a string is expensive enough to dominate the runtime of a"
              + " block analysis, so set this to ALL only when those formulas are what you are"
              + " looking for.",
      secure = true)
  private Level logLevel = Level.FINE;

  @Option(
      description =
          "Whether to reset callstack state before running a block analysis; usually used together "
              + "with the inlining decomposition",
      secure = true)
  private boolean resetCallstackState = false;

  @Option(
      name = "syntacticVcEquality",
      description =
          "Whether to decide equality of violation conditions by their representation instead of"
              + " asking the solver. A violation condition is built from the edges of a path, so"
              + " the same path yields the same formula. Deciding it this way spares an implication"
              + " query per compared pair, but tells equivalent conditions that are written"
              + " differently apart.",
      secure = true)
  private boolean syntacticViolationConditionEquality = false;

  @Option(
      name = "combineVcsByHash",
      description = "Whether to combine violation conditions at same program location",
      secure = true)
  private boolean combineViolationConditionsByHash = true;

  @Option(
      secure = true,
      description = "Whether to combine incoming states and outgoing summaries by exact union.")
  private boolean combineStates = true;

  @Option(
      secure = true,
      description =
          "Whether to send and use precision updates in precondition and violation-condition"
              + " messages.")
  private boolean sharePrecision = true;

  @Option(
      secure = true,
      description = "Whether to cache violation conditions normalized for comparison.")
  private boolean cacheViolationConditions = true;

  @Option(
      secure = true,
      description =
          "Whether a block without successors stops exploring after its first run. Such a block"
              + " never receives a violation condition, and its first run, from the unconstrained"
              + " start state, already finds every violation it can report. A block that iterates"
              + " its own loop is excluded, because it covers later iterations.")
  private boolean retireTerminalBlocks = false;

  @Option(
      secure = true,
      description =
          "Whether a block analyzed with partial replacement publishes the violations it finds from"
              + " the unconstrained start state while it already knows a real precondition. Such"
              + " violations need not be reachable, and the violation conditions they cause keep"
              + " growing around loops.")
  private boolean publishSpeculativeViolations = true;

  @Option(
      secure = true,
      description =
          "Let the analysis of a block that is a loop over its own head iterate the loop itself,"
              + " instead of sending itself one violation condition per iteration. The root block"
              + " then publishes the postcondition of its first run, because a loop block that"
              + " covers its later iterations only finds the violations reachable from its real"
              + " preconditions.")
  private boolean iterateLoopBlocks = false;

  @Option(
      secure = true,
      description =
          "Whether a block analyzed with partial replacement keeps what its own refinements learned"
              + " for all later explorations, like the predicate analysis of the whole program"
              + " does. Otherwise it only explores with the precisions of the received messages.")
  private boolean retainLearnedPrecision = false;

  @Option(
      secure = true,
      description = "Whether to dictionary-encode repeated text in serialized messages.")
  private boolean compressMessages = true;

  @Option(
      secure = true,
      description = "Whether to add the block entry as an explicit predicate abstraction location.")
  private boolean abstractAtBlockEntry = true;

  // TODO How to make sure the other Witness export does not overwrite this?
  @Option(
      secure = true,
      name = "yamlProofWitness",
      description =
          "The path to which the different "
              + "versions of the correctness witnesses will be exported. "
              + "Each witness version replaces the string '%s' "
              + "with its version number.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private PathTemplate yamlWitnessOutputFileTemplate =
      PathTemplate.ofFormatString("witness-dss-%s.yml");

  @Option(
      description = "Which block analysis to use for the distributed summaries algorithm",
      secure = true)
  private DssBlockAnalysisType blockAnalysisType = DssBlockAnalysisType.ALWAYS_REPLACE;

  public DssAnalysisOptions(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
  }

  public Path getBlockCFAFile() {
    return blockCFAFile;
  }

  public Path getReportFiles() {
    return reportFiles;
  }

  public boolean isDebugModeEnabled() {
    return debug;
  }

  public Path getForwardConfiguration() {
    return forwardConfiguration;
  }

  public Path getLogDirectory() {
    return logDirectory;
  }

  public Level getLogLevel() {
    return logLevel;
  }

  /** Whether serialized predicate states carry a solver-rendered copy of their formula. */
  public boolean writeReadableFormulas() {
    return debug && readableFormulas;
  }

  public boolean abstractAtBlockEntry() {
    return abstractAtBlockEntry;
  }

  public boolean combineStates() {
    return combineStates;
  }

  public boolean sharePrecision() {
    return sharePrecision;
  }

  public boolean cacheViolationConditions() {
    return cacheViolationConditions;
  }

  public boolean retireTerminalBlocks() {
    return retireTerminalBlocks;
  }

  public boolean publishSpeculativeViolations() {
    return publishSpeculativeViolations;
  }

  public boolean iterateLoopBlocks() {
    return iterateLoopBlocks;
  }

  public boolean retainLearnedPrecision() {
    return retainLearnedPrecision;
  }

  public boolean compressMessages() {
    return compressMessages;
  }

  public boolean combineViolationConditionsByHash() {
    return combineViolationConditionsByHash;
  }

  public boolean useSyntacticViolationConditionEquality() {
    return syntacticViolationConditionEquality;
  }

  public PathTemplate getYamlCorrectnessWitnessOutputFileTemplate() {
    return yamlWitnessOutputFileTemplate;
  }

  public DssBlockAnalysisType getBlockAnalysisType() {
    return blockAnalysisType;
  }

  public boolean callStackStateRequiresStateReset() {
    return resetCallstackState;
  }
}
