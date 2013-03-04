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
package org.sosy_lab.cpachecker.cpa.explicit.refiner;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitPrecision;
import org.sosy_lab.cpachecker.cpa.explicit.refiner.utils.ExplicitInterpolator;
import org.sosy_lab.cpachecker.cpa.explicit.refiner.utils.ExplictFeasibilityChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

@Options(prefix="cpa.explicit.refiner")
public class ExplicitInterpolationBasedExplicitRefiner implements Statistics {
  /**
   * whether or not to always use the initial node as starting point for the next re-exploration of the ARG
   */
  @Option(description="whether or not to always use the inital node as starting point for the next re-exploration of the ARG")
  private boolean useInitialNodeAsRestartingPoint = true;

  /**
   * the ART element, from where to cut-off the subtree, and restart the analysis
   */
  private ARGState firstInterpolationPoint = null;

  // statistics
  private int numberOfRefinements           = 0;
  private int numberOfSuccessfulRefinements = 0;
  private int numberOfInterpolations        = 0;
  private int numberOfErrorPathElements     = 0;
  private Timer timerInterpolation          = new Timer();

  protected ExplicitInterpolationBasedExplicitRefiner(Configuration config, PathFormulaManager pathFormulaManager)
      throws InvalidConfigurationException {
    config.inject(this);
  }

  protected Multimap<CFANode, String> determinePrecisionIncrement(UnmodifiableReachedSet reachedSet,
      ARGPath errorPath) throws CPAException {
    timerInterpolation.start();

    firstInterpolationPoint = null;

    Multimap<CFANode, String> increment = HashMultimap.create();
    // only do a refinement if a full-precision check shows that the path is infeasible
    if (!isPathFeasable(errorPath)) {
      numberOfRefinements++;

      ExplicitInterpolator interpolator     = new ExplicitInterpolator();
      Map<String, Long> currentInterpolant  = new HashMap<>();

      for (int i = 0; i < errorPath.size(); i++) {
        numberOfErrorPathElements++;

        CFAEdge currentEdge = errorPath.get(i).getSecond();
        if (currentEdge instanceof CFunctionReturnEdge) {
          currentEdge = ((CFunctionReturnEdge)currentEdge).getSummaryEdge();
        }

        //System.out.println("\n\ncurrent edge: " + currentEdge);

        // do interpolation
        Map<String, Long> inputInterpolant = new HashMap<>(currentInterpolant);
        try {
          numberOfInterpolations++;
          //System.out.println("\t\tinput interpolant: " + inputInterpolant);
          Set<Pair<String, Long>> interpolant = interpolator.deriveInterpolant(errorPath, i, inputInterpolant);

          //System.out.println("\t\t ----> feasible: " + (interpolator.isFeasible() ? "YES" : "NO"));
          //System.out.println("\t\t ----> element: " + element);

          // early stop once we are past the first statement that made a path feasible for the first time
          if (interpolant == null) {
            timerInterpolation.stop();
            return increment;
          }
          for (Pair<String, Long> element : interpolant) {
            if (element.getSecond() == null) {
              currentInterpolant.remove(element.getFirst());
            } else {
              currentInterpolant.put(element.getFirst(), element.getSecond());
            }
          }
        }
        catch (InterruptedException e) {
          throw new CPAException("Explicit-Interpolation failed: ", e);
        }

        // remove variables from the interpolant that belong to the scope of the returning function
        // this is done one iteration after returning from the function, as the special FUNCTION_RETURN_VAR is needed that long
        if (i > 0 && errorPath.get(i - 1).getSecond().getEdgeType() == CFAEdgeType.ReturnStatementEdge) {
          currentInterpolant = clearInterpolant(currentInterpolant, errorPath.get(i - 1).getSecond().getSuccessor().getFunctionName());
        }

        // add the current interpolant to the precision
        for (String variableName : currentInterpolant.keySet()) {
          if (!isRedundant(extractPrecision(reachedSet, errorPath.get(i).getFirst()), currentEdge, variableName)) {
            increment.put(currentEdge.getSuccessor(), variableName);

            if (firstInterpolationPoint == null) {
              firstInterpolationPoint = errorPath.get(Math.max(1, i - 1)).getFirst();
              numberOfSuccessfulRefinements++;
            }
          }
        }
      }
    }

    timerInterpolation.stop();
    return increment;
  }

  /**
   * This method removes variables from the interpolant that belong to the scope of the given function.
   *
   * @param currentInterpolant the current interpolant
   * @param functionName the name of the function for which to remove variables
   * @return the current interpolant with the respective variables removed
   */
  private Map<String, Long> clearInterpolant(Map<String, Long> currentInterpolant, String functionName) {
    List<String> toDrop = new ArrayList<>();

    for (String variableName : currentInterpolant.keySet()) {
      if (variableName.startsWith(functionName + "::")) {
        toDrop.add(variableName);
      }
    }

    currentInterpolant.keySet().remove(toDrop);

    return currentInterpolant;
  }

  private ExplicitPrecision extractPrecision(UnmodifiableReachedSet reachedSet, ARGState currentArgState) {
    return Precisions.extractPrecisionByType(reachedSet.getPrecision(currentArgState), ExplicitPrecision.class);
  }

  private boolean isRedundant(ExplicitPrecision precision, CFAEdge currentEdge, String currentVariable) {
    return precision.getCegarPrecision().allowsTrackingAt(currentEdge.getSuccessor(), currentVariable);
  }

  /**
   * This method determines the new interpolation point.
   *
   * @param errorPath the error path from where to determine the interpolation point
   * @return the new interpolation point
   */
  protected ARGState determineInterpolationPoint(ARGPath errorPath) {
    // just use initial node of error path if the respective option is set
    if (useInitialNodeAsRestartingPoint) {
      return errorPath.get(1).getFirst();
    }

    // otherwise, use the first node where new information is present
    else {
      return firstInterpolationPoint;
    }
  }

  /**
   * This method checks if the given path is feasible, when not tracking the given set of variables.
   *
   * @param path the path to check
   * @return true, if the path is feasible, else false
   * @throws CPAException if the path check gets interrupted
   */
  private boolean isPathFeasable(ARGPath path) throws CPAException {
    try {
      // create a new ExplicitPathChecker, which does not track any of the given variables
      ExplictFeasibilityChecker checker = new ExplictFeasibilityChecker();

      return checker.isFeasible(path);
    }
    catch (InterruptedException e) {
      throw new CPAException("counterexample-check failed: ", e);
    }
  }

  @Override
  public String getName() {
    return "Explicit Interpolation-Based Refiner";
  }

  @Override
  public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
    out.println("  number of explicit refinements:                      " + numberOfRefinements);
    out.println("  number of successful explicit refinements:           " + numberOfSuccessfulRefinements);
    out.println("  number of explicit interpolations:                   " + numberOfInterpolations);
    out.println("  total number of elements in error paths:             " + numberOfErrorPathElements);
    out.println("  percentage of elements checked:                      " + (Math.round(((double)numberOfInterpolations / (double)numberOfErrorPathElements) * 10000) / 100.00) + "%");
    out.println("  max. time for singe interpolation:                   " + timerInterpolation.printMaxTime());
    out.println("  total time for interpolation:                        " + timerInterpolation);
  }
}
