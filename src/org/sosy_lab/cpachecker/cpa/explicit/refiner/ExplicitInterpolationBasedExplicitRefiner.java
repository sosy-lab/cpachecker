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
import java.util.List;

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
import org.sosy_lab.cpachecker.cpa.arg.Path;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitPrecision;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitPrecision.CegarPrecision;
import org.sosy_lab.cpachecker.cpa.explicit.refiner.utils.AssignedVariablesCollector;
import org.sosy_lab.cpachecker.cpa.explicit.refiner.utils.AssumptionVariablesCollector;
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

  protected ExplicitInterpolationBasedExplicitRefiner(
      Configuration config, PathFormulaManager
      pathFormulaManager) throws InvalidConfigurationException {
    config.inject(this);
  }

  @Override
  protected Multimap<CFANode, String> determinePrecisionIncrement(UnmodifiableReachedSet reachedSet,
      Path errorPath) throws CPAException {
    timerCounterExampleChecks.start();

    firstInterpolationPoint = null;

    numberOfRefinements++;

    Multimap<CFANode, String> increment = HashMultimap.create();

    // only do a refinement if a full-precision check shows that the path is infeasible
    if(!isPathFeasable(errorPath, HashMultimap.<CFANode, String>create())) {
      List<CFAEdge> cfaTrace = extractCFAEdgeTrace(errorPath);

      Multimap<CFANode, String> variablesToBeIgnored      = HashMultimap.create();
      Multimap<CFANode, String> referencedVariableMapping = determineReferencedVariableMapping(cfaTrace);

      for(int i = 0; i < cfaTrace.size(); i++){
        CFAEdge currentEdge = cfaTrace.get(i);

        numberOfErrorPathElements++;

        if(currentEdge instanceof CFunctionReturnEdge) {
          currentEdge = ((CFunctionReturnEdge)currentEdge).getSummaryEdge();
        }

        Collection<String> referencedVariablesAtEdge = referencedVariableMapping.get(currentEdge.getSuccessor());

        // no potentially interesting variables referenced - skip
        if(referencedVariablesAtEdge.isEmpty()) {
          continue;
        }

        // check for each variable, if ignoring it makes the error path feasible
        for(String importantVariable : referencedVariablesAtEdge) {
          // do a redundancy check against current node of the ART (and not against the error node in the ART)
          ExplicitPrecision currentPrecision = extractExplicitPrecision(reachedSet.getPrecision(edgeToState.get(cfaTrace.get(i))));
          if(isRedundant(currentPrecision.getCegarPrecision(), currentEdge, importantVariable)) {
            continue;
          }

          numberOfCounterExampleChecks++;

          // variables to ignore in the current run
          variablesToBeIgnored.put(currentEdge.getSuccessor(), importantVariable);

          // if path becomes feasible, remove it from the set of variables to be ignored,
          // and add the variable to the precision increment, also setting the interpolation point
          if(isPathFeasable(errorPath, variablesToBeIgnored)) {
            variablesToBeIgnored.remove(currentEdge.getSuccessor(), importantVariable);
            increment.put(currentEdge.getSuccessor(), importantVariable);

            if(firstInterpolationPoint == null) {
              firstInterpolationPoint = edgeToState.get(cfaTrace.get(i + 1));
            }
          }
        }
      }
    }

    timerCounterExampleChecks.stop();
    return increment;
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