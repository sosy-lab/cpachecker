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

@Options(prefix = "distributedSummaries.worker")
public class BlockSummaryAnalysisOptions {

  @Option(
      description = "Configuration for forward analysis in computation of distributed summaries")
  @FileOption(Type.OPTIONAL_INPUT_FILE)
  private Path forwardConfiguration =
      Path.of("config/distributed-block-summaries/predicateAnalysis-block-forward.properties");

  @Option(
      description =
          "Destination directory for the logfiles of all BlockSummaryWorkers. The logfiles have the"
              + " same name as the ID of the worker.")
  @FileOption(Type.OUTPUT_DIRECTORY)
  private Path logDirectory = Path.of("block_summary/logfiles");

  private final Configuration parentConfig;

  public BlockSummaryAnalysisOptions(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
    parentConfig = pConfig;
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
