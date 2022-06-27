// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.giacombiner;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.core.algorithm.giageneration.GIAARGStateEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.MatchOtherwise;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpression.ResultValue;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonExpressionArguments;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonInternalState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonStateTypes;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonTargetInformation;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonTransition;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonVariable;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.Pair;

public class GIACombinerTransferRelation extends SingleEdgeTransferRelation {

  private final LogManager logger;
  private final CFA cfa;
  private Map<AutomatonInternalState, Boolean> mayLeadToFinalInAutomaton1 = new HashMap<>();
  private Map<AutomatonInternalState, Boolean> mayLeadToFinalInAutomaton2 = new HashMap<>();
  private Map<AutomatonInternalState, Boolean> onlyLeadsToFUInAutomaton1 = new HashMap<>();
  private Map<AutomatonInternalState, Boolean> onlyLeadsToFUInAutomaton2 = new HashMap<>();
  private Automaton automaton1;
  private Automaton automaton2;

  public GIACombinerTransferRelation(LogManager pLogger, CFA pCFA) {
    this.logger = pLogger;
    this.cfa = pCFA;
  }

  public void setAutomaton(Automaton pAutomaton1, Automaton pAutomaton2) {
    this.automaton1 = pAutomaton1;
    this.automaton2 = pAutomaton2;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    assert state instanceof GIACombinerState;
    GIACombinerState combinerState = (GIACombinerState) state;
    AbstractGIAState first = combinerState.getStateOfAutomaton1();
    AbstractGIAState second = combinerState.getStateOfAutomaton2();

    if (first instanceof NotPresentGIAState && second instanceof NotPresentGIAState) {
      // This is a shortcut to abort the computation, if only a single gia is present
      return ImmutableSet.of();
    }

    // Abort if a target state is reached
    if (stateIsTargetOrNonTarget(first) || stateIsTargetOrNonTarget(second)) {
      return ImmutableSet.of();
    }

    if (first.statePresent()) {
      AutomatonState firstState = ((GIAInternalState) first).getAutomatonState();

      Map<AutomatonTransition, AutomatonState> successorFirst = getSuccesor(firstState, cfaEdge);
      if (successorFirst.isEmpty()) {
        throw new CPATransferException(
            "The automaton cannot be merged, as the first state has no successor"
                + "and the second state is already irrelevant!");
      } else {
        if (!second.statePresent()) {
          // Second is not present
          Set<GIACombinerState> successors = new HashSet<>();
          for (Entry<AutomatonTransition, AutomatonState> transition : successorFirst.entrySet()) {
            GIACombinerState newState =
                new GIACombinerState(new GIAInternalState(transition.getValue()), second.copy());
            successors.add(newState);
            combinerState.addSuccessor(
                new GIATransition(
                    transition.getKey().getTrigger(),
                    transition.getKey().getAssumptions(),
                    GIAARGStateEdge.getScopeForEdge(cfaEdge)),
                newState);
          }
          return successors;
        } else {

          AutomatonState secondState = ((GIAInternalState) second).getAutomatonState();

          Map<AutomatonTransition, AutomatonState> successorSecond =
              getSuccesor(secondState, cfaEdge);
          if (successorSecond.isEmpty()
              || (successorSecond.entrySet().stream()
                      .allMatch(t -> t.getKey().getTrigger() instanceof MatchOtherwise)
                  && successorFirst.entrySet().stream()
                      .noneMatch(
                          t ->
                              t.getKey().getTrigger() instanceof MatchOtherwise
                                  || t.getKey().getTrigger() == AutomatonBoolExpr.TRUE))) {
            // Second has no trnasitions at this edge or only an otherwise edge and first has no
            // otherweise edge
            Set<GIACombinerState> successors = new HashSet<>();
            for (Entry<AutomatonTransition, AutomatonState> transition :
                successorFirst.entrySet()) {
              GIACombinerState newState =
                  new GIACombinerState(
                      new GIAInternalState(transition.getValue()), new NotPresentGIAState());
              successors.add(newState);
              combinerState.addSuccessor(
                  new GIATransition(
                      transition.getKey().getTrigger(),
                      transition.getKey().getAssumptions(),
                      GIAARGStateEdge.getScopeForEdge(cfaEdge)),
                  newState);
            }
            return successors;
          } else if (successorFirst.isEmpty()
              || (successorFirst.entrySet().stream()
                      .allMatch(t -> t.getKey().getTrigger() instanceof MatchOtherwise)
                  && successorSecond.entrySet().stream()
                      .noneMatch(
                          t ->
                              t.getKey().getTrigger() instanceof MatchOtherwise
                                  || t.getKey().getTrigger() == AutomatonBoolExpr.TRUE))) {
            // First has no transitions at this edge or only a self-loop and second has no otherwise
            // edge
            Set<GIACombinerState> successors = new HashSet<>();
            for (Entry<AutomatonTransition, AutomatonState> transition :
                successorSecond.entrySet()) {
              GIACombinerState newState =
                  new GIACombinerState(
                      new NotPresentGIAState(), new GIAInternalState(transition.getValue()));
              successors.add(newState);
              combinerState.addSuccessor(
                  new GIATransition(
                      transition.getKey().getTrigger(),
                      transition.getKey().getAssumptions(),
                      GIAARGStateEdge.getScopeForEdge(cfaEdge)),
                  newState);
            }
            return successors;
          } else if (successorFirst.entrySet().stream()
              .allMatch(t -> t.getKey().getTrigger() == AutomatonBoolExpr.TRUE)) {
            // First is only true (hence we are in qTemp, qFinal or qError, hence no need to merge
            Set<GIACombinerState> successors = new HashSet<>();
            for (Entry<AutomatonTransition, AutomatonState> transition :
                successorSecond.entrySet()) {
              GIACombinerState newState =
                  new GIACombinerState(first, new GIAInternalState(transition.getValue()));
              successors.add(newState);
              combinerState.addSuccessor(
                  new GIATransition(
                      transition.getKey().getTrigger(),
                      transition.getKey().getAssumptions(),
                      GIAARGStateEdge.getScopeForEdge(cfaEdge)),
                  newState);
            }
            return successors;
          } else if (successorSecond.entrySet().stream()
              .allMatch(t -> t.getKey().getTrigger() == AutomatonBoolExpr.TRUE)) {
            // Second is only true (hence we are in qTemp, qFinal or qError, hence no need to merge
            Set<GIACombinerState> successors = new HashSet<>();
            for (Entry<AutomatonTransition, AutomatonState> transition :
                successorFirst.entrySet()) {
              GIACombinerState newState = new GIACombinerState(first, new NotPresentGIAState());
              successors.add(newState);
              combinerState.addSuccessor(
                  new GIATransition(
                      transition.getKey().getTrigger(),
                      transition.getKey().getAssumptions(),
                      GIAARGStateEdge.getScopeForEdge(cfaEdge)),
                  newState);   }
            return successors;
            }else {
            // Both states have at least one transition different than ohterwiese or both are
            // otherwise
            Map<AutomatonTransition, Set<AutomatonTransition>> edgesTakenByBoth =
                edgesWithSameSourceCodeLine(successorFirst.keySet(), successorSecond.keySet());
            Set<GIACombinerState> successors = new HashSet<>();
            // Merge according to MERGE algorithm
            for (AutomatonTransition transitionFirst : edgesTakenByBoth.keySet()) {
              for (AutomatonTransition transitionSecond : edgesTakenByBoth.get(transitionFirst)) {
                successors.addAll(
                    merge(
                        combinerState,
                        transitionFirst,
                        successorFirst.get(transitionFirst),
                        transitionSecond,
                        successorSecond.get(transitionSecond),
                        cfaEdge,
                        logger,
                        cfa.getMachineModel()));
              }
            }
            return successors;
          }
        }
      }
    } else if (second.statePresent()) {

      AutomatonState secondState = ((GIAInternalState) second).getAutomatonState();

      Map<AutomatonTransition, AutomatonState> successorSecond = getSuccesor(secondState, cfaEdge);
      if (successorSecond.isEmpty()) {
        throw new CPATransferException(
            "The automaton cannot be merged, as the first state has no succesor"
                + "and the second state is already irrlelvant!");
      } else {
        if (!first.statePresent()) {
          // First is not present
          Set<GIACombinerState> successors = new HashSet<>();
          for (Entry<AutomatonTransition, AutomatonState> transition : successorSecond.entrySet()) {
            GIACombinerState newState =
                new GIACombinerState(first.copy(), new GIAInternalState(transition.getValue()));
            successors.add(newState);
            combinerState.addSuccessor(
                new GIATransition(
                    transition.getKey().getTrigger(),
                    transition.getKey().getAssumptions(),
                    GIAARGStateEdge.getScopeForEdge(cfaEdge)),
                newState);
          }
          return successors;
        } else {

          AutomatonState firstState = ((GIAInternalState) first).getAutomatonState();

          Map<AutomatonTransition, AutomatonState> successorFirst =
              getSuccesor(firstState, cfaEdge);
          if (successorFirst.isEmpty()
              || (successorFirst.entrySet().stream()
                      .allMatch(t -> t.getKey().getTrigger() instanceof MatchOtherwise)
                  && successorSecond.entrySet().stream()
                      .noneMatch(
                          t ->
                              t.getKey().getTrigger() instanceof MatchOtherwise
                                  || t.getKey().getTrigger() == AutomatonBoolExpr.TRUE))) {
            // First has no transitions at this edge or only a self-loop
            Set<GIACombinerState> successors = new HashSet<>();
            for (Entry<AutomatonTransition, AutomatonState> transition :
                successorSecond.entrySet()) {
              GIACombinerState newState =
                  new GIACombinerState(
                      new NotPresentGIAState(), new GIAInternalState(transition.getValue()));
              successors.add(newState);
              combinerState.addSuccessor(
                  new GIATransition(
                      transition.getKey().getTrigger(),
                      transition.getKey().getAssumptions(),
                      GIAARGStateEdge.getScopeForEdge(cfaEdge)),
                  newState);
            }
            return successors;
          } else {
            // Nothing to do here, as handled above (hence unreachable)
            throw new CPATransferException(
                "This code should be unreachable, hence an error occured!");
          }
        }
      }

    } else {
      throw new CPATransferException(
          "Cannot compute the transfer relation, if both states are not presetn!");
    }
  }

  private boolean stateIsTargetOrNonTarget(AbstractGIAState first) {
    return first.statePresent()
        && (((GIAInternalState) first)
                .getAutomatonState()
                .getInternalState()
                .getStateType()
                .equals(AutomatonStateTypes.NON_TARGET)
            || ((GIAInternalState) first)
                .getAutomatonState()
                .getInternalState()
                .getStateType()
                .equals(AutomatonStateTypes.TARGET));
  }

  private List<GIACombinerState> merge(
      GIACombinerState pCurrentState,
      AutomatonTransition pTransitionFirst,
      AutomatonState pFirst,
      AutomatonTransition pTransitionSecond,
      AutomatonState pSecond,
      CFAEdge pCfaEdge,
      LogManager pLogger,
      MachineModel pMachineModel) {

    if (pFirst.getCandidateInvariants().equals(pSecond.getCandidateInvariants())
        && pTransitionFirst
            .getAssumptions(pCfaEdge, pLogger, pMachineModel)
            .equals(pTransitionSecond.getAssumptions(pCfaEdge, pLogger, pMachineModel))) {
      // CAse in line 2
      GIACombinerState newState =
          new GIACombinerState(new GIAInternalState(pFirst), new GIAInternalState(pSecond));
      pCurrentState.addSuccessor(
          new GIATransition(
              pTransitionFirst.getTrigger(),
              pTransitionFirst.getAssumptions(),
              GIAARGStateEdge.getScopeForEdge(pCfaEdge)),
          newState);
      return Lists.newArrayList(newState);
    } else if (pFirst.getCandidateInvariants().equals(pSecond.getCandidateInvariants())
        && onlyLeadsToFU(pFirst, automaton1)
        && onlyLeadsToFU(pSecond,automaton2)
        && ((isTrueAssumption(pTransitionFirst, pCfaEdge, pLogger, pMachineModel)
                && trueAssumptions(pFirst, pCfaEdge, pLogger, pMachineModel))
            || (isTrueAssumption(pTransitionSecond, pCfaEdge, pLogger, pMachineModel)
                && trueAssumptions(pSecond, pCfaEdge, pLogger, pMachineModel)))) {
      // CAse in line 5
      GIACombinerState newState =
          new GIACombinerState(new GIAInternalState(pFirst), new GIAInternalState(pSecond));
      if (isTrueAssumption(pTransitionFirst, pCfaEdge, pLogger, pMachineModel)
          && trueAssumptions(pFirst, pCfaEdge, pLogger, pMachineModel)) {
        pCurrentState.addSuccessor(
            new GIATransition(
                pTransitionFirst.getTrigger(),
                pTransitionFirst.getAssumptions(),
                GIAARGStateEdge.getScopeForEdge(pCfaEdge)),
            newState);
        return Lists.newArrayList(newState);
      } else {
        pCurrentState.addSuccessor(
            new GIATransition(
                pTransitionFirst.getTrigger(),
                pTransitionSecond.getAssumptions(),
                GIAARGStateEdge.getScopeForEdge(pCfaEdge)),
            newState);
        return Lists.newArrayList(newState);
      }
    } else if (pFirst.getCandidateInvariants().equals(pSecond.getCandidateInvariants())
        && mayLeadToFinalOrNonFinal(pFirst, automaton1)
        && onlyLeadsToFU(pSecond,automaton2)) {
      // Case in line 8
      GIACombinerState newState =
          new GIACombinerState(new GIAInternalState(pFirst), new GIAInternalState(pSecond));
      pCurrentState.addSuccessor(
          new GIATransition(
              pTransitionFirst.getTrigger(),
              pTransitionFirst.getAssumptions(),
              GIAARGStateEdge.getScopeForEdge(pCfaEdge)),
          newState);
      return Lists.newArrayList(newState);
    } else if (pFirst.getCandidateInvariants().equals(pSecond.getCandidateInvariants())
        && onlyLeadsToFU(pFirst,automaton1)
        && mayLeadToFinalOrNonFinal(pSecond, automaton2)) {
      // Case in line 11
      GIACombinerState newState =
          new GIACombinerState(new GIAInternalState(pFirst), new GIAInternalState(pSecond));
      pCurrentState.addSuccessor(
          new GIATransition(
              pTransitionSecond.getTrigger(),
              pTransitionSecond.getAssumptions(),
              GIAARGStateEdge.getScopeForEdge(pCfaEdge)),
          newState);
      return Lists.newArrayList(newState);
    } else {
      // Case in line 14
      GIACombinerState newState1 =
          new GIACombinerState(new GIAInternalState(pFirst), new SplitGIAState());
      pCurrentState.addSuccessor(
          new GIATransition(
              pTransitionFirst.getTrigger(),
              pTransitionFirst.getAssumptions(),
              GIAARGStateEdge.getScopeForEdge(pCfaEdge)),
          newState1);
      GIACombinerState newState2 =
          new GIACombinerState(new SplitGIAState(), new GIAInternalState(pSecond));
      pCurrentState.addSuccessor(
          new GIATransition(
              pTransitionSecond.getTrigger(),
              pTransitionSecond.getAssumptions(),
              GIAARGStateEdge.getScopeForEdge(pCfaEdge)),
          newState2);
      return Lists.newArrayList(newState1, newState2);
    }
  }

  /** Checks if any path starting in pFirst leads to a state in final or Non-Final */
  private boolean mayLeadToFinalOrNonFinal(AutomatonState pFirst, Automaton pAutomaton) {

    Set<AutomatonInternalState> processed = new HashSet<>();
    Set<AutomatonInternalState> toProcess = new HashSet<>();
    toProcess.add(pFirst.getInternalState());
    Map<AutomatonInternalState, Boolean> currentMap;

    if (pAutomaton.equals(automaton1)) {
      currentMap = this.mayLeadToFinalInAutomaton1;
    } else {
      currentMap = this.mayLeadToFinalInAutomaton2;
    }
    if (currentMap.containsKey(pFirst.getInternalState())) {
      return currentMap.get(pFirst.getInternalState());
    }
    if (pFirst.getInternalState().getStateType().equals(AutomatonStateTypes.NON_TARGET)
        || pFirst.getInternalState().getStateType().equals(AutomatonStateTypes.TARGET)) {
      currentMap.putIfAbsent(pFirst.getInternalState(), true);
      return true;
    }
    while (!toProcess.isEmpty()) {
      AutomatonInternalState current =toProcess.stream().findFirst().orElseThrow();
          toProcess.remove(current);
      if (current.getTransitions().stream()
          .anyMatch(
              t ->
                  t.getFollowState().getStateType().equals(AutomatonStateTypes.NON_TARGET)
                      || t.getFollowState().getStateType().equals(AutomatonStateTypes.TARGET))) {
        currentMap.putIfAbsent(pFirst.getInternalState(), true);
        return true;
      } else {
        processed.add(current);
        current
            .getTransitions()
            .forEach(
                t -> {
                  if (!processed.contains(t.getFollowState())) {
                    toProcess.add(t.getFollowState());
                  }
                });

      }
    }
    currentMap.putIfAbsent(pFirst.getInternalState(), false);
    return false;
  }

  private boolean isTrueAssumption(
      AutomatonTransition pTrans,
      CFAEdge pCfaEdge,
      LogManager pLogger,
      MachineModel pMachineModel) {
    ImmutableList<AExpression> ass = pTrans.getAssumptions(pCfaEdge, pLogger, pMachineModel);
    return ass.isEmpty()
        || ass.stream()
            .allMatch(
                a ->
                    a instanceof CIntegerLiteralExpression
                        && a.equals(CIntegerLiteralExpression.ONE));
  }

  /** Checks, if all path starting in pFirst have true assumptions only */
  private boolean trueAssumptions(
      AutomatonState pFirst, CFAEdge pCfaEdge, LogManager pLogger, MachineModel pMachineModel) {
    Set<AutomatonInternalState> processed = new HashSet<>();
    Set<AutomatonInternalState> toProcess = new HashSet<>();
    toProcess.add(pFirst.getInternalState());

    while (!toProcess.isEmpty()) {
      AutomatonInternalState current = toProcess.stream().findFirst().orElseThrow();
          toProcess.remove(current);
      if (current.getTransitions().stream()
          .anyMatch(t -> isTrueAssumption(t, pCfaEdge, pLogger, pMachineModel))) {
        return false;
      } else {
        processed.add(current);
        current
            .getTransitions()
            .forEach(
                t -> {
                  if (!processed.contains(t.getFollowState())) {
                    toProcess.add(t.getFollowState());
                  }
                });

      }
    }
    return true;
  }

  /**
   * Checks, if (1) pFirst and any successor is neither target not non-target, (2) at least one
   * successor of pFirst or pFirst is in unknown.
   */
  private boolean onlyLeadsToFU(AutomatonState pFirst, Automaton pAutomaton) {
    boolean unknownAlreadySeen =
        pFirst.getInternalState().getStateType().equals(AutomatonStateTypes.UNKNOWN);
    Set<AutomatonInternalState> processed = new HashSet<>();
    Set<AutomatonInternalState> toProcess = new HashSet<>();
    toProcess.add(pFirst.getInternalState());

    Map<AutomatonInternalState, Boolean> currentMap;
    if (pAutomaton.equals(automaton1)) {
      currentMap = this.onlyLeadsToFUInAutomaton1;
    } else {
      currentMap = this.onlyLeadsToFUInAutomaton2;
    }
    if (currentMap.containsKey(pFirst.getInternalState())) {
      return currentMap.get(pFirst.getInternalState());
    }

    if (pFirst.getInternalState().getStateType().equals(AutomatonStateTypes.NON_TARGET)
        || pFirst.getInternalState().getStateType().equals(AutomatonStateTypes.TARGET)) {
      currentMap.putIfAbsent(pFirst.getInternalState(), false);logger.log(Level.INFO,"returning false");
      return false;
    }
    while (!toProcess.isEmpty()) {
      AutomatonInternalState current = toProcess.stream().findFirst().orElseThrow();
          toProcess.remove(current);

      if (current.getTransitions().stream()
          .anyMatch(
              t ->
                  t.getFollowState().getStateType().equals(AutomatonStateTypes.NON_TARGET)
                      || t.getFollowState().getStateType().equals(AutomatonStateTypes.TARGET))) {
        currentMap.putIfAbsent(pFirst.getInternalState(), false);

        return false;
      } else {
        if (current.getStateType().equals(AutomatonStateTypes.UNKNOWN)) {
          unknownAlreadySeen = true;
        }
        processed.add(current);
        current
            .getTransitions()
            .forEach(
                t -> {
                  if (!processed.contains(t.getFollowState())) {
                    toProcess.add(t.getFollowState());
                  }
                });

      }
    }
    currentMap.putIfAbsent(pFirst.getInternalState(), unknownAlreadySeen);
    return unknownAlreadySeen;
  }

  /**
   * Computes a map each key is from pFirst and points to all edges from pSecond with the same
   * source code guard
   */
  private Map<AutomatonTransition, Set<AutomatonTransition>> edgesWithSameSourceCodeLine(
      Set<AutomatonTransition> pFirst, Set<AutomatonTransition> pSecond) {

    Map<AutomatonTransition, Set<AutomatonTransition>> res = new HashMap<>();
    for (AutomatonTransition first : pFirst) {
      for (AutomatonTransition second : pSecond) {
        if (first.getTrigger().equals(second.getTrigger())) {
          if (res.containsKey(first)) {
            res.get(first).add(second);
          } else {
            res.put(first, Sets.newHashSet(second));
          }
          //          if (res.containsKey(second)) {
          //            res.get(second).add(first);
          //          } else {
          //            res.put(second, Sets.newHashSet(first));
          //          }
        }
      }
    }
    return res;
  }

  private Map<AutomatonTransition, AutomatonState> getSuccesor(AutomatonState state, CFAEdge edge)
      throws CPATransferException {

    Map<AutomatonTransition, AutomatonState> lSuccessors = new HashMap<>();
    AutomatonExpressionArguments exprArgs =
        new AutomatonExpressionArguments(state, state.getVars(), new ArrayList<>(), edge, logger);
    boolean edgeMatched = false;
    boolean nonDetState = state.getInternalState().isNonDetState();

    // these transitions cannot be evaluated until last, because they might have sideeffects on
    // other CPAs (dont want to execute them twice)
    // the transitionVariables have to be cached (produced during the match operation)
    // the list holds a Transition and the TransitionVariables generated during its match
    List<Pair<AutomatonTransition, Map<Integer, AAstNode>>> transitionsToBeTaken =
        new ArrayList<>(2);

    for (AutomatonTransition t : state.getInternalState().getTransitions()) {
      exprArgs.clearTransitionVariables();

      ResultValue<Boolean> match = t.match(exprArgs);

      if (match.canNotEvaluate()) {
        // if one transition cannot be evaluated the evaluation must be postponed until enough
        // information is available
        throw new CPATransferException("unknown transitions currently not supported");
        // TODO: Check if needed
        // return ImmutableSet.of(new AutomatonUnknownState(state));
      } else {
        if (match.getValue()) {
          edgeMatched = true;

          ResultValue<Boolean> assertionsHold = t.assertionsHold(exprArgs);

          if (assertionsHold.canNotEvaluate()) {
            // TODO: Check if needed
            // cannot yet be evaluated
            //            return ImmutableSet.of(new AutomatonUnknownState(state));
            throw new CPATransferException("unknown transitions currently not supported");
          } else if (assertionsHold.getValue()) {
            if (!t.canExecuteActionsOn(exprArgs)) {
              // TODO: Check if needed
              // cannot yet execute, goto UnknownState
              //              return ImmutableSet.of(new AutomatonUnknownState(state));
              throw new CPATransferException("unknown transitions currently not supported");
            }
            if (t.getTrigger() instanceof MatchOtherwise) {
              if (!transitionsToBeTaken.isEmpty()) {
                // The otherwise edge is by default true.
                // If there is another transition that was taken in advance,
                // it is ignored at this point
                continue;
              }
            }

            // delay execution as described above
            Map<Integer, AAstNode> transitionVariables =
                ImmutableMap.copyOf(exprArgs.getTransitionVariables());
            transitionsToBeTaken.add(Pair.of(t, transitionVariables));

          } else {
            // matching transitions, but unfulfilled assertions: goto error state
            final String desc = Strings.nullToEmpty(t.getTargetInformation(exprArgs));
            AutomatonTargetInformation prop =
                new AutomatonTargetInformation(state.getOwningAutomaton(), t, desc);

            AutomatonState errorState =
                AutomatonState.automatonStateFactory(
                    ImmutableMap.of(),
                    AutomatonInternalState.ERROR,
                    state.getOwningAutomaton(),
                    0,
                    0,
                    prop,
                    state.isTreatingErrorsAsTarget());

            logger.log(
                Level.FINER,
                "Automaton going to ErrorState on edge \"" + edge.getDescription() + "\"");
            lSuccessors.put(t, errorState);
          }

          if (!nonDetState) {
            // not a nondet State, break on the first matching edge
            break;
          }
        } else {
          // do nothing if the edge did not match
        }
      }
    }

    if (edgeMatched) {
      // execute Transitions
      for (Pair<AutomatonTransition, Map<Integer, AAstNode>> pair : transitionsToBeTaken) {
        // this transition will be taken. copy the variables
        AutomatonTransition t = pair.getFirst();
        Map<Integer, AAstNode> transitionVariables = pair.getSecond();

        Map<String, AutomatonVariable> newVars = deepCloneVars(state.getVars());
        exprArgs.setAutomatonVariables(newVars);
        exprArgs.putTransitionVariables(transitionVariables);
        t.executeActions(exprArgs);

        AutomatonTargetInformation targetInformation = null;
        if (t.getFollowState().isTarget()) {
          final String desc = Strings.nullToEmpty(t.getTargetInformation(exprArgs));
          targetInformation = new AutomatonTargetInformation(state.getOwningAutomaton(), t, desc);
        }

        logger.log(Level.ALL, "Replace variables in automata assumptions");
        ImmutableList<AExpression> instantiatedAssumes =
            exprArgs.instantiateAssumptions(t.getAssumptions(edge, logger, cfa.getMachineModel()));

        AutomatonState lSuccessor =
            AutomatonState.automatonStateFactory(
                newVars,
                t.getFollowState(),
                state.getOwningAutomaton(),
                instantiatedAssumes,
                t.getCandidateInvariants(),
                state.getMatches() + 1,
                state.getFailedMatches(),
                targetInformation,
                state.isTreatingErrorsAsTarget());

        if (!(lSuccessor instanceof AutomatonState.BOTTOM)) {
          lSuccessors.put(t, lSuccessor);
        } else {
          // add nothing
        }
      }
      return lSuccessors;
    } else {
      // stay in same state, no transitions to be executed here (no transition matched)
      //      AutomatonState stateNewCounters =
      //          AutomatonState.automatonStateFactory(
      //              state.getVars(),
      //              state.getInternalState(),
      //              state.getOwningAutomaton(),
      //              state.getMatches(),
      //              state.getFailedMatches() + failedMatches,
      //              null,
      //              state.isTreatingErrorsAsTarget());
      return new HashMap<>();
    }
  }

  private static Map<String, AutomatonVariable> deepCloneVars(Map<String, AutomatonVariable> pOld) {
    Map<String, AutomatonVariable> result = Maps.newHashMapWithExpectedSize(pOld.size());
    for (Entry<String, AutomatonVariable> e : pOld.entrySet()) {
      result.put(e.getKey(), e.getValue().clone());
    }
    return result;
  }
}
