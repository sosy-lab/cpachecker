// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;

/**
 * An interface which is provided by a configurable program analysis using {@link
 * ConfigurableProgramAnalysisWithBAM} which allows it to use BAM memoization framework.
 */
public interface Reducer {

  /**
   * Return an over-approximation of {@code expandedState}, discarding all information which is not
   * relevant to the block {@code context}.
   *
   * @param expandedState Input state to be reduced.
   * @param context Block with respect to which the reduction is performed.
   * @param callNode Function call node for the block.
   */
  AbstractState getVariableReducedState(
      AbstractState expandedState, Block context, CFANode callNode) throws InterruptedException;

  /**
   * Perform the opposite of the reduction: return an under-approximation of the state {@code
   * reducedState} which includes constraints from {@code rootState}, where all of the added
   * constraints are irrelevant to {@code reducedContext}.
   *
   * @param rootState State which was not reduced, and contains the global information, some of
   *     which was reduced before due to its irrelevancy to the block {@code reducedContext}.
   * @param reducedContext Block with respect to which the reduction was performed.
   * @param reducedState Input state to be expanded.
   * @return the expanded state or <code>Null</code> if the state becomes unsatisfiable during
   *     expansion
   */
  AbstractState getVariableExpandedState(
      AbstractState rootState, Block reducedContext, AbstractState reducedState)
      throws InterruptedException;

  Precision getVariableReducedPrecision(Precision precision, Block context);

  Precision getVariableExpandedPrecision(
      Precision rootPrecision, Block rootContext, Precision reducedPrecision);

  /**
   * Returns a hashable object for the stateKey and the precisionKey. This object is used to
   * identify elements in the <code> BAMCache.AbstractStateHash </code>.
   */
  Object getHashCodeForState(AbstractState stateKey, Precision precisionKey);

  /**
   * Returns value (non-negative) for the difference between two precisions.
   *
   * <p>This function is used only when {@code cpa.bam.aggressiveCaching} is enabled (cf. {@link
   * org.sosy_lab.cpachecker.cpa.bam.cache.BAMCache#get(AbstractState, Precision, Block)
   * BAMCache.get}).
   *
   * <p>A greater value indicates a bigger difference in the precision. If the implementation of
   * this function is not important, return zero.
   *
   * @param pPrecision Precision object.
   * @param pOtherPrecision Other precision object.
   */
  default int measurePrecisionDifference(Precision pPrecision, Precision pOtherPrecision) {
    return 0;
  }

  default AbstractState getVariableReducedStateForProofChecking(
      AbstractState expandedState, Block context, CFANode callNode) throws InterruptedException {
    return getVariableReducedState(expandedState, context, callNode);
  }

  default AbstractState getVariableExpandedStateForProofChecking(
      AbstractState rootState, Block reducedContext, AbstractState reducedState)
      throws InterruptedException {
    return getVariableExpandedState(rootState, reducedContext, reducedState);
  }

  /**
   * Use the expandedState as basis for a new state, that can be used as rebuildState for the next
   * function-return-edge.
   *
   * @param rootState state before the function-call. this is the predecessor of the
   *     block-start-state, that will be reduced.
   * @param entryState state after the function-call. this is the block-start-state, that will be
   *     reduced.
   * @param expandedState expanded state at function-return, before the function-return-dge.
   * @param exitLocation location of expandedState and also reducedExitState, must be the location
   *     of rebuildState, TODO should be instance of FunctionExitNode?
   *     <pre>{@code
   *                                             +---------- BLOCK ----------+
   *                                             |                           |
   * rootState ---------------> entryState - - - - - -> reducedEntryState    |
   *     |     functionCallEdge               reduce          |              |
   *     |                                       |            V              |
   *     |function-                              |         function-         |
   *     |summary-                               |         execution         |
   *     |edge                                   |            |              |
   *     |                                    expand          V              |
   *     |                     expandedState <- - - - - reducedExitState     |
   *     |                         | | |         |                           |
   *     V     functionReturnEdge  V V V         +---------------------------+
   * returnState <------------  rebuildState
   * }</pre>
   */
  AbstractState rebuildStateAfterFunctionCall(
      AbstractState rootState,
      AbstractState entryState,
      AbstractState expandedState,
      FunctionExitNode exitLocation);

  /**
   * See option bam.useDynamicAdjustment
   *
   * @param pState an abstract state which might be used in cache
   */
  default boolean canBeUsedInCache(AbstractState pState) {
    return true;
  }
}
