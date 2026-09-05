// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkElementIndex;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage.DssMessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializeOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.block.BlockState;
import org.sosy_lab.cpachecker.cpa.pathrestriction.SegmentedPaths;

/**
 * Abstract base class for messages used in distributed summary synthesis. Each message has a sender
 * ID, a type, a timestamp, a status, a list of states and content. The content is a flat map of
 * key-value pairs, where keys can be hierarchical using dot notation.
 */
public abstract class LegacyDssMessage implements DssMessage {

  private final String senderId;
  private final DssMessageType type;
  private final Instant timestamp;
  private final Optional<AlgorithmStatus> status;
  private final ImmutableList<ImmutableMap<String, String>> states;
  private final ImmutableMap<String, String> content;

  /**
   * Creates a new message with the given sender ID, type, and content.
   *
   * @param pSenderId the ID of the sender
   * @param pType the type of the message
   * @param pStatus the optional status of the message
   * @param pStates the list of states associated with this message
   * @param pContent the content of the message
   */
  LegacyDssMessage(
      String pSenderId,
      DssMessageType pType,
      Optional<AlgorithmStatus> pStatus,
      List<? extends Map<String, String>> pStates,
      Map<String, String> pContent) {
    validateParameters(pStatus, pStates, pContent);
    senderId = pSenderId;
    type = pType;
    status = pStatus;
    states = pStates.stream().map(ImmutableMap::copyOf).collect(ImmutableList.toImmutableList());
    timestamp = Instant.now();
    content = ImmutableMap.copyOf(pContent);
  }

  /**
   * Checks whether the given parameters are valid for this message type.
   *
   * @param pStatus the algorithm status to check
   * @param pStates the states to check
   * @param pContent the content to check
   * @throws IllegalArgumentException if parameters are invalid for this message type.
   */
  abstract void validateParameters(
      Optional<AlgorithmStatus> pStatus,
      List<? extends Map<String, String>> pStates,
      Map<String, String> pContent);

  private static boolean hasStatus(DssMessageType pType) {
    return pType == DssMessage.DssMessageType.POST_CONDITION
        || pType == DssMessage.DssMessageType.VIOLATION_CONDITION;
  }

  public final Instant getTimestamp() {
    return timestamp;
  }

  public final DssMessageType getType() {
    return type;
  }

  public final String getSenderId() {
    return senderId;
  }

  public final ImmutableList<ImmutableMap<String, String>> getStates() {
    return states;
  }

  private ContentReader getArbitraryContent(String pKey, int pStateIndex) {
    checkElementIndex(pStateIndex, states.size(), "state index");
    checkArgument(
        type == DssMessage.DssMessageType.POST_CONDITION
            || type == DssMessage.DssMessageType.VIOLATION_CONDITION
            || type == DssMessage.DssMessageType.WITNESS,
        "Cannot get content for type: %s",
        type);
    Map<String, String> stateContent =
        ContentReader.read(states.get(pStateIndex)).pushLevel(pKey).getContent();
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
    if (!states.isEmpty()) {
      return OptionalInt.of(states.size());
    }
    if (content.containsKey(DssMessageKeys.MULTIPLE_STATES)) {
      return OptionalInt.of(
          Integer.parseInt(Objects.requireNonNull(content.get(DssMessageKeys.MULTIPLE_STATES))));
    }
    return OptionalInt.empty();
  }

  public final ContentReader getAbstractStateContent(
      Class<? extends AbstractState> pType, int pStateIndex) {
    return getArbitraryContent(pType.getName(), pStateIndex);
  }

  public final ContentReader getPrecisionContent(
      Class<? extends Precision> pPrecision, int pStateIndex) {
    return getArbitraryContent(pPrecision.getName(), pStateIndex);
  }

  public final Result getResult() {
    checkArgument(
        type == DssMessage.DssMessageType.RESULT, "Cannot get content for type: " + "%s", type);
    return Result.valueOf(
        Preconditions.checkNotNull(
            content.get(DssMessageKeys.RESULT), "Result content is missing in message: %s", this));
  }

  public final DssWitnessMessage.WitnessType getWitnessType() {
    checkArgument(
        type == DssMessage.DssMessageType.WITNESS, "Cannot get content for type: %s", type);
    return DssWitnessMessage.WitnessType.valueOf(
        Preconditions.checkNotNull(
            content.get(DssMessageKeys.WITNESS_TYPE),
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
            content.get(DssMessageKeys.VIOLATION_PATH),
            "No violation path present in witness message: %s",
            this));
  }

  public final String extractBlockStateWitnessString() {
    checkArgument(
        type == DssMessage.DssMessageType.VIOLATION_CONDITION,
        "Cannot get content for type: " + "%s",
        type);

    checkState(getNumberOfContainedStates().orElse(-1) >= 1, "No state to extract witness from");

    return ContentReader.read(states.getFirst())
        .pushLevel(BlockState.class.getName())
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
    checkArgument(
        type == DssMessage.DssMessageType.POST_CONDITION, "Cannot get content for type: %s", type);
    return Boolean.parseBoolean(content.get(DssMessageKeys.UNREACHABLE_BLOCK_END));
  }

  public final AlgorithmStatus getAlgorithmStatus() {
    checkArgument(hasStatus(type), "Cannot get content for type: %s", type);
    return status.orElseThrow();
  }

  public final String getExceptionMessage() {
    checkArgument(
        type == DssMessage.DssMessageType.EXCEPTION, "Cannot get content for type: " + "%s", type);
    return Preconditions.checkNotNull(
        content.get(DssMessageKeys.EXCEPTION), "Exception message is missing in message: %s", this);
  }

  /**
   * Convert the message to the current DSS JSON payload representation with an identifier.
   *
   * @param pIdentifier A unique identifier indicating a set of messages that belong together. All
   *     messages produced in one run of DSS should have the same identifier. This simplifies the
   *     separation of old and new messages after the analysis, especially.
   * @return the structured payload containing header, optional status, states and content.
   */
  @SuppressWarnings("JavaInstantGetSecondsGetNano")
  public final DssMessagePayload asJsonPayloadWithIdentifier(int pIdentifier) {
    DssHeaderPayload header =
        new DssHeaderPayload(
            senderId,
            type,
            Long.toString(
                getTimestamp().getEpochSecond() * 1_000_000_000L + getTimestamp().getNano()),
            pIdentifier);
    DssStatusPayload statusPayload = status.map(DssStatusPayload::fromAlgorithmStatus).orElse(null);

    return new DssMessagePayload(header, statusPayload, states, content);
  }

  public final DssMessagePayload asJsonPayload() {
    return asJsonPayloadWithIdentifier(0);
  }

  /**
   * Convert the message to the legacy flattened JSON representation with an identifier.
   *
   * <p>This exists for callers that still expect states and status to be embedded in the content
   * map. New code should use {@link #asJsonPayloadWithIdentifier(int)}
   *
   * @param pIdentifier A unique identifier indicating a set of messages that belong together. All
   *     messages produced in one run of DSS should have the same identifier. This simplifies the
   *     separation of old and new messages after the analysis, especially.
   * @return JSON representation of the message.
   */
  public final ImmutableMap<String, ImmutableMap<String, String>> asDebugFlatMapWithIdentifier(
      int pIdentifier) {
    return asJsonPayloadWithIdentifier(pIdentifier).asDebugFlatMap();
  }

  public final ImmutableMap<String, ImmutableMap<String, String>> asDebugFlatMap() {
    return asDebugFlatMapWithIdentifier(0);
  }
}
