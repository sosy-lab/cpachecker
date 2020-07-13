// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.stream.Collectors;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFAEdge;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFANode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.cpa.timedautomata.TAUnrollingState;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions.EncodingExtension;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.locationencodings.LocationEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.timeencodings.TimeEncoding;
import org.sosy_lab.cpachecker.util.predicates.smt.BooleanFormulaManagerView;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public abstract class AutomatonEncoding implements TAFormulaEncoding {
  protected final FormulaManagerView fmgr;
  protected final BooleanFormulaManagerView bFmgr;
  protected final LocationEncoding locations;
  private final TimeEncoding time;
  protected final TimedAutomatonView automata;
  private Iterable<EncodingExtension> extensions;

  public AutomatonEncoding(
      FormulaManagerView pFmgr,
      TimedAutomatonView pAutomata,
      TimeEncoding pTime,
      LocationEncoding pLocations,
      Iterable<EncodingExtension> pExtensions) {
    fmgr = pFmgr;
    bFmgr = fmgr.getBooleanFormulaManager();
    automata = pAutomata;
    time = pTime;
    locations = pLocations;
    extensions = pExtensions;
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
    var result = bFmgr.makeTrue();

    var automataFormulas =
        from(automata.getAllAutomata())
            .transform(automaton -> makeSuccessorFormulaForAutomaton(automaton, pLastReachedIndex))
            .toSet();
    result = bFmgr.and(bFmgr.and(automataFormulas), result);

    var extensionFormulas =
        from(extensions).transform(extension -> extension.makeStepFormula(pLastReachedIndex));
    var extensionsFormula = bFmgr.and(extensionFormulas.toSet());
    result = bFmgr.and(extensionsFormula, result);

    return ImmutableSet.of(result);
  }

  private final BooleanFormula makeSuccessorFormulaForAutomaton(
      TaDeclaration pAutomaton, int pLastReachedIndex) {
    var extensionFormulas =
        from(extensions)
            .transform(extension -> extension.makeAutomatonStep(pAutomaton, pLastReachedIndex));
    var extensionsFormula = bFmgr.and(extensionFormulas.toSet());
    var automatonTransitionsFormula =
        makeAutomatonTransitionsFormula(pAutomaton, pLastReachedIndex);
    return bFmgr.and(automatonTransitionsFormula, extensionsFormula);
  }

  protected abstract BooleanFormula makeAutomatonTransitionsFormula(
      TaDeclaration pAutomaton, int pLastReachedIndex);

  protected BooleanFormula makeDiscreteStep(
      TaDeclaration pAutomaton, int pLastReachedIndex, TCFAEdge pEdge) {
    var guardFormula =
        pEdge
            .getGuard()
            .transform(guard -> time.makeConditionFormula(pAutomaton, pLastReachedIndex, guard))
            .or(bFmgr.makeTrue());

    var clockResets =
        time.makeResetToZeroFormula(pAutomaton, pLastReachedIndex + 1, pEdge.getVariablesToReset());
    var clocksUnchanged =
        time.makeClockVariablesDoNotChangeFormula(
            pAutomaton,
            pLastReachedIndex,
            Sets.difference(
                ImmutableSet.copyOf(automata.getClocksByAutomaton(pAutomaton)),
                pEdge.getVariablesToReset()));
    var clockFormulas = bFmgr.and(clockResets, clocksUnchanged);

    var locationBefore =
        locations.makeLocationEqualsFormula(
            pAutomaton, pLastReachedIndex, (TCFANode) pEdge.getPredecessor());
    var locationAfter =
        locations.makeLocationEqualsFormula(
            pAutomaton, pLastReachedIndex + 1, (TCFANode) pEdge.getSuccessor());

    var timeDoesNotAdvance = time.makeTimeDoesNotAdvanceFormula(pAutomaton, pLastReachedIndex);

    var extensionFormulas =
        from(extensions)
            .transform(
                extension -> extension.makeDiscreteStep(pAutomaton, pLastReachedIndex, pEdge));
    var extensionsFormula = bFmgr.and(extensionFormulas.toSet());

    return bFmgr.and(
        guardFormula,
        clockFormulas,
        locationBefore,
        locationAfter,
        timeDoesNotAdvance,
        extensionsFormula);
  }

  protected BooleanFormula makeDelayTransition(TaDeclaration pAutomaton, int pLastReachedIndex) {
    var result = bFmgr.makeTrue();
    result = bFmgr.and(locations.makeDoesNotChangeFormula(pAutomaton, pLastReachedIndex), result);
    result = bFmgr.and(time.makeTimeUpdateFormula(pAutomaton, pLastReachedIndex), result);

    var extensionFormulas =
        from(extensions)
            .transform(extension -> extension.makeDelayTransition(pAutomaton, pLastReachedIndex));
    var extensionsFormula = bFmgr.and(extensionFormulas.toSet());
    result = bFmgr.and(extensionsFormula, result);

    return result;
  }

  protected BooleanFormula makeIdleTransition(TaDeclaration pAutomaton, int pLastReachedIndex) {
    var result = bFmgr.makeTrue();
    result = bFmgr.and(locations.makeDoesNotChangeFormula(pAutomaton, pLastReachedIndex), result);
    result = bFmgr.and(time.makeTimeDoesNotAdvanceFormula(pAutomaton, pLastReachedIndex), result);
    result =
        bFmgr.and(
            time.makeClockVariablesDoNotChangeFormula(
                pAutomaton, pLastReachedIndex, automata.getClocksByAutomaton(pAutomaton)),
            result);

    var extensionFormulas =
        from(extensions)
            .transform(extension -> extension.makeIdleTransition(pAutomaton, pLastReachedIndex));
    var extensionsFormula = bFmgr.and(extensionFormulas.toSet());
    result = bFmgr.and(extensionsFormula, result);

    return result;
  }

  @Override
  public BooleanFormula getInitialFormula(CFANode pInitialNode, int pInitialIndex) {
    var encodingResults =
        from(automata.getAllAutomata())
            .transform(automaton -> makeInitialFormulaForAutomaton(automaton, pInitialIndex));
    return bFmgr.and(encodingResults.toList());
  }

  private BooleanFormula makeInitialFormulaForAutomaton(
      TaDeclaration pAutomaton, int pInitialIndex) {
    var initialLocations =
        from(automata.getInitialNodesByAutomaton(pAutomaton))
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
        from(automata.getAllAutomata())
            .transform(automaton -> makeFinalConditionForAutomaton(automaton, pHighestReachedIndex))
            .toSet());
  }

  private BooleanFormula makeFinalConditionForAutomaton(
      TaDeclaration pAutomaton, int pHighestReachedIndex) {
    var errorNodes =
        from(automata.getNodesByAutomaton(pAutomaton)).filter(TCFANode::isErrorLocation).toSet();
    if (errorNodes.isEmpty()) {
      errorNodes = ImmutableSet.copyOf(automata.getNodesByAutomaton(pAutomaton));
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
