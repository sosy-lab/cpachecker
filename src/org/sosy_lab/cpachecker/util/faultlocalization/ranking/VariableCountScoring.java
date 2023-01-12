// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.faultlocalization.ranking;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.sosy_lab.cpachecker.util.faultlocalization.Fault;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultContribution;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultRankingUtils;
import org.sosy_lab.cpachecker.util.faultlocalization.FaultScoring;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.FaultInfo;
import org.sosy_lab.cpachecker.util.faultlocalization.appendables.RankInfo;

public class VariableCountScoring implements FaultScoring {

  private final Map<Fault, Double> scores = new HashMap<>();
  private final Map<String, Integer> tokenCount = new HashMap<>();

  @Override
  public RankInfo scoreFault(Fault fault) {
    String usedTokens =
        "["
            + Joiner.on(", ")
                .join(
                    Sets.intersection(
                        tokenCount.keySet(), FaultRankingUtils.findTokensInFault(fault)))
            + "]";
    return FaultInfo.rankInfo(
        "Statements contains symbols also used in other faults: " + usedTokens,
        scores.getOrDefault(fault, 0d));
  }

  @Override
  public void balancedScore(Collection<Fault> faults) {
    for (Fault fault : faults) {
      for (FaultContribution faultContribution : fault) {
        String ast = faultContribution.correspondingEdge().getRawStatement();
        handleToken(ast, s -> tokenCount.merge(s, 1, Integer::sum));
      }
    }
    // never give points if this is the only set containing a symbol
    tokenCount.keySet().forEach(key -> tokenCount.merge(key, -1, Integer::sum));
    for (String s : ImmutableSet.copyOf(tokenCount.keySet())) {
      if (tokenCount.getOrDefault(s, 0) <= 0) {
        tokenCount.remove(s);
        continue;
      }
      if (Pattern.matches("[0-9]+", s)) {
        tokenCount.remove(s);
        continue;
      }
    }
    for (Fault fault : faults) {
      String joined = Joiner.on(" ").join(FaultRankingUtils.findTokensInFault(fault));
      handleToken(
          joined, s -> scores.merge(fault, (double) tokenCount.getOrDefault(s, 0), Double::sum));
    }
    FaultScoring.super.balancedScore(faults);
  }

  private void handleToken(String ast, Function<String, ?> handler) {
    ast = ast.replaceAll(FaultRankingUtils.NON_VARIABLE_TOKENS, " ");
    for (String s : Splitter.on(FaultRankingUtils.BLANK_CHARACTERS).split(ast)) {
      if (s.isBlank()) {
        continue;
      }
      s = s.replaceAll(FaultRankingUtils.NON_VARIABLE_TOKENS, " ");
      handler.apply(s);
    }
  }
}
