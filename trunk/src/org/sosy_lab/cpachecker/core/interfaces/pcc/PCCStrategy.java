// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces.pcc;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.specification.Specification;
import org.sosy_lab.cpachecker.cpa.PropertyChecker.PropertyCheckerCPA;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Interface for classes implementing proof carrying code approaches. Theses classes can be used by
 * ProofGenerator and ProofCheckAlgorithm.
 */
public interface PCCStrategy {

  /**
   * Constructs the proof/certificate from the given save overapproximation. Subsequently writes the
   * certificate to disk, etc.
   *
   * @param pReached - save overapproximation of state space
   */
  void writeProof(UnmodifiableReachedSet pReached, ConfigurableProgramAnalysis pCpa);

  /**
   * Constructs the proof/certificate from the given save overapproximation and saves it in its
   * internal data structures. Thus, certificate checking can immediately follow analysis, e.g. to
   * check correctness of analysis result.
   *
   * @param pReached - save overapproximation of state space
   * @throws InvalidConfigurationException
   *     <ul>
   *       <li>if format of abstract state does not match expectation of PCC strategy, configuration
   *           of PCC strategy
   *       <li>if class does not support direct checking of analysis result
   *     </ul>
   *
   * @throws InterruptedException if construction took longer than remaining time available for
   *     CPAchecker execution
   */
  void constructInternalProofRepresentation(
      UnmodifiableReachedSet pReached, ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException, InterruptedException;

  /**
   * Reads the certificate from disk, stream, etc. and stores it internally.
   *
   * @throws IOException if reading fails
   * @throws ClassNotFoundException if reading correct object from stream fails
   * @throws InvalidConfigurationException if format of abstract state does not match expectation of
   *     PCC strategy
   */
  void readProof() throws IOException, ClassNotFoundException, InvalidConfigurationException;

  /**
   * Checks the certificate. The certificate is not given and must be available internally, e.g.
   * because method <code>constructInternalProofRepresentation(UnfmodifableReachedSet)</code> or
   * <code>readProof()</code> has been called before.
   *
   * <p>Checks if the certificate is valid. This means it describes an overapproximation of the
   * reachable state space starting in initial state given by <code>pInitialState</code>.
   * Furthermore, the overapproximation may not violate the considered safety criteria.
   *
   * @param reachedSet - contains initial state and initial precision
   * @return true only if the certificate is valid, returns false if certificate is invalid or
   *     validity may not be checked
   * @throws CPAException if e.g. recomputation of successor or coverage check fails
   * @throws InterruptedException if thread is interrupted while checking
   */
  boolean checkCertificate(final ReachedSet reachedSet) throws CPAException, InterruptedException;

  /**
   * Ask strategy for additional statistics information which should be displayed with statistics of
   * proof generation.
   *
   * @return additional statistics which should be displayed with proof generation statistics
   */
  Collection<Statistics> getAdditionalProofGenerationStatistics();

  interface Factory {
    PCCStrategy create(
        Configuration config,
        LogManager logger,
        ShutdownNotifier shutdownNotifier,
        Path pProofFile,
        @Nullable CFA cfa,
        @Nullable Specification specification,
        @Nullable ProofChecker proofChecker,
        @Nullable PropertyCheckerCPA propertyChecker)
        throws InvalidConfigurationException;
  }
}
