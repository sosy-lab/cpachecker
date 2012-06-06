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
import java.util.Collection;
import java.util.List;

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.Timer;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.MultiEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionReturnEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitPrecision;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitPrecision.CegarPrecision;
import org.sosy_lab.cpachecker.cpa.explicit.refiner.utils.AssignedVariablesCollector;
import org.sosy_lab.cpachecker.cpa.explicit.refiner.utils.AssumptionVariablesCollector;
import org.sosy_lab.cpachecker.cpa.explicit.refiner.utils.ExplictPathChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.predicates.interfaces.PathFormulaManager;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

@Options(prefix="cpa.explict.refiner")
public class ExplicitInterpolationBasedExplicitRefiner extends ExplicitRefiner {

  @Option(description="whether or not to use assumption-closure for explicit refinement")
  boolean useAssumptionClosure = true;

  /**
   * the ARG element, from where to cut-off the subtree, and restart the analysis
   */
  private ARGState firstInterpolationPoint = null;

  // statistics
  private int numberOfCounterExampleChecks                  = 0;
  private int numberOfErrorPathElements                     = 0;
  private Timer timerCounterExampleChecks                   = new Timer();

  protected ExplicitInterpolationBasedExplicitRefiner(
      Configuration config, PathFormulaManager
      pathFormulaManager) throws InvalidConfigurationException {
    super(config, pathFormulaManager);
    config.inject(this);
  }

  @Override
  protected Multimap<CFANode, String> determinePrecisionIncrement(ExplicitPrecision oldPrecision) throws CPAException {
    timerCounterExampleChecks.start();

    Multimap<CFANode, String> increment = HashMultimap.create();

    List<CFAEdge> cfaTrace = new ArrayList<CFAEdge>();
    for(Pair<ARGState, CFAEdge> pathElement : currentEdgePath){
      // expand any multi-edge
      if(pathElement.getSecond() instanceof MultiEdge) {
        for(CFAEdge singleEdge : (MultiEdge)pathElement.getSecond()) {
          cfaTrace.add(singleEdge);
        }
      }
      else {
        cfaTrace.add(pathElement.getSecond());
      }
    }

    firstInterpolationPoint = null;

    Multimap<CFANode, String> variablesToBeIgnored      = HashMultimap.create();
    Multimap<CFANode, String> referencedVariableMapping = determineReferencedVariableMapping(cfaTrace);

    for(int i = 0; i < cfaTrace.size(); i++){
      CFAEdge currentEdge = cfaTrace.get(i);

      numberOfErrorPathElements++;

      if(currentEdge instanceof FunctionReturnEdge) {
        currentEdge = ((FunctionReturnEdge)currentEdge).getSummaryEdge();
      }

      Collection<String> referencedVariablesAtEdge = referencedVariableMapping.get(currentEdge.getSuccessor());

      // no potentially interesting variables referenced - skip
      if(referencedVariablesAtEdge.isEmpty()) {
        continue;
      }

      // check for each variable, if ignoring it makes the error path feasible
      // it might be more reasonable to do this once for all variables referenced in this edge (esp. for assumes)
      for(String importantVariable : referencedVariablesAtEdge) {
        // referenced variable is already known to be important, so skip it, but set the location as interpolation point
        if(isRedundant(oldPrecision.getCegarPrecision(), currentEdge, importantVariable)) {
          if(firstInterpolationPoint == null) {
            firstInterpolationPoint = currentEdgePath.get(i + 1).getFirst();
          }

          // either set interpolation point and continue with next referenced variable,
          // or do not set interpolation point and proceed, but then, do not add current
          // variable to set of variables to be ignored, as it is known to be important,
          // hence, ignoring it would be wrong!
          // the strategy of choice is, to continue here, as we can save a few counterexample
          // checks - an evaluation will show if both strategies are equivalent (which I highly doubt),
          // or at least lead to the same verification results (which might be the case)
          // see commits 6143 to 6145
          continue;
        }

        numberOfCounterExampleChecks++;

        // variables to ignore in the current run
        variablesToBeIgnored.put(currentEdge.getSuccessor(), importantVariable);

        // if path becomes feasible, remove it from the set of variables to be ignored,
        // and add the variable to the precision increment, also setting the interpolation point
        if(isPathFeasable(cfaTrace, variablesToBeIgnored)) {
          variablesToBeIgnored.remove(currentEdge.getSuccessor(), importantVariable);
          increment.put(currentEdge.getSuccessor(), importantVariable);

          if(firstInterpolationPoint == null) {
            firstInterpolationPoint = currentEdgePath.get(i + 1).getFirst();
          }
        }
      }
    }

    timerCounterExampleChecks.stop();
    return increment;
  }

  @Override
  protected ARGState determineInterpolationPoint(
      List<Pair<ARGState, CFANode>> errorPath,
      Multimap<CFANode, String> precisionIncrement) {

    // just use initial node of error path if the respective option is set
    if(useInitialNodeAsRestartingPoint) {
      return errorPath.get(1).getFirst();
    }
    else {
      return firstInterpolationPoint;
    }
  }

  /**
   * This method determines the locations where to do a counterexample-check.
   *
   * @param cfaTrace
   * @return
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

  /**
   * This method checks whether or not a variable is already in the precision for the given edge.
   *
   * @param precision the current precision
   * @param currentEdge the current CFA edge
   * @param referencedVariableAtEdge the variable referenced at the current edge
   * @return true, if adding the given variable to the precision would be redundant, else false
  */
  private boolean isRedundant(CegarPrecision precision, CFAEdge currentEdge, String referencedVariableAtEdge) {
    return precision.allowsTrackingAt(currentEdge.getSuccessor(), referencedVariableAtEdge);
  }

  /**
   * This method checks if the given path is feasible, when not tracking the given set of variables.
   *
   * @param path the path to check
   * @param variablesToBeIgnored the variables to ignore
   * @return true, if the path is feasible, else false
   * @throws CPAException
   */
  private boolean isPathFeasable(List<CFAEdge> path, Multimap<CFANode, String> variablesToBeIgnored) throws CPAException {
    try {
      // create a new ExplicitPathChecker, which does not track any of the given variables
      ExplictPathChecker checker = new ExplictPathChecker();

      return checker.checkPath(path, variablesToBeIgnored);
    }
    catch (InterruptedException e) {
      throw new CPAException("counterexample-check failed: ", e);
    }
  }

  @Override
  public void printStatistics(PrintStream out, Result result, ReachedSet reached) {
    out.println(this.getClass().getSimpleName() + ":");
    out.println("  number of explicit refinements:           " + numberOfExplicitRefinements);
    out.println("  number of counter-example checks:         " + numberOfCounterExampleChecks);
    out.println("  total number of elements in error paths:  " + numberOfErrorPathElements);
    out.println("  percentage of elements checked:           " + (Math.round(((double)numberOfCounterExampleChecks / (double)numberOfErrorPathElements) * 10000) / 100.00) + "%");
    out.println("  max. time for singe check:                " + timerCounterExampleChecks.printMaxTime());
    out.println("  total time for checks:                    " + timerCounterExampleChecks);
  }
}
