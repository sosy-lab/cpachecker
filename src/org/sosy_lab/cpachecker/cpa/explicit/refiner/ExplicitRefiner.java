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

import org.sosy_lab.common.Pair;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.Path;
import org.sosy_lab.cpachecker.cpa.explicit.refiner.utils.ExplictPathChecker;
import org.sosy_lab.cpachecker.exceptions.CPAException;

import com.google.common.collect.Multimap;

@Options(prefix="cpa.explict.refiner")
abstract class ExplicitRefiner {
  @Option(description="whether or not to always use the inital node as starting point for the next re-exploration of the ARG")
  boolean useInitialNodeAsRestartingPoint = true;

  /**
   * the ART element, from where to cut-off the subtree, and restart the analysis
   */
  protected ARGState firstInterpolationPoint = null;

  protected HashMap<CFAEdge, ARGState> edgeToState = new HashMap<CFAEdge, ARGState>();

  abstract protected Multimap<CFANode, String> determinePrecisionIncrement(
      final UnmodifiableReachedSet reachedSet,
      final Path errorPath) throws CPAException, InterruptedException;

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
   * This method checks if the given path is feasible, when not tracking the given set of variables.
   *
   * @param path the path to check
   * @param variablesToBeIgnored the variables to ignore
   * @return true, if the path is feasible, else false
   * @throws CPAException if the path check gets interrupted
   */
  protected boolean isPathFeasable(Path path, Multimap<CFANode, String> variablesToBeIgnored) throws CPAException {
    try {
      // create a new ExplicitPathChecker, which does not track any of the given variables
      ExplictPathChecker checker = new ExplictPathChecker();

      return checker.checkPath(path, variablesToBeIgnored);
    }
    catch (InterruptedException e) {
      throw new CPAException("counterexample-check failed: ", e);
    }
  }

  protected List<CFAEdge> extractCFAEdgeTrace(Path path) {
    edgeToState = new HashMap<CFAEdge, ARGState>();
    List<CFAEdge> cfaTrace = new ArrayList<CFAEdge>();
    for(Pair<ARGState, CFAEdge> pathElement : path){
      // expand any multi-edge
      if(pathElement.getSecond() instanceof MultiEdge) {
        for(CFAEdge singleEdge : (MultiEdge)pathElement.getSecond()) {
          cfaTrace.add(singleEdge);
          edgeToState.put(singleEdge, pathElement.getFirst());
        }
      }
      else {
        cfaTrace.add(pathElement.getSecond());
        edgeToState.put(pathElement.getSecond(), pathElement.getFirst());
      }
    }

    return cfaTrace;
  }

  abstract public void printStatistics(PrintStream out, Result result, ReachedSet reached);
}