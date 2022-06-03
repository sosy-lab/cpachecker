// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.collect.Iterables.getOnlyElement;
import static org.sosy_lab.cpachecker.util.AbstractStates.extractLocation;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.overflow.OverflowState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.predicates.pathformula.SSAMap;
import org.sosy_lab.java_smt.api.BooleanFormula;

public final class BAMBlockFormulaStrategy extends BlockFormulaStrategy {

  private final PathFormulaManager pfmgr;

  public BAMBlockFormulaStrategy(PathFormulaManager pPfmgr) {
    pfmgr = pPfmgr;
  }

  @Override
  BlockFormulas getFormulasForPath(final ARGState pRoot, final List<ARGState> pPath)
      throws CPATransferException, InterruptedException {
    // the elements in the path are not expanded, so they contain the path formulas
    // with the wrong indices
    // we need to re-create all path formulas in the flattened ARG

    final Map<ARGState, ARGState> callStacks =
        new HashMap<>(); // contains states and their next higher callstate
    final Map<ARGState, PathFormula> finishedFormulas = new HashMap<>();
    final ImmutableList.Builder<BooleanFormula> abstractionFormulas = ImmutableList.builder();
    final Deque<ARGState> waitlist = new ArrayDeque<>();

    // map from states to formulas for truth assumption path formula
    final ImmutableMap.Builder<Pair<ARGState, CFAEdge>, PathFormula> branchingFormulas =
        ImmutableMap.builder();

    // initialize
    assert pRoot.getParents().isEmpty() : "rootState must be the first state of the program";
    callStacks.put(pRoot, null); // main-start has no callstack
    finishedFormulas.put(pRoot, pfmgr.makeEmptyPathFormula());
    waitlist.addAll(pRoot.getChildren());

    // iterate over all elements in the ARG with BFS
    while (!waitlist.isEmpty()) {
      final ARGState currentState = waitlist.pollFirst();
      if (finishedFormulas.containsKey(currentState)) {
        continue; // already handled
      }

      if (!finishedFormulas.keySet().containsAll(currentState.getParents())) {
        // parent not handled yet, re-queue current element and wait for all parents
        waitlist.addLast(currentState);
        continue;
      }

      // collect formulas for current location
      final List<PathFormula> currentFormulas = new ArrayList<>(currentState.getParents().size());
      final List<ARGState> currentStacks = new ArrayList<>(currentState.getParents().size());
      for (ARGState parentElement : currentState.getParents()) {
        PathFormula parentFormula = finishedFormulas.get(parentElement);
        final List<CFAEdge> edges = parentElement.getEdgesToChild(currentState);
        assert !edges.isEmpty() : "ARG is invalid: parent has no edge to child";

        final ARGState prevCallState;

        boolean isSingleEdge = edges.size() == 1;

        // we enter a function, so lets add the previous state to the stack
        if (isSingleEdge
            && Iterables.getOnlyElement(edges).getEdgeType() == CFAEdgeType.FunctionCallEdge) {
          prevCallState = parentElement;

        } else if (isSingleEdge
            && Iterables.getOnlyElement(edges).getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
          // we leave a function, so rebuild return-state before assigning the return-value.
          // rebuild states with info from previous state
          assert callStacks.containsKey(parentElement);
          final ARGState callState = callStacks.get(parentElement);

          assert Objects.equals(
                  extractLocation(callState).getLeavingSummaryEdge().getSuccessor(),
                  extractLocation(currentState))
              : "callstack does not match entry of current function-exit.";
          assert callState != null || currentState.getChildren().isEmpty()
              : "returning from empty callstack is only possible at program-exit";

          prevCallState = callStacks.get(callState);
          parentFormula =
              rebuildStateAfterFunctionCall(
                  parentFormula,
                  finishedFormulas.get(callState),
                  (FunctionExitNode) extractLocation(parentElement));

        } else {
          assert callStacks.containsKey(parentElement); // check for null is not enough
          prevCallState = callStacks.get(parentElement);
        }

        PathFormula currentFormula = strengthen(parentElement, parentFormula);
        for (CFAEdge edge : edges) {
          currentFormula = pfmgr.makeAnd(currentFormula, edge);
          if (edge.getEdgeType() == CFAEdgeType.AssumeEdge) {
            PathFormula f = pfmgr.makeEmptyPathFormulaWithContextFrom(parentFormula);
            f = pfmgr.makeAnd(f, edge);
            Pair<ARGState, CFAEdge> key = Pair.of(parentElement, edge);
            branchingFormulas.put(key, f);
          }
        }
        currentFormulas.add(currentFormula);
        currentStacks.add(prevCallState);
      }

      assert currentFormulas.size() >= 1 : "each state except root must have parents";
      assert currentStacks.size() == currentFormulas.size()
          : "number of callstacks must match predecessors";

      // merging after functioncall with different callstates is ugly.
      // this is also guaranteed by the abstraction-locations at function-entries
      // (--> no merge of states with different latest abstractions).
      assert new HashSet<>(currentStacks).size() <= 1
          : "function with multiple entry-states not supported";

      callStacks.put(currentState, currentStacks.get(0));

      PathFormula currentFormula;
      final PredicateAbstractState predicateElement =
          PredicateAbstractState.getPredicateState(currentState);
      if (predicateElement.isAbstractionState()) {
        // abstraction element is the start of a new part of the ARG

        assert waitlist.isEmpty() : "todo should be empty, because of the special ARG structure";
        assert currentState.getParents().size() == 1
            : "there should be only one parent, because of the special ARG structure";

        // finishedFormulas.clear(); // free some memory
        // TODO disabled, we need to keep callStates for later usage

        // start new block with empty formula
        currentFormula = getOnlyElement(currentFormulas);
        BooleanFormula bFormula =
            pfmgr.addBitwiseAxiomsIfNeeded(
                currentFormula.getFormula(), currentFormula.getFormula());
        abstractionFormulas.add(bFormula);
        currentFormula = pfmgr.makeEmptyPathFormulaWithContextFrom(currentFormula);

      } else {
        // merge the formulas
        Iterator<PathFormula> it = currentFormulas.iterator();
        currentFormula = it.next();
        while (it.hasNext()) {
          currentFormula = pfmgr.makeOr(currentFormula, it.next());
        }
      }

      assert !finishedFormulas.containsKey(currentState) : "a state should only be finished once";
      finishedFormulas.put(currentState, currentFormula);
      waitlist.addAll(currentState.getChildren());
    }
    return new BlockFormulas(abstractionFormulas.build(), branchingFormulas.buildOrThrow());
  }

  /** Add assumptions from OverflowCPA. */
  private PathFormula strengthen(final ARGState currentState, PathFormula currentFormula)
      throws CPATransferException, InterruptedException {
    OverflowState other = AbstractStates.extractStateByType(currentState, OverflowState.class);
    if (other != null) {
      for (CExpression assumption : Iterables.filter(other.getAssumptions(), CExpression.class)) {
        currentFormula = pfmgr.makeAnd(currentFormula, assumption);
      }
    }
    return currentFormula;
  }

  /* rebuild indices from outer scope */
  public static PathFormula rebuildStateAfterFunctionCall(
      final PathFormula parentFormula,
      final PathFormula rootFormula,
      final FunctionExitNode functionExitNode) {
    final SSAMap newSSA =
        BAMPredicateReducer.updateIndices(
            rootFormula.getSsa(), parentFormula.getSsa(), functionExitNode);
    // TODO: seems buggy because it ignores PointerTargetSet
    return parentFormula.withContext(newSSA, parentFormula.getPointerTargetSet());
  }
}
