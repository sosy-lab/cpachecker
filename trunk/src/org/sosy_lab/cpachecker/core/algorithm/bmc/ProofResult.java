// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc;

import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;

public class ProofResult {

  private final boolean successful;

  private final Optional<AlgorithmStatus> earlyReturn;

  protected ProofResult(boolean pSuccessful) {
    successful = pSuccessful;
    earlyReturn = Optional.empty();
  }

  protected ProofResult(AlgorithmStatus pEarlyReturn) {
    successful = false;
    earlyReturn = Optional.of(pEarlyReturn);
  }

  public boolean isSuccessful() {
    return successful;
  }

  public Optional<AlgorithmStatus> getEarlyReturn() {
    return earlyReturn;
  }

  @Override
  public String toString() {
    if (earlyReturn.isPresent()) {
      return earlyReturn.orElseThrow().toString();
    }
    return successful ? "Successful" : "Failure";
  }
}
