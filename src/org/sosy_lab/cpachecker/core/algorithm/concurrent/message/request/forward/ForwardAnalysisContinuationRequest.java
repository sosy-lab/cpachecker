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
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.Scheduler;
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

public class ForwardAnalysisContinuationRequest implements TaskRequest {
  private final Configuration globalConfiguration;
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
  ) {
    globalConfiguration = pGlobalConfiguration;
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
      Map<Block, Integer> pSummaryVersions,
      Set<CFANode> pAlreadyPropagated) throws RequestInvalidatedException,
                                              InvalidConfigurationException {
    assert Thread.currentThread().getName().equals(Scheduler.getThreadName())
        : "Only " + Scheduler.getThreadName() + " may call process()";

    int currentVersion = pSummaryVersions.getOrDefault(target, 0);
    if(currentVersion > expectedVersion) {
      throw new RequestInvalidatedException();
    }
    
    return new ForwardAnalysisCore(
        globalConfiguration, target, reachedSet, expectedVersion, algorithm, cpa, solver, pfMgr, messageFactory,
        logManager, shutdownNotifier
    );
  }
}
