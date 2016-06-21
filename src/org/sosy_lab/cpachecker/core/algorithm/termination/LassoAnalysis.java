/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.termination;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.solver.api.SolverContext;

import java.util.Optional;

public interface LassoAnalysis {

  LassoAnalysisResult checkTermination(AbstractState targetState)
      throws CPATransferException, InterruptedException;

  public interface Factory {

    public LassoAnalysis create(
        LogManager pLogger,
        Configuration pConfig,
        ShutdownNotifier pShutdownNotifier,
        SolverContext pSolverContext,
        CFA pCfa)
        throws InvalidConfigurationException;
  }

  public static class LassoAnalysisResult {

    private Optional<?> nonTerminationArgument;

    private Optional<?> terminationArgument;

    public static LassoAnalysisResult unknown() {
      return new LassoAnalysisResult(Optional.empty(), Optional.empty());
    }

    public LassoAnalysisResult(
        Optional<?> pNonTerminationArgument, Optional<?> pTerminationArgument) {
      checkArgument(!(pNonTerminationArgument.isPresent() && pTerminationArgument.isPresent()));
      nonTerminationArgument = checkNotNull(pNonTerminationArgument);
      terminationArgument = checkNotNull(pTerminationArgument);
    }

    public Optional<?> getNonTerminationArgument() {
      return nonTerminationArgument;
    }

    public Optional<?> getTerminationArgument() {
      return terminationArgument;
    }

    public boolean isUnknowm() {
      return !nonTerminationArgument.isPresent() && !terminationArgument.isPresent();
    }
  }
}
