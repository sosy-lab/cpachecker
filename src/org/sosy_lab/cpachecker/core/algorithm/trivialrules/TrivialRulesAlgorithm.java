// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.trivialrules;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.defaults.DummyTargetState;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.interfaces.StatisticsProvider;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Property;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Checks a specification with trivial rules: arguments that settle a proposition without reasoning
 * about what the program computes, such as "a program without loops and without recursion
 * terminates" or "a program that never allocates memory cannot leak memory".
 *
 * <p>The specification is a conjunction of propositions, so the analysis needs a rule for
 * <em>every</em> proposition in order to answer TRUE, while one rule that refutes <em>one</em>
 * proposition is enough to answer FALSE. Every rule may abstain, and the answer is UNKNOWN if the
 * propositions are not settled, so a rule never has to guess.
 *
 * <p>All rules read the CFA, so the analysis is cheap and needs no CPA. Its purpose is to be the
 * first component of a portfolio: an instant answer for the tasks that need no analysis leaves the
 * time limit to the tasks that do.
 */
@Options(prefix = "trivialrules")
public class TrivialRulesAlgorithm implements Algorithm, StatisticsProvider {

  @Option(
      secure = true,
      description =
          "Use only the trivial rules with these names, e.g. to measure a single rule. Leave empty"
              + " to use all rules that can decide a proposition of the specification. The names of"
              + " the rules are printed in the statistics.")
  private Set<String> rules = ImmutableSet.of();

  private final CFA cfa;
  private final Specification specification;
  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final TrivialRulesStatistics stats = new TrivialRulesStatistics();
  private final TrivialRulesWitnessExporter witnessExporter;

  public TrivialRulesAlgorithm(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      Specification pSpecification)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    config = pConfig;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    cfa = pCfa;
    specification = pSpecification;
    witnessExporter =
        new TrivialRulesWitnessExporter(pConfig, pCfa, pSpecification, pLogger, stats);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    ImmutableSet<Property> propositions = propositionsToSettle();
    if (propositions.isEmpty()) {
      logger.log(
          Level.INFO,
          "The trivial rules need to know which propositions to settle, which is stated by the"
              + " property file of the task (e.g. '--spec unreach-call.prp'). The specification of"
              + " this run contains no proposition, so there is nothing to decide.");
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    }
    ProgramFacts facts = new ProgramFacts(cfa, specification, config, logger, shutdownNotifier);
    stats.setFacts(facts);

    Map<Property, TrivialRule> proven = new LinkedHashMap<>();
    TrivialRule violatedBy = null;
    RuleVerdict violation = null;

    stats.totalTime().start();
    try {
      for (TrivialRule rule : TrivialRules.all()) {
        if (!rules.isEmpty() && !rules.contains(rule.name())) {
          continue;
        }
        if (Collections.disjoint(rule.decides(), propositions)) {
          continue;
        }
        shutdownNotifier.shutdownIfNecessary();

        Optional<RuleVerdict> verdict = rule.check().apply(facts);
        if (verdict.isEmpty()) {
          stats.abstained(rule);
          continue;
        }
        stats.decided(rule, verdict.orElseThrow());
        if (verdict.orElseThrow().isViolation()) {
          violatedBy = rule;
          violation = verdict.orElseThrow();
          for (Property proposition : Sets.intersection(rule.decides(), propositions)) {
            stats.settled(proposition, rule, verdict.orElseThrow());
          }
          // One violated proposition violates the specification, so the task is decided.
          break;
        }
        for (Property proposition : Sets.intersection(rule.decides(), propositions)) {
          if (proven.putIfAbsent(proposition, rule) == null) {
            stats.settled(proposition, rule, verdict.orElseThrow());
          }
        }
      }
    } finally {
      stats.totalTime().stop();
    }

    // A rule that refutes a proposition which another rule has proven is a bug in one of them, and
    // we must not report either answer.
    if (violation != null && !Collections.disjoint(proven.keySet(), violatedBy.decides())) {
      logger.logf(
          Level.SEVERE,
          "Rule %s refutes a proposition that another rule has proven. This is a bug, please"
              + " report it. The answer of the trivial rules is UNKNOWN.",
          violatedBy.name());
      return unknown(pReachedSet);
    }

    if (violation != null) {
      logger.logf(Level.INFO, "Trivial rule %s: %s", violatedBy.name(), violation.reason());
      pReachedSet.clear();
      pReachedSet.add(
          DummyTargetState.withSimpleTargetInformation(targetDescription(violatedBy, violation)),
          SingletonPrecision.getInstance());
      return AlgorithmStatus.SOUND_AND_PRECISE;
    }

    if (proven.keySet().containsAll(propositions)) {
      for (Map.Entry<Property, TrivialRule> entry : proven.entrySet()) {
        logger.logf(
            Level.INFO,
            "Trivial rule %s proves %s",
            entry.getValue().name(),
            TrivialRules.nameOf(entry.getKey()));
      }
      pReachedSet.clear();
      return AlgorithmStatus.SOUND_AND_PRECISE;
    }

    logger.logf(
        Level.INFO,
        "No trivial rule decides %s",
        Sets.difference(propositions, proven.keySet()).stream()
            .map(TrivialRules::nameOf)
            .reduce((a, b) -> a + ", " + b)
            .orElse("the specification"));
    return unknown(pReachedSet);
  }

  /** The answer UNKNOWN: no target state, but also no proof. */
  private AlgorithmStatus unknown(ReachedSet pReachedSet) {
    pReachedSet.clear();
    return AlgorithmStatus.UNSOUND_AND_PRECISE;
  }

  /**
   * The propositions that the analysis has to settle. This is empty if the specification does not
   * state them, i.e., if the task comes without a property file: the argument of a rule is about a
   * proposition, so a rule cannot decide a specification whose propositions we do not know.
   */
  private ImmutableSet<Property> propositionsToSettle() {
    return ImmutableSet.copyOf(
        specification.getProperties().stream().filter(Property::isVerification).toList());
  }

  /** The description of the violation, in the format that SV-COMP expects. */
  private String targetDescription(TrivialRule pRule, RuleVerdict pVerdict) {
    ImmutableSet<Property> refuted =
        ImmutableSet.copyOf(Sets.intersection(pRule.decides(), propositionsToSettle()));
    String propositions =
        refuted.stream()
            .map(TrivialRules::nameOf)
            .reduce((a, b) -> a + ", " + b)
            .orElse("specification");
    return propositions + ": " + pVerdict.reason();
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
    pStatsCollection.add(witnessExporter);
  }
}
