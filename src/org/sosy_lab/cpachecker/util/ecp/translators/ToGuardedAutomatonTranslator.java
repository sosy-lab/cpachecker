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
package org.sosy_lab.cpachecker.util.ecp.translators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.util.automaton.NondeterministicFiniteAutomaton;
import org.sosy_lab.cpachecker.util.ecp.ECPAtom;
import org.sosy_lab.cpachecker.util.ecp.ECPConcatenation;
import org.sosy_lab.cpachecker.util.ecp.ECPEdgeSet;
import org.sosy_lab.cpachecker.util.ecp.ECPGuard;
import org.sosy_lab.cpachecker.util.ecp.ECPNodeSet;
import org.sosy_lab.cpachecker.util.ecp.ECPPredicate;
import org.sosy_lab.cpachecker.util.ecp.ECPRepetition;
import org.sosy_lab.cpachecker.util.ecp.ECPUnion;
import org.sosy_lab.cpachecker.util.ecp.ECPVisitor;
import org.sosy_lab.cpachecker.util.ecp.ElementaryCoveragePattern;

public class ToGuardedAutomatonTranslator {

  public static NondeterministicFiniteAutomaton<GuardedEdgeLabel> toAutomaton(ElementaryCoveragePattern pPattern, GuardedEdgeLabel pAlphaLabel, GuardedEdgeLabel pInverseAlphaLabel, GuardedEdgeLabel pOmegaLabel) {
    NondeterministicFiniteAutomaton<GuardedLabel> lAutomaton1 = translate(pPattern);

    NondeterministicFiniteAutomaton<GuardedLabel> lAutomaton2 = removeLambdaEdges(lAutomaton1, pAlphaLabel, pOmegaLabel);

    NondeterministicFiniteAutomaton<GuardedEdgeLabel> lAutomaton3 = removeNodeSetGuards(lAutomaton2);

    lAutomaton3.createEdge(lAutomaton3.getInitialState(), lAutomaton3.getInitialState(), pInverseAlphaLabel);

    // TODO do we need that?
    // not really --- analysis stops as soon as an omega edge is passed
    /*for (Automaton.State lFinalState : lAutomaton3.getFinalStates()) {
      lAutomaton3.createEdge(lFinalState, lFinalState, AllCFAEdgesGuardedEdgeLabel.getInstance());
    }*/

    return lAutomaton3;
  }

  public static NondeterministicFiniteAutomaton<GuardedLabel> translate(ElementaryCoveragePattern pPattern) {
    Visitor lVisitor = new Visitor();

    pPattern.accept(lVisitor);

    lVisitor.getAutomaton().addToFinalStates(lVisitor.getFinalState());

    return lVisitor.getAutomaton();
  }

  public static NondeterministicFiniteAutomaton<GuardedLabel> removeLambdaEdges(NondeterministicFiniteAutomaton<GuardedLabel> pAutomaton, GuardedEdgeLabel pAlphaLabel, GuardedEdgeLabel pOmegaLabel) {
    /** first we augment the given automaton with the alpha and omega edge */
    // TODO move into separate (private) method
    NondeterministicFiniteAutomaton.State lNewInitialState = pAutomaton.createState();
    NondeterministicFiniteAutomaton<GuardedLabel>.Edge lInitialEdge = pAutomaton.createEdge(lNewInitialState, pAutomaton.getInitialState(), pAlphaLabel);
    pAutomaton.setInitialState(lNewInitialState);

    NondeterministicFiniteAutomaton.State lNewFinalState = pAutomaton.createState();

    for (NondeterministicFiniteAutomaton.State lFinalState : pAutomaton.getFinalStates()) {
      pAutomaton.createEdge(lFinalState, lNewFinalState, pOmegaLabel);
    }

    pAutomaton.setFinalStates(Collections.singleton(lNewFinalState));

    /** now we remove guarded lambda edges */

    NondeterministicFiniteAutomaton<GuardedLabel> lAutomaton = new NondeterministicFiniteAutomaton<GuardedLabel>();
    Map<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State> lStateMap = new HashMap<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State>();
    lStateMap.put(lNewInitialState, lAutomaton.getInitialState());

    List<NondeterministicFiniteAutomaton<GuardedLabel>.Edge> lWorklist = new LinkedList<NondeterministicFiniteAutomaton<GuardedLabel>.Edge>();
    lWorklist.add(lInitialEdge);

    Set<NondeterministicFiniteAutomaton<GuardedLabel>.Edge> lReachedEdges = new HashSet<NondeterministicFiniteAutomaton<GuardedLabel>.Edge>();

    while (!lWorklist.isEmpty()) {
      NondeterministicFiniteAutomaton<GuardedLabel>.Edge lCurrentEdge = lWorklist.remove(0);

      if (lReachedEdges.contains(lCurrentEdge)) {
        continue;
      }

      lReachedEdges.add(lCurrentEdge);

      GuardedState lInitialGuardedState = new GuardedState(lCurrentEdge.getTarget(), lCurrentEdge.getLabel().getGuards());

      /** determine the lambda successors */
      // TODO refactor into distinguished method
      List<GuardedState> lStatesWorklist = new LinkedList<GuardedState>();
      lStatesWorklist.add(lInitialGuardedState);

      Set<GuardedState> lReachedStates = new HashSet<GuardedState>();

      while (!lStatesWorklist.isEmpty()) {
        GuardedState lCurrentState = lStatesWorklist.remove(0);

        boolean lIsCovered = false;

        for (GuardedState lGuardedState : lReachedStates) {
          if (lGuardedState.covers(lCurrentState)) {
            lIsCovered = true;

            break;
          }
        }

        if (lIsCovered) {
          continue;
        }

        lReachedStates.add(lCurrentState);

        for (NondeterministicFiniteAutomaton<GuardedLabel>.Edge lOutgoingEdge : pAutomaton.getOutgoingEdges(lCurrentState.getState())) {
          if (lOutgoingEdge.getLabel() instanceof GuardedLambdaLabel) {
            GuardedState lNewState = new GuardedState(lOutgoingEdge.getTarget(), lCurrentState, lOutgoingEdge.getLabel().getGuards());
            lStatesWorklist.add(lNewState);
          }
        }
      }

      NondeterministicFiniteAutomaton.State lOldSource = lCurrentEdge.getSource();

      if (!lStateMap.containsKey(lOldSource)) {
        lStateMap.put(lOldSource, lAutomaton.createState());
      }

      NondeterministicFiniteAutomaton.State lSource = lStateMap.get(lOldSource);

      GuardedLabel lCurrentLabel = lCurrentEdge.getLabel();

      GuardedEdgeLabel lEdgeLabel = (GuardedEdgeLabel)lCurrentLabel;

      ECPEdgeSet lCurrentEdgeSet = lEdgeLabel.getEdgeSet();

      for (GuardedState lReachedState : lReachedStates) {
        boolean lHasNonLambdaEdge = false;

        // TODO create variable for lReachedState.getState()

        for (NondeterministicFiniteAutomaton<GuardedLabel>.Edge lOutgoingEdge : pAutomaton.getOutgoingEdges(lReachedState.getState())) {
          if (!(lOutgoingEdge.getLabel() instanceof GuardedLambdaLabel)) {
            lHasNonLambdaEdge = true;

            lWorklist.add(lOutgoingEdge);
          }
        }

        // final state has no outgoing edges
        if (pAutomaton.getOutgoingEdges(lReachedState.getState()).isEmpty()) {
          lHasNonLambdaEdge = true;
        }

        if (lHasNonLambdaEdge) {
          NondeterministicFiniteAutomaton.State lOldTarget = lReachedState.getState();

          if (!lStateMap.containsKey(lOldTarget)) {
            lStateMap.put(lOldTarget, lAutomaton.createState());
          }

          NondeterministicFiniteAutomaton.State lTarget = lStateMap.get(lOldTarget);

          lAutomaton.createEdge(lSource, lTarget, new GuardedEdgeLabel(lCurrentEdgeSet, lReachedState.getGuards()));
        }
      }
    }

    for (NondeterministicFiniteAutomaton.State lFinalState : pAutomaton.getFinalStates()) {
      lAutomaton.addToFinalStates(lStateMap.get(lFinalState));
    }

    return lAutomaton;
  }

  /**
   * @param pAutomaton Automaton that contains no lambda edges.
   * @return Automaton that is only labeled with GuardedEdgeLabel objects.
   */
  public static NondeterministicFiniteAutomaton<GuardedEdgeLabel> removeNodeSetGuards(NondeterministicFiniteAutomaton<GuardedLabel> pAutomaton) {
    NondeterministicFiniteAutomaton<GuardedEdgeLabel> lAutomaton = new NondeterministicFiniteAutomaton<GuardedEdgeLabel>();

    Map<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State> lStateMap = new HashMap<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State>();
    lStateMap.put(pAutomaton.getInitialState(), lAutomaton.getInitialState());

    List<NondeterministicFiniteAutomaton<GuardedLabel>.Edge> lWorklist = new LinkedList<NondeterministicFiniteAutomaton<GuardedLabel>.Edge>();
    lWorklist.addAll(pAutomaton.getOutgoingEdges(pAutomaton.getInitialState()));

    Set<NondeterministicFiniteAutomaton<GuardedLabel>.Edge> lReachedEdges = new HashSet<NondeterministicFiniteAutomaton<GuardedLabel>.Edge>();

    while (!lWorklist.isEmpty()) {
      NondeterministicFiniteAutomaton<GuardedLabel>.Edge lCurrentEdge = lWorklist.remove(0);

      if (lReachedEdges.contains(lCurrentEdge)) {
        continue;
      }

      lReachedEdges.add(lCurrentEdge);

      GuardedLabel lLabel = lCurrentEdge.getLabel();

      if (lLabel.hasGuards()) {
        ECPNodeSet lNodeSet = null;

        Set<ECPGuard> lRemainingGuards = new HashSet<ECPGuard>();

        for (ECPGuard lGuard : lLabel.getGuards()) {
          if (lGuard instanceof ECPNodeSet) {
            if (lNodeSet == null) {
              lNodeSet = (ECPNodeSet)lGuard;
            }
            else {
              lNodeSet = lNodeSet.intersect((ECPNodeSet)lGuard);
            }
          }
          else {
            lRemainingGuards.add(lGuard);
          }
        }

        if (lNodeSet != null) {
          // TODO move this condition upwards
          if (!lNodeSet.isEmpty()) {
            assert(lLabel instanceof GuardedEdgeLabel);

            GuardedEdgeLabel lEdgeLabel = (GuardedEdgeLabel)lLabel;

            ECPEdgeSet lCurrentEdgeSet = lEdgeLabel.getEdgeSet();

            Set<CFAEdge> lRemainingCFAEdges = new HashSet<CFAEdge>();

            for (CFAEdge lCFAEdge : lCurrentEdgeSet) {
              if (lNodeSet.contains(lCFAEdge.getSuccessor())) {
                lRemainingCFAEdges.add(lCFAEdge);
              }
            }

            if (!lRemainingCFAEdges.isEmpty()) {
              ECPEdgeSet lNewEdgeSet = new ECPEdgeSet(lRemainingCFAEdges);

              GuardedEdgeLabel lNewGuard = new GuardedEdgeLabel(lNewEdgeSet, lRemainingGuards);

              // add edge

              NondeterministicFiniteAutomaton.State lCurrentSource = lCurrentEdge.getSource();
              NondeterministicFiniteAutomaton.State lCurrentTarget = lCurrentEdge.getTarget();

              if (!lStateMap.containsKey(lCurrentSource)) {
                lStateMap.put(lCurrentSource, lAutomaton.createState());
              }

              if (!lStateMap.containsKey(lCurrentTarget)) {
                lStateMap.put(lCurrentTarget, lAutomaton.createState());
              }

              NondeterministicFiniteAutomaton.State lSourceState = lStateMap.get(lCurrentSource);
              NondeterministicFiniteAutomaton.State lTargetState = lStateMap.get(lCurrentTarget);

              lAutomaton.createEdge(lSourceState, lTargetState, lNewGuard);
            }
          }
        }
        else {
          assert(lLabel instanceof GuardedEdgeLabel);

          GuardedEdgeLabel lEdgeLabel = (GuardedEdgeLabel)lLabel;

          if (!lEdgeLabel.getEdgeSet().isEmpty()) {
            // add edge
            NondeterministicFiniteAutomaton.State lCurrentSource = lCurrentEdge.getSource();
            NondeterministicFiniteAutomaton.State lCurrentTarget = lCurrentEdge.getTarget();

            if (!lStateMap.containsKey(lCurrentSource)) {
              lStateMap.put(lCurrentSource, lAutomaton.createState());
            }

            if (!lStateMap.containsKey(lCurrentTarget)) {
              lStateMap.put(lCurrentTarget, lAutomaton.createState());
            }

            NondeterministicFiniteAutomaton.State lSourceState = lStateMap.get(lCurrentSource);
            NondeterministicFiniteAutomaton.State lTargetState = lStateMap.get(lCurrentTarget);

            lAutomaton.createEdge(lSourceState, lTargetState, lEdgeLabel);
          }
        }
      }
      else {
        assert(lLabel instanceof GuardedEdgeLabel);

        GuardedEdgeLabel lEdgeLabel = (GuardedEdgeLabel)lLabel;

        if (!lEdgeLabel.getEdgeSet().isEmpty()) {
          // add edge
          NondeterministicFiniteAutomaton.State lCurrentSource = lCurrentEdge.getSource();
          NondeterministicFiniteAutomaton.State lCurrentTarget = lCurrentEdge.getTarget();

          if (!lStateMap.containsKey(lCurrentSource)) {
            lStateMap.put(lCurrentSource, lAutomaton.createState());
          }

          if (!lStateMap.containsKey(lCurrentTarget)) {
            lStateMap.put(lCurrentTarget, lAutomaton.createState());
          }

          NondeterministicFiniteAutomaton.State lSourceState = lStateMap.get(lCurrentSource);
          NondeterministicFiniteAutomaton.State lTargetState = lStateMap.get(lCurrentTarget);

          lAutomaton.createEdge(lSourceState, lTargetState, lEdgeLabel);
        }
      }

      lWorklist.addAll(pAutomaton.getOutgoingEdges(lCurrentEdge.getTarget()));
    }

    for (NondeterministicFiniteAutomaton.State lFinalState : pAutomaton.getFinalStates()) {
      if (lStateMap.containsKey(lFinalState)) {
        lAutomaton.addToFinalStates(lStateMap.get(lFinalState));
      }
    }

    return lAutomaton;
  }

  private static class Visitor implements ECPVisitor<Void> {

    private static final Map<ECPAtom, GuardedLabel> sLabelCache = new HashMap<ECPAtom, GuardedLabel>();

    private NondeterministicFiniteAutomaton<GuardedLabel> mAutomaton;

    private NondeterministicFiniteAutomaton.State mInitialState;
    private NondeterministicFiniteAutomaton.State mFinalState;

    public Visitor(NondeterministicFiniteAutomaton<GuardedLabel> pAutomaton) {
      mAutomaton = pAutomaton;
      setInitialState(mAutomaton.getInitialState());
      setFinalState(mAutomaton.createState());
    }

    public Visitor() {
      this(new NondeterministicFiniteAutomaton<GuardedLabel>());
    }

    public NondeterministicFiniteAutomaton<GuardedLabel> getAutomaton() {
      return mAutomaton;
    }

    public NondeterministicFiniteAutomaton.State getInitialState() {
      return mInitialState;
    }

    public NondeterministicFiniteAutomaton.State getFinalState() {
      return mFinalState;
    }

    public void setInitialState(NondeterministicFiniteAutomaton.State pInitialState) {
      mInitialState = pInitialState;
    }

    public void setFinalState(NondeterministicFiniteAutomaton.State pFinalState) {
      mFinalState = pFinalState;
    }

    @Override
    public Void visit(ECPEdgeSet pEdgeSet) {
      GuardedLabel lLabel = sLabelCache.get(pEdgeSet);

      if (lLabel == null) {
        lLabel = new GuardedEdgeLabel(pEdgeSet);
        sLabelCache.put(pEdgeSet, lLabel);
      }

      mAutomaton.createEdge(getInitialState(), getFinalState(), lLabel);

      return null;
    }

    @Override
    public Void visit(ECPNodeSet pNodeSet) {
      GuardedLabel lLabel = sLabelCache.get(pNodeSet);

      if (lLabel == null) {
        lLabel = new GuardedLambdaLabel(pNodeSet);
        sLabelCache.put(pNodeSet, lLabel);
      }

      mAutomaton.createEdge(getInitialState(), getFinalState(), lLabel);

      return null;
    }

    @Override
    public Void visit(ECPPredicate pPredicate) {
      GuardedLabel lLabel = sLabelCache.get(pPredicate);

      if (lLabel == null) {
        lLabel = new GuardedLambdaLabel(pPredicate);
        sLabelCache.put(pPredicate, lLabel);
      }

      mAutomaton.createEdge(getInitialState(), getFinalState(), lLabel);

      return null;
    }

    @Override
    public Void visit(ECPConcatenation pConcatenation) {
      if (pConcatenation.isEmpty()) {
        mAutomaton.createEdge(getInitialState(), getFinalState(), GuardedLambdaLabel.UNGUARDED_LAMBDA_LABEL);
      }
      else {
        NondeterministicFiniteAutomaton.State lTmpInitialState = getInitialState();
        NondeterministicFiniteAutomaton.State lTmpFinalState = getFinalState();

        for (int i = 0; i < pConcatenation.size(); i++) {
          ElementaryCoveragePattern lSubpattern = pConcatenation.get(i);

          if (i > 0) {
            // use final state from before
            setInitialState(getFinalState());
          }

          if (i == pConcatenation.size() - 1) {
            // use lTmpFinalState
            setFinalState(lTmpFinalState);
          }
          else {
            // create new final state
            setFinalState(mAutomaton.createState());
          }

          lSubpattern.accept(this);
        }

        setInitialState(lTmpInitialState);
      }

      return null;
    }

    @Override
    public Void visit(ECPUnion pUnion) {
      if (pUnion.isEmpty()) {
        mAutomaton.createEdge(getInitialState(), getFinalState(), GuardedLambdaLabel.UNGUARDED_LAMBDA_LABEL);
      }
      else if (pUnion.size() == 1) {
        pUnion.get(0).accept(this);
      }
      else {
        NondeterministicFiniteAutomaton.State lTmpInitialState = getInitialState();

        for (ElementaryCoveragePattern lSubpattern : pUnion) {
          setInitialState(mAutomaton.createState());

          mAutomaton.createEdge(lTmpInitialState, getInitialState(), GuardedLambdaLabel.UNGUARDED_LAMBDA_LABEL);

          lSubpattern.accept(this);
        }

        setInitialState(lTmpInitialState);
      }

      return null;
    }

    @Override
    public Void visit(ECPRepetition pRepetition) {
      NondeterministicFiniteAutomaton.State lTmpInitialState = getInitialState();
      NondeterministicFiniteAutomaton.State lTmpFinalState = getFinalState();

      mAutomaton.createEdge(lTmpInitialState, lTmpFinalState, GuardedLambdaLabel.UNGUARDED_LAMBDA_LABEL);

      setInitialState(mAutomaton.createState());
      setFinalState(lTmpInitialState);

      mAutomaton.createEdge(lTmpInitialState, getInitialState(), GuardedLambdaLabel.UNGUARDED_LAMBDA_LABEL);

      pRepetition.getSubpattern().accept(this);

      setInitialState(lTmpInitialState);
      setFinalState(lTmpFinalState);

      return null;
    }

  }

  /*
   * We only need the backwards reachability closure since in case a state is not
   * reachable via the initial state, it will not be considered during analysis
   * anyhow.
   */
  public static NondeterministicFiniteAutomaton<GuardedEdgeLabel> removeDeadEnds(NondeterministicFiniteAutomaton<GuardedEdgeLabel> pAutomaton) {
    // TODO does hash set introduce nondeterminism?
    Set<NondeterministicFiniteAutomaton.State> lClosure = new HashSet<NondeterministicFiniteAutomaton.State>();

    for (NondeterministicFiniteAutomaton.State lFinalState : pAutomaton.getFinalStates()) {
      LinkedList<NondeterministicFiniteAutomaton.State> lWorklist = new LinkedList<NondeterministicFiniteAutomaton.State>();

      lWorklist.add(lFinalState);
      lClosure.add(lFinalState);

      while (!lWorklist.isEmpty()) {
        NondeterministicFiniteAutomaton.State lCurrentState = lWorklist.removeFirst();

        for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lIncomingTransition : pAutomaton.getIncomingEdges(lCurrentState)) {
          NondeterministicFiniteAutomaton.State lSource = lIncomingTransition.getSource();

          if (!lClosure.contains(lSource)) {
            lWorklist.add(lSource);
            lClosure.add(lSource);
          }
        }
      }
    }

    NondeterministicFiniteAutomaton<GuardedEdgeLabel> lNewAutomaton = new NondeterministicFiniteAutomaton<GuardedEdgeLabel>();

    if (lClosure.contains(pAutomaton.getInitialState())) {
      Map<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State> lStateMap = new HashMap<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State>();

      lStateMap.put(pAutomaton.getInitialState(), lNewAutomaton.getInitialState());

      for (NondeterministicFiniteAutomaton.State lState : pAutomaton.getStates()) {
        if (!lState.equals(pAutomaton.getInitialState())) {
          lStateMap.put(lState, lNewAutomaton.createState());
        }
      }

      for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lTransition : pAutomaton.getEdges()) {
        if (lClosure.contains(lTransition.getTarget())) {
          lNewAutomaton.createEdge(lStateMap.get(lTransition.getSource()), lStateMap.get(lTransition.getTarget()), lTransition.getLabel());
        }
      }

      // set final states
      for (NondeterministicFiniteAutomaton.State lFinalState : pAutomaton.getFinalStates()) {
        if (lClosure.contains(lFinalState)) {
          lNewAutomaton.addToFinalStates(lStateMap.get(lFinalState));
        }
      }
    }

    return lNewAutomaton;
  }

  public static NondeterministicFiniteAutomaton<GuardedEdgeLabel> reduceEdgeSets(NondeterministicFiniteAutomaton<GuardedEdgeLabel> pAutomaton) {
    NondeterministicFiniteAutomaton<GuardedEdgeLabel> lNewAutomaton = new NondeterministicFiniteAutomaton<GuardedEdgeLabel>();

    Map<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State> lStateMap = new HashMap<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State>();

    lStateMap.put(pAutomaton.getInitialState(), lNewAutomaton.getInitialState());

    for (NondeterministicFiniteAutomaton.State lState : pAutomaton.getStates()) {
      if (!lState.equals(pAutomaton.getInitialState())) {
        lStateMap.put(lState, lNewAutomaton.createState());
      }
    }

    // this implementation is a very simple heuristic ... TODO generalize
    for (NondeterministicFiniteAutomaton.State lState : pAutomaton.getStates()) {

      boolean lMatch = false;

      if (!pAutomaton.isFinalState(lState) && !pAutomaton.getInitialState().equals(lState)) {
        if (pAutomaton.getOutgoingEdges(lState).size() == 1) {
          NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lOutgoingEdge = pAutomaton.getOutgoingEdges(lState).iterator().next();

          GuardedEdgeLabel lLabel = lOutgoingEdge.getLabel();

          ECPEdgeSet lEdgeSet = lLabel.getEdgeSet();

          if (lEdgeSet.size() == 1) {
            CFAEdge lOutgoingCFAEdge = lEdgeSet.iterator().next();

            if (pAutomaton.getIncomingEdges(lState).size() == 1) {
              NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lIncomingEdge = pAutomaton.getIncomingEdges(lState).iterator().next();

              ECPEdgeSet lIncomingEdgeSet = lIncomingEdge.getLabel().getEdgeSet();

              CFANode lPredecessor = lOutgoingCFAEdge.getPredecessor();

              ArrayList<CFAEdge> lIncomingCFAEdges = new ArrayList<CFAEdge>(lPredecessor.getNumEnteringEdges());

              for (int lIndex = 0; lIndex < lPredecessor.getNumEnteringEdges(); lIndex++) {
                CFAEdge lIncomingCFAEdge = lPredecessor.getEnteringEdge(lIndex);

                if (lIncomingEdgeSet.contains(lIncomingCFAEdge)) {
                  lIncomingCFAEdges.add(lIncomingCFAEdge);
                }
              }

              ECPEdgeSet lNewEdgeSet = new ECPEdgeSet(lIncomingCFAEdges);

              GuardedEdgeLabel lNewLabel = new GuardedEdgeLabel(lNewEdgeSet, lIncomingEdge.getLabel().getGuards());

              lNewAutomaton.createEdge(lStateMap.get(lIncomingEdge.getSource()), lStateMap.get(lIncomingEdge.getTarget()), lNewLabel);

              lMatch = true;
            }
          }
        }
      }

      if (!lMatch) {
        // add incoming automaton edges
        for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lIncomingEdge : pAutomaton.getIncomingEdges(lState)) {
          lNewAutomaton.createEdge(lStateMap.get(lIncomingEdge.getSource()), lStateMap.get(lIncomingEdge.getTarget()), lIncomingEdge.getLabel());
        }
      }
    }

    // set final states
    for (NondeterministicFiniteAutomaton.State lFinalState : pAutomaton.getFinalStates()) {
      lNewAutomaton.addToFinalStates(lStateMap.get(lFinalState));
    }

    return lNewAutomaton;
  }

  public static NondeterministicFiniteAutomaton<GuardedEdgeLabel> removeInfeasibleTransitions(NondeterministicFiniteAutomaton<GuardedEdgeLabel> pAutomaton) {
    NondeterministicFiniteAutomaton<GuardedEdgeLabel> lNewAutomaton = new NondeterministicFiniteAutomaton<GuardedEdgeLabel>();

    Map<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State> lStateMap = new HashMap<NondeterministicFiniteAutomaton.State, NondeterministicFiniteAutomaton.State>();

    lStateMap.put(pAutomaton.getInitialState(), lNewAutomaton.getInitialState());

    for (NondeterministicFiniteAutomaton.State lState : pAutomaton.getStates()) {
      if (!lState.equals(pAutomaton.getInitialState())) {
        lStateMap.put(lState, lNewAutomaton.createState());
      }
    }

    for (NondeterministicFiniteAutomaton.State lState : pAutomaton.getStates()) {
      for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lIncomingEdge : pAutomaton.getIncomingEdges(lState)) {
        boolean lMatch = false;

        GuardedEdgeLabel lIncomingLabel = lIncomingEdge.getLabel();

        if (!(lIncomingLabel instanceof InverseGuardedEdgeLabel)
            && !pAutomaton.getFinalStates().contains(lState)) {
          for (NondeterministicFiniteAutomaton<GuardedEdgeLabel>.Edge lOutgoingEdge : pAutomaton.getOutgoingEdges(lState)) {
            GuardedEdgeLabel lOutgoingLabel = lOutgoingEdge.getLabel();

            for (CFAEdge lIncomingCFAEdge : lIncomingLabel.getEdgeSet()) {
              for (CFAEdge lOutgoingCFAEdge : lOutgoingLabel.getEdgeSet()) {
                if (lIncomingCFAEdge.getSuccessor().equals(lOutgoingCFAEdge.getPredecessor())) {
                  lMatch = true;
                  break;
                }
              }

              if (lMatch) {
                break;
              }
            }

            if (lMatch) {
              break;
            }
          }
        }
        else {
          lMatch = true;
        }

        if (lMatch) {
          lNewAutomaton.createEdge(lStateMap.get(lIncomingEdge.getSource()), lStateMap.get(lIncomingEdge.getTarget()), lIncomingLabel);
        }
      }
    }

    // set final states
    for (NondeterministicFiniteAutomaton.State lFinalState : pAutomaton.getFinalStates()) {
      lNewAutomaton.addToFinalStates(lStateMap.get(lFinalState));
    }

    return lNewAutomaton;
  }

}
