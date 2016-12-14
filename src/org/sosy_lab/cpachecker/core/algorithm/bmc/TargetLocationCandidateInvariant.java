/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.bmc;

import static com.google.common.collect.FluentIterable.from;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

import java.util.Set;

public class TargetLocationCandidateInvariant extends AbstractLocationFormulaInvariant {

  public TargetLocationCandidateInvariant(Set<CFANode> pLocations) {
    super(pLocations);
  }

  @Override
  public BooleanFormula getFormula(
      FormulaManagerView pFMGR, PathFormulaManager pPFMGR, PathFormula pContext)
      throws CPATransferException, InterruptedException {
    return pFMGR.getBooleanFormulaManager().makeFalse();
  }

  @Override
  public BooleanFormula getAssertion(
      Iterable<AbstractState> pReachedSet,
      FormulaManagerView pFMGR,
      PathFormulaManager pPFMGR,
      int pDefaultIndex) {
    Iterable<AbstractState> targetStates = from(pReachedSet).filter(AbstractStates.IS_TARGET_STATE);
    return pFMGR.getBooleanFormulaManager().not(
        BMCHelper.createFormulaFor(targetStates, pFMGR.getBooleanFormulaManager()));
  }

  @Override
  public void assumeTruth(ReachedSet pReachedSet) {
    Iterable<AbstractState> targetStates = from(pReachedSet).filter(AbstractStates.IS_TARGET_STATE).toList();
    pReachedSet.removeAll(targetStates);
    for (ARGState s : from(targetStates).filter(ARGState.class)) {
      s.removeFromARG();
    }
  }

  @Override
  public String toString() {
    return "No target locations reachable from: " + getLocations();
  }

  @Override
  public int hashCode() {
    return getLocations().hashCode();
  }

  @Override
  public boolean equals(Object pObj) {
    if (this == pObj) {
      return true;
    }
    if (pObj instanceof TargetLocationCandidateInvariant) {
      return getLocations().equals(((TargetLocationCandidateInvariant) pObj).getLocations());
    }
    return false;
  }

}
