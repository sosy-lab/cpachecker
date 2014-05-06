/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.interfaces.pcc;

import java.io.IOException;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.reachedset.ReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * Interface for classes implementing proof carrying code approaches. Theses classes can be used by ProofGenerator and
 * ProofCheckAlgorithm.
 */
public interface PCCStrategy {

  /**
   * Constructs the proof/certificate from the given save overapproximation. Subsequently writes the certificate to disk, etc.
   *
   * @param pReached - save overapproximation of state space
   */
  public void writeProof(UnmodifiableReachedSet pReached);

  /**
   * Constructs the proof/certificate from the given save overapproximation and saves it in its internal data structures.
   * Thus, certificate checking can immediately follow analysis, e.g. to check correctness of analysis result.
   *
   * @param pReached - save overapproximation of state space
   * @throws InvalidConfigurationException <ul>
   * <li>if format of abstract state does not match expectation of PCC strategy, configuration of PCC strategy</li>
   * <li>if class does not support direct checking of analysis result</li></ul>
   */
  public void constructInternalProofRepresentation(UnmodifiableReachedSet pReached) throws InvalidConfigurationException;

  /**
   * Reads the certificate from disk, stream, etc. and stores it internally.
   *
   * @throws IOException if reading fails
   * @throws ClassNotFoundException if reading correct object from stream fails
   * @throws InvalidConfigurationException if format of abstract state does not match expectation of PCC strategy
   */
  public void readProof() throws IOException, ClassNotFoundException, InvalidConfigurationException;

  /**
   * Checks the certificate. The certificate is not given and must be available internally, e.g. because method
   * <code>constructInternalProofRepresentation(UnfmodifableReachedSet)</code> or <code>readProof()</code> has
   * been called before.
   *
   * Checks if the certificate is valid. This means it describes an overapproximation of the reachable state space
   * starting in initial state given by <code>pInitialState</code>. Furthermore, the overapproximation
   * may not violate the considered safety criteria.
   *
   * @param reachedSet - contains initial state and initial precision
   * @return true only if the certificate is valid, returns false if certificate is invalid or validity may not be checked
   * @throws CPAException if e.g. recomputation of successor or coverage check fails
   * @throws InterruptedException if thread is interrupted while checking
   */
  public boolean checkCertificate(final ReachedSet reachedSet) throws CPAException, InterruptedException;

}
