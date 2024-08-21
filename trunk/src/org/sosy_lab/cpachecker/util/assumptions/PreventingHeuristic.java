// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.assumptions;

import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

/** Enum listing several possible reasons for giving up analysis at a certain point. */
public enum PreventingHeuristic {
  PATHLENGTH("PL"),
  SUCCESSORCOMPTIME("SCT"),
  PATHCOMPTIME("PCT"),
  ASSUMEEDGESINPATH("AEIP"),
  ASSIGNMENTSINPATH("ASIP"),
  REPETITIONSINPATH("RIP"),
  MEMORYUSED("MU"),
  MEMORYOUT("MO"),
  TIMEOUT("TO"),
  LOOPITERATIONS("LI");

  private final String predicateString;

  PreventingHeuristic(String predicateStr) {
    predicateString = predicateStr;
  }

  /** Returns a formula of this reason, which includes the threshold value which was exceeded. */
  public BooleanFormula getFormula(FormulaManagerView fmgr, long thresholdValue) {
    IntegerFormulaManager nfmgr = fmgr.getIntegerFormulaManager();
    final IntegerFormula number = nfmgr.makeNumber(thresholdValue);
    final IntegerFormula var = nfmgr.makeVariable(predicateString);
    // TODO better idea?
    return nfmgr.equal(var, number);
  }
}
