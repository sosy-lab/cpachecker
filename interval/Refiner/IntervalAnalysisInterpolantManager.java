/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.interval.Refiner;

import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisState;
import org.sosy_lab.cpachecker.util.refinement.InterpolantManager;

public class IntervalAnalysisInterpolantManager
    implements InterpolantManager<IntervalAnalysisState, IntervalAnalysisInterpolant> {

  private static final IntervalAnalysisInterpolantManager SINGLETON =
      new IntervalAnalysisInterpolantManager();

  private IntervalAnalysisInterpolantManager() {}

  public static IntervalAnalysisInterpolantManager getInstance() {
    return SINGLETON;
  }

  @Override
  public IntervalAnalysisInterpolant createInitialInterpolant() {
    return IntervalAnalysisInterpolant.createInitial();
  }

  @Override
  public IntervalAnalysisInterpolant createInterpolant(IntervalAnalysisState state) {
    return new IntervalAnalysisInterpolant(state.getPersistentIntervalMap());
  }

  @Override
  public IntervalAnalysisInterpolant getTrueInterpolant() {
    return IntervalAnalysisInterpolant.TRUE;
  }

  @Override
  public IntervalAnalysisInterpolant getFalseInterpolant() {
    return IntervalAnalysisInterpolant.FALSE;
  }
}
