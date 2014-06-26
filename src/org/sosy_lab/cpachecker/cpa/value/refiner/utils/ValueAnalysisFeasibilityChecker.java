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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisPrecision;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

public class ValueAnalysisFeasibilityChecker {

  private final CFA cfa;
  private final LogManager logger;
  private final ValueAnalysisTransferRelation transfer;
  private final ValueAnalysisPrecision precision;
  private final Configuration config;

  /**
   * This method acts as the constructor of the class.
   *
   * @param pLogger the logger to use
   * @param pCfa the cfa in use
   * @param pInitial the initial state for starting the exploration
   * @throws InvalidConfigurationException
   */
  public ValueAnalysisFeasibilityChecker(LogManager pLogger, CFA pCfa) throws InvalidConfigurationException {
    this.cfa    = pCfa;
    this.logger = pLogger;

    config    = Configuration.builder().build();
    transfer  = new ValueAnalysisTransferRelation(config, logger, cfa);
    precision = ValueAnalysisPrecision.createDefaultPrecision();
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
    return isFeasible(path, new ValueAnalysisState());
  }

  /**
   * This method checks if the given path is feasible, starting with the given initial state.
   *
   * @param path the path to check
   * @param pInitial the initial state
   * @return true, if the path is feasible, else false
   * @throws CPAException
   * @throws InterruptedException
   */
  public boolean isFeasible(final ARGPath path, final ValueAnalysisState pInitial)
      throws CPAException, InterruptedException {

    return path.size() == getInfeasilbePrefix(path, pInitial).size();
  }

  /**
   * This method obtains the shortest prefix of the path, that is infeasible by itself. If the path is feasible, the whole path
   * is returned.
   *
   * @param path the path to check
   * @param pInitial the initial state
   * @return the shortest prefix of the path that is feasible by itself
   * @throws CPAException
   * @throws InterruptedException
   */
  public ARGPath getInfeasilbePrefix(final ARGPath path, final ValueAnalysisState pInitial)
      throws CPAException, InterruptedException {
    return getInfeasilbePrefixes(path, pInitial).get(0);
  }

  /**
   * This method obtains a list of prefixes of the path, that are infeasible by themselves. If the path is feasible, the whole path
   * is returned as the only element of the list.
   *
   * @param path the path to check
   * @param pInitial the initial state
   * @return the list of prefix of the path that are feasible by themselves
   * @throws CPAException
   * @throws InterruptedException
   */
  public List<ARGPath> getInfeasilbePrefixes(final ARGPath path, final ValueAnalysisState pInitial)
      throws CPAException, InterruptedException {

    List<ARGPath> prefixes = new ArrayList<>();

    try {
      ARGPath currentPrefix   = new ARGPath();
      ValueAnalysisState next = pInitial;

      for (Pair<ARGState, CFAEdge> pathElement : path) {
        Collection<ValueAnalysisState> successors = transfer.getAbstractSuccessors(
            next,
            precision,
            pathElement.getSecond());

        currentPrefix.addLast(pathElement);

        // no successors => path is infeasible
        if(successors.isEmpty()) {
          prefixes.add(currentPrefix);

          currentPrefix = new ARGPath();
          successors    = Sets.newHashSet(next);
        }

        // extract singleton successor state
        next = Iterables.getOnlyElement(successors);
      }

      // prefixes is empty => path is feasible, so add complete path
      if(prefixes.isEmpty()) {
        prefixes.add(path);
      }

      return prefixes;
    } catch (CPATransferException e) {
      throw new CPAException("Computation of successor failed for checking path: " + e.getMessage(), e);
    }
  }
}
