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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.pathrestriction.SegmentedPaths;

/**
 * Abstract base class for messages used in distributed summary synthesis. Each message has a sender
 * ID, a type, a timestamp, and content. The content is a flat map of key-value pairs, where keys
 * can be hierarchical using dot notation.
 */
public abstract class DssMessage {

  public enum DssMessageType {
    POST_CONDITION,
    VIOLATION_CONDITION,
    EXCEPTION,
    RESULT,
    WITNESS
  }

  private final String senderId;
  private final DssMessageType type;
  private final Instant timestamp;
  private final ImmutableMap<String, String> content;

  /**
   * Creates a new message with the given sender ID, type, and content.
   *
   * @param pSenderId the ID of the sender
   * @param pType the type of the message
   * @param pContent the content of the message
   */
  DssMessage(String pSenderId, DssMessageType pType, Map<String, String> pContent) {
    checkArgument(isValid(pContent), "Invalid content for message type: %s", pType);
    senderId = pSenderId;
    type = pType;
    timestamp = Instant.now();
    content = ImmutableMap.copyOf(pContent);
  }

  /**
   * Checks whether the given content is valid for this message type.
   *
   * @param pContent the content to check
   * @return true if the content is valid, false otherwise
   */
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
        type == DssMessageType.POST_CONDITION
            || type == DssMessageType.VIOLATION_CONDITION
            || type == DssMessageType.WITNESS,
        "Cannot get content for type: %s",
        type);
    Map<String, String> stateContent = ContentReader.read(content).pushLevel(pKey).getContent();
    checkState(!stateContent.isEmpty(), "State content cannot be empty for key %s.", pKey);
    checkState(
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
    if (content.containsKey(DssMessageFormat.MULTIPLE_STATES_KEY)) {
      return OptionalInt.of(
          Integer.parseInt(
              Objects.requireNonNull(content.get(DssMessageFormat.MULTIPLE_STATES_KEY))));
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
    return Result.valueOf(
        Preconditions.checkNotNull(
            content.get(DssMessageFormat.RESULT_KEY),
            "Result content is missing in message: %s",
            this));
  }

  public final DssWitnessMessage.WitnessType getWitnessType() {
    checkArgument(type == DssMessageType.WITNESS, "Cannot get content for type: %s", type);
    return DssWitnessMessage.WitnessType.valueOf(
        Preconditions.checkNotNull(
            content.get(DssMessageFormat.WITNESS_TYPE_KEY),
            "Witness type is missing in message: %s",
            this));
  }

  public final SegmentedPaths getViolationPath() {
    checkArgument(
        getWitnessType() == DssWitnessMessage.WitnessType.VIOLATION,
        "Cannot get violation path for witness type: %s",
        type);
    return SegmentedPaths.deserialize(
        Preconditions.checkNotNull(
            content.get(DssMessageFormat.VIOLATION_PATH_KEY),
            "No violation path present in witness message: %s",
            this));
  }

  public final String extractBlockStateWitnessString() {
    checkArgument(
        type == DssMessageType.VIOLATION_CONDITION, "Cannot get content for type: " + "%s", type);

    checkState(getNumberOfContainedStates().orElse(-1) >= 1, "No state to extract witness from");

    return ContentReader.read(content)
        .pushLevel(BlockState.class.getName())
        .pushLevel(SerializeOperator.STATE_KEY + 0)
        .get(SerializeOperator.STATE_KEY);
  }

  /**
   * Whether the sender reports the end of its block as unreachable, i.e., its postcondition is
   * {@code false} and successor blocks must not be entered through it.
   *
   * <p>This is an explicit flag on purpose. It must not be inferred from the states the message
   * carries: the most general (top) state is a perfectly valid postcondition for a block whose end
   * is reachable but unconstrained, and treating it as a marker would silently discard it.
   *
   * @return whether this message reports an unreachable block end
   */
  public final boolean indicatesUnreachableBlockEnd() {
    checkArgument(type == DssMessageType.POST_CONDITION, "Cannot get content for type: %s", type);
    return Boolean.parseBoolean(content.get(DssMessageFormat.UNREACHABLE_BLOCK_END_KEY));
  }

  public final AlgorithmStatus getAlgorithmStatus() {
    checkArgument(
        type == DssMessageType.POST_CONDITION || type == DssMessageType.VIOLATION_CONDITION,
        "Cannot get content for type: %s",
        type);
    ContentReader reader = ContentReader.read(content).pushLevel(DssMessageFormat.STATUS_KEY);
    boolean checkedProperty = Boolean.parseBoolean(reader.get(DssMessageFormat.PROPERTY_KEY));
    if (!checkedProperty) {
      return AlgorithmStatus.NO_PROPERTY_CHECKED;
    } else {
      boolean isSound = Boolean.parseBoolean(reader.get(DssMessageFormat.SOUND_KEY));
      boolean isPrecise = Boolean.parseBoolean(reader.get(DssMessageFormat.PRECISE_KEY));
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
    return Preconditions.checkNotNull(
        content.get(DssMessageFormat.EXCEPTION_KEY),
        "Exception message is missing in message: %s",
        this);
  }

  @SuppressWarnings("JavaInstantGetSecondsGetNano")
  public final DssMessagePayload asJsonPayloadWithIdentifier(int pIdentifier) {
    DssHeaderPayload header =
        new DssHeaderPayload(
            senderId,
            type,
            Long.toString(
                getTimestamp().getEpochSecond() * 1_000_000_000L + getTimestamp().getNano()),
            pIdentifier);
    DssStatusPayload statusPayload = extractStatusPayload();
    ImmutableMap<String, String> payloadContent = contentWithoutLegacyStatus();

    return new DssMessagePayload(header, statusPayload, payloadContent);
  }

  public final DssMessagePayload asJsonPayload() {
    return asJsonPayloadWithIdentifier(0);
  }

  /**
   * Convert the message to a JSON representation with an identifier.
   *
   * @param pIdentifier A unique identifier indicating a set of messages that belong together. All
   *     messages produced in one run of DSS should have the same identifier. This simplifies the
   *     separation of old and new messages after the analysis, especially.
   * @return JSON representation of the message.
   */
  public final ImmutableMap<String, ImmutableMap<String, String>> asJsonWithIdentifier(
      int pIdentifier) {
    return asJsonPayloadWithIdentifier(pIdentifier).asLegacyMap();
  }

  public final ImmutableMap<String, ImmutableMap<String, String>> asJson() {
    return asJsonWithIdentifier(0);
  }

  public static DssMessage fromJson(Path pJson) throws IOException {
    DssMessagePayload payload = DssMessagePayload.fromJson(pJson);
    return fromPayload(payload);
  }

  public static DssMessage fromPayload(DssMessagePayload pPayload) {
    DssHeaderPayload header = pPayload.header();
    ImmutableMap<String, String> content = pPayload.legacyContent();

    String senderId = header.senderId();
    DssMessageType type = header.messageType();

    return switch (type) {
      case POST_CONDITION -> new DssPostConditionMessage(senderId, content);
      case VIOLATION_CONDITION -> new DssViolationConditionMessage(senderId, content);
      case EXCEPTION -> new DssExceptionMessage(senderId, content);
      case RESULT -> new DssResultMessage(senderId, content);
      case WITNESS -> new DssWitnessMessage(senderId, content);
    };
  }

  private @Nullable DssStatusPayload extractStatusPayload() {
    if (type != DssMessageType.POST_CONDITION && type != DssMessageType.VIOLATION_CONDITION) {
      return null;
    }
    return DssStatusPayload.fromAlgorithmStatus(getAlgorithmStatus());
  }

  private ImmutableMap<String, String> contentWithoutLegacyStatus() {
    return content.entrySet().stream()
        .filter(entry -> !entry.getKey().startsWith(DssMessageFormat.STATUS_KEY + "."))
        .collect(ImmutableMap.toImmutableMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
