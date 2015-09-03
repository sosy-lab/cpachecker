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

import static com.google.common.collect.Iterables.getLast;
import static com.google.common.truth.Truth.assert_;
import static com.google.common.truth.TruthJUnit.assume;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.sosy_lab.cpachecker.exceptions.SolverException;
import org.sosy_lab.cpachecker.util.UniqueIdGenerator;
import org.sosy_lab.cpachecker.util.predicates.FormulaManagerFactory.Solvers;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BasicProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.interfaces.NumeralFormula.IntegerFormula;
import org.sosy_lab.cpachecker.util.test.SolverBasedTest0;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;


/**
 * This class contains some simple Junit-tests to check the interpolation-API of our solvers.
 */
@RunWith(Parameterized.class)
public class SolverInterpolationTest extends SolverBasedTest0 {

  @Parameters(name="{0} (shared={1})")
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
  public boolean shared;

  /** Generate a prover environment depending on the parameter above. */
  @SuppressWarnings("unchecked")
  private <T> InterpolatingProverEnvironment<T> newEnvironmentForTest() {
    return (InterpolatingProverEnvironment<T>)mgr.newProverEnvironmentWithInterpolation(shared);
  }

  private static final UniqueIdGenerator index = new UniqueIdGenerator(); // to get different names


  @Test
  @SuppressWarnings({"unchecked", "varargs"})
  public <T> void binaryInterpolation() throws SolverException, InterruptedException {

    InterpolatingProverEnvironment<T> stack = newEnvironmentForTest();

    int i = index.getFreshId();

    IntegerFormula zero = imgr.makeNumber(0);
    IntegerFormula one = imgr.makeNumber(1);

    IntegerFormula a = imgr.makeVariable("a" + i);
    IntegerFormula b = imgr.makeVariable("b" + i);
    IntegerFormula c = imgr.makeVariable("c" + i);

    // build formula:  1 = A = B = C = 0
    BooleanFormula A = imgr.equal(one, a);
    BooleanFormula B = imgr.equal(a, b);
    BooleanFormula C = imgr.equal(b, c);
    BooleanFormula D = imgr.equal(c, zero);

    T TA = stack.push(A);
    T TB = stack.push(B);
    T TC = stack.push(C);
    stack.push(D);

    assert_().about(ProverEnvironment()).that(stack).isUnsatisfiable();

    BooleanFormula itpA = stack.getInterpolant(Lists.newArrayList(TA));
    BooleanFormula itpB = stack.getInterpolant(Lists.newArrayList(TA, TB));
    BooleanFormula itpC = stack.getInterpolant(Lists.newArrayList(TA, TB, TC));

    stack.pop(); // clear stack, such that we can re-use the solver
    stack.pop();
    stack.pop();
    stack.pop();

    // we check here the stricter properties for sequential interpolants,
    // but this simple example should work for all solvers
    checkItpSequence(stack, Lists.newArrayList(A, B, C, D), Lists.newArrayList(itpA, itpB, itpC));
  }

  private void requireSequentialItp() {
    assume().withFailureMessage("Solver does not support sequential interpolation.")
            .that(solver).isNotEqualTo(Solvers.MATHSAT5);
  }

  private void requireTreeItp() {
    assume().withFailureMessage("Solver does not support tree-interpolation.")
        .that(solver).isAnyOf(Solvers.Z3, Solvers.SMTINTERPOL);
  }

  @Test
  @SuppressWarnings({"unchecked", "varargs"})
  public <T> void sequentialInterpolation() throws SolverException, InterruptedException {

    requireSequentialItp();

    InterpolatingProverEnvironment<T> stack = newEnvironmentForTest();

    int i = index.getFreshId();

    IntegerFormula zero = imgr.makeNumber(0);
    IntegerFormula one = imgr.makeNumber(1);

    IntegerFormula a = imgr.makeVariable("a" + i);
    IntegerFormula b = imgr.makeVariable("b" + i);
    IntegerFormula c = imgr.makeVariable("c" + i);

    // build formula:  1 = A = B = C = 0
    BooleanFormula A = imgr.equal(one, a);
    BooleanFormula B = imgr.equal(a, b);
    BooleanFormula C = imgr.equal(b, c);
    BooleanFormula D = imgr.equal(c, zero);

    Set<T> TA = Sets.newHashSet(stack.push(A));
    Set<T> TB = Sets.newHashSet(stack.push(B));
    Set<T> TC = Sets.newHashSet(stack.push(C));
    Set<T> TD = Sets.newHashSet(stack.push(D));

    assert_().about(ProverEnvironment()).that(stack).isUnsatisfiable();

    List<BooleanFormula> itps = stack.getSeqInterpolants(Lists.newArrayList(TA,TB,TC,TD));

    stack.pop(); // clear stack, such that we can re-use the solver
    stack.pop();
    stack.pop();
    stack.pop();

    checkItpSequence(stack, Lists.newArrayList(A, B, C, D), itps);
  }

  @Test
  @SuppressWarnings({"unchecked", "varargs"})
  public <T> void treeInterpolation() throws SolverException, InterruptedException {

    requireTreeItp();

    InterpolatingProverEnvironment<T> stack = newEnvironmentForTest();

    int i = index.getFreshId();

    IntegerFormula zero = imgr.makeNumber(0);
    IntegerFormula one = imgr.makeNumber(1);

    IntegerFormula a = imgr.makeVariable("a" + i);
    IntegerFormula b = imgr.makeVariable("b" + i);
    IntegerFormula c = imgr.makeVariable("c" + i);
    IntegerFormula d = imgr.makeVariable("d" + i);

    // build formula:  1 = A = B = C = D = 0
    BooleanFormula A = imgr.equal(one, a);
    BooleanFormula B = imgr.equal(a, b);
    BooleanFormula R = imgr.equal(b, c);
    BooleanFormula C = imgr.equal(c, d);
    BooleanFormula D = imgr.equal(d, zero);

    Set<T> TA = Sets.newHashSet(stack.push(A));
    Set<T> TB = Sets.newHashSet(stack.push(B));
    Set<T> TR = Sets.newHashSet(stack.push(R));
    Set<T> TC = Sets.newHashSet(stack.push(C));
    Set<T> TD = Sets.newHashSet(stack.push(D));

    assert_().about(ProverEnvironment()).that(stack).isUnsatisfiable();

    // we build a very simple tree:
    // A  D
    // |  |
    // B  C
    // | /
    // R
    List<BooleanFormula> itps = stack.getTreeInterpolants(
        Lists.newArrayList(TA, TB, TD, TC, TR),  // post-order
        new int[]         { 0,  0,  2,  2,  0}); // left-most node in current subtree

    stack.pop(); // clear stack, such that we can re-use the solver
    stack.pop();
    stack.pop();
    stack.pop();
    stack.pop();

    checkImplies(stack, A, itps.get(0));
    checkImplies(stack, bmgr.and(itps.get(0),B), itps.get(1));
    checkImplies(stack, D, itps.get(2));
    checkImplies(stack, bmgr.and(itps.get(2),C), itps.get(3));
    checkImplies(stack, bmgr.and(Lists.newArrayList(itps.get(1), itps.get(3), R)), bmgr.makeBoolean(false));
  }

  @Test
  @SuppressWarnings({"unchecked", "varargs"})
  public <T> void treeInterpolation2() throws SolverException, InterruptedException {

    requireTreeItp();

    InterpolatingProverEnvironment<T> stack = newEnvironmentForTest();

    int i = index.getFreshId();

    IntegerFormula one = imgr.makeNumber(1);
    IntegerFormula five = imgr.makeNumber(5);

    IntegerFormula a = imgr.makeVariable("a" + i);
    IntegerFormula b = imgr.makeVariable("b" + i);
    IntegerFormula c = imgr.makeVariable("c" + i);
    IntegerFormula d = imgr.makeVariable("d" + i);
    IntegerFormula e = imgr.makeVariable("e" + i);

    // build formula:  1 = A = B = C = D+1 and D = E = 5
    BooleanFormula A = imgr.equal(one, a);
    BooleanFormula B = imgr.equal(a, b);
    BooleanFormula R1 = imgr.equal(b, c);
    BooleanFormula C = imgr.equal(c, imgr.add(d, one));
    BooleanFormula R2 = imgr.equal(d, e);
    BooleanFormula D = imgr.equal(e, five);

    Set<T> TA = Sets.newHashSet(stack.push(A));
    Set<T> TB = Sets.newHashSet(stack.push(B));
    Set<T> TR1 = Sets.newHashSet(stack.push(R1));
    Set<T> TC = Sets.newHashSet(stack.push(C));
    Set<T> TR2 = Sets.newHashSet(stack.push(R2));
    Set<T> TD = Sets.newHashSet(stack.push(D));

    assert_().about(ProverEnvironment()).that(stack).isUnsatisfiable();

    // we build a simple tree:
    // A
    // |
    // B  C
    // | /
    // R1 D
    // | /
    // R2
    List<BooleanFormula> itps = stack.getTreeInterpolants(
        Lists.newArrayList(TA, TB, TC, TR1, TD, TR2),  // post-order
        new int[]         { 0,  0,  2,   0,  4,  0}); // left-most node in current subtree

    stack.pop(); // clear stack, such that we can re-use the solver
    stack.pop();
    stack.pop();
    stack.pop();
    stack.pop();
    stack.pop();

    checkImplies(stack, A, itps.get(0));
    checkImplies(stack, bmgr.and(itps.get(0),B), itps.get(1));
    checkImplies(stack, C, itps.get(2));
    checkImplies(stack, bmgr.and(Lists.newArrayList(itps.get(1), itps.get(2), R1)), itps.get(3));
    checkImplies(stack, D, itps.get(4));
    checkImplies(stack, bmgr.and(Lists.newArrayList(itps.get(3), itps.get(4), R2)), bmgr.makeBoolean(false));
  }

  private void checkItpSequence(InterpolatingProverEnvironment<?> stack,
                                    List<BooleanFormula> formulas, List<BooleanFormula> itps)
          throws SolverException, InterruptedException {

    assert formulas.size() - 1 == itps.size() : "there should be N-1 interpolants for N formulas";

    checkImplies(stack, formulas.get(0), itps.get(0));
    for (int i = 1; i < formulas.size() - 1; i++) {
      checkImplies(stack, bmgr.and(itps.get(i-1), formulas.get(i)), itps.get(i));
    }
    checkImplies(stack, bmgr.and(getLast(itps), getLast(formulas)), bmgr.makeBoolean(false));
  }

  private void checkImplies(BasicProverEnvironment<?> stack, BooleanFormula a, BooleanFormula b)
          throws SolverException, InterruptedException {
    // a=>b  <-->  !a||b
    stack.push(bmgr.or(bmgr.not(a), b));
    assert_().about(ProverEnvironment()).that(stack).isSatisfiable();
    stack.pop();
  }
}
