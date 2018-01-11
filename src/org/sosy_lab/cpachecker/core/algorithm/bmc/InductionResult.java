/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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

import java.util.Collections;
import java.util.Set;
import javax.annotation.Nullable;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class InductionResult {

  private static final InductionResult SUCCESSFUL =
      new InductionResult(true, Collections.emptySet(), null, -1);

  private final boolean successful;

  private final Set<CounterexampleToInductivity> model;

  private final BooleanFormula inputAssignments;

  private final int k;

  private InductionResult(
      boolean pSuccessful,
      Set<CounterexampleToInductivity> pModel,
      @Nullable BooleanFormula pInputAssignments,
      int pK) {
    if (pSuccessful != pModel.isEmpty() || pSuccessful != (pInputAssignments == null)) {
      throw new IllegalArgumentException(
          "A model should be present if and only if induction failed.");
    }
    if (!pSuccessful && pK < 0) {
      throw new IllegalArgumentException(
          "k must not be negative for failed induction results, but is " + pK);
    }
    successful = pSuccessful;
    model = pModel;
    inputAssignments = pInputAssignments;
    k = pK;
  }

  public boolean isSuccessful() {
    return successful;
  }

  public Set<CounterexampleToInductivity> getModel() {
    if (isSuccessful()) {
      throw new IllegalStateException("A model is only present if induction failed.");
    }
    assert !model.isEmpty();
    return model;
  }

  public BooleanFormula getInputAssignments() {
    if (isSuccessful()) {
      throw new IllegalStateException("Input assignments are only present if induction failed.");
    }
    assert inputAssignments == null;
    return inputAssignments;
  }

  public int getK() {
    if (isSuccessful()) {
      throw new IllegalStateException(
          "Input-assignment length is only present if induction failed.");
    }
    return k;
  }

  public static InductionResult getSuccessful() {
    return SUCCESSFUL;
  }

  public static InductionResult getFailed(
      Set<CounterexampleToInductivity> pModel, BooleanFormula pInputAssignments, int pK) {
    return new InductionResult(false, pModel, pInputAssignments, pK);
  }
}
