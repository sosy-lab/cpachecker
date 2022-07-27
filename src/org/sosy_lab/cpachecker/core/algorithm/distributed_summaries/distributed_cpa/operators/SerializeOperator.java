// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Payload;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public interface SerializeOperator {

  /**
   * Serialize an abstract state to a transportable string
   *
   * @param pState this state will be serialized
   * @return payload
   */
  Payload serialize(AbstractState pState);
}
