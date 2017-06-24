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
package org.sosy_lab.cpachecker.util.predicates.interpolation.strategy;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Triple;
import org.sosy_lab.cpachecker.util.predicates.interpolation.InterpolationManager;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;
import org.sosy_lab.java_smt.api.BooleanFormula;
import org.sosy_lab.java_smt.api.BooleanFormulaManager;
import org.sosy_lab.java_smt.api.SolverException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

public abstract class AbstractTreeInterpolation<T> extends ITPStrategy<T> {

  /**
   * Tree interpolants are used for the analysis recursive procedures
   * and to have a modular analysis.
   * Current status:
   * - We need abstraction states at function entry and exit nodes (and maybe also at function calls).
   * - Tree interpolants are useless for the 'normal' PredicateAnalysis.
   * - Tree interpolants are useful in for PredicateAnalysis in combination with BAM and recursion.
   */
  AbstractTreeInterpolation(LogManager pLogger, ShutdownNotifier pShutdownNotifier,
                            FormulaManagerView pFmgr, BooleanFormulaManager pBfmgr) {
    super(pLogger, pShutdownNotifier, pFmgr, pBfmgr);
  }

  /** This method checks the validity of the tree interpolants and
   * overrides the default check.
   *
   * @param solver is for checking satisfiability
   * @param formulasWithStatesAndGroupdIds is a list of (F,E,T) where
   *          the path formula F starting at an abstract state E corresponds
   *          with the ITP-group T. We assume the sorting of the list matches
   *          the order of abstract states along the counterexample.
   * @param interpolants computed with {@link InterpolationManager#getInterpolants} and will be checked.
   */
  @Override
  public void checkInterpolants(final Solver solver,
      final List<Triple<BooleanFormula, AbstractState, T>> formulasWithStatesAndGroupdIds,
      final List<BooleanFormula> interpolants)
      throws SolverException, InterruptedException {

    final List<BooleanFormula> formulas =
        Lists.transform(formulasWithStatesAndGroupdIds, Triple::getFirst);
    final List<Integer> subtrees = buildTreeStructure(formulasWithStatesAndGroupdIds).getSecond();

    // The following four properties need to be checked for tree interpolants:
    // (A) for all leafs of the tree:  f_leaf => itp_leaf
    // (B) \forall i \in [1..n-1] :    (itp_sub1_i & itp_sub2_i & ...) & f_i => itp_i
    // (C)                             (itp_sub1_{n-1} & itp_sub2_{n-1} & ...) & itp_{n-1} & f_n => false
    // (D) variables/symbols in each interpolant are part of both partitions

    // PROBLEM: we rebuild some interpolants before returning them from {@getInterpolants()}.
    // Thus the check might fail. TODO check this!

    assert formulas.size() == subtrees.size() : "each formula must be part of a subtree";
    assert formulas.size() == interpolants.size() + 1 : "number of interpolants should match the tree-structure";

    // check (A)
    if (!solver.implies(formulas.get(0), interpolants.get(0))) {
      throw new SolverException(String.format("interpolant %s is not implied by leaf formula.", interpolants.get(0)));
    }
    for (int i = 1; i < subtrees.size() - 1; i++) {
      if (subtrees.get(i) > subtrees.get(i - 1)) {
        // new subtree -> new leaf
        if (!solver.implies(formulas.get(i), interpolants.get(i))) {
          throw new SolverException(
                  String.format("interpolant %s is not implied by leaf formula.", interpolants.get(i)));
        }
      }
    }

    // check (B)
    for (int i = 1; i < subtrees.size() - 1; i++) {
      final List<BooleanFormula> previousInterpolants = new ArrayList<>();
      final int currentSubtree = subtrees.get(i);

      int pos = i;
      while (subtrees.get(pos - 1) > currentSubtree) {
        // add children from right to left (left is excluded because of equal subtree)
        previousInterpolants.add(interpolants.get(pos));
        pos = subtrees.get(pos - 1); // jump to first leaf of subtree
      }

      // add left most child
      previousInterpolants.add(interpolants.get(pos - 1));

      // add the node itself (it is not an interpolant)
      previousInterpolants.add(formulas.get(i));

      if (!solver.implies(bfmgr.and(previousInterpolants), interpolants.get(i))) {
        throw new SolverException(
                String.format("Interpolant %s is not implied by previous part of the path.", interpolants.get(i)));
      }
    }

    // check (C)
    final List<BooleanFormula> previousInterpolants = new ArrayList<>();
    final int currentSubtree = subtrees.get(subtrees.size() - 1);
    assert currentSubtree == 0 : "root should be in left-most subtree";

    int pos = subtrees.size() - 1;
    while (subtrees.get(pos - 1) > currentSubtree) {
      // add children from right to left (left is excluded because of equal subtree)
      previousInterpolants.add(interpolants.get(pos));
      pos = subtrees.get(pos - 1); // jump to first leaf of subtree
    }

    // add left most child
    previousInterpolants.add(interpolants.get(pos - 1));

    // add the node itself (it is not an interpolant)
    previousInterpolants.add(formulas.get(subtrees.size() - 1));

    if (!solver.implies(bfmgr.and(previousInterpolants), bfmgr.makeFalse())) {
      throw new SolverException(
              "Interpolant " + interpolants.get(subtrees.size() - 1) + " is not implied by previous part of the path");
    }

    // check (D)
    final List<Set<String>> variablesInFormulas = Lists.newArrayListWithExpectedSize(formulas.size());
    for (BooleanFormula f : formulas) {
      variablesInFormulas.add(fmgr.extractVariableNames(f));
    }

    for (int i = 0; i < interpolants.size(); i++) {

      int checksum = 0;

      final Set<String> variablesInA = new HashSet<>();
      for (int j = i; j >= 0 && subtrees.get(j) >= subtrees.get(i); j--) { // subtree backwards
        // formula i is in subtree of current node
        variablesInA.addAll(variablesInFormulas.get(j));
        checksum++;
      }

      final Set<String> variablesInB = new HashSet<>();
      for (int j = 0; j < subtrees.get(i); j++) { // sibling subtree
        // formula i is NOT in subtree of current node
        variablesInB.addAll(variablesInFormulas.get(j));
        checksum++;
      }
      for (int j = i + 1; j < subtrees.size(); j++) { // parent-part of tree
        // formula i is NOT in subtree of current node
        variablesInB.addAll(variablesInFormulas.get(j));
        checksum++;
      }

      assert checksum == formulas.size() : "partitions for interpolant have wrong size";

      Set<String> allowedVariables = Sets.intersection(variablesInA, variablesInB).immutableCopy();
      Set<String> variablesInInterpolant = fmgr.extractVariableNames(interpolants.get(i));

      variablesInInterpolant.removeAll(allowedVariables);

      if (!variablesInInterpolant.isEmpty()) {
        throw new SolverException(String.format(
                "Interpolant %s contains forbidden variable(s) %s", interpolants.get(i), variablesInInterpolant));
      }
    }
  }


  private static enum TreePosition {
    START,    // leaf-node with no children, start of a subtree
    MIDDLE,   // node with exactly one child, middle node in a sequence
    END       // node with several children, end of a subtree
  }

  /** returns the current position in a interpolation tree. */
  private static <T> TreePosition getTreePosition(
      final List<Triple<BooleanFormula, AbstractState, T>> formulasWithStatesAndGroupdIds,
      final int position) {
    final AbstractState abstractionState = checkNotNull(formulasWithStatesAndGroupdIds.get(position).getSecond());
    final CFANode node = AbstractStates.extractLocation(abstractionState);
    if (node instanceof FunctionEntryNode && callHasReturn(formulasWithStatesAndGroupdIds, position)) {
      return TreePosition.START;
    } else if (node instanceof FunctionExitNode) {
      return TreePosition.END;
    } else {
      return TreePosition.MIDDLE;
    }
  }

  /** check, if there exists a function-exit-node to the current call-node. */
  protected static <T> boolean callHasReturn(
      final List<Triple<BooleanFormula, AbstractState, T>> formulasWithStatesAndGroupdIds,
      int callIndex) {
    // TODO caching as optimization to reduce from  k*O(n)  to  O(n)+k*O(1)  ?
    final Deque<CFANode> callstack = new ArrayDeque<>();

    {
      final AbstractState abstractionState = formulasWithStatesAndGroupdIds.get(callIndex).getSecond();
      final CFANode node = AbstractStates.extractLocation(abstractionState);
      assert (node instanceof FunctionEntryNode) : "call needed as input param";
      callstack.addLast(node);
    }

    // walk along path and track the call stack
    for (Triple<BooleanFormula, AbstractState, T> t : Iterables.skip(formulasWithStatesAndGroupdIds, callIndex + 1)) {
      assert !callstack.isEmpty() : "should have returned when callstack is empty";

      final AbstractState abstractionState = checkNotNull(t.getSecond());
      final CFANode node = AbstractStates.extractLocation(abstractionState);

      if (node instanceof FunctionEntryNode) {
        callstack.addLast(node);
      }

      final CFANode lastEntryNode = callstack.getLast();
      if ((node instanceof FunctionExitNode
              && ((FunctionExitNode) node).getEntryNode() == lastEntryNode)
        //|| (node.getEnteringSummaryEdge() != null
        // && node.getEnteringSummaryEdge().getPredecessor().getLeavingEdge(0).getSuccessor() == lastEntryNode)
              ) {
        callstack.removeLast();

        // we found the function exit for the input param
        if (callstack.isEmpty()) {
          return true;
        }
      }
    }
    return false;
  }

  /**
   * Build a tree of formulas according to control flow (function calls and returns).
   * A new subtree is started with the first node (FunctionEntryNode) inside a function that has a function-return.
   * A subtree is connected with the whole tree with the calling statement
   * (i.e. the function call edge with arg-to-param-assignment).
   *
   * @param formulasWithStatesAndGroupdIds formulas and abstract states, sorted according to position on the solver-stack.
   *                        we assume DIRECTION.FORWARDS as order, such that itpGroups and orderedFormulas are sorted equal.
   *
   * @return Pair (formulas := tree-elements, startOfSubTree := tree-structure),
   *         where a tree-element is the asserted formula (as normal formula for
   *         logging and as ITP-group) and the corresponding abstract state.
   */
  protected Pair<List<Triple<BooleanFormula, AbstractState, T>>, List<Integer>> buildTreeStructure(
          final List<Triple<BooleanFormula, AbstractState, T>> formulasWithStatesAndGroupdIds) {

    final List<Triple<BooleanFormula, AbstractState, T>> formulas = new ArrayList<>();
    final List<Integer> startOfSubTree = new ArrayList<>();
    final Deque<Pair<Triple<BooleanFormula, AbstractState, T>, Integer>> stack = new ArrayDeque<>();
    final Pair<Triple<BooleanFormula, AbstractState, T>, Integer> leftMostSubtree =
            Pair.of(formulasWithStatesAndGroupdIds.get(0), 0); // initial element of the tree

    stack.add(leftMostSubtree); // every tree starts at the left-most node, post-order!
    for (int positionOfA = 0; positionOfA < formulasWithStatesAndGroupdIds.size(); positionOfA++) {
      // first element is handled before

      final Triple<BooleanFormula, AbstractState, T> formula = formulasWithStatesAndGroupdIds.get(positionOfA);

      switch (getTreePosition(formulasWithStatesAndGroupdIds, positionOfA)) {
        case START: {
          // start new left subtree, i.e. next formula is left leaf of a subtree.
          // current formula will be used as merge-formula (common root of new subtree and previous formulas)
          stack.addLast(Pair.of(formula, formulas.size()));
          break;
        }
        case END: {
          // first add the last inner formula
          startOfSubTree.add(stack.getLast().getSecond());
          formulas.add(formula);

          // then add the common root (merge-formula)
          final Pair<Triple<BooleanFormula, AbstractState, T>, Integer> commonRoot = stack.removeLast();
          startOfSubTree.add(stack.getLast().getSecond());
          formulas.add(commonRoot.getFirst());

          assert commonRoot.getSecond() >= stack.getLast().getSecond()
                  : "adding a complete subtree can only be done on the right side";

          break;
        }
        case MIDDLE: {
          startOfSubTree.add(stack.getLast().getSecond());
          formulas.add(formula);
          break;
        }
        default:
          throw new AssertionError();
      }

      assert formulas.size() == startOfSubTree.size() : "invalid number of tree elements: " + startOfSubTree;
    }

    final Pair<Triple<BooleanFormula, AbstractState, T>, Integer> last = stack.removeLast();
    assert last == leftMostSubtree : "root must start at left-most subtree";
    assert stack.isEmpty() : "after building the tree-structure there should not be formulas on the stack";

    logger.log(Level.ALL, "formulas of tree are:", formulas);
    logger.log(Level.ALL, "subtree-structure is:", startOfSubTree);
    assert formulas.size() == formulasWithStatesAndGroupdIds.size() :
            "invalid number of tree elements: " + formulas.size() + " vs " + formulasWithStatesAndGroupdIds.size();

    return Pair.of(formulas, startOfSubTree);
  }

  /**
   * The default Predicate Analysis can only handle a flat list of interpolants.
   * Thus we convert the tree-structure back into a linear chain of interpolants.
   * The analysis must handle special cases on its own, i.e. use BAM with function-rebuilding.
   *
   * For function-entries (START-point) we use TRUE,
   * for function-returns (END-point) both function-summary and function-execution (merged into one formula).
   *
   * @param formulasWithStatesAndGroupdIds contains the input formulas and abstract states
   * @param itps tree-interpolants
   * @return interpolants linear chain of interpolants, created from the tree-interpolants
   */
  protected List<BooleanFormula> flattenTreeItps(
          final List<Triple<BooleanFormula, AbstractState, T>> formulasWithStatesAndGroupdIds,
          final List<BooleanFormula> itps) {
    final List<BooleanFormula> interpolants = new ArrayList<>();
    final Iterator<BooleanFormula> iter = itps.iterator();
    for (int positionOfA = 0; positionOfA < formulasWithStatesAndGroupdIds.size() - 1; positionOfA++) {
      // last interpolant would be False.

      final BooleanFormula itp;
      switch (getTreePosition(formulasWithStatesAndGroupdIds, positionOfA)) {
        case START: {
          itp = bfmgr.makeTrue();
          break;
        }
        case END: {
          // add the last inner formula and the common root (merge-formula)
          final BooleanFormula functionSummary = iter.next();
          final BooleanFormula functionExecution = iter.next();
          itp = rebuildInterpolant(functionSummary, functionExecution);
          break;
        }
        case MIDDLE: {
          itp = iter.next();
          break;
        }
        default:
          throw new AssertionError();
      }
      interpolants.add(itp);
    }

    assert !iter.hasNext() : "remaining interpolants: " + Lists.newArrayList(iter);

    return interpolants;
  }

  /**
   * We need all atoms of both interpolants in one formula,
   * If one of the formulas is True or False, we do not get Atoms from it. Thus we remove those cases.
   */
  protected BooleanFormula rebuildInterpolant(final BooleanFormula functionSummary, final BooleanFormula functionExecution) {
    final BooleanFormula rebuildItp;
    if (bfmgr.isTrue(functionSummary) || bfmgr.isFalse(functionSummary)) {
      rebuildItp = functionExecution;
    } else if (bfmgr.isTrue(functionExecution) || bfmgr.isFalse(functionExecution)) {
      rebuildItp = functionSummary;
    } else {
      // TODO operation OR is weak, we could also use AND.
      // There is no difference for the atoms later, because we filter out True and False here.
      rebuildItp = bfmgr.or(functionSummary, functionExecution);
    }
    return rebuildItp;
  }

}
