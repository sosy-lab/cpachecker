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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DSSMessage;

/**
 * Message processing information. This consists of information whether a block analysis should
 * proceed with its analysis ({@link #shouldProceed()}) and a collection of produced messages.
 *
 * @see
 *     org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator
 */
public class DSSMessageProcessing extends ForwardingCollection<DSSMessage> {

  private final Collection<DSSMessage> messages;
  private final boolean end;

  private static final DSSMessageProcessing EMPTY_PROCEED =
      new DSSMessageProcessing(ImmutableList.of(), false);
  private static final DSSMessageProcessing EMPTY_STOP =
      new DSSMessageProcessing(ImmutableList.of(), true);

  private DSSMessageProcessing(Collection<DSSMessage> pMessages, boolean pEnd) {
    messages = pMessages;
    end = pEnd;
  }

  public static DSSMessageProcessing proceed() {
    return EMPTY_PROCEED;
  }

  public static DSSMessageProcessing stop() {
    return EMPTY_STOP;
  }

  public static DSSMessageProcessing proceedWith(Collection<DSSMessage> pMessages) {
    return new DSSMessageProcessing(pMessages, false);
  }

  public static DSSMessageProcessing stopWith(Collection<DSSMessage> pMessages) {
    return new DSSMessageProcessing(pMessages, true);
  }

  public static DSSMessageProcessing proceedWith(DSSMessage... pMessages) {
    return new DSSMessageProcessing(ImmutableList.copyOf(pMessages), false);
  }

  public static DSSMessageProcessing stopWith(DSSMessage... pMessages) {
    return new DSSMessageProcessing(ImmutableList.copyOf(pMessages), true);
  }

  /** Returns whether the analysis should proceed. */
  public boolean shouldProceed() {
    return !end;
  }

  @Override
  public String toString() {
    return (end ? "End with " : "Start with ") + delegate();
  }

  public DSSMessageProcessing merge(
      DSSMessageProcessing pProcessingInfo, boolean removeDuplicates) {
    Collection<DSSMessage> copy = removeDuplicates ? new LinkedHashSet<>() : new ArrayList<>();

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
    return new DSSMessageProcessing(copy, end || pProcessingInfo.end);
  }

  @Override
  protected Collection<DSSMessage> delegate() {
    return messages;
  }
}
