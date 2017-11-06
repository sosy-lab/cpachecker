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
package org.sosy_lab.cpachecker.core.algorithm.residualprogram;

import com.google.common.base.Preconditions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CFACreator;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.Specification;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ForwardingReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.ParserException;
import org.sosy_lab.cpachecker.util.AbstractStates;

@Options(prefix="conditional.verifier")
public class ConditionalVerifierAlgorithm implements Algorithm {

  @Option(description = "configuration for the verification of the residual program which is constructed from another verifier's condition")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path verifierConfig;

  @Option(description = "configuration of the residual program generator")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path generatorConfig;

  private final LogManager logger;
  private final ShutdownNotifier shutdown;
  private final CFA cfa;
  private final Specification spec;
  private final Configuration globalConfig;

  public ConditionalVerifierAlgorithm(final Configuration pConfig, final LogManager pLogger,
      final ShutdownNotifier pShutdownNotifier, final Specification pSpecification, final CFA pCfa)
      throws InvalidConfigurationException {
    pConfig.inject(this);

    logger = pLogger;
    shutdown = pShutdownNotifier;
    spec = pSpecification;
    cfa = pCfa;

    globalConfig = pConfig;
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    Preconditions.checkArgument(pReachedSet instanceof ForwardingReachedSet);

    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;

    try {
      Path residProg = Files.createTempFile("residualProg", ".c");

      CFANode entryFunction = AbstractStates.extractLocation(pReachedSet.getFirstState());
      if (!generateResidualProgram(entryFunction, residProg.toString())) {
        return status.withSound(false);
      }

      shutdown.shutdownIfNecessary();

      status = status.update(
          verifyResidualProgram(entryFunction.getFunctionName(), residProg.toString(),
          (ForwardingReachedSet) pReachedSet));

    } catch (IOException e) {
      logger.logException(Level.SEVERE, e, "Failed to create temporary file for residual program");
      return status.withSound(false);
    }
    return status;
  }

  private boolean generateResidualProgram(final CFANode pEntryNode, final String residualProg)
      throws InterruptedException, CPAException {
    logger.log(Level.INFO, "Start constructing residual program");

    logger.log(Level.FINE, "Build configuration for residual program generation");
    ConfigurationBuilder configBuild = Configuration.builder();
    try {
      configBuild.copyFrom(globalConfig)
                 .clearOption("analysis.asConditionalVerifier")
                 .clearOption("conditional.verifier.verifierConfig")
                 .clearOption("conditional.verifier.generatorConfig")
                 .loadFromFile(generatorConfig)
                 .copyOptionFromIfPresent(globalConfig, "AssumptionGuidingAutomaton.cpa.automaton.inputFile")
                 .copyOptionFromIfPresent(globalConfig, "AssumptionAutomaton.cpa.automaton.inputFile")
                 .setOption("residualprogram.file", residualProg);
      Configuration config = configBuild.build();
      shutdown.shutdownIfNecessary();

      CoreComponentsFactory coreComponents =
          new CoreComponentsFactory(config, logger, shutdown, new AggregatedReachedSets());

      logger.log(Level.FINE, "Build configurable program analysis");
      ConfigurableProgramAnalysis cpa;
      cpa = coreComponents.createCPA(cfa, spec);
      shutdown.shutdownIfNecessary();

      logger.log(Level.FINE, "Instantiate residual program construction algorithm");
      Algorithm algorithm = new ResidualProgramConstructionAlgorithm(cfa, config, logger,
          shutdown, spec, cpa, CPAAlgorithm.create(cpa, logger, config, shutdown));
      shutdown.shutdownIfNecessary();

      logger.log(Level.FINE, "Create reached set");
      AbstractState initialState = cpa.getInitialState(pEntryNode, StateSpacePartition.getDefaultPartition());
      Precision initialPrecision = cpa.getInitialPrecision(pEntryNode, StateSpacePartition.getDefaultPartition());
      ReachedSet reachedSet = coreComponents.createReachedSet();
      reachedSet.add(initialState, initialPrecision);
      shutdown.shutdownIfNecessary();

      logger.log(Level.FINE, "Run algorithm for residual program construction");
      AlgorithmStatus status = algorithm.run(reachedSet);

      if (!status.isSound() || reachedSet.hasWaitingState()) {
        logger.log(Level.SEVERE, "Residual program construction failed.");
        return false;
      }

    } catch (IOException | InvalidConfigurationException  e) {
      logger.logException(Level.SEVERE, e, "Residual program construction failed");
      return false;
    }

    logger.log(Level.INFO, "Finished construction of residual program");

    return true;
  }

  private AlgorithmStatus verifyResidualProgram(final String pEntryFunctionName,
      final String pResidProgPath, final ForwardingReachedSet reached) throws InterruptedException, CPAException {
    logger.log(Level.INFO, "Start verification of residual program");

    logger.log(Level.FINE, "Build configuration for verification");
    ConfigurationBuilder configBuild = Configuration.builder();
    try {
      configBuild.copyFrom(globalConfig)
                 .clearOption("analysis.asConditionalVerifier")
                 .clearOption("conditional.verifier.verifierConfig")
                 .clearOption("conditional.verifier.generatorConfig")
                 .loadFromFile(verifierConfig)
                 .setOption("analysis.entryFunction", pEntryFunctionName)
                 .setOption("parser.usePreprocessor", "true");
      Configuration config = configBuild.build();
      shutdown.shutdownIfNecessary();

      logger.log(Level.FINE, "Parse constructed residual program");
      CFA cfaResidProg = new CFACreator(config, logger, shutdown)
          .parseFileAndCreateCFA(Collections.singletonList(pResidProgPath));
      shutdown.shutdownIfNecessary();

      CoreComponentsFactory coreComponents =
          new CoreComponentsFactory(config, logger, shutdown, new AggregatedReachedSets());

      logger.log(Level.FINE, "Build configurable program analysis");
      ConfigurableProgramAnalysis cpa;
      cpa = coreComponents.createCPA(cfaResidProg, spec);
      shutdown.shutdownIfNecessary();

      logger.log(Level.FINE, "Get verification algorithm");
      Algorithm algorithm = coreComponents.createAlgorithm(cpa, cfaResidProg, spec);
      shutdown.shutdownIfNecessary();

      logger.log(Level.FINE, "Create reached set");
      AbstractState initialState = cpa.getInitialState(cfaResidProg.getMainFunction(),
          StateSpacePartition.getDefaultPartition());
      Precision initialPrecision = cpa.getInitialPrecision(cfaResidProg.getMainFunction(),
          StateSpacePartition.getDefaultPartition());
      ReachedSet reachedSet = coreComponents.createReachedSet();
      reachedSet.add(initialState, initialPrecision);
      reached.setDelegate(reachedSet);
      shutdown.shutdownIfNecessary();

      logger.log(Level.FINE, "Run verification algorithm");
      AlgorithmStatus status = algorithm.run(reachedSet);

      logger.log(Level.INFO, "Finished verification of residual program");

      return status;

    } catch (IOException | InvalidConfigurationException | ParserException e) {
      logger.logException(Level.SEVERE, e, "Verification of residual program failed");
      return AlgorithmStatus.UNSOUND_AND_PRECISE;
    }
  }


}
