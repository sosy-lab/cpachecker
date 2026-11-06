// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.simplerules;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import java.io.PrintStream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.DummyTargetState;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.composite.CompositeState;
import org.sosy_lab.cpachecker.cpa.location.LocationStateFactory;
import org.sosy_lab.cpachecker.util.witnesses.LocationWitnessExporter;

/**
 * Records the decision of a rule and writes the witness for it when the analysis is finished.
 *
 * <p>A rule decides a task without exploring the program, so the reached set cannot describe its
 * executions. It carries only what CPAchecker reads the verdict from: a target state for a
 * violation, and a state without a successor for a proof. A task that no rule decides leaves the
 * reached set empty, which is the answer UNKNOWN and writes no witness.
 */
class SimpleRulesWitnessExporter implements Statistics {

  private final LocationWitnessExporter witnessExporter;
  private final LocationStateFactory locations;
  private final CFA cfa;

  private boolean proved = false;
  private @Nullable CFAEdge violatingEdge = null;
  private @Nullable DummyTargetState violation = null;

  SimpleRulesWitnessExporter(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    witnessExporter = new LocationWitnessExporter(pConfig, pLogger, pSpecification, pCfa);
    locations = new LocationStateFactory(pCfa, AnalysisDirection.FORWARD, pConfig);
    cfa = pCfa;
  }

  /** Report that a rule has proven the propositions of the specification. */
  void recordProof(ReachedSet pReached) {
    pReached.clear();
    pReached.add(
        new ARGState(locations.getState(cfa.getMainFunction()), null),
        SingletonPrecision.getInstance());
    // Use add() to also populate indexes of partitioned reached sets, then mark it exhausted.
    pReached.clearWaitlist();
    proved = true;
  }

  /** Report the edge whose execution a rule has proven to violate the specification. */
  void recordViolation(ReachedSet pReached, CFAEdge pEdge, DummyTargetState pTarget) {
    pReached.clear();
    ARGState root = new ARGState(locations.getState(pEdge.getPredecessor()), null);
    ARGState target =
        new ARGState(
            new CompositeState(ImmutableList.of(locations.getState(pEdge.getSuccessor()), pTarget)),
            root);
    pReached.add(root, SingletonPrecision.getInstance());
    pReached.add(target, SingletonPrecision.getInstance());
    pReached.clearWaitlist();
    violatingEdge = pEdge;
    violation = pTarget;
  }

  @Override
  public void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {
    if (pResult == Result.TRUE && proved) {
      // No rule reasons about the state of the program, so there is no invariant to offer.
      witnessExporter.writeCorrectnessWitness(ImmutableListMultimap.of(), pReached);
    } else if (pResult == Result.FALSE && violatingEdge != null) {
      witnessExporter.writeViolationWitness(violatingEdge, violation);
    }
  }

  @Override
  public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
    // This class only writes output files.
  }

  @Override
  public @Nullable String getName() {
    return null;
  }
}
