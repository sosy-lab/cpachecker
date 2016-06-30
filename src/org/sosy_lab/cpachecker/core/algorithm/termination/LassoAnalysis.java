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
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.solver.api.SolverContext;

import java.util.Optional;
import java.util.Set;

import javax.annotation.CheckReturnValue;

public interface LassoAnalysis {

  LassoAnalysisResult checkTermination(
      CounterexampleInfo pCounterexample, Set<CVariableDeclaration> pRelevantVariables)
      throws CPATransferException, InterruptedException;

  public interface Factory {

    public LassoAnalysis create(
        LogManager pLogger,
        Configuration pConfig,
        ShutdownNotifier pShutdownNotifier,
        SolverContext pSolverContext,
        CFA pCfa,
        TerminationStatistics pStatistics)
        throws InvalidConfigurationException;
  }

  public static class LassoAnalysisResult {

    private Optional<?> nonTerminationArgument;

    private Optional<RankingRelation> terminationArgument;

    public static LassoAnalysisResult unknown() {
      return new LassoAnalysisResult(Optional.empty(), Optional.empty());
    }

    public LassoAnalysisResult(
        Optional<?> pNonTerminationArgument, Optional<RankingRelation> pTerminationArgument) {
      checkArgument(!(pNonTerminationArgument.isPresent() && pTerminationArgument.isPresent()));
      nonTerminationArgument = checkNotNull(pNonTerminationArgument);
      terminationArgument = checkNotNull(pTerminationArgument);
    }

    public Object getNonTerminationArgument() {
      return nonTerminationArgument.get();
    }

    public RankingRelation getTerminationArgument() {
      return terminationArgument.get();
    }

    public boolean isUnknowm() {
      return !hasNonTerminationArgument() && !hasTerminationArgument();
    }

    public boolean hasNonTerminationArgument() {
      return nonTerminationArgument.isPresent();
    }

    public boolean hasTerminationArgument() {
      return terminationArgument.isPresent();
    }

    @CheckReturnValue
    public LassoAnalysisResult update(LassoAnalysisResult pOther) {
      if (isUnknowm()) {
        return pOther;
      } else if (pOther.isUnknowm()) {
        return this;
      } else if (hasNonTerminationArgument()) {
        return this;
      } else if (pOther.hasNonTerminationArgument()) {
        return pOther;

      } else { // merge
        assert hasTerminationArgument() && pOther.hasTerminationArgument();

        RankingRelation newRankingRelation =
            getTerminationArgument().merge(pOther.getTerminationArgument());
        return new LassoAnalysisResult(Optional.empty(), Optional.of(newRankingRelation));
      }
    }
  }
}
