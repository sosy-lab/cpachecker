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
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class Message {

  public enum MessageType {
    PRECONDITION,
    POSTCONDITION,
    FINISHED
  }

  private final BlockNode sender;
  private final MessageType type;
  private final BooleanFormula condition;
  private final FormulaManagerView fmgr;

  public Message(
      MessageType pType,
      BlockNode pSender,
      BooleanFormula pCondition,
      FormulaManagerView pFmgr) {
    sender = pSender;
    type = pType;
    condition = pCondition;
    fmgr = pFmgr;
  }

  public BlockNode getSender() {
    return sender;
  }

  public BooleanFormula getCondition() {
    return condition;
  }

  public MessageType getType() {
    return type;
  }

  public FormulaManagerView getFmgr() {
    return fmgr;
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
