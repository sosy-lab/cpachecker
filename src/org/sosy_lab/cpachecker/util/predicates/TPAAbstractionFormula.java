// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates;

import java.util.Set;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.regions.Region;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class TPAAbstractionFormula extends AbstractionFormula {

//  private final ImmutableList<String> primeVariable;

//  private final ImmutableMap<String, String> varToPrimeVarPredicate;

  public TPAAbstractionFormula(
      FormulaManagerView mgr,
      Region pRegion,
      BooleanFormula pFormula,
      BooleanFormula pInstantiatedFormula,
      PathFormula pBlockFormula,
      Set<Integer> pIdOfStoredAbstractionReused) {
    super(mgr, pRegion, pFormula, pInstantiatedFormula, pBlockFormula,
        pIdOfStoredAbstractionReused);
  }
}
