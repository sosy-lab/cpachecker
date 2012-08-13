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
import java.util.List;
import java.util.Map;

import org.sosy_lab.common.Pair;
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
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitTransferRelation;
import org.sosy_lab.cpachecker.cpa.explicit.refiner.utils.AssignedVariablesCollector;
import org.sosy_lab.cpachecker.cpa.explicit.refiner.utils.AssumptionVariablesCollector;
import org.sosy_lab.cpachecker.cpa.explicit.refiner.utils.ExplicitInterpolator;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

@Options(prefix="cpa.explict.refiner")
public class ExplicitInterpolationBasedExplicitRefiner extends ExplicitRefiner {

  @Option(description="whether or not to use assumption-closure for explicit refinement")
  boolean useAssumptionClosure = true;

  // statistics
  private int numberOfRefinements           = 0;
  private int numberOfCounterExampleChecks  = 0;
  private int numberOfErrorPathElements     = 0;
  private Timer timerCounterExampleChecks   = new Timer();

  protected ExplicitInterpolationBasedExplicitRefiner(Configuration config, PathFormulaManager pathFormulaManager)
      throws InvalidConfigurationException {
    config.inject(this);
  }

  public static boolean DEBUG = !true;
  private static void debug(String message) {
    if(DEBUG) System.out.println(message);
  }

  @Override
  protected Multimap<CFANode, String> determinePrecisionIncrement(UnmodifiableReachedSet reachedSet,
      Path errorPath) throws CPAException {
    timerCounterExampleChecks.start();

    firstInterpolationPoint = null;

    numberOfRefinements++;

debug("\n\ndeterminePrecisionIncrement");

    Multimap<CFANode, String> increment = HashMultimap.create();
ExplicitTransferRelation.DEBUG = false;
    // only do a refinement if a full-precision check shows that the path is infeasible
    if(!isPathFeasable(errorPath, HashMultimap.<CFANode, String>create())) {

      // make copy of error path, as it will be destructed here, but is needed later on, again
      Path currentErrorPath   = cloneErrorPath(errorPath);
      List<CFAEdge> cfaTrace  = extractCFAEdgeTrace(currentErrorPath);

      Multimap<CFANode, String> referencedVariableMapping = determineReferencedVariableMapping(cfaTrace);

debug("error path: " + currentErrorPath);

      ExplicitInterpolator interpolator     = new ExplicitInterpolator();
      Map<String, Long> currentInterpolant  = new HashMap<String, Long>();

      for(int i = 0; i < cfaTrace.size(); i++){
        if(i > 0) {
          currentErrorPath.remove();
        }

        numberOfErrorPathElements++;

        CFAEdge currentEdge = cfaTrace.get(i);
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

debug("\ncurrentEdge [" + currentEdge.getEdgeType() + "]: " + currentEdge);

        // check for each variable, if ignoring it makes the error path feasible
        for(String currentVariable : referencedVariablesAtEdge) {
          numberOfCounterExampleChecks++;
debug("  currentVariable: " + currentVariable);
          try {
            currentInterpolant = interpolator.deriveInterpolant(currentErrorPath, currentVariable, currentInterpolant);
debug("--> currentInterpolant: " + currentInterpolant);
          }
          catch (InterruptedException e) {
            throw new CPAException("Explicit-Interpolation failed: ", e);
          }

          for(String variableName : currentInterpolant.keySet()) {
            increment.put(currentEdge.getSuccessor(), variableName);

            if(firstInterpolationPoint == null) {
              firstInterpolationPoint = edgeToState.get(cfaTrace.get(i + 1));
            }
          }
        }
      }
    }
debug("\nincrement: " + increment);
//ExplicitTransferRelation.DEBUG = true;
    timerCounterExampleChecks.stop();
    return increment;
  }

  /**
   * This method creates a clone of the current error path, as it will read "destructive".
   *
   * @param errorPath the error path to clone
   * @return the cloned error path
   */
  private Path cloneErrorPath(Path errorPath) {
    Path currentErrorPath = new Path();
    for(Pair<ARGState, CFAEdge> pathElement : errorPath) {
      currentErrorPath.add(pathElement);
    }
    return currentErrorPath;
  }

  /**
   * This method determines the mapping where to do an explicit interpolation for which variables.
   *
   * @param cfaTrace the CFA trace to check
   * @return the mapping where to do an explicit interpolation for which variables
   */
  private Multimap<CFANode, String> determineReferencedVariableMapping(List<CFAEdge> cfaTrace) {
    if(useAssumptionClosure) {
      AssumptionVariablesCollector coll = new AssumptionVariablesCollector();
      return coll.collectVariables(cfaTrace);
    }

    else {
      AssignedVariablesCollector collector = new AssignedVariablesCollector();
      return collector.collectVars(cfaTrace);
    }
  }

  @Override
  public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
    out.println(this.getClass().getSimpleName() + ":");
    out.println("  number of explicit-interpolation-based refinements:  " + numberOfRefinements);
    out.println("  number of counter-example checks:                    " + numberOfCounterExampleChecks);
    out.println("  total number of elements in error paths:             " + numberOfErrorPathElements);
    out.println("  percentage of elements checked:                      " + (Math.round(((double)numberOfCounterExampleChecks / (double)numberOfErrorPathElements) * 10000) / 100.00) + "%");
    out.println("  max. time for singe check:                           " + timerCounterExampleChecks.printMaxTime());
    out.println("  total time for checks:                               " + timerCounterExampleChecks);
  }
}