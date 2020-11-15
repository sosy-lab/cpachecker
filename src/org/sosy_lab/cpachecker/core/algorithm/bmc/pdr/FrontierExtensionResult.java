// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.bmc.pdr;

import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.bmc.ProofResult;

class FrontierExtensionResult extends ProofResult {

  private static final FrontierExtensionResult SUCCESS = new FrontierExtensionResult();

  private final Optional<ProofObligation> proofObligation;

  private FrontierExtensionResult(AlgorithmStatus pEarlyReturn) {
    super(pEarlyReturn);
    proofObligation = Optional.empty();
  }

  private FrontierExtensionResult(ProofObligation pProofObligation) {
    super(false);
    proofObligation = Optional.of(pProofObligation);
  }

  private FrontierExtensionResult() {
    super(true);
    proofObligation = Optional.empty();
  }

  public ProofObligation getProofObligation() {
    return proofObligation.orElseThrow();
  }

  public static FrontierExtensionResult getSuccess() {
    return SUCCESS;
  }

  public static FrontierExtensionResult getFailure(ProofObligation pProofObligation) {
    return new FrontierExtensionResult(pProofObligation);
  }

  public static FrontierExtensionResult earlyReturn(AlgorithmStatus pStatus) {
    return new FrontierExtensionResult(pStatus);
  }
}
