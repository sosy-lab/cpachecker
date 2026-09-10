// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.execution;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.SetMultimap;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ExpressionTreeReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.ExecutionToYAMLWitness;

/**
 * Collects the information that {@link ExecutionCPA} needs for its witnesses and writes them when
 * the analysis is finished.
 *
 * <p>For a correctness witness we collect all assignments that the execution has at loop heads and
 * in front of function calls; their disjunction is an invariant of the location, because the
 * program has only this one execution. For a violation witness we only need the location whose
 * execution violates the specification.
 */
@Options(prefix = "cpa.execution")
class ExecutionWitnessExporter implements Statistics {

  @Option(
      secure = true,
      name = "witness",
      description =
          "The file to write the witness to. On the result TRUE a correctness witness with the"
              + " invariants of the execution is written, on the result FALSE a violation witness"
              + " that points to the violated location. Witnesses are written in version 2.0."
              + " Leave empty to not export any witness.")
  @FileOption(FileOption.Type.OUTPUT_FILE)
  private @Nullable Path witnessFile = Path.of("witness.yml");

  @Option(
      secure = true,
      description =
          "Maximum number of different assignments to collect per location for the invariants of a"
              + " correctness witness. If a location has more assignments than this, no invariant"
              + " is exported for it, because an incomplete disjunction would be unsound. Use -1"
              + " for no limit (which needs memory proportional to the length of the execution).")
  private int maxAssignmentsPerLocation = 100;

  private final ExecutionToYAMLWitness exporter;
  private final LogManager logger;

  private final SetMultimap<CFANode, ExpressionTreeReportingState> loopInvariants =
      LinkedHashMultimap.create();
  private final SetMultimap<CFANode, ExpressionTreeReportingState> locationInvariants =
      LinkedHashMultimap.create();

  /** Locations with more assignments than we are willing to collect. */
  private final Set<CFANode> abandonedLocations = new HashSet<>();

  private @Nullable CFAEdge violatingEdge = null;

  ExecutionWitnessExporter(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    logger = pLogger;
    exporter = new ExecutionToYAMLWitness(pConfig, pCfa, pSpecification, pLogger);
  }

  /** Whether {@link #observe} needs to be called at all. */
  boolean collectsInvariants() {
    return witnessFile != null;
  }

  /**
   * Report the state that the execution had at the given location. Locations that are neither a
   * loop head nor a function call are ignored.
   */
  void observe(CFANode pNode, AbstractState pState) {
    final SetMultimap<CFANode, ExpressionTreeReportingState> invariants;
    if (pNode.isLoopStart()) {
      invariants = loopInvariants;
    } else if (pNode.getNumLeavingEdges() == 1
        && pNode.getLeavingEdge(0) instanceof FunctionCallEdge) {
      invariants = locationInvariants;
    } else {
      return;
    }
    if (abandonedLocations.contains(pNode)) {
      return;
    }

    for (ExpressionTreeReportingState state :
        AbstractStates.asIterable(pState).filter(ExpressionTreeReportingState.class)) {
      invariants.put(pNode, state);
    }
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
    if (witnessFile == null) {
      return;
    }
    try {
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
          exporter.exportCorrectnessWitness(loopInvariants, locationInvariants, witnessFile);
        }
        case FALSE -> {
          if (violatingEdge == null) {
            logger.log(
                Level.WARNING,
                "Cannot export a violation witness because the violated location is unknown.");
          } else {
            exporter.exportViolationWitness(violatingEdge, witnessFile);
          }
        }
        default -> {}
      }
    } catch (IOException e) {
      logger.logUserException(Level.WARNING, e, "Could not write the witness to " + witnessFile);
    } catch (InterruptedException e) {
      logger.log(Level.WARNING, "Witness export was interrupted.");
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
