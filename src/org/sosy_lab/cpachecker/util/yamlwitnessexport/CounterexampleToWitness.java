// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import static org.sosy_lab.cpachecker.util.AbstractStates.extractStateByType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Sets;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.counterexample.CFAEdgeWithAssumptions;
import org.sosy_lab.cpachecker.core.counterexample.CounterexampleInfo;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.threading.ThreadingState;

/** Exports a counterexample of the analyzed program itself as a violation witness. */
public class CounterexampleToWitness extends AbstractCounterexampleToWitness {

  public CounterexampleToWitness(
      Configuration pConfig, CFA pCfa, Specification pSpecification, LogManager pLogger)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
  }

  /**
   * Return all CFA edges of the given path together with the threads executing them. Consecutive
   * states of an {@link ARGPath} are not necessarily connected by a single CFA edge, since an
   * analysis may handle a whole basic block in one step (cf. option
   * cpa.composite.aggregateBasicBlocks). {@link ARGPath#fullPathIterator()} resolves such holes
   * into the edges they stand for. For such edges the states enclosing the whole hole are used.
   */
  private static ImmutableList<WitnessPathStep> getPathSteps(ARGPath pPath) {
    // an analysis that does not track threads has no thread name for any step
    boolean tracksThreads = extractStateByType(pPath.getFirstState(), ThreadingState.class) != null;
    ImmutableList.Builder<WitnessPathStep> steps = ImmutableList.builder();

    for (PathIterator it = pPath.fullPathIterator(); it.hasNext(); it.advance()) {
      CFAEdge edge = it.getOutgoingEdge();
      if (!tracksThreads) {
        steps.add(new WitnessPathStep(edge, Optional.empty(), Optional.empty()));
        continue;
      }
      ARGState previousState =
          it.isPositionWithState() ? it.getAbstractState() : it.getPreviousAbstractState();
      ARGState nextState = it.getNextAbstractState();
      steps.add(
          new WitnessPathStep(
              edge,
              getCurrentThreadNameIfExists(nextState, edge),
              getNewThreadNameIfExists(nextState, previousState)));
    }

    return steps.build();
  }

  private static Optional<String> getNewThreadNameIfExists(
      ARGState pState, ARGState pPreviousState) {
    ThreadingState threadingState = extractStateByType(pState, ThreadingState.class);
    if (threadingState == null) {
      return Optional.empty();
    }

    ThreadingState previousThreadingState =
        extractStateByType(pPreviousState, ThreadingState.class);
    if (previousThreadingState == null) {
      return Optional.empty();
    }

    return Sets.difference(threadingState.getThreadIds(), previousThreadingState.getThreadIds())
        .stream()
        .findFirst();
  }

  private static Optional<String> getCurrentThreadNameIfExists(ARGState pState, CFAEdge pEdge) {
    ThreadingState threadingState = extractStateByType(pState, ThreadingState.class);
    if (threadingState == null) {
      return Optional.empty();
    }

    for (String threadId : threadingState.getThreadIds()) {
      if (threadingState
          .getThreadLocation(threadId)
          .getLocationNode()
          .equals(pEdge.getSuccessor())) {

        return Optional.of(threadId);
      }
    }

    return Optional.empty();
  }

  /**
   * Export the given counterexample to the path as a violation witness.
   *
   * @param pCex the counterexample to be exported
   * @param pPath the path to export the witness to
   * @throws IOException if writing the witness to the path is not possible
   */
  @Override
  protected void exportWitness(
      CounterexampleInfo pCex, Path pPath, YAMLWitnessVersion pWitnessVersion) throws IOException {

    ImmutableListMultimap.Builder<CFAEdge, String> edgeToAssumptionsBuilder =
        new ImmutableListMultimap.Builder<>();
    if (pCex.isPreciseCounterExample()) {
      for (CFAEdgeWithAssumptions edgeWithAssumptions : pCex.getCFAPathWithAssignments()) {
        edgeToAssumptionsBuilder.put(
            edgeWithAssumptions.getCFAEdge(),
            buildAssumptionConstraint(getAssumptions(edgeWithAssumptions)));
      }
    }

    ARGPath targetPath = pCex.getTargetPath();
    ImmutableList<WitnessPathStep> steps = getPathSteps(targetPath);
    // the target waypoint is built for the last non-blank edge, but the thread executing it is
    // taken from the very last state of the path
    CFAEdge lastEdge = violatingStep(steps).edge();

    exportEntries(
        buildViolationSequence(
            steps,
            getCurrentThreadNameIfExists(targetPath.getLastState(), lastEdge),
            edgeToAssumptionsBuilder.build(),
            pWitnessVersion),
        pPath);
  }
}
