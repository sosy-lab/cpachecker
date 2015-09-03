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

import static com.google.common.truth.Truth.*;
import static com.google.common.truth.TruthJUnit.assume;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.UniqueIdGenerator;
import org.sosy_lab.cpachecker.util.predicates.AssignableTerm.Function;
import org.sosy_lab.cpachecker.util.predicates.AssignableTerm.Variable;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory.Solvers;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BasicProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.FormulaType;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.interfaces.UninterpretedFunctionDeclaration;
import org.sosy_lab.cpachecker.util.test.SolverBasedTest0;

import com.google.common.collect.ImmutableList;

@RunWith(Parameterized.class)
public class SolverStackTest extends SolverBasedTest0 {

  @Parameters(name="{0} (interpolation={1}}")
  public static List<Object[]> getAllCombinations() {
    List<Object[]> result = new ArrayList<>();
    for (Solvers solver : Solvers.values()) {
      result.add(new Object[] {solver, false});
      result.add(new Object[] {solver, true});
    }
    return result;
  }

  @Parameter(0)
  public Solvers solver;

  @Override
  protected Solvers solverToUse() {
    return solver;
  }

  @Parameter(1)
  public boolean useInterpolatingEnvironment;

  /**
   * Generate a prover environment depending on the parameter above.
   * @param generateUnsatCore should be true unless unsat core is explicitly not desired
   */
  private BasicProverEnvironment<?> newEnvironmentForTest(boolean generateUnsatCore) {
    if (useInterpolatingEnvironment) {
      return mgr.newProverEnvironmentWithInterpolation(false);
    } else {
      return mgr.newProverEnvironment(true, generateUnsatCore);
    }
  }

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  private static final UniqueIdGenerator index = new UniqueIdGenerator(); // to get different names

  private void requireMultipleStackSupport() {
    assume().withFailureMessage("Solver does not support multiple stacks yet")
            .that(solver).isNotEqualTo(Solvers.SMTINTERPOL);
  }

  protected final void requireUfValuesInModel() {
    assume().withFailureMessage("Integration of solver does not support retrieving values for UFs from a model")
            .that(solver).isNotEqualTo(Solvers.Z3);
  }

  @Test
  public void simpleStackTestBool() throws SolverException, InterruptedException {
    BasicProverEnvironment<?> stack = newEnvironmentForTest(true);

    int i = index.getFreshId();
    BooleanFormula a = bmgr.makeVariable("bool_a"+i);
    BooleanFormula b = bmgr.makeVariable("bool_b"+i);
    BooleanFormula or = bmgr.or(a, b);

    stack.push(or); //L1
    assert_().about(ProverEnvironment()).that(stack).isSatisfiable();
    BooleanFormula c = bmgr.makeVariable("bool_c"+i);
    BooleanFormula d = bmgr.makeVariable("bool_d"+i);
    BooleanFormula and = bmgr.and(c, d);

    stack.push(and); //L2
    assert_().about(ProverEnvironment()).that(stack).isSatisfiable();

    BooleanFormula notOr = bmgr.not(or);

    stack.push(notOr); //L3
    assert_().about(ProverEnvironment()).that(stack).isUnsatisfiable(); // "or" AND "not or" --> UNSAT

    stack.pop(); //L2
    assert_().about(ProverEnvironment()).that(stack).isSatisfiable();

    stack.pop(); //L1
    assert_().about(ProverEnvironment()).that(stack).isSatisfiable();

    // we are lower than before creating c and d.
    // however we assume that they are usable now (this violates SMTlib).
    stack.push(and); //L2
    assert_().about(ProverEnvironment()).that(stack).isSatisfiable();

    stack.pop(); //L1
    assert_().about(ProverEnvironment()).that(stack).isSatisfiable();

    stack.push(notOr); //L2
    assert_().about(ProverEnvironment()).that(stack).isUnsatisfiable(); // "or" AND "not or" --> UNSAT

    stack.pop(); //L1
    assert_().about(ProverEnvironment()).that(stack).isSatisfiable();

    stack.pop(); //L0 empty stack
  }

  @Test
  public void singleStackTestInteger() throws Exception {
    BasicProverEnvironment<?> env = newEnvironmentForTest(true);
    simpleStackTestNum(imgr, env);
  }

  @Test
  public void singleStackTestRational() throws Exception {
    requireRationals();

    BasicProverEnvironment<?> env = newEnvironmentForTest(true);
    simpleStackTestNum(rmgr, env);
  }

  private <X extends NumeralFormula, Y extends X> void simpleStackTestNum(NumeralFormulaManager<X, Y> nmgr, BasicProverEnvironment<?> stack) throws Exception {
    int i = index.getFreshId();
    X a = nmgr.makeVariable("num_a"+i);
    X b = nmgr.makeVariable("num_b"+i);
    BooleanFormula leqAB = nmgr.lessOrEquals(a, b);

    stack.push(leqAB); //L1
    assert_().about(ProverEnvironment()).that(stack).isSatisfiable();
    X c = nmgr.makeVariable("num_c"+i);
    X d = nmgr.makeVariable("num_d"+i);
    BooleanFormula eqCD = nmgr.lessOrEquals(c, d);

    stack.push(eqCD); //L2
    assert_().about(ProverEnvironment()).that(stack).isSatisfiable();

    BooleanFormula gtAB = nmgr.greaterThan(a, b);

    stack.push(gtAB); //L3
    assert_().about(ProverEnvironment()).that(stack).isUnsatisfiable(); // "<=" AND ">" --> UNSAT

    stack.pop(); //L2
    assert_().about(ProverEnvironment()).that(stack).isSatisfiable();

    stack.pop(); //L1
    assert_().about(ProverEnvironment()).that(stack).isSatisfiable();

    // we are lower than before creating c and d.
    // however we assume that they are usable now (this violates SMTlib).
    stack.push(eqCD); //L2
    assert_().about(ProverEnvironment()).that(stack).isSatisfiable();

    stack.pop(); //L1
    assert_().about(ProverEnvironment()).that(stack).isSatisfiable();

    stack.push(gtAB); //L2
    assert_().about(ProverEnvironment()).that(stack).isUnsatisfiable(); // "or" AND "not or" --> UNSAT

    stack.pop(); //L1
    assert_().about(ProverEnvironment()).that(stack).isSatisfiable();

    stack.pop(); //L0 empty stack
  }

  @Test
  public void stackTest() {
    BasicProverEnvironment<?> stack = newEnvironmentForTest(true);
    thrown.expect(RuntimeException.class);
    stack.pop();
  }

  @Test
  public void dualStackTest() throws Exception {
    requireMultipleStackSupport();

    BooleanFormula a = bmgr.makeVariable("bool_a");
    BooleanFormula not = bmgr.not(a);

    BasicProverEnvironment<?> stack1 = newEnvironmentForTest(true);
    stack1.push(a); // L1
    stack1.push(a); // L2
    BasicProverEnvironment<?> stack2 = newEnvironmentForTest(true);
    stack1.pop(); // L1
    stack1.pop(); // L0

    stack1.push(a); //L1
    assert_().about(ProverEnvironment()).that(stack1).isSatisfiable();

    stack2.push(not); //L1
    assert_().about(ProverEnvironment()).that(stack2).isSatisfiable();

    stack1.pop(); // L0
    stack2.pop(); // L0
  }

  @Test
  public void dualStackTest2() throws Exception {
    requireMultipleStackSupport();

    BooleanFormula a = bmgr.makeVariable("bool_a");
    BooleanFormula not = bmgr.not(a);

    BasicProverEnvironment<?> stack1 = newEnvironmentForTest(true);
    BasicProverEnvironment<?> stack2 = newEnvironmentForTest(true);
    stack1.push(a); // L1
    stack1.push(bmgr.makeBoolean(true)); // L2
    assert_().about(ProverEnvironment()).that(stack1).isSatisfiable();
    stack2.push(not); // L1
    assert_().about(ProverEnvironment()).that(stack2).isSatisfiable();
    stack1.pop(); // L1
    assert_().about(ProverEnvironment()).that(stack1).isSatisfiable();
    stack1.pop(); // L1
    assert_().about(ProverEnvironment()).that(stack1).isSatisfiable();
    stack2.pop(); // L1
    assert_().about(ProverEnvironment()).that(stack2).isSatisfiable();
    assert_().about(ProverEnvironment()).that(stack1).isSatisfiable();
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
    BasicProverEnvironment<?> stack1 = newEnvironmentForTest(true);
    stack1.push(bmgr.makeVariable("bool_a"));

    // Declare b while non-empty stack exists
    final String varName = "bool_b";
    final BooleanFormula b = bmgr.makeVariable(varName);

    // Clear stack (without global declarations b gets deleted)
    stack1.push(b);
    assert_().about(ProverEnvironment()).that(stack1).isSatisfiable();
    stack1.pop();
    stack1.pop();
    stack1.close();

    // Check that "b" (the reference to the old formula)
    // is equivalent to a new formula with the same variable
    assert_().about(BooleanFormula())
             .that(b).isEquivalentTo(bmgr.makeVariable(varName));
  }

  @Test
  public void modelForUnsatFormula() throws Exception {
    try (BasicProverEnvironment<?> stack = newEnvironmentForTest(true)) {
      stack.push(imgr.greaterThan(imgr.makeVariable("a"), imgr.makeNumber(0)));
      stack.push(imgr.lessThan(imgr.makeVariable("a"), imgr.makeNumber(0)));
      assert_().about(ProverEnvironment()).that(stack).isUnsatisfiable();

      thrown.expect(Exception.class);
      stack.getModel();
    }
  }

  @Test
  public void modelForSatFormula() throws Exception {
    try (BasicProverEnvironment<?> stack = newEnvironmentForTest(false)) {
      stack.push(imgr.greaterThan(imgr.makeVariable("a"), imgr.makeNumber(0)));
      stack.push(imgr.lessThan(imgr.makeVariable("a"), imgr.makeNumber(2)));
      assert_().about(ProverEnvironment()).that(stack).isSatisfiable();

      Model model = stack.getModel();
      Variable expectedVar = new Variable("a", TermType.Integer);
      assertThat(model.keySet()).containsExactly(expectedVar);
      assertThat(model).containsEntry(expectedVar, BigInteger.ONE);
    }
  }

  @Test
  public void modelForSatFormulaWithLargeValue() throws Exception {
    try (BasicProverEnvironment<?> stack = newEnvironmentForTest(false)) {
      BigInteger val = BigInteger.TEN.pow(1000);
      stack.push(imgr.equal(imgr.makeVariable("a"), imgr.makeNumber(val)));
      assert_().about(ProverEnvironment()).that(stack).isSatisfiable();

      Model model = stack.getModel();
      Variable expectedVar = new Variable("a", TermType.Integer);
      assertThat(model.keySet()).containsExactly(expectedVar);
      assertThat(model).containsEntry(expectedVar, val);
    }
  }

  @Test
  public void modelForSatFormulaWithUF() throws Exception {
    try (BasicProverEnvironment<?> stack = newEnvironmentForTest(false)) {
      IntegerFormula zero = imgr.makeNumber(0);
      IntegerFormula varA = imgr.makeVariable("a");
      IntegerFormula varB = imgr.makeVariable("b");
      stack.push(imgr.equal(varA, zero));
      stack.push(imgr.equal(varB, zero));
      UninterpretedFunctionDeclaration<IntegerFormula> uf = fmgr.declareUninterpretedFunction("uf", FormulaType.IntegerType, FormulaType.IntegerType);
      stack.push(imgr.equal(fmgr.callUninterpretedFunction(uf, ImmutableList.of(varA)), zero));
      stack.push(imgr.equal(fmgr.callUninterpretedFunction(uf, ImmutableList.of(varB)), zero));
      assert_().about(ProverEnvironment()).that(stack).isSatisfiable();

      Model model = stack.getModel();
      Variable expectedVarA = new Variable("a", TermType.Integer);
      Variable expectedVarB = new Variable("b", TermType.Integer);
      assertThat(model.keySet()).containsAllOf(expectedVarA, expectedVarB);
      // actual type of object is not defined, thus do string matching:
      assertThat(model).containsEntry(expectedVarA, BigInteger.ZERO);
      assertThat(model).containsEntry(expectedVarB, BigInteger.ZERO);

      requireUfValuesInModel();

      Function expectedFunc = new Function("uf", TermType.Integer, new Object[]{BigInteger.ZERO});
      assertThat(model.keySet()).containsExactly(expectedVarA, expectedVarB, expectedFunc);
      assertThat(model).containsEntry(expectedFunc, BigInteger.ZERO);
    }
  }
}
