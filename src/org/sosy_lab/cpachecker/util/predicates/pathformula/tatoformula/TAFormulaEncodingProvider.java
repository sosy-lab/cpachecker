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
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TaEncodingOptions.AutomatonEncodingType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TaEncodingOptions.TAEncodingExtension;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.TaEncodingOptions.TimeEncodingType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions.EncodingExtension;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions.ShallowSyncEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions.TaActionSynchronization;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.extensions.TaInvariants;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.actionencodings.ActionEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.actionencodings.BooleanVarActionEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.actionencodings.GlobalVarActionEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.actionencodings.LocalVarActionEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.locationencodings.BooleanVarLocationEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.locationencodings.LocalVarLocationEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.locationencodings.LocationEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.timeencodings.ExplicitTimeEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.timeencodings.GlobalImplicitTimeEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.timeencodings.TimeEncoding;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

public class TAFormulaEncodingProvider {
  private final TaEncodingOptions options;
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
    options = new TaEncodingOptions();
    config.inject(options, TaEncodingOptions.class);
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
    var automatonEncoding = createEncoding(fmgr, automatonView, time, locations, extensions);

    return automatonEncoding;
  }

  private LocationEncoding createLocationEncoding(
      FormulaManagerView pFmgr, TimedAutomatonView pAutomata) {
    switch (options.locationEncoding) {
      case LOCAL_ID:
        return new LocalVarLocationEncoding(pFmgr, pAutomata);
      case BOOLEAN_VAR:
        return new BooleanVarLocationEncoding(pFmgr, pAutomata);
      default:
        throw new AssertionError("Location encoding not supported");
    }
  }

  private ActionEncoding createActionEncoding(
      FormulaManagerView pFmgr, TimedAutomatonView pAutomata) {
    switch (options.actionEncoding) {
      case LOCAL_ID:
        return new LocalVarActionEncoding(pFmgr, pAutomata);
      case GLOBAL_ID:
        return new GlobalVarActionEncoding(pFmgr, pAutomata);
      case BOOLEAN_VAR:
        return new BooleanVarActionEncoding(pFmgr, pAutomata);
      default:
        throw new AssertionError("Action encoding not supported");
    }
  }

  private TimeEncoding createTimeEncoding(FormulaManagerView pFmgr) {
    if (options.timeEncoding == TimeEncodingType.GLOBAL_EXPLICIT) {
      return new GlobalImplicitTimeEncoding(pFmgr);
    }
    if (options.timeEncoding == TimeEncodingType.GLOBAL_IMPLICIT) {
      return new ExplicitTimeEncoding(pFmgr, false);
    }
    if (options.timeEncoding == TimeEncodingType.LOCAL_EXPLICIT) {
      return new ExplicitTimeEncoding(pFmgr, true);
    }
    throw new AssertionError("Unknown encoding type");
  }

  private Iterable<EncodingExtension> createExtensions(
      FormulaManagerView pFmgr,
      TimedAutomatonView pAutomata,
      TimeEncoding pTime,
      LocationEncoding pLocations,
      ActionEncoding pActions) {
    var result = new ArrayList<EncodingExtension>(options.encodingExtensions.size());
    if (options.encodingExtensions.contains(TAEncodingExtension.SHALLOW_SYNC)) {
      if (!(pTime instanceof ExplicitTimeEncoding)) {
        throw new AssertionError("Shallow sync is only possible with explicit time encoding");
      }
      result.add(new ShallowSyncEncoding(pFmgr, pAutomata, (ExplicitTimeEncoding) pTime, pActions));
    }
    if (options.encodingExtensions.contains(TAEncodingExtension.INVARIANTS)) {
      result.add(new TaInvariants(pFmgr, pAutomata, pTime, pLocations));
    }
    if (options.encodingExtensions.contains(TAEncodingExtension.ACTION_SYNC)) {
      result.add(
          new TaActionSynchronization(
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
      TimeEncoding pTime,
      LocationEncoding pLocations,
      Iterable<EncodingExtension> pExtensions) {
    if (options.automatonEncodingType == AutomatonEncodingType.TRANSITION_UNROLLING) {
      return new TATransitionUnrolling(pFmgr, pAutomata, pTime, pLocations, pExtensions);
    }
    if (options.automatonEncodingType == AutomatonEncodingType.LOCATION_UNROLLING) {
      return new TALocationUnrolling(pFmgr, pAutomata, pTime, pLocations, pExtensions);
    }
    if (options.automatonEncodingType == AutomatonEncodingType.CONSTRAINT_UNROLLING) {
      return new TAConstraintUnrolling(pFmgr, pAutomata, pTime, pLocations, pExtensions);
    }

    throw new AssertionError("Unknown encoding type");
  }
}
