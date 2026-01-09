// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssStatisticsMessage.StatisticsKey;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

public abstract class DssMessage {

  public enum DssMessageType {
    POST_CONDITION,
    VIOLATION_CONDITION,
    EXCEPTION,
    RESULT,
    STATISTIC
  }

  private static class DssMessageProxy {
    private final ImmutableMap<String, String> header;
    private final ImmutableMap<String, String> content;

    @JsonCreator
    DssMessageProxy(
        @JsonProperty(DSS_MESSAGE_HEADER_ID) Map<String, String> pHeader,
        @JsonProperty(DSS_MESSAGE_CONTENT_ID) Map<String, String> pContent) {
      Preconditions.checkNotNull(pHeader, "Message JSON does not contain header");
      Preconditions.checkNotNull(pContent, "Message JSON does not contain content");
      header = ImmutableMap.copyOf(pHeader);
      content = ImmutableMap.copyOf(pContent);
    }

    private ImmutableMap<String, String> getHeader() {
      return header;
    }

    private ImmutableMap<String, String> getContent() {
      return content;
    }
  }

  public static final String DSS_MESSAGE_HEADER_ID = "header";
  public static final String DSS_MESSAGE_CONTENT_ID = "content";

  public static final String DSS_MESSAGE_HEADER_SENDER_ID_KEY = "senderId";
  public static final String DSS_MESSAGE_HEADER_TYPE_KEY = "messageType";
  public static final String DSS_MESSAGE_HEADER_TIMESTAMP_KEY = "timestamp";
  public static final String DSS_MESSAGE_HEADER_IDENTIFIER_KEY = "identifier";

  private final String senderId;
  private final DssMessageType type;
  private final Instant timestamp;
  private final ImmutableMap<String, String> content;

  DssMessage(String pSenderId, DssMessageType pType, Map<String, String> pContent) {
    checkArgument(isValid(pContent), "Invalid content for message type: " + "%s", pType);
    senderId = pSenderId;
    type = pType;
    timestamp = Instant.now();
    content = ImmutableMap.copyOf(pContent);
  }

  abstract boolean isValid(Map<String, String> pContent);

  public final Instant getTimestamp() {
    return timestamp;
  }

  public final DssMessageType getType() {
    return type;
  }

  public final String getSenderId() {
    return senderId;
  }

  private ContentReader getArbitraryContent(String pKey) {
    checkArgument(
        type == DssMessageType.POST_CONDITION || type == DssMessageType.VIOLATION_CONDITION,
        "Cannot get content for type: " + "%s",
        type);
    Map<String, String> stateContent = ContentReader.read(content).pushLevel(pKey).getContent();
    Preconditions.checkState(
        !stateContent.isEmpty(), "State content cannot be empty for key %s.", pKey);
    Preconditions.checkState(
        stateContent.values().stream().noneMatch(Objects::isNull),
        "Null values are not allowed in content.");
    return ContentReader.read(stateContent);
  }

  /**
   * Get the number of contained states in this message, if any.
   *
   * @return An OptionalInt containing the number of states, or empty if not present.
   */
  public final OptionalInt getNumberOfContainedStates() {
    if (content.containsKey(DistributedConfigurableProgramAnalysis.MULTIPLE_STATES_KEY)) {
      return OptionalInt.of(
          Integer.parseInt(
              Objects.requireNonNull(
                  content.get(DistributedConfigurableProgramAnalysis.MULTIPLE_STATES_KEY))));
    }
    return OptionalInt.empty();
  }

  public final DssMessage advance(String pPrefix) {
    Map<String, String> prefixContent = ContentReader.read(content).pushLevel(pPrefix).getContent();
    ImmutableMap.Builder<String, String> advanced = ImmutableMap.builder();
    advanced.putAll(content).putAll(prefixContent);
    return new DssMessage(senderId, type, advanced.buildOrThrow()) {
      @Override
      boolean isValid(Map<String, String> pContent) {
        return DssMessage.this.isValid(pContent);
      }
    };
  }

  public final ContentReader getAbstractStateContent(Class<? extends AbstractState> pType) {
    return getArbitraryContent(pType.getName());
  }

  public final ContentReader getPrecisionContent(Class<? extends Precision> pPrecision) {
    return getArbitraryContent(pPrecision.getName());
  }

  public final Result getResult() {
    checkArgument(type == DssMessageType.RESULT, "Cannot get content for type: " + "%s", type);
    String resultString = content.get(DssResultMessage.DSS_MESSAGE_RESULT_KEY);
    Preconditions.checkNotNull(resultString, "Result content is missing in message: %s", this);
    return Result.valueOf(resultString);
  }

  public final AlgorithmStatus getAlgorithmStatus() {
    checkArgument(
        type == DssMessageType.POST_CONDITION || type == DssMessageType.VIOLATION_CONDITION,
        "Cannot get content for type: %s",
        type);
    ContentReader reader =
        ContentReader.read(content).pushLevel(DssMessageFactory.DSS_MESSAGE_STATUS_KEY);
    boolean checkedProperty =
        Boolean.parseBoolean(reader.get(DssMessageFactory.DSS_MESSAGE_PROPERTY_KEY));
    if (!checkedProperty) {
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    } else {
      boolean isSound = Boolean.parseBoolean(reader.get(DssMessageFactory.DSS_MESSAGE_SOUND_KEY));
      boolean isPrecise =
          Boolean.parseBoolean(reader.get(DssMessageFactory.DSS_MESSAGE_PRECISE_KEY));
      if (isSound && isPrecise) {
        return AlgorithmStatus.SOUND_AND_PRECISE;
      } else if (isSound) {
        return AlgorithmStatus.SOUND_AND_IMPRECISE;
      } else if (isPrecise) {
        return AlgorithmStatus.UNSOUND_AND_PRECISE;
      } else {
        return AlgorithmStatus.UNSOUND_AND_IMPRECISE;
      }
    }
  }

  public final String getExceptionMessage() {
    checkArgument(type == DssMessageType.EXCEPTION, "Cannot get content for type: " + "%s", type);
    String exceptionMessage = content.get(DssExceptionMessage.DSS_MESSAGE_EXCEPTION_KEY);
    Preconditions.checkNotNull(
        exceptionMessage, "Exception message is missing in message: %s", this);
    return exceptionMessage;
  }

  /**
   * Convert the message to a JSON representation with an identifier.
   *
   * @param pIdentifier Identifier to include in the header. Used to show only the most recent
   *     messages in the visualizer.
   * @return JSON representation of the message.
   */
  @SuppressWarnings("JavaInstantGetSecondsGetNano")
  public final ImmutableMap<String, ImmutableMap<String, String>> asJsonWithIdentifier(
      int pIdentifier) {
    ImmutableMap.Builder<String, String> header =
        ImmutableMap.<String, String>builder()
            .put(DSS_MESSAGE_HEADER_SENDER_ID_KEY, getSenderId())
            .put(DSS_MESSAGE_HEADER_TYPE_KEY, getType().name())
            .put(
                DSS_MESSAGE_HEADER_TIMESTAMP_KEY,
                Long.toString(
                    getTimestamp().getEpochSecond() * 1_000_000_000L + getTimestamp().getNano()))
            .put(DSS_MESSAGE_HEADER_IDENTIFIER_KEY, Integer.toString(pIdentifier));
    return ImmutableMap.<String, ImmutableMap<String, String>>builder()
        .put(DSS_MESSAGE_HEADER_ID, header.buildOrThrow())
        .put(DSS_MESSAGE_CONTENT_ID, content)
        .buildOrThrow();
  }

  public final ImmutableMap<String, ImmutableMap<String, String>> asJson() {
    return asJsonWithIdentifier(0);
  }

  public final Map<StatisticsKey, String> getStats() {
    checkState(
        type == DssMessageType.STATISTIC, "Cannot get stats for message type: " + "%s", type);
    ImmutableMap.Builder<StatisticsKey, String> statsBuilder = ImmutableMap.builder();
    for (Map.Entry<String, String> entry : content.entrySet()) {
      StatisticsKey key = StatisticsKey.valueOf(entry.getKey());
      statsBuilder.put(key, entry.getValue());
    }
    return statsBuilder.buildOrThrow();
  }

  public static DssMessage fromJson(Path pJson) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    DssMessageProxy proxy = mapper.readValue(pJson.toFile(), DssMessageProxy.class);
    ImmutableMap<String, ImmutableMap<String, String>> json =
        ImmutableMap.of(
            DSS_MESSAGE_HEADER_ID, ImmutableMap.copyOf(proxy.getHeader()),
            DSS_MESSAGE_CONTENT_ID, ImmutableMap.copyOf(proxy.getContent()));
    return fromJson(json);
  }

  public static DssMessage fromJson(ImmutableMap<String, ImmutableMap<String, String>> pJson) {
    ImmutableMap<String, String> header =
        Objects.requireNonNull(
            pJson.get(DSS_MESSAGE_HEADER_ID), "Message JSON does not contain header: " + pJson);
    ImmutableMap<String, String> content =
        Objects.requireNonNull(
            pJson.get(DSS_MESSAGE_CONTENT_ID), "Message JSON does not contain content: " + pJson);

    String senderId = header.get(DSS_MESSAGE_HEADER_SENDER_ID_KEY);
    DssMessageType type = DssMessageType.valueOf(header.get(DSS_MESSAGE_HEADER_TYPE_KEY));

    return switch (type) {
      case POST_CONDITION -> new DssPostConditionMessage(senderId, ImmutableList.of(), content);
      case VIOLATION_CONDITION -> new DssViolationConditionMessage(senderId, content);
      case EXCEPTION -> new DssExceptionMessage(senderId, content);
      case RESULT -> new DssResultMessage(senderId, content);
      case STATISTIC -> new DssStatisticsMessage(senderId, content);
    };
  }
}
