/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.core.algorithm.bmc;

import java.util.Optional;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;

public class FrontierExtensionResult extends ProofResult {

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
    return proofObligation.get();
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
