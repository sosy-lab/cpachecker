// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.yamlwitnessexport;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.concurrent.ConcurrentCPA;
import org.sosy_lab.cpachecker.cpa.oc.OrderingConsistencyCPA;
import org.sosy_lab.cpachecker.util.CPAs;
import org.sosy_lab.cpachecker.util.yamlwitnessexport.model.InvariantSetEntry;

/**
 * Exports a correctness witness for a proof found by one of the concurrency analyses whose ARG does
 * not describe the executions of the program location by location:
 *
 * <ul>
 *   <li>The partial-order-reduction analysis ({@link ConcurrentCPA}) explores per-thread clones of
 *       the CFA and keeps only one interleaving per equivalence class, so the states at a location
 *       do not cover all executions that reach it.
 *   <li>The ordering-consistency analysis ({@link OrderingConsistencyCPA}) explores each thread on
 *       its own, ignoring the other threads, and leaves the interleavings to an SMT solver, so the
 *       states of the ARG are not invariants of the program at all.
 * </ul>
 *
 * <p>Invariants extracted from such an ARG would not be sound, so the witness holds no invariant.
 * Only the metadata is exported.
 */
public final class ConcurrentARGToCorrectnessWitness extends AbstractARGToCorrectnessWitness {

  private final String analysisName;

  private ConcurrentARGToCorrectnessWitness(
      Configuration pConfig,
      CFA pCfa,
      Specification pSpecification,
      LogManager pLogger,
      String pAnalysisName)
      throws InvalidConfigurationException {
    super(pConfig, pCfa, pSpecification, pLogger);
    analysisName = pAnalysisName;
  }

  /**
   * Returns an exporter if {@code pCpa} contains one of the concurrency analyses described in
   * {@link ConcurrentARGToCorrectnessWitness}, and empty otherwise.
   */
  public static Optional<AbstractARGToCorrectnessWitness> createIfNeeded(
      Configuration pConfig,
      CFA pCfa,
      Specification pSpecification,
      LogManager pLogger,
      ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException {
    if (CPAs.retrieveCPA(pCpa, OrderingConsistencyCPA.class) != null) {
      return Optional.of(
          new ConcurrentARGToCorrectnessWitness(
              pConfig, pCfa, pSpecification, pLogger, "the ordering-consistency analysis"));
    }
    if (CPAs.retrieveCPA(pCpa, ConcurrentCPA.class) != null) {
      return Optional.of(
          new ConcurrentARGToCorrectnessWitness(
              pConfig, pCfa, pSpecification, pLogger, "partial-order reduction"));
    }
    return Optional.empty();
  }

  /**
   * Exports an empty invariant set for every requested witness version. Since the witness does not
   * use any information from the analysis, {@code pRootState} and {@code pReachedSet} are unused.
   */
  @Override
  public void export(
      ARGState pRootState, UnmodifiableReachedSet pReachedSet, PathTemplate pOutputFileTemplate)
      throws IOException {
    logger.log(
        Level.INFO,
        "The ARG of",
        analysisName,
        "does not yield sound invariants, exporting a correctness witness without invariants.");
    for (YAMLWitnessVersion witnessVersion : ImmutableSet.copyOf(witnessVersions)) {
      exportEntries(
          new InvariantSetEntry(getMetadata(witnessVersion), ImmutableList.of()),
          pOutputFileTemplate.getPath(witnessVersion.toString()));
    }
  }
}
