// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.message.request.backward;

import com.google.common.collect.Table;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.MessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.request.RequestInvalidatedException;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.request.TaskRequest;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.Task;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.backward.BackwardAnalysisCore;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.util.ErrorOrigin;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.util.ShareableBooleanFormula;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

public class BackwardAnalysisContinuationRequest implements TaskRequest {
  final Block block;
  final ErrorOrigin origin;
  final ReachedSet reachedSet;
  final Algorithm algorithm;
  final ARGCPA argcpa;
  final Solver solver;
  final MessageFactory messageFactory;
  final LogManager logManager;
  final ShutdownNotifier shutdownNotifier;

  public BackwardAnalysisContinuationRequest(
      final Block pBlock,
      final ErrorOrigin pOrigin,
      final ReachedSet pReachedSet,
      final Algorithm pAlgorithm,
      final ARGCPA pCPA,
      final Solver pSolver,
      final MessageFactory pMessageFactory,
      final LogManager pLogManager,
      final ShutdownNotifier pShutdownNotifier
  ) {
    block = pBlock;
    origin = pOrigin;
    reachedSet = pReachedSet;
    algorithm = pAlgorithm;
    argcpa = pCPA;
    solver = pSolver;
    messageFactory = pMessageFactory;
    logManager = pLogManager;
    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  public Task process(
      Table<Block, Block, ShareableBooleanFormula> pSummaries,
      Map<Block, Integer> pSummaryVersions,
      Set<CFANode> pAlreadyPropagated) throws RequestInvalidatedException {

    return new BackwardAnalysisCore(
        block, reachedSet, origin, algorithm, argcpa,
        solver, messageFactory, logManager, shutdownNotifier
    );
  }
}
