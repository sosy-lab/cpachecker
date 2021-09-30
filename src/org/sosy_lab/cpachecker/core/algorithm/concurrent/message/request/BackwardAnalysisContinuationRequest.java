// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.concurrent.message.request;

import com.google.common.collect.Table;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.blockgraph.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.message.MessageFactory;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.Task;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.task.backward.BackwardAnalysisCore;
import org.sosy_lab.cpachecker.core.algorithm.concurrent.util.ShareableBooleanFormula;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.composite.BlockAwareCompositeCPA;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateCPA;

public class BackwardAnalysisContinuationRequest implements TaskRequest {
  final Block block;
  final ReachedSet reachedSet;
  final Algorithm algorithm;
  final BlockAwareCompositeCPA cpa;
  final MessageFactory messageFactory;
  final LogManager logManager;
  final ShutdownNotifier shutdownNotifier;
      
  public BackwardAnalysisContinuationRequest(
      final Block pBlock,
      final ReachedSet pReachedSet,
      final Algorithm pAlgorithm,
      final BlockAwareCompositeCPA pCPA,
      final MessageFactory pMessageFactory,
      final LogManager pLogManager,
      final ShutdownNotifier pShutdownNotifier
  ) {
    block = pBlock;
    reachedSet = pReachedSet;
    algorithm = pAlgorithm;
    cpa = pCPA;
    messageFactory = pMessageFactory;
    logManager = pLogManager;
    shutdownNotifier = pShutdownNotifier;
  }
  
  @Override
  public Task process(
      Table<Block, Block, ShareableBooleanFormula> pSummaries,
      Map<Block, Integer> pSummaryVersions,
      Set<CFANode> pAlreadyPropagated) throws RequestInvalidatedException {
    PredicateCPA predicateCPA = cpa.retrieveWrappedCpa(PredicateCPA.class);
    assert predicateCPA != null;
    
    return new BackwardAnalysisCore(
        block, reachedSet, algorithm, cpa, predicateCPA.getSolver(), messageFactory, logManager, shutdownNotifier
    );
  }
}
