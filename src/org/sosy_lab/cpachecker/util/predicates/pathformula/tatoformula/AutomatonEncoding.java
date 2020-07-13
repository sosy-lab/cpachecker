// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula;

import static com.google.common.base.Predicates.instanceOf;
import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariableCondition;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.timedautomata.TAUnrollingState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions.EncodingExtension;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.actionencodings.ActionEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.locationencodings.LocationEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.timeencodings.TimeEncoding;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class AutomatonEncoding implements TAFormulaEncoding {
  protected final FormulaManagerView fmgr;
  protected final BooleanFormulaManagerView bFmgr;
  private final Collection<TaDeclaration> automata;
  protected final Map<TaDeclaration, Collection<TCFANode>> nodesByAutomaton;
  protected final Map<TaDeclaration, Collection<TCFAEdge>> edgesByAutomaton;
  private final ActionEncoding actions;
  protected final LocationEncoding locations;
  private final TimeEncoding time;
  private Map<TaDeclaration, Collection<TCFANode>> initialNodesByAutomaton;
  private Iterable<EncodingExtension> extensions;

  // Placeholder for config
  private boolean useDelayAction = true;
  private boolean useIdleAction = false;

  private boolean actionDetachedDelayTransition = false;
  private boolean actionDetachedIdleTransition = true;

  private boolean noTwoActions = true;

  public AutomatonEncoding(
      FormulaManagerView pFmgr,
      CFA pCfa,
      TimeEncoding pTime,
      ActionEncoding pActions,
      LocationEncoding pLocations,
      Iterable<EncodingExtension> pExtensions) {
    fmgr = pFmgr;
    bFmgr = fmgr.getBooleanFormulaManager();
    time = pTime;
    actions = pActions;
    locations = pLocations;
    extensions = pExtensions;

    automata =
        from(pCfa.getAllFunctions().values())
            .transform(functionEntry -> (TaDeclaration) functionEntry.getFunctionDefinition())
            .toSet();

    initialNodesByAutomaton = new HashMap<>();
    var nodes = from(pCfa.getAllNodes()).filter(instanceOf(TCFANode.class));
    for (var node : nodes) {
      var tNode = (TCFANode) node;
      initialNodesByAutomaton.computeIfAbsent(
          tNode.getAutomatonDeclaration(), automaton -> new HashSet<>());
      if (tNode.isInitialState()) {
        initialNodesByAutomaton.get(tNode.getAutomatonDeclaration()).add(tNode);
      }
    }

    nodesByAutomaton =
        from(pCfa.getAllNodes())
            .filter(instanceOf(TCFANode.class))
            .transform(node -> (TCFANode) node)
            .index(node -> node.getAutomatonDeclaration())
            .asMap();
    edgesByAutomaton = new HashMap<>();
    nodesByAutomaton
        .entrySet()
        .forEach(
            entry -> {
              var edges =
                  from(entry.getValue())
                      .transformAndConcat(node -> CFAUtils.allEnteringEdges(node))
                      .filter(instanceOf(TCFAEdge.class))
                      .transform(edge -> (TCFAEdge) edge);
              edgesByAutomaton.put(entry.getKey(), edges.toSet());
            });
  }

  @Override
  public Collection<BooleanFormula> buildSuccessorFormulas(
      BooleanFormula pPredecessor, int pLastReachedIndex, CFAEdge pEdge) {
    throw new UnsupportedOperationException(
        "Location wise formula construction not supported by " + this.getClass().getSimpleName());
  }

  @Override
  public Collection<BooleanFormula> buildSuccessorFormulas(
      BooleanFormula pPredecessor, int pLastReachedIndex) {
    var result = bFmgr.makeTrue();

    var automataFormulas =
        from(automata)
            .transform(automaton -> makeSuccessorFormulaForAutomaton(automaton, pLastReachedIndex))
            .toSet();
    result = bFmgr.and(bFmgr.and(automataFormulas), result);

    if (noTwoActions) {
      var allActions =
          ImmutableSet.copyOf(
              actions.makeAllActionFormulas(pLastReachedIndex, useDelayAction, useIdleAction));
      var actionPairs =
          from(Sets.cartesianProduct(allActions, allActions))
              .filter(pair -> !pair.get(0).equals(pair.get(1)))
              .transform(pair -> bFmgr.and(pair.get(0), pair.get(1)));
      var noTwoActionsFormula = bFmgr.not(bFmgr.or(actionPairs.toSet()));
      result = bFmgr.and(noTwoActionsFormula, result);
    }

    return ImmutableSet.of(result);
  }

  private BooleanFormula makeSuccessorFormulaForAutomaton(
      TaDeclaration pAutomaton, int pLastReachedIndex) {
    var extensionFormulas =
        from(extensions)
            .transform(extension -> extension.makeAutomatonStep(pAutomaton, pLastReachedIndex));
    var extensionsFormula = bFmgr.and(extensionFormulas.toSet());
    var automatonTransitionsFormula =
        makeAutomatonTransitionsFormula(pAutomaton, pLastReachedIndex);
    return bFmgr.and(automatonTransitionsFormula, extensionsFormula);
  }

  protected BooleanFormula makeAutomatonTransitionsFormula(
      TaDeclaration pAutomaton, int pLastReachedIndex) {
    var delayFormula = makeDelayTransition(pAutomaton, pLastReachedIndex);
    var idleFormula = makeIdleTransition(pAutomaton, pLastReachedIndex);

    var discreteSteps =
        from(edgesByAutomaton.get(pAutomaton))
            .transform(edge -> makeDiscreteStep(pAutomaton, pLastReachedIndex, edge));

    return bFmgr.or(delayFormula, idleFormula, bFmgr.or(discreteSteps.toSet()));
  }

  protected BooleanFormula makeDiscreteStep(
      TaDeclaration pAutomaton, int pLastReachedIndex, TCFAEdge pEdge) {
    var guardFormula =
        pEdge
            .getGuard()
            .transform(guard -> time.makeConditionFormula(pAutomaton, pLastReachedIndex, guard))
            .or(bFmgr.makeTrue());

    var invariantFormula = bFmgr.makeTrue();
    if (invariantType == InvariantType.LOCAL) {
      Optional<TaVariableCondition> targetInvariant = Optional.absent();
      if (pEdge.getSuccessor() instanceof TCFANode) {
        targetInvariant = ((TCFANode) pEdge.getSuccessor()).getInvariant();
      }
      invariantFormula =
          targetInvariant
              .transform(
                  invariant ->
                      time.makeConditionFormula(pAutomaton, pLastReachedIndex + 1, invariant))
              .or(bFmgr.makeTrue());
    }

    var clockResets =
        time.makeResetToZeroFormula(pAutomaton, pLastReachedIndex + 1, pEdge.getVariablesToReset());
    var clocksUnchanged =
        time.makeClockVariablesDoNotChangeFormula(
            pAutomaton,
            pLastReachedIndex,
            Sets.difference(pAutomaton.getClocks(), pEdge.getVariablesToReset()));
    var clockFormulas = bFmgr.and(clockResets, clocksUnchanged);

    var locationBefore =
        locations.makeLocationEqualsFormula(
            pAutomaton, pLastReachedIndex, (TCFANode) pEdge.getPredecessor());
    var locationAfter =
        locations.makeLocationEqualsFormula(
            pAutomaton, pLastReachedIndex + 1, (TCFANode) pEdge.getSuccessor());

    var timeDoesNotAdvance = time.makeTimeDoesNotAdvanceFormula(pAutomaton, pLastReachedIndex);

    var actionFormula =
        pEdge
            .getAction()
            .transform(
                action -> actions.makeActionEqualsFormula(pAutomaton, pLastReachedIndex, action))
            .or(actions.makeLocalDummyActionFormula(pAutomaton, pLastReachedIndex));

    return bFmgr.and(
        guardFormula,
        clockFormulas,
        invariantFormula,
        locationBefore,
        locationAfter,
        timeDoesNotAdvance,
        actionFormula);
  }

  protected BooleanFormula makeDelayTransition(TaDeclaration pAutomaton, int pLastReachedIndex) {
    var result = bFmgr.makeTrue();
    result = bFmgr.and(locations.makeDoesNotChangeFormula(pAutomaton, pLastReachedIndex), result);
    result = bFmgr.and(time.makeTimeUpdateFormula(pAutomaton, pLastReachedIndex), result);
    if (useDelayAction) {
      result = bFmgr.and(actions.makeDelayActionFormula(pAutomaton, pLastReachedIndex), result);
    }

    if (invariantType == InvariantType.LOCAL) {
      result = bFmgr.and(makeAutomatonInvariantFormula(pAutomaton, pLastReachedIndex + 1), result);
    }

    if (actionDetachedDelayTransition) {
      if (useIdleAction) {
        result =
            bFmgr.and(
                bFmgr.not(actions.makeIdleActionFormula(pAutomaton, pLastReachedIndex)), result);
      }

      var notActionOccurs =
          from(pAutomaton.getActions())
              .transform(
                  action ->
                      bFmgr.not(
                          actions.makeActionEqualsFormula(pAutomaton, pLastReachedIndex, action)));
      var notDummyAction =
          bFmgr.not(actions.makeLocalDummyActionFormula(pAutomaton, pLastReachedIndex));
      result = bFmgr.and(bFmgr.and(notActionOccurs.toSet()), notDummyAction, result);
    }

    return result;
  }

  protected BooleanFormula makeIdleTransition(TaDeclaration pAutomaton, int pLastReachedIndex) {
    var result = bFmgr.makeTrue();
    result = bFmgr.and(locations.makeDoesNotChangeFormula(pAutomaton, pLastReachedIndex), result);
    result = bFmgr.and(time.makeTimeDoesNotAdvanceFormula(pAutomaton, pLastReachedIndex), result);
    result =
        bFmgr.and(
            time.makeClockVariablesDoNotChangeFormula(
                pAutomaton, pLastReachedIndex, pAutomaton.getClocks()),
            result);

    if (useIdleAction) {
      result = bFmgr.and(actions.makeIdleActionFormula(pAutomaton, pLastReachedIndex), result);
    }

    if (actionDetachedIdleTransition) {
      if (useDelayAction) {
        result =
            bFmgr.and(
                bFmgr.not(actions.makeDelayActionFormula(pAutomaton, pLastReachedIndex)), result);
      }

      var notActionOccurs =
          from(pAutomaton.getActions())
              .transform(
                  action ->
                      bFmgr.not(
                          actions.makeActionEqualsFormula(pAutomaton, pLastReachedIndex, action)));
      var notDummyAction =
          bFmgr.not(actions.makeLocalDummyActionFormula(pAutomaton, pLastReachedIndex));
      result = bFmgr.and(bFmgr.and(notActionOccurs.toSet()), notDummyAction, result);
    }

    return result;
  }

  private BooleanFormula makeAutomatonInvariantFormula(
      TaDeclaration pAutomaton, int pVariableIndex) {
    var invariantFormulas =
        from(nodesByAutomaton.get(pAutomaton))
            .filter(node -> node.getInvariant().isPresent())
            .transform(
                node -> {
                  var locationFormula =
                      locations.makeLocationEqualsFormula(pAutomaton, pVariableIndex, node);
                  var invariantFormula =
                      time.makeConditionFormula(
                          pAutomaton, pVariableIndex, node.getInvariant().get());
                  return bFmgr.implication(locationFormula, invariantFormula);
                });

    return bFmgr.and(invariantFormulas.toSet());
  }

  @Override
  public BooleanFormula getInitialFormula(CFANode pInitialNode, int pInitialIndex) {
    var encodingResults =
        from(automata)
            .transform(automaton -> makeInitialFormulaForAutomaton(automaton, pInitialIndex));
    return bFmgr.and(encodingResults.toList());
  }

  private BooleanFormula makeInitialFormulaForAutomaton(
      TaDeclaration pAutomaton, int pInitialIndex) {
    var initialLocations =
        from(initialNodesByAutomaton.get(pAutomaton))
            .transform(
                node -> locations.makeLocationEqualsFormula(pAutomaton, pInitialIndex, node));
    var initialTime = time.makeInitiallyZeroFormula(pAutomaton, pInitialIndex);

    var extensionFormulas =
        from(extensions)
            .transform(extension -> extension.makeInitialFormula(pAutomaton, pInitialIndex));
    var extensionsFormula = bFmgr.and(extensionFormulas.toSet());

    return bFmgr.and(initialTime, bFmgr.and(initialLocations.toSet()), extensionsFormula);
  }

  @Override
  public BooleanFormula getFormulaFromReachedSet(Iterable<AbstractState> pReachedSet) {
    var taStates =
        from(pReachedSet)
            .transform(aState -> AbstractStates.extractStateByType(aState, TAUnrollingState.class));

    var behaviorEncoding = bFmgr.and(taStates.transform(TAUnrollingState::getFormula).toSet());
    var maxUnrolling =
        taStates.transform(TAUnrollingState::getStepCount).stream()
            .collect(Collectors.maxBy(Integer::compareTo))
            .orElseThrow();

    var errorCondition = bFmgr.makeFalse();
    for (int i = 0; i <= maxUnrolling; i++) {
      errorCondition = bFmgr.or(makeFinalCondition(i), errorCondition);
    }

    return bFmgr.and(behaviorEncoding, errorCondition);
  }

  protected BooleanFormula makeFinalCondition(int pHighestReachedIndex) {
    return bFmgr.and(
        from(automata)
            .transform(automaton -> makeFinalConditionForAutomaton(automaton, pHighestReachedIndex))
            .toSet());
  }

  private BooleanFormula makeFinalConditionForAutomaton(
      TaDeclaration pAutomaton, int pHighestReachedIndex) {
    var errorNodes =
        from(nodesByAutomaton.get(pAutomaton)).filter(TCFANode::isErrorLocation).toSet();
    if (errorNodes.isEmpty()) {
      errorNodes = ImmutableSet.copyOf(nodesByAutomaton.get(pAutomaton));
    }
    var finalLocationsFormulas =
        from(errorNodes)
            .transform(
                node ->
                    locations.makeLocationEqualsFormula(pAutomaton, pHighestReachedIndex, node));
    var anyFinalLocationReached = bFmgr.or(finalLocationsFormulas.toSet());

    var extensionFormulas =
        from(extensions)
            .transform(
                extension ->
                    extension.makeFinalConditionForAutomaton(pAutomaton, pHighestReachedIndex));
    var extensionsFormula = bFmgr.and(extensionFormulas.toSet());

    return bFmgr.and(anyFinalLocationReached, extensionsFormula);
  }
}
