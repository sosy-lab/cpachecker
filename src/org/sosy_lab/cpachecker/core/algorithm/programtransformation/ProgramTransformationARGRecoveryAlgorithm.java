// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.programtransformation;

import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMultimap;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.transformation.ProgramTransformationInformation;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.arg.ARGUtils;
import org.sosy_lab.cpachecker.exceptions.CPAException;

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

    public ProgramTransformationARGRecoveryAlgorithmFactory(
        Algorithm pAlgorithm,
        LogManager pLogger,
        CFA pCFA)
        throws InvalidConfigurationException {
      this(() -> pAlgorithm, pLogger, pCFA);
    }

    public ProgramTransformationARGRecoveryAlgorithmFactory(
        AlgorithmFactory pAlgorithmFactory,
        LogManager pLogger,
        CFA pCFA)
        throws InvalidConfigurationException {
      algorithmFactory = pAlgorithmFactory;
      logger = pLogger;
      nodesToProgramTransformations = pCFA.getMetadata().getNodesToProgramTransformations().orElse(
          ImmutableListMultimap.of());
    }

    @Override
    public Algorithm newInstance() {
      return new ProgramTransformationARGRecoveryAlgorithm(logger, algorithmFactory.newInstance(), nodesToProgramTransformations);
    }
  }

  private final LogManager logger;
  private final Algorithm algorithm;
  private final ImmutableMultimap<CFANode, ProgramTransformationInformation>
      nodesToProgramTransformations;

  private ProgramTransformationARGRecoveryAlgorithm(
      LogManager pLogger,
      Algorithm pAlgorithm,
      ImmutableMultimap<CFANode, ProgramTransformationInformation> pNodesToProgramTransformations) {
    logger = pLogger;
    algorithm = pAlgorithm;
    nodesToProgramTransformations = pNodesToProgramTransformations;
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
      // TODO continue
    } else {
      logger.log(Level.INFO, "Not directly called after PT-CEGAR; doing nothing");
    }

    return status;
  }
}
