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
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
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
  ARGState blockingState = null;
  boolean block = false;
  Long returnValue = null;

  public static boolean DEBUG = !true;
  private static void debug(String message) {
    if(DEBUG) System.out.println(message);
  }


  /**
   * TODO never gets called when using AssignedVariablesCollector and current edge is a CReturnStatementEdge, as that does not collect variables from these edges
   *
   * @param path the path to check
   * @throws CPAException
   * @throws InterruptedException
   */
  public Map<String, Long> deriveInterpolant(Path path, String currentVariable, Map<String, Long> currentInterpolant) throws CPAException, InterruptedException {
    try {

      ExplicitState next          = new ExplicitState(currentInterpolant);
      ExplicitPrecision precision = new ExplicitPrecision("", config, Optional.<VariableClassification>absent());

      Long variableValue = null;

      boolean interpolated = false;

      if(block) {
        return currentInterpolant;
      }

      if(blockingState != null && blockingState == path.get(0).getFirst()) {
        block = true;
      }

      CFAEdge interpolationEdge = path.get(0).getSecond();
      interpolationEdge.hashCode();

      for(Pair<ARGState, CFAEdge> pathElement : path) {
        CFAEdge presentEdge = pathElement.getSecond();
        Collection<ExplicitState> successors = transfer.getAbstractSuccessors(
            next,
            precision,
            pathElement.getSecond());

        next = extractNextState(successors);

        // there is no successor, but current path element is not an error state => error path is spurious
        if(next == null && !pathElement.getFirst().isTarget()) {
          blockingState = pathElement.getFirst();
debug("    path is infeasible @ " + pathElement.getSecond());

/*
          if(variableValue == null) {
            System.out.println("REMOVED-INFEASIBLE " + currentVariable);
            currentInterpolant.remove(currentVariable);
          }
*/

          return currentInterpolant;
        }

        if(!interpolated) {
          if(next.contains(currentVariable)) {
            variableValue = next.getValueFor(currentVariable);
          }
          else {
            variableValue = null;
          }

          if(interpolationEdge.getEdgeType() == CFAEdgeType.ReturnStatementEdge) {
            String returnVariableName = interpolationEdge.getSuccessor().getFunctionName() + "::___cpa_temp_result_var_";
            if(next.contains(returnVariableName)) {
              returnValue = next.getValueFor(returnVariableName);
              next.forget(returnVariableName);
            }
          }

          next.forget(currentVariable);

          interpolated = true;
        }
      }
debug("    path is feasable");

      if(variableValue != null) {
debug("      adding " + currentVariable + " to interpolant with value " + variableValue);
        currentInterpolant.put(currentVariable, variableValue);
      }/* else {
        System.out.println("REMOVED-FEASABLE " + currentVariable);
        currentInterpolant.remove(currentVariable);
      }*/


      // might be no longer needed, as fun::___cpa_temp_result_var_ is in set of collected variables/ReturnStatementEdge in set of relevant edges
      if(interpolationEdge.getEdgeType() == CFAEdgeType.FunctionReturnEdge && returnValue != null) {
debug("      adding function assignment to interpolant");
        currentInterpolant.put(currentVariable, returnValue);
        returnValue = null;
      }

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