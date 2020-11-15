// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import java.util.Objects;
import java.util.function.Supplier;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class ModelValue {

  private final String variableName;

  private final Supplier<String> textualRepresentation;

  private final String formula;

  public ModelValue(
      String pVariableName, String pFormula, Supplier<String> pTextualRepresentation) {
    variableName = Objects.requireNonNull(pVariableName);
    formula = Objects.requireNonNull(pFormula);
    textualRepresentation = Objects.requireNonNull(pTextualRepresentation);
  }

  public String getVariableName() {
    return variableName;
  }

  @Override
  public String toString() {
    return textualRepresentation.get();
  }

  @Override
  public boolean equals(Object pOther) {
    if (this == pOther) {
      return true;
    }
    if (pOther instanceof ModelValue) {
      ModelValue other = (ModelValue) pOther;
      return variableName.equals(other.variableName) && formula.equals(other.formula);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hash(variableName, formula);
  }

  public BooleanFormula toAssignment(FormulaManagerView pFMGR) {
    return pFMGR.parse(formula);
  }
}