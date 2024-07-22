// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.instrumentation;

import com.google.common.collect.ImmutableMap;

/**
 * Data structure that defines the required transformation of CFA. It is used in
 * InstrumentationOperatorAlgorithm and injects new transitions into an original CFA.
 */
public class InstrumentationAutomaton{
  private InstrumentationProperty instrumentationProperty;
  private ImmutableMap<String, String> liveVariablesAndTypes;

  enum InstrumentationProperty {
    TERMINATION,
    NOOVERFLOW
  }

  /**
   * @param pInstrumentationProperty temporary indication of which property is used in the
   *                                 transformation
   * @param pLiveVariablesAndTypes the mapping from variable names used, but not declared, in a loop
   *                              to their types
   */
  InstrumentationAutomaton(InstrumentationProperty pInstrumentationProperty,
                           ImmutableMap<String, String> pLiveVariablesAndTypes) {
    this.liveVariablesAndTypes = pLiveVariablesAndTypes;
  }
}
