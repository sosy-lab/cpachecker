// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.programtransformation;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import java.util.Collection;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.transformation.ProgramTransformationInformation;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGCPA;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.cpa.location.LocationCPA;
import org.sosy_lab.cpachecker.cpa.location.LocationStateFactory;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;

/**
 * Algorithm for reversing the ARG to match the original source code after a ProgramTransformationCEGARAlgorithm.
 */
@SuppressWarnings("unused")
public class ProgramTransformationARGRecoveryAlgorithm implements Algorithm {

  public static class ProgramTransformationARGRecoveryAlgorithmFactory
      implements AlgorithmFactory {

    private final AlgorithmFactory algorithmFactory;
    private final LogManager logger;
    private final ImmutableMultimap<CFANode, ProgramTransformationInformation> nodesToProgramTransformations;
    LocationStateFactory locFac;

    public ProgramTransformationARGRecoveryAlgorithmFactory(
        Algorithm pAlgorithm,
        ConfigurableProgramAnalysis cpa,
        LogManager pLogger,
        CFA pCFA)
        throws InvalidConfigurationException {
      this(() -> pAlgorithm, cpa, pLogger, pCFA);
    }

    public ProgramTransformationARGRecoveryAlgorithmFactory(
        AlgorithmFactory pAlgorithmFactory,
        ConfigurableProgramAnalysis pCPA,
        LogManager pLogger,
        CFA pCFA) {
      algorithmFactory = pAlgorithmFactory;
      logger = pLogger;
      nodesToProgramTransformations = pCFA.getMetadata().getNodesToProgramTransformations().orElse(
          ImmutableListMultimap.of());
      locFac = ((ARGCPA) pCPA).retrieveWrappedCpa(LocationCPA.class).getStateFactory();
    }

    @Override
    public Algorithm newInstance() {
      return new ProgramTransformationARGRecoveryAlgorithm(logger, locFac, algorithmFactory.newInstance(), nodesToProgramTransformations);
    }
  }

  private final LogManager logger;
  private final LocationStateFactory locationStateFactory;
  private final Algorithm algorithm;
  private final ImmutableMultimap<CFANode, ProgramTransformationInformation>
      nodesToProgramTransformations;
  private final ImmutableMap<CFANode, ProgramTransformationInformation> nodeMap;

  private ProgramTransformationARGRecoveryAlgorithm(
      LogManager pLogger,
      LocationStateFactory pLocationStateFactory,
      Algorithm pAlgorithm,
      ImmutableMultimap<CFANode, ProgramTransformationInformation> pNodesToProgramTransformations) {
    logger = pLogger;
    locationStateFactory = pLocationStateFactory;
    algorithm = pAlgorithm;
    nodesToProgramTransformations = pNodesToProgramTransformations;
    ImmutableMap.Builder<CFANode, ProgramTransformationInformation> nodeMapBuilder = new ImmutableMap.Builder<>();
    for (ProgramTransformationInformation programTransformation :
        nodesToProgramTransformations.values()) {
      for (CFANode node : programTransformation.subCFA().allNodes()) {
        nodeMapBuilder.put(node, programTransformation);
      }
    }
    nodeMap = nodeMapBuilder.build();
  }

  @Override
  public AlgorithmStatus run(ReachedSet reached) throws CPAException, InterruptedException {

    AlgorithmStatus status = AlgorithmStatus.SOUND_AND_PRECISE;

    // run algorithm
    status = status.update(algorithm.run(reached));

    // does the ARG need to be adjusted?
    if (algorithm instanceof ProgramTransformationCEGARAlgorithm ptCEGARAlgorithm) {
      logger.log(Level.FINE, "Reversing the ARG after possibly using program transformations for verification");
      assert ARGUtils.checkARG(reached) : "ARG and reached set do not match before ARG reversal";

      boolean changed = true;
      while (changed) {
        changed = false;

        // TODO revert a single program transformation
        Optional<AbstractState> stateBeforeEntering = detectNextProgramTransformation(reached.getFirstState());
        if(stateBeforeEntering.isPresent()) {
          revertProgramTransformation(stateBeforeEntering.orElseThrow(), reached);
          // TODO Comment out for now
          //changed = true;
        }
      }

    } else {
      logger.log(Level.INFO, "Not directly called after PT-CEGAR; doing nothing");
    }

    return status;
  }

  /**
   * Iterating depth-first through an ARG and returns the first State before entering a Program transformation or Optional.empty if none is found.
   *
   * @param pState the current state of the ARG
   * @return AbstractState before the next program transformation or Optional.empty
   */
  private Optional<AbstractState> detectNextProgramTransformation(AbstractState pState) {
    ARGState argState = (ARGState) pState;
    Collection<ARGState> children = argState.getChildren();
    // only nodes in nodesToProgramTransformations can have child states in a different program transformation
    if (nodesToProgramTransformations.containsKey(AbstractStates.extractLocation(pState))) {
      if (children.isEmpty()) {
        // if no children exist return Optional.empty for this branch
        return Optional.empty();
      } else {
        ProgramTransformationInformation parentProgramTransformation = nodeMap.getOrDefault(AbstractStates.extractLocation(pState), null);
        for (ARGState child : children) {
          ProgramTransformationInformation childProgramTransformation = nodeMap.getOrDefault(AbstractStates.extractLocation(child), null);
          if (parentProgramTransformation != childProgramTransformation) {
            // entering different program transformation
            return Optional.of(pState);
          }
        }
      }
    }
    // this state does not enter a program transformation, check child states depth-first
    for (ARGState child : children) {
      Optional<AbstractState> maybeStateBeforeProgramTransformation = detectNextProgramTransformation(child);
      if (maybeStateBeforeProgramTransformation.isPresent()) {
        return maybeStateBeforeProgramTransformation;
      }
    }
    // If all branches return Optional.empty, then there are no program transformation states left in the arg
    return Optional.empty();
  }

  private void revertProgramTransformation(AbstractState pState, ReachedSet reached) {
    ARGState parentARGState = (ARGState) pState;
    ProgramTransformationInformation parentProgramTransformation = nodeMap.getOrDefault(AbstractStates.extractLocation(pState), null);
    ARGState childARGState = null;
    ProgramTransformationInformation childProgramTransformation = null;

    for (ARGState child : parentARGState.getChildren()) {
      childProgramTransformation = nodeMap.getOrDefault(AbstractStates.extractLocation(child), null);
      if (parentProgramTransformation != childProgramTransformation) {
        childARGState = child;
        break;
      }
    }
    assert (childARGState != null && childProgramTransformation != null);
    assert (nodesToProgramTransformations.containsKey(AbstractStates.extractLocation(pState)));
    assert (nodeMap.get(AbstractStates.extractLocation(childARGState)) == childProgramTransformation);

    switch (childProgramTransformation.subCFA().programTransformationEnum()) {
      case TAIL_RECURSION_ELIMINATION:
        childProgramTransformation.programTransformationRecovery().revertProgramTransformation(
            parentARGState,
            childARGState,
            // TODO use Optional
            parentProgramTransformation == null ? null : parentProgramTransformation.subCFA(),
            childProgramTransformation.subCFA(),
            reached,
            locationStateFactory
        );
        break;
      default:
        break;
    }
  }
}
