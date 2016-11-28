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
package org.sosy_lab.cpachecker.cpa.summary;

import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

/**
 * Signal the desire to recompute the summary.
 */
public class ToComputeSummaryState
    implements AbstractState,
               Partitionable {

  /**
   * State associated with {@link FunctionEntryNode},
   * <em>inside</em> the function.
   */
  private final AbstractState functionEntryState;

  /**
   * Precision associated with {@link FunctionEntryNode}.
   */
  private final Precision functionEntryPrecision;

  /**
   * Function entry node for this summary.
   */
  private final FunctionEntryNode node;

  public ToComputeSummaryState(
      AbstractState pFunctionEntryState,
      Precision pFunctionEntryPrecision,
      FunctionEntryNode pNode) {
    functionEntryState = pFunctionEntryState;
    functionEntryPrecision = pFunctionEntryPrecision;
    node = pNode;
  }

  public String getFunctionName() {
    return node.getFunctionName();
  }

  public AbstractState getState() {
    return functionEntryState;
  }

  public Precision getPrecision() {
    return functionEntryPrecision;
  }

  @Override
  public Object getPartitionKey() {

    // Do not merge, do not compare.
    return this;
  }
}
