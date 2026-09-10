// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.trivialrules;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import java.io.PrintStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.time.Timer;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Property;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

/**
 * Reports which rule decided a task and why, such that the answer can be checked by hand, and which
 * rules abstained.
 */
class TrivialRulesStatistics implements Statistics {

  private final Timer totalTime = new Timer();
  private final Map<String, String> outcomes = new LinkedHashMap<>();
  private final Map<String, String> propositions = new LinkedHashMap<>();
  private final Map<String, String> arguments = new LinkedHashMap<>();

  private @Nullable TrivialRule decidingRule = null;
  private @Nullable RuleVerdict verdict = null;
  private @Nullable ProgramFacts facts = null;

  Timer totalTime() {
    return totalTime;
  }

  void setFacts(ProgramFacts pFacts) {
    facts = pFacts;
  }

  void abstained(TrivialRule pRule) {
    outcomes.put(pRule.name(), "abstained");
  }

  void decided(TrivialRule pRule, RuleVerdict pVerdict) {
    outcomes.put(
        pRule.name(), (pVerdict.isViolation() ? "violated: " : "proven: ") + pVerdict.reason());
    if (decidingRule == null) {
      decidingRule = pRule;
      verdict = pVerdict;
    }
  }

  /** Report which rule settled the given proposition, and how. */
  void settled(Property pProposition, TrivialRule pRule, RuleVerdict pVerdict) {
    propositions.put(
        TrivialRules.nameOf(pProposition),
        (pVerdict.isViolation() ? "violated according to " : "proven by ") + pRule.name());
    arguments.put(pRule.name(), pRule.argument());
  }

  /** The rule that decided the task, if there is one. */
  @Nullable TrivialRule decidingRule() {
    return decidingRule;
  }

  /** The answer of the rule that decided the task, if there is one. */
  @Nullable RuleVerdict verdict() {
    return verdict;
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    StatisticsWriter writer =
        StatisticsWriter.writingStatisticsTo(pOut).put("Time for the trivial rules", totalTime);

    if (facts != null) {
      writer =
          writer
              .put("Reachable locations", facts.reachableNodes().size())
              .put("Reachable edges", facts.reachableEdges().size())
              .put("Constant variables", facts.constants().size())
              .put(
                  "Forced prefix of every execution",
                  facts.chain().edges().size() + " edges, ends at " + facts.chain().end())
              .put(
                  "Unknown functions without a body",
                  facts.unknownFunctions().isEmpty() ? "none" : describe(facts.unknownFunctions()));
    }

    writer = writer.put("Rules checked", outcomes.size());
    putAll(writer.beginLevel(), outcomes);

    if (!propositions.isEmpty()) {
      writer = writer.put("Propositions settled", propositions.size());
      putAll(writer.beginLevel(), propositions);
      writer = writer.put("Arguments of the rules that settled them", arguments.size());
      putAll(writer.beginLevel(), arguments);
    }
  }

  private static void putAll(StatisticsWriter pWriter, Map<String, String> pValues) {
    for (Map.Entry<String, String> value : pValues.entrySet()) {
      pWriter.put(value.getKey(), value.getValue());
    }
  }

  private static String describe(Iterable<String> pNames) {
    FluentIterable<String> names = FluentIterable.from(pNames);
    if (names.size() <= 3) {
      return Joiner.on(", ").join(names);
    }
    return Joiner.on(", ").join(names.limit(3)) + ", ... (" + (names.size() - 3) + " more)";
  }

  @Override
  public String getName() {
    return "Trivial rules";
  }
}
