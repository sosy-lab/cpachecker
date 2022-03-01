// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.state_transformer;

import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class FormulaTransformer {

  private final PathFormulaManagerImpl pathFormulaManager;
  private final FormulaManagerView fmgr;

  public FormulaTransformer(FormulaManagerView pFmgr, PathFormulaManagerImpl pPathFormulaManager) {
    fmgr = pFmgr;
    pathFormulaManager = pPathFormulaManager;
  }

  public <T extends AbstractState> T transformIfPossible(Class<T> pStateClass, T pState, BooleanFormula pFormula) {
    if (pState instanceof PredicateAbstractState) {
      return pStateClass.cast(PredicateAbstractState.mkNonAbstractionStateWithNewPathFormula(
          pathFormulaManager.makeAnd(pathFormulaManager.makeEmptyPathFormula(),
              pFormula),
          (PredicateAbstractState) pState));
    }
    return pState;
  }


}
