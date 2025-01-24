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
import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import java.io.IOException;
import java.io.Serial;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DssMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssObserverWorker.StatusObserver;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

/**
 * Immutable communication entity for the actor model. Messages cannot be created with the
 * constructor as they have to contain different information depending on their type. Therefore,
 * this class provides static methods to create messages of a certain type. {@link DssMessage}s are
 * the interface for communication for {@link
 * org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssActor}s
 */
public abstract class DssMessage implements Comparable<DssMessage> {

  private final int targetNodeNumber;
  private final String uniqueBlockId;
  private final MessageType type;

  // forwards an immutable hashmap
  private final DssMessagePayload payload;
  private final @Nullable Instant timestamp;

  /**
   * Messages transports information between different {@link
   * org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis}.
   * Messages consist of four parts. The type decide which information is guaranteed to be part of
   * the payload. The unique block id {@code pUniqueBlockId} stores the block id from which this
   * message originates from. The target node number {@code pTargetNodeNumber} provides the unique
   * id of a {@link org.sosy_lab.cpachecker.cfa.model.CFANode}. This id is only relevant for
   * messages that actually trigger an analysis: {@link DssPostConditionMessage}, {@link
   * DssViolationConditionMessage}. Finally, the payload contains a map of key-value pairs that
   * transport arbitrary information.
   *
   * @param pType the type of the message
   * @param pUniqueBlockId the id of the worker/block that sends this message
   * @param pTargetNodeNumber the location from which this message originated from
   * @param pPayload a map that will be transformed into JSON.
   */
  protected DssMessage(
      MessageType pType, String pUniqueBlockId, int pTargetNodeNumber, DssMessagePayload pPayload) {
    this(pType, pUniqueBlockId, pTargetNodeNumber, pPayload, null);
  }

  /**
   * Messages transports information between different {@link
   * org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis}.
   * Messages consist of four parts. The type decide which information is guaranteed to be part of
   * the payload. The unique block id {@code pUniqueBlockId} stores the block id from which this
   * message originates from. The target node number {@code pTargetNodeNumber} provides the unique
   * id of a {@link org.sosy_lab.cpachecker.cfa.model.CFANode}. This id is only relevant for
   * messages that actually trigger an analysis: {@link DssPostConditionMessage}, {@link
   * DssViolationConditionMessage}. Finally, the payload contains a map of key-value pairs that
   * transport arbitrary information.
   *
   * @param pType the type of the message
   * @param pUniqueBlockId the id of the worker/block that sends this message
   * @param pTargetNodeNumber the location from which this message originated from
   * @param pPayload a map that will be transformed into JSON.
   * @param pTimeStamp optional timestamp for the message. This field should only be used for
   *     debugging.
   * @deprecated for debug mode only. use {@link #DssMessage(MessageType, String, int,
   *     DssMessagePayload)} instead
   */
  @Deprecated
  protected DssMessage(
      MessageType pType,
      String pUniqueBlockId,
      int pTargetNodeNumber,
      DssMessagePayload pPayload,
      @Nullable Instant pTimeStamp) {
    targetNodeNumber = pTargetNodeNumber;
    type = pType;
    payload = pPayload;
    uniqueBlockId = pUniqueBlockId;
    timestamp = pTimeStamp;
  }

  public final String getPayloadJSON(Predicate<String> pKeyFilter) throws IOException {
    return payload.toJSONString(pKeyFilter);
  }

  public Optional<Object> getPrecision(Class<? extends Precision> pKey) {
    return Optional.ofNullable(getPayload().get(pKey.getName()));
  }

  public Optional<Object> getAbstractState(Class<? extends ConfigurableProgramAnalysis> pKey) {
    Object value = getPayload().get(pKey.getName());
    return Optional.ofNullable(value);
  }

  public Optional<AlgorithmStatus> getOptionalStatus() {
    if (!(payload.containsKey(DssMessagePayload.PRECISE)
        && payload.containsKey(DssMessagePayload.PROPERTY)
        && payload.containsKey(DssMessagePayload.SOUND))) {
      return Optional.empty();
    }
    StatusObserver.StatusPrecise isPrecise =
        StatusObserver.StatusPrecise.valueOf((String) payload.get(DssMessagePayload.PRECISE));
    StatusObserver.StatusPropertyChecked isPropertyChecked =
        StatusObserver.StatusPropertyChecked.valueOf(
            (String) payload.get(DssMessagePayload.PROPERTY));
    StatusObserver.StatusSoundness isSound =
        StatusObserver.StatusSoundness.valueOf((String) payload.get(DssMessagePayload.SOUND));
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

  public String getBlockId() {
    return uniqueBlockId;
  }

  @Override
  public int compareTo(DssMessage o) {
    return getType().compareTo(o.getType());
  }

  protected boolean extractFlag(String key, boolean defaultValue) {
    return Boolean.parseBoolean(getPayload().getOrDefault(key, defaultValue).toString());
  }

  public int getTargetNodeNumber() {
    return targetNodeNumber;
  }

  protected DssMessagePayload getPayload() {
    return payload;
  }

  public MessageType getType() {
    return type;
  }

  public String getUniqueBlockId() {
    return uniqueBlockId;
  }

  public Optional<Instant> getTimestamp() {
    return Optional.ofNullable(timestamp);
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
    return pO instanceof DssMessage message
        && targetNodeNumber == message.targetNodeNumber
        && Objects.equals(uniqueBlockId, message.uniqueBlockId)
        && type == message.type
        && Objects.equals(payload, message.payload);
  }

  @Override
  public int hashCode() {
    return Objects.hash(targetNodeNumber, uniqueBlockId, type, payload);
  }

  public enum MessageType {

    /** Sent after analysis finished to show statistics. */
    STATISTICS,

    /**
     * Messages of this type contain a final verification result verdict. See {@link
     * DssResultMessage}.
     */
    FOUND_RESULT,

    /**
     * Messages of this type transport the stack trace of an exception. See {@link
     * DssExceptionMessage}.
     */
    ERROR,

    /**
     * Messages of this type transport results of a backward analysis. See {@link
     * DssViolationConditionMessage}.
     */
    VIOLATION_CONDITION,

    /**
     * Messages of this type transport results of a forward analysis. See {@link
     * DssPostConditionMessage}.
     */
    BLOCK_POSTCONDITION
  }

  public static class MessageConverter {

    private final ObjectMapper mapper;

    public MessageConverter() {
      mapper = new ObjectMapper();
      SimpleModule serializer =
          new SimpleModule("MessageSerializer", new Version(1, 0, 0, null, null, null));
      serializer.addSerializer(DssMessage.class, new MessageSerializer(DssMessage.class));
      mapper.registerModule(serializer);
      SimpleModule deserializer =
          new SimpleModule("MessageDeserializer", new Version(1, 0, 0, null, null, null));
      deserializer.addDeserializer(DssMessage.class, new MessageDeserializer(DssMessage.class));
      mapper.registerModule(deserializer);
    }

    public String messageToJson(DssMessage pMessage) throws IOException {
      return mapper.writeValueAsString(pMessage);
    }

    public DssMessage jsonToMessage(String pBytes) throws IOException {
      return mapper.readValue(pBytes, DssMessage.class);
    }
  }

  private static class MessageDeserializer extends StdDeserializer<DssMessage> {

    @Serial private static final long serialVersionUID = 196344175L;

    public MessageDeserializer(Class<DssMessage> vc) {
      super(vc);
    }

    @Override
    public DssMessage deserialize(JsonParser parser, DeserializationContext deserializer)
        throws IOException {
      ObjectCodec codec = parser.getCodec();
      JsonNode node = codec.readTree(parser);

      String uniqueBlockId = node.get("uniqueBlockId").asText();
      int nodeNumber = node.get("targetNodeNumber").asInt();
      MessageType type = MessageType.valueOf(node.get("type").asText());
      DssMessagePayload payload =
          DssMessagePayload.builder()
              .addEntriesFromJSON(node.get("payload").asText())
              .buildPayload();
      // Messages may have a timestamp, but we only use that for debugging.
      // So we do not need to deserialize that from existing JSONs.

      return switch (type) {
        case FOUND_RESULT -> new DssResultMessage(uniqueBlockId, nodeNumber, payload);
        case ERROR -> new DssExceptionMessage(uniqueBlockId, nodeNumber, payload);
        case VIOLATION_CONDITION -> new DssViolationConditionMessage(uniqueBlockId, nodeNumber, payload);
        case BLOCK_POSTCONDITION -> new DssPostConditionMessage(uniqueBlockId, nodeNumber, payload);
        default -> throw new AssertionError("Unknown MessageType " + type);
      };
    }
  }

  private static class MessageSerializer extends StdSerializer<DssMessage> {

    @Serial private static final long serialVersionUID = 1324289L;

    private MessageSerializer(Class<DssMessage> t) {
      super(t);
    }

    @Override
    public void serialize(
        DssMessage pMessage, JsonGenerator pJsonGenerator, SerializerProvider pSerializerProvider)
        throws IOException {
      Optional<Instant> timestamp = pMessage.getTimestamp();
      pJsonGenerator.writeStartObject();
      pJsonGenerator.writeStringField("uniqueBlockId", pMessage.getUniqueBlockId());
      pJsonGenerator.writeNumberField("targetNodeNumber", pMessage.getTargetNodeNumber());
      pJsonGenerator.writeStringField("type", pMessage.getType().name());
      pJsonGenerator.writeStringField("payload", pMessage.getPayload().toJSONString());
      if (timestamp.isPresent()) {
        String timestampAsString = timestamp.orElseThrow().toString();
        pJsonGenerator.writeStringField("timestamp", timestampAsString);
      }
      pJsonGenerator.writeEndObject();
    }
  }
}
