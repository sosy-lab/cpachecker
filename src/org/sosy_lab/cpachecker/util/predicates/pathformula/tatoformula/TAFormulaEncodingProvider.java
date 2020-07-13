// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula;

import java.util.ArrayList;
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

// @Options(prefix = "cpa.timedautomata")
public class TAFormulaEncodingProvider {
  private final TaEncodingOptions options;
  private final CFA cfa;

  public TAFormulaEncodingProvider(CFA pCfa, TaEncodingOptions pOptions
      /* Configuration config */ ) { // throws InvalidConfigurationException {
    // config.inject(this, TAFormulaEncodingProvider.class);
    // config.toString();
    cfa = pCfa;
    options = pOptions;
  }

  public TAFormulaEncoding createConfiguredEncoding(FormulaManagerView pFmgr) {
    var automatonView = new TimedAutomatonView(cfa, options);
    var locations = createLocationEncoding(pFmgr, automatonView);
    var actions = createActionEncoding(pFmgr, automatonView);
    var time = createTimeEncoding(pFmgr);

    var extensions = createExtensions(pFmgr, automatonView, time, locations, actions);
    var automatonEncoding = createEncoding(pFmgr, automatonView, time, locations, extensions);

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
      result.add(new TaActionSynchronization(pFmgr, pAutomata, pActions));
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
