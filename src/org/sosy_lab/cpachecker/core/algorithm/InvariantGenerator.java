/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm;

import static com.google.common.base.Preconditions.checkState;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;

import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.common.Concurrency;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.base.Throwables;

/**
 * Class that encapsulates invariant generation.
 * Supports synchronous and asynchronous execution.
 */
@Options(prefix="bmc")
class InvariantGenerator {

  @Option(name="invariantGenerationConfigFile",
          description="configuration file for invariant generation")
  @FileOption(FileOption.Type.OPTIONAL_INPUT_FILE)
  private File configFile;

  @Option(description="generate invariants for induction in parallel to the analysis")
  private boolean parallelInvariantGeneration = false;

  private final Timer invariantGeneration = new Timer();

  private final LogManager logger;
  private final Algorithm invariantAlgorithm;
  private final ConfigurableProgramAnalysis invariantCPAs;
  private final ReachedSet reached;

  private CFANode initialLocation = null;

  private ExecutorService executor = null;
  private Future<ReachedSet> invariantGenerationFuture = null;

  public InvariantGenerator(Configuration config, LogManager pLogger, ReachedSetFactory reachedSetFactory, CFA cfa) throws InvalidConfigurationException, CPAException {
    config.inject(this);
    logger = pLogger;

    if (configFile != null) {
      Configuration invariantConfig;
      try {
        invariantConfig = Configuration.builder()
                              .loadFromFile(configFile)
                              .build();
      } catch (IOException e) {
        throw new InvalidConfigurationException("could not read configuration file for invariant generation: " + e.getMessage(), e);
      }

      invariantCPAs = new CPABuilder(invariantConfig, logger, reachedSetFactory).buildCPAs(cfa);
      invariantAlgorithm = new CPAAlgorithm(invariantCPAs, logger, invariantConfig);
      reached = new ReachedSetFactory(invariantConfig, logger).create();

    } else {
      // invariant generation is disabled
      invariantAlgorithm = null;
      invariantCPAs = null;
      reached = new ReachedSetFactory(config, logger).create(); // create reached set that will stay empty
    }
  }

  public void start(CFANode pInitialLocation) {
    checkState(initialLocation == null);
    initialLocation = pInitialLocation;

    if (invariantCPAs == null) {
      // invariant generation disabled
      return;
    }

    reached.add(invariantCPAs.getInitialState(initialLocation), invariantCPAs.getInitialPrecision(initialLocation));

    if (parallelInvariantGeneration) {

      executor = Executors.newSingleThreadExecutor();
      invariantGenerationFuture = executor.submit(new Callable<ReachedSet>() {
            @Override
            public ReachedSet call() throws Exception {
              return findInvariants();
            }
          });
      executor.shutdown();
    }
  }

  public void cancelAndWait() {
    if (invariantGenerationFuture != null) {
      invariantGenerationFuture.cancel(true);
      Concurrency.waitForTermination(executor);
    }
  }

  public ReachedSet get() throws CPAException, InterruptedException {
    if (invariantGenerationFuture == null) {
      return findInvariants();

    } else {
      try {
        return invariantGenerationFuture.get();

      } catch (ExecutionException e) {
        Throwables.propagateIfPossible(e.getCause(), CPAException.class, InterruptedException.class);
        throw new UnexpectedCheckedException("invariant generation", e.getCause());
      }
    }
  }

  /*
   * Returns a Timer from which the time that was necessary to generate
   * the invariants can be read.
   * For correct measurements, the caller should not modify the Timer.
   */
  public Timer getTimeOfExecution() {
    return invariantGeneration;
  }

  private ReachedSet findInvariants() throws CPAException, InterruptedException {
    checkState(initialLocation != null);

    if (!reached.hasWaitingState()) {
      // invariant generation disabled
      return reached;
    }

    invariantGeneration.start();
    logger.log(Level.INFO, "Finding invariants");

    try {
      assert invariantAlgorithm != null;
      invariantAlgorithm.run(reached);

      return reached;

    } finally {
      invariantGeneration.stop();
    }
  }
}