// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
package org.sosy_lab.cpachecker.core.algorithm.legion.selection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

public class UnvisitedEdgesStrategy implements Selector {

  private final LogManager logger;
  private final PathFormulaManager formulaManager;
  private final Random random = new Random(1636672210L);
  private final Set<PathFormula> blacklisted = new HashSet<>();
  private final StatTimer iterationTimer = new StatTimer(StatKind.SUM, "Selection time");

  public UnvisitedEdgesStrategy(LogManager logger, PathFormulaManager formulaManager) {
    this.logger = logger;
    this.formulaManager = formulaManager;
  }

  @Override
  public PathFormula select(ReachedSet pReachedSet) throws InterruptedException {
    this.iterationTimer.start();
    ARGState first = (ARGState) pReachedSet.getFirstState();
    List<PathFormula> foundStates = new ArrayList<>();

    // Search through the arg
    depthSearch(first, foundStates);

    // Select a state at random
    PathFormula selected = considerWeights(foundStates);

    this.iterationTimer.stop();
    return selected;
  }

  /** Take the found_states and try to retrieve one by considering the blacklist. */
  PathFormula considerWeights(List<PathFormula> foundStates) {
    // Select a state at random
    PathFormula selected;
    while (true) {
      selected = foundStates.remove(this.random.nextInt(foundStates.size()));

      // If the just selected target is the last one,
      // No choice but to return it.
      if (foundStates.isEmpty()) {
        break;
      }

      // Otherwhise, check if it's blacklisted
      if (this.blacklisted.contains(selected)) {
        continue;
      } else {
        break;
      }
    }
    return selected;
  }

  PathFormula makeFormula(ARGState pState, CFAEdge pEdge)
      throws InterruptedException, CPATransferException {
    PredicateAbstractState ps =
        AbstractStates.extractStateByType(pState, PredicateAbstractState.class);

    return formulaManager.makeAnd(ps.getPathFormula(), pEdge);
  }

  /**
   * The random selector just blacklists states with a weight below zero. Selection will then try to
   * select a state not blacklisted, only when there is no other choice it will return one.
   */
  @Override
  public void feedback(PathFormula pPathFormula, int pWeight) {
    if (pWeight < 0) {
      this.blacklisted.add(pPathFormula);
    }
  }

  /**
   * Search through the states referenced by state in a depth-first manor in order to find the first
   * unvisited CFAEdge.
   */
  void depthSearch(ARGState state, List<PathFormula> foundEdges) throws InterruptedException {
    for (CFAEdge unvisitedEdge : getUnvisitedEdges(state)) {
      try {
        foundEdges.add(this.makeFormula(state, unvisitedEdge));
      } catch (CPATransferException exc) {
        logger.logUserException(Level.INFO, exc, "Could not do formula makeAnd");
        continue;
      }
    }

    // If there are children, search them for unvisited Edge
    Collection<ARGState> children = state.getChildren();
    for (ARGState child : children) {
      depthSearch(child, foundEdges);
    }
  }

  /**
   * Checks for unvisited edges in the ARGState.
   *
   * <p>An unvisited edge is an edge in the CFA which leads to a node not reachable via the edges
   * from the ARG states.
   *
   * @return null if no unvisited edges, the first unvisited edge otherwise
   */
  List<CFAEdge> getUnvisitedEdges(ARGState currentState) {

    LocationState currentLocationState =
        AbstractStates.extractStateByType(currentState, LocationState.class);
    Iterable<CFAEdge> currentEdges = currentLocationState.getOutgoingEdges();
    List<CFAEdge> foundEdges = new ArrayList<>();

    // Check each edge
    for (CFAEdge edge : currentEdges) {
      // Get the next cfa node this edge would lead to
      CFANode targetNode = edge.getSuccessor();

      // If the edge is anything other than a conditional edge, the fuzzer
      // will walk it. Only if there is a conditional to solve, it should be
      // selected.
      if (!edge.getEdgeType().equals(CFAEdgeType.AssumeEdge)) {
        continue;
      }

      // Now search the currents states children if we find one which
      // leads to targetNode.
      if (!AbstractStates.extractLocations(currentState.getChildren()).contains(targetNode)) {
        foundEdges.add(edge);
      }
    }
    return foundEdges;
  }

  @Override
  public void writeStats(StatisticsWriter writer) {
    writer.put("Selection Kind", "UnvisitedEdgesStrategy");
    writer.put(this.iterationTimer);
    writer.put("Selection Iterations", this.iterationTimer.getUpdateCount());
    writer.put("Blacklisted States", this.blacklisted.size());
  }
}
