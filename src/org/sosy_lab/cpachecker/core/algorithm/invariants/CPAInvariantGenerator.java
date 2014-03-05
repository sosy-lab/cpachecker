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
package org.sosy_lab.cpachecker.core.algorithm.invariants;

import static com.google.common.base.Preconditions.*;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;

import org.sosy_lab.common.Classes.UnexpectedCheckedException;
import org.sosy_lab.common.LazyFutureTask;
import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.concurrency.Threads;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.CPABuilder;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.CPAAlgorithm;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSetWrapper;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.CPAs;

import com.google.common.base.Throwables;

/**
 * Class that encapsulates invariant generation by using the CPAAlgorithm
 * with an appropriate configuration.
 * Supports synchronous and asynchronous execution.
 */
@Options(prefix="invariantGeneration")
public class CPAInvariantGenerator implements InvariantGenerator {

  @Option(name="config",
          required=true,
          description="configuration file for invariant generation")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path configFile;

  @Option(description="generate invariants in parallel to the normal analysis")
  private boolean async = false;

  private final Timer invariantGeneration = new Timer();

  private final LogManager logger;
  private final Algorithm invariantAlgorithm;
  private final ConfigurableProgramAnalysis invariantCPAs;
  private final ReachedSet reached;

  private final ShutdownNotifier shutdownNotifier;

  private Future<UnmodifiableReachedSet> invariantGenerationFuture = null;

  public CPAInvariantGenerator(Configuration config, LogManager pLogger,
      ReachedSetFactory reachedSetFactory, ShutdownNotifier pShutdownNotifier, CFA cfa) throws InvalidConfigurationException, CPAException {
    config.inject(this);
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;

    Configuration invariantConfig;
    try {
      invariantConfig = Configuration.builder()
                            .loadFromFile(configFile)
                            .build();
    } catch (IOException e) {
      throw new InvalidConfigurationException("could not read configuration file for invariant generation: " + e.getMessage(), e);
    }

    invariantCPAs = new CPABuilder(invariantConfig, logger, pShutdownNotifier, reachedSetFactory).buildCPAs(cfa);
    invariantAlgorithm = CPAAlgorithm.create(invariantCPAs, logger, invariantConfig, pShutdownNotifier);
    reached = new ReachedSetFactory(invariantConfig, logger).create();
  }

  @Override
  public void start(CFANode initialLocation) {
    checkNotNull(initialLocation);
    checkState(invariantGenerationFuture == null);
    checkState(!reached.hasWaitingState());

    reached.add(invariantCPAs.getInitialState(initialLocation), invariantCPAs.getInitialPrecision(initialLocation));

    Callable<UnmodifiableReachedSet> task = new Callable<UnmodifiableReachedSet>() {
      @Override
      public UnmodifiableReachedSet call() throws Exception {
        return findInvariants();
      }
    };

    if (async) {
      ExecutorService executor = Executors.newSingleThreadExecutor(Threads.threadFactory());
      invariantGenerationFuture = executor.submit(task);
      executor.shutdown();
    } else {
      invariantGenerationFuture = new LazyFutureTask<>(task);
    }
  }

  @Override
  public void cancel() {
    checkState(invariantGenerationFuture != null);
    shutdownNotifier.requestShutdown("Invariant generation cancel requested.");
    invariantGenerationFuture.cancel(true);
  }

  @Override
  public UnmodifiableReachedSet get() throws CPAException, InterruptedException {
    checkState(invariantGenerationFuture != null);
    try {
      return invariantGenerationFuture.get();

    } catch (ExecutionException e) {
      Throwables.propagateIfPossible(e.getCause(), CPAException.class, InterruptedException.class);
      throw new UnexpectedCheckedException("invariant generation", e.getCause());
    }
  }

  @Override
  public Timer getTimeOfExecution() {
    return invariantGeneration;
  }

  private UnmodifiableReachedSet findInvariants() throws CPAException, InterruptedException {
    checkState(reached.hasWaitingState());

    invariantGeneration.start();
    logger.log(Level.INFO, "Finding invariants");

    try {
      assert invariantAlgorithm != null;
      invariantAlgorithm.run(reached);

      CPAs.closeCpaIfPossible(invariantCPAs, logger);
      CPAs.closeIfPossible(invariantAlgorithm, logger);

      if (reached.hasWaitingState()) {
        // We may not return the reached set in this case
        // because the invariants may be incomplete
        throw new CPAException("Invariant generation algorithm did not finish processing the reached set, invariants not available.");
      }

      return new UnmodifiableReachedSetWrapper(reached);

    } finally {
      invariantGeneration.stop();
    }
  }
}