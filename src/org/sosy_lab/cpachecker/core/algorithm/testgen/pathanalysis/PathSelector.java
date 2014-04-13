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
package org.sosy_lab.cpachecker.core.algorithm.testgen.pathanalysis;

import org.sosy_lab.cpachecker.core.algorithm.testgen.TestGenAlgorithm;
import org.sosy_lab.cpachecker.core.algorithm.testgen.iteration.PredicatePathAnalysisResult;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.util.predicates.interpolation.CounterexampleTraceInfo;

/**
 * class for selecting new paths used for {@link TestGenAlgorithm}s selection of new executions.
 */
public interface PathSelector {

  /**
   * computes a new viable execution path based on the given path.
   * The new path is a variation of the given path and fulfills the following:
   * <ul>
   * <li>givenPath.sublist(0,x) equals newPath.sublist(0,x): The paths match for a depth x. x is a value between 0 and givenPath.size()-2.</li>
   * <li>givenPath.get(x+1) and newPath.get(x+1) both have the same predecessor (in an ARGState or CFANode sense)</li>
   * <li>newPath.size() = x+1</li>
   * <li>newPath is empty if this algorithm was unable to find another viable path.</li>
   * </ul>
   * @param pExecutedPath
   * @param reachedStates
   * @return the new path and model if a valid path was found or {@link PredicatePathAnalysisResult#INVALID} otherwise.
   * @throws CPATransferException
   * @throws InterruptedException
   */
  public PredicatePathAnalysisResult findNewFeasiblePathUsingPredicates(final ARGPath pExecutedPath, final ReachedSet reachedStates) throws CPATransferException, InterruptedException;

  /**
   * performs a SMT check on the given path using the underlying solver of this strategy.
   * @param pExecutedPath
   * @return
   * @throws CPATransferException
   * @throws InterruptedException
   */
  public CounterexampleTraceInfo computePredicateCheck(final ARGPath pExecutedPath) throws CPATransferException, InterruptedException;

}
