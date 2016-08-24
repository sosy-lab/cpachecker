/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.interfaces;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;


/**
 * An interface which is provided by a configurable program analysis using
 * {@link ConfigurableProgramAnalysisWithBAM} which allows it to use BAM
 * memoization framework.
 */
public interface Reducer {

  /**
   * Return an over-approximation of {@code expandedState},
   * discarding all information which is not relevant to the block
   * {@code context}.
   *
   * @param expandedState Input state to be reduced.
   * @param context Block with respect to which the reduction is performed.
   * @param callNode Function call node for the block.
   */
  AbstractState getVariableReducedState(
      AbstractState expandedState,
      Block context,
      CFANode callNode)
      throws InterruptedException;

  /**
   * Perform the opposite of the reduction: return an under-approximation of
   * the state {@code reducedState} which includes constraints from
   * {@code rootState}, where all of the added constraints are irrelevant to
   * {@code reducedContext}.
   *
   * @param rootState State which was not reduced, and contains the global
   *                  information, some of which was reduced before due to
   *                  its irrelevancy to the block {@code reducedContext}.
   * @param reducedContext Block with respect to which the reduction was
   *                       performed.
   * @param reducedState Input state to be expanded.
   */
  AbstractState getVariableExpandedState(AbstractState rootState, Block reducedContext, AbstractState reducedState)
      throws InterruptedException;

  Precision getVariableReducedPrecision(Precision precision, Block context);

  Precision getVariableExpandedPrecision(Precision rootPrecision, Block rootContext, Precision reducedPrecision);

  /** Returns a hashable object for the stateKey and the precisionKey.
   * This object is used to identify elements in the
   * <code> BAMCache.AbstractStateHash </code>. */
  Object getHashCodeForState(AbstractState stateKey, Precision precisionKey);

  /**
   * @param pPrecision Precision object.
   * @param pOtherPrecision Other precision object.
   *
   * @return value (non-negative) for the difference between two
   * precisions.
   *
   * <p>This function is used only when {@code cpa.bam.aggressiveCaching} is
   * enabled (cf. {@link org.sosy_lab.cpachecker.cpa.bam.BAMCache#get(AbstractState, Precision, Block) BAMCache.get}).
   *
   * <p>A greater value indicates a bigger difference in the precision.
   * If the implementation of this function is not important, return zero. */
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
   * Use the expandedState as basis for a new state,
   * that can be used as rebuildState for the next function-return-edge.
   *
   * @param rootState state before the function-call. this is the predecessor of the block-start-state, that will be reduced.
   * @param entryState state after the function-call. this is the block-start-state, that will be reduced.
   * @param expandedState expanded state at function-return, before the function-return-dge.
   * @param exitLocation location of expandedState and also reducedExitState,
   *                     must be the location of rebuildState,
   *                     TODO should be instance of FunctionExitNode?
   *
   * <pre>
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
   * </pre>
   */
  AbstractState rebuildStateAfterFunctionCall(AbstractState rootState, AbstractState entryState,
      AbstractState expandedState, FunctionExitNode exitLocation);
  }
