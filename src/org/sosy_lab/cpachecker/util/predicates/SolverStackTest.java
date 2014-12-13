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

import static com.google.common.truth.Truth.assert_;
import static com.google.common.truth.TruthJUnit.assume;
import static org.junit.Assert.*;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.UniqueIdGenerator;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory.Solvers;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.ProverEnvironment;
import org.sosy_lab.cpachecker.util.test.SolverBasedTest0;

@RunWith(Parameterized.class)
public class SolverStackTest extends SolverBasedTest0 {

  @Parameters(name="{0}")
  public static Object[] getAllSolvers() {
    return Solvers.values();
  }

  @Parameter(0)
  public Solvers solver;

  @Override
  protected Solvers solverToUse() {
    return solver;
  }

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static final UniqueIdGenerator index = new UniqueIdGenerator(); // to get different names

  private void requireMultipleStackSupport() {
    assume().withFailureMessage("Solver does not support multiple stacks yet")
            .that(solver).isNotEqualTo(Solvers.SMTINTERPOL);
  }

  @Test
  public void simpleStackTestBool() throws SolverException, InterruptedException {
    ProverEnvironment stack = mgr.newProverEnvironment(true, true);

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
    ProverEnvironment env = mgr.newProverEnvironment(true, true);
    simpleStackTestNum(imgr, env);
  }

  @Test
  public void singleStackTestRational() throws Exception {
    requireRationals();

    ProverEnvironment env = mgr.newProverEnvironment(true, true);
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
    ProverEnvironment stack = mgr.newProverEnvironment(true, true);
    thrown.expect(RuntimeException.class);
    stack.pop();
  }

  @Test
  public void stackTestItp() throws Exception {
    InterpolatingProverEnvironment<?> stack = mgr.newProverEnvironmentWithInterpolation(true);
    thrown.expect(RuntimeException.class);
    stack.pop();
  }

  @Test
  public void dualStackTest() throws Exception {
    requireMultipleStackSupport();

    BooleanFormula a = bmgr.makeVariable("bool_a");
    BooleanFormula not = bmgr.not(a);

    ProverEnvironment stack1 = mgr.newProverEnvironment(true, true);
    stack1.push(a); // L1
    stack1.push(a); // L2
    ProverEnvironment stack2 = mgr.newProverEnvironment(true, true);
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
    requireMultipleStackSupport();

    BooleanFormula a = bmgr.makeVariable("bool_a");
    BooleanFormula not = bmgr.not(a);

    ProverEnvironment stack1 = mgr.newProverEnvironment(true, true);
    ProverEnvironment stack2 = mgr.newProverEnvironment(true, true);
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

  /**
   * This test checks that a SMT solver uses "global declarations":
   * regardless of the stack at declaration time,
   * declarations always live for the full life time of the solver
   * (i.e., they do not get deleted on pop()).
   * This is contrary to the SMTLib standard,
   * but required by us, e.g. for BMC with induction
   * (where we create new formulas while there is something on the stack).
   */
  @Test
  public void dualStackGlobalDeclarations() throws Exception {
    requireMultipleStackSupport();

    // Create non-empty stack
    ProverEnvironment stack1 = mgr.newProverEnvironment(true, true);
    stack1.push(bmgr.makeVariable("bool_a"));

    // Declare b while non-empty stack exists
    final String varName = "bool_b";
    final BooleanFormula b = bmgr.makeVariable(varName);

    // Clear stack (without global declarations b gets deleted)
    stack1.push(b);
    assertFalse(stack1.isUnsat());
    stack1.pop();
    stack1.pop();
    stack1.close();

    // Check that "b" (the reference to the old formula)
    // is equivalent to a new formula with the same variable
    assert_().about(BooleanFormula())
             .that(b).isEquivalentTo(bmgr.makeVariable(varName));
  }
}
