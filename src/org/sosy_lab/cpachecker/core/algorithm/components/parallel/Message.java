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
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;

public class Message {

  public enum MessageType {
    PRECONDITION,
    POSTCONDITION,
    FINISHED
  }

  private final int targetNodeNumber;
  private final String uniqueBlockId;
  private final MessageType type;
  private final String payload;

  private Message(
      MessageType pType,
      String pUniqueBlockId,
      int pTargetNodeNumber,
      String pPayload) {
    targetNodeNumber = pTargetNodeNumber;
    type = pType;
    payload = pPayload;
    uniqueBlockId = pUniqueBlockId;
  }

  public int getTargetNodeNumber() {
    return targetNodeNumber;
  }

  public String getPayload() {
    return payload;
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
        ", condition='" + payload + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object comp) {
    if (!(comp instanceof Message)) {
      return false;
    }
    Message message = (Message) comp;
    return targetNodeNumber == message.targetNodeNumber && Objects.equals(uniqueBlockId,
        message.uniqueBlockId) && type == message.type && Objects.equals(payload,
        message.payload);
  }

  public static String encode(Message m) {
    String result = "NodeNumber:" + m.getTargetNodeNumber() + "\n";
    result += "BlockId:" + m.getUniqueBlockId() + "\n";
    result += "Type:" + m.getType() + "\n";
    result += "Payload:" + m.getPayload();
    return result;
  }

  public static Message decode(String s) {
    String blockId = "";
    String payload = "";
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
        case "Payload":
          payload = value;
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
    return new Message(type, blockId, nodeNumber, payload);
  }

  @Override
  public int hashCode() {
    return Objects.hash(targetNodeNumber, uniqueBlockId, type, payload);
  }

  public static class FinishMessage extends Message {

    public FinishMessage(
        String pUniqueBlockId,
        int pTargetNodeNumber,
        Result pResult) {
      super(MessageType.FINISHED, pUniqueBlockId, pTargetNodeNumber, pResult.name());
    }

  }

  public static class ConditionMessage extends Message {

    public ConditionMessage(
        MessageType pType,
        String pUniqueBlockId,
        int pTargetNodeNumber,
        String pPayload) {
      super(pType, pUniqueBlockId, pTargetNodeNumber, pPayload);
    }
  }
}
