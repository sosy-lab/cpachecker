/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.predicate;

import org.sosy_lab.common.collect.PersistentMap;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.defaults.EmptyEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractStateWithEdge;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState.NonAbstractionState;
import org.sosy_lab.cpachecker.util.predicates.AbstractionFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;

public class PredicateNonAbstractionStateWithEdge extends NonAbstractionState
    implements AbstractStateWithEdge {

  private static final long serialVersionUID = -3840974182711165535L;

  private final AbstractEdge edge;

  PredicateNonAbstractionStateWithEdge(PathFormula pF, AbstractionFormula pA,
      PersistentMap<CFANode, Integer> pAbstractionLocations,
      AbstractEdge pEdge) {
    super(pF, pA, pAbstractionLocations);
    edge = pEdge;
  }

  PredicateNonAbstractionStateWithEdge(PredicateAbstractState pState, AbstractEdge pEdge) {
    this(
        pState.getPathFormula(),
        pState.getAbstractionFormula(),
        pState.getAbstractionLocationsOnPath(),
        pEdge);
  }

  @Override
  public AbstractEdge getAbstractEdge() {
    return edge;
  }

  @Override
  public boolean hasEmptyEffect() {
    return edge == EmptyEdge.getInstance();
  }

  @Override
  public boolean isProjection() {
    return false;
  }

}
