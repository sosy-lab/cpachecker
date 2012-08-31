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

import java.util.Collection;
import java.util.Map;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.Path;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitPrecision;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitState;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitTransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.CounterexampleAnalysisFailed;
import org.sosy_lab.cpachecker.util.VariableClassification;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

public class ExplicitInterpolator {

  private Configuration config = null;

  private ExplicitTransferRelation transfer = null;

  /**
   * This method acts as the constructor of the class.
   */
  public ExplicitInterpolator() throws CPAException {
    try {
      config    = Configuration.builder().build();
      transfer  = new ExplicitTransferRelation(config);
    }

    catch(InvalidConfigurationException e) {
      throw new CounterexampleAnalysisFailed("Invalid configuration for checking path: " + e.getMessage(), e);
    }
  }

  /**
   * This method derives an interpolant for the given error path and interpolation state.
   *
   * @param errorPath the path to check
   * @param iterpolationState the state at where to start the current interpolation
   * @param currentVariable the variable on which the interpolation is performed on
   * @param currentInterpolant the current interpolant, which is build iteratively
   * @throws CPAException
   * @throws InterruptedException
   */
  public static boolean isFeasible = true;
  public Map<String, Long> deriveInterpolant(
      Path errorPath,
      Pair<ARGState, CFAEdge> iterpolationState,
      String currentVariable,
      Map<String, Long> currentInterpolant) throws CPAException, InterruptedException {
    try {

      ExplicitState next          = new ExplicitState(currentInterpolant);
      ExplicitPrecision precision = new ExplicitPrecision("", config, Optional.<VariableClassification>absent());

      Long variableValue    = null;
      boolean interpolated = false;

      boolean startInterpolation = false;
      for(Pair<ARGState, CFAEdge> pathElement : errorPath) {
        if(iterpolationState == pathElement) {
          startInterpolation = true;
        }

        if(!startInterpolation) {
          continue;
        }

        Collection<ExplicitState> successors = transfer.getAbstractSuccessors(
            next,
            precision,
            pathElement.getSecond());

        next = extractNextState(successors);

        // there is no successor, but current path element is not an error state => error path is spurious
        if(next == null && !pathElement.getFirst().isTarget()) {
//System.out.println("\t---> infeasible at " + pathElement.getSecond());

          if(variableValue == null) {
            currentInterpolant.remove(currentVariable);
          }

          isFeasible = false;

          return currentInterpolant;
        }

        if(!interpolated) {
          if(next.contains(currentVariable)) {
            variableValue = next.getValueFor(currentVariable);
          }
          else {
            variableValue = null;
          }

          next.forget(currentVariable);

          interpolated = true;
        }
      }

      if(variableValue != null) {
        currentInterpolant.put(currentVariable, variableValue);
      } else {
        currentInterpolant.remove(currentVariable);
      }

      isFeasible = true;

//System.out.println("\t---> feasible");

      // path is feasible
      return currentInterpolant;
    } catch(InvalidConfigurationException e) {
      throw new CounterexampleAnalysisFailed("Invalid configuration for checking path: " + e.getMessage(), e);
    }
  }

  /**
   * This method extracts the single successor out of the (hopefully singleton) successor collection.
   *
   * @param successors the collection of successors
   * @return the successor, or null if none exists
   */
  private ExplicitState extractNextState(Collection<ExplicitState> successors) {
    if(successors.isEmpty()) {
      return null;
    }
    else {
      assert(successors.size() == 1);
      return Lists.newArrayList(successors).get(0);
    }
  }
}