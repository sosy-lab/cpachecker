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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.Path;
import org.sosy_lab.cpachecker.cpa.explicit.refiner.utils.AssignedVariablesCollector;
import org.sosy_lab.cpachecker.cpa.explicit.refiner.utils.AssumptionVariablesCollector;
import org.sosy_lab.cpachecker.cpa.explicit.refiner.utils.ExplicitInterpolator;
import org.sosy_lab.cpachecker.cpa.explicit.refiner.utils.ExplictPathChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

@Options(prefix="cpa.explicit.refiner")
public class ExplicitInterpolationBasedExplicitRefiner {
  /**
   * whether or not to always use the initial node as starting point for the next re-exploration of the ARG
   */
  @Option(description="whether or not to always use the inital node as starting point for the next re-exploration of the ARG")
  private boolean useInitialNodeAsRestartingPoint = true;

  /**
   * whether or not to use the assumption-closure for explicit refinement (defaults to true)
   */
  @Option(description="whether or not to use assumption-closure for explicit refinement")
  private boolean useAssumptionClosure = true;

  /**
   * the ART element, from where to cut-off the subtree, and restart the analysis
   */
  private ARGState firstInterpolationPoint = null;

  // statistics
  private int numberOfRefinements           = 0;
  private int numberOfCounterExampleChecks  = 0;
  private int numberOfErrorPathElements     = 0;
  private Timer timerCounterExampleChecks   = new Timer();

  protected ExplicitInterpolationBasedExplicitRefiner(Configuration config, PathFormulaManager pathFormulaManager)
      throws InvalidConfigurationException {
    config.inject(this);
  }

  protected Multimap<CFANode, String> determinePrecisionIncrement(UnmodifiableReachedSet reachedSet,
      Path errorPath) throws CPAException {
    timerCounterExampleChecks.start();

    firstInterpolationPoint = null;

    numberOfRefinements++;

    Multimap<CFANode, String> increment = HashMultimap.create();
    // only do a refinement if a full-precision check shows that the path is infeasible
    if(!isPathFeasable(errorPath, HashMultimap.<CFANode, String>create())) {

      Multimap<CFANode, String> referencedVariableMapping = determineReferencedVariableMapping(errorPath);

      ExplicitInterpolator interpolator     = new ExplicitInterpolator();
      Map<String, Long> currentInterpolant  = new HashMap<String, Long>();

      for(int i = 0; i < errorPath.size(); i++) {
        numberOfErrorPathElements++;

        CFAEdge currentEdge = errorPath.get(i).getSecond();
        if(currentEdge instanceof CFunctionReturnEdge) {
          currentEdge = ((CFunctionReturnEdge)currentEdge).getSummaryEdge();
        }

        Collection<String> referencedVariablesAtEdge = referencedVariableMapping.get(currentEdge.getSuccessor());

        // no potentially interesting variables referenced - skip
        if(referencedVariablesAtEdge.isEmpty()) {
          for(String variableName : currentInterpolant.keySet()) {
            increment.put(currentEdge.getSuccessor(), variableName);
          }
        }

        // check for each variable, if ignoring it makes the error path feasible
        for(String currentVariable : referencedVariablesAtEdge) {
          numberOfCounterExampleChecks++;

          try {
            currentInterpolant = interpolator.deriveInterpolant(errorPath, errorPath.get(i), currentVariable, currentInterpolant);
          }
          catch (InterruptedException e) {
            throw new CPAException("Explicit-Interpolation failed: ", e);
          }

          for(String variableName : currentInterpolant.keySet()) {
            increment.put(currentEdge.getSuccessor(), variableName);

            if(firstInterpolationPoint == null) {
              firstInterpolationPoint = errorPath.get(i).getFirst();
            }
          }
        }
      }
    }

    timerCounterExampleChecks.stop();
    return increment;
  }

  /**
   * This method determines the new interpolation point.
   *
   * @param errorPath the error path from where to determine the interpolation point
   * @return the new interpolation point
   */
  protected ARGState determineInterpolationPoint(Path errorPath) {
    // just use initial node of error path if the respective option is set
    if(useInitialNodeAsRestartingPoint) {
      return errorPath.get(1).getFirst();
    }

    // otherwise, use the first node where new information is present
    else {
      return firstInterpolationPoint;
    }
  }

  /**
   * This method determines the mapping where to do an explicit interpolation for which variables.
   *
   * @param currentErrorPath the current error path to check
   * @return the mapping where to do an explicit interpolation for which variables
   */
  private Multimap<CFANode, String> determineReferencedVariableMapping(Path currentErrorPath) {
    if(useAssumptionClosure) {
      AssumptionVariablesCollector coll = new AssumptionVariablesCollector();
      return coll.collectVariables(currentErrorPath);
    }

    else {
      AssignedVariablesCollector collector = new AssignedVariablesCollector();
      return collector.collectVars(currentErrorPath);
    }
  }

  /**
   * This method checks if the given path is feasible, when not tracking the given set of variables.
   *
   * @param path the path to check
   * @param variablesToBeIgnored the variables to ignore
   * @return true, if the path is feasible, else false
   * @throws CPAException if the path check gets interrupted
   */
  private boolean isPathFeasable(Path path, Multimap<CFANode, String> variablesToBeIgnored) throws CPAException {
    try {
      // create a new ExplicitPathChecker, which does not track any of the given variables
      ExplictPathChecker checker = new ExplictPathChecker();

      return checker.checkPath(path, variablesToBeIgnored);
    }
    catch (InterruptedException e) {
      throw new CPAException("counterexample-check failed: ", e);
    }
  }

  protected void printStatistics(PrintStream out, Result result, ReachedSet reached) {
    out.println(this.getClass().getSimpleName() + ":");
    out.println("  number of explicit-interpolation-based refinements:  " + numberOfRefinements);
    out.println("  number of counter-example checks:                    " + numberOfCounterExampleChecks);
    out.println("  total number of elements in error paths:             " + numberOfErrorPathElements);
    out.println("  percentage of elements checked:                      " + (Math.round(((double)numberOfCounterExampleChecks / (double)numberOfErrorPathElements) * 10000) / 100.00) + "%");
    out.println("  max. time for singe check:                           " + timerCounterExampleChecks.printMaxTime());
    out.println("  total time for checks:                               " + timerCounterExampleChecks);
  }
}