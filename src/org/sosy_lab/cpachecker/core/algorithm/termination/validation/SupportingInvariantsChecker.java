// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination.validation;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.CoreComponentsFactory;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.AggregatedReachedSets;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.automaton.Automaton;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessV2Parser;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonWitnessV2ParserUtils;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.exceptions.RefinementFailedException;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.WitnessInvariantType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.AbstractInvariantEntry;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantEntry.InvariantRecordType;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantSetEntry;

/**
 * Checks whether the (supporting) invariants of a correctness witness are invariants of the
 * program, by running a nested analysis that validates the witness.
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
   * @param pConfigFile the configuration of the analysis that checks the invariants. The witness is
   *     converted into an automaton according to this configuration.
   * @param pWitnessPath the correctness witness whose invariants are checked
   * @param pCfa the CFA of the program
   * @return {@link InvariantCheckResult#INVALID} if a violation of an invariant was found, {@link
   *     InvariantCheckResult#VALID} if the analysis finished soundly without finding a violation,
   *     and {@link InvariantCheckResult#UNKNOWN} otherwise
   */
  public static InvariantCheckResult checkInvariants(
      Path pConfigFile,
      Path pWitnessPath,
      CFA pCfa,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier)
      throws CPAException, InterruptedException {
    List<AbstractEntry> witnessEntries;
    try {
      witnessEntries = AutomatonWitnessV2ParserUtils.parseYAML(pWitnessPath);
    } catch (IOException e) {
      throw new CPAException("Supporting invariants check failed: " + e.getMessage(), e);
    }
    return checkInvariants(
        pConfigFile, withoutTransitionInvariants(witnessEntries), pCfa, pLogger, pShutdownNotifier);
  }

  /**
   * Removes the transition invariants from the given entries of a witness. They are not checked
   * here, but the automaton of the witness has a transition without assumptions for each of them,
   * besides the transitions for the invariants at the same location. So the automaton branches at
   * each visit of such a location, which can make the check infeasible.
   */
  private static ImmutableList<AbstractEntry> withoutTransitionInvariants(
      List<AbstractEntry> pWitnessEntries) {
    return transformedImmutableListCopy(
        pWitnessEntries,
        entry ->
            entry instanceof InvariantSetEntry invariantSet
                ? new InvariantSetEntry(
                    invariantSet.getMetadata(),
                    invariantSet.content.stream()
                        .filter(invariant -> !isTransitionInvariant(invariant))
                        .collect(ImmutableList.toImmutableList()))
                : entry);
  }

  private static boolean isTransitionInvariant(AbstractInvariantEntry pInvariant) {
    return WitnessInvariantType.of(InvariantRecordType.fromKeyword(pInvariant.getType()))
        .map(WitnessInvariantType::isTransitionInvariant)
        .orElse(false);
  }

  /**
   * Check whether the invariants of the given entries of a correctness witness hold in the program
   * of the given CFA.
   *
   * @param pConfigFile the configuration of the analysis that checks the invariants. The witness is
   *     converted into an automaton according to this configuration.
   * @param pWitnessEntries the entries of the correctness witness whose invariants are checked
   * @param pCfa the CFA of the program
   * @return {@link InvariantCheckResult#INVALID} if a violation of an invariant was found, {@link
   *     InvariantCheckResult#VALID} if the analysis finished soundly without finding a violation,
   *     and {@link InvariantCheckResult#UNKNOWN} otherwise
   */
  public static InvariantCheckResult checkInvariants(
      Path pConfigFile,
      List<AbstractEntry> pWitnessEntries,
      CFA pCfa,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier)
      throws CPAException, InterruptedException {
    try {
      Configuration invariantsCheckConfig =
          Configuration.builder().loadFromFile(pConfigFile).build();
      Automaton witnessAutomaton =
          new AutomatonWitnessV2Parser(invariantsCheckConfig, pLogger, pShutdownNotifier, pCfa)
              .parseAutomaton(pWitnessEntries);
      return checkSpecification(
          invariantsCheckConfig,
          Specification.fromAutomata(ImmutableList.of(witnessAutomaton)),
          pCfa,
          pLogger,
          pShutdownNotifier);
    } catch (InvalidConfigurationException | IOException e) {
      throw new CPAException("Supporting invariants check failed: " + e.getMessage(), e);
    }
  }

  /**
   * Run the analysis of the given configuration with the given specification, which contains the
   * automaton of the witness whose invariants are checked.
   */
  private static InvariantCheckResult checkSpecification(
      Configuration pInvariantsCheckConfig,
      Specification pInvariantSpec,
      CFA pCfa,
      LogManager pLogger,
      ShutdownNotifier pShutdownNotifier)
      throws InvalidConfigurationException, CPAException, InterruptedException {
    CoreComponentsFactory coreComponents =
        new CoreComponentsFactory(
            pInvariantsCheckConfig,
            pLogger,
            pShutdownNotifier,
            AggregatedReachedSets.empty(),
            pCfa);
    ConfigurableProgramAnalysis supportingInvariantsCPA = coreComponents.createCPA(pInvariantSpec);
    Algorithm invariantCheckingAlgorithm =
        coreComponents.createAlgorithm(supportingInvariantsCPA, pInvariantSpec);

    ReachedSet reachedSet =
        coreComponents.createInitializedReachedSet(supportingInvariantsCPA, pCfa.getMainFunction());

    AlgorithmStatus status;
    try {
      status = invariantCheckingAlgorithm.run(reachedSet);
    } catch (RefinementFailedException e) {
      return InvariantCheckResult.UNKNOWN;
    }
    // An interrupted analysis may look as if it finished, so it must not be reported as valid
    pShutdownNotifier.shutdownIfNecessary();
    if (reachedSet.wasTargetReached()) {
      return InvariantCheckResult.INVALID;
    }
    if (status.isSound() && !reachedSet.hasWaitingState()) {
      return InvariantCheckResult.VALID;
    }
    return InvariantCheckResult.UNKNOWN;
  }
}
