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
import org.sosy_lab.java_smt.api.BooleanFormula;

public class Message {

  public enum MessageType {
    PRECONDITION,
    POSTCONDITION,
    FINISHED
  }

  private final BlockNode from;
  private final MessageType type;
  private final BooleanFormula condition;

  public Message(MessageType pType, BlockNode pFrom, BooleanFormula pCondition) {
    from = pFrom;
    type = pType;
    condition = pCondition;
  }

  public BlockNode getFrom() {
    return from;
  }

  public BooleanFormula getCondition() {
    return condition;
  }

  public MessageType getType() {
    return type;
  }

  @Override
  public String toString() {
    return "Message{" +
        "from=" + from +
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
    return Objects.equals(from, message.from) && type == message.type && Objects
        .equals(condition, message.condition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(from, type, condition);
  }
}
