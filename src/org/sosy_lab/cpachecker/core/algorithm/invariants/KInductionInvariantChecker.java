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

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.configuration.TimeSpanOption;
import org.sosy_lab.common.io.Path;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.time.TimeSpan;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.bmc.CandidateGenerator;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.resources.ResourceLimit;
import org.sosy_lab.cpachecker.util.resources.ResourceLimitChecker;
import org.sosy_lab.cpachecker.util.resources.WalltimeLimit;

@Options(prefix = "invariantChecker")
public class KInductionInvariantChecker {

  @Option(
    secure = true,
    description =
        "Configuration file for the K-Induction algorithm for checking candidates on invariance."
  )
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path kInductionConfig = Paths.get("config/bmc-invgen.properties");

  @Option(
    secure = true,
    description =
        "Timelimit for invariant generation which may be"
            + " used during refinement.\n"
            + "(Use seconds or specify a unit; 0 for infinite)"
  )
  @TimeSpanOption(codeUnit = TimeUnit.NANOSECONDS, defaultUserUnit = TimeUnit.SECONDS, min = 0)
  private TimeSpan timeForInvariantCheck = TimeSpan.ofNanos(0);

  private final Configuration config;
  private final ShutdownNotifier shutdownNotifier;
  private final LogManager logger;

  private final CFA cfa;
  private final KInductionInvariantGenerator invGen;
  private final ResourceLimitChecker limits;

  private boolean isComputationFinished = false;

  /**
   * Create a new k-induction based invariant checker. Actual computation is started
   * with {@link #checkCandidates()}, it is blocking and runs synchronously. The
   * created instance of this class should be only used once.
   *
   * @param pConfig the Configuration for this check
   * @param pShutdownNotifier the parent shutdown notifier
   * @param pLogger the logger which should be used
   * @param pCfa the whole CFA
   * @param pCandidateGenerator the Candidate Generator (it's CandidateInvariants have
   * to work with different solvers, as in most cases this check will use another solver
   * than the caller of this method does.)
   * @throws InvalidConfigurationException is thrown if the configuration file for k-induction
   * is not available
   * @throws CPAException may be thrown while building the CPAs used for this check
   */
  public KInductionInvariantChecker(
      Configuration pConfig,
      ShutdownNotifier pShutdownNotifier,
      LogManager pLogger,
      CFA pCfa,
      CandidateGenerator pCandidateGenerator)
      throws InvalidConfigurationException, CPAException {
    pConfig.inject(this);

    config = pConfig;
    shutdownNotifier = pShutdownNotifier;
    logger = pLogger;
    cfa = pCfa;

    Configuration invariantConfig;
    try {
      invariantConfig = Configuration.builder().loadFromFile(kInductionConfig).build();
    } catch (IOException e) {
      throw new InvalidConfigurationException(
          "Could not read configuration file for invariant generation: " + e.getMessage(), e);
    }

    ReachedSetFactory reached = new ReachedSetFactory(config);

    ShutdownManager invariantShutdown = ShutdownManager.createWithParent(shutdownNotifier);

    if (!timeForInvariantCheck.isEmpty()) {
      WalltimeLimit l = WalltimeLimit.fromNowOn(timeForInvariantCheck);
      limits =
          new ResourceLimitChecker(invariantShutdown, Collections.<ResourceLimit>singletonList(l));
    } else {
      limits = null;
    }

    invGen =
        KInductionInvariantGenerator.create(
            invariantConfig, logger, invariantShutdown, cfa, reached, pCandidateGenerator, false);
  }

  /**
   * Determines if the program could be successfully proven to be safe with k-induction.
   */
  public boolean isProgramSafe() {
    checkState(isComputationFinished);
    return invGen.isProgramSafe();
  }

  /**
   * This method starts the inductiveness check.
   * Candidates that are proved to be invariant can be retrieved by calling
   * {@link CandidateGenerator#getConfirmedCandidates()}.
   */
  public void checkCandidates() throws CPAException, InterruptedException {
    checkState(!isComputationFinished);

    if (limits != null) {
      limits.start();
    }

    invGen.start(cfa.getMainFunction());
    invGen.get(); // let invariant generator do the work

    if (limits != null) {
      limits.cancel();
    }

    isComputationFinished = true;
  }
}
