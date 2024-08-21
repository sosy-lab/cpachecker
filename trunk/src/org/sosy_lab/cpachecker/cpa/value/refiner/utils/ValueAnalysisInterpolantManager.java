// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.refiner.utils;

import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisInterpolant;
import org.sosy_lab.cpachecker.util.refinement.InterpolantManager;

/** InterpolantManager for interpolants of {@link ValueAnalysisState}. */
public class ValueAnalysisInterpolantManager
    implements InterpolantManager<ValueAnalysisState, ValueAnalysisInterpolant> {

  private static final ValueAnalysisInterpolantManager SINGLETON =
      new ValueAnalysisInterpolantManager();

  private ValueAnalysisInterpolantManager() {
    // DO NOTHING
  }

  public static ValueAnalysisInterpolantManager getInstance() {
    return SINGLETON;
  }

  @Override
  public ValueAnalysisInterpolant createInitialInterpolant() {
    return ValueAnalysisInterpolant.createInitial();
  }

  @Override
  public ValueAnalysisInterpolant createInterpolant(ValueAnalysisState state) {
    return state.createInterpolant();
  }

  @Override
  public ValueAnalysisInterpolant getTrueInterpolant() {
    return ValueAnalysisInterpolant.TRUE;
  }

  @Override
  public ValueAnalysisInterpolant getFalseInterpolant() {
    return ValueAnalysisInterpolant.FALSE;
  }
}
