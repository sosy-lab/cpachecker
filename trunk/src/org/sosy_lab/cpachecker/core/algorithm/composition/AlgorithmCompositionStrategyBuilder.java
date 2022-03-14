// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.composition;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.specification.Specification;

@Options(prefix = "compositionAlgorithm")
public class AlgorithmCompositionStrategyBuilder {

  @Option(
      secure = true,
      description =
          "Qualified name for class which implements strategy that decides how to compose given"
              + " analyses")
  @ClassOption(packagePrefix = "org.sosy_lab.cpachecker.core.algorithm.composition")
  private AlgorithmCompositionStrategy.Factory strategy =
      (config, logger, shutdownNotifier, cfa, spec) ->
          new CircularCompositionStrategy(config, logger);

  private AlgorithmCompositionStrategyBuilder() {}

  public static AlgorithmCompositionStrategy buildStrategy(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      Specification pSpecification)
      throws InvalidConfigurationException {

    AlgorithmCompositionStrategyBuilder builder = new AlgorithmCompositionStrategyBuilder();
    pConfig.inject(builder);

    return builder.strategy.create(pConfig, pLogger, pShutdownNotifier, pCfa, pSpecification);
  }
}
