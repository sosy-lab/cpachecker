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
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.summary.blocks.Block;
import org.sosy_lab.cpachecker.cpa.summary.blocks.BlockManager;
import org.sosy_lab.cpachecker.exceptions.CPAException;


// todo: starts to have way to many methods as well,
// maybe now projections could be removed.
public interface SummaryManager {

  /**
   * Calculate the abstract successors subject to a summary {@code pSummary}
   *
   * @param pFunctionCallState Initial state, associated with a function call.
   *               Usually has an outgoing {@link FunctionCallEdge}.
   * @param pFunctionCallPrecision Analysis precision at the to-state.
   * @param pSummaries Summaries which hold for the callsite.
   * @param pBlock The block for which the summary was calculated.
   *               Contains information obtained from the dataflow analysis,
   *               which is useful for summary application.
   * @return resulting state
   */
  AbstractState getAbstractSuccessorForSummary(
      AbstractState pFunctionCallState,
      Precision pFunctionCallPrecision,
      List<Summary> pSummaries,
      Block pBlock)
      throws CPAException, InterruptedException;

  /**
   * Weaken the call state (the successor from which is associated
   * with {@link FunctionEntryNode}), by removing all information not
   * relevant to the function being called.
   *
   * @param pState state associated with a call node.
   * @param pPrecision precision associated with {@code pState}
   * @param pBlock block for which the function is computed.
   *
   * @return Abstract states associated with a block entry node.
   */
  AbstractState getWeakenedCallState(
      AbstractState pState,
      Precision pPrecision,
      Block pBlock
  );

  /**
   * Project summary to the function precondition: state at the
   * call node <em>outside</em> of the function.
   *
   * @param pSummary Function summary
   * @return Projection of the summary to the function entry point:
   * the summary precondition.
   */
  AbstractState projectToCallsite(Summary pSummary);

  /**
   * Project the summary to the postcondition: state at the return
   * node <em>inside</em> of the function.
   *
   * // todo: the name is not very consistent with {@link #projectToCallsite(Summary)}
   *
   * @param pSummary Function summary
   * @return Projection of the summary to the return site:
   * the summary postcondition.
   */
  AbstractState projectToPostcondition(Summary pSummary);

  /**
   * Generate summaries from the result of the intraprocedural analysis,
   * represented by the {@link ReachedSet} {@code pReached}.
   * A {@link UseSummaryCPA} may choose to generate as
   * many summaries as it wishes.
   * Implementation can assume that {@link #getWeakenedCallState}
   * was already called on the {@code pCallState}.
   *
   * @param pCallState Summary precondition, associated with
   *                   the call node, which is about to jump into the block.
   * @param pEntryPrecision Entry precision associated with
   *                        {@code pCallState}.
   * @param pReturnStates Return states associated with the block return.
   * @param pReturnPrecisions Return precisions associated with {@code pReturnState},
   *                         in the same order as {@code pReturnStates}.
   * @param pBlock The block for which the summary is generated.
   * @param pEntryNode Entry node for the summarized block.
   * @return summaries which describe the result.
   */
  List<? extends Summary> generateSummaries(
      AbstractState pCallState,
      Precision pEntryPrecision,
      List<? extends AbstractState> pReturnStates,
      List<Precision> pReturnPrecisions,
      CFANode pEntryNode,
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
   * <p>Coverage relation for summaries: precondition may be weakened,
   * postcondition may be strengthened.
   * There is no point in storing a summary with a stronger precondition
   * and a weaker postcondition of an already existing one.
   *
   * <p>An implementation of this interface might be able to provide a
   * more efficient comparison.
   *
   * @return whether {@code pSummary1} is described by {@code pSummary2}.
   */
  boolean isDescribedBy(Summary pSummary1,
                        Summary pSummary2) throws CPAException, InterruptedException;

  /**
   * Communicate the block partitioning to the configurable
   * program analysis.
   */
  default void setBlockManager(BlockManager pManager) {}

  default boolean isSummaryCoveringCallsite(
      Summary pSummary,
      AbstractState pCallsite,
      AbstractDomain pAbstractDomain
  ) throws CPAException, InterruptedException {
    return pAbstractDomain.isLessOrEqual(
        pCallsite,
        projectToCallsite(pSummary)
    );
  }
}
