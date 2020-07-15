// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula;

import java.util.ArrayList;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
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
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions.TAShallowSync;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions.TATransitionActions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.actions.TAActions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.actions.TABooleanVarActions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.actions.TAGlobalVarActions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.actions.TALocalVarActions;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.locations.TABooleanVarLocations;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.locations.TALocalVarLocations;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.locations.TALocations;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.time.TAExplicitTime;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.time.TAGlobalImplicitTime;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.time.TATime;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

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
    var time = createTimeEncoding(fmgr);

    var extensions = createExtensions(fmgr, automatonView, time, locations, actions);
    var extensionsWrapper = new TAEncodingExtensionWrapper(fmgr, extensions);
    var automatonEncoding = createEncoding(fmgr, automatonView, time, locations, extensionsWrapper);

    return automatonEncoding;
  }

  private TALocations createLocationEncoding(
      FormulaManagerView pFmgr, TimedAutomatonView pAutomata) {
    switch (options.locationEncoding) {
      case LOCAL_ID:
        return new TALocalVarLocations(pFmgr, pAutomata);
      case BOOLEAN_VAR:
        return new TABooleanVarLocations(pFmgr, pAutomata);
      default:
        throw new AssertionError("Location encoding not supported");
    }
  }

  private TAActions createActionEncoding(FormulaManagerView pFmgr, TimedAutomatonView pAutomata) {
    switch (options.actionEncoding) {
      case LOCAL_ID:
        return new TALocalVarActions(pFmgr, pAutomata);
      case GLOBAL_ID:
        return new TAGlobalVarActions(pFmgr, pAutomata);
      case BOOLEAN_VAR:
        return new TABooleanVarActions(pFmgr, pAutomata);
      default:
        throw new AssertionError("Action encoding not supported");
    }
  }

  private TATime createTimeEncoding(FormulaManagerView pFmgr) {
    if (options.timeEncoding == TimeEncodingType.GLOBAL_EXPLICIT) {
      return new TAExplicitTime(pFmgr, false);
    }
    if (options.timeEncoding == TimeEncodingType.GLOBAL_IMPLICIT) {
      return new TAGlobalImplicitTime(pFmgr);
    }
    if (options.timeEncoding == TimeEncodingType.LOCAL_EXPLICIT) {
      return new TAExplicitTime(pFmgr, true);
    }
    throw new AssertionError("Unknown encoding type");
  }

  private Iterable<TAEncodingExtension> createExtensions(
      FormulaManagerView pFmgr,
      TimedAutomatonView pAutomata,
      TATime pTime,
      TALocations pLocations,
      TAActions pActions) {
    var result = new ArrayList<TAEncodingExtension>(options.encodingExtensions.size());
    if (options.encodingExtensions.contains(TAEncodingExtensionType.SHALLOW_SYNC)) {
      if (!(pTime instanceof TAExplicitTime)) {
        throw new AssertionError("Shallow sync is only possible with explicit time encoding");
      }
      result.add(new TAShallowSync(pFmgr, pAutomata, (TAExplicitTime) pTime, pActions));
    }
    if (options.encodingExtensions.contains(TAEncodingExtensionType.INVARIANTS)) {
      result.add(
          new TAInvariants(
              pFmgr, pAutomata, pTime, pLocations, options.invariantType == InvariantType.LOCAL));
    }
    if (options.encodingExtensions.contains(TAEncodingExtensionType.TRANSITION_ACTIONS)) {
      result.add(
          new TATransitionActions(
              pFmgr,
              pAutomata,
              pActions,
              options.actionDetachedDelay,
              options.actionDetachedIdle,
              options.noTwoActions));
    }

    return result;
  }

  private TAFormulaEncoding createEncoding(
      FormulaManagerView pFmgr,
      TimedAutomatonView pAutomata,
      TATime pTime,
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
