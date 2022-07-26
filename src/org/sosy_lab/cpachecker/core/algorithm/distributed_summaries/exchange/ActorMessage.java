// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
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
import com.google.common.base.MoreObjects;
import com.google.common.base.Throwables;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;

/**
 * Immutable communication entity for the actor model. Messages cannot be created with the
 * constructor as they have to contain different information depending on their type. Therefore,
 * this class provides static methods to create messages of a certain type.
 */
public class ActorMessage implements Comparable<ActorMessage> {

  private final int targetNodeNumber;
  private final String uniqueBlockId;
  private final MessageType type;
  // forwards an immutable hashmap
  private final Payload payload;
  private final Instant timestamp;

  /**
   * A message is the interface of communication of {@link
   * org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.BlockSummaryActor}
   *
   * @param pType the type of the message
   * @param pUniqueBlockId the id of the worker/block that sends this message
   * @param pTargetNodeNumber the location from which this message originated from
   * @param pPayload a map that will be transformed into JSON.
   */
  private ActorMessage(
      MessageType pType, String pUniqueBlockId, int pTargetNodeNumber, Payload pPayload) {
    targetNodeNumber = pTargetNodeNumber;
    type = pType;
    payload = pPayload;
    uniqueBlockId = pUniqueBlockId;
    // when the message was created
    timestamp = Instant.now();
  }

  // For deserialization only
  private ActorMessage(
      MessageType pType,
      String pUniqueBlockId,
      int pTargetNodeNumber,
      Instant pTimestamp,
      Payload pPayload) {
    targetNodeNumber = pTargetNodeNumber;
    type = pType;
    payload = pPayload;
    uniqueBlockId = pUniqueBlockId;
    // the deserialization does not change the timestamp
    timestamp = pTimestamp;
  }

  /**
   * Copy {@code pMessage} and replace its payload with {@code pPayload}. The message {@code
   * pMessage} remains unchanged.
   *
   * @param pMessage message to copy
   * @param pPayload new payload
   * @return new message that is a copy of {@code pMessage} with a new payload {@code pPayload}
   */
  public static ActorMessage replacePayload(ActorMessage pMessage, Payload pPayload) {
    return new ActorMessage(
        pMessage.getType(),
        pMessage.getUniqueBlockId(),
        pMessage.getTargetNodeNumber(),
        pMessage.getTimestamp(),
        pPayload);
  }

  public static ActorMessage newBlockPostCondition(
      String pUniqueBlockId,
      int pTargetNodeNumber,
      Payload pPayload,
      boolean pFull,
      boolean pReachable,
      Set<String> pVisited) {
    Payload newPayload =
        Payload.builder()
            .putAll(pPayload)
            .addEntry(Payload.FULL_PATH, Boolean.toString(pFull))
            .addEntry(Payload.VISITED, Joiner.on(",").join(pVisited))
            .addEntry(Payload.REACHABLE, Boolean.toString(pReachable))
            .build();
    return new ActorMessage(
        MessageType.BLOCK_POSTCONDITION, pUniqueBlockId, pTargetNodeNumber, newPayload);
  }

  public static ActorMessage newErrorConditionMessage(
      String pUniqueBlockId,
      int pTargetNodeNumber,
      Payload pPayload,
      boolean pFirst,
      Set<String> pVisited) {
    Payload newPayload =
        Payload.builder()
            .putAll(pPayload)
            .addEntry(Payload.FIRST, Boolean.toString(pFirst))
            .addEntry(Payload.VISITED, Joiner.on(",").join(pVisited))
            .build();
    return new ActorMessage(
        MessageType.ERROR_CONDITION, pUniqueBlockId, pTargetNodeNumber, newPayload);
  }

  public static ActorMessage newErrorConditionUnreachableMessage(
      String pUniqueBlockId, String denied) {
    return new ActorMessage(
        MessageType.ERROR_CONDITION_UNREACHABLE,
        pUniqueBlockId,
        0,
        Payload.builder().addEntry(Payload.REASON, denied).build());
  }

  public static ActorMessage newResultMessage(
      String pUniqueBlockId, int pTargetNodeNumber, Result pResult, Set<String> pVisited) {
    Payload payload =
        Payload.builder()
            .addEntry(Payload.RESULT, pResult.name())
            .addEntry(Payload.VISITED, Joiner.on(",").join(pVisited))
            .build();
    return new ActorMessage(MessageType.FOUND_RESULT, pUniqueBlockId, pTargetNodeNumber, payload);
  }

  public static ActorMessage newErrorMessage(String pUniqueBlockId, Throwable pException) {
    return new ActorMessage(
        MessageType.ERROR,
        pUniqueBlockId,
        0,
        Payload.builder()
            .addEntry(Payload.EXCEPTION, Throwables.getStackTraceAsString(pException))
            .build());
  }

  @Override
  public int compareTo(ActorMessage o) {
    return Integer.compare(type.priority, o.getType().priority);
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

  public Instant getTimestamp() {
    return timestamp;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("targetNodeNumber", targetNodeNumber)
        .add("uniqueBlockId", uniqueBlockId)
        .add("type", type)
        .add("payload", payload)
        .toString();
  }

  @Override
  public boolean equals(Object pO) {
    if (!(pO instanceof ActorMessage)) {
      return false;
    }
    ActorMessage message = (ActorMessage) pO;
    return targetNodeNumber == message.targetNodeNumber
        && Objects.equals(uniqueBlockId, message.uniqueBlockId)
        && type == message.type
        && Objects.equals(payload, message.payload);
  }

  @Override
  public int hashCode() {
    return Objects.hash(targetNodeNumber, uniqueBlockId, type, payload);
  }

  // ORDER BY PRIORITY:
  public enum MessageType {
    FOUND_RESULT(0),
    ERROR_CONDITION(1),
    ERROR_CONDITION_UNREACHABLE(1),
    BLOCK_POSTCONDITION(2),
    ERROR(0);

    private final int priority;

    MessageType(int pPriority) {
      priority = pPriority;
    }
  }

  public static class MessageConverter {

    private final ObjectMapper mapper;

    public MessageConverter() {
      mapper = new ObjectMapper();
      SimpleModule serializer =
          new SimpleModule("MessageSerializer", new Version(1, 0, 0, null, null, null));
      serializer.addSerializer(ActorMessage.class, new MessageSerializer(ActorMessage.class));
      mapper.registerModule(serializer);

      SimpleModule deserializer =
          new SimpleModule("MessageDeserializer", new Version(1, 0, 0, null, null, null));
      deserializer.addDeserializer(ActorMessage.class, new MessageDeserializer(ActorMessage.class));
      mapper.registerModule(deserializer);
    }

    public byte[] messageToJson(ActorMessage pMessage) throws IOException {
      // return mapper.writeValueAsBytes(pMessage);
      return mapper.writeValueAsBytes(pMessage);
    }

    public ActorMessage jsonToMessage(byte[] pBytes) throws IOException {
      // return mapper.readValue(pBytes, Message.class);
      return mapper.readValue(pBytes, ActorMessage.class);
    }
  }
  /** Mimics a MessageConverter but it zips messages. */
  public static class CompressedMessageConverter extends MessageConverter {

    @Override
    public byte[] messageToJson(ActorMessage pMessage) throws IOException {
      try (ByteArrayOutputStream output = new ByteArrayOutputStream();
          GZIPOutputStream writer = new GZIPOutputStream(output)) {
        byte[] message = super.messageToJson(pMessage);
        writer.write(message);
        return output.toByteArray();
      }
    }

    @Override
    public ActorMessage jsonToMessage(byte[] pBytes) throws IOException {
      try (GZIPInputStream reader = new GZIPInputStream(new ByteArrayInputStream(pBytes));
          ByteArrayOutputStream output = new ByteArrayOutputStream()) {

        byte[] buf = new byte[4096];
        while (true) {
          int n = reader.read(buf);
          if (n < 0) {
            break;
          }
          output.write(buf, 0, n);
        }
        byte[] data = output.toByteArray();
        return super.jsonToMessage(data);
      }
    }
  }

  private static class MessageDeserializer extends StdDeserializer<ActorMessage> {

    private static final long serialVersionUID = 196344175L;

    public MessageDeserializer(Class<ActorMessage> vc) {
      super(vc);
    }

    @Override
    public ActorMessage deserialize(JsonParser parser, DeserializationContext deserializer)
        throws IOException {
      ObjectCodec codec = parser.getCodec();
      JsonNode node = codec.readTree(parser);

      String uniqueBlockId = node.get("uniqueBlockId").asText();
      int nodeNumber = node.get("targetNodeNumber").asInt();
      MessageType type = MessageType.valueOf(node.get("type").asText());
      Payload payload = Payload.from(node.get("payload").asText());
      Instant timestamp = Instant.parse(node.get("timestamp").asText());
      return new ActorMessage(type, uniqueBlockId, nodeNumber, timestamp, payload);
    }
  }

  private static class MessageSerializer extends StdSerializer<ActorMessage> {

    private static final long serialVersionUID = 1324289L;

    private MessageSerializer(Class<ActorMessage> t) {
      super(t);
    }

    @Override
    public void serialize(
        ActorMessage pMessage, JsonGenerator pJsonGenerator, SerializerProvider pSerializerProvider)
        throws IOException {
      pJsonGenerator.writeStartObject();
      pJsonGenerator.writeStringField("uniqueBlockId", pMessage.getUniqueBlockId());
      pJsonGenerator.writeNumberField("targetNodeNumber", pMessage.getTargetNodeNumber());
      pJsonGenerator.writeStringField("type", pMessage.getType().name());
      pJsonGenerator.writeStringField("payload", pMessage.getPayload().toJSONString());
      pJsonGenerator.writeStringField("timestamp", pMessage.getTimestamp().toString());
      pJsonGenerator.writeEndObject();
    }
  }
}
