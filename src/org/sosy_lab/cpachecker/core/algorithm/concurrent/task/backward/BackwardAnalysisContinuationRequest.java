// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.task.backward;

import com.google.common.collect.Table;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.ShareableBooleanFormula;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.Task;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.TaskInvalidatedException;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.TaskManager;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.TaskRequest;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.composite.BlockAwareCompositeCPA;

public class BackwardAnalysisContinuationRequest implements TaskRequest {
  final Block block;
  final ReachedSet reachedSet;
  final Algorithm algorithm;
  final BlockAwareCompositeCPA cpa;
  final TaskManager taskManager;
  final LogManager logManager;
  final ShutdownNotifier shutdownNotifier;
      
  public BackwardAnalysisContinuationRequest(
      final Block pBlock,
      final ReachedSet pReachedSet,
      final Algorithm pAlgorithm,
      final BlockAwareCompositeCPA pCPA,
      final TaskManager pTaskManager,
      final LogManager pLogManager,
      final ShutdownNotifier pShutdownNotifier
  ) {
    block = pBlock;
    reachedSet = pReachedSet;
    algorithm = pAlgorithm;
    cpa = pCPA;
    taskManager = pTaskManager;
    logManager = pLogManager;
    shutdownNotifier = pShutdownNotifier;
  }
  
  @Override
  public Task finalize(
      Table<Block, Block, ShareableBooleanFormula> pSummaries,
      Map<Block, Integer> pSummaryVersions,
      Set<CFANode> pAlreadyPropagated) throws TaskInvalidatedException {
    return new BackwardAnalysisContinuation(
        block, reachedSet, algorithm, cpa, taskManager, logManager, shutdownNotifier
    );
  }
}
