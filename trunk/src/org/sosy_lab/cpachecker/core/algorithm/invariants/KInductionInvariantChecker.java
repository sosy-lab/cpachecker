// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.invariants;

import static com.google.common.base.Preconditions.checkState;

import java.io.IOException;
import java.nio.file.Path;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.ShutdownManager;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.bmc.CandidateGenerator;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSetFactory;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options(prefix = "invariantChecker")
public class KInductionInvariantChecker {

  @Option(
      secure = true,
      description =
          "Configuration file for the K-Induction algorithm for checking candidates on invariance.")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path kInductionConfig =
      Classes.getCodeLocation(KInductionInvariantChecker.class)
          .resolveSibling("config/bmc-invgen.properties");

  private final CFA cfa;
  private final KInductionInvariantGenerator invGen;

  private boolean isComputationFinished = false;

  /**
   * Create a new k-induction based invariant checker. Actual computation is started with {@link
   * #checkCandidates()}, it is blocking and runs synchronously. The created instance of this class
   * should be only used once.
   *
   * @param pConfig the Configuration for this check
   * @param pShutdownNotifier the parent shutdown notifier
   * @param pLogger the logger which should be used
   * @param pCfa the whole CFA
   * @param pCandidateGenerator the Candidate Generator (it's CandidateInvariants have to work with
   *     different solvers, as in most cases this check will use another solver than the caller of
   *     this method does.)
   * @throws InvalidConfigurationException is thrown if the configuration file for k-induction is
   *     not available
   * @throws CPAException may be thrown while building the CPAs used for this check
   */
  public KInductionInvariantChecker(
      Configuration pConfig,
      ShutdownNotifier pShutdownNotifier,
      LogManager pLogger,
      CFA pCfa,
      Specification specification,
      CandidateGenerator pCandidateGenerator)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    pConfig.inject(this);
    cfa = pCfa;

    Configuration invariantConfig;
    try {
      // clear interrupted flag before we try to load the configuration, this might lead to
      // exceptions otherwise
      Thread.interrupted();
      invariantConfig = Configuration.builder().loadFromFile(kInductionConfig).build();

    } catch (IOException e) {
      throw new InvalidConfigurationException(
          "Could not read configuration file for invariant generation: " + e.getMessage(), e);
    }

    ReachedSetFactory reached = new ReachedSetFactory(pConfig, pLogger);

    invGen =
        KInductionInvariantGenerator.create(
            invariantConfig,
            pLogger,
            ShutdownManager.createWithParent(pShutdownNotifier),
            cfa,
            specification,
            reached,
            pCandidateGenerator,
            false);
  }

  /** Determines if the program could be successfully proven to be safe with k-induction. */
  public boolean isProgramSafe() {
    checkState(isComputationFinished);
    return invGen.isProgramSafe();
  }

  /**
   * This method starts the inductiveness check. Candidates that are proved to be invariant can be
   * retrieved by calling {@link CandidateGenerator#getConfirmedCandidates()}.
   */
  public void checkCandidates() throws CPAException, InterruptedException {
    checkState(!isComputationFinished);

    invGen.start(cfa.getMainFunction());
    invGen.getSupplier(); // let invariant generator do the work

    isComputationFinished = true;
  }
}
