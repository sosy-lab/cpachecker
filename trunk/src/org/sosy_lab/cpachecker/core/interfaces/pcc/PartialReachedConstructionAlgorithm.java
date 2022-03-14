// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces.pcc;

import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;

public interface PartialReachedConstructionAlgorithm {

  /**
   * Computes a subset of the reached set <code>pReached</code> which should be sufficient to be
   * used as certificate.
   *
   * @param pReached - analysis result, overapproximation of reachable states
   * @return a subset of <code>pReached</code>
   * @throws InvalidConfigurationException if abstract state format does not match expectations for
   *     construction
   */
  AbstractState[] computePartialReachedSet(
      UnmodifiableReachedSet pReached, ConfigurableProgramAnalysis pCpa)
      throws InvalidConfigurationException;
}
