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
package org.sosy_lab.cpachecker.util.coverage;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.cpachecker.util.AbstractStates.EXTRACT_LOCATION;

import com.google.common.collect.Iterables;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Class responsible for extracting coverage information.
 */
public abstract class CoverageCollector {

  public static CoverageData fromReachedSet(Iterable<AbstractState> pReached, CFA cfa) {
    return new ReachedSetCoverageCollector().collectFromReachedSet(pReached, cfa);
  }

  public static CoverageData fromCounterexample(ARGPath pPath) {
    return new CounterexampleCoverageCollector().collectFromCounterexample(pPath);
  }
}

class CounterexampleCoverageCollector {

  /**
   * Coverage from a counterexample does not report all existing edges, but the set of existing
   * edges needs to contain all covered edges at the minimum.
   */
  CoverageData collectFromCounterexample(ARGPath cexPath) {
    CoverageData cov = new CoverageData();
    collectCoveredEdges(cexPath, cov);
    return cov;
  }

  private boolean isOutsideAssumptionAutomaton(ARGState s) {
    boolean foundAssumptionAutomaton = false;
    for (AutomatonState aState : AbstractStates.asIterable(s).filter(AutomatonState.class)) {
      if (aState.getOwningAutomatonName().equals("AssumptionAutomaton")) {
        foundAssumptionAutomaton = true;
        if (aState.getInternalStateName().equals("__FALSE")) {
          return true;
        }
      }
    }
    if (!foundAssumptionAutomaton) {
      throw new IllegalArgumentException(
          "This method should only be called when an " +
          "Assumption Automaton is used as part of the specification.");
    }
    return false;
  }

  private void collectCoveredEdges(ARGPath cexPath, CoverageData cov) {
    PathIterator pathIterator = cexPath.fullPathIterator();
    while (pathIterator.hasNext()) {
      CFAEdge edge = pathIterator.getOutgoingEdge();

      // Considering covered up until (but not including) when the
      // AssumptionAutomaton state is __FALSE.
      if (isOutsideAssumptionAutomaton(pathIterator.getNextAbstractState())) {
        break;
      }
      cov.addVisitedEdge(edge);
      pathIterator.advance();
    }
  }
}

class ReachedSetCoverageCollector {

  CoverageData collectFromReachedSet(Iterable<AbstractState> reached, CFA cfa) {
    CoverageData cov = new CoverageData();
    cov.putCFA(cfa);

    // Add information about visited functions
    for (FunctionEntryNode entryNode :
        AbstractStates.extractLocations(reached)
            .filter(notNull())
            .filter(FunctionEntryNode.class)) {

      final FileLocation loc = entryNode.getFileLocation();
      if (loc.getStartingLineNumber() == 0) {
        // dummy location
        continue;
      }

      cov.addVisitedFunction(entryNode);
    }

    collectCoveredEdges(reached, cov);

    return cov;
  }

  private void collectCoveredEdges(Iterable<AbstractState> reached, CoverageData cov) {
    Set<CFANode> reachedNodes = from(reached)
        .transform(EXTRACT_LOCATION)
        .filter(notNull())
        .toSet();
    //Add information about visited locations

    for (AbstractState state : reached) {
      ARGState argState = AbstractStates.extractStateByType(state, ARGState.class);
      if (argState != null ) {
        for (ARGState child : argState.getChildren()) {
          // Do not specially check child.isCovered, as the edge to covered state also should be marked as covered edge
          List<CFAEdge> edges = argState.getEdgesToChild(child);
          if (edges.size() > 1) {
            for (CFAEdge innerEdge : edges) {
              cov.addVisitedEdge(innerEdge);
            }

            //BAM produces paths with no edge connection thus the list will be empty
          } else if (!edges.isEmpty()) {
            cov.addVisitedEdge(Iterables.getOnlyElement(edges));
          }
        }
      } else {
        //Simple kind of analysis
        //Cover all edges from reached nodes
        //It is less precise, but without ARG it is impossible to know what path we chose
        CFANode node = AbstractStates.extractLocation(state);
        for (int i = 0; i < node.getNumLeavingEdges(); i++) {
          CFAEdge edge = node.getLeavingEdge(i);
          if (reachedNodes.contains(edge.getSuccessor())) {
            cov.addVisitedEdge(edge);
          }
        }
      }
    }
  }
}