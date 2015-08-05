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
package org.sosy_lab.cpachecker.core.algorithm.tiger.util;

import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.ECPEdgeSet;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.translators.GuardedEdgeLabel;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.StateSpacePartition;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.GuardedEdgeAutomatonStateElement;
import org.sosy_lab.cpachecker.cpa.guardededgeautomaton.productautomaton.ProductAutomatonElement;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;


public class ARTReuse {

  public static boolean isDegeneratedAutomaton(NondeterministicFiniteAutomaton<GuardedEdgeLabel> automaton) {
    // if the previous automaton got so much optimized that there is nothing left, then we have to reinitialize the reached set (it should be only one element anyways)
    return (automaton != null && automaton.getFinalStates().isEmpty() && automaton.getOutgoingEdges(automaton.getInitialState()).isEmpty());
  }

  public static void modifyReachedSet(ReachedSet pReachedSet, FunctionEntryNode pEntryNode, ARGCPA pARTCPA, int pProductAutomatonIndex, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pPreviousAutomaton, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pCurrentAutomaton) {

    assert !isDegeneratedAutomaton(pCurrentAutomaton) && !isDegeneratedAutomaton(pPreviousAutomaton);

    if (pReachedSet.isEmpty()/* || isDegeneratedAutomaton(pCurrentAutomaton)*/) {
      AbstractState lInitialElement = pARTCPA.getInitialState(pEntryNode, StateSpacePartition.getDefaultPartition());
      Precision lInitialPrecision = pARTCPA.getInitialPrecision(pEntryNode, StateSpacePartition.getDefaultPartition());

      pReachedSet.add(lInitialElement, lInitialPrecision);
    }
    /*else if (isDegeneratedAutomaton(pPreviousAutomaton)) {
      if (pReachedSet.size() != 1) {
        throw new RuntimeException();
      }

      if (pReachedSet.getWaitlistSize() != 0) {
        throw new RuntimeException();
      }

      pReachedSet.reAddToWaitlist(pReachedSet.getFirstState());
    }*/
    else {
      if (pPreviousAutomaton == null) {
        throw new RuntimeException();
      }

      modifyART(pReachedSet, pARTCPA, pProductAutomatonIndex, pPreviousAutomaton, pCurrentAutomaton);
    }
  }

  /**
   * Like {@link modifyReachedSet}, but also returns the set elements that are directly removed.
   * @param pReachedSet
   * @param pEntryNode
   * @param pARTCPA
   * @param pProductAutomatonIndex
   * @param pPreviousAutomaton
   * @param pCurrentAutomaton
   * @return
   */
  public static Set<AbstractState> modifyReachedSet2(ReachedSet pReachedSet, FunctionEntryNode pEntryNode, ARGCPA pARTCPA, int pProductAutomatonIndex, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pPreviousAutomaton, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pCurrentAutomaton) {
    Set<AbstractState> set = new HashSet<>();

    if (pReachedSet.isEmpty()) {
      AbstractState lInitialElement = pARTCPA.getInitialState(pEntryNode, StateSpacePartition.getDefaultPartition());
      Precision lInitialPrecision = pARTCPA.getInitialPrecision(pEntryNode, StateSpacePartition.getDefaultPartition());

      pReachedSet.add(lInitialElement, lInitialPrecision);
    }
    else {
      if (pPreviousAutomaton == null) {
        throw new RuntimeException();
      }

      Set<AbstractState> s = modifyART2(pReachedSet, pARTCPA, pProductAutomatonIndex, pPreviousAutomaton, pCurrentAutomaton);
      set.addAll(s);
    }


    return set;
  }

  private static void modifyART(ReachedSet pReachedSet, ARGReachedSet pARTReachedSet, int pProductAutomatonIndex, Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge> pFrontierEdges) {
    //Set<Pair<ARTElement, ARTElement>> lPathEdges = Collections.emptySet();
    //ARTStatistics.dumpARTToDotFile(new File("/home/andreas/art01.dot"), lARTCPA, pReachedSet, lPathEdges);

    for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lEdge : pFrontierEdges) {
      GuardedEdgeLabel lLabel = lEdge.getLabel();

      ECPEdgeSet lEdgeSet = lLabel.getEdgeSet();

      for (CFAEdge lCFAEdge : lEdgeSet) {
        CFANode lCFANode = lCFAEdge.getPredecessor();

        Collection<AbstractState> lAbstractElements = pReachedSet.getReached(lCFANode);

        LinkedList<AbstractState> lAbstractElements2 = new LinkedList<>();
        lAbstractElements2.addAll(lAbstractElements);

        for (AbstractState lAbstractElement : lAbstractElements2) {
          if (!pReachedSet.contains(lAbstractElement)) {
            // lAbstractElement was removed in an earlier step
            continue;
          }

          ARGState lARTElement = (ARGState)lAbstractElement;

          if (AbstractStates.extractLocation(lARTElement) != lCFANode) {
            continue;
          }

          // what's the semantics of getWrappedElement*s*()?
          CompositeState lCompositeElement = (CompositeState)lARTElement.getWrappedState();

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
              removeElement(lARTElement, pARTReachedSet);
            }
          }
        }
      }
    }
  }


  /**
   * Like {@link modifyART}, but returns the roots of the removed subgraphs.
   * @param pReachedSet
   * @param pARTReachedSet
   * @param pProductAutomatonIndex
   * @param pFrontierEdges
   * @return
   */
  private static Set<AbstractState> modifyART2(ReachedSet pReachedSet, ARGReachedSet pARTReachedSet, int pProductAutomatonIndex,
      Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge> pFrontierEdges) {
    //Set<Pair<ARTElement, ARTElement>> lPathEdges = Collections.emptySet();
    //ARTStatistics.dumpARTToDotFile(new File("/home/andreas/art01.dot"), lARTCPA, pReachedSet, lPathEdges);

    Set<AbstractState> set = new HashSet<>();

    for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lEdge : pFrontierEdges) {
      GuardedEdgeLabel lLabel = lEdge.getLabel();

      ECPEdgeSet lEdgeSet = lLabel.getEdgeSet();

      for (CFAEdge lCFAEdge : lEdgeSet) {
        CFANode lCFANode = lCFAEdge.getPredecessor();

        Collection<AbstractState> lAbstractElements = pReachedSet.getReached(lCFANode);

        LinkedList<AbstractState> lAbstractElements2 = new LinkedList<>();
        lAbstractElements2.addAll(lAbstractElements);

        for (AbstractState lAbstractElement : lAbstractElements2) {
          if (!pReachedSet.contains(lAbstractElement)) {
            // lAbstractElement was removed in an earlier step
            continue;
          }

          ARGState lARTElement = (ARGState)lAbstractElement;

          if (AbstractStates.extractLocation(lARTElement) != lCFANode) {
            continue;
          }

          // what's the semantics of getWrappedElement*s*()?
          CompositeState lCompositeElement = (CompositeState)lARTElement.getWrappedState();

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
              set.addAll(lARTElement.getChildren());

              removeElement(lARTElement, pARTReachedSet);
            }
          }
        }
      }
    }

    return set;
  }


  /**
   * Removes all children of the element from the reached set and adds the element it to the worklist.
   * The element's precision is update to the sum of the children precisions.
   * @param lARTElement
   * @param pARTReachedSet
   * @throws InvalidConfigurationException
   */
  /*private static void removeElementLazy(ARGState lARTElement, ReachedSet pReachedSet, ARGReachedSet pARTReachedSet) {

    // TODO only predicate and explicit precisions are handled

    // merge all children precision
    Precision prec = pReachedSet.getPrecision(lARTElement);
    PredicatePrecision predPrec = Precisions.extractPrecisionByType(prec, PredicatePrecision.class);
    ExplicitPrecision expPrec = Precisions.extractPrecisionByType(prec, ExplicitPrecision.class);

    if (predPrec == null && expPrec == null) {
      assert false;
    }

    Collection<ARGState> children = lARTElement.getChildren();

    for (ARGState child : children){
      if (!pReachedSet.contains(child)) {
        continue;
      }

      Precision cPrec = pReachedSet.getPrecision(child);

      if (predPrec != null){
        PredicatePrecision cPredPrec = Precisions.extractPrecisionByType(cPrec, PredicatePrecision.class);
        predPrec = predPrec.mergeWith(cPredPrec);
      }

      if (expPrec != null){
        ExplicitPrecision cExpPrec = Precisions.extractPrecisionByType(cPrec, ExplicitPrecision.class);
        expPrec = expPrec.mergeWith(cExpPrec);
      }

    }

    // update parent precision
    if (predPrec != null){
      Precisions.replaceByType(prec, predPrec, PredicatePrecision.class);
    }

    if (expPrec != null){
      Precisions.replaceByType(prec, expPrec, ExplicitPrecision.class);
    }

    pReachedSet.updatePrecision(lARTElement, prec);

    // remove children
    removeElement(lARTElement, pARTReachedSet);

  }*/

  /**
   * Removes all children of the element from the reached set and adds the element it to the worklist.
   * @param lARTElement
   * @param pARTReachedSet
   */
  private static void removeElement(ARGState lARTElement, ARGReachedSet pARTReachedSet) {
    while (!lARTElement.getChildren().isEmpty()) {
      ARGState lChildElement = lARTElement.getChildren().iterator().next();

      pARTReachedSet.removeSubtree(lChildElement);
    }

  }

  private static void modifyART(ReachedSet pReachedSet, ARGCPA pARTCPA, int pProductAutomatonIndex, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pPreviousAutomaton, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pCurrentAutomaton) {
    ARGReachedSet lARTReachedSet = new ARGReachedSet(pReachedSet, pARTCPA);

    Pair<Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>, Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>> lFrontier = determineFrontier(pPreviousAutomaton, pCurrentAutomaton);

    modifyART(pReachedSet, lARTReachedSet, pProductAutomatonIndex, lFrontier.getFirst());
    modifyART(pReachedSet, lARTReachedSet, pProductAutomatonIndex, lFrontier.getSecond());
  }


  /**
   * Like {@link modifyART}, but returns the roots of the removed subgraphs.
   * @param pReachedSet
   * @param pARTCPA
   * @param pProductAutomatonIndex
   * @param pPreviousAutomaton
   * @param pCurrentAutomaton
   * @return
   */
  private static Set<AbstractState> modifyART2(ReachedSet pReachedSet, ARGCPA pARTCPA, int pProductAutomatonIndex, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pPreviousAutomaton, NondeterministicFiniteAutomaton<GuardedEdgeLabel> pCurrentAutomaton) {
    Set<AbstractState> set = new HashSet<>();

    ARGReachedSet lARTReachedSet = new ARGReachedSet(pReachedSet, pARTCPA);

    Pair<Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>, Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>> lFrontier = determineFrontier(pPreviousAutomaton, pCurrentAutomaton);

    Set<AbstractState> s1 = modifyART2(pReachedSet, lARTReachedSet, pProductAutomatonIndex, lFrontier.getFirst());
    Set<AbstractState> s2 = modifyART2(pReachedSet, lARTReachedSet, pProductAutomatonIndex, lFrontier.getSecond());
    set.addAll(s1);
    set.addAll(s2);

    return set;
  }


  private static Pair<Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>, Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>> determineLocalDifference(
      NondeterministicFiniteAutomaton<GuardedEdgeLabel> pPreviousAutomaton,
      NondeterministicFiniteAutomaton<GuardedEdgeLabel> pCurrentAutomaton,
      NondeterministicFiniteAutomaton.State pPreviousState,
      NondeterministicFiniteAutomaton.State pCurrentState) {

    Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge> lE1 = new HashSet<>();
    Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge> lE2 = new HashSet<>();

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

  private static Pair<Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>, Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>> determineFrontier(
        NondeterministicFiniteAutomaton<GuardedEdgeLabel> pPreviousAutomaton,
        NondeterministicFiniteAutomaton<GuardedEdgeLabel> pCurrentAutomaton) {

    if (pPreviousAutomaton.getInitialState() != pCurrentAutomaton.getInitialState()) {
      throw new RuntimeException();
    }

    Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge> frontOne = new HashSet<>();
    Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge> frontTwo = new HashSet<>();

    Deque<Pair<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State>> worklist = new LinkedList<>();
    worklist.add(Pair.of(
        pPreviousAutomaton.getInitialState(),
        pCurrentAutomaton.getInitialState()));

    Set<Pair<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State>> visited = new HashSet<>();

    while (!worklist.isEmpty()) {
      Pair<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State> currentPair = worklist.removeLast();

      if (!visited.contains(currentPair)) {
        visited.add(currentPair);

        // determine local difference

        Pair<Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>, Set<NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge>> lFrontier = determineLocalDifference(pPreviousAutomaton, pCurrentAutomaton, currentPair.getFirst(), currentPair.getSecond());

        if (lFrontier.getFirst().isEmpty() && lFrontier.getSecond().isEmpty()) {
          // update worklist
          for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lEdge : pPreviousAutomaton.getOutgoingEdges(currentPair.getFirst())) {
            // was mit !lFrontier.getFirst().isEmpty() machen?
            worklist.add(Pair.of(lEdge.getTarget(), lEdge.getTarget()));
          }
        }
        else {
          frontOne.addAll(lFrontier.getFirst());
          frontTwo.addAll(lFrontier.getSecond());
        }
      }
    }

    return Pair.of(frontOne, frontTwo);
  }

}
