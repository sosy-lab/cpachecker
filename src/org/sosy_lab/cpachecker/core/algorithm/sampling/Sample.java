// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.sampling;

import java.util.Map;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class Sample {

  // Formula that was used to create the sample
  private final BooleanFormula pathFormula;
  // Formula manager which created the stored path formula
  private final FormulaManagerView fmgr;
  // Mapping of variables to their respective values.
  // Supported are boolean or numeral values (Rational/Double/BigInteger/Long/Integer)
  private final Map<MemoryLocation, Object> variableValues;

  public Sample(
      BooleanFormula pPathFormula,
      FormulaManagerView pFmgr,
      Map<MemoryLocation, Object> pVariableValues) {
    pathFormula = pPathFormula;
    fmgr = pFmgr;
    variableValues = pVariableValues;
  }

  /** Gets the stored path formula for the original formula manager. */
  public BooleanFormula getPathFormula() {
    return pathFormula;
  }

  /** Gets the stored path formula for the given formula manager. */
  public BooleanFormula getPathFormula(FormulaManagerView pFmgr) {
    return pFmgr.translateFrom(pathFormula, fmgr);
  }

  public Map<MemoryLocation, Object> getVariableValues() {
    return variableValues;
  }

  @Override
  public String toString() {
    return variableValues.toString();
  }
}
