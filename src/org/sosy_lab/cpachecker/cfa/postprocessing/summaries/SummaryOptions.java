// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.summaries;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.postprocessing.summaries.StrategyDependencies.StrategyDependencyEnum;

@Options
public class SummaryOptions {

  @Option(
      name = "cfa.summaries.strategies",
      secure = true,
      description =
          "Strategies to be used in the generation of the CFA, to summarize some parts of it.")
  private Set<StrategiesEnum> strategies =
      new HashSet<>(
          Arrays.asList(
              StrategiesEnum.LOOPCONSTANTEXTRAPOLATION,
              StrategiesEnum.NONDETBOUNDCONSTANTEXTRAPOLATION,
              StrategiesEnum.NAIVELOOPACCELERATION,
              StrategiesEnum.HAVOCSTRATEGY,
              StrategiesEnum.LOOPUNROLLING));

  @Option(
      secure = true,
      name = "cfa.summaries.maxUnrollingsStrategy",
      description = "Max amount fo Unrollings for the Unrolling Strategy")
  private int maxUnrollingsStrategy = 10;

  @Option(
      secure = true,
      name = "cfa.summaries.maxIterations",
      description = "Max amount fo Iterations for adapting the CFA")
  private int maxIterationsSummaries = 10;

  @Option(
      secure = true,
      name = "cfa.summaries.dependencies",
      description = "Dependencies between the Different Strategies")
  private StrategyDependencyEnum cfaCreationStrategy =
      StrategyDependencyEnum.BASESTRATEGYDEPENDENCY;

  @Option(
      secure = true,
      name = "summaries.transfer",
      description = "Dependencies between the Different Strategies")
  private StrategyDependencyEnum transferStrategy = StrategyDependencyEnum.BASESTRATEGYDEPENDENCY;

  public SummaryOptions(Configuration pConfig) throws InvalidConfigurationException {
    pConfig.inject(this);
  }

  public Set<StrategiesEnum> getStrategies() {
    return strategies;
  }

  public int getMaxUnrollingsStrategy() {
    return maxUnrollingsStrategy;
  }

  public int getMaxIterationsSummaries() {
    return maxIterationsSummaries;
  }

  public StrategyDependencyEnum getCfaCreationStrategy() {
    return cfaCreationStrategy;
  }

  public StrategyDependencyEnum getTransferStrategy() {
    return transferStrategy;
  }
}
