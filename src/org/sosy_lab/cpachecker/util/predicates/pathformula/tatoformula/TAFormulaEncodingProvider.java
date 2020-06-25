// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.tatoformula;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

@Options(prefix = "cpa.timedautomata")
public class TAFormulaEncodingProvider {
  private static enum Encoding {
    SHALLOW_SYNC_PATH,
    ALWAYS_TRUE,
    TRANSITION_RELATION
  }

  @Option(
      secure = true,
      description = "Amount by which to increase the maximum unrolling step bound.")
  private Encoding encoding = Encoding.ALWAYS_TRUE;

  public TAFormulaEncodingProvider(Configuration config) throws InvalidConfigurationException {
    config.inject(this, TAFormulaEncodingProvider.class);
  }

  public TAFormulaEncoding createConfiguredEncoding(CFA pCFA, FormulaManagerView pFmgr) {
    switch (encoding) {
      case SHALLOW_SYNC_PATH:
        return new ShallowSyncPathEncoding(pFmgr, pCFA);
      case TRANSITION_RELATION:
        return new TransitionRelationEncoding(pFmgr, pCFA);
      case ALWAYS_TRUE:
        return new AlwaysTrueEncoding(pFmgr);
      default:
        throw new AssertionError("Unknown timed automaton encoding");
    }
  }
}
