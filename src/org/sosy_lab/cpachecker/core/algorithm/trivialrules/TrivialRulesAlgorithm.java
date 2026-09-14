// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.trivialrules;

import static com.google.common.collect.ImmutableSet.toImmutableSet;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.defaults.DummyTargetState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
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

  private static final Joiner COMMA = Joiner.on(", ");

  /** The rule that decided the task, together with the answer it gave. */
  private record Decision(TrivialRule rule, RuleVerdict verdict) {}

  /**
   * What the rules found out about the propositions of the specification.
   *
   * @param proven the propositions that hold, each with the rule that proved it first
   * @param violation the rule that refuted a proposition, if there is one; a single violated
   *     proposition violates the whole specification
   */
  private record Outcome(
      ImmutableMap<Property, TrivialRule> proven, @Nullable Decision violation) {}

  private final CFA cfa;
  private final Specification specification;
  private final Configuration config;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;
  private final ImmutableSet<Property> propositions;
  private final TrivialRulesStatistics stats = new TrivialRulesStatistics();
  private final TrivialRulesWitnessExporter witnessExporter;

  public TrivialRulesAlgorithm(
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      CFA pCfa,
      Specification pSpecification,
      ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    config = pConfig;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    cfa = pCfa;
    specification = pSpecification;
    propositions =
        pSpecification.getProperties().stream()
            .filter(Property::isVerification)
            .collect(toImmutableSet());
    witnessExporter = new TrivialRulesWitnessExporter(pConfig, pCfa, pSpecification, pLogger, pCpa);
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    if (propositions.isEmpty()) {
      // The argument of a rule is about a proposition, so a rule cannot decide a specification
      // whose propositions we do not know, i.e. a task that comes without a property file.
      logger.log(
          Level.INFO,
          "The trivial rules need to know which propositions to settle, which is stated by the"
              + " property file of the task (e.g. '--spec unreach-call.prp'). The specification of"
              + " this run contains no proposition, so there is nothing to decide.");
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    }

    ProgramFacts facts = new ProgramFacts(cfa, specification, config, logger, shutdownNotifier);
    stats.setFacts(facts);
    Outcome outcome = applyRules(facts);

    if (outcome.violation() != null) {
      return reportViolation(pReachedSet, outcome.violation(), outcome.proven().keySet());
    }
    if (outcome.proven().keySet().containsAll(propositions)) {
      return reportProof(pReachedSet, outcome);
    }

    logger.logf(
        Level.INFO,
        "No trivial rule decides %s",
        describe(Sets.difference(propositions, outcome.proven().keySet())));
    return unknown(pReachedSet);
  }

  /**
   * Apply every rule that is enabled and that can settle a proposition of the specification, until
   * one of them refutes a proposition or no rule is left.
   */
  private Outcome applyRules(ProgramFacts pFacts) throws CPAException, InterruptedException {
    Map<Property, TrivialRule> proven = new LinkedHashMap<>();

    stats.totalTime().start();
    try {
      for (TrivialRule rule : TrivialRules.all()) {
        if (!isEnabled(rule) || Collections.disjoint(rule.decides(), propositions)) {
          continue;
        }
        shutdownNotifier.shutdownIfNecessary();

        RuleVerdict verdict = rule.check().apply(pFacts);
        stats.recordVerdict(rule, verdict);
        if (verdict.isAbstention()) {
          continue;
        }
        if (verdict.isViolation()) {
          settle(rule, verdict);
          // One violated proposition violates the specification, so the task is decided.
          return new Outcome(ImmutableMap.copyOf(proven), new Decision(rule, verdict));
        }
        for (Property proposition : Sets.intersection(rule.decides(), propositions)) {
          if (proven.putIfAbsent(proposition, rule) == null) {
            stats.settled(proposition, rule, verdict);
          }
        }
      }
    } finally {
      stats.totalTime().stop();
    }
    return new Outcome(ImmutableMap.copyOf(proven), null);
  }

  /** Whether the given rule is selected by the option {@code trivialrules.rules}. */
  private boolean isEnabled(TrivialRule pRule) {
    return rules.isEmpty() || rules.contains(pRule.name());
  }

  /** Report all propositions of the given rule that the specification asks about. */
  private void settle(TrivialRule pRule, RuleVerdict pVerdict) {
    for (Property proposition : Sets.intersection(pRule.decides(), propositions)) {
      stats.settled(proposition, pRule, pVerdict);
    }
  }

  /** The answer FALSE: a target state that describes the violated propositions. */
  private AlgorithmStatus reportViolation(
      ReachedSet pReachedSet, Decision pViolation, ImmutableSet<Property> pProven) {

    // A rule that refutes a proposition which another rule has proven is a bug in one of them, and
    // we must not report either answer.
    if (!Collections.disjoint(pProven, pViolation.rule().decides())) {
      logger.logf(
          Level.SEVERE,
          "Rule %s refutes a proposition that another rule has proven. This is a bug, please"
              + " report it. The answer of the trivial rules is UNKNOWN.",
          pViolation.rule().name());
      return unknown(pReachedSet);
    }

    logger.logf(
        Level.INFO, "Trivial rule %s: %s", pViolation.rule().name(), pViolation.verdict().reason());
    witnessExporter.prepareViolationWitness(
        pReachedSet,
        pViolation.verdict().violatingEdgeOrThrow(),
        DummyTargetState.withSimpleTargetInformation(targetDescription(pViolation)));
    return AlgorithmStatus.SOUND_AND_PRECISE;
  }

  /** The answer TRUE: an empty proof, because no rule reasons about the state of the program. */
  private AlgorithmStatus reportProof(ReachedSet pReachedSet, Outcome pOutcome) {
    for (Map.Entry<Property, TrivialRule> entry : pOutcome.proven().entrySet()) {
      logger.logf(
          Level.INFO,
          "Trivial rule %s proves %s",
          entry.getValue().name(),
          TrivialRules.nameOf(entry.getKey()));
    }
    witnessExporter.prepareCorrectnessWitness(pReachedSet, ImmutableListMultimap.of());
    return AlgorithmStatus.SOUND_AND_PRECISE;
  }

  /** The answer UNKNOWN: no target state, but also no proof. */
  private AlgorithmStatus unknown(ReachedSet pReachedSet) {
    pReachedSet.clear();
    return AlgorithmStatus.UNSOUND_AND_PRECISE;
  }

  /** The description of the violation, in the format that SV-COMP expects. */
  private String targetDescription(Decision pViolation) {
    return describe(Sets.intersection(pViolation.rule().decides(), propositions))
        + ": "
        + pViolation.verdict().reason();
  }

  /** The names that SV-COMP uses for the given propositions. */
  private static String describe(Set<Property> pPropositions) {
    if (pPropositions.isEmpty()) {
      return "specification";
    }
    return COMMA.join(pPropositions.stream().map(TrivialRules::nameOf).iterator());
  }

  @Override
  public void collectStatistics(Collection<Statistics> pStatsCollection) {
    pStatsCollection.add(stats);
    pStatsCollection.add(witnessExporter);
  }
}
