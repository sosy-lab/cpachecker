// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import org.sosy_lab.java_smt.api.NumeralFormula;
import org.sosy_lab.java_smt.api.NumeralFormula.RationalFormula;
import org.sosy_lab.java_smt.api.NumeralFormulaManager;
import org.sosy_lab.java_smt.api.RationalFormulaManager;

public class RationalFormulaManagerView
    extends NumeralFormulaManagerView<NumeralFormula, RationalFormula>
    implements RationalFormulaManager {
  RationalFormulaManagerView(
      FormulaWrappingHandler pWrappingHandler,
      NumeralFormulaManager<NumeralFormula, RationalFormula> pManager) {
    super(pWrappingHandler, pManager);
  }
}
