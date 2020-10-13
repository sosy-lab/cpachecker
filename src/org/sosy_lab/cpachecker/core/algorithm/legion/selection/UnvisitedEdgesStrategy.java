package org.sosy_lab.cpachecker.core.algorithm.legion.selection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;

import com.google.common.collect.Lists;

import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.location.LocationState;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class UnvisitedEdgesStrategy implements Selector {

    final LogManager logger;
    final PathFormulaManager formulaManager;

    public UnvisitedEdgesStrategy(LogManager logger, PathFormulaManager formulaManager) {
        this.logger = logger;
        this.formulaManager = formulaManager;
    }

    @Override
    public BooleanFormula select(ReachedSet pReachedSet) {
        // Perform search
        ARGState first = (ARGState) pReachedSet.getFirstState();
        Pair<ARGState, CFAEdge> selected = depthSearch(first);

        // Extract needed formula
        PredicateAbstractState ps =
                AbstractStates
                        .extractStateByType(selected.getFirst(), PredicateAbstractState.class);

        // TODO manage exceptions
        PathFormula f = null;
        try {
            f = formulaManager.makeAnd(ps.getPathFormula(), selected.getSecond());
        } catch (InterruptedException ex) {
        } catch (CPATransferException ex) {
        }
        return f.getFormula();
    }

    /**
     * Search through the states referenced by state in a depth-first manor in order to find the
     * first unvisited CFAEdge.
     */
    Pair<ARGState, CFAEdge> depthSearch(ARGState state) {

        CFAEdge unvisitedEdge = getUnvisitedEdge(state);
        Collection<ARGState> children = state.getChildren();

        // If there is an unvisited edge, return it
        if (unvisitedEdge != null) {
            return Pair.of(state, unvisitedEdge);
        }

        // If there are no children and no edges => null
        if (children.size() == 0) {
            return Pair.of(state, null);
        }

        // If there are children, search them for unvisited Edge
        for (ARGState child : children) {
            Pair<ARGState, CFAEdge> searched = depthSearch(child);

            // If the child has a CFAEddge, return the found combination
            // If not, search continues!
            if (searched.getSecond() != null) {
                return searched;
            } else {
                continue;
            }
        }

        // If there are no unvisited edges remaining and no children had
        // unvisited children, this node does not have any unvisited edges.
        return Pair.of(state, null);
    }

    /**
     * Checks for unvisited edges in the ARGState.
     * 
     * Searches for CFAEdges leading to children states not contained in the states children.
     * 
     * @return null if no unvisited edges, the first unvisited edge otherwise
     */
    CFAEdge getUnvisitedEdge(ARGState state) {
        LocationState ls = AbstractStates.extractStateByType(state, LocationState.class);
        ArrayList<CFAEdge> outgoingEdges = Lists.newArrayList(ls.getOutgoingEdges());
        ArrayList<ARGState> currentChildren = Lists.newArrayList(state.getChildren());

        // If the sizes match, there are children for all outgoing edges,
        // so no edges hasn't been visited.
        if (outgoingEdges.size() == currentChildren.size()) {
            return null;
        }

        // ValueAnalysisState as = AbstractStates.extractStateByType(state,
        // ValueAnalysisState.class);

        // Check for every outgoing edge, if a child exists with the same
        // CFANode
        for (CFAEdge edge : outgoingEdges) {
            CFANode possibleSuccessor = edge.getSuccessor();

            Boolean found = false;
            for (ARGState child : currentChildren) {
                LocationState alreadyPresentLocation =
                        AbstractStates.extractStateByType(child, LocationState.class);

                // If the already present childs location node equals the possible successor,
                // a match is found and search continues
                if (alreadyPresentLocation.getLocationNode().equals(possibleSuccessor)) {
                    found = true;
                    break;
                }
            }

            // If the edge was not found in any of the currentChildren, it is an unvisited edge
            if (!found) {
                logger.log(Level.INFO, "Found unvisited edge", edge);
                return edge;
            }

        }

        return null;
    }

}
