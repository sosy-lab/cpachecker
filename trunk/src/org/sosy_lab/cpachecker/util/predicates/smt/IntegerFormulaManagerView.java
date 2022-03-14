// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import java.math.BigInteger;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.IntegerFormulaManager;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

public class IntegerFormulaManagerView
    extends NumeralFormulaManagerView<IntegerFormula, IntegerFormula>
    implements IntegerFormulaManager {
  private final IntegerFormulaManager integerFormulaManager;

  IntegerFormulaManagerView(
      FormulaWrappingHandler pWrappingHandler, IntegerFormulaManager pManager) {
    super(pWrappingHandler, pManager);
    integerFormulaManager = pManager;
  }

  @Override
  public BooleanFormula modularCongruence(IntegerFormula number1, IntegerFormula number2, long n) {
    return integerFormulaManager.modularCongruence(number1, number2, n);
  }

  @Override
  public BooleanFormula modularCongruence(
      IntegerFormula number1, IntegerFormula number2, BigInteger n) {
    return integerFormulaManager.modularCongruence(number1, number2, n);
  }

  @Override
  public IntegerFormula modulo(IntegerFormula pNumber1, IntegerFormula pNumber2) {
    return integerFormulaManager.modulo(pNumber1, pNumber2);
  }
}
