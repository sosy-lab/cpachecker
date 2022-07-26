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
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.ActorMessage;

/**
 * Proceed operators need to return a collection of messages and a boolean indicating whether the
 * analysis should proceed. This class combines these two return types by forwarding a collection
 * and having an unmodifiable boolean attribute {@code end}.
 */
public class MessageProcessing extends ForwardingCollection<ActorMessage> {

  private final Collection<ActorMessage> messages;
  private final boolean end;

  private static final MessageProcessing EMPTY_PROCEED =
      new MessageProcessing(ImmutableList.of(), false);
  private static final MessageProcessing EMPTY_STOP =
      new MessageProcessing(ImmutableList.of(), true);

  private MessageProcessing(Collection<ActorMessage> pMessages, boolean pEnd) {
    messages = pMessages;
    end = pEnd;
  }

  public static MessageProcessing proceed() {
    return EMPTY_PROCEED;
  }

  public static MessageProcessing stop() {
    return EMPTY_STOP;
  }

  public static MessageProcessing proceedWith(Collection<ActorMessage> pMessages) {
    return new MessageProcessing(pMessages, false);
  }

  public static MessageProcessing stopWith(Collection<ActorMessage> pMessages) {
    return new MessageProcessing(pMessages, true);
  }

  public static MessageProcessing proceedWith(ActorMessage... pMessages) {
    return new MessageProcessing(ImmutableList.copyOf(pMessages), false);
  }

  public static MessageProcessing stopWith(ActorMessage... pMessages) {
    return new MessageProcessing(ImmutableList.copyOf(pMessages), true);
  }

  public boolean end() {
    return end;
  }

  public MessageProcessing merge(MessageProcessing pProcessing, boolean removeDuplicates) {
    Collection<ActorMessage> copy =
        removeDuplicates ? new HashSet<>(messages) : new ArrayList<>(messages);
    copy.addAll(pProcessing);
    return new MessageProcessing(copy, end || pProcessing.end);
  }

  @Override
  protected Collection<ActorMessage> delegate() {
    return messages;
  }
}
