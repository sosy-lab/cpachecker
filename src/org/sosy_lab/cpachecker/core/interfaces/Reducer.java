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
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;


public interface Reducer {

  AbstractState getVariableReducedState(AbstractState expandedState, Block context, CFANode callNode);

  AbstractState getVariableExpandedState(AbstractState rootState, Block reducedContext, AbstractState reducedState);

  Precision getVariableReducedPrecision(Precision precision, Block context);

  Precision getVariableExpandedPrecision(Precision rootPrecision, Block rootContext, Precision reducedPrecision);

  /** Returns a hashable object for the stateKey and the precisionKey.
   * This object is used to identify elements in the
   * {@link org.sosy_lab.cpachecker.cpa.bam.BAMCache.AbstractStateHash AbstractStateHash}. */
  Object getHashCodeForState(AbstractState stateKey, Precision precisionKey);

  /** Returns a (non-negative) value for the difference between two precisions.
   * This function is called for aggressive caching
   * (see {@link org.sosy_lab.cpachecker.cpa.bam.BAMCache#get(AbstractState, Precision, Block) BAMCache.get}).
   * A greater value indicates a bigger difference in the precision.
   * If the implementation of this function is not important, return zero. */
  int measurePrecisionDifference(Precision pPrecision, Precision pOtherPrecision);

  AbstractState getVariableReducedStateForProofChecking(AbstractState expandedState, Block context, CFANode callNode);

  AbstractState getVariableExpandedStateForProofChecking(AbstractState rootState, Block reducedContext, AbstractState reducedState);

  /** Special version of TransferRelation, needed for recursive functioncalls.
   * Parameters of the functioncall might be equal to variables in the calling function,
   * the transfer must handle this case.
   * The returned state should be reduced in this function.
   * @return the state as if the TransferRelation would have executed one step on the edge. */
  // TODO do we need a set of states as returnvalue?
  @Deprecated
  AbstractState getReducedStateAfterFunctionCall(
          AbstractState expandedState, Block context, FunctionCallEdge edge)
          throws UnrecognizedCodeException;

  /** Special version of TransferRelation, needed for recursive functioncalls.
   * Return-values of the functioncall might be equal to variables in the calling function,
   * the transfer must handle this case.
   * The returned state should be expanded while executing this transfer.
   * @return the state as if the TransferRelation would have executed one step on the edge
   * or NULL, iff there is no successor-state. */
  // TODO do we need a set of states as returnvalue?
  @Deprecated
  AbstractState getExpandedStateAfterFunctionReturn(
          AbstractState rootState, Block reducedContext, AbstractState reducedState, FunctionReturnEdge edge)
          throws UnrecognizedCodeException;

  /**
   * @param rootState state before the function-call, this is the predecessor of the block-start-state.
   * @param expandedState expanded state at function-return */
  AbstractState rebuildStateAfterFunctionCall(AbstractState rootState, AbstractState expandedState);
  }
