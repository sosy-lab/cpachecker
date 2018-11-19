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

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisCPA;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisPrecision;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisPrecision.IntervalAnalysisFullPrecision;
import org.sosy_lab.cpachecker.cpa.interval.IntervalAnalysisState;
import org.sosy_lab.cpachecker.util.refinement.GenericPrefixProvider;

public class IntervalAnalysisPrefixProvider extends GenericPrefixProvider<IntervalAnalysisState> {

  IntervalAnalysisPrecision precision = new IntervalAnalysisFullPrecision();
  /**
   * This method acts as the constructor of the class.
   *
   * @param pLogger the logger to use
   * @param pCfa the cfa in use
   */
  public IntervalAnalysisPrefixProvider(
      LogManager pLogger,
      CFA pCfa,
      Configuration config,
      ShutdownNotifier pShutdownNotifier,
      IntervalAnalysisState initialState)
      throws InvalidConfigurationException {

    super(
        new IntervalAnalysisStrongestPostOperator(pLogger, true, 2000),
        initialState,
        pLogger,
        pCfa,
        config,
        IntervalAnalysisCPA.class,
        pShutdownNotifier);
  }

  @Override
  public Precision getPrecision() {
    return precision;
  }
}
