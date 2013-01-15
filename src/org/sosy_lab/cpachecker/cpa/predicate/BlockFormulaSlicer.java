/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.toState;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.sosy_lab.cpachecker.cfa.model.ADeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.AReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.BooleanFormula;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;


public class BlockFormulaSlicer {

  final private PathFormulaManager pfmgr;
  final private Collection<CFAEdge> importantEdges = new HashSet<>();

  private static final Function<PredicateAbstractState, BooleanFormula> GET_BLOCK_FORMULA =
      new Function<PredicateAbstractState, BooleanFormula>() {

        @Override
        public BooleanFormula apply(PredicateAbstractState e) {
          assert e.isAbstractionState();
          return e.getAbstractionFormula().getBlockFormula();
        }
      };

  private static final Function<PathFormula, BooleanFormula> GET_BOOLEAN_FORMULA =
      new Function<PathFormula, BooleanFormula>() {

        @Override
        public BooleanFormula apply(PathFormula pf) {
          return pf.getFormula();
        }
      };

  public BlockFormulaSlicer(PathFormulaManager pPfmgr) {
    this.pfmgr = pPfmgr;
  }

  public List<BooleanFormula> sliceFormulasForPath(List<ARGState> path, ARGState initialState)
      throws CPATransferException {

    // first find all ARGStates for each sub-path
    Map<ARGState, Collection<ARGState>> subpaths = new HashMap<>(path.size());
    for (int i = 0; i < path.size(); i++) {
      ARGState start = i > 0 ? path.get(i - 1) : initialState;
      ARGState end = path.get(i);
      subpaths.put(start, getARGStatesOfSubpath(start, end));
    }

    assert path.size() == subpaths.size();

    // slice each sub-path, we do this backwards
    for (int i = path.size() - 1; i >= 0; i--) {
      ARGState start = i > 0 ? path.get(i - 1) : initialState;
      ARGState end = path.get(i);
      sliceSubpath(start, end, subpaths.get(start));
    }

    // build new pathformulas
    PathFormula pf = pfmgr.makeEmptyPathFormula();
    List<PathFormula> pfs = new ArrayList<>(path.size());
    for (int i = 0; i < path.size(); i++) {
      PathFormula oldPf = pfmgr.makeEmptyPathFormula(pf);
      ARGState start = i > 0 ? path.get(i - 1) : initialState;
      ARGState end = path.get(i);
      pf = buildFormula(start, end, subpaths.get(start), oldPf);
      pfs.add(pf);
    }

    ImmutableList<BooleanFormula> list = from(pfs)
        .transform(GET_BOOLEAN_FORMULA)
        .toImmutableList();

    System.out.println("\n\nFORMULA::");
    for (BooleanFormula formula : list) {
      System.out.println(formula);
    }

    ImmutableList<BooleanFormula> origlist = from(path)
        .transform(toState(PredicateAbstractState.class))
        .transform(GET_BLOCK_FORMULA)
        .toImmutableList();

    System.out.println("\n\nORIG FORMULA::");
    for (BooleanFormula formula : origlist) {
      System.out.println(formula);
    }

    return origlist;
  }

  /** This function returns all states, that are contained in a subpath.
   * The subpath is the union of all paths, that end in the end-state.
   * We assume, that all paths begin in the start-state (that may be null).
   * The returned collection includes the end-state and the start-state. */
  private Collection<ARGState> getARGStatesOfSubpath(ARGState start, ARGState end) {
    Collection<ARGState> states = new HashSet<>();
    states.add(start); // start is the last state to be reachable backwards
    states.add(end); // end is the first state to be reachable backwards

    // backwards-bfs for parents, visit each state once
    List<ARGState> waitlist = new LinkedList<>();
    waitlist.add(end);
    while (!waitlist.isEmpty()) {
      final ARGState current = waitlist.remove(0);
      for (ARGState parent : current.getParents()) {
        // stop, if state was seen before
        if (!states.contains(parent)) {
          states.add(parent);
          waitlist.add(parent);
        }
      }
    }

    return states;
  }

  private void sliceSubpath(ARGState start, ARGState end,
      Collection<ARGState> subpath) {
    Collection<ARGState> visited = new HashSet<>();
    visited.add(end);

    // backwards-bfs for parents, visit each state once
    List<ARGState> waitlist = new LinkedList<>();
    waitlist.add(end);
    while (!waitlist.isEmpty()) {
      final ARGState current = waitlist.remove(0);

      // if there are some children remaining,
      // add state again for working with it later
      if (!isAllChildrenDone(current, visited, subpath)) {
        waitlist.add(current);
        continue;
      }

      // add parents to waitlist and work with all incoming edges
      for (ARGState parent : current.getParents()) {

        // skip, if state is not part of subpath
        if (!subpath.contains(parent)) {
          continue;
        }

        if (!visited.contains(parent)) {
          visited.add(parent);
          waitlist.add(parent);
        }

        // do the hard work
        CFAEdge edge = parent.getEdgeToChild(current);
        handleEdge(edge, parent, current);
      }
    }

    // logging
    System.out.println("START::  " + (start == null ? null : start.getStateId()));
    System.out.println("END::    " + end.getStateId());
    System.out.print("SUBPATH::  ");
    for (ARGState current : subpath) {
      System.out.print(current.getStateId() + ", ");
    }
    System.out.println();
    System.out.print("VISITED::  ");
    for (ARGState current : visited) {
      System.out.print(current.getStateId() + ", ");
    }
    System.out.println("\n\n");
  }

  /** This function returns, if all children of a state,
   * that are reachable backwards from the state, are visited. */
  private boolean isAllChildrenDone(ARGState s,
      Collection<ARGState> visited, Collection<ARGState> subpath) {
    for (ARGState child : s.getChildren()) {
      if (subpath.contains(child) && !visited.contains(child)) { return false; }
    }
    return true;
  }

  /** This function returns, if all parents of a state,
   * that are reachable from the state, are visited. */
  private boolean isAllParentsDone(ARGState s,
      Collection<ARGState> visited, Collection<ARGState> subpath) {
    for (ARGState parent : s.getParents()) {
      if (subpath.contains(parent) && !visited.contains(parent)) { return false; }
    }
    return true;
  }

  /** This function chooses, which edges are important for the formula. */
  private void handleEdge(CFAEdge edge, ARGState stateBefore, ARGState stateAfter) {
    System.out.println("WORK   " +
        stateBefore.getStateId() + " -> " +
        stateAfter.getStateId() + "    \t" +
        edge.getRawStatement() + "    \t" +
        edge.getClass().toString().replace("class org.sosy_lab.cpachecker.cfa.model.", ""));

    importantEdges.add(edge);

    // check the type of the edge
    switch (edge.getEdgeType()) {

    // int a;
    case DeclarationEdge:
      handleDeclaration((ADeclarationEdge) edge);
      break;

    // if (a == b) {...}
    case AssumeEdge:
      handleAssumption((AssumeEdge) edge);
      break;

    // a = b + c;
    case StatementEdge:
      handleStatement((AStatementEdge) edge);
      break;

    // return (x);
    case ReturnStatementEdge:
      handleReturnStatement((AReturnStatementEdge) edge);
      break;

    // assignment from y = f(x);
    case FunctionReturnEdge:
      break;

    case BlankEdge:
      break;

    // call from y = f(x);
    case FunctionCallEdge:
      break;

    default:
      throw new AssertionError("unhandled edge");
    }


  }


  // TODO
  private void handleDeclaration(ADeclarationEdge pEdge) {}

  private void handleStatement(AStatementEdge pEdge) {}

  private void handleReturnStatement(AReturnStatementEdge pEdge) {}

  private void handleAssumption(AssumeEdge pEdge) {}

  /** This function returns a PathFormula for the whole subpath from start to end.
   * The SSA-indices of the new formula are based on the old formula. */
  private PathFormula buildFormula(ARGState start, ARGState end,
      Collection<ARGState> subpath, PathFormula oldPf) throws CPATransferException {

    // this map contains all done states with their formulas
    Map<ARGState, PathFormula> s2f = new HashMap<>(subpath.size());

    // bfs for parents, visit each state once
    List<ARGState> waitlist = new LinkedList<>();

    // special handling of first state
    s2f.put(start, oldPf);
    for (ARGState child : start.getChildren()) {
      if (subpath.contains(child)) {
        waitlist.add(child);
      }
    }

    while (!waitlist.isEmpty()) {
      final ARGState current = waitlist.remove(0);

      // collect new states, ignore unreachable states
      for (ARGState child : current.getChildren()) {
        if (subpath.contains(child)) {
          waitlist.add(child);
        }
      }

      // we have to wait for all parents completed,
      // because we want to join the branches
      if (!isAllParentsDone(current, s2f.keySet(), subpath)) {
        waitlist.add(current);
        continue;
      }

      // handle state
      PathFormula pf = makeFormulaForState(current, s2f);
      s2f.put(current, pf);

    }
    return s2f.get(end);
  }


  private PathFormula makeFormulaForState(ARGState current, Map<ARGState, PathFormula> s2f)
      throws CPATransferException {

    assert current.getParents().size() > 0 : "no parent for " + current.getStateId();

    // join all formulas from parents
    List<PathFormula> pfs = new ArrayList<>(current.getParents().size());
    for (ARGState parent : current.getParents()) {
      CFAEdge edge = parent.getEdgeToChild(current);
      PathFormula oldPf = s2f.get(parent);
      pfs.add(buildFormulaForEdge(edge, oldPf));
    }

    PathFormula joined = pfs.get(0);
    for (int i = 1; i < pfs.size(); i++) {
      joined = pfmgr.makeOr(joined, pfs.get(i));
    }

    return joined;
  }

  private PathFormula buildFormulaForEdge(CFAEdge edge, PathFormula oldFormula)
      throws CPATransferException {
    if (importantEdges.contains(edge)){
      return pfmgr.makeAnd(oldFormula, edge);
    } else {
      return oldFormula;
    }
  }

}
