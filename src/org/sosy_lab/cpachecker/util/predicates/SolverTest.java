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

import static com.google.common.truth.TruthJUnit.assume;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.common.configuration.Builder;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.ConfigurationBuilder;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.converters.FileTypeConverter;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.common.log.TestLogManager;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.UniqueIdGenerator;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory.Solvers;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.RationalFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;

@RunWith(Parameterized.class)
public class SolverTest {

  @Parameters(name="{0}")
  public static List<Object[]> getSolvers() {
    List<Object[]> result = new ArrayList<>();
    for (Solvers solver : Solvers.values()) {
      result.add(new Object[] { solver });
    }
    return result;
  }

  @Parameter(0)
  public Solvers solver;

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private final LogManager logger = TestLogManager.getInstance();
  private static final UniqueIdGenerator index = new UniqueIdGenerator(); // to get different names

  private FormulaManagerFactory factory;
  private FormulaManager mgr;
  private BooleanFormulaManager bmgr;
  private NumeralFormulaManager<IntegerFormula, IntegerFormula> imgr;
  private NumeralFormulaManager<NumeralFormula, RationalFormula> rmgr;

  @Before
  public void initSolver() throws Exception {
    ConfigurationBuilder builder = new Builder();
    builder.setOption("cpa.predicate.solver", solver.toString());

    // FileOption-Converter for correct output-paths, otherwise files are written in current working directory.
    builder.addConverter(FileOption.class, FileTypeConverter.createWithSafePathsOnly(Configuration.defaultConfiguration()));

    Configuration config = builder.build();

    try {
      factory = new FormulaManagerFactory(config, logger, ShutdownNotifier.create());
    } catch (NoClassDefFoundError e) {
      assume().withFailureMessage("Scala is not on class path")
              .that(e.getMessage()).doesNotContain("scala");
    }
    mgr = factory.getFormulaManager();
    bmgr = mgr.getBooleanFormulaManager();
    imgr = mgr.getIntegerFormulaManager();
    try {
      rmgr = mgr.getRationalFormulaManager();
    } catch (UnsupportedOperationException e) {
      rmgr = null;
    }
  }

  @After
  public void closeSolver() throws Exception {
    if (mgr instanceof AutoCloseable) {
      ((AutoCloseable)mgr).close();
    }
  }

  @Test
  public void simpleStackTestBool() throws SolverException, InterruptedException {
    ProverEnvironment stack = factory.newProverEnvironment(true, true);

    int i = index.getFreshId();
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

  @Test
  public void singleStackTestInteger() throws Exception {
    ProverEnvironment env = factory.newProverEnvironment(true, true);
    simpleStackTestNum(imgr, env);
  }

  @Test
  public void singleStackTestRational() throws Exception {
    assume().withFailureMessage("Solver does not support theory of rationals")
            .that(rmgr).isNotNull();

    ProverEnvironment env = factory.newProverEnvironment(true, true);
    simpleStackTestNum(rmgr, env);
  }

  private <X extends NumeralFormula, Y extends X> void simpleStackTestNum(NumeralFormulaManager<X, Y> nmgr, ProverEnvironment stack) throws Exception {
    int i = index.getFreshId();
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

  @Test
  public void stackTest() throws Exception {
    ProverEnvironment stack = factory.newProverEnvironment(true, true);
    thrown.expect(RuntimeException.class);
    stack.pop();
  }

  @Test
  public void stackTestItp() throws Exception {
    InterpolatingProverEnvironment<?> stack = factory.newProverEnvironmentWithInterpolation(true);
    thrown.expect(RuntimeException.class);
    stack.pop();
  }

  @Test
  public void dualStackTest() throws Exception {
    assume().withFailureMessage("Solver does not support multiple stacks yet")
            .that(solver).isNotEqualTo(Solvers.SMTINTERPOL);

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
  public void dualStackTest2() throws Exception {
    assume().withFailureMessage("Solver does not support multiple stacks yet")
            .that(solver).isNotEqualTo(Solvers.SMTINTERPOL);

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
  public void intTest1() throws Exception {
    IntegerFormula a = imgr.makeVariable("int_a");
    IntegerFormula num = imgr.makeNumber(2);

    BooleanFormula f = imgr.equal(imgr.add(a, a), num);

    ProverEnvironment stack1 = factory.newProverEnvironment(true, true);
    stack1.push(f); // L1
    assertFalse(stack1.isUnsat());
  }

  @Test
  public void intTest2() throws Exception {
    IntegerFormula a = imgr.makeVariable("int_b");
    IntegerFormula num = imgr.makeNumber(1);

    BooleanFormula f = imgr.equal(imgr.add(a, a), num);

    ProverEnvironment stack1 = factory.newProverEnvironment(true, true);
    stack1.push(f); // L1
    assertTrue(stack1.isUnsat());
  }

  @Test
  public void realTest() throws Exception {
    assume().withFailureMessage("Solver does not support theory of rationals")
            .that(rmgr).isNotNull();

    RationalFormula a = rmgr.makeVariable("int_c");
    RationalFormula num = rmgr.makeNumber(1);

    BooleanFormula f = rmgr.equal(rmgr.add(a, a), num);

    ProverEnvironment stack1 = factory.newProverEnvironment(true, true);
    stack1.push(f); // L1
    assertFalse(stack1.isUnsat());
  }
}
