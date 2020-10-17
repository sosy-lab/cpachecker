// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.timedautomata.TaVariable;
import org.sosy_lab.cpachecker.cfa.model.timedautomata.TCFANode;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TAEncodingOptions.AutomatonEncodingType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TAEncodingOptions.InvariantType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TAEncodingOptions.TAEncodingExtensionType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TAEncodingOptions.TimeEncodingType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.encodings.TAConstraintUnrolling;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.encodings.TAFormulaEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.encodings.TALocationUnrolling;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.encodings.TATransitionUnrolling;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions.TAEncodingExtension;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions.TAEncodingExtensionWrapper;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions.TAInvariants;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions.TALocalMutexActions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions.TANoConsequentDelays;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions.TANoConsequentDiscretes;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions.TAOnlyFinalIdles;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions.TAShallowStrictSync;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions.TAShallowSync;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions.TAShallowSyncDifference;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions.TAShallowSyncTimeStamp;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions.TATransitionActions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions.TATransitionTypes;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions.TAUnsyncMutexActions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.TABooleanVarFeatureEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.TAGlobalVarDiscreteFeatureEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.TALocalVarDiscreteFeatureEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.actions.TAActions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.actions.TABooleanVarActions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.actions.TAGlobalVarActions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.actions.TALocalVarActions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.locations.TABooleanVarLocations;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.locations.TALocalVarLocations;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.locations.TALocations;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.variables.TAExplicitDifferenceTime;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.variables.TAExplicitTime;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.variables.TAImplicitTime;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.variables.TAVariables;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.FormulaType;

public class TAFormulaEncodingProvider {
  private final TAEncodingOptions options;
  private final CFA cfa;
  private final FormulaManagerView fmgr;
  private final TAFormulaEncoding encoding;

  private static TAFormulaEncodingProvider instance;

  public static TAFormulaEncoding getEncoding(
      Configuration config, CFA pCfa, FormulaManagerView pFmgr)
      throws InvalidConfigurationException {
    if (instance == null) {
      instance = new TAFormulaEncodingProvider(config, pCfa, pFmgr);
    }

    return instance.encoding;
  }

  private TAFormulaEncodingProvider(Configuration config, CFA pCfa, FormulaManagerView pFmgr)
      throws InvalidConfigurationException {
    options = new TAEncodingOptions();
    config.inject(options, TAEncodingOptions.class);
    cfa = pCfa;
    fmgr = pFmgr;

    encoding = createConfiguredEncoding();
  }

  private TAFormulaEncoding createConfiguredEncoding() {
    var automatonView = new TimedAutomatonView(cfa, options);
    var locations = createLocationEncoding(fmgr, automatonView);
    var actions = createActionEncoding(fmgr, automatonView);
    var time = createTimeEncoding(fmgr, automatonView);

    var extensions = createExtensions(fmgr, automatonView, time, locations, actions);
    var extensionsWrapper = new TAEncodingExtensionWrapper(fmgr, extensions);
    var automatonEncoding = createEncoding(fmgr, automatonView, time, locations, extensionsWrapper);

    return automatonEncoding;
  }

  private TALocations createLocationEncoding(
      FormulaManagerView pFmgr, TimedAutomatonView pAutomata) {
    switch (options.locationEncoding) {
      case LOCAL_ID:
        return createTaLocalVarLocations(pFmgr, pAutomata);
      case BOOLEAN_VAR:
        return createTABooleanVarLocations(pFmgr, pAutomata);
      default:
        throw new AssertionError("Location encoding not supported");
    }
  }

  private TALocalVarLocations createTaLocalVarLocations(
      FormulaManagerView pFmgr, TimedAutomatonView pAutomata) {
    var featureEncoding =
        new TALocalVarDiscreteFeatureEncoding<>(
            pFmgr, "location", ImmutableSet.copyOf(pAutomata.getAllNodes()));
    return new TALocalVarLocations(featureEncoding);
  }

  private TABooleanVarLocations createTABooleanVarLocations(
      FormulaManagerView pFmgr, TimedAutomatonView pAutomata) {
    var variableNames = new HashMap<TCFANode, String>();
    var elementsByAutomaton = new HashMap<TaDeclaration, Collection<TCFANode>>();
    pAutomata
        .getAllNodes()
        .forEach(
            location -> {
              var automaton = location.getAutomatonDeclaration();
              variableNames.put(location, location.getName());
              elementsByAutomaton.computeIfAbsent(automaton, a -> new HashSet<>());
              elementsByAutomaton.get(automaton).add(location);
            });

    var featureEncoding =
        new TABooleanVarFeatureEncoding<>(pFmgr, variableNames, elementsByAutomaton);
    return new TABooleanVarLocations(featureEncoding);
  }

  private TAActions createActionEncoding(FormulaManagerView pFmgr, TimedAutomatonView pAutomata) {
    switch (options.actionEncoding) {
      case LOCAL_ID:
        return createTALocalVarActions(pFmgr, pAutomata);
      case GLOBAL_ID:
        return createTAGlobalVarActions(pFmgr, pAutomata);
      case BOOLEAN_VAR:
        return createTABooleanVarActions(pFmgr, pAutomata);
      default:
        throw new AssertionError("Action encoding not supported");
    }
  }

  private TALocalVarActions createTALocalVarActions(
      FormulaManagerView pFmgr, TimedAutomatonView pAutomata) {
    var featureEncoding =
        new TALocalVarDiscreteFeatureEncoding<>(
            pFmgr, "action", ImmutableSet.copyOf(pAutomata.getAllActions()));
    return new TALocalVarActions(pFmgr, pAutomata, featureEncoding);
  }

  private TAGlobalVarActions createTAGlobalVarActions(
      FormulaManagerView pFmgr, TimedAutomatonView pAutomata) {
    var featureEncoding =
        new TAGlobalVarDiscreteFeatureEncoding<>(
            pFmgr, "global#action", ImmutableSet.copyOf(pAutomata.getAllActions()));
    return new TAGlobalVarActions(featureEncoding);
  }

  private TABooleanVarActions createTABooleanVarActions(
      FormulaManagerView pFmgr, TimedAutomatonView pAutomata) {
    var variableNames = new HashMap<TaVariable, String>();
    var elementsByAutomaton = new HashMap<TaDeclaration, Collection<TaVariable>>();
    for (var automaton : pAutomata.getAllAutomata()) {
      pAutomata
          .getActionsByAutomaton(automaton)
          .forEach(
              action -> {
                variableNames.put(action, action.getName());
                elementsByAutomaton.computeIfAbsent(automaton, a -> new HashSet<>());
                elementsByAutomaton.get(automaton).add(action);
              });
    }

    var featureEncoding =
        new TABooleanVarFeatureEncoding<>(pFmgr, variableNames, elementsByAutomaton);
    return new TABooleanVarActions(featureEncoding);
  }

  private TAVariables createTimeEncoding(FormulaManagerView pFmgr, TimedAutomatonView pAutomata) {
    var clockClockVariableType = getClockVariableType();

    if (options.timeEncoding == TimeEncodingType.GLOBAL_EXPLICIT) {
      return new TAExplicitTime(
          pFmgr, false, options.allowZeroDelay, clockClockVariableType, pAutomata);
    }
    if (options.timeEncoding == TimeEncodingType.GLOBAL_EXPLICIT_DIFFERENCE) {
      return new TAExplicitDifferenceTime(
          pFmgr, false, options.allowZeroDelay, clockClockVariableType, pAutomata);
    }
    if (options.timeEncoding == TimeEncodingType.GLOBAL_IMPLICIT) {
      return new TAImplicitTime(
          pFmgr, false, options.allowZeroDelay, clockClockVariableType, pAutomata);
    }
    if (options.timeEncoding == TimeEncodingType.LOCAL_IMPLICIT) {
      return new TAImplicitTime(
          pFmgr, true, options.allowZeroDelay, clockClockVariableType, pAutomata);
    }
    if (options.timeEncoding == TimeEncodingType.LOCAL_EXPLICIT) {
      return new TAExplicitTime(
          pFmgr, true, options.allowZeroDelay, clockClockVariableType, pAutomata);
    }
    if (options.timeEncoding == TimeEncodingType.LOCAL_EXPLICIT_DIFFERENCE) {
      return new TAExplicitDifferenceTime(
          pFmgr, true, options.allowZeroDelay, clockClockVariableType, pAutomata);
    }
    throw new AssertionError("Unknown encoding type");
  }

  private FormulaType<?> getClockVariableType() {
    switch (options.clockVariableType) {
      case BITVECTOR:
        {
          return FormulaType.getBitvectorTypeWithSize(options.bitVectorClockVariableSize);
        }
      case INTEGER:
        {
          return FormulaType.IntegerType;
        }
      case RATIONAL:
        {
          return FormulaType.RationalType;
        }
      default:
        {
          throw new AssertionError("Unknown clock variable type");
        }
    }
  }

  private Iterable<TAEncodingExtension> createExtensions(
      FormulaManagerView pFmgr,
      TimedAutomatonView pAutomata,
      TAVariables pTime,
      TALocations pLocations,
      TAActions pActions) {
    var result = new ArrayList<TAEncodingExtension>(options.encodingExtensions.size());
    if (options.encodingExtensions.contains(TAEncodingExtensionType.SHALLOW_SYNC)) {
      result.add(new TAShallowSync(pFmgr, pAutomata, (TAExplicitTime) pTime, pActions));
    }
    if (options.encodingExtensions.contains(TAEncodingExtensionType.SHALLOW_SYNC_TIMESTAMP)) {
      result.add(new TAShallowSyncTimeStamp(pFmgr, pAutomata, pTime, pActions));
    }
    if (options.encodingExtensions.contains(TAEncodingExtensionType.SHALLOW_SYNC_DIFFERENCE)) {
      result.add(
          new TAShallowSyncDifference(
              pFmgr, pAutomata, pTime, pActions, options.carryOverCounterValue));
    }
    if (options.encodingExtensions.contains(TAEncodingExtensionType.INVARIANTS)) {
      result.add(
          new TAInvariants(
              pFmgr, pAutomata, pTime, pLocations, options.invariantType == InvariantType.LOCAL));
    }
    if (options.encodingExtensions.contains(TAEncodingExtensionType.TRANSITION_ACTIONS)) {
      result.add(
          new TATransitionActions(
              pFmgr, pAutomata, pActions, options.actionDetachedDelay, options.actionDetachedIdle));
    }
    if (options.encodingExtensions.contains(TAEncodingExtensionType.UNSYNC_MUTEX_ACTIONS)) {
      result.add(new TAUnsyncMutexActions(pFmgr, pAutomata, pActions));
    }
    if (options.encodingExtensions.contains(TAEncodingExtensionType.LOCAL_MUTEX_ACTIONS)) {
      result.add(new TALocalMutexActions(pFmgr, pAutomata, pActions));
    }
    if (options.encodingExtensions.contains(TAEncodingExtensionType.SHALLOW_STRICT)) {
      result.add(new TAShallowStrictSync(pFmgr, pAutomata, pActions, false));
    }
    if (options.encodingExtensions.contains(TAEncodingExtensionType.SHALLOW_MULTISTEP)) {
      result.add(new TAShallowStrictSync(pFmgr, pAutomata, pActions, true));
    }
    if (options.encodingExtensions.contains(TAEncodingExtensionType.TRANSITION_TYPES)) {
      result.add(new TATransitionTypes(pFmgr));
    }
    if (options.encodingExtensions.contains(TAEncodingExtensionType.NO_CONSEQUENT_DELAYS)) {
      result.add(new TANoConsequentDelays(pFmgr, false));
    }
    if (options.encodingExtensions.contains(TAEncodingExtensionType.NO_CONSEQUENT_DELAYS_LOCAL)) {
      result.add(new TANoConsequentDelays(pFmgr, true));
    }
    if (options.encodingExtensions.contains(TAEncodingExtensionType.NO_CONSEQUENT_DISCRETES)) {
      result.add(new TANoConsequentDiscretes(pFmgr, false));
    }
    if (options.encodingExtensions.contains(
        TAEncodingExtensionType.NO_CONSEQUENT_DISCRETES_LOCAL)) {
      result.add(new TANoConsequentDiscretes(pFmgr, true));
    }
    if (options.encodingExtensions.contains(TAEncodingExtensionType.ONLY_FINAL_IDLES)) {
      result.add(new TAOnlyFinalIdles(pFmgr));
    }
    return result;
  }

  private TAFormulaEncoding createEncoding(
      FormulaManagerView pFmgr,
      TimedAutomatonView pAutomata,
      TAVariables pTime,
      TALocations pLocations,
      TAEncodingExtension pExtension) {
    if (options.automatonEncodingType == AutomatonEncodingType.TRANSITION_UNROLLING) {
      return new TATransitionUnrolling(pFmgr, pAutomata, pTime, pLocations, pExtension);
    }
    if (options.automatonEncodingType == AutomatonEncodingType.LOCATION_UNROLLING) {
      return new TALocationUnrolling(pFmgr, pAutomata, pTime, pLocations, pExtension);
    }
    if (options.automatonEncodingType == AutomatonEncodingType.CONSTRAINT_UNROLLING) {
      return new TAConstraintUnrolling(pFmgr, pAutomata, pTime, pLocations, pExtension);
    }

    throw new AssertionError("Unknown encoding type");
  }
}
