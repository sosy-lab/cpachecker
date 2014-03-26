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
package org.sosy_lab.cpachecker.core.algorithm.testgen.iteration;

import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.PredicatedAnalysisPropertyViolationException;

/**
 *
 */
public interface TestGenIterationStrategy {

  /**
   *
   * @param result
   */
  public void updateIterationModelForNextIteration(PredicatePathAnalysisResult result);

  public TestGenIterationStrategy.IterationModel getModel();

  public boolean runAlgorithm() throws PredicatedAnalysisPropertyViolationException, CPAException, InterruptedException;

  /**
   *
   */
  public class IterationModel{
    private Algorithm algorithm;
    private ReachedSet globalReached;
    private ReachedSet localReached;

    public IterationModel(Algorithm pAlgorithm, ReachedSet pGlobalReached, ReachedSet pLocalReached) {
      super();
      algorithm = pAlgorithm;
      globalReached = pGlobalReached;
      localReached = pLocalReached;
    }

    public Algorithm getAlgorithm() {
      return algorithm;
    }

    public ReachedSet getGlobalReached() {
      return globalReached;
    }

    public ReachedSet getLocalReached() {
      return localReached;
    }

    public void setAlgorithm(Algorithm pAlgorithm) {
      algorithm = pAlgorithm;
    }

    public void setGlobalReached(ReachedSet pGlobalReached) {
      globalReached = pGlobalReached;
    }

    public void setLocalReached(ReachedSet pLocalReached) {
      localReached = pLocalReached;
    }

  }

  public AbstractState getLastState();

  public void initializeModel(ReachedSet pReachedSet);

}
