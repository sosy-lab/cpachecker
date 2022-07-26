// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages;

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
import com.google.common.base.Splitter;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.Payload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.ObserverBlockSummaryWorker.StatusObserver;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;

/**
 * Immutable communication entity for the actor model. Messages cannot be created with the
 * constructor as they have to contain different information depending on their type. Therefore,
 * this class provides static methods to create messages of a certain type.
 */
public abstract class ActorMessage implements Comparable<ActorMessage> {

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
  protected ActorMessage(
      MessageType pType,
      String pUniqueBlockId,
      int pTargetNodeNumber,
      Payload pPayload,
      Instant pTimeStamp) {
    targetNodeNumber = pTargetNodeNumber;
    type = pType;
    payload = pPayload;
    uniqueBlockId = pUniqueBlockId;
    timestamp = pTimeStamp;
    // when the message was created
  }

  public static ActorMessage addEntry(ActorMessage message, String key, String value) {
    return message.replacePayload(
        new Payload.Builder()
            .addAllEntries(message.getPayload())
            .addEntry(key, value)
            .buildPayload());
  }

  public static ActorMessage removeEntry(ActorMessage message, String key) {
    Map<String, String> copy = new HashMap<>(message.getPayload());
    copy.remove(key);
    return message.replacePayload(new Payload.Builder().addAllEntries(copy).buildPayload());
  }

  public final String getPayloadJSON() throws IOException {
    return payload.toJSONString();
  }

  public Optional<String> getAbstractStateString(
      Class<? extends ConfigurableProgramAnalysis> pKey) {
    return Optional.ofNullable(getPayload().get(pKey.getName()));
  }

  public Optional<AlgorithmStatus> getOptionalStatus() {
    if (!(payload.containsKey(Payload.PRECISE)
        && payload.containsKey(Payload.PROPERTY)
        && payload.containsKey(Payload.SOUND))) {
      return Optional.empty();
    }
    StatusObserver.StatusPrecise isPrecise =
        StatusObserver.StatusPrecise.valueOf(payload.get(Payload.PRECISE));
    StatusObserver.StatusPropertyChecked isPropertyChecked =
        StatusObserver.StatusPropertyChecked.valueOf(payload.get(Payload.PROPERTY));
    StatusObserver.StatusSoundness isSound =
        StatusObserver.StatusSoundness.valueOf(payload.get(Payload.SOUND));
    return Optional.of(statusOf(isPropertyChecked, isSound, isPrecise));
  }

  private AlgorithmStatus statusOf(
      StatusObserver.StatusPropertyChecked pPropertyChecked,
      StatusObserver.StatusSoundness pIsSound,
      StatusObserver.StatusPrecise pIsPrecise) {
    if (pPropertyChecked == StatusObserver.StatusPropertyChecked.UNCHECKED) {
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    }
    if (pIsSound == StatusObserver.StatusSoundness.SOUND) {
      if (pIsPrecise == StatusObserver.StatusPrecise.PRECISE) {
        return AlgorithmStatus.SOUND_AND_PRECISE;
      }
      return AlgorithmStatus.SOUND_AND_IMPRECISE;
    } else {
      if (pIsPrecise == StatusObserver.StatusPrecise.PRECISE) {
        return AlgorithmStatus.UNSOUND_AND_PRECISE;
      }
      return AlgorithmStatus.UNSOUND_AND_IMPRECISE;
    }
  }

  /**
   * Copy this message and replace its payload with {@code pPayload}. This message remains
   * unchanged.
   *
   * @param pPayload new payload
   * @return new message that is a copy of this message with a new payload {@code pPayload}
   */
  protected abstract ActorMessage replacePayload(Payload pPayload);

  public static ActorMessage newBlockPostCondition(
      String pUniqueBlockId,
      int pTargetNodeNumber,
      Payload pPayload,
      boolean pFull,
      boolean pReachable,
      Set<String> pVisited) {
    Payload newPayload =
        new Payload.Builder()
            .addAllEntries(pPayload)
            .addEntry(Payload.FULL_PATH, Boolean.toString(pFull))
            .addEntry(Payload.VISITED, Joiner.on(",").join(pVisited))
            .addEntry(Payload.REACHABLE, Boolean.toString(pReachable))
            .buildPayload();
    return new BlockPostConditionMessage(
        pUniqueBlockId, pTargetNodeNumber, newPayload, Instant.now());
  }

  public static ActorMessage newErrorConditionMessage(
      String pUniqueBlockId,
      int pTargetNodeNumber,
      Payload pPayload,
      boolean pFirst,
      Set<String> pVisited) {
    Payload newPayload =
        new Payload.Builder()
            .addAllEntries(pPayload)
            .addEntry(Payload.FIRST, Boolean.toString(pFirst))
            .addEntry(Payload.VISITED, Joiner.on(",").join(pVisited))
            .buildPayload();
    return new ErrorConditionMessage(pUniqueBlockId, pTargetNodeNumber, newPayload, Instant.now());
  }

  public static ActorMessage newErrorConditionUnreachableMessage(
      String pUniqueBlockId, String denied) {
    return new ErrorConditionUnreachableMessage(
        pUniqueBlockId,
        0,
        new Payload.Builder().addEntry(Payload.REASON, denied).buildPayload(),
        Instant.now());
  }

  public static ActorMessage newResultMessage(
      String pUniqueBlockId, int pTargetNodeNumber, Result pResult, Set<String> pVisited) {
    Payload payload =
        new Payload.Builder()
            .addEntry(Payload.RESULT, pResult.name())
            .addEntry(Payload.VISITED, Joiner.on(",").join(pVisited))
            .buildPayload();
    return new ResultMessage(pUniqueBlockId, pTargetNodeNumber, payload, Instant.now());
  }

  public static ActorMessage newErrorMessage(String pUniqueBlockId, Throwable pException) {
    return new ErrorMessage(
        pUniqueBlockId,
        0,
        new Payload.Builder()
            .addEntry(Payload.EXCEPTION, Throwables.getStackTraceAsString(pException))
            .buildPayload(),
        Instant.now());
  }

  @Override
  public int compareTo(ActorMessage o) {
    return getType().compareTo(o.getType());
  }

  protected Set<String> extractVisited() {
    ImmutableSet.Builder<String> builder = ImmutableSet.builder();
    for (String s : Splitter.on(",").split(getPayload().getOrDefault(Payload.VISITED, ""))) {
      if (!s.isBlank()) {
        builder.add(s);
      }
    }
    return builder.build();
  }

  protected boolean extractFlag(String key, boolean defaultValue) {
    return Boolean.parseBoolean(getPayload().getOrDefault(key, Boolean.toString(defaultValue)));
  }

  public int getTargetNodeNumber() {
    return targetNodeNumber;
  }

  protected Payload getPayload() {
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
    FOUND_RESULT,
    ERROR,
    ERROR_CONDITION_UNREACHABLE,
    ERROR_CONDITION,
    BLOCK_POSTCONDITION
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
      Payload payload =
          new Payload.Builder().addEntriesFromJSON(node.get("payload").asText()).buildPayload();
      Instant timestamp = Instant.parse(node.get("timestamp").asText());

      switch (type) {
        case FOUND_RESULT:
          return new ResultMessage(uniqueBlockId, nodeNumber, payload, timestamp);
        case ERROR:
          return new ErrorMessage(uniqueBlockId, nodeNumber, payload, timestamp);
        case ERROR_CONDITION_UNREACHABLE:
          return new ErrorConditionUnreachableMessage(
              uniqueBlockId, nodeNumber, payload, timestamp);
        case ERROR_CONDITION:
          return new ErrorConditionMessage(uniqueBlockId, nodeNumber, payload, timestamp);
        case BLOCK_POSTCONDITION:
          return new BlockPostConditionMessage(uniqueBlockId, nodeNumber, payload, timestamp);
        default:
          throw new AssertionError("Unknown MessageType " + type);
      }
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
