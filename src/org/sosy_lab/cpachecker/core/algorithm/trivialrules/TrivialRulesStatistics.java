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

  /** How many unknown functions are listed before the rest is summarized as a count. */
  private static final int MAX_LISTED_FUNCTIONS = 3;

  private static final Joiner COMMA = Joiner.on(", ");

  private final Timer totalTime = new Timer();

  /** The answer of every rule that was applied, by the name of the rule. */
  private final Map<String, String> outcomes = new LinkedHashMap<>();

  /** The rule that settled a proposition of the specification, by the name of the proposition. */
  private final Map<String, String> propositions = new LinkedHashMap<>();

  /** The argument of every rule that settled a proposition, by the name of the rule. */
  private final Map<String, String> arguments = new LinkedHashMap<>();

  private @Nullable ProgramFacts facts = null;

  Timer totalTime() {
    return totalTime;
  }

  void setFacts(ProgramFacts pFacts) {
    facts = pFacts;
  }

  /** Report the answer that the given rule gave for this program. */
  void recordVerdict(TrivialRule pRule, RuleVerdict pVerdict) {
    outcomes.put(pRule.name(), describe(pVerdict));
  }

  /** Report which rule settled the given proposition, and how. */
  void settled(Property pProposition, TrivialRule pRule, RuleVerdict pVerdict) {
    propositions.put(
        TrivialRules.nameOf(pProposition),
        (pVerdict.isViolation() ? "violated according to " : "proven by ") + pRule.name());
    arguments.put(pRule.name(), pRule.argument());
  }

  private static String describe(RuleVerdict pVerdict) {
    return switch (pVerdict.outcome()) {
      case ABSTAINED -> "abstained";
      case PROVEN -> "proven: " + pVerdict.reason();
      case REFUTED -> "violated: " + pVerdict.reason();
    };
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
                  facts.unknownFunctions().isEmpty()
                      ? "none"
                      : describeFunctions(facts.unknownFunctions()));
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

  private static String describeFunctions(Iterable<String> pNames) {
    FluentIterable<String> names = FluentIterable.from(pNames);
    if (names.size() <= MAX_LISTED_FUNCTIONS) {
      return COMMA.join(names);
    }
    return COMMA.join(names.limit(MAX_LISTED_FUNCTIONS))
        + ", ... ("
        + (names.size() - MAX_LISTED_FUNCTIONS)
        + " more)";
  }

  @Override
  public String getName() {
    return "Trivial rules";
  }
}
