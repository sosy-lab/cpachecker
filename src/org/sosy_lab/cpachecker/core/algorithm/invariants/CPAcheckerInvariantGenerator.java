/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.invariants;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition.getDefaultPartition;
import static org.sosy_lab.cpachecker.util.AbstractStates.IS_TARGET_STATE;

import com.google.common.base.Preconditions;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.concurrency.Threads;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Files;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory.SpecAutomatonCompositionType;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.invariants.CPAInvariantGenerator.LazyLocationMapping;
import org.sosy_lab.cpachecker.core.algorithm.invariants.CPAInvariantGenerator.ReachedSetBasedExpressionTreeSupplier;
import org.sosy_lab.cpachecker.core.algorithm.invariants.CPAInvariantGenerator.ReachedSetBasedInvariantSupplier;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.StateToFormulaWriter;

import java.io.IOException;
import java.io.Writer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;

@Options(prefix="invariantGeneration.cpachecker")
public class CPAcheckerInvariantGenerator extends AbstractInvariantGenerator {

  @Option(
    secure = true,
    name = "config.path",
    description = "Configuration file for invariant generation with a full CPAchecker run."
  )
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path configPath = Paths.get("config/invgen-components/pred-Invgen-restart.properties");

  @Option(secure=true, description="generate invariants in parallel to the normal analysis")
  private boolean async = false;

  @Option(secure=true, description="dump generated invariants")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private Path invExportPath = null;

  private final LogManager logger;
  private final CFA cfa;
  private final ReachedSet reached;
  private final Algorithm algorithm;
  private final ConfigurableProgramAnalysis cpa;
  private final ShutdownManager shutdownNotifier;

  private volatile boolean generationCompleted = false;
  private volatile boolean programIsSafe = false;

  private final StateToFormulaWriter formulaWriter;

  public CPAcheckerInvariantGenerator(
      Configuration config,
      ShutdownNotifier pShutdownNotifier,
      LogManager pLogger,
      CFA pCfa,
      String pFilename)
      throws InvalidConfigurationException, IOException, CPAException {
    config.inject(this);
    logger = pLogger;
    cfa = pCfa;

    ConfigurationBuilder configBuilder = Configuration.builder().loadFromFile(configPath);
    // we want the invariant generator to run with the same specification as the outside
    // analysis, otherwise the invariants might be useless for us
    if (config.hasProperty("specification")) {
      configBuilder.copyOptionFrom(config, "specification");
    }
    Configuration invgenConfig = configBuilder.build();

    shutdownNotifier = ShutdownManager.createWithParent(pShutdownNotifier);

    CoreComponentsFactory componentsFactory =
        new CoreComponentsFactory(invgenConfig, logger, shutdownNotifier.getNotifier());
    reached = componentsFactory.createReachedSet();

    cpa = componentsFactory.createCPA(pCfa, SpecAutomatonCompositionType.TARGET_SPEC);
    algorithm = componentsFactory.createAlgorithm(cpa, pFilename, pCfa);

    formulaWriter = new StateToFormulaWriter(config, logger, shutdownNotifier.getNotifier(), cfa);
  }

  @Override
  public void start(CFANode pInitialLocation) {
    Preconditions.checkState(!generationCompleted);
    reached.add(
        cpa.getInitialState(pInitialLocation, getDefaultPartition()),
        cpa.getInitialPrecision(pInitialLocation, getDefaultPartition()));

    if (async) {
      ExecutorService executor = Executors.newSingleThreadExecutor(Threads.threadFactory());
      executor.submit(computeInvariants());
      executor.shutdown();
    } else {
      computeInvariants().run();
    }
  }

  private Runnable computeInvariants() {
    return new Runnable() {
      @Override
      public void run() {
        try {
          AlgorithmStatus status = algorithm.run(reached);

          if (!from(reached).anyMatch(IS_TARGET_STATE) && status.isSound()) {
            // program is safe (waitlist is empty, algorithm was sound, no target states present)
            logger.log(
                Level.INFO,
                "Invariant generation with abstract interpretation proved specification to hold.");
            programIsSafe = true;
          }

          generationCompleted = true;

          if (invExportPath != null) {
            try (Writer w = Files.openOutputFile(invExportPath)) {
              formulaWriter.write(reached, w);
            } catch (IOException e) {
              logger.logUserException(Level.WARNING, e, "Could not write formulas to file");
            }
          }

        } catch (CPAException | InterruptedException e) {
          logger.logUserException(Level.WARNING, e, "Invariant generation failed.");
        }
      }
    };
  }

  @Override
  public void cancel() {
    shutdownNotifier.requestShutdown("Cancellation of invariant generation requested");
  }

  @Override
  public boolean isProgramSafe() {
    return programIsSafe;
  }

  @Override
  public InvariantSupplier get() throws CPAException, InterruptedException {
    if (generationCompleted) {
      checkState(programIsSafe || !reached.hasWaitingState());
      checkState(!reached.isEmpty());
      LazyLocationMapping lazyLocationMapping = new LazyLocationMapping(reached);
      return new FormulaAndTreeSupplier(
          new ReachedSetBasedInvariantSupplier(lazyLocationMapping, logger),
          new ReachedSetBasedExpressionTreeSupplier(lazyLocationMapping, cfa));
    } else {
      return InvariantSupplier.TrivialInvariantSupplier.INSTANCE;
    }
  }
}
