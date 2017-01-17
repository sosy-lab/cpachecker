/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

import java.util.List;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.summary.blocks.Block;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Interface CPA has to satisfy in order to support the new summary framework.
 */
public interface SummaryManager {

  /**
   * Perform the function application using the summary
   * derived from the exit site of the called function.
   *
   * @param callSite Abstract state associated with the calling location.
   * @param exitState Abstract state associated with function exit,
   *                  effectively encodes the summary.
   * @param callNode Node which performs the call.
   * @param calledBlock Block wrapping the called function.
   * @return Result of the function application:
   * intersection of {@code callSite} with variables modified
   * in {@code calledBlock} projected out,
   * and {@code exitState}, with input variables renamed to match the parameters
   * of the call from {@code callSite},
   * and output variables renamed to match the return edge
   * associated with {@code callNode}.
   */
  List<? extends AbstractState> applyFunctionSummary(
      AbstractState callSite,
      AbstractState exitState,
      CFANode callNode,
      Block calledBlock
  ) throws CPAException, InterruptedException;

  /**
   * Get entry state for the function from the callsite.
   *
   * @param callSite abstract state at the callsite.
   * @param callNode node associated with the function call.
   * @param calledBlock block enclosing the called function.
   *
   * @return {@code callSite} with parameters not read in the function
   * projected out, and function parameters associated with {@code callNode}
   * renamed.
   */
  List<? extends AbstractState> getEntryStates(
      AbstractState callSite,
      CFANode callNode,
      Block calledBlock
  ) throws CPAException, InterruptedException;

  /**
   * @param callSite Abstract state at the calling site.
   * @param exitState Abstract state associated with the function exit,
   *                  encoding summary.
   * @param callNode Node from which there is an outgoing function call.
   * @param calledBlock Block enclosing the called function.
   *
   * @return whether the summary encoded by {@code exitState}
   * is applicable at {@code callSite}.
   *
   * <p>Exists purely as an optimization: otherwise function
   * application should create an empty state.
   *
   */
  default boolean isSummaryApplicable(
      AbstractState callSite,
      AbstractState exitState,
      CFANode callNode,
      Block calledBlock
  ) {
    return true;
  }
}
