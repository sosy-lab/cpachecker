// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.validation;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.Path;
import org.sosy_lab.common.Classes;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cmdline.CPAMain;
import org.sosy_lab.cpachecker.cmdline.InvalidCmdlineArgumentException;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Checks whether the (supporting) invariants of a correctness witness are invariants of the
 * program, by validating the witness with the configuration for the validation of correctness
 * witnesses.
 */
public final class SupportingInvariantsChecker {

  /** The result of checking the invariants of a witness. */
  public enum InvariantCheckResult {
    /** All invariants of the witness were proven to hold. */
    VALID,
    /** At least one invariant of the witness does not hold. */
    INVALID,
    /** The check could not decide whether the invariants hold. */
    UNKNOWN
  }

  private SupportingInvariantsChecker() {}

  /**
   * Check whether the invariants of the given correctness witness hold in the program of the given
   * CFA.
   *
   * @param pWitnessPath the correctness witness whose invariants are checked
   * @param pCfa the CFA of the program
   * @return {@link InvariantCheckResult#INVALID} if a violation of an invariant was found, {@link
   *     InvariantCheckResult#VALID} if the analysis finished soundly without finding a violation,
   *     and {@link InvariantCheckResult#UNKNOWN} otherwise
   */
  public static InvariantCheckResult checkInvariants(
      Path pWitnessPath, CFA pCfa, LogManager pLogger, ShutdownNotifier pShutdownNotifier)
      throws CPAException, InterruptedException {
    try {
      Path invariantsSpecPath =
          Classes.getCodeLocation(SupportingInvariantsChecker.class)
              .resolveSibling("config/properties/no-overflow.prp");
      Path validationConfigPath =
          Classes.getCodeLocation(SupportingInvariantsChecker.class)
              .resolveSibling("config/witnessValidation.properties");
      ImmutableList.Builder<String> arguments = ImmutableList.builder();
      arguments.add(
          "--witness",
          pWitnessPath.toString(),
          "--spec",
          invariantsSpecPath.toString(),
          "--config",
          validationConfigPath.toString(),
          "--no-output-files");
      // The configuration can only be created if the program is given on the command line
      for (Path programFile : pCfa.getFileNames()) {
        arguments.add(programFile.toString());
      }
      Configuration generationConfig =
          CPAMain.createConfiguration(arguments.build().toArray(new String[0])).configuration();
      Specification invariantSpec =
          Specification.fromFiles(
              ImmutableList.of(invariantsSpecPath, pWitnessPath),
              pCfa,
              generationConfig,
              pLogger,
              pShutdownNotifier);
      CoreComponentsFactory coreComponents =
          new CoreComponentsFactory(
              generationConfig, pLogger, pShutdownNotifier, AggregatedReachedSets.empty(), pCfa);
      ConfigurableProgramAnalysis supportingInvariantsCPA = coreComponents.createCPA(invariantSpec);
      Algorithm invariantCheckingAlgorithm =
          coreComponents.createAlgorithm(supportingInvariantsCPA, invariantSpec);

      ReachedSet reachedSet =
          coreComponents.createInitializedReachedSet(
              supportingInvariantsCPA, pCfa.getMainFunction());

      AlgorithmStatus status = invariantCheckingAlgorithm.run(reachedSet);
      if (reachedSet.wasTargetReached()) {
        return InvariantCheckResult.INVALID;
      }
      if (status.isSound() && !reachedSet.hasWaitingState()) {
        return InvariantCheckResult.VALID;
      }
      return InvariantCheckResult.UNKNOWN;
    } catch (InvalidConfigurationException | InvalidCmdlineArgumentException | IOException e) {
      throw new CPAException("Supporting invariants check failed: " + e.getMessage(), e);
    }
  }
}
