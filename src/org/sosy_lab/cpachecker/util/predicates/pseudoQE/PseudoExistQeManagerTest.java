/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.predicates.pseudoQE;

import static org.junit.Assert.assertFalse;

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
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.FormulaType;
import org.sosy_lab.java_smt.api.NumeralFormula.IntegerFormula;

@RunWith(Parameterized.class)
public class PseudoExistQeManagerTest extends SolverViewBasedTest0 {

  // Princess does not store the formulas in a consistent way
  // So it is difficult to write test
  // TODO: The tests are pretty much depending on internal implementation of SMTSolver,
  // figure out a way to avoid this
  @Parameters(name = "{0}")
  public static Object[] getTestSolvers() {
    return new Object[] {Solvers.Z3};
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

    this.pQEmgr = new PseudoExistQeManager(mgrv, testConfig, logger);
  }

  @Before
  public void setUp() throws InvalidConfigurationException {
    this.pQEmgr = new PseudoExistQeManager(mgrv, config, logger);
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

    assertFalse(result.hasQuantifiers()); // no longer quantified
    assertThatFormula(result.getInnerFormula()).isEquivalentTo(expectedResult);

    // TODO: Ask if okay?
    // Catching an (of course better) exception for eliminateQuantifiers,
    // if not solved its also correct behavior, but not the thing I want to test here
    // Reason for this is inside the SMTSolver so no way to assure desired behavior in
    // future releases
  }

  //Assert ex y. x=5 and y>4  after QE is x=5
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

    assertFalse(result.hasQuantifiers()); // no longer quantified
    assertThatFormula(result.getInnerFormula()).isEquivalentTo(expectedResult);
  }

  @Test
  public void testRealQESimpleInteger() throws Exception {
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
    BooleanFormula result = pQEmgr.eliminateQuantifiers(boundVars, toExist);
    assertThatFormula(result).isEquivalentTo(expectedResult);
  }

  //Assert ex y,z. x=5 and y>z and y<5 and z<5  after QE is x=5
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

    assertFalse(result.hasQuantifiers()); // no longer quantified
    assertThatFormula(result.getInnerFormula()).isEquivalentTo(expectedResult);
  }
}
