// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.components.parallel;

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

public class Message {

  public enum MessageType {
    PRECONDITION,
    POSTCONDITION,
    FINISHED,
    STALE
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
      FormulaManagerView pFmgr) {
    return new Message(MessageType.POSTCONDITION, pUniqueBlockId, pTargetNodeNumber,
        pFmgr.dumpFormula(pPayload).toString());
  }

  public static Message newFinishMessage(
      String pUniqueBlockId,
      int pTargetNodeNumber,
      Result pResult) {
    return new Message(MessageType.FINISHED, pUniqueBlockId, pTargetNodeNumber, pResult.name());
  }

  public static Message newStaleMessage(
      String pUniqueBlockId,
      boolean pIsStale) {
    return new Message(MessageType.STALE, pUniqueBlockId, 0, Boolean.toString(pIsStale));
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
      return mapper.writeValueAsString(pMessage);
    }

    public Message jsonToMessage(String pJSON) throws JsonProcessingException {
      return mapper.readValue(pJSON, Message.class);
    }

  }

  private static class MessageDeserializer extends StdDeserializer<Message> {

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
      return new Message(type, uniqueBlockId, nodeNumber, payload);
    }
  }

  private static class MessageSerializer extends StdSerializer<Message> {

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
      pJsonGenerator.writeEndObject();
    }
  }

}
