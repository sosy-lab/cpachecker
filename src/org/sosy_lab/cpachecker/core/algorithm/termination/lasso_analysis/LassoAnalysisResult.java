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
package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import de.uni_freiburg.informatik.ultimate.lassoranker.nontermination.NonTerminationArgument;
import java.util.Optional;
import com.google.errorprone.annotations.CheckReturnValue;

public class LassoAnalysisResult {

  private final Optional<NonTerminationArgument> nonTerminationArgument;

  private final Optional<RankingRelation> terminationArgument;

  public static LassoAnalysisResult unknown() {
    return new LassoAnalysisResult(Optional.empty(), Optional.empty());
  }

  public static LassoAnalysisResult fromNonTerminationArgument(
      NonTerminationArgument nonTerminationArgument) {
    return new LassoAnalysisResult(Optional.of(nonTerminationArgument), Optional.empty());
  }

  public static LassoAnalysisResult fromTerminationArgument(RankingRelation terminationArgument) {
    return new LassoAnalysisResult(Optional.empty(), Optional.of(terminationArgument));
  }

  private LassoAnalysisResult(
      Optional<NonTerminationArgument> pNonTerminationArgument,
      Optional<RankingRelation> pTerminationArgument) {
    checkArgument(!(pNonTerminationArgument.isPresent() && pTerminationArgument.isPresent()));
    nonTerminationArgument = checkNotNull(pNonTerminationArgument);
    terminationArgument = checkNotNull(pTerminationArgument);
  }

  public NonTerminationArgument getNonTerminationArgument() {
    return nonTerminationArgument.get();
  }

  public RankingRelation getTerminationArgument() {
    return terminationArgument.get();
  }

  public boolean isUnknown() {
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
    if (isUnknown()) {
      return pOther;
    } else if (pOther.isUnknown()) {
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
