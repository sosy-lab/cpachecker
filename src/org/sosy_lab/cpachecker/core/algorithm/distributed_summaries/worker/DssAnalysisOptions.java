// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker;

import java.nio.file.Path;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;

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
      description =
          "If this option is set to true, the analysis will try to cover all violation conditions"
              + " in the block, even if no valid ARG path exists. In some cases,"
              + " ARGUtils#getAllPaths does not compute all paths as promised. However, setting"
              + " this option to true may lead to a significant increase in analysis time.",
      secure = true)
  private boolean forcefullyCollectAllViolationConditions = false;

  private final Configuration parentConfig;

  public DssAnalysisOptions(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
    parentConfig = pConfig;
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

  public boolean forcefullyCollectAllViolationConditions() {
    return forcefullyCollectAllViolationConditions;
  }

  public Path getForwardConfiguration() {
    return forwardConfiguration;
  }

  public Configuration getParentConfig() {
    return parentConfig;
  }

  public Path getLogDirectory() {
    return logDirectory;
  }
}
