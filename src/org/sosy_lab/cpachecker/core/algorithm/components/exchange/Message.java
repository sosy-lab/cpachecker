// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.exchange;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.io.IOException;
import java.util.Objects;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class Message implements Comparable<Message> {

  @Override
  public int compareTo(Message o) {
    return 0;
    //return Integer.compare(o.type.ordinal(), type.ordinal());
  }

  // ORDER BY PRIORITY:
  public enum MessageType {
    PRECONDITION,
    POSTCONDITION,
    POSTCONDITION_UNREACHABLE,
    FOUND_RESULT,
    ERROR
  }

  private final int targetNodeNumber;
  private final String uniqueBlockId;
  private final MessageType type;
  private final String payload;
  private final String additionalInformation;

  private Message(
      MessageType pType,
      String pUniqueBlockId,
      int pTargetNodeNumber,
      String pPayload) {
    this(pType, pUniqueBlockId, pTargetNodeNumber, pPayload, "");
  }

  private Message(
      MessageType pType,
      String pUniqueBlockId,
      int pTargetNodeNumber,
      String pPayload,
      String pAdditionalInformation) {
    targetNodeNumber = pTargetNodeNumber;
    type = pType;
    payload = pPayload;
    uniqueBlockId = pUniqueBlockId;
    additionalInformation = pAdditionalInformation;
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

  public String getAdditionalInformation() {
    return additionalInformation;
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

  public static Message newPreconditionMessage(
      String pUniqueBlockId,
      int pTargetNodeNumber,
      BooleanFormula pPayload,
      FormulaManagerView pFmgr) {
    return new Message(MessageType.PRECONDITION, pUniqueBlockId, pTargetNodeNumber,
        pFmgr.dumpFormula(pPayload).toString());
  }

  public static Message newPostconditionMessage(
      String pUniqueBlockId,
      int pTargetNodeNumber,
      BooleanFormula pPayload,
      FormulaManagerView pFmgr,
      boolean first) {
    return new Message(MessageType.POSTCONDITION, pUniqueBlockId, pTargetNodeNumber,
        pFmgr.dumpFormula(pPayload).toString(), Boolean.toString(first));
  }

  public static Message newPostConditionUnreachableMessage(String pUniqueBlockId) {
    return new Message(MessageType.POSTCONDITION_UNREACHABLE, pUniqueBlockId, 0, "");
  }

  public static Message newResultMessage(
      String pUniqueBlockId,
      int pTargetNodeNumber,
      Result pResult
  ) {
    return new Message(MessageType.FOUND_RESULT, pUniqueBlockId, pTargetNodeNumber, pResult.name());
  }

  public static Message newErrorMessage(String pUniqueBlockId, Exception pException) {
    return new Message(MessageType.ERROR, pUniqueBlockId, 0, pException.getMessage());
  }

  @Override
  public int hashCode() {
    return Objects.hash(targetNodeNumber, uniqueBlockId, type, payload);
  }

  public static class MessageConverter {

    private final ObjectMapper mapper;

    public MessageConverter() {
      mapper = new ObjectMapper();
      SimpleModule serializer =
          new SimpleModule("MessageSerializer", new Version(1, 0, 0, null, null, null));
      serializer.addSerializer(Message.class, new MessageSerializer(Message.class));
      mapper.registerModule(serializer);

      SimpleModule deserializer =
          new SimpleModule("MessageDeserializer", new Version(1, 0, 0, null, null, null));
      deserializer.addDeserializer(Message.class, new MessageDeserializer(Message.class));
      mapper.registerModule(deserializer);
    }

    public String messageToJson(Message pMessage) throws JsonProcessingException {
      return mapper.writeValueAsString(pMessage).replace("\n", " ");
    }

    public Message jsonToMessage(String pJSON) throws JsonProcessingException {
      return mapper.readValue(pJSON, Message.class);
    }

  }

  private static class MessageDeserializer extends StdDeserializer<Message> {

    private static final long serialVersionUID = 196344175L;

    public MessageDeserializer(Class<Message> vc) {
      super(vc);
    }

    @Override
    public Message deserialize(JsonParser parser, DeserializationContext deserializer)
        throws IOException {
      ObjectCodec codec = parser.getCodec();
      JsonNode node = codec.readTree(parser);

      String uniqueBlockId = node.get("uniqueBlockId").asText();
      int nodeNumber = node.get("targetNodeNumber").asInt();
      MessageType type = MessageType.valueOf(node.get("type").asText());
      String payload = node.get("payload").asText();
      String additionalInfo = node.get("additionalInformation").asText();
      return new Message(type, uniqueBlockId, nodeNumber, payload, additionalInfo);
    }
  }

  private static class MessageSerializer extends StdSerializer<Message> {

    private static final long serialVersionUID = 1324289L;

    private MessageSerializer(Class<Message> t) {
      super(t);
    }

    @Override
    public void serialize(
        Message pMessage, JsonGenerator pJsonGenerator, SerializerProvider pSerializerProvider)
        throws IOException {
      pJsonGenerator.writeStartObject();
      pJsonGenerator.writeStringField("uniqueBlockId", pMessage.getUniqueBlockId());
      pJsonGenerator.writeNumberField("targetNodeNumber", pMessage.getTargetNodeNumber());
      pJsonGenerator.writeStringField("type", pMessage.getType().name());
      pJsonGenerator.writeStringField("payload", pMessage.getPayload());
      pJsonGenerator.writeStringField("additionalInformation", pMessage.getAdditionalInformation());
      pJsonGenerator.writeEndObject();
    }
  }
}
