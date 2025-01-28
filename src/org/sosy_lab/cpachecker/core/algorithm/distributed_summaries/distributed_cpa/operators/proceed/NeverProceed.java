// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DssMessageProcessing;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public class NeverProceed implements ProceedOperator {

  NeverProceed() {}

  @Override
  public DssMessageProcessing processForward(AbstractState pState) {
    return DssMessageProcessing.stop();
  }

  @Override
  public DssMessageProcessing processBackward(AbstractState pState) {
    return DssMessageProcessing.stop();
  }
}
