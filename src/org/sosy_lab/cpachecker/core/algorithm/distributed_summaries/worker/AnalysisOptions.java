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
public class AnalysisOptions {

  @Option(description = "forces the precondition of fault localization workers to be true")
  private boolean flPreconditionAlwaysTrue = false;

  @Option(description = "whether analysis worker store circular post conditions")
  private boolean storeCircularPostConditions = false;

  @Option(description = "whether error conditions are always checked for unsatisfiability")
  private boolean checkEveryErrorCondition = true;

  @Option(description = "loop free programs do not require to deny all possible error messages")
  private boolean sendEveryErrorMessage = false;

  @Option(
      description = "Configuration for forward analysis in computation of distributed summaries")
  @FileOption(Type.OPTIONAL_INPUT_FILE)
  private Path forwardConfiguration =
      Path.of("config/distributed-block-summaries/predicateAnalysis-block-forward.properties");

  @Option(
      description = "Configuration for backward analysis in computation of distributed summaries")
  @FileOption(Type.OPTIONAL_INPUT_FILE)
  private Path backwardConfiguration =
      Path.of("config/distributed-block-summaries/predicateAnalysis-block-backward.properties");

  private final Configuration parentConfig;

  public AnalysisOptions(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
    parentConfig = pConfig;
  }

  public boolean isFlPreconditionAlwaysTrue() {
    return flPreconditionAlwaysTrue;
  }

  public boolean storeCircularPostConditions() {
    return storeCircularPostConditions;
  }

  public boolean checkEveryErrorConditionForUnsatisfiability() {
    return checkEveryErrorCondition;
  }

  public boolean sendEveryErrorMessage() {
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
}
