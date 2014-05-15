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
package org.sosy_lab.cpachecker.cpa.octagon.refiner;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.logging.Level;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.ShutdownNotifier;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.octagon.OctPrecision;
import org.sosy_lab.cpachecker.cpa.octagon.OctState;
import org.sosy_lab.cpachecker.cpa.octagon.OctTransferRelation;
import org.sosy_lab.cpachecker.cpa.octagon.OctagonCPA;
import org.sosy_lab.cpachecker.cpa.value.refiner.utils.AssumptionUseDefinitionCollector;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class OctagonAnalysisFeasabilityChecker {

  private final OctTransferRelation transfer;

  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final ARGPath checkedPath;
  private final ARGPath foundPath;

  public OctagonAnalysisFeasabilityChecker(CFA cfa, LogManager log, ShutdownNotifier pShutdownNotifier, ARGPath path, OctagonCPA cpa) throws InvalidConfigurationException, CPAException, InterruptedException {
    logger = log;
    shutdownNotifier = pShutdownNotifier;

    // use the normal configuration for creating the transferrelation
    transfer  = new OctTransferRelation(logger, cfa, cpa.handleFloats());
    checkedPath = path;

    // use a new configuration which only has the default values for the precision
    // we do not want any special options to be set there
    foundPath = getInfeasiblePrefix(new OctPrecision(Configuration.defaultConfiguration()),
                                    new OctState(logger, cpa.handleFloats(), cpa.getManager()));
  }

  /**
   * This method checks if the given path is feasible, when not tracking the given set of variables.
   *
   * @param path the path to check
   * @return true, if the path is feasible, else false
   * @throws CPAException
   * @throws InterruptedException
   */
  public boolean isFeasible() {
      return checkedPath.size() == foundPath.size();
  }

  public Set<String> getPrecisionIncrement(OctPrecision precision) {
    if (isFeasible()) {
      return Collections.emptySet();
    } else {
      Set<String> varNames;
      LinkedList<CFAEdge> edgesList = new LinkedList<>(foundPath.asEdgesList());

      // search for new trackable variables until we find some
      do {
        varNames = Sets.difference(new AssumptionUseDefinitionCollector().obtainUseDefInformation(edgesList),
                                   precision.getTrackedVars());
        edgesList.removeLast();
        while (!edgesList.isEmpty() && !(edgesList.getLast() instanceof AssumeEdge)) {
          edgesList.removeLast();
        }
      } while (varNames.isEmpty() && !edgesList.isEmpty());

      return varNames;
    }
  }

  /**
   * This method obtains the prefix of the path, that is infeasible by itself. If the path is feasible, the whole path
   * is returned
   *
   * @param path the path to check
   * @param pPrecision the precision to use
   * @param pInitial the initial state
   * @return the prefix of the path that is feasible by itself
   * @throws CPAException
   * @throws InterruptedException
   */
  private ARGPath getInfeasiblePrefix(final OctPrecision pPrecision, final OctState pInitial)
      throws CPAException, InterruptedException {
    try {
      Collection<OctState> next = Lists.newArrayList(pInitial);

      ARGPath prefix = new ARGPath();

      Collection<OctState> successors = new HashSet<>();

      for (Pair<ARGState, CFAEdge> pathElement : checkedPath) {
        successors.clear();
        for (OctState st : next) {
          successors.addAll(transfer.getAbstractSuccessors(
              st,
              pPrecision,
              pathElement.getSecond()));

          // computing the feasibility check takes sometimes much time with ocatongs
          // so if the shutdownNotifer says that we should shutdown, we cannot
          // make any assumptions about the path reachibility and say that it's
          // reachable (over-approximation)
          if (shutdownNotifier.shouldShutdown()) {
            logger.log(Level.INFO, "Cancelling feasibility check with octagon Analysis, timelimit reached");
            return checkedPath;
          }
        }

        prefix.addLast(pathElement);

        // no successors => path is infeasible
        if(successors.isEmpty()) {
          break;
        }

        // get matching successor state and apply precision
        next.clear();
        next.addAll(successors);
      }
      return prefix;
    } catch (CPATransferException e) {
      throw new CPAException("Computation of successor failed for checking path: " + e.getMessage(), e);
    }
  }

}
