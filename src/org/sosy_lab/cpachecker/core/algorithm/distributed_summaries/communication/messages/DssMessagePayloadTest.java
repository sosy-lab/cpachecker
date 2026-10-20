// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.algorithm.Algorithm.AlgorithmStatus;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack.DssCallstackEntryPayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.callstack.DssCallstackStatePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DssCompositePrecisionPayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite.DssCompositeStatePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.distributed_block_cpa.DssBlockStatePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer.DssFunctionPointerEntryPayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.function_pointer.DssFunctionPointerStatePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.location.DssLocationStatePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.DssLocationInstancePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.DssPredicatePrecisionPayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.DssPredicateStatePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate.DssSSAVariablePayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.worker.DssAnalysisOptions;

public class DssMessagePayloadTest {
  private DssMessageFactory defaultFactory() throws InvalidConfigurationException {
    return new DssMessageFactory(new DssAnalysisOptions(Configuration.defaultConfiguration()));
  }

  private DssMessage roundTrip(DssMessage pMessage) throws IOException {
    Path file = tempFolder.newFile("message.json").toPath();
    DssMessageJsonUtils.write(pMessage.asJsonPayload(), file);
    return DssMessageCodec.fromJson(file);
  }

  @Rule public TemporaryFolder tempFolder = new TemporaryFolder();

  @Test
  public void fromJsonRejectsMissingHeader() throws Exception {
    Path file = tempFolder.newFile("message.json").toPath();
    String jsonContent =
        String.format(
            """
            {
              "%s": "result",
              "%s": "%s"
            }
            """,
            DssMessageKeys.TYPE, DssMessageKeys.RESULT, Result.TRUE.name());
    Files.writeString(file, jsonContent);

    IOException exception = assertThrows(IOException.class, () -> DssMessageJsonUtils.read(file));
    assertThat(exception).hasMessageThat().contains(DssMessageKeys.HEADER);
  }

  @Test
  public void fromPayloadCreatesPostConditionMessageWithStatus() {
    DssMessagePayload payload = getPayload();
    DssMessage message = DssMessageCodec.fromPayload(payload);

    assertThat(message).isInstanceOf(DssPostConditionMessage.class);
    assertThat(message).isInstanceOf(DssMessageWithStates.class);
    assertThat(message).isInstanceOf(DssMessageWithStatus.class);
    assertThat(message).isInstanceOf(DssConditionMessage.class);
    DssPostConditionMessage postConditionMessage = (DssPostConditionMessage) message;
    assertThat(postConditionMessage.getAlgorithmStatus())
        .isEqualTo(AlgorithmStatus.SOUND_AND_IMPRECISE);
  }

  private static DssMessagePayload getPayload() {
    DssStatePayload statePayload = new DssLocationStatePayload(1);
    DssPrecisionPayload precisionPayload = DssNoPrecisionPayload.INSTANCE;
    DssStateAndPrecisionPayload stateAndPrecisionPayload =
        new DssStateAndPrecisionPayload(statePayload, precisionPayload);
    return new DssPostConditionPayload(
        new DssHeaderPayload("B1", "123", 0),
        new DssStatusPayload(true, false, true),
        ImmutableList.of(stateAndPrecisionPayload),
        true);
  }

  @Test
  public void writeJsonCreatesJsonFile() throws IOException {
    DssMessagePayload payload = getPayload();
    Path file = tempFolder.getRoot().toPath().resolve("message.json");

    DssMessageJsonUtils.write(payload, file);

    assertThat(Files.isRegularFile(file)).isTrue();
    assertThat(Files.readString(file)).contains(DssMessageKeys.HEADER);
  }

  @Test
  public void postConditionRoundTripPreservesData()
      throws InvalidConfigurationException, IOException {
    DssPostConditionMessage original =
        defaultFactory()
            .createDssUnreachableBlockEndMessage("B1", AlgorithmStatus.SOUND_AND_PRECISE);
    DssMessage parsed = roundTrip(original);

    assertThat(parsed.getType()).isEqualTo(original.getType());
    assertThat(parsed.getClass()).isEqualTo(original.getClass());
    DssPostConditionMessage parsedPost = (DssPostConditionMessage) parsed;
    assertThat(parsedPost.getAlgorithmStatus()).isEqualTo(original.getAlgorithmStatus());
    assertThat(parsedPost.getStates()).isEqualTo(original.getStates());
    assertThat(parsed.getSenderId()).isEqualTo(original.getSenderId());
    assertThat(parsedPost.indicatesUnreachableBlockEnd()).isTrue();
  }

  @Test
  public void unreachableBlockEndPayloadAllowsEmptyStates()
      throws InvalidConfigurationException, IOException {
    DssPostConditionMessage original =
        defaultFactory()
            .createDssUnreachableBlockEndMessage("B1", AlgorithmStatus.SOUND_AND_PRECISE);
    DssPostConditionPayload payload = (DssPostConditionPayload) original.asJsonPayload();
    Path file = tempFolder.newFile("unreachable-message.json").toPath();

    assertThat(payload.states()).isEmpty();

    DssMessageJsonUtils.write(payload, file);
    assertThat(Files.readString(file)).contains(String.format("\"%s\":[]", DssMessageKeys.STATES));

    DssMessagePayload parsedPayload = DssMessageJsonUtils.read(file);
    assertThat(parsedPayload).isEqualTo(payload);
    DssPostConditionMessage parsed =
        (DssPostConditionMessage) DssMessageCodec.fromPayload(parsedPayload);
    assertThat(parsed.getStates()).isEmpty();
    assertThat(parsed.indicatesUnreachableBlockEnd()).isTrue();
  }

  @Test
  public void callstackStateRoundTripPreservesPayload()
      throws InvalidConfigurationException, IOException {
    DssCallstackStatePayload originalCallstackPayload =
        new DssCallstackStatePayload(
            true,
            ImmutableList.of(
                new DssCallstackEntryPayload(12, "main"),
                new DssCallstackEntryPayload(27, "helper")));
    DssPostConditionMessage original =
        defaultFactory()
            .createDssPostConditionMessage(
                "B1",
                AlgorithmStatus.SOUND_AND_PRECISE,
                ImmutableList.of(
                    new DssStateAndPrecisionPayload(
                        originalCallstackPayload, DssNoPrecisionPayload.INSTANCE)));

    DssMessage parsed = roundTrip(original);

    assertThat(parsed).isInstanceOf(DssPostConditionMessage.class);
    DssPostConditionMessage parsedPost = (DssPostConditionMessage) parsed;
    assertThat(parsedPost.getType()).isEqualTo(DssMessage.DssMessageType.POST_CONDITION);
    DssStateAndPrecisionPayload parsedStateAndPrecision = parsedPost.getStates().getFirst();
    assertThat(parsedStateAndPrecision.state()).isEqualTo(originalCallstackPayload);
    assertThat(parsedStateAndPrecision.precision()).isEqualTo(DssNoPrecisionPayload.INSTANCE);
  }

  @Test
  public void compositeStateRoundTripPreservesLocationAndCallstackPayloads()
      throws InvalidConfigurationException, IOException {
    DssPredicateStatePayload predicateStatePayload =
        new DssPredicateStatePayload(
            "x == 0", ImmutableMap.of("x", new DssSSAVariablePayload(3, "int")), "pts-1", "x == 0");
    DssPredicatePrecisionPayload predicatePrecisionPayload =
        new DssPredicatePrecisionPayload(
            ImmutableMap.of(new DssLocationInstancePayload(42, 1), ImmutableSet.of("x == 0")),
            ImmutableMap.of(42, ImmutableSet.of("x == 1")),
            ImmutableMap.of("main", ImmutableSet.of("x == 2")),
            ImmutableSet.of("x == 3"));
    DssCompositeStatePayload originalCompositePayload =
        new DssCompositeStatePayload(
            ImmutableMap.of(
                DssComponentKey.LOCATION_STATE.getSerializedName(),
                new DssLocationStatePayload(42),
                DssComponentKey.PREDICATE_STATE.getSerializedName(),
                predicateStatePayload));
    DssCompositePrecisionPayload originalCompositePrecisionPayload =
        new DssCompositePrecisionPayload(
            ImmutableMap.of(
                DssComponentKey.LOCATION_STATE.getSerializedName(),
                DssNoPrecisionPayload.INSTANCE,
                DssComponentKey.PREDICATE_PRECISION.getSerializedName(),
                predicatePrecisionPayload));
    DssPostConditionMessage original =
        defaultFactory()
            .createDssPostConditionMessage(
                "B1",
                AlgorithmStatus.SOUND_AND_PRECISE,
                ImmutableList.of(
                    new DssStateAndPrecisionPayload(
                        originalCompositePayload, originalCompositePrecisionPayload)));

    DssMessage parsed = roundTrip(original);

    assertThat(parsed).isInstanceOf(DssPostConditionMessage.class);
    DssPostConditionMessage parsedPost = (DssPostConditionMessage) parsed;
    DssStateAndPrecisionPayload parsedStateAndPrecision = parsedPost.getStates().getFirst();
    assertThat(parsedStateAndPrecision.state()).isEqualTo(originalCompositePayload);
    assertThat(parsedStateAndPrecision.precision()).isEqualTo(originalCompositePrecisionPayload);
  }

  @Test
  public void blockStateRoundTripPreservesPayload()
      throws InvalidConfigurationException, IOException {
    DssBlockStatePayload originalBlockPayload =
        new DssBlockStatePayload(
            "unique-id", "block-1", "witness", ImmutableList.of("entry", "loop", "exit"));
    DssPostConditionMessage original =
        defaultFactory()
            .createDssPostConditionMessage(
                "B1",
                AlgorithmStatus.SOUND_AND_PRECISE,
                ImmutableList.of(
                    new DssStateAndPrecisionPayload(
                        originalBlockPayload, DssNoPrecisionPayload.INSTANCE)));

    DssMessage parsed = roundTrip(original);

    assertThat(parsed).isInstanceOf(DssPostConditionMessage.class);
    DssPostConditionMessage parsedPost = (DssPostConditionMessage) parsed;
    DssStateAndPrecisionPayload parsedStateAndPrecision = parsedPost.getStates().getFirst();
    assertThat(parsedStateAndPrecision.state()).isEqualTo(originalBlockPayload);
    assertThat(parsedStateAndPrecision.precision()).isEqualTo(DssNoPrecisionPayload.INSTANCE);
  }

  @Test
  public void functionPointerStateRoundTripPreservesAllEntryVariants()
      throws InvalidConfigurationException, IOException {
    DssFunctionPointerStatePayload originalFunctionPointerPayload =
        new DssFunctionPointerStatePayload(
            ImmutableList.of(
                new DssFunctionPointerEntryPayload(
                    "invalidPointer", DssFunctionPointerEntryPayload.TargetKind.INVALID, null),
                new DssFunctionPointerEntryPayload(
                    "nullPointer", DssFunctionPointerEntryPayload.TargetKind.NULL, null),
                new DssFunctionPointerEntryPayload(
                    "unknownPointer", DssFunctionPointerEntryPayload.TargetKind.UNKNOWN, null),
                new DssFunctionPointerEntryPayload(
                    "namedPointer", DssFunctionPointerEntryPayload.TargetKind.NAMED, "target")));
    DssPostConditionMessage original =
        defaultFactory()
            .createDssPostConditionMessage(
                "B1",
                AlgorithmStatus.SOUND_AND_PRECISE,
                ImmutableList.of(
                    new DssStateAndPrecisionPayload(
                        originalFunctionPointerPayload, DssNoPrecisionPayload.INSTANCE)));

    DssMessage parsed = roundTrip(original);

    assertThat(parsed).isInstanceOf(DssPostConditionMessage.class);
    DssPostConditionMessage parsedPost = (DssPostConditionMessage) parsed;
    DssStateAndPrecisionPayload parsedStateAndPrecision = parsedPost.getStates().getFirst();
    assertThat(parsedStateAndPrecision.state()).isEqualTo(originalFunctionPointerPayload);
    assertThat(parsedStateAndPrecision.precision()).isEqualTo(DssNoPrecisionPayload.INSTANCE);
  }

  @Test
  public void predicateStateRoundTripPreservesPayload()
      throws InvalidConfigurationException, IOException {
    DssPredicateStatePayload originalPredicateStatePayload =
        new DssPredicateStatePayload(
            "x == 0", ImmutableMap.of("x", new DssSSAVariablePayload(3, "int")), "pts-1", "x == 0");
    DssPredicatePrecisionPayload originalPredicatePrecisionPayload =
        new DssPredicatePrecisionPayload(
            ImmutableMap.of(new DssLocationInstancePayload(42, 1), ImmutableSet.of("x == 0")),
            ImmutableMap.of(42, ImmutableSet.of("x == 1")),
            ImmutableMap.of("main", ImmutableSet.of("x == 2")),
            ImmutableSet.of("x == 3"));
    DssPostConditionMessage original =
        defaultFactory()
            .createDssPostConditionMessage(
                "B1",
                AlgorithmStatus.SOUND_AND_PRECISE,
                ImmutableList.of(
                    new DssStateAndPrecisionPayload(
                        originalPredicateStatePayload, originalPredicatePrecisionPayload)));

    DssMessage parsed = roundTrip(original);

    assertThat(parsed).isInstanceOf(DssPostConditionMessage.class);
    DssPostConditionMessage parsedPost = (DssPostConditionMessage) parsed;
    DssStateAndPrecisionPayload parsedStateAndPrecision = parsedPost.getStates().getFirst();
    assertThat(parsedStateAndPrecision.state()).isEqualTo(originalPredicateStatePayload);
    assertThat(parsedStateAndPrecision.precision()).isEqualTo(originalPredicatePrecisionPayload);
  }

  @Test
  public void resultMessageRoundTripPreservesResult()
      throws InvalidConfigurationException, IOException {
    DssResultMessage original = defaultFactory().createDssResultMessage("B1", Result.TRUE);
    DssMessage parsed = roundTrip(original);

    assertThat(parsed).isInstanceOf(DssResultMessage.class);
    assertThat(((DssResultMessage) parsed).getResult()).isEqualTo(Result.TRUE);
  }

  @Test
  public void exceptionMessageRoundTripPreservesExceptionMessage()
      throws InvalidConfigurationException, IOException {
    String message = "Exception Message";
    DssExceptionMessage original =
        defaultFactory().createDssExceptionMessage("B1", new Exception(message));
    DssMessage parsed = roundTrip(original);

    assertThat(parsed).isInstanceOf(DssExceptionMessage.class);
    assertThat(((DssExceptionMessage) parsed).getExceptionMessage()).contains(message);
  }

  @Test
  public void violationConditionMessageRoundTripPreservesData()
      throws InvalidConfigurationException, IOException {
    DssViolationConditionMessage original =
        defaultFactory()
            .createViolationConditionMessage(
                "B1",
                AlgorithmStatus.SOUND_AND_IMPRECISE,
                ImmutableList.of(
                    new DssStateAndPrecisionPayload(
                        new DssLocationStatePayload(42), DssNoPrecisionPayload.INSTANCE)));
    DssMessage parsed = roundTrip(original);

    assertThat(parsed).isInstanceOf(DssViolationConditionMessage.class);
    DssViolationConditionMessage parsedViolation = (DssViolationConditionMessage) parsed;
    assertThat(parsedViolation.getStates()).isEqualTo(original.getStates());
    assertThat(parsedViolation.getAlgorithmStatus()).isEqualTo(original.getAlgorithmStatus());
  }

  @Test
  public void correctnessWitnessRoundTripPreservesData()
      throws InvalidConfigurationException, IOException {
    DssCorrectnessWitnessMessage original =
        defaultFactory()
            .createDssCorrectnessWitnessMessage(
                "B1",
                ImmutableList.of(
                    new DssStateAndPrecisionPayload(
                        new DssLocationStatePayload(42), DssNoPrecisionPayload.INSTANCE)));
    DssMessage parsed = roundTrip(original);

    assertThat(parsed).isInstanceOf(DssCorrectnessWitnessMessage.class);
    DssCorrectnessWitnessMessage parsedWitness = (DssCorrectnessWitnessMessage) parsed;
    assertThat(parsedWitness.getStates()).isEqualTo(original.getStates());
  }
}
