// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.strategy;

import java.nio.file.Path;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PCCStrategy;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.pcc.strategy.arg.ARGProofCheckerStrategy;

@Options(prefix = "pcc")
public class PCCStrategyBuilder {

  @Option(
      secure = true,
      description =
          "Qualified name for class which implements certification strategy, hence proof writing,"
              + " to be used.")
  @ClassOption(
      packagePrefix = {
        "org.sosy_lab.cpachecker.pcc.strategy",
        "org.sosy_lab.cpachecker.pcc.strategy.parallel"
      })
  private PCCStrategy.Factory strategy =
      (config,
          logger,
          shutdownNotifier,
          proofFile,
          cfa,
          specification,
          proofChecker,
          propertyChecker) ->
          new ARGProofCheckerStrategy(config, logger, shutdownNotifier, proofFile, proofChecker);

  private PCCStrategyBuilder() {}

  public static PCCStrategy buildStrategy(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Path pProofFile,
      @Nullable ConfigurableProgramAnalysis pCpa,
      @Nullable CFA pCfa,
      @Nullable Specification pSpecification)
      throws InvalidConfigurationException {

    PCCStrategyBuilder builder = new PCCStrategyBuilder();
    pConfig.inject(builder);

    ProofChecker proofChecker = pCpa instanceof ProofChecker ? (ProofChecker) pCpa : null;
    PropertyCheckerCPA propertyChecker =
        pCpa instanceof PropertyCheckerCPA ? (PropertyCheckerCPA) pCpa : null;

    return builder.strategy.create(
        pConfig,
        pLogger,
        pShutdownNotifier,
        pProofFile,
        pCfa,
        pSpecification,
        proofChecker,
        propertyChecker);
  }
}
