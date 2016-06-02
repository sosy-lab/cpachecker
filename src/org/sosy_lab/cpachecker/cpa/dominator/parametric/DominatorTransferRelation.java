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
package org.sosy_lab.cpachecker.cpa.dominator.parametric;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import java.util.ArrayList;
import java.util.Collection;

public class DominatorTransferRelation extends SingleEdgeTransferRelation {

  private final ConfigurableProgramAnalysis cpa;

  public DominatorTransferRelation(ConfigurableProgramAnalysis cpa) {
    if (cpa == null) {
      throw new IllegalArgumentException("cpa is null!");
    }

    this.cpa = cpa;
  }

  @Override
  public Collection<DominatorState> getAbstractSuccessorsForEdge(
    AbstractState element, Precision prec, CFAEdge cfaEdge) throws CPATransferException, InterruptedException {

    assert element instanceof DominatorState;

    DominatorState dominatorState = (DominatorState)element;

    Collection<? extends AbstractState> successorsOfDominatedElement =
        this.cpa.getTransferRelation().getAbstractSuccessorsForEdge(
            dominatorState.getDominatedState(), prec, cfaEdge);

    Collection<DominatorState> successors = new ArrayList<>(successorsOfDominatedElement.size());
    for (AbstractState successorOfDominatedElement : successorsOfDominatedElement) {
      DominatorState successor = new DominatorState(successorOfDominatedElement, dominatorState);
      successor.update(successorOfDominatedElement);
      successors.add(successor);
    }

    return successors;
  }
}
