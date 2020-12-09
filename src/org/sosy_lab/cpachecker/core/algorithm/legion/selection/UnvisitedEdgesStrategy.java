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
import org.sosy_lab.cpachecker.core.algorithm.legion.LegionComponentStatistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;

public class UnvisitedEdgesStrategy implements Selector {

    final LogManager logger;
    final PathFormulaManager formulaManager;
    private LegionComponentStatistics stats;
    private Random random;
    private Set<PathFormula> blacklisted;

    public UnvisitedEdgesStrategy(LogManager logger, PathFormulaManager formulaManager) {
        this.logger = logger;
        this.formulaManager = formulaManager;
        this.random = new Random(1636672210L);
        this.blacklisted = new HashSet<>();

        this.stats = new LegionComponentStatistics("selection");
    }

    @Override
    public PathFormula select(ReachedSet pReachedSet) throws InterruptedException {
        this.stats.start();
        ARGState first = (ARGState) pReachedSet.getFirstState();
        List<PathFormula> found_states = new ArrayList<>();
        
        // Search through the arg
        depthSearch(first, found_states);

        // Select a state at random
        PathFormula selected = considerWeights(found_states);

        this.stats.finish();
        return selected;
    }

    /**
     * Take the found_states and try to retrieve one by considering the blacklist.
     */
    PathFormula considerWeights(List<PathFormula> found_states) {
        // Select a state at random
        PathFormula selected;
        while (true) {
            selected = found_states.remove(this.random.nextInt(found_states.size()));

            // If the just selected target is the last one,
            // No choice but to return it.
            if (found_states.isEmpty()){
                break;
            }

            // Otherwhise, check if it's blacklisted
            if (this.blacklisted.contains(selected)){
                continue;
            } else {
                break;
            }
        }
        return selected;
    }

    PathFormula makeFormula(ARGState pState, CFAEdge pEdge) throws InterruptedException,
            CPATransferException {
        PredicateAbstractState ps =
                AbstractStates
                        .extractStateByType(pState, PredicateAbstractState.class);

        return formulaManager.makeAnd(ps.getPathFormula(), pEdge);
    }

    /**
     * The random selector just blacklists states with a weight below zero.
     * Selection will then try to select a state not blacklisted, only
     * when there is no other choice it will return one.
     */
    @Override
    public void feedback(PathFormula pPathFormula, int pWeight) {
        if (pWeight < 0){
            this.blacklisted.add(pPathFormula);
        }
    }

    /**
     * Search through the states referenced by state in a depth-first manor in order to find the
     * first unvisited CFAEdge.
     */
    void depthSearch(ARGState state, List<PathFormula> found_edges)
            throws InterruptedException {
        for (CFAEdge unvisitedEdge : getUnvisitedEdges(state)){
            try {
                found_edges.add(this.makeFormula(state, unvisitedEdge));
            } catch (CPATransferException exc){
                logger.log(Level.SEVERE, "Could not do formula makeAnd", exc);
                continue;
            }
        }
        
        // If there are children, search them for unvisited Edge
        Collection<ARGState> children = state.getChildren();
        for (ARGState child : children) {

            try {
                depthSearch(child, found_edges);
            } catch (StackOverflowError e) {
                // If the stack is too deep, opt out of this path
                return;
            }
        }
    }

    /**
     * Checks for unvisited edges in the ARGState.
     * 
     * An unvisited edge is an edge in the CFA which leads to a node not reachable via the edges
     * from the ARG states.
     * 
     * @return null if no unvisited edges, the first unvisited edge otherwise
     */
    List<CFAEdge> getUnvisitedEdges(ARGState current_state) {

        LocationState current_ls =
                AbstractStates.extractStateByType(current_state, LocationState.class);
        Iterable<CFAEdge> current_edges = current_ls.getOutgoingEdges();
        List<CFAEdge> found_edges = new ArrayList<>();

        // Check each edge
        for (CFAEdge edge : current_edges) {
            // Get the next cfa node this edge would lead to
            CFANode target_node = edge.getSuccessor();

            // If the edge is anything other than a conditional edge, the fuzzer
            // will walk it. Only if there is a conditional to solve, it should be
            // selected.
            if (!edge.getEdgeType().equals(CFAEdgeType.AssumeEdge)) {
                continue;
            }

            // Now search the currents states children if we find one which
            // leads to target_node.
            boolean found = false;
            for (ARGState arg_child : current_state.getChildren()) {
                LocationState child_ls =
                        AbstractStates.extractStateByType(arg_child, LocationState.class);
                CFANode cfa_actual_node = child_ls.getLocationNode();
                if (target_node.equals(cfa_actual_node)) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                found_edges.add(edge);
            }
        }
        return found_edges;
    }
    

    @Override
    public LegionComponentStatistics getStats() {
        this.stats.set_other("blacklisted", (double)this.blacklisted.size());
        return this.stats;
    }

}
