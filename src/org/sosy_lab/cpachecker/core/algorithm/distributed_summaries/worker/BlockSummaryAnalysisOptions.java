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

  enum SerializationType {
    IDENTITY,
    BYTE
  }

  @Option(description = "whether error conditions are always checked for unsatisfiability")
  private boolean checkEveryErrorCondition = true;

  @Option(
      description =
          "Whether loop free programs have to deny all possible error messages. Enable this option"
              + " to eagerly process every possible error message that occurs after an precondition"
              + " update.")
  private boolean sendEveryErrorMessage = false;

  @Option(
      secure = true,
      description = "Configuration for forward analysis in computation of distributed summaries")
  @FileOption(Type.OPTIONAL_INPUT_FILE)
  private Path forwardConfiguration =
      Path.of("config/distributed-block-summaries/predicateAnalysis-block-forward.properties");

  @Option(
      secure = true,
      description = "Configuration for backward analysis in computation of distributed summaries")
  @FileOption(Type.OPTIONAL_INPUT_FILE)
  private Path backwardConfiguration =
      Path.of("config/distributed-block-summaries/predicateAnalysis-block-backward.properties");

  @Option(
      description =
          "Destination directory for the logfiles of all BlockSummaryWorkers. The logfiles have the"
              + " same name as the ID of the worker.")
  @FileOption(Type.OUTPUT_DIRECTORY)
  private Path logDirectory = Path.of("block_summary/logfiles");

  @Option(
      description =
          "Specifies which serialization type should be used. Possible values are IDENTITY to pass"
              + " the actual abstract state objects and BYTE to serialize the abstract state to a"
              + " byte string.")
  private SerializationType serializationType = SerializationType.BYTE;

  private final Configuration parentConfig;

  public BlockSummaryAnalysisOptions(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
    parentConfig = pConfig;
  }

  public boolean shouldCheckEveryErrorConditionForUnsatisfiability() {
    return checkEveryErrorCondition;
  }

  public boolean shouldSendEveryErrorMessage() {
    return sendEveryErrorMessage;
  }

  public Path getBackwardConfiguration() {
    return backwardConfiguration;
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

  public SerializationType getSerializationType() {
    return serializationType;
  }
}
