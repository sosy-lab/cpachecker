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
package org.sosy_lab.cpachecker.cpa.invariants;

interface AbstractionStrategy {

  /**
   * Gets an abstraction state with no specific data.
   *
   * @return an abstraction state with no specific data.
   */
  public AbstractionState getAbstractionState();

  /**
   * Gets an abstraction state that represents the successor of the given
   * abstraction state.
   *
   * @param pPrevious the preceding state.
   *
   * @return an abstraction state that represents the successor of the given
   * abstraction state.
   */
  public AbstractionState getSuccessorState(AbstractionState pPrevious);

  /**
   * Gets an abstraction state that resembles the given abstraction state as
   * close as this factory allows.
   *
   * @param pOther the state to be represented.
   *
   * @return an abstraction state that resembles the given abstraction state as
   * close as this factory allows.
   */
  public AbstractionState from(AbstractionState pOther);

}