// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.message.request.forward;

import com.google.common.collect.Table;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.Scheduler;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.Scheduler.SummaryVersion;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.MessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.request.RequestInvalidatedException;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.request.TaskRequest;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.Task;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.forward.ForwardAnalysisCore;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.util.ShareableBooleanFormula;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

@Options(prefix = "concurrent.task.forward")
public class ForwardAnalysisContinuationRequest implements TaskRequest {
  private final Block target;
  private final int expectedVersion;
  private final ARGCPA cpa;
  private final Algorithm algorithm;
  private final ReachedSet reachedSet;
  private final Solver solver;
  private final PathFormulaManager pfMgr;
  private final MessageFactory messageFactory;
  private final LogManager logManager;
  private final ShutdownNotifier shutdownNotifier;

  @SuppressWarnings("FieldMayBeFinal")
  @Option(description = "Indicates whether the analysis should first check if a new summary "
      + "really adds new information or is just redundant. In the later case, the analysis "
      + "for the new summary gets aborted. These checks involve satisfiability queries and "
      + "get performed for each new forward analysis task. This option provides the user with "
      + "the ability to declare whether the overhead of these checks is worth the advantage "
      + "of aborting analysis tasks early.", secure = true)
  private boolean performRedundancyChecks = true;

  public ForwardAnalysisContinuationRequest(
      final Configuration pGlobalConfiguration,
      final Block pTarget,
      final int pPExpectedVersion,
      final ARGCPA pCPA,
      final Algorithm pAlgorithm,
      final ReachedSet pReachedSet,
      final Solver pSolver,
      final PathFormulaManager pPfMgr,
      final MessageFactory pMessageFactory,
      final LogManager pLogManager,
      final ShutdownNotifier pShutdownNotifier
  ) throws InvalidConfigurationException {
    pGlobalConfiguration.inject(this);

    target = pTarget;
    expectedVersion = pPExpectedVersion;
    cpa = pCPA;
    algorithm = pAlgorithm;
    reachedSet = pReachedSet;
    solver = pSolver;
    pfMgr = pPfMgr;
    messageFactory = pMessageFactory;
    logManager = pLogManager;
    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  public Task process(
      Table<Block, Block, ShareableBooleanFormula> pSummaries,
      Map<Block, SummaryVersion> pSummaryVersions,
      Set<CFANode> pAlreadyPropagated) throws RequestInvalidatedException,
                                              InvalidConfigurationException {
    assert Thread.currentThread().getName().equals(Scheduler.getThreadName())
        : "Only " + Scheduler.getThreadName() + " may call process()";

    SummaryVersion version = pSummaryVersions.getOrDefault(target, SummaryVersion.getInitial());
    if (performRedundancyChecks) {
      if (version.passedNonRedundancyCheck < expectedVersion) {
        /*
         * This analysis just passed the non-redundancy-check and publishes this information by
         * setting the latestVersionWhichPassedNonRedundancyCheck-value for the summary of the target
         * block to its version value.
         */
        pSummaryVersions.put(target,
            version.setLatestVersionWhichPassedRedundancyCheck(expectedVersion));
      } else if (version.passedNonRedundancyCheck > expectedVersion) {
        /*
         * Another more recent analysis with a higher version value has passed the
         * non-redundancy-check. The present analysis therefore becomes redundant and stops.
         */
        throw new RequestInvalidatedException();
      }
      /*
       * The case version.passedNonRedundancyCheck == expectedVersion represents that the current task
       * remains the most recent analysis for the target block which has passed the non-redundancy-
       * check and can just continue to run.
       */
    } else {
      if (version.current > expectedVersion) {
        throw new RequestInvalidatedException();
      }
    }

    return new ForwardAnalysisCore(
        target, reachedSet, expectedVersion, algorithm, cpa, solver, pfMgr, messageFactory,
        logManager, shutdownNotifier
    );
  }
}
