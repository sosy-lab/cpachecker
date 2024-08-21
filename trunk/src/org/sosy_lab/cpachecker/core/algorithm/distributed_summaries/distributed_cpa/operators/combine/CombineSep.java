// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class CombineSep implements CombineOperator {

  @Override
  public List<AbstractState> combine(
      AbstractState pState1, AbstractState pState2, Precision pPrecision)
      throws CPAException, InterruptedException {
    return ImmutableList.of(pState1, pState2);
  }
}
