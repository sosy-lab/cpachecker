// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.validation;

import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Level;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.termination.validation.SupportingInvariantsChecker.InvariantCheckResult;
import org.sosy_lab.cpachecker.core.defaults.DummyTargetState;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessV2ParserUtils;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantSetEntry;

/**
 * Validates a correctness witness for termination by executing the program. The automaton of the
 * witness checks its loop and location invariants at each visit. An execution cannot check
 * transition invariants, so for a witness with transition invariants the result is UNKNOWN.
 */
@Options(prefix = "termination.validation")
public class TerminationWitnessExecutionValidator implements Algorithm {

  @Option(
      secure = true,
      required = true,
      description =
          "configuration of the analysis that executes the program, e.g., with the InterpreterCPA."
              + " The analysis must finish only if the program terminates.")
  @FileOption(FileOption.Type.REQUIRED_INPUT_FILE)
  private Path executionConfig;

  private static final DummyTargetState DUMMY_TARGET_STATE =
      DummyTargetState.withSimpleTargetInformation("termination");

  private final Path witnessPath;
  private final CFA cfa;
  private final LogManager logger;
  private final ShutdownNotifier shutdownNotifier;

  public TerminationWitnessExecutionValidator(
      CFA pCfa,
      Configuration pConfig,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier,
      ImmutableSet<Path> pWitnessPath)
      throws InvalidConfigurationException {
    pConfig.inject(this);
    cfa = pCfa;
    logger = pLogger;
    shutdownNotifier = pShutdownNotifier;
    if (pWitnessPath.size() != 1) {
      throw new InvalidConfigurationException(
          "Expected exactly one correctness witness as input of the algorithm.");
    }
    witnessPath = pWitnessPath.iterator().next();
  }

  @Override
  public AlgorithmStatus run(ReachedSet pReachedSet) throws CPAException, InterruptedException {
    List<AbstractEntry> witnessEntries;
    try {
      witnessEntries = AutomatonWitnessV2ParserUtils.parseYAML(witnessPath);
    } catch (IOException e) {
      throw new CPAException("Invalid witness: " + e.getMessage(), e);
    }
    if (hasTransitionInvariants(witnessEntries)) {
      logger.log(Level.INFO, "An execution cannot check the transition invariants of the witness.");
      return AlgorithmStatus.UNSOUND_AND_IMPRECISE;
    }

    InvariantCheckResult result =
        SupportingInvariantsChecker.checkInvariants(
            executionConfig, witnessEntries, cfa, logger, shutdownNotifier);
    return switch (result) {
      case VALID -> {
        // The execution finished, so the program terminates
        pReachedSet.clear();
        yield AlgorithmStatus.SOUND_AND_PRECISE;
      }
      case INVALID -> {
        pReachedSet.addNoWaitlist(
            DUMMY_TARGET_STATE, pReachedSet.getPrecision(pReachedSet.getFirstState()));
        yield AlgorithmStatus.SOUND_AND_PRECISE;
      }
      case UNKNOWN -> AlgorithmStatus.UNSOUND_AND_IMPRECISE;
    };
  }

  private static boolean hasTransitionInvariants(List<AbstractEntry> pWitnessEntries) {
    for (AbstractEntry entry : pWitnessEntries) {
      if (entry instanceof InvariantSetEntry invariantSet
          && invariantSet.content.stream()
              .anyMatch(SupportingInvariantsChecker::isTransitionInvariant)) {
        return true;
      }
    }
    return false;
  }
}
