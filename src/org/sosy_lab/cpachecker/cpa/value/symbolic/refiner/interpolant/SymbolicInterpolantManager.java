// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.interpolant;

import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.constraints.util.ConstraintsInformation;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.ForgettingCompositeState;
import org.sosy_lab.cpachecker.util.refinement.InterpolantManager;

/** Manager for {@link SymbolicInterpolant}. */
public class SymbolicInterpolantManager
    implements InterpolantManager<ForgettingCompositeState, SymbolicInterpolant> {

  private static final SymbolicInterpolantManager SINGLETON = new SymbolicInterpolantManager();

  public static SymbolicInterpolantManager getInstance() {
    return SINGLETON;
  }

  private SymbolicInterpolantManager() {
    // DO NOTHING
  }

  @Override
  public SymbolicInterpolant createInitialInterpolant() {
    return SymbolicInterpolant.TRUE;
  }

  @Override
  public SymbolicInterpolant createInterpolant(ForgettingCompositeState state) {
    final ValueAnalysisState values = state.getValueState();
    final ConstraintsState constraints = state.getConstraintsState();

    return new SymbolicInterpolant(
        values.getInformation(), new ConstraintsInformation(constraints));
  }

  @Override
  public SymbolicInterpolant getTrueInterpolant() {
    return SymbolicInterpolant.TRUE;
  }

  @Override
  public SymbolicInterpolant getFalseInterpolant() {
    return SymbolicInterpolant.FALSE;
  }
}
