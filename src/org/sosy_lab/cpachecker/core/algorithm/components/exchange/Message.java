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
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableSet;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;

public class Message implements Comparable<Message> {

  @Override
  public int compareTo(Message o) {
    return Integer.compare(type.priority, o.getType().priority);
  }

  // ORDER BY PRIORITY:
  public enum MessageType {
    FOUND_RESULT(1),
    ERROR_CONDITION(2),
    ERROR_CONDITION_UNREACHABLE(2),
    BLOCK_POSTCONDITION(2),
    ERROR(1);

    private final int priority;

    MessageType(int pPriority) {
      priority = pPriority;
    }
  }

  private final int targetNodeNumber;
  private final String uniqueBlockId;
  private final MessageType type;
  private final Payload payload;
  private final long timestamp;

  private Message(
      MessageType pType,
      String pUniqueBlockId,
      int pTargetNodeNumber,
      Payload pPayload) {
    targetNodeNumber = pTargetNodeNumber;
    type = pType;
    payload = pPayload;
    uniqueBlockId = pUniqueBlockId;
    timestamp = System.currentTimeMillis();
  }

  // Deserialize
  private Message(
      MessageType pType,
      String pUniqueBlockId,
      int pTargetNodeNumber,
      long pTimestamp,
      Payload pPayload) {
    targetNodeNumber = pTargetNodeNumber;
    type = pType;
    payload = pPayload;
    uniqueBlockId = pUniqueBlockId;
    timestamp = pTimestamp;
  }

  public static Message replacePayload(Message pMessage, Payload pPayload) {
    return new Message(pMessage.getType(), pMessage.getUniqueBlockId(),
        pMessage.getTargetNodeNumber(), pMessage.getTimestamp(), pPayload);
  }

  public int getTargetNodeNumber() {
    return targetNodeNumber;
  }

  public Payload getPayload() {
    return payload;
  }

  public MessageType getType() {
    return type;
  }

  public String getUniqueBlockId() {
    return uniqueBlockId;
  }

  public long getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    return "Message{" +
        "targetNodeNumber=" + targetNodeNumber +
        ", uniqueBlockId='" + uniqueBlockId + '\'' +
        ", type=" + type +
        ", payload='" + payload + '\'' +
        '}';
  }

  @Override
  public boolean equals(Object pO) {
    if (!(pO instanceof Message)) {
      return false;
    }
    Message message = (Message) pO;
    return targetNodeNumber == message.targetNodeNumber && uniqueBlockId.equals(
        message.uniqueBlockId)
        && type == message.type && payload.equals(message.payload);
  }

  @Override
  public int hashCode() {
    return Objects.hash(targetNodeNumber, uniqueBlockId, type, payload);
  }

  public static Message newBlockPostCondition(
      String pUniqueBlockId,
      int pTargetNodeNumber,
      Payload pPayload,
      boolean pFull,
      Set<String> pVisited) {
    Payload newPayload =
        Payload.builder().putAll(pPayload).addEntry(Payload.FULL_PATH, Boolean.toString(pFull))
            .addEntry(Payload.VISITED,
                Joiner.on(",").join(pVisited)).build();
    return new Message(MessageType.BLOCK_POSTCONDITION, pUniqueBlockId, pTargetNodeNumber,
        newPayload);
  }

  public static Message newErrorConditionMessage(
      String pUniqueBlockId,
      int pTargetNodeNumber,
      Payload pPayload,
      boolean pFirst,
      Set<String> pVisited) {
    Payload newPayload =
        Payload.builder().putAll(pPayload).addEntry(Payload.FIRST, Boolean.toString(pFirst))
            .addEntry(Payload.VISITED,
                Joiner.on(",").join(pVisited))
            .build();
    return new Message(MessageType.ERROR_CONDITION, pUniqueBlockId, pTargetNodeNumber,
        newPayload);
  }

  public static Message newErrorConditionUnreachableMessage(String pUniqueBlockId) {
    return new Message(MessageType.ERROR_CONDITION_UNREACHABLE, pUniqueBlockId, 0, Payload.empty());
  }

  public static Message newResultMessage(
      String pUniqueBlockId,
      int pTargetNodeNumber,
      Result pResult,
      Set<String> pVisited
  ) {
    Payload payload =
        Payload.builder().addEntry(Payload.RESULT, pResult.name())
            .addEntry(Payload.VISITED,
                Joiner.on(",").join(pVisited))
            .build();
    return new Message(MessageType.FOUND_RESULT, pUniqueBlockId, pTargetNodeNumber, payload);
  }

  public static Message newErrorMessage(String pUniqueBlockId, Exception pException) {
    ByteArrayOutputStream arrayWriter = new ByteArrayOutputStream();
    PrintWriter printer = new PrintWriter(new ByteArrayOutputStream());
    pException.printStackTrace(printer);
    return new Message(MessageType.ERROR, pUniqueBlockId, 0,
       Payload.builder().addEntry(Payload.EXCEPTION, arrayWriter.toString()).build());
  }

  public static Collection<Message> makeBroadcastReady(Message... pMessages) {
    return ImmutableSet.copyOf(pMessages);
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
      Payload payload = Payload.from(node.get("payload").asText());
      long timestamp = node.get("timestamp").asLong();
      return new Message(type, uniqueBlockId, nodeNumber, timestamp, payload);
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
      pJsonGenerator.writeStringField("payload", pMessage.getPayload().toJSONString());
      pJsonGenerator.writeNumberField("timestamp", pMessage.getTimestamp());
      pJsonGenerator.writeEndObject();
    }
  }
}
