package org.sosy_lab.cpachecker.core.algorithm.legion.selection;

import java.util.Collection;
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
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManager;

public class UnvisitedEdgesStrategy implements Selector {

    final LogManager logger;
    final PathFormulaManager formulaManager;

    public UnvisitedEdgesStrategy(LogManager logger, PathFormulaManager formulaManager) {
        this.logger = logger;
        this.formulaManager = formulaManager;
    }

    @Override
    public PathFormula select(ReachedSet pReachedSet) {
        // Perform search
        ARGState first = (ARGState) pReachedSet.getFirstState();
        Pair<ARGState, CFAEdge> selected = depthSearch(first);

        if (selected.getSecond() == null) {
            return null;
        }

        // Extract needed formula
        PredicateAbstractState ps =
                AbstractStates
                        .extractStateByType(selected.getFirst(), PredicateAbstractState.class);

        // TODO manage exceptions
        PathFormula f = null;
        try {
            f = formulaManager.makeAnd(ps.getPathFormula(), selected.getSecond());
        } catch (InterruptedException ex) {
            logger.log(Level.SEVERE, "Could not finish formula makeAnd", ex);
        } catch (CPATransferException ex) {
            logger.log(Level.SEVERE, "Could not do formula makeAnd", ex);
        }
        return f;
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
            logger.log(Level.SEVERE, state.toString(), unvisitedEdge.toString());
            return Pair.of(state, unvisitedEdge);
        }

        // If there are no children and no edges => null
        if (children.size() == 0) {
            return Pair.of(state, null);
        }

        // If there are children, search them for unvisited Edge
        for (ARGState child : children) {

            Pair<ARGState, CFAEdge> searched;
            try {
                searched = depthSearch(child);
            } catch (StackOverflowError e){
                // If the stack is too deep, opt out of this path
                return Pair.of(state, null);
            }

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
     * An unvisited edge is an edge in the CFA which leads to a node not reachable via the edges
     * from the ARG states.
     * 
     * @return null if no unvisited edges, the first unvisited edge otherwise
     */
    CFAEdge getUnvisitedEdge(ARGState current_state) {

        LocationState current_ls =
                AbstractStates.extractStateByType(current_state, LocationState.class);
        Iterable<CFAEdge> current_edges = current_ls.getOutgoingEdges();

        for (CFAEdge edge : current_edges) {
            // The next cfa node this edge would lead to
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
                return edge;
            }
        }
        return null;
    }

}
