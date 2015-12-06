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
package org.sosy_lab.cpachecker.cpa.value.refiner.utils;

import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.refiner.ValueAnalysisInterpolant;
import org.sosy_lab.cpachecker.util.refinement.InterpolantManager;

/**
 * InterpolantManager for interpolants of {@link ValueAnalysisState}.
 */
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
