// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.parallel;

import java.util.Objects;
import org.sosy_lab.cpachecker.core.algorithm.components.tree.BlockNode;

public class Message {

  public enum MessageType {
    PRECONDITION,
    POSTCONDITION,
    FINISHED
  }

  private final BlockNode sender;
  private final MessageType type;
  private final String condition;

  public Message(
      MessageType pType,
      BlockNode pSender,
      String pCondition) {
    sender = pSender;
    type = pType;
    condition = pCondition;
  }

  public BlockNode getSender() {
    return sender;
  }

  public String getCondition() {
    return condition;
  }

  public MessageType getType() {
    return type;
  }

  @Override
  public String toString() {
    return "Message{" +
        "sender=" + sender +
        ", type=" + type +
        ", condition=" + condition +
        '}';
  }

  @Override
  public boolean equals(Object pO) {
    if (!(pO instanceof Message)) {
      return false;
    }
    Message message = (Message) pO;
    return Objects.equals(sender, message.sender) && type == message.type && Objects
        .equals(condition, message.condition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sender, type, condition);
  }
}
