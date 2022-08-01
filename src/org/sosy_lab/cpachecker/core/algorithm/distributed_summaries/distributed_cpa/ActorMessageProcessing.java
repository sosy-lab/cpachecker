// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa;

import com.google.common.collect.ForwardingCollection;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;

/**
 * Proceed operators need to return a collection of messages and a boolean indicating whether the
 * analysis should proceed. This class combines these two return types by forwarding a collection
 * and having an unmodifiable boolean attribute {@code end}.
 *
 * @see
 *     org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator
 */
public class ActorMessageProcessing extends ForwardingCollection<BlockSummaryMessage> {

  private final Collection<BlockSummaryMessage> messages;
  private final boolean end;

  private static final ActorMessageProcessing EMPTY_PROCEED =
      new ActorMessageProcessing(ImmutableList.of(), false);
  private static final ActorMessageProcessing EMPTY_STOP =
      new ActorMessageProcessing(ImmutableList.of(), true);

  private ActorMessageProcessing(Collection<BlockSummaryMessage> pMessages, boolean pEnd) {
    messages = pMessages;
    end = pEnd;
  }

  public static ActorMessageProcessing proceed() {
    return EMPTY_PROCEED;
  }

  public static ActorMessageProcessing stop() {
    return EMPTY_STOP;
  }

  public static ActorMessageProcessing proceedWith(Collection<BlockSummaryMessage> pMessages) {
    return new ActorMessageProcessing(pMessages, false);
  }

  public static ActorMessageProcessing stopWith(Collection<BlockSummaryMessage> pMessages) {
    return new ActorMessageProcessing(pMessages, true);
  }

  public static ActorMessageProcessing proceedWith(BlockSummaryMessage... pMessages) {
    return new ActorMessageProcessing(ImmutableList.copyOf(pMessages), false);
  }

  public static ActorMessageProcessing stopWith(BlockSummaryMessage... pMessages) {
    return new ActorMessageProcessing(ImmutableList.copyOf(pMessages), true);
  }

  public boolean end() {
    return end;
  }

  public ActorMessageProcessing merge(
      ActorMessageProcessing pProcessing, boolean removeDuplicates) {
    Collection<BlockSummaryMessage> copy =
        removeDuplicates ? new HashSet<>(messages) : new ArrayList<>(messages);
    copy.addAll(pProcessing);
    return new ActorMessageProcessing(copy, end || pProcessing.end);
  }

  @Override
  protected Collection<BlockSummaryMessage> delegate() {
    return messages;
  }
}
