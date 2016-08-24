/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.interpolant;

import org.sosy_lab.cpachecker.cpa.constraints.domain.ConstraintsState;
import org.sosy_lab.cpachecker.cpa.constraints.util.ConstraintsInformation;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.symbolic.refiner.ForgettingCompositeState;
import org.sosy_lab.cpachecker.util.refinement.InterpolantManager;

/**
 * Manager for {@link SymbolicInterpolant}.
 */
public class SymbolicInterpolantManager implements InterpolantManager<ForgettingCompositeState, SymbolicInterpolant> {

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

    return new SymbolicInterpolant(values.getInformation(),
                                   new ConstraintsInformation(constraints, constraints.getDefiniteAssignment()));
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
