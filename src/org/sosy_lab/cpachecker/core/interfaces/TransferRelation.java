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

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Interface for transfer relations.
 * The transfer relation is used to compute the successors of abstract states
 * (post operation).
 *
 * Most CPAs can use
 * {@link org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation}
 * as base class.
 */
public interface TransferRelation {

  /**
   * Get all successors of the current abstract state.
   *
   * Note that most CPAs do not need this method and should only implement
   * {@link #getAbstractSuccessorsForEdge(AbstractState, Precision, CFAEdge)}.
   *
   * @param state current abstract state
   * @param precision precision for abstract state
   * @return collection of all successors of the current state (may be empty)
   */
  Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState state,
      Precision precision)
          throws CPATransferException, InterruptedException;

  /**
   * Get all successors of the current abstract state for a given single CFA edge.
   *
   * This is an optimization.
   * In theory, we could ask all CPAs for all abstract successors
   * regarding any CFA edge.
   * One CPA tracks the program counter and would return bottom (no successor)
   * for all edges that cannot be applied at the current location.
   * By taking the cross product of the returned states of all CPAs,
   * the states for the invalid edges would be filtered out.
   * Of course this is inefficient,
   * thus we pass the edge we are currently interested in to the other CPAs.
   *
   * Most CPAs only need this method and can extend
   * {@link org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation}.
   *
   * @param state current abstract state
   * @param precision precision for abstract state
   * @param cfaEdge the edge for which the successors should be computed
   * @return collection of all successors of the current state (may be empty)
   */
  Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state,
      Precision precision,
      CFAEdge cfaEdge)
          throws CPATransferException, InterruptedException;

  /**
   * Updates an abstract state with information from the abstract states of
   * other CPAs. An implementation of this method should only modify the
   * abstract state of the domain it belongs to.
   * @param state abstract state of the current domain
   * @param otherStates list of abstract states of all domains
   * @param cfaEdge null or an edge of the CFA
   * @param precision the precision to use during strengthening
   * @return list of all abstract states which should replace the old one, empty list for bottom or.
   * @throws CPATransferException If operation fails.
   * @throws InterruptedException If operation is interrupted.
   */
  default Collection<? extends AbstractState> strengthen(
      AbstractState state,
      List<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision precision)
      throws CPATransferException, InterruptedException {
    return Collections.singleton(state);
  }
}
