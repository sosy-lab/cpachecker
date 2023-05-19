// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc.candidateinvariants;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.FluentIterable;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.bmc.BMCHelper;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public enum TargetLocationCandidateInvariant implements CandidateInvariant {
  INSTANCE;

  @Override
  public BooleanFormula getFormula(
      FormulaManagerView pFMGR, PathFormulaManager pPFMGR, PathFormula pContext) {
    return pFMGR.getBooleanFormulaManager().makeFalse();
  }

  @Override
  public BooleanFormula getAssertion(
      Iterable<AbstractState> pReachedSet, FormulaManagerView pFMGR, PathFormulaManager pPFMGR)
      throws InterruptedException {
    Iterable<AbstractState> targetStates = filterApplicable(pReachedSet);
    return pFMGR
        .getBooleanFormulaManager()
        .not(BMCHelper.createFormulaFor(targetStates, pFMGR.getBooleanFormulaManager()));
  }

  @Override
  public void assumeTruth(ReachedSet pReachedSet) {
    Iterable<AbstractState> targetStates = filterApplicable(pReachedSet).toList();
    pReachedSet.removeAll(targetStates);
    for (ARGState s : from(targetStates).filter(ARGState.class)) {
      s.removeFromARG();
    }
  }

  @Override
  public String toString() {
    return "No target locations reachable";
  }

  @Override
  public boolean appliesTo(CFANode pLocation) {
    return true;
  }

  @Override
  public FluentIterable<AbstractState> filterApplicable(Iterable<AbstractState> pStates) {
    return from(pStates).filter(AbstractStates::isTargetState);
  }
}
