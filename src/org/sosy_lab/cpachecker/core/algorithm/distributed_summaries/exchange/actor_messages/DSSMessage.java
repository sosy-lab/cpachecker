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
import com.google.common.base.Throwables;
import java.io.IOException;
import java.io.Serial;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.DSSMessagePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DSSActor;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DSSObserverWorker.StatusObserver;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

/**
 * Immutable communication entity for the actor model. Messages cannot be created with the
 * constructor as they have to contain different information depending on their type. Therefore,
 * this class provides static methods to create messages of a certain type. {@link DSSMessage}s are
 * the interface for communication for {@link DSSActor}s
 */
public abstract class DSSMessage implements Comparable<DSSMessage> {

  private final int targetNodeNumber;
  private final String uniqueBlockId;
  private final MessageType type;

  // forwards an immutable hashmap
  private final DSSMessagePayload payload;
  private final Instant timestamp;

  /**
   * Messages transports information between different {@link
   * org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis}.
   * Messages consist of four parts. The type decide which information is guaranteed to be part of
   * the payload. The unique block id {@code pUniqueBlockId} stores the block id from which this
   * message originates from. The target node number {@code pTargetNodeNumber} provides the unique
   * id of a {@link org.sosy_lab.cpachecker.cfa.model.CFANode}. This id is only relevant for
   * messages that actually trigger an analysis: {@link DSSPostConditionMessage}, {@link
   * DSSErrorConditionMessage}. Finally, the payload contains a map of key-value pairs that
   * transport arbitrary information.
   *
   * @param pType the type of the message
   * @param pUniqueBlockId the id of the worker/block that sends this message
   * @param pTargetNodeNumber the location from which this message originated from
   * @param pPayload a map that will be transformed into JSON.
   */
  protected DSSMessage(
      MessageType pType,
      String pUniqueBlockId,
      int pTargetNodeNumber,
      DSSMessagePayload pPayload,
      Instant pTimeStamp) {
    targetNodeNumber = pTargetNodeNumber;
    type = pType;
    payload = pPayload;
    uniqueBlockId = pUniqueBlockId;
    timestamp = pTimeStamp;
    // when the message was created
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
    if (!(payload.containsKey(DSSMessagePayload.PRECISE)
        && payload.containsKey(DSSMessagePayload.PROPERTY)
        && payload.containsKey(DSSMessagePayload.SOUND))) {
      return Optional.empty();
    }
    StatusObserver.StatusPrecise isPrecise =
        StatusObserver.StatusPrecise.valueOf((String) payload.get(DSSMessagePayload.PRECISE));
    StatusObserver.StatusPropertyChecked isPropertyChecked =
        StatusObserver.StatusPropertyChecked.valueOf(
            (String) payload.get(DSSMessagePayload.PROPERTY));
    StatusObserver.StatusSoundness isSound =
        StatusObserver.StatusSoundness.valueOf((String) payload.get(DSSMessagePayload.SOUND));
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

  public static DSSMessage newBlockPostCondition(
      String pUniqueBlockId,
      int pTargetNodeNumber,
      DSSMessagePayload pPayload,
      boolean pReachable) {
    DSSMessagePayload newPayload =
        DSSMessagePayload.builder()
            .addAllEntries(pPayload)
            .addEntry(DSSMessagePayload.REACHABLE, Boolean.toString(pReachable))
            .buildPayload();
    return new DSSPostConditionMessage(
        pUniqueBlockId, pTargetNodeNumber, newPayload, Instant.now());
  }

  public static DSSMessage newErrorConditionMessage(
      String pUniqueBlockId,
      int pTargetNodeNumber,
      DSSMessagePayload pPayload,
      boolean pFirst,
      String pOrigin) {
    DSSMessagePayload newPayload =
        DSSMessagePayload.builder()
            .addAllEntries(pPayload)
            .addEntry(DSSMessagePayload.FIRST, Boolean.toString(pFirst))
            .addEntry(DSSMessagePayload.ORIGIN, pOrigin)
            .buildPayload();
    return new DSSErrorConditionMessage(
        pUniqueBlockId, pTargetNodeNumber, newPayload, Instant.now());
  }

  public static DSSMessage newErrorConditionUnreachableMessage(
      String pUniqueBlockId, String denied) {
    return new DSSErrorConditionUnreachableMessage(
        pUniqueBlockId,
        0,
        DSSMessagePayload.builder()
            .addEntry(DSSMessagePayload.REASON, denied)
            .addEntry("readable", denied)
            .buildPayload(),
        Instant.now());
  }

  public static DSSMessage newResultMessage(
      String pUniqueBlockId, int pTargetNodeNumber, Result pResult) {
    DSSMessagePayload payload =
        DSSMessagePayload.builder()
            .addEntry(DSSMessagePayload.RESULT, pResult.name())
            .buildPayload();
    return new DSSResultMessage(pUniqueBlockId, pTargetNodeNumber, payload, Instant.now());
  }

  public static DSSMessage newErrorMessage(String pUniqueBlockId, Throwable pException) {
    return new DSSExceptionMessage(
        pUniqueBlockId,
        0,
        DSSMessagePayload.builder()
            .addEntry(DSSMessagePayload.EXCEPTION, Throwables.getStackTraceAsString(pException))
            .buildPayload(),
        Instant.now());
  }

  public static DSSMessage newStatisticsMessage(String pUniqueBlockId, Map<String, Object> pStats) {
    return new DSSStatisticsMessage(
        pUniqueBlockId,
        0,
        DSSMessagePayload.builder().addEntry(DSSMessagePayload.STATS, pStats).buildPayload(),
        Instant.now());
  }

  public String getBlockId() {
    return uniqueBlockId;
  }

  @Override
  public int compareTo(DSSMessage o) {
    return getType().compareTo(o.getType());
  }

  protected boolean extractFlag(String key, boolean defaultValue) {
    return Boolean.parseBoolean(getPayload().getOrDefault(key, defaultValue).toString());
  }

  public int getTargetNodeNumber() {
    return targetNodeNumber;
  }

  protected DSSMessagePayload getPayload() {
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
    return pO instanceof DSSMessage message
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
     * DSSResultMessage}.
     */
    FOUND_RESULT,

    /**
     * Messages of this type transport the stack trace of an exception. See {@link
     * DSSExceptionMessage}.
     */
    ERROR,

    /**
     * Messages of this type deny a previously received {@link DSSErrorConditionMessage}. See {@link
     * DSSErrorConditionUnreachableMessage}.
     */
    ERROR_CONDITION_UNREACHABLE,

    /**
     * Messages of this type transport results of a backward analysis. See {@link
     * DSSErrorConditionMessage}.
     */
    ERROR_CONDITION,

    /**
     * Messages of this type transport results of a forward analysis. See {@link
     * DSSPostConditionMessage}.
     */
    BLOCK_POSTCONDITION
  }

  public static class MessageConverter {

    private final ObjectMapper mapper;

    public MessageConverter() {
      mapper = new ObjectMapper();
      SimpleModule serializer =
          new SimpleModule("MessageSerializer", new Version(1, 0, 0, null, null, null));
      serializer.addSerializer(DSSMessage.class, new MessageSerializer(DSSMessage.class));
      mapper.registerModule(serializer);
      SimpleModule deserializer =
          new SimpleModule("MessageDeserializer", new Version(1, 0, 0, null, null, null));
      deserializer.addDeserializer(DSSMessage.class, new MessageDeserializer(DSSMessage.class));
      mapper.registerModule(deserializer);
    }

    public String messageToJson(DSSMessage pMessage) throws IOException {
      return mapper.writeValueAsString(pMessage);
    }

    public DSSMessage jsonToMessage(String pBytes) throws IOException {
      return mapper.readValue(pBytes, DSSMessage.class);
    }
  }

  private static class MessageDeserializer extends StdDeserializer<DSSMessage> {

    @Serial private static final long serialVersionUID = 196344175L;

    public MessageDeserializer(Class<DSSMessage> vc) {
      super(vc);
    }

    @Override
    public DSSMessage deserialize(JsonParser parser, DeserializationContext deserializer)
        throws IOException {
      ObjectCodec codec = parser.getCodec();
      JsonNode node = codec.readTree(parser);

      String uniqueBlockId = node.get("uniqueBlockId").asText();
      int nodeNumber = node.get("targetNodeNumber").asInt();
      MessageType type = MessageType.valueOf(node.get("type").asText());
      DSSMessagePayload payload =
          DSSMessagePayload.builder()
              .addEntriesFromJSON(node.get("payload").asText())
              .buildPayload();
      Instant timestamp = Instant.parse(node.get("timestamp").asText());

      return switch (type) {
        case FOUND_RESULT -> new DSSResultMessage(uniqueBlockId, nodeNumber, payload, timestamp);
        case ERROR -> new DSSExceptionMessage(uniqueBlockId, nodeNumber, payload, timestamp);
        case ERROR_CONDITION_UNREACHABLE ->
            new DSSErrorConditionUnreachableMessage(uniqueBlockId, nodeNumber, payload, timestamp);
        case ERROR_CONDITION ->
            new DSSErrorConditionMessage(uniqueBlockId, nodeNumber, payload, timestamp);
        case BLOCK_POSTCONDITION ->
            new DSSPostConditionMessage(uniqueBlockId, nodeNumber, payload, timestamp);
        default -> throw new AssertionError("Unknown MessageType " + type);
      };
    }
  }

  private static class MessageSerializer extends StdSerializer<DSSMessage> {

    @Serial private static final long serialVersionUID = 1324289L;

    private MessageSerializer(Class<DSSMessage> t) {
      super(t);
    }

    @Override
    public void serialize(
        DSSMessage pMessage, JsonGenerator pJsonGenerator, SerializerProvider pSerializerProvider)
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
