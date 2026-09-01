// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages;

import static com.google.common.base.Preconditions.checkArgument;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.checkerframework.checker.nullness.qual.Nullable;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({
  DssMessageKeys.HEADER,
  DssMessageKeys.STATUS,
  DssMessageKeys.STATES,
  DssMessageKeys.CONTENT
})
public record DssMessagePayload(
    @JsonProperty(DssMessageKeys.HEADER) DssHeaderPayload header,
    @JsonProperty(DssMessageKeys.STATUS) @Nullable DssStatusPayload status,
    @JsonProperty(DssMessageKeys.STATES) ImmutableList<ImmutableMap<String, String>> states,
    @JsonProperty(DssMessageKeys.CONTENT) ImmutableMap<String, String> content) {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  public DssMessagePayload(
      DssHeaderPayload header,
      @Nullable DssStatusPayload status,
      ImmutableList<ImmutableMap<String, String>> states,
      ImmutableMap<String, String> content) {
    this.header = requireHeader(header);
    this.status = status;
    this.states = copyStates(states);
    this.content = requireContent(content);
  }

  @JsonCreator
  DssMessagePayload(
      @JsonProperty(DssMessageKeys.HEADER) DssHeaderPayload pHeader,
      @JsonProperty(DssMessageKeys.STATUS) @Nullable DssStatusPayload pStatus,
      @JsonProperty(DssMessageKeys.STATES) @Nullable List<? extends Map<String, String>> pStates,
      @JsonProperty(DssMessageKeys.CONTENT) Map<String, String> pContent) {
    this(
        requireHeader(pHeader),
        pStatus,
        copyStates(pStates),
        ImmutableMap.copyOf(requireContent(pContent)));
  }

  public static DssMessagePayload fromJson(Path pJson) throws IOException {
    JsonNode root = OBJECT_MAPPER.readTree(pJson.toFile());

    boolean hasCurrentOnlyFields =
        root.has(DssMessageKeys.STATUS) || root.has(DssMessageKeys.STATES);

    ImmutableMap<String, String> content =
        ImmutableMap.copyOf(
            OBJECT_MAPPER.convertValue(
                root.required(DssMessageKeys.CONTENT),
                new TypeReference<Map<String, String>>() {}));

    boolean contentLooksLegacy = content.keySet().stream().anyMatch(k -> isLegacyKey(k));

    checkArgument(
        !hasCurrentOnlyFields || !contentLooksLegacy, "JSON mixes legacy and current format.");
    if (!hasCurrentOnlyFields && contentLooksLegacy) {
      return fromLegacyJson(root);
    }
    return OBJECT_MAPPER.treeToValue(root, DssMessagePayload.class);
  }

  public DssMessagePayload withoutTimestamp() {
    return new DssMessagePayload(header.withoutTimestamp(), status, states, content);
  }

  public ImmutableMap<String, ImmutableMap<String, String>> asLegacyMap() {
    return ImmutableMap.of(
        DssMessageKeys.HEADER, header.asLegacyHeader(), DssMessageKeys.CONTENT, legacyContent());
  }

  public ImmutableMap<String, String> legacyContent() {
    ImmutableMap.Builder<String, String> legacyContent = ImmutableMap.builder();
    if (status != null) {
      legacyContent.putAll(status.asLegacyContent());
    }
    if (!states.isEmpty()) {
      legacyContent.put(DssMessageKeys.MULTIPLE_STATES, Integer.toString(states.size()));
      for (int i = 0; i < states.size(); i++) {
        String statePrefix = DssMessageKeys.STATE + i;
        for (Entry<String, String> entry : states.get(i).entrySet()) {
          legacyContent.put(statePrefix + "." + entry.getKey(), entry.getValue());
        }
      }
    }
    legacyContent.putAll(content);
    return legacyContent.buildKeepingLast();
  }

  public void writeJson(Path pPath) throws IOException {
    Path parent = pPath.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    OBJECT_MAPPER.writeValue(pPath.toFile(), this);
  }

  private static DssMessagePayload fromLegacyJson(JsonNode root) {
    DssHeaderPayload header =
        OBJECT_MAPPER.convertValue(root.required(DssMessageKeys.HEADER), DssHeaderPayload.class);
    ImmutableMap<String, String> legacyContent =
        ImmutableMap.copyOf(
            OBJECT_MAPPER.convertValue(
                root.required(DssMessageKeys.CONTENT),
                new TypeReference<Map<String, String>>() {}));

    DssStatusPayload status = extractLegacyStatus(legacyContent);
    ImmutableList<ImmutableMap<String, String>> states = extractLegacyStates(legacyContent);
    ImmutableMap<String, String> content = extractRemainingContent(legacyContent);

    return new DssMessagePayload(header, status, states, content);
  }

  private static DssStatusPayload extractLegacyStatus(ImmutableMap<String, String> pLegacyContent) {
    boolean sound = Boolean.parseBoolean(pLegacyContent.get(DssMessageKeys.SOUND));
    boolean precise = Boolean.parseBoolean(pLegacyContent.get(DssMessageKeys.PRECISE));
    boolean propertyChecked = Boolean.parseBoolean(pLegacyContent.get(DssMessageKeys.PROPERTY));

    return new DssStatusPayload(sound, precise, propertyChecked);
  }

  private static ImmutableList<ImmutableMap<String, String>> extractLegacyStates(
      ImmutableMap<String, String> pLegacyContent) {
    String numberOfStates = pLegacyContent.get(DssMessageKeys.MULTIPLE_STATES);
    if (numberOfStates == null) {
      return ImmutableList.of();
    }
    int stateCount = Integer.parseInt(numberOfStates);
    ImmutableList.Builder<ImmutableMap<String, String>> states =
        ImmutableList.builderWithExpectedSize(stateCount);
    for (int i = 0; i < stateCount; i++) {
      String statePrefix = DssMessageKeys.STATE + i;
      ImmutableMap<String, String> stateContent =
          ImmutableMap.copyOf(
              ContentReader.read(pLegacyContent).pushLevel(statePrefix).getContent());
      states.add(stateContent);
    }
    return states.build();
  }

  private static ImmutableMap<String, String> extractRemainingContent(
      ImmutableMap<String, String> pLegacyContent) {
    return pLegacyContent.entrySet().stream()
        .filter(entry -> !isLegacyKey(entry.getKey()))
        .collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
  }

  private static boolean isLegacyKey(String pKey) {
    return pKey.equals(DssMessageKeys.MULTIPLE_STATES)
        || pKey.startsWith(DssMessageKeys.STATUS)
        || pKey.matches(DssMessageKeys.STATE + "\\d+\\..*");
  }

  private static DssHeaderPayload requireHeader(DssHeaderPayload pHeader) {
    return Preconditions.checkNotNull(pHeader, "Message JSON does not contain header");
  }

  private static <T extends Map<String, String>> T requireContent(T pContent) {
    return Preconditions.checkNotNull(pContent, "Message JSON does not contain content");
  }

  private static ImmutableList<ImmutableMap<String, String>> copyStates(
      @Nullable List<? extends Map<String, String>> pStates) {
    if (pStates == null) {
      return ImmutableList.of();
    }
    return pStates.stream().map(ImmutableMap::copyOf).collect(ImmutableList.toImmutableList());
  }
}
