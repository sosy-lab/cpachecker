/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.pcc;

import java.io.IOException;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.pcc.strategy.arg.AbstractARGStrategy;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.ci.CustomInstructionApplications;
import org.sosy_lab.cpachecker.util.ci.CustomInstructionApplications.CustomInstructionApplicationBuilder;
import org.sosy_lab.cpachecker.util.ci.CustomInstructionApplications.CustomInstructionApplicationBuilder.CIDescriptionType;
import org.sosy_lab.cpachecker.util.ci.CustomInstructionRequirementsExtractor;

@Options
public class ProofCheckAndExtractCIRequirementsAlgorithm extends ProofCheckAlgorithm {

  private final CustomInstructionApplicationBuilder ciasBuilder;
  private final CustomInstructionRequirementsExtractor ciExtractor;
  private final ConfigurableProgramAnalysis cpa;

  @Option(secure = true,
      name = "pcc.HWrequirements.extraction.mode",
      description = "Specifies the mode how HW requirements are detected in the proof.")
  private CIDescriptionType ciMode = CIDescriptionType.OPERATOR;

  public ProofCheckAndExtractCIRequirementsAlgorithm(
      ConfigurableProgramAnalysis pCpa,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      Specification pSpecification)
      throws InvalidConfigurationException, CPAException {
    super(pCpa, pConfig, pLogger, pShutdownNotifier, pCfa, pSpecification);

    pConfig.inject(this);

    ciasBuilder = CustomInstructionApplicationBuilder.getBuilder(ciMode, pConfig, pLogger, pShutdownNotifier, pCfa);
    ciExtractor = new CustomInstructionRequirementsExtractor(pConfig, pLogger, pShutdownNotifier, pCpa);
    cpa = pCpa;

    Class<? extends AbstractState> requirementsStateClass = ciExtractor.getRequirementsStateClass();
    try {
      if (AbstractStates.extractStateByType(pCpa.getInitialState(pCfa.getMainFunction(), StateSpacePartition.getDefaultPartition()),
                                            requirementsStateClass) == null) {
        throw new InvalidConfigurationException(requirementsStateClass + "is not an abstract state.");
      }
    } catch (InterruptedException e) {
      throw new InvalidConfigurationException(requirementsStateClass + "initial state computation did not finish in time");
    }

    if(!(checkingStrategy instanceof AbstractARGStrategy)) {
      throw new InvalidConfigurationException("Custom instruction requirements extraction only works with proofs that are ARGs.");
    }
  }

  @Override
  public AlgorithmStatus run(final ReachedSet reachedSet) throws CPAException, InterruptedException {

    CustomInstructionApplications cia;
    try {
      logger.log(Level.INFO, "Get custom instruction applications in program.");
      cia = ciasBuilder.identifyCIApplications();
    } catch (IOException e) {
      logger.log(Level.SEVERE, "Detecting the custom instruction applications in program failed.", e);
      return AlgorithmStatus.UNSOUND_AND_PRECISE;
    }

    if (ciExtractor.getRequirementsStateClass().equals(PredicateAbstractState.class)) {
      PredicateCPA predCPA = CPAs.retrieveCPA(cpa, PredicateCPA.class);
      if (predCPA == null) {
        logger.log(Level.SEVERE,
            "Cannot find PredicateCPA in CPA configuration but it is required to set abstraction nodes");
        return AlgorithmStatus.UNSOUND_AND_PRECISE;
      }
      predCPA.changeExplicitAbstractionNodes(cia.getStartAndEndLocationsOfCIApplications());
    }

    // proof checking
    AlgorithmStatus status = super.run(reachedSet);

    if (status.isSound()) {
      logger.log(Level.INFO, "Extracting custom instruction requirements.");
      ciExtractor.extractRequirements(((AbstractARGStrategy) checkingStrategy).getARG(), cia);
    } else {
      logger.log(Level.INFO, "Proof checking failed, do not extract requirements!");
    }

    return status;

  }

}
