// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.acsltoformula;

import org.junit.Test;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;

import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslTerm;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.cpachecker.util.test.TestDataTools;
import org.sosy_lab.java_smt.api.Formula;

@SuppressWarnings("unused")
public class AcslTermToFromulaConverterTest {

  final LogManager logger = LogManager.createTestLogManager();

  private Solver createSolver() throws InvalidConfigurationException {
    Configuration config = TestDataTools.configurationForTest().build();
    return Solver.create(config, logger, ShutdownNotifier.createDummy());
  }

  // This is just a dummy for now
  @Test
  public void testAcslXYZTerm() throws InvalidConfigurationException {
    Solver smtSolver = createSolver();
    FormulaManagerView fmgr = smtSolver.getFormulaManager();

    AcslTerm term = null;  // create an AcslTerm to test here

    // Formula f = AcslTermToFormulaConverter.convertAcslTerm(term, fmgr);
    // TODO: How do I work with a solver here?
    // checks like assertThat(smtSolver.isUnsat(f)).isTrue(); require a BooleanFormula
    // but not everything ACSL can express is boolean, right?
    // So I probably need to handle the solver differently here.

  }
}
