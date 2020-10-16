// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.encodings;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Queue;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.timedautomata.TAUnrollingState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TimedAutomatonView;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions.TAEncodingExtension;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.locations.TALocations;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.variables.TAVariables;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public abstract class TAEncodingBase implements TAFormulaEncoding {
  protected final FormulaManagerView fmgr;
  protected final BooleanFormulaManagerView bFmgr;
  protected final TALocations locations;
  private final TAVariables clocks;
  protected final TimedAutomatonView automata;
  private TAEncodingExtension extensions;

  private Queue<BooleanFormula> formulas;

  public TAEncodingBase(
      FormulaManagerView pFmgr,
      TimedAutomatonView pAutomata,
      TAVariables pClocks,
      TALocations pLocations,
      TAEncodingExtension pExtension) {
    fmgr = pFmgr;
    bFmgr = fmgr.getBooleanFormulaManager();
    automata = pAutomata;
    clocks = pClocks;
    locations = pLocations;
    extensions = pExtension;

    formulas = new ArrayDeque<>();
  }

  @Override
  public Collection<BooleanFormula> buildSuccessorFormulas(
      BooleanFormula pPredecessor, int pLastReachedIndex, CFAEdge pEdge) {
    throw new UnsupportedOperationException(
        "Location wise formula construction not supported by " + this.getClass().getSimpleName());
  }

  @Override
  public final Collection<BooleanFormula> buildSuccessorFormulas(
      BooleanFormula pPredecessor, int pLastReachedIndex) {
    var automatonFormulas =
        from(automata.getAllAutomata())
            .transform(automaton -> makeSuccessorFormulaForAutomaton(automaton, pLastReachedIndex))
            .toSet();
    var automataFormula = bFmgr.and(automatonFormulas);
    var extensionsFormula = extensions.makeStepFormula(pLastReachedIndex);
    var successorFormula = bFmgr.and(automataFormula, extensionsFormula);

    formulas.add(successorFormula);
    return ImmutableSet.of(bFmgr.makeTrue());
  }

  private final BooleanFormula makeSuccessorFormulaForAutomaton(
      TaDeclaration pAutomaton, int pLastReachedIndex) {
    var extensionsFormula = extensions.makeAutomatonStep(pAutomaton, pLastReachedIndex);
    var automatonTransitionsFormula =
        makeAutomatonTransitionsFormula(pAutomaton, pLastReachedIndex);
    return bFmgr.and(automatonTransitionsFormula, extensionsFormula);
  }

  protected abstract BooleanFormula makeAutomatonTransitionsFormula(
      TaDeclaration pAutomaton, int pLastReachedIndex);

  protected final BooleanFormula makeDiscreteStep(
      TaDeclaration pAutomaton, int pLastReachedIndex, TCFAEdge pEdge) {
    var guardFormula =
        pEdge
            .getGuard()
            .transform(guard -> clocks.makeConditionFormula(pAutomaton, pLastReachedIndex, guard))
            .or(bFmgr.makeTrue());

    var clockResets =
        clocks.makeEqualsZeroFormula(
            pAutomaton, pLastReachedIndex + 1, pEdge.getVariablesToReset(), true);
    var unChangedVariables =
        Sets.difference(
            ImmutableSet.copyOf(automata.getClocksByAutomaton(pAutomaton)),
            pEdge.getVariablesToReset());
    var clocksUnchanged =
        clocks.makeVariablesDoNotChangeFormula(pAutomaton, pLastReachedIndex, unChangedVariables);
    var clockFormulas = bFmgr.and(clockResets, clocksUnchanged);

    var predecessor = (TCFANode) pEdge.getPredecessor();
    var successor = (TCFANode) pEdge.getSuccessor();
    var locationBefore =
        locations.makeLocationEqualsFormula(pAutomaton, pLastReachedIndex, predecessor);
    var locationAfter =
        locations.makeLocationEqualsFormula(pAutomaton, pLastReachedIndex + 1, successor);

    var extensionsFormula = extensions.makeDiscreteStep(pAutomaton, pLastReachedIndex, pEdge);

    return bFmgr.and(
        guardFormula,
        clockFormulas,
        locationBefore,
        locationAfter,
        extensionsFormula);
  }

  protected final BooleanFormula makeDelayTransition(
      TaDeclaration pAutomaton, int pLastReachedIndex) {
    var locationDoesNotChange = locations.makeDoesNotChangeFormula(pAutomaton, pLastReachedIndex);
    var timeUpdate = clocks.makeTimeElapseFormula(pAutomaton, pLastReachedIndex);
    var extensionsFormula = extensions.makeDelayTransition(pAutomaton, pLastReachedIndex);
    return bFmgr.and(locationDoesNotChange, timeUpdate, extensionsFormula);
  }

  protected final BooleanFormula makeIdleTransition(
      TaDeclaration pAutomaton, int pLastReachedIndex) {
    var locationDoesNotChange = locations.makeDoesNotChangeFormula(pAutomaton, pLastReachedIndex);
    var clockVariables = automata.getClocksByAutomaton(pAutomaton);
    var clocksDoNotChange =
        clocks.makeVariablesDoNotChangeFormula(pAutomaton, pLastReachedIndex, clockVariables);
    var extensionsFormula = extensions.makeIdleTransition(pAutomaton, pLastReachedIndex);
    return bFmgr.and(locationDoesNotChange, clocksDoNotChange, extensionsFormula);
  }

  @Override
  public BooleanFormula getInitialFormula(CFANode pInitialNode, int pInitialIndex) {
    var initialFormulas =
        from(automata.getAllAutomata())
            .transform(automaton -> makeInitialFormulaForAutomaton(automaton, pInitialIndex));

    var initialFormula = bFmgr.and(initialFormulas.toSet());
    formulas.add(initialFormula);
    return initialFormula;
  }

  private final BooleanFormula makeInitialFormulaForAutomaton(
      TaDeclaration pAutomaton, int pInitialIndex) {
    var initialLocationFormulas =
        from(automata.getInitialNodesByAutomaton(pAutomaton))
            .transform(
                node -> locations.makeLocationEqualsFormula(pAutomaton, pInitialIndex, node));
    var initialLocationsFormula = bFmgr.or(initialLocationFormulas.toSet());
    var initialVariables =
        clocks.makeEqualsZeroFormula(
            pAutomaton, pInitialIndex, automata.getClocksByAutomaton(pAutomaton), true);
    var initialTime = clocks.makeTimeEqualsZeroFormula(pAutomaton, pInitialIndex);
    var extensionsFormula = extensions.makeInitialFormula(pAutomaton, pInitialIndex);
    return bFmgr.and(initialTime, initialLocationsFormula, extensionsFormula, initialVariables);
  }

  @Override
  public Collection<BooleanFormula> getFormulaFromReachedSet(Iterable<AbstractState> pReachedSet) {
    var unrollingStates =
        from(pReachedSet)
            .transform(aState -> AbstractStates.extractStateByType(aState, TAUnrollingState.class));
    var maxUnrolling =
        unrollingStates.transform(TAUnrollingState::getStepCount).stream()
            .collect(Collectors.maxBy(Integer::compareTo))
            .orElseThrow();

    var resultFormulas = new ArrayList<BooleanFormula>();
    while (!formulas.isEmpty()) {
      resultFormulas.add(formulas.remove());
    }

    var finalConditions =
        from(automata.getAllAutomata())
            .transform(automaton -> makeFinalConditionForAutomaton(automaton, maxUnrolling));
    var finalCondition = bFmgr.and(finalConditions.toSet());

    resultFormulas.add(finalCondition);
    return resultFormulas;
  }

  private final BooleanFormula makeFinalConditionForAutomaton(
      TaDeclaration pAutomaton, int pMaxUnrolling) {
    var targetLocations =
        from(automata.getNodesByAutomaton(pAutomaton)).filter(TCFANode::isTargetLocation).toSet();

    var anyTargetLocationReached = bFmgr.makeTrue();
    if (!targetLocations.isEmpty()) {
      var targetLocationFormulas =
          from(targetLocations)
              .transform(
                  node -> locations.makeLocationEqualsFormula(pAutomaton, pMaxUnrolling, node));
      anyTargetLocationReached = bFmgr.or(targetLocationFormulas.toSet());
    }

    var extensionsFormula = extensions.makeFinalConditionForAutomaton(pAutomaton, pMaxUnrolling);

    return bFmgr.and(anyTargetLocationReached, extensionsFormula);
  }
}
