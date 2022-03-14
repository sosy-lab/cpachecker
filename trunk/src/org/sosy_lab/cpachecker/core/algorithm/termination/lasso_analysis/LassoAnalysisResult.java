// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.lasso_analysis;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.errorprone.annotations.CheckReturnValue;
import de.uni_freiburg.informatik.ultimate.lassoranker.nontermination.NonTerminationArgument;
import java.util.Optional;

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
    return nonTerminationArgument.orElseThrow();
  }

  public RankingRelation getTerminationArgument() {
    return terminationArgument.orElseThrow();
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
