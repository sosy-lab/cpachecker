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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.DssMessage;

/**
 * Message processing information. This consists of information whether a block analysis should
 * proceed with its analysis ({@link #shouldProceed()}) and a collection of produced messages.
 *
 * @see
 *     org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.proceed.ProceedOperator
 */
public class DssMessageProcessing extends ForwardingCollection<DssMessage> {

  private final Collection<DssMessage> messages;
  private final boolean end;

  private static final DssMessageProcessing EMPTY_PROCEED =
      new DssMessageProcessing(ImmutableList.of(), false);
  private static final DssMessageProcessing EMPTY_STOP =
      new DssMessageProcessing(ImmutableList.of(), true);

  private DssMessageProcessing(Collection<DssMessage> pMessages, boolean pEnd) {
    messages = pMessages;
    end = pEnd;
  }

  public static DssMessageProcessing proceed() {
    return EMPTY_PROCEED;
  }

  public static DssMessageProcessing stop() {
    return EMPTY_STOP;
  }

  public static DssMessageProcessing proceedWith(
      Collection<DssMessage> pMessages) {
    return new DssMessageProcessing(pMessages, false);
  }

  public static DssMessageProcessing stopWith(Collection<DssMessage> pMessages) {
    return new DssMessageProcessing(pMessages, true);
  }

  public static DssMessageProcessing proceedWith(DssMessage... pMessages) {
    return new DssMessageProcessing(ImmutableList.copyOf(pMessages), false);
  }

  public static DssMessageProcessing stopWith(DssMessage... pMessages) {
    return new DssMessageProcessing(ImmutableList.copyOf(pMessages), true);
  }

  /** Returns whether the analysis should proceed. */
  public boolean shouldProceed() {
    return !end;
  }

  @Override
  public String toString() {
    return (end ? "End with " : "Start with ") + delegate();
  }

  public DssMessageProcessing merge(
      DssMessageProcessing pProcessingInfo, boolean removeDuplicates) {
    Collection<DssMessage> copy =
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
    return new DssMessageProcessing(copy, end || pProcessingInfo.end);
  }

  @Override
  protected Collection<DssMessage> delegate() {
    return messages;
  }
}
