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
import java.util.LinkedHashSet;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;

/**
 * Proceed operators need to return a collection of messages and a boolean indicating whether the
 * analysis should proceed. This class combines these two return types by forwarding a collection
 * and having an unmodifiable boolean attribute {@code end}.
 *
 * @see
 *     org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator
 */
public class BlockSummaryMessageProcessing extends ForwardingCollection<BlockSummaryMessage> {

  private final Collection<BlockSummaryMessage> messages;
  private final boolean end;

  private static final BlockSummaryMessageProcessing EMPTY_PROCEED =
      new BlockSummaryMessageProcessing(ImmutableList.of(), false);
  private static final BlockSummaryMessageProcessing EMPTY_STOP =
      new BlockSummaryMessageProcessing(ImmutableList.of(), true);

  private BlockSummaryMessageProcessing(Collection<BlockSummaryMessage> pMessages, boolean pEnd) {
    messages = pMessages;
    end = pEnd;
  }

  public static BlockSummaryMessageProcessing proceed() {
    return EMPTY_PROCEED;
  }

  public static BlockSummaryMessageProcessing stop() {
    return EMPTY_STOP;
  }

  public static BlockSummaryMessageProcessing proceedWith(
      Collection<BlockSummaryMessage> pMessages) {
    return new BlockSummaryMessageProcessing(pMessages, false);
  }

  public static BlockSummaryMessageProcessing stopWith(Collection<BlockSummaryMessage> pMessages) {
    return new BlockSummaryMessageProcessing(pMessages, true);
  }

  public static BlockSummaryMessageProcessing proceedWith(BlockSummaryMessage... pMessages) {
    return new BlockSummaryMessageProcessing(ImmutableList.copyOf(pMessages), false);
  }

  public static BlockSummaryMessageProcessing stopWith(BlockSummaryMessage... pMessages) {
    return new BlockSummaryMessageProcessing(ImmutableList.copyOf(pMessages), true);
  }

  public boolean end() {
    return end;
  }

  public BlockSummaryMessageProcessing merge(
      BlockSummaryMessageProcessing pProcessing, boolean removeDuplicates) {
    Collection<BlockSummaryMessage> copy =
        removeDuplicates ? new LinkedHashSet<>(messages) : new ArrayList<>(messages);
    copy.addAll(pProcessing);
    return new BlockSummaryMessageProcessing(copy, end || pProcessing.end);
  }

  @Override
  protected Collection<BlockSummaryMessage> delegate() {
    return messages;
  }
}
