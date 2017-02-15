/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.pcc.strategy;

import java.nio.file.Path;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.ClassOption;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.pcc.PCCStrategy;
import org.sosy_lab.cpachecker.core.interfaces.pcc.ProofChecker;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.pcc.strategy.arg.ARGProofCheckerStrategy;

@Options(prefix = "pcc")
public class PCCStrategyBuilder {

  @Option(
    secure = true,
    description =
        "Qualified name for class which implements certification strategy, hence proof writing, to be used."
  )
  @ClassOption(
    packagePrefix = {
      "org.sosy_lab.cpachecker.pcc.strategy",
      "org.sosy_lab.cpachecker.pcc.strategy.parallel"
    }
  )
  private PCCStrategy.Factory strategy =
      (config, logger, shutdownNotifier, proofFile, cfa, specification, proofChecker, propertyChecker) ->
          new ARGProofCheckerStrategy(config, logger, shutdownNotifier, proofFile, proofChecker);

  private PCCStrategyBuilder() {}

  public static PCCStrategy buildStrategy(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      Path pProofFile,
      ConfigurableProgramAnalysis pCpa,
      CFA pCfa,
      Specification pSpecification)
      throws InvalidConfigurationException {

    PCCStrategyBuilder builder = new PCCStrategyBuilder();
    pConfig.inject(builder);

    ProofChecker proofChecker = pCpa instanceof ProofChecker ? (ProofChecker) pCpa : null;
    PropertyCheckerCPA propertyChecker =
        pCpa instanceof PropertyCheckerCPA ? (PropertyCheckerCPA) pCpa : null;

    return builder.strategy.create(
        pConfig, pLogger, pShutdownNotifier, pProofFile, pCfa, pSpecification, proofChecker, propertyChecker);
  }
}
