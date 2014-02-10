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

import java.util.Collection;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * Interface for transfer relations.
 * The instance of the relation is used to calculate the post operation
 */
public interface TransferRelation {

  /**
   * Gets all successors of the current state. If cfaEdge is null, all edges
   * of the CFA may be checked if they lead to successors, otherwise only the
   * specified edge should be handled.
   * @param state abstract state with current state
   * @param cfaEdge null or an edge of the CFA
   * @return list of all successors of the current state (may be empty)
   */
  public Collection<? extends AbstractState> getAbstractSuccessors(AbstractState state, Precision precision, CFAEdge cfaEdge)
    throws CPATransferException, InterruptedException;

  /**
   * Updates an abstract state with information from the abstract states of
   * other CPAs. An implementation of this method should only modify the
   * abstract state of the domain it belongs to.
   * @param state abstract state of the current domain
   * @param otherStates list of abstract states of all domains
   * @param cfaEdge the current edge of the CFA
   * @param precision
   * @return list of all abstract states which should replace the old one, empty list for bottom or null for no change.
   */
  public Collection<? extends AbstractState> strengthen(AbstractState state,
                                     List<AbstractState> otherStates,
                                     CFAEdge cfaEdge,
                                     Precision precision)
                                     throws CPATransferException,
                                            InterruptedException;
}
