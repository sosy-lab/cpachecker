// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.smt;

import org.junit.Test;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.BitvectorFormula;
import org.sosy_lab.java_smt.api.FloatingPointFormula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.FormulaType.FloatingPointType;

public class FloatingPointFormulaManagerViewTest extends SolverViewBasedTest0 {

  @Override
  protected Solvers solverToUse() {
    return Solvers.MATHSAT5;
  }

  @Override
  protected ConfigurationBuilder createTestConfigBuilder() {
    return super.createTestConfigBuilder()
        .setOption("cpa.predicate.encodeBitvectorAs", "INTEGER")
        .setOption("cpa.predicate.encodeFloatAs", "FLOAT");
  }

  /** The bits of a floating point number are an integer that wraps a bitvector. */
  @Test
  public void testFromIeeeBitvectorWithIntegerEncoding() throws Exception {
    FloatingPointFormulaManagerView fpmgrv = mgrv.getFloatingPointFormulaManager();
    FloatingPointType type = FormulaType.getSinglePrecisionFloatingPointType();
    BitvectorFormula bitsOfOne = mgrv.getBitvectorFormulaManager().makeBitvector(32, 0x3f800000L);

    FloatingPointFormula one = fpmgrv.fromIeeeBitvector(bitsOfOne, type);

    assertThatFormula(fpmgrv.equalWithFPSemantics(one, fpmgrv.makeNumber(1.0, type)))
        .isTautological();
  }
}
