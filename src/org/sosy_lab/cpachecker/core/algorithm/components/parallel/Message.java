// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.parallel;

import com.google.common.base.Splitter;
import java.util.List;
import java.util.Objects;

public class Message {

  public enum MessageType {
    PRECONDITION,
    POSTCONDITION,
    FINISHED
  }

  private final int targetNodeNumber;
  private final String uniqueBlockId;
  private final MessageType type;
  private final String condition;

  public Message(
      MessageType pType,
      String pUniqueBlockId,
      int pTargetNodeNumber,
      String pCondition) {
    targetNodeNumber = pTargetNodeNumber;
    type = pType;
    condition = pCondition;
    uniqueBlockId = pUniqueBlockId;
  }

  public int getTargetNodeNumber() {
    return targetNodeNumber;
  }

  public String getCondition() {
    return condition;
  }

  public MessageType getType() {
    return type;
  }

  public String getUniqueBlockId() {
    return uniqueBlockId;
  }

  @Override
  public String toString() {
    return "Message{" +
        "targetNodeNumber=" + targetNodeNumber +
        ", uniqueBlockId='" + uniqueBlockId + '\'' +
        ", type=" + type +
        ", condition='" + condition + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object comp) {
    if (!(comp instanceof Message)) {
      return false;
    }
    Message message = (Message) comp;
    return targetNodeNumber == message.targetNodeNumber && Objects.equals(uniqueBlockId,
        message.uniqueBlockId) && type == message.type && Objects.equals(condition,
        message.condition);
  }

  public static String encode(Message m) {
    String result = "NodeNumber:" + m.getTargetNodeNumber() + "\n";
    result += "BlockId:" + m.getUniqueBlockId() + "\n";
    result += "Type:" + m.getType() + "\n";
    result += "Condition:" + m.getCondition();
    return result;
  }

  public static Message decode(String s) {
    String blockId = "";
    String condition = "";
    int nodeNumber = 0;
    MessageType type = MessageType.POSTCONDITION;
    for (String line: Splitter.on("\n").limit(4).splitToList(s)) {
      List<String> separatedLine = Splitter.on(":").limit(2).splitToList(line);
      String key = separatedLine.get(0);
      String value = separatedLine.get(1);
      switch (key) {
        case "NodeNumber":
          nodeNumber = Integer.parseInt(value);
          break;
        case "Condition":
          condition = value;
          break;
        case "Type":
          type = MessageType.valueOf(value);
          break;
        case "BlockId":
          blockId = value;
          break;
        default:
          throw new IllegalArgumentException("Argument does not exist: " + key);
      }
    }
    return new Message(type, blockId, nodeNumber, condition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(targetNodeNumber, uniqueBlockId, type, condition);
  }
}
