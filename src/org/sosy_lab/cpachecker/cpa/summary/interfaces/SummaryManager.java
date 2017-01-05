/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cpa.summary.interfaces;

import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.summary.blocks.Block;
import org.sosy_lab.cpachecker.cpa.summary.blocks.BlockManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;


/**
 * An interface for {@link org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis}
 * to generate, compare and use summaries.
 *
 * <p>The following vocabulary is used:
 * <ul>
 *   <li>The state initiating the call in the outer function, is the <em>call</em> state.</li>
 *   <li>First state in the called function is the <em>entry</em> state.</li>
 *   <li>Last state in the called function is the <em>returned</em> state.</li>
 *   <li>The state in the outer function to which the control gets back into is the
 *   <em>joined</em> state.</li>
 * </ul>
 */
public interface SummaryManager {

  /**
   * Generate summaries from the result of the intraprocedural analysis.
   * A {@link UseSummaryCPA} may choose to generate as
   * many summaries as it wishes.
   * Implementation can assume that {@link #getWeakenedCallState}
   * was already called on the {@code pCallState}.
   *
   * @param pCallState Summary precondition, associated with
   *                   the call node, which is about to jump into the block.
   * @param pCallPrecision Precision associated with
   *                        {@code pCallState}.
   * @param pReturnStates Return states associated with the <em>return</em> node.
   *                      one transition after the exit state of the block.
   * @param pJoinPrecisions Return precisions associated with {@code pReturnStates},
   *                         in the same order as {@code pReturnStates}.
   * @param pBlock The block for which the summary is generated.
   * @param pCallNode Call node with an outgoing edge to the summarized block.
   * @return summaries which describe the result.
   */
  List<? extends Summary> generateSummaries(
      AbstractState pCallState,
      Precision pCallPrecision,
      List<? extends AbstractState> pReturnStates,
      List<Precision> pJoinPrecisions,
      CFANode pCallNode,
      Block pBlock
  ) throws CPATransferException;

  /**
   * Calculate the abstract successors subject to a list of summaries {@code pSummaries},
   * <b>assuming</b> they are all applicable at the callsite.
   *
   * @param pCallState Initial state, associated with a function call.
   *               Usually has an outgoing {@link FunctionCallEdge}.
   * @param pCallPrecision Analysis precision at the to-state.
   * @param pSummaries All summaries which hold for the callsite.
   * @param pBlock The block for which the summary was calculated.
   *               Contains information obtained from the dataflow analysis,
   *               which is useful for summary application.
   * @param pCallEdge Call edge, which successor is the entry node of {@code pBlock}.
   * @return resulting states associated with the <em>join</em> node.
   */
  List<? extends AbstractState> getAbstractSuccessorsForSummary(
      AbstractState pCallState,
      Precision pCallPrecision,
      List<Summary> pSummaries,
      Block pBlock,
      CFAEdge pCallEdge)
      throws CPAException, InterruptedException;

  /**
   * Weaken the call state (the successor from which is associated
   * with {@link FunctionEntryNode}), by removing all information not
   * relevant to the function being called.
   *
   * @param pCallState state associated with a call node.
   * @param pPrecision precision associated with {@code pCallState}
   * @param pCallEdge Edge associated with the call inside the block.
   * @param pBlock block for which the function is computed.
   *
   * @return Abstract states associated with a block entry node.
   */
  AbstractState getWeakenedCallState(
      AbstractState pCallState,
      Precision pPrecision,
      CFAEdge pCallEdge,
      Block pBlock
  );


  /**
   * Optionally merge two summaries, same interface as
   * {@link org.sosy_lab.cpachecker.core.interfaces.MergeOperator}.
   * The result has to subsume the second argument.
   *
   * <p>Implementation should return the second argument to refuse
   * merging and to keep the states separate instead.
   */
  Summary merge(Summary pSummary1, Summary pSummary2) throws CPAException, InterruptedException;

  /**
   * A summary is described by another summary iff it allows more behaviors.
   *
   * @return whether {@code pSummary1} is described by {@code pSummary2}.
   */
  boolean isDescribedBy(Summary pSummary1, Summary pSummary2);

  /**
   * @return whether {@code pSummary} is general enough to describe the condition at the
   * callsite {@code pCallSite}.
   * Note that {@code pCallsite} is outside of the called block.
   */
  boolean isCallsiteLessThanSummary(AbstractState pCallsite, Summary pSummary);

  /**
   * A summary {@code pSummary} is applicable at the callsite {@code pCallstate}
   * only if {@link #getCallstatePartition(AbstractState pCallstate)}
   * is equal to {@code getSummaryPartition(pSummary)}.
   */
  default String getSummaryPartition(Summary pSummary) {
    return "";
  }

  /**
   * Partition for the callstack,
   * should be consistent with {@link #getSummaryPartition(Summary)}
   *
   * @see #getSummaryPartition(Summary)
   */
  default String getCallstatePartition(AbstractState pCallstate) {
    return "";
  }


  /**
   * Communicate the block partitioning to the configurable
   * program analysis.
   */
  default void setBlockManager(BlockManager pManager) {}
}
