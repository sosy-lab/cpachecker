// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators;

import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;

public interface DeserializeOperator {

  /**
   * Deserialize a message to an abstract state
   *
   * @param pMessage The message that is converted to an abstract state {@link AbstractState}
   * @return An abstract state described by {@code pMessage}
   * @throws InterruptedException thrown if program is interrupted from the outside.
   */
  AbstractState deserialize(BlockSummaryMessage pMessage) throws InterruptedException;
}
