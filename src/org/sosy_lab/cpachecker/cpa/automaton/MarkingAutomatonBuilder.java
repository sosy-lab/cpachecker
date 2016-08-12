/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.automaton;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.AStatement;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.MatchSuccessorNotWeaved;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;

import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

public class MarkingAutomatonBuilder {

  private abstract static class BackLinkedState {
    @Nullable private final BackLinkedState predecessor;
    private final AutomatonInternalState state;

    private BackLinkedState(@Nullable BackLinkedState pPred, AutomatonInternalState pState) {
      state = Preconditions.checkNotNull(pState);
      predecessor = pPred;
    }

    public Optional<BackLinkedState> getPredecessor() {
      return Optional.fromNullable(predecessor);
    }

    public AutomatonInternalState getState() {
      return state;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((predecessor == null) ? 0 : predecessor.hashCode());
      result = prime * result + state.hashCode();
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) { return true; }
      if (obj == null) { return false; }
      if (!(obj instanceof BackLinkedState)) { return false; }
      BackLinkedState other = (BackLinkedState) obj;
      if (predecessor == null) {
        if (other.predecessor != null) { return false; }
      } else if (!predecessor.equals(other.predecessor)) { return false; }
      if (!state.equals(other.state)) { return false; }
      return true;
    }
  }

  private static class UnmarkedState extends BackLinkedState {

    public static UnmarkedState of(@Nullable BackLinkedState pPred, AutomatonInternalState pState) {
      return new UnmarkedState(pPred, pState);
    }

    private UnmarkedState(@Nullable BackLinkedState pPred, AutomatonInternalState pState) {
      super(pPred, pState);
    }
  }

  private static class MarkedState extends BackLinkedState {

    @Nullable private final Integer markerId;

    public static MarkedState of(@Nullable BackLinkedState pPred, AutomatonInternalState pState, @Nullable Integer pMarkerId) {
      return new MarkedState(pPred, pState, pMarkerId);
    }

    private MarkedState(@Nullable BackLinkedState pPred, AutomatonInternalState pState, @Nullable Integer pMarkerId) {
      super(pPred, pState);
      markerId = pMarkerId;
    }
  }

  private static class MarkerCode {

    private final int markerId;
    private final String namePrefix;

    private final CIdExpression markerVariable;
    private final CVariableDeclaration markerDeclaration;

    private final AAstNode markerIncrementStatement;
    private final AAstNode markerDeclarationStatement;

    public MarkerCode(String pNamePrefix, int pMarkerId) {
      markerId = pMarkerId;
      namePrefix = pNamePrefix;

      final String varName = String.format("__%s_MARKER_%d", namePrefix, markerId);

      markerDeclaration = new CVariableDeclaration(
          FileLocation.DUMMY, true, CStorageClass.AUTO,
          CNumericTypes.INT, varName, varName, varName,
          new CInitializerExpression(FileLocation.DUMMY, CIntegerLiteralExpression.ZERO) );

      markerVariable = new CIdExpression(FileLocation.DUMMY, CNumericTypes.INT, varName, markerDeclaration);

      markerDeclarationStatement = markerDeclaration;
      markerIncrementStatement = new CExpressionAssignmentStatement(FileLocation.DUMMY,
          markerVariable, CIntegerLiteralExpression.ONE);
    }

  }

  private static int skipOverShadowcodeToErrorSeq = 0;

  private static synchronized AutomatonInternalState skipOverShadowcodeToError(
      List<AStatement> pAssumptions,
      Set<SafetyProperty> pViolatedOnTarget) {
    return new AutomatonInternalState(
        "SKIP_TO_ERROR_" + (skipOverShadowcodeToErrorSeq++),
        Collections.singletonList(new AutomatonTransition(
            MatchSuccessorNotWeaved.INSTANCE,
            pAssumptions,
            true,
            ImmutableList.<AAstNode>of(),
            ImmutableList.<AutomatonAction>of(),
            AutomatonInternalState.ERROR,
            pViolatedOnTarget)),
        false, false);
  }

  public static Automaton build(Automaton pInput) {
    Preconditions.checkNotNull(pInput);

    // Initialize the data structures
    Map<AutomatonBoolExpr, Integer> edgeToMarkerMap = Maps.newLinkedHashMap();
    Set<AutomatonTransition> visited = Sets.newIdentityHashSet(); // An equal (but not identical)transition might be used several times
    Deque<BackLinkedState> worklist = Lists.newLinkedList();
    Map<Integer, MarkerCode> markerCode = Maps.newLinkedHashMap();
    Multimap<AutomatonTransition, BackLinkedState> targetStates = HashMultimap.create();

    int edgeId = 0;

    // We start from the initial automaton state
    worklist.add(UnmarkedState.of(null, pInput.getInitialState()));

    // Perform the marking...
    while (worklist.size() > 0) {
      BackLinkedState q = worklist.pop();

      for (AutomatonTransition t: q.getState().getTransitions()) {
        if (!visited.add(t)) {
          // Detect and break cycles
          continue;
        }
        if (t.getFollowState().equals(q.getState())) {
          // Skip stuttering transitions
          continue;
        }

        final BackLinkedState succ;

        if (shouldMark(q, t)) {
          Integer markerId = edgeToMarkerMap.get(t.getTrigger());
          final MarkerCode mc;
          if (markerId == null) {
            markerId = edgeId++;
            edgeToMarkerMap.put(t.getTrigger(), Integer.valueOf(markerId));

            mc = new MarkerCode(pInput.getName(), markerId);
            markerCode.put(markerId, mc);
          }

          succ = MarkedState.of(q, t.getFollowState(), markerId);

        } else {
          succ = UnmarkedState.of(q, t.getFollowState());
        }

        if (succ.getState().isTarget()) {
          targetStates.put(t, succ);
        }

        worklist.add(succ);
      }
    }

    // Build the new automaton
    final List<AutomatonInternalState> resultStates = Lists.newArrayList();
    final String resultInitialStateName = "qInitMarkers";

    // -- construct new states and transitions
    for (AutomatonInternalState q: pInput.getStates()) {

      List<AutomatonTransition> qPrimeTrans = Lists.newArrayList();

      for (AutomatonTransition t: q.getTransitions()) {
        Integer markerId = edgeToMarkerMap.get(t.getTrigger());

        List<AAstNode> newShadowCode = Lists.newLinkedList();
        newShadowCode.addAll(t.getShadowCode());

        if (markerId != null) {
          final MarkerCode mc = markerCode.get(markerId);

          // -- add the marker code to the transition
          newShadowCode.add(0, mc.markerIncrementStatement);
        }

        // -- in case of a target state: add an assumption on the markers
        List<AStatement> assumptions = Lists.newArrayList();
        if (t.getFollowState().isTarget()) {
          Collection<BackLinkedState> markers = targetStates.get(t);
          for (BackLinkedState m: markers) {

            List<Integer> pathMarkers = Lists.newArrayList();
            BackLinkedState travers = m;
            while (travers != null) {
              if (travers instanceof MarkedState) {
                MarkedState mm = (MarkedState) travers;
                pathMarkers.add(mm.markerId);
              }
              travers = travers.predecessor;
            }

            for (Integer pm: pathMarkers) {
              MarkerCode mc = markerCode.get(pm);
              Preconditions.checkState(mc != null);
              assumptions.add(new CExpressionStatement(FileLocation.DUMMY,
                  new CBinaryExpression(FileLocation.DUMMY,
                      CNumericTypes.INT, CNumericTypes.INT,
                      mc.markerVariable,
                      CIntegerLiteralExpression.ONE, CBinaryExpression.BinaryOperator.EQUALS)));
            }

            if (newShadowCode.size() > 0) {
              final AutomatonInternalState skipToErrorState =
                  skipOverShadowcodeToError(assumptions,
                      (Set<SafetyProperty>) t.getViolatedWhenEnteringTarget());

              qPrimeTrans.add(new AutomatonTransition(
                  t.getTrigger(),
                  ImmutableList.of(), //FIXME
                  ImmutableList.of(),
                  true,               //FIXME
                  newShadowCode,
                  ExpressionTrees.getTrue(),
                  t.getActions(),
                  skipToErrorState.getName(), skipToErrorState,
                  ImmutableSet.of(),
                  ImmutableSet.of()));
            } else {
              qPrimeTrans.add(new AutomatonTransition(
                  t.getTrigger(),
                  ImmutableList.of(), //FIXME
                  assumptions,
                  true,               //FIXME
                  newShadowCode,
                  ExpressionTrees.getTrue(),
                  t.getActions(),
                  t.getFollowState().getName(), null,
                  t.getViolatedWhenEnteringTarget(),
                  t.getViolatedWhenAssertionFailed()));
            }
          }
        } else {
          qPrimeTrans.add(new AutomatonTransition(
              t.getTrigger(),
              ImmutableList.<AutomatonBoolExpr>of(), //FIXME
              t.getAssumptions(),
              true,               //FIXME
              newShadowCode,
              ExpressionTrees.<AExpression>getTrue(),
              t.getActions(),
              t.getFollowState().getName(), null,
              t.getViolatedWhenEnteringTarget(),
              t.getViolatedWhenAssertionFailed()));
        }
      }

      resultStates.add(new AutomatonInternalState(q.getName(), qPrimeTrans, q.isTarget(), q.isNonDetState()));
    }

    // -- construct the new initial state that initializes the markers when entering the program
    List<AAstNode> initMarkersCode = Lists.newLinkedList();
    for (MarkerCode e: markerCode.values()) {
      initMarkersCode.add(e.markerDeclarationStatement);
    }
    final AutomatonTransition initTransition = new AutomatonTransition(AutomatonBoolExpr.MatchProgramEntry.INSTANCE,
        ImmutableList.<AutomatonBoolExpr>of(),
        ImmutableList.<AStatement>of(), true,
        initMarkersCode, ExpressionTrees.<AExpression>getTrue(), ImmutableList.<AutomatonAction>of(),
        pInput.getInitialState().getName(),
        null, ImmutableSet.<SafetyProperty>of(), ImmutableSet.<SafetyProperty>of());

    final AutomatonInternalState resultInitialState = new AutomatonInternalState(resultInitialStateName,
        ImmutableList.of(initTransition), false, false);
    resultStates.add(resultInitialState);

    resultStates.add(AutomatonInternalState.BOTTOM);
    resultStates.add(AutomatonInternalState.INTERMEDIATEINACTIVE);

    // -- assemble the resulting automaton
    try {
      return new Automaton(pInput.getPropertyFactory(),
          pInput.getName(), Maps.<String, AutomatonVariable> newHashMap(),
          resultStates, resultInitialStateName);

    } catch (InvalidAutomatonException e) {
      throw new RuntimeException("Conversion failed!", e);
    }

  }

  private static boolean shouldMark(BackLinkedState pQ, AutomatonTransition pT) {
    return pT.getActions().contains(AutomatonAction.SetMarkerVariable.getInstance());
  }

}
