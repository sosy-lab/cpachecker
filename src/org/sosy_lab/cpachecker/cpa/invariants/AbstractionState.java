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

import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.invariants.formula.BooleanFormula;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;


interface AbstractionState {

  /**
   * Determine on which variables to use abstraction when merging two
   * invariants states having this and the given abstraction state.
   *
   * @param pOther the other abstraction state.
   * @return the set of widening targets.
   */
  public Set<MemoryLocation> determineWideningTargets(AbstractionState pOther);

  public Set<BooleanFormula<CompoundInterval>> getWideningHints();

  public AbstractionState addEnteringEdge(CFAEdge pEdge);

  public AbstractionState join(AbstractionState pOther);

  public boolean isLessThanOrEqualTo(AbstractionState pOther);

}