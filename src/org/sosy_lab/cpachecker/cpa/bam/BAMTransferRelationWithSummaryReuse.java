// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.Collection;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmFactory;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGTransferRelation;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisSummaryCache;
import org.sosy_lab.cpachecker.cpa.value.SummaryEdge;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class BAMTransferRelationWithSummaryReuse extends BAMTransferRelation {

  private ValueAnalysisSummaryCache summaryCache;

  public BAMTransferRelationWithSummaryReuse(
      BAMCPA bamCpa,
      ShutdownNotifier pShutdownNotifier,
      AlgorithmFactory pFactory,
      BAMPCCManager pBamPccManager,
      boolean pSearchTargetStatesOnExit) {
    super(bamCpa, pShutdownNotifier, pFactory, pBamPccManager, pSearchTargetStatesOnExit);
    summaryCache = ValueAnalysisSummaryCache.getInstance();
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      final AbstractState pState, final Precision pPrecision)
      throws CPATransferException, InterruptedException {

    ARGState argState = (ARGState) pState;
    final CFANode node = extractLocation(pState);

    if (startNewBlockAnalysis(argState, node)) {
      var block = partitioning.getBlockForCallNode(node);
      var callState = AbstractStates.extractStateByType(argState, ValueAnalysisState.class);

      if (summaryCache.getApplicableSummary(block, callState) != null) {
        if (node.getNumLeavingEdges() == 1) {
          var returnNode = Iterables.getOnlyElement(block.getReturnNodes());
          var edge = new SummaryEdge(node, returnNode);
          node.addLeavingEdge(edge);
          returnNode.addEnteringEdge(edge);
        }

        var edge = node.getLeavingEdge(1);

        var compositeTransferRelation =
            Iterables.getOnlyElement(
                ((ARGTransferRelation) transferRelation).getWrappedTransferRelations());

        return ImmutableList.of(
            new ARGState(
                Iterables.getOnlyElement(
                    compositeTransferRelation.getAbstractSuccessorsForEdge(
                        argState.getWrappedState(), pPrecision, edge)),
                argState));
      }
    }

    return super.getAbstractSuccessors(pState, pPrecision);
  }
}
