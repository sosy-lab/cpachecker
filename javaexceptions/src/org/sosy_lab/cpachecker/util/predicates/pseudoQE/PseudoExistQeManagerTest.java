// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pseudoQE;

import static com.google.common.truth.Truth.assertThat;
import static org.sosy_lab.java_smt.api.FormulaType.IntegerType;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.util.predicates.pseudoQE.PseudoExistQeManager.SolverQeTactic;
import org.sosy_lab.cpachecker.util.predicates.smt.SolverViewBasedTest0;
import org.sosy_lab.java_smt.SolverContextFactory.Solvers;
import org.sosy_lab.java_smt.api.ArrayFormula;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

@RunWith(Parameterized.class)
public class PseudoExistQeManagerTest extends SolverViewBasedTest0 {

  // Princess does not store the formulas in a consistent way
  // So it is difficult to write test
  @Parameters(name = "{0}")
  public static Object[] getTestSolvers() {
    return new Object[] {Solvers.MATHSAT5, Solvers.Z3, Solvers.SMTINTERPOL};
  }

  @Parameter(0)
  public Solvers solverUnderTest;

  @Override
  protected Solvers solverToUse() {
    return solverUnderTest;
  }

  private PseudoExistQeManager pQEmgr;

  /** Initialize the QE Manager with selected configuration options. */
  private void initQeManager(boolean der, boolean upd, SolverQeTactic qeTactic, boolean overapprox)
      throws InvalidConfigurationException {
    Configuration testConfig =
        Configuration.builder()
            .copyFrom(config)
            .setOption("cpa.predicate.pseudoExistQE.useDER", der ? "true" : "false")
            .setOption("cpa.predicate.pseudoExistQE.useUPD", upd ? "true" : "false")
            .setOption("cpa.predicate.pseudoExistQE.solverQeTactic", qeTactic.name())
            .setOption("cpa.predicate.pseudoExistQE.overapprox", overapprox ? "true" : "false")
            .build();

    pQEmgr = new PseudoExistQeManager(solver, testConfig, logger);
  }

  @Before
  public void setUp() throws InvalidConfigurationException {
    pQEmgr = new PseudoExistQeManager(solver, config, logger);
  }

  // Assert ex x. x=5 and y>x  after QE is y>5
  @Test
  public void testDERSimpleInteger() throws Exception {
    // Create the Formula: x=5 and y>x
    IntegerFormula x = mgrv.makeVariable(FormulaType.IntegerType, "x");
    IntegerFormula y = mgrv.makeVariable(FormulaType.IntegerType, "y");
    BooleanFormula t1 = mgrv.makeEqual(x, mgrv.getIntegerFormulaManager().makeNumber(5));
    BooleanFormula t2 = mgrv.makeGreaterThan(y, x, true);
    BooleanFormula toExist = mgrv.makeAnd(t1, t2);

    Map<String, Formula> boundVars = new HashMap<>();
    boundVars.put("x", x);

    // Prepare Input
    PseudoExistFormula input = new PseudoExistFormula(boundVars, toExist, mgrv);

    // Prepare expectedResult formula
    BooleanFormula expectedResult =
        mgrv.makeGreaterThan(y, mgrv.getIntegerFormulaManager().makeNumber(5), true);

    PseudoExistFormula result = pQEmgr.applyDER(input);

    assertThat(result.hasQuantifiers()).isFalse(); // no longer quantified
    assertThatFormula(result.getInnerFormula()).isEquivalentTo(expectedResult);

    // TODO: Ask if okay?
    // Catching an (of course better) exception for eliminateQuantifiers,
    // if not solved its also correct behavior, but not the thing I want to test here
    // Reason for this is inside the SMTSolver so no way to assure desired behavior in
    // future releases
  }

  // Assert ex y. x=5 and y>4  after QE is x=5
  @Test
  public void testUPDSimpleInteger() throws Exception {
    // Create the Formula: x=5 and y>4
    IntegerFormula x = mgrv.makeVariable(FormulaType.IntegerType, "x");
    IntegerFormula y = mgrv.makeVariable(FormulaType.IntegerType, "y");
    BooleanFormula t1 = mgrv.makeEqual(x, mgrv.getIntegerFormulaManager().makeNumber(5));
    BooleanFormula t2 =
        mgrv.makeGreaterThan(y, mgrv.getIntegerFormulaManager().makeNumber(4), true);
    BooleanFormula toExist = mgrv.makeAnd(t1, t2);

    Map<String, Formula> boundVars = new HashMap<>();
    boundVars.put("y", y);

    // Prepare Input
    PseudoExistFormula input = new PseudoExistFormula(boundVars, toExist, mgrv);

    // Prepare expectedResult formula
    BooleanFormula expectedResult = t1;

    PseudoExistFormula result = pQEmgr.applyUPD(input);

    assertThat(result.hasQuantifiers()).isFalse(); // no longer quantified
    assertThatFormula(result.getInnerFormula()).isEquivalentTo(expectedResult);
  }

  @Test
  public void testArrayDER() throws Exception {
    requireArrays();
    initQeManager(true, false, SolverQeTactic.NONE, false);
    ArrayFormula<IntegerFormula, IntegerFormula> array =
        amgr.makeArray("array", IntegerType, IntegerType);
    ArrayFormula<IntegerFormula, IntegerFormula> arraystore =
        amgr.store(array, imgr.makeNumber(0), imgr.makeNumber(5));
    ArrayFormula<IntegerFormula, IntegerFormula> test =
        amgr.makeArray("test", FormulaType.getArrayType(IntegerType, IntegerType));
    Formula arrayselect = amgr.select(test, imgr.makeNumber(0));
    IntegerFormula xFormula = mgrv.makeVariable(IntegerType, "x");
    BooleanFormula t1 = mgrv.makeEqual(test, arraystore);
    BooleanFormula t2 = mgrv.makeEqual(arrayselect, xFormula);
    BooleanFormula toExist = mgrv.makeAnd(t1, t2);
    Map<String, Formula> boundVars = new HashMap<>();
    boundVars.put("test", test);

    // Prepare expectedResult formula
    BooleanFormula expectedResult = mgrv.makeEqual(xFormula, imgr.makeNumber(5));

    // Prepare Input
    PseudoExistFormula input = new PseudoExistFormula(boundVars, toExist, mgrv);

    PseudoExistFormula result = pQEmgr.applyDER(input);

    assertThat(result.hasQuantifiers()).isFalse(); // no longer quantified
    assertThatFormula(result.getInnerFormula()).isEquivalentTo(expectedResult);
  }

  @Test
  public void testRealQESimpleInteger() throws Exception {
    requireQuantifiers();
    initQeManager(false, false, SolverQeTactic.LIGHT, false);

    // Create the Formula: x=5 and y>x
    IntegerFormula x = mgrv.makeVariable(FormulaType.IntegerType, "x");
    IntegerFormula y = mgrv.makeVariable(FormulaType.IntegerType, "y");
    BooleanFormula t1 = mgrv.makeEqual(x, mgrv.getIntegerFormulaManager().makeNumber(5));
    BooleanFormula t2 = mgrv.makeGreaterThan(y, x, true);
    BooleanFormula toExist = mgrv.makeAnd(t1, t2);

    Map<String, Formula> boundVars = new HashMap<>();
    boundVars.put("x", x);

    BooleanFormula expectedResult =
        mgrv.makeGreaterThan(y, mgrv.getIntegerFormulaManager().makeNumber(5), true);

    // Assert result is equivalent to y>5
    BooleanFormula result = pQEmgr.eliminateQuantifiers(boundVars, toExist).orElseThrow();
    assertThatFormula(result).isEquivalentTo(expectedResult);
  }

  // Assert ex y,z. x=5 and y>z and y<5 and z<5  after QE is x=5
  @Test
  public void testUPDSeveralBoundVars() throws Exception {
    // Create the Formula: x=5 and y>x
    IntegerFormula x = mgrv.makeVariable(FormulaType.IntegerType, "x");
    IntegerFormula y = mgrv.makeVariable(FormulaType.IntegerType, "y");
    IntegerFormula z = mgrv.makeVariable(FormulaType.IntegerType, "z");
    IntegerFormula n5 = mgrv.getIntegerFormulaManager().makeNumber(5);
    BooleanFormula t1 = mgrv.makeEqual(x, n5);
    BooleanFormula t2 = mgrv.makeGreaterThan(y, z, true);
    BooleanFormula t3 = mgrv.makeLessThan(y, n5, true);
    BooleanFormula t4 = mgrv.makeLessThan(z, n5, true);
    BooleanFormula toExist = mgrv.getBooleanFormulaManager().and(t1, t2, t3, t4);

    Map<String, Formula> boundVars = new HashMap<>();
    boundVars.put("y", y);
    boundVars.put("z", z);

    // Prepare Input
    PseudoExistFormula input = new PseudoExistFormula(boundVars, toExist, mgrv);

    // Prepare expectedResult formula
    BooleanFormula expectedResult = t1;

    PseudoExistFormula result = pQEmgr.applyUPD(input);

    assertThat(result.hasQuantifiers()).isFalse(); // no longer quantified
    assertThatFormula(result.getInnerFormula()).isEquivalentTo(expectedResult);
  }
}
