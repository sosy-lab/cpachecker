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
package org.sosy_lab.cpachecker.cpa.summary.summaryUsage;

import org.sosy_lab.cpachecker.cfa.blocks.Block;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

/**
 * Request for the summary computation,
 * appears in the same reached set as intraprocedural states.
 */
public class SummaryComputationRequestState implements AbstractState, Partitionable {

  private final AbstractState callingContext;
  private final AbstractState functionEntryState;
  private final Precision functionEntryPrecision;
  private final Block block;

  SummaryComputationRequestState(
      AbstractState pCallingContext,
      AbstractState pFunctionEntryState,
      Precision pFunctionEntryPrecision,
      Block pBlock) {
    functionEntryState = pFunctionEntryState;
    functionEntryPrecision = pFunctionEntryPrecision;
    callingContext = pCallingContext;
    block = pBlock;
  }

  /**
   * @return state from which the outgoing
   * {@link org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge}
   * was going into {@link #getFunctionEntryState()}.
   */
  public AbstractState getCallingContext() {
    return callingContext;
  }

  /**
   * @return state associated with {@link org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode},
   * first state in the function.
   */
  public AbstractState getFunctionEntryState() {
    return functionEntryState;
  }

  /**
   * @return precision associated with {@link #getFunctionEntryState()}.
   */
  public Precision getFunctionEntryPrecision() {
    return functionEntryPrecision;
  }

  /**
   * @return syntactical bounds for the summarized block.
   */
  public Block getBlock() {
    return block;
  }

  @Override
  public Object getPartitionKey() {

    // todo: might we want to group several ones? probably there is only one
    // at a single time.
    return this;
  }
}
