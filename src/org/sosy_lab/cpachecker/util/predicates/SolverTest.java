/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.predicates;

import static org.junit.Assert.*;

import org.junit.Test;
import org.sosy_lab.common.configuration.Builder;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.converters.FileTypeConverter;
import org.sosy_lab.common.io.Paths;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.TestLogManager;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;

public class SolverTest {

  private boolean isLibFociAvailable = Paths.get("lib/native/x86_64-linux/libfoci.so").exists();

  private LogManager logger = TestLogManager.getInstance();
  private static int index = 0; // to get different names

  FormulaManagerFactory factory;
  FormulaManager mgr;
  BooleanFormulaManager bmgr;
  NumeralFormulaManager<IntegerFormula, IntegerFormula> imgr;
  NumeralFormulaManager<NumeralFormula, RationalFormula> rmgr;

  private void init(String solver) throws Exception {
    ConfigurationBuilder builder = new Builder();
    builder.setOption("cpa.predicate.solver", solver);

    // FileOption-Converter for correct output-paths, otherwise files are written in current working directory.
    builder.addConverter(FileOption.class, FileTypeConverter.createWithSafePathsOnly(Configuration.defaultConfiguration()));

    Configuration config = builder.build();

    factory = new FormulaManagerFactory(config, logger, ShutdownNotifier.create());
    mgr = factory.getFormulaManager();
    bmgr = mgr.getBooleanFormulaManager();
    imgr = mgr.getIntegerFormulaManager();
    if (!"PRINCESS".equals(solver)) { // PRINCESS does not support Rational-theory
      rmgr = mgr.getRationalFormulaManager();
    }
  }

  @Test
  public void singleStackTestMATHSAT() throws Exception {
    singleStackTest("MATHSAT5");
  }

  @Test
  public void singleStackTestZ3() throws Exception {
    if (isLibFociAvailable) {
      singleStackTest("Z3");
    }
  }

  @Test
  public void singleStackTestSMTINTERPOL() throws Exception {
    singleStackTest("SMTINTERPOL");
  }

  @Test
  public void singleStackTestPRINCESS() throws Exception {
    singleStackTest("PRINCESS");
  }

  private void singleStackTest(String solver) throws Exception {
    init(solver);
    ProverEnvironment env = factory.newProverEnvironment(true, true);
    simpleStackTestBool(bmgr, env);
    simpleStackTestNum(imgr, env);
    if (rmgr != null) {
      simpleStackTestNum(rmgr, env);
    }
  }

  private void simpleStackTestBool(BooleanFormulaManager bmgr, ProverEnvironment stack) throws SolverException, InterruptedException {

    int i = index++;
    BooleanFormula a = bmgr.makeVariable("bool_a"+i);
    BooleanFormula b = bmgr.makeVariable("bool_b"+i);
    BooleanFormula or = bmgr.or(a, b);

    stack.push(or); //L1
    assertFalse(stack.isUnsat());
    BooleanFormula c = bmgr.makeVariable("bool_c"+i);
    BooleanFormula d = bmgr.makeVariable("bool_d"+i);
    BooleanFormula and = bmgr.and(c, d);

    stack.push(and); //L2
    assertFalse(stack.isUnsat());

    BooleanFormula notOr = bmgr.not(or);

    stack.push(notOr); //L3
    assertTrue(stack.isUnsat()); // "or" AND "not or" --> UNSAT

    stack.pop(); //L2
    assertFalse(stack.isUnsat());

    stack.pop(); //L1
    assertFalse(stack.isUnsat());

    // we are lower than before creating c and d.
    // however we assume that they are usable now (this violates SMTlib).
    stack.push(and); //L2
    assertFalse(stack.isUnsat());

    stack.pop(); //L1
    assertFalse(stack.isUnsat());

    stack.push(notOr); //L2
    assertTrue(stack.isUnsat()); // "or" AND "not or" --> UNSAT

    stack.pop(); //L1
    assertFalse(stack.isUnsat());

    stack.pop(); //L0 empty stack
  }

  private <X extends NumeralFormula, Y extends X> void simpleStackTestNum(NumeralFormulaManager<X, Y> nmgr, ProverEnvironment stack) throws SolverException, InterruptedException {

    int i = index++;
    X a = nmgr.makeVariable("num_a"+i);
    X b = nmgr.makeVariable("num_b"+i);
    BooleanFormula leqAB = nmgr.lessOrEquals(a, b);

    stack.push(leqAB); //L1
    assertFalse(stack.isUnsat());
    X c = nmgr.makeVariable("num_c"+i);
    X d = nmgr.makeVariable("num_d"+i);
    BooleanFormula eqCD = nmgr.lessOrEquals(c, d);

    stack.push(eqCD); //L2
    assertFalse(stack.isUnsat());

    BooleanFormula gtAB = nmgr.greaterThan(a, b);

    stack.push(gtAB); //L3
    assertTrue(stack.isUnsat()); // "<=" AND ">" --> UNSAT

    stack.pop(); //L2
    assertFalse(stack.isUnsat());

    stack.pop(); //L1
    assertFalse(stack.isUnsat());

    // we are lower than before creating c and d.
    // however we assume that they are usable now (this violates SMTlib).
    stack.push(eqCD); //L2
    assertFalse(stack.isUnsat());

    stack.pop(); //L1
    assertFalse(stack.isUnsat());

    stack.push(gtAB); //L2
    assertTrue(stack.isUnsat()); // "or" AND "not or" --> UNSAT

    stack.pop(); //L1
    assertFalse(stack.isUnsat());

    stack.pop(); //L0 empty stack
  }

  @Test(expected = Exception.class)
  public void stackTestMATHSAT() throws Exception {
    stackTest("MATHSAT5");
  }

  @Test(expected = Exception.class)
  public void stackTestZ3() throws Exception {
    if (isLibFociAvailable) {
      stackTest("Z3");
    } else {
      throw new RuntimeException("dummy exception for junit");
    }
  }

  @Test(expected = Exception.class)
  public void stackTestSMTINTERPOL() throws Exception {
    stackTest("SMTINTERPOL");
  }

  @Test(expected = Exception.class)
  public void stackTestPRINCESS() throws Exception {
    stackTest("PRINCESS");
  }

  private void stackTest(String solver) throws Exception {
    init(solver);
    ProverEnvironment stack = factory.newProverEnvironment(true, true);
    stack.pop();
  }

  @Test(expected = Exception.class)
  public void stackTest2MATHSAT() throws Exception {
    stackTest2("MATHSAT5");
  }

  @Test(expected = Exception.class)
  public void stackTest2Z3() throws Exception {
    if (isLibFociAvailable) {
      stackTest2("Z3");
    } else {
      throw new RuntimeException("dummy exception for junit");
    }
  }

  @Test(expected = Exception.class)
  public void stackTest2SMTINTERPOL() throws Exception {
    stackTest2("SMTINTERPOL");
  }

  @Test(expected = Exception.class)
  public void stackTest2PRINCESS() throws Exception {
    stackTest2("PRINCESS");
  }

  private void stackTest2(String solver) throws Exception {
    init(solver);
    InterpolatingProverEnvironment<?> stack = factory.newProverEnvironmentWithInterpolation(true);
    stack.pop();
  }

  @Test
  public void dualStackTestMATHSAT() throws Exception {
    dualStackTest("MATHSAT5");
  }

  @Test
  public void dualStackTestZ3() throws Exception {
    if (isLibFociAvailable) {
      dualStackTest("Z3");
    }
  }

//  @Test
//  public void dualStackTestSMTINTERPOL() throws Exception {
//    dualStackTest("SMTINTERPOL");
//  }

  @Test
  public void dualStackTestPRINCESS() throws Exception {
    dualStackTest("PRINCESS");
  }

  private void dualStackTest(String solver) throws Exception {

    init(solver);

    BooleanFormula a = bmgr.makeVariable("bool_a");
    BooleanFormula not = bmgr.not(a);

    ProverEnvironment stack1 = factory.newProverEnvironment(true, true);
    stack1.push(a); // L1
    stack1.push(a); // L2
    ProverEnvironment stack2 = factory.newProverEnvironment(true, true);
    stack1.pop(); // L1
    stack1.pop(); // L0

    stack1.push(a); //L1
    assertFalse(stack1.isUnsat());

    stack2.push(not); //L1
    assertFalse(stack2.isUnsat());

    stack1.pop(); // L0
    stack2.pop(); // L0
  }

  @Test
  public void dualStackTest2MATHSAT() throws Exception {
    dualStackTest2("MATHSAT5");
  }

  @Test
  public void dualStackTest2Z3() throws Exception {
    if (isLibFociAvailable) {
      dualStackTest2("Z3");
    }
  }

//  @Test
//  public void dualStackTest2SMTINTERPOL() throws Exception {
//    dualStackTest2("SMTINTERPOL");
//  }

  @Test
  public void dualStackTest2PRINCESS() throws Exception {
    dualStackTest2("PRINCESS");
  }

  private void dualStackTest2(String solver) throws Exception {

    init(solver);

    BooleanFormula a = bmgr.makeVariable("bool_a");
    BooleanFormula not = bmgr.not(a);

    ProverEnvironment stack1 = factory.newProverEnvironment(true, true);
    ProverEnvironment stack2 = factory.newProverEnvironment(true, true);
    stack1.push(a); // L1
    stack1.push(bmgr.makeBoolean(true)); // L2
    assertFalse(stack1.isUnsat());
    stack2.push(not); // L1
    assertFalse(stack2.isUnsat());
    stack1.pop(); // L1
    assertFalse(stack1.isUnsat());
    stack1.pop(); // L1
    assertFalse(stack1.isUnsat());
    stack2.pop(); // L1
    assertFalse(stack2.isUnsat());
    assertFalse(stack1.isUnsat());
  }

  @Test
  public void numTestMATHSAT() throws Exception {
    intTest1("MATHSAT5");
    intTest2("MATHSAT5");
    realTest("MATHSAT5");
  }

  @Test
  public void numTestZ3() throws Exception {
    if (isLibFociAvailable) {
      intTest1("Z3");
      intTest2("Z3");
      realTest("Z3");
    }
  }

  @Test
  public void numTestSMTINTERPOL() throws Exception {
    intTest1("SMTINTERPOL");
    intTest2("SMTINTERPOL");
    realTest("SMTINTERPOL");
  }

  @Test
  public void numTestPRINCESS() throws Exception {
    intTest1("PRINCESS");
    intTest2("PRINCESS");
    // realTest("PRINCESS"); // PRINCESS does not support Rational-theory
  }

  private void intTest1(String solver) throws Exception {

    init(solver);

    IntegerFormula a = imgr.makeVariable("int_a");
    IntegerFormula num = imgr.makeNumber(2);

    BooleanFormula f = imgr.equal(imgr.add(a, a), num);

    ProverEnvironment stack1 = factory.newProverEnvironment(true, true);
    stack1.push(f); // L1
    assertFalse(stack1.isUnsat());
  }

  private void intTest2(String solver) throws Exception {

    init(solver);

    IntegerFormula a = imgr.makeVariable("int_b");
    IntegerFormula num = imgr.makeNumber(1);

    BooleanFormula f = imgr.equal(imgr.add(a, a), num);

    ProverEnvironment stack1 = factory.newProverEnvironment(true, true);
    stack1.push(f); // L1
    assertTrue(stack1.isUnsat());
  }

  private void realTest(String solver) throws Exception {

    init(solver);

    RationalFormula a = rmgr.makeVariable("int_c");
    RationalFormula num = rmgr.makeNumber(1);

    BooleanFormula f = rmgr.equal(rmgr.add(a, a), num);

    ProverEnvironment stack1 = factory.newProverEnvironment(true, true);
    stack1.push(f); // L1
    assertFalse(stack1.isUnsat());
  }
}
