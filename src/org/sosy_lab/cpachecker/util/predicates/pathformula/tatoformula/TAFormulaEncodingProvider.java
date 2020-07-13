// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula;

import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.CFA;
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
  private static enum TAEncodingExtension {
    SHALLOW_SYNC,
    INVARIANTS,
    ACTION_SYNC
  }

  private List<TAEncodingExtension> encodingExtensions =
      Lists.newArrayList(TAEncodingExtension.INVARIANTS);

  private static enum DiscreteEncodingType {
    LOCAL_ID,
    GLOBAL_ID,
    BOOLEAN_VAR
  }

  private DiscreteEncodingType locationEncoding = DiscreteEncodingType.LOCAL_ID;
  private DiscreteEncodingType actionEncoding = DiscreteEncodingType.GLOBAL_ID;

  private static enum TimeEncodingType {
    GLOBAL_IMPLICIT,
    GLOBAL_EXPLICIT,
    LOCAL_EXPLICIT,
  }

  private TimeEncodingType timeEncoding = TimeEncodingType.GLOBAL_IMPLICIT;

  private static enum AutomatonEncodingType {
    TRANSITION_UNROLLING,
    LOCATION_UNROLLING,
    CONSTRAINT_UNROLLING
  }

  private AutomatonEncodingType automatonEncodingType = AutomatonEncodingType.CONSTRAINT_UNROLLING;

  // SpecialVariableType
  // LOCAL, GLOBAL, NONE
  // idleType, delayType -> passed to TAView

  private final CFA cfa;

  public TAFormulaEncodingProvider(CFA pCfa
      /* Configuration config */ ) { // throws InvalidConfigurationException {
    // config.inject(this, TAFormulaEncodingProvider.class);
    // config.toString();
    cfa = pCfa;
  }

  public TAFormulaEncoding createConfiguredEncoding(FormulaManagerView pFmgr) {
    var automatonView = new TimedAutomatonView(cfa);
    var locations = createLocationEncoding(pFmgr, automatonView);
    var actions = createActionEncoding(pFmgr, automatonView);
    var time = createTimeEncoding(pFmgr);

    var extensions = createExtensions(pFmgr, automatonView, time, locations, actions);
    var automatonEncoding = createEncoding(pFmgr, automatonView, time, locations, extensions);

    return automatonEncoding;
  }

  private LocationEncoding createLocationEncoding(
      FormulaManagerView pFmgr, TimedAutomatonView pAutomata) {
    switch (locationEncoding) {
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
    switch (actionEncoding) {
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
    if (timeEncoding == TimeEncodingType.GLOBAL_EXPLICIT) {
      return new GlobalImplicitTimeEncoding(pFmgr);
    }
    if (timeEncoding == TimeEncodingType.GLOBAL_IMPLICIT) {
      return new ExplicitTimeEncoding(pFmgr, false);
    }
    if (timeEncoding == TimeEncodingType.LOCAL_EXPLICIT) {
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
    var result = new ArrayList<EncodingExtension>(encodingExtensions.size());
    if (encodingExtensions.contains(TAEncodingExtension.SHALLOW_SYNC)) {
      if (!(pTime instanceof ExplicitTimeEncoding)) {
        throw new AssertionError("Shallow sync is only possible with explicit time encoding");
      }
      result.add(new ShallowSyncEncoding(pFmgr, pAutomata, (ExplicitTimeEncoding) pTime, pActions));
    }
    if (encodingExtensions.contains(TAEncodingExtension.INVARIANTS)) {
      result.add(new TaInvariants(pFmgr, pAutomata, pTime, pLocations));
    }
    if (encodingExtensions.contains(TAEncodingExtension.ACTION_SYNC)) {
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
    if (automatonEncodingType == AutomatonEncodingType.TRANSITION_UNROLLING) {
      return new TATransitionUnrolling(pFmgr, pAutomata, pTime, pLocations, pExtensions);
    }
    if (automatonEncodingType == AutomatonEncodingType.LOCATION_UNROLLING) {
      return new TALocationUnrolling(pFmgr, pAutomata, pTime, pLocations, pExtensions);
    }
    if (automatonEncodingType == AutomatonEncodingType.CONSTRAINT_UNROLLING) {
      return new TAConstraintUnrolling(pFmgr, pAutomata, pTime, pLocations, pExtensions);
    }

    throw new AssertionError("Unknown encoding type");
  }
}
