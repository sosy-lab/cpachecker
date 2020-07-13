// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula;

import java.util.HashSet;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.actionencodings.ActionEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.actionencodings.BooleanVarActionEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.actionencodings.GlobalVarActionEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.actionencodings.LocalVarActionEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.locationencodings.BooleanVarLocationEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.locationencodings.LocalVarLocationEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.locationencodings.LocationEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.timeencodings.GlobalImplicitTimeEncoding;
import org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula.featureencodings.timeencodings.TimeEncoding;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

// @Options(prefix = "cpa.timedautomata")
public class TAFormulaEncodingProvider {

  public TAFormulaEncodingProvider(
      /* Configuration config */ ) { // throws InvalidConfigurationException {
    // config.inject(this, TAFormulaEncodingProvider.class);
    // config.toString();
  }

  private static enum DiscreteEncodingType {
    LOCAL_ID,
    GLOBAL_ID,
    BOOLEAN_VAR
  }

  // private static enum TimeEncodingType {
  //   GLOBAL_IMPLICIT
  // }

  // private static enum AutomatonEncodingType {
  //   CONSTRAINT_UNROLLING
  // }

  private DiscreteEncodingType locationEncoding = DiscreteEncodingType.LOCAL_ID;
  private DiscreteEncodingType actionEncoding = DiscreteEncodingType.GLOBAL_ID;
  // private TimeEncodingType timeEncoding = TimeEncodingType.GLOBAL_IMPLICIT;
  // private AutomatonEncodingType atuomatonEncoding = AutomatonEncodingType.CONSTRAINT_UNROLLING;

  public TAFormulaEncoding createConfiguredEncoding(CFA pCfa, FormulaManagerView pFmgr) {
    var locations = makeLocationEncoding(pCfa, pFmgr);
    var actions = makeActionEncoding(pCfa, pFmgr);
    var time = makeTimeEncoding(pFmgr);

    var automaton = new AutomatonEncoding(pFmgr, pCfa, time, actions, locations, new HashSet<>());

    return automaton;
  }

  private LocationEncoding makeLocationEncoding(CFA pCfa, FormulaManagerView pFmgr) {
    switch (locationEncoding) {
      case LOCAL_ID:
        return new LocalVarLocationEncoding(pFmgr, pCfa);
      case BOOLEAN_VAR:
        return new BooleanVarLocationEncoding(pFmgr, pCfa);
      default:
        throw new AssertionError("Location encoding not supported");
    }
  }

  private ActionEncoding makeActionEncoding(CFA pCfa, FormulaManagerView pFmgr) {
    switch (actionEncoding) {
      case LOCAL_ID:
        return new LocalVarActionEncoding(pFmgr, pCfa);
      case GLOBAL_ID:
        return new GlobalVarActionEncoding(pFmgr, pCfa);
      case BOOLEAN_VAR:
        return new BooleanVarActionEncoding(pFmgr, pCfa);
      default:
        throw new AssertionError("Action encoding not supported");
    }
  }

  private TimeEncoding makeTimeEncoding(FormulaManagerView pFmgr) {
    return new GlobalImplicitTimeEncoding(pFmgr);
  }
}
