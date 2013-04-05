/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.explicit.refiner.utils;

import static com.google.common.collect.Iterables.skip;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.collect.PathCopyingPersistentTreeMap;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitPrecision;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitState;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CounterexampleAnalysisFailed;
import org.sosy_lab.cpachecker.util.VariableClassification;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;

public class ExplicitInterpolator {

  /**
   * the configuration of the interpolator
   */
  private Configuration config = null;

  /**
   * the transfer relation in use
   */
  private ExplicitTransferRelation transfer = null;

  /**
   * the first path element without any successors
   */
  public Pair<ARGState, CFAEdge> conflictingElement = null;

  /**
   * boolean flag telling whether the current path is feasible
   */
  private boolean isFeasible = true;

  /**
   * boolean flag telling whether any previous path was feasible
   */
  private boolean wasFeasible = false;

  /**
   * boolean flag telling whether to cancel the interpolation after the current run
   */
  boolean cancelInterpolation = false;

  /**
   * This method acts as the constructor of the class.
   */
  public ExplicitInterpolator() throws CPAException {
    try {
      config    = Configuration.builder().build();
      transfer  = new ExplicitTransferRelation(config);
    }

    catch (InvalidConfigurationException e) {
      throw new CounterexampleAnalysisFailed("Invalid configuration for checking path: " + e.getMessage(), e);
    }
  }

  /**
   * This method derives an interpolant for the given error path and interpolation state.
   *
   * @param errorPath the path to check
   * @param offset offset of the state at where to start the current interpolation
   * @param inputInterpolant the input interpolant
   * @throws CPAException
   * @throws InterruptedException
   */
  public Set<Pair<String, Long>> deriveInterpolant(
      ARGPath errorPath,
      int offset,
      Map<String, Long> inputInterpolant) throws CPAException, InterruptedException {
    try {
      ExplicitState initialState  = new ExplicitState(PathCopyingPersistentTreeMap.copyOf(inputInterpolant));
      ExplicitPrecision precision = new ExplicitPrecision("", config, Optional.<VariableClassification>absent(), HashMultimap.<CFANode, String>create());

      Long currentVariableValue             = null;
      Pair<String, Long> currentInterpolant = null;
      Set<Pair<String, Long>> interpolant   = new HashSet<>();

      Pair<ARGState, CFAEdge> interpolationState = errorPath.get(offset);

      // cancel the interpolation if we are interpolating at the conflicting element
      if (wasFeasible && interpolationState == conflictingElement) {
        cancelInterpolation = true;
      }

      // consume subsequent edge
      Collection<ExplicitState> successors = transfer.getAbstractSuccessors(
          initialState,
          precision,
          errorPath.get(offset).getSecond());
      ExplicitState initialSuccessor = extractSuccessorState(successors);

      if (initialSuccessor == null) {
        return null;
      }

      // for each variable in the difference: remove the variable from the abstract assignment and check the path
      // TODO: also do this the other way round, remove all first, than re-add one by one
      Set<String> irrelevantVariables = new HashSet<>();
      for (String currentVar : initialState.getDifference(initialSuccessor)) {
        // start off with the successor of the initial state
        ExplicitState successor = initialSuccessor.clone();

        if (successor.contains(currentVar)) {
          currentVariableValue = successor.getValueFor(currentVar);
        }

        currentInterpolant = Pair.of(currentVar, currentVariableValue);

        // remove the value of the current variable and the already-found-irrelevant variables from the successor
        successor.forget(currentVar);
        for (String irrelevantVar : irrelevantVariables) {
          successor.forget(irrelevantVar);
        }

        // simulate the remaining path
        for (Pair<ARGState, CFAEdge> pathElement : skip(errorPath, offset + 1)) {
          successors = transfer.getAbstractSuccessors(
              successor,
              precision,
              pathElement.getSecond());

          successor = extractSuccessorState(successors);

          // there is no successor and the current path element is not an error state => error path is spurious
          if (successor == null && !pathElement.getFirst().isTarget()) {
            if (conflictingElement == null || conflictingElement.getFirst().isOlderThan(pathElement.getFirst())) {
              conflictingElement = pathElement;
            }

            isFeasible = false;
            //System.out.println("\t\t\tinfeasable at " + pathElement.getSecond());
            //return Pair.of(currentVariable, null);
            currentInterpolant = Pair.of(currentVar, null);
            irrelevantVariables.add(currentVar);
            break;
          }
        }

        if (isFeasible) {
          wasFeasible = true;
        }

        interpolant.add(currentInterpolant);
      }


      // signal callee to cancel any further interpolation runs
      if (cancelInterpolation) {
        return null;
      }

      isFeasible  = true;
      wasFeasible = true;

      // path is feasible
      return interpolant;
    } catch (InvalidConfigurationException e) {
      throw new CounterexampleAnalysisFailed("Invalid configuration for checking path: " + e.getMessage(), e);
    }
  }

  /**
   * This method returns whether or not the last error path was feasible.
   *
   * @return whether or not the last error path was feasible
   */
  public boolean isFeasible() {
    return isFeasible;
  }

  /**
   * This method extracts the single successor out of the (hopefully singleton) successor collection.
   *
   * @param successors the collection of successors
   * @return the successor, or null if none exists
   */
  private ExplicitState extractSuccessorState(Collection<ExplicitState> successors) {
    if (successors.isEmpty()) {
      return null;
    } else {
      assert (successors.size() == 1);
      return Lists.newArrayList(successors).get(0);
    }
  }
}
