/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.fshell.artreuse;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.art.ARTCPA;
import org.sosy_lab.cpachecker.cpa.art.ARTElement;
import org.sosy_lab.cpachecker.cpa.art.ARTReachedSet;
import org.sosy_lab.cpachecker.cpa.composite.CompositeElement;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonStateElement;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton.ProductAutomatonElement;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.ecp.ECPEdgeSet;
import org.sosy_lab.cpachecker.util.ecp.translators.GuardedEdgeLabel;

public class ARTReuse {

  public static void modifyReachedSet(ReachedSet pReachedSet, CFAFunctionDefinitionNode pEntryNode, ARTCPA pARTCPA, int pProductAutomatonIndex, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pPreviousAutomaton, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pCurrentAutomaton) {
    if (pReachedSet.isEmpty()) {
      AbstractElement lInitialElement = pARTCPA.getInitialElement(pEntryNode);
      Precision lInitialPrecision = pARTCPA.getInitialPrecision(pEntryNode);

      pReachedSet.add(lInitialElement, lInitialPrecision);
    }
    else {
      if (pPreviousAutomaton == null) {
        throw new RuntimeException();
      }

      modifyART(pReachedSet, pARTCPA, pProductAutomatonIndex, pPreviousAutomaton, pCurrentAutomaton);
    }
  }

  private static void modifyART(ReachedSet pReachedSet, ARTReachedSet pARTReachedSet, int pProductAutomatonIndex, Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge> pFrontierEdges) {
    //Set<Pair<ARTElement, ARTElement>> lPathEdges = Collections.emptySet();
    //ARTStatistics.dumpARTToDotFile(new File("/home/andreas/art01.dot"), lARTCPA, pReachedSet, lPathEdges);

    for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lEdge : pFrontierEdges) {
      GuardedEdgeLabel lLabel = lEdge.getLabel();

      ECPEdgeSet lEdgeSet = lLabel.getEdgeSet();

      for (CFAEdge lCFAEdge : lEdgeSet) {
        CFANode lCFANode = lCFAEdge.getPredecessor();

        Set<AbstractElement> lAbstractElements = pReachedSet.getReached(lCFANode);

        LinkedList<AbstractElement> lAbstractElements2 = new LinkedList<AbstractElement>();
        lAbstractElements2.addAll(lAbstractElements);

        for (AbstractElement lAbstractElement : lAbstractElements2) {
          if (!pReachedSet.contains(lAbstractElement)) {
            // lAbstractElement was removed in an earlier step
            continue;
          }

          ARTElement lARTElement = (ARTElement)lAbstractElement;

          if (lARTElement.retrieveLocationElement().getLocationNode() != lCFANode) {
            continue;
          }

          // what's the semantics of getWrappedElement*s*()?
          CompositeElement lCompositeElement = (CompositeElement)lARTElement.getWrappedElement();

          ProductAutomatonElement lProductAutomatonElement = (ProductAutomatonElement)lCompositeElement.get(pProductAutomatonIndex);

          GuardedEdgeAutomatonStateElement lStateElement = (GuardedEdgeAutomatonStateElement)lProductAutomatonElement.get(0);

          if (lStateElement.getAutomatonState() == lEdge.getSource()) {
            if (lARTElement.getChildren().isEmpty()) {
              // re-add element to worklist
              pReachedSet.reAddToWaitlist(lARTElement);
            }
            else {
              // by removing the children, lARTElement gets added to the
              // worklist automatically

              /* TODO add removal of only non-isomorphic parts again */
              while (!lARTElement.getChildren().isEmpty()) {
                ARTElement lChildElement = lARTElement.getChildren().iterator().next();

                pARTReachedSet.removeSubtree(lChildElement);
              }
            }
          }
        }
      }
    }
  }

  private static void modifyART(ReachedSet pReachedSet, ARTCPA pARTCPA, int pProductAutomatonIndex, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pPreviousAutomaton, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pCurrentAutomaton) {
    ARTReachedSet lARTReachedSet = new ARTReachedSet(pReachedSet, pARTCPA);

    Pair<Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>, Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>> lFrontier = determineFrontier(pPreviousAutomaton, pCurrentAutomaton);

    modifyART(pReachedSet, lARTReachedSet, pProductAutomatonIndex, lFrontier.getFirst());
    modifyART(pReachedSet, lARTReachedSet, pProductAutomatonIndex, lFrontier.getSecond());
  }

  private static Pair<Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>, Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>> determineLocalDifference(NondeterministicFiniteAutomaton<GuardedEdgeLabel> pPreviousAutomaton, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pCurrentAutomaton, NondeterministicFiniteAutomaton.State pPreviousState, NondeterministicFiniteAutomaton.State pCurrentState) {
    Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge> lE1 = new HashSet<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>();
    Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge> lE2 = new HashSet<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>();

    for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lEdge : pPreviousAutomaton.getOutgoingEdges(pPreviousState)) {
      boolean lFound = false;

      if (lEdge.getLabel().hasGuards()) {
        // TODO extend implementation to guards
        throw new RuntimeException("No support for guards!");
      }

      for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lOtherEdge : pCurrentAutomaton.getOutgoingEdges(pCurrentState)) {
        if (lEdge.getTarget() == lOtherEdge.getTarget()) {
          if (lEdge.getLabel().equals(lOtherEdge.getLabel())) {
            lFound = true;
          }
        }
      }

      if (!lFound) {
        lE1.add(lEdge);
      }
    }

    for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lEdge : pCurrentAutomaton.getOutgoingEdges(pCurrentState)) {
      boolean lFound = false;

      if (lEdge.getLabel().hasGuards()) {
        // TODO extend implementation to guards
        throw new RuntimeException("No support for guards!");
      }

      for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lOtherEdge : pPreviousAutomaton.getOutgoingEdges(pPreviousState)) {
        if (lEdge.getTarget() == lOtherEdge.getTarget()) {
          if (lEdge.getLabel().equals(lOtherEdge.getLabel())) {
            lFound = true;
          }
        }
      }

      if (!lFound) {
        lE2.add(lEdge);
      }
    }

    return Pair.of(lE1, lE2);
  }

  private static Pair<Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>, Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>> determineFrontier(NondeterministicFiniteAutomaton<GuardedEdgeLabel> pPreviousAutomaton, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pCurrentAutomaton) {
    Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge> lF1 = new HashSet<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>();
    Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge> lF2 = new HashSet<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>();

    LinkedList<Pair<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State>> lWorklist = new LinkedList<Pair<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State>>();

    if (pPreviousAutomaton.getInitialState() != pCurrentAutomaton.getInitialState()) {
      throw new RuntimeException();
    }

    lWorklist.add(Pair.of(pPreviousAutomaton.getInitialState(), pCurrentAutomaton.getInitialState()));

    HashSet<Pair<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State>> lVisited = new HashSet<Pair<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State>>();

    while (!lWorklist.isEmpty()) {
      Pair<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State> lCurrentPair = lWorklist.removeLast();

      if (!lVisited.contains(lCurrentPair)) {
        lVisited.add(lCurrentPair);

        // determine local difference

        Pair<Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>, Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>> lFrontier = determineLocalDifference(pPreviousAutomaton, pCurrentAutomaton, lCurrentPair.getFirst(), lCurrentPair.getSecond());

        if (lFrontier.getFirst().isEmpty() && lFrontier.getSecond().isEmpty()) {
          // update worklist
          for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lEdge : pPreviousAutomaton.getOutgoingEdges(lCurrentPair.getFirst())) {
            // was mit !lFrontier.getFirst().isEmpty() machen?
            lWorklist.add(Pair.of(lEdge.getTarget(), lEdge.getTarget()));
          }
        }
        else {
          lF1.addAll(lFrontier.getFirst());
          lF2.addAll(lFrontier.getSecond());
        }
      }
    }

    return Pair.of(lF1, lF2);
  }

}
