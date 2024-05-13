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
 * Message processing information. This consists of information whether a block analysis should
 * proceed with its analysis ({@link #shouldProceed()}) and a collection of produced messages.
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

  /** Returns whether the analysis should proceed. */
  public boolean shouldProceed() {
    return !end;
  }

  @Override
  public String toString() {
    return (end ? "End with " : "Start with ") + delegate();
  }

  public BlockSummaryMessageProcessing merge(
      BlockSummaryMessageProcessing pProcessingInfo, boolean removeDuplicates) {
    Collection<BlockSummaryMessage> copy =
        removeDuplicates ? new LinkedHashSet<>() : new ArrayList<>();

    // never merge messages of different proceed types
    // proceed messages of one DCPA may corrupt the analysis, if another DCPA wants to stop
    if (shouldProceed() == pProcessingInfo.shouldProceed()) {
      copy.addAll(messages);
      copy.addAll(pProcessingInfo);
    } else {
      if (!shouldProceed()) {
        copy.addAll(messages);
      } else {
        copy.addAll(pProcessingInfo);
      }
    }
    return new BlockSummaryMessageProcessing(copy, end || pProcessingInfo.end);
  }

  @Override
  protected Collection<BlockSummaryMessage> delegate() {
    return messages;
  }
}
