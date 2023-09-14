// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa;

import java.io.Serial;

/**
 * This exception is thrown iff a potential counterexample is not feasible, but it could not be
 * recognized by the analysis. Forward analysis in DSS may miss some important context that is only
 * visible when computing the verification condition.
 */
public class VerificationConditionException extends Exception {

  @Serial
  private static final long serialVersionUID = -3488942813490840660L;

  public VerificationConditionException(String pMessage) {
    super(pMessage);
  }
}
