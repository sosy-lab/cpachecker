/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import java.util.Collection;

import org.sosy_lab.common.LogManager;
import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisPrecision;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.CounterexampleAnalysisFailed;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.VariableClassification;

import com.google.common.base.Optional;

public class ValueAnalysisFeasibilityChecker {

  private final CFA cfa;
  private final LogManager logger;
  private final ValueAnalysisTransferRelation transfer;
  private final Configuration config;

  /**
   * This method acts as the constructor of the class.
   * @throws CounterexampleAnalysisFailed
   */
  public ValueAnalysisFeasibilityChecker(LogManager pLogger, CFA pCfa) throws InvalidConfigurationException {
    this.cfa    = pCfa;
    this.logger = pLogger;

    config    = Configuration.builder().build();
    transfer  = new ValueAnalysisTransferRelation(config, logger, cfa);
  }

  /**
   * This method checks if the given path is feasible, when not tracking the given set of variables.
   *
   * @param path the path to check
   * @return true, if the path is feasible, else false
   * @throws CPAException
   * @throws InterruptedException
   */
  public boolean isFeasible(final ARGPath path) throws CPAException, InterruptedException {
    try {
      return isFeasible(path, new ValueAnalysisPrecision("", config, Optional.<VariableClassification>absent()));
    }
    catch (InvalidConfigurationException e) {
      throw new CPAException("Configuring ValueAnalysisFeasibilityChecker failed: " + e.getMessage(), e);
    }
  }

  /**
   * This method checks if the given path is feasible, when not tracking the given set of variables.
   *
   * @param path the path to check
   * @return true, if the path is feasible, else false
   * @throws CPAException
   * @throws InterruptedException
   */
  public boolean isFeasible(final ARGPath path, final ValueAnalysisPrecision pPrecision)
      throws CPAException, InterruptedException {
    try {
      ValueAnalysisState next = new ValueAnalysisState();

      for (Pair<ARGState, CFAEdge> pathElement : path) {
        Collection<ValueAnalysisState> successors = transfer.getAbstractSuccessors(
            next,
            pPrecision,
            pathElement.getSecond());

        // no successors => path is infeasible
        if(successors.isEmpty()) {
          return false;
        }

        // get successor state and apply precision
        next = pPrecision.computeAbstraction(successors.iterator().next(),
            AbstractStates.extractLocation(pathElement.getFirst()));
      }

      // path is feasible
      return true;
    } catch (CPATransferException e) {
      throw new CPAException("Computation of successor failed for checking path: " + e.getMessage(), e);
    }
  }
}
