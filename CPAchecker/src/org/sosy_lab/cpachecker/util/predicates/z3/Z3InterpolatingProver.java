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
package org.sosy_lab.cpachecker.util.predicates.z3;

import static org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApi.*;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.util.predicates.Model;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.InterpolatingProverEnvironment;
import org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApi.PointerToLong;
import org.sosy_lab.cpachecker.util.predicates.z3.Z3NativeApiConstants.Z3_LBOOL;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Longs;

class Z3InterpolatingProver implements InterpolatingProverEnvironment<Long> {

  private final Z3FormulaManager mgr;
  private long z3context;
  private long z3solver;
  private final Z3SmtLogger smtLogger;
  private int level = 0;
  private List<Long> assertedFormulas = new LinkedList<>();

  Z3InterpolatingProver(Z3FormulaManager mgr, long z3params) {
    this.mgr = mgr;
    this.z3context = mgr.getEnvironment();
    this.z3solver = mk_solver(z3context);
    solver_inc_ref(z3context, z3solver);
    solver_set_params(z3context, z3solver, z3params);
    this.smtLogger = mgr.getSmtLogger();
  }

  @Override
  public Long push(BooleanFormula f) {
    level++;

    long e = Z3FormulaManager.getZ3Expr(f);
    solver_push(z3context, z3solver);

    if (mgr.simplifyFormulas) {
      e = simplify(z3context, e);
      inc_ref(z3context, e);
    }

    solver_assert(z3context, z3solver, e);
    assertedFormulas.add(e);

    smtLogger.logPush(1);
    smtLogger.logInterpolationAssert(e);

    return e;
  }

  @Override
  public void pop() {
    level--;

    assertedFormulas.remove(assertedFormulas.size() - 1); // remove last
    solver_pop(z3context, z3solver, 1);

    smtLogger.logPop(1);
  }

  @Override
  public boolean isUnsat() throws Z3SolverException {
    Preconditions.checkState(z3context != 0);
    Preconditions.checkState(z3solver != 0);
    int result = solver_check(z3context, z3solver);

    smtLogger.logCheck();

    Preconditions.checkState(result != Z3_LBOOL.Z3_L_UNDEF.status);
    return result == Z3_LBOOL.Z3_L_FALSE.status;
  }

  @Override
  @SuppressWarnings({"unchecked", "varargs"})
  public BooleanFormula getInterpolant(final List<Long> formulasOfA) {

    // calc difference: formulasOfB := assertedFormulas - formulasOfA
    // we have to handle equal formulas on the stack, so we copy the whole stack and remove the formulas of A once.
    final List<Long> formulasOfB = Lists.newLinkedList(assertedFormulas);
    for (long af : formulasOfA) {
      boolean check = formulasOfB.remove(af); // remove only first occurrence
      assert check : "formula from A must be part of all asserted formulas";
    }

    // binary interpolant is a sequence interpolant of only 2 elements
    return Iterables.getOnlyElement(getSeqInterpolants(Lists.<Set<Long>>newArrayList(
            Sets.newHashSet(formulasOfA), Sets.newHashSet(formulasOfB))));
  }

  @Override
  public List<BooleanFormula> getSeqInterpolants(List<Set<Long>> partitionedFormulas) {
    Preconditions.checkArgument(partitionedFormulas.size() >= 2, "at least 2 partitions needed for interpolation");

    // a 'tree' with all subtrees starting at 0 is called a 'sequence'
    return getTreeInterpolants(partitionedFormulas, new int[partitionedFormulas.size()]);
  }

  @Override
  public List<BooleanFormula> getTreeInterpolants(List<Set<Long>> partitionedFormulas, int[] startOfSubTree) {

    final long[] conjunctionFormulas = new long[partitionedFormulas.size()];

    // build conjunction of each partition
    for (int i = 0; i < partitionedFormulas.size(); i++) {
      Preconditions.checkState(!partitionedFormulas.get(i).isEmpty());
      long conjunction = mk_and(z3context, Longs.toArray(partitionedFormulas.get(i)));
      inc_ref(z3context, conjunction);
      conjunctionFormulas[i] = conjunction;
    }

    // build tree of interpolation-points
    final long[] interpolationFormulas = new long[partitionedFormulas.size()];
    final Deque<Pair<Integer,Long>> stack = new ArrayDeque<>(); // contains <subtree,interpolationPoint>

    int lastSubtree = -1; // subtree starts with 0. With -1<0 we start a new subtree.
    for (int i = 0; i < startOfSubTree.length; i++) {
      final int currentSubtree = startOfSubTree[i];
      final long conjunction;
      if (currentSubtree > lastSubtree) {
        // start of a new subtree -> first element has no children
        conjunction = conjunctionFormulas[i];

      } else { // if (currentSubtree <= lastSubtree) {
        // merge-point in tree, several children at a node -> pop from stack and conjunct
        final List<Long> children = new ArrayList<>();
        while (!stack.isEmpty() && currentSubtree <= stack.peekLast().getFirstNotNull()) {
          // adding at front is important for tree-structure!
          children.add(0, stack.pollLast().getSecond());
        }
        children.add(conjunctionFormulas[i]); // add the node itself
        conjunction = mk_and(z3context, Longs.toArray(children));
      }

      final long interpolationPoint;
      if (i == startOfSubTree.length - 1) {
        // the last node in the tree (=root) does not need the interpolation-point-flag
        interpolationPoint = conjunction;
        Preconditions.checkState(currentSubtree == 0, "subtree of root should start at 0.");
        Preconditions.checkState(stack.isEmpty(), "root should be the last element in the stack.");
      } else {
        interpolationPoint = mk_interpolant(z3context, conjunction);
      }

      inc_ref(z3context, interpolationPoint);
      interpolationFormulas[i] = interpolationPoint;
      stack.addLast(Pair.of(currentSubtree, interpolationPoint));
      lastSubtree = currentSubtree;
    }

    Preconditions.checkState(stack.peekLast().getFirst() == 0, "subtree of root should start at 0.");
    long root = stack.pollLast().getSecond();
    Preconditions.checkState(stack.isEmpty(), "root should have been the last element in the stack.");

    final PointerToLong model = new PointerToLong();
    final PointerToLong interpolant = new PointerToLong();
    int isSat = compute_interpolant(
        z3context,
        root, // last element is end of chain (root of tree)
        0,
        interpolant,
        model
    );

    Preconditions.checkState(isSat == Z3_LBOOL.Z3_L_FALSE.status,
            "interpolation not possible, because SAT-check returned status '%s'", isSat);

    // n partitions -> n-1 interpolants
    // the given tree interpolants are sorted in post-order,
    // so we only need to copy them
    final List<BooleanFormula> result = new ArrayList<>();
    for (int i = 0; i < partitionedFormulas.size() - 1; i++) {
      result.add(mgr.encapsulateBooleanFormula(ast_vector_get(z3context, interpolant.value, i)));
    }

    // cleanup
    for (long partition : conjunctionFormulas) {
      dec_ref(z3context, partition);
    }
    for (long partition : interpolationFormulas) {
      dec_ref(z3context, partition);
    }

    return result;
  }

  @Override
  public Model getModel() {
    return Z3Model.createZ3Model(mgr, z3context, z3solver);
  }

  @Override
  public void close() {
    Preconditions.checkState(z3context != 0);
    Preconditions.checkState(z3solver != 0);

    while (level > 0) { // TODO do we need this?
      pop();
    }

    assertedFormulas = null;
    //TODO solver_reset(z3context, z3solver);
    solver_dec_ref(z3context, z3solver);
    z3context = 0;
    z3solver = 0;
  }
}