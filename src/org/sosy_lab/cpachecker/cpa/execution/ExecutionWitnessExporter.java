// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.execution;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.DummyTargetState;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.witnesses.LocationWitnessExporter;

/**
 * Collects the information that {@link ExecutionCPA} needs for its witnesses and writes them when
 * the analysis is finished.
 *
 * <p>For a correctness witness we collect all assignments that the execution has at loop heads and
 * function entries and in front of function calls; their disjunction is an invariant of the
 * location, because the program has only this one execution. For a violation witness we only need
 * the location whose execution violates the specification.
 */
@Options(prefix = "cpa.execution")
class ExecutionWitnessExporter implements Statistics {

  @Option(
      secure = true,
      description =
          "Maximum number of different assignments to collect per location for the invariants of a"
              + " correctness witness. If a location has more assignments than this, no invariant"
              + " is exported for it, because an incomplete disjunction would be unsound. Use -1"
              + " for no limit (which needs memory proportional to the length of the execution).")
  private int maxAssignmentsPerLocation = 100;

  private final LocationWitnessExporter exporter;
  private final LogManager logger;

  private final SetMultimap<CFANode, ImmutableList<ExpressionTreeReportingState>> invariants =
      LinkedHashMultimap.create();

  /** Locations with more assignments than we are willing to collect. */
  private final Set<CFANode> abandonedLocations = new HashSet<>();

  private @Nullable CFAEdge violatingEdge = null;

  ExecutionWitnessExporter(
      Configuration pConfig,
      CFA pCfa,
      Specification pSpecification,
      LogManager pLogger,
      ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;
    exporter = new LocationWitnessExporter(pConfig, pLogger, pCpa, pSpecification, pCfa);
  }

  /** Whether {@link #observe} needs to be called at all. */
  boolean collectsInvariants() {
    return exporter.isYamlWitnessExportEnabled();
  }

  /**
   * Report the state that the execution had at the given location. Locations that are neither a
   * loop head, function entry, nor a function call are ignored.
   */
  void observe(CFANode pNode, AbstractState pState) {
    if (!pNode.isLoopStart()
        && !(pNode instanceof FunctionEntryNode)
        && !(pNode.getNumLeavingEdges() == 1
            && pNode.getLeavingEdge(0) instanceof FunctionCallEdge)) {
      return;
    }
    if (abandonedLocations.contains(pNode)) {
      return;
    }

    // Keep each observation together: components describe a conjunction, visits a disjunction.
    // Do not retain execution states, call stacks, or links to preceding observations.
    invariants.put(
        pNode,
        AbstractStates.asIterable(pState).filter(ExpressionTreeReportingState.class).toList());
    if (maxAssignmentsPerLocation >= 0
        && invariants.get(pNode).size() > maxAssignmentsPerLocation) {
      // We cannot export a subset of the assignments, this would be an unsound invariant.
      abandonedLocations.add(pNode);
      invariants.removeAll(pNode);
    }
  }

  /** Report the edge whose execution violates the specification. */
  void reportViolation(CFAEdge pEdge) {
    if (violatingEdge == null) {
      violatingEdge = pEdge;
    }
  }

  @Override
  public void writeOutputFiles(Result pResult, UnmodifiableReachedSet pReached) {
    if (!exporter.isYamlWitnessExportEnabled()) {
      return;
    }
    ReachedSet witnessReached = exporter.createReachedSet();
    switch (pResult) {
      case TRUE -> {
        if (!abandonedLocations.isEmpty()) {
          logger.log(
              Level.INFO,
              "The correctness witness has no invariant for",
              abandonedLocations.size(),
              "location(s) because the execution had more than",
              maxAssignmentsPerLocation,
              "different assignments there.");
        }
        exporter.prepareCorrectnessWitness(witnessReached, invariants);
      }
      case FALSE -> {
        if (violatingEdge == null) {
          logger.log(
              Level.WARNING,
              "Cannot export a violation witness because the violated location is unknown.");
          return;
        }
        exporter.prepareViolationWitness(
            witnessReached, violatingEdge, DummyTargetState.withoutTargetInformation());
      }
      default -> {
        return;
      }
    }
    exporter.writeYamlWitnesses(pResult, witnessReached);
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
