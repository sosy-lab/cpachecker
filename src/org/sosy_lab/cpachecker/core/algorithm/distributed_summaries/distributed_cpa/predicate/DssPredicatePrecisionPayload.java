// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentReader;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssComponentKey;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssComponentPrecisionPayload;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;

@JsonPropertyOrder({
  DssPredicatePrecisionPayload.LOCATION_INSTANCES,
  DssPredicatePrecisionPayload.LOCAL_PREDICATES,
  DssPredicatePrecisionPayload.FUNCTION_PREDICATES,
  DssPredicatePrecisionPayload.GLOBAL_PREDICATES
})
public record DssPredicatePrecisionPayload(
    @JsonProperty(LOCATION_INSTANCES)
        ImmutableMap<DssLocationInstancePayload, ImmutableSet<String>> locationInstances,
    @JsonProperty(LOCAL_PREDICATES) ImmutableMap<Integer, ImmutableSet<String>> localPredicates,
    @JsonProperty(FUNCTION_PREDICATES)
        ImmutableMap<String, ImmutableSet<String>> functionPredicates,
    @JsonProperty(GLOBAL_PREDICATES) ImmutableSet<String> globalPredicates)
    implements DssComponentPrecisionPayload {
  public static final String LOCATION_INSTANCES = "locationInstances";
  public static final String LOCAL_PREDICATES = "localPredicates";
  public static final String FUNCTION_PREDICATES = "functionPredicates";
  public static final String GLOBAL_PREDICATES = "globalPredicates";

  private static final Joiner PREDICATE_JOINER = Joiner.on(" , ");
  private static final Splitter PREDICATE_SPLITTER = Splitter.on(" , ").omitEmptyStrings();

  @JsonCreator
  DssPredicatePrecisionPayload(
      @JsonProperty(LOCATION_INSTANCES)
          Map<DssLocationInstancePayload, Set<String>> pLocationInstances,
      @JsonProperty(LOCAL_PREDICATES) Map<Integer, Set<String>> pLocalPredicates,
      @JsonProperty(FUNCTION_PREDICATES) Map<String, Set<String>> pFunctionPredicates,
      @JsonProperty(GLOBAL_PREDICATES) Set<String> pGlobalPredicates) {
    this(
        copySetMap(pLocationInstances),
        copySetMap(pLocalPredicates),
        copySetMap(pFunctionPredicates),
        ImmutableSet.copyOf(pGlobalPredicates));
  }

  private static <K> ImmutableMap<K, ImmutableSet<String>> copySetMap(
      Map<K, ? extends Set<String>> pSetMap) {
    return pSetMap.entrySet().stream()
        .collect(
            ImmutableMap.toImmutableMap(
                Map.Entry::getKey, entry -> ImmutableSet.copyOf(entry.getValue())));
  }

  @JsonIgnore
  @Override
  public DssComponentKey getComponentKey() {
    return DssComponentKey.PREDICATE_PRECISION;
  }

  @Override
  public ImmutableMap<String, String> asContentMap() {
    ContentBuilder contentBuilder =
        ContentBuilder.builder()
            .pushLevel(DssComponentKey.serializedNameFor(PredicatePrecision.class));

    contentBuilder.pushLevel(
        SerializePredicatePrecisionOperator.DSS_MESSAGE_LOCATION_INSTANCES_KEY);
    for (DssLocationInstancePayload key : locationInstances.keySet()) {
      contentBuilder.put(key.serialize(), PREDICATE_JOINER.join(locationInstances.get(key)));
    }
    contentBuilder.popLevel();

    contentBuilder.pushLevel(SerializePredicatePrecisionOperator.DSS_MESSAGE_LOCAL_PREDICATES_KEY);
    for (Integer key : localPredicates.keySet()) {
      contentBuilder.put(key.toString(), PREDICATE_JOINER.join(localPredicates.get(key)));
    }
    contentBuilder.popLevel();

    contentBuilder.pushLevel(
        SerializePredicatePrecisionOperator.DSS_MESSAGE_FUNCTION_PREDICATES_KEY);
    for (String key : functionPredicates.keySet()) {
      contentBuilder.put(key, PREDICATE_JOINER.join(functionPredicates.get(key)));
    }
    contentBuilder.popLevel();

    contentBuilder.put(
        SerializePredicatePrecisionOperator.DSS_MESSAGE_GLOBAL_KEY,
        PREDICATE_JOINER.join(globalPredicates));

    return contentBuilder.build();
  }

  private static ImmutableMap<DssLocationInstancePayload, ImmutableSet<String>>
      parseLocationInstances(ContentReader contentReader) {
    contentReader.pushLevel(SerializePredicatePrecisionOperator.DSS_MESSAGE_LOCATION_INSTANCES_KEY);
    Map<String, String> locationInstanceMap = contentReader.getContent();
    ImmutableMap.Builder<DssLocationInstancePayload, ImmutableSet<String>>
        locationInstancesBuilder = ImmutableMap.builder();
    locationInstanceMap.forEach(
        (nodeNumberAndLocationInstance, serializedPredicates) -> {
          DssLocationInstancePayload locationInstance =
              DssLocationInstancePayload.deserialize(nodeNumberAndLocationInstance);
          locationInstancesBuilder.put(
              locationInstance,
              ImmutableSet.copyOf(PREDICATE_SPLITTER.split(serializedPredicates)));
        });

    contentReader.popLevel();
    return locationInstancesBuilder.build();
  }

  private static ImmutableMap<Integer, ImmutableSet<String>> parseLocalPredicates(
      ContentReader contentReader) {
    contentReader.pushLevel(SerializePredicatePrecisionOperator.DSS_MESSAGE_LOCAL_PREDICATES_KEY);
    Map<String, String> localPredicatesMap = contentReader.getContent();
    ImmutableMap.Builder<Integer, ImmutableSet<String>> localPredicatesBuilder =
        ImmutableMap.builder();
    localPredicatesMap.forEach(
        (location, serializedPrecisions) -> {
          localPredicatesBuilder.put(
              Integer.parseInt(location),
              ImmutableSet.copyOf(PREDICATE_SPLITTER.split(serializedPrecisions)));
        });
    contentReader.popLevel();
    return localPredicatesBuilder.build();
  }

  private static ImmutableMap<String, ImmutableSet<String>> parseFunctionPredicates(
      ContentReader contentReader) {
    contentReader.pushLevel(
        SerializePredicatePrecisionOperator.DSS_MESSAGE_FUNCTION_PREDICATES_KEY);
    ImmutableMap.Builder<String, ImmutableSet<String>> functionPredicatesBuilder =
        ImmutableMap.builder();
    Map<String, String> functionPredicatesMap = contentReader.getContent();
    functionPredicatesMap.forEach(
        (function, serializedPredicates) -> {
          functionPredicatesBuilder.put(
              function, ImmutableSet.copyOf(PREDICATE_SPLITTER.split(serializedPredicates)));
        });
    contentReader.popLevel();
    return functionPredicatesBuilder.build();
  }

  private static ImmutableSet<String> parseGlobals(ContentReader contentReader) {
    String serializedPredicates =
        contentReader.get(SerializePredicatePrecisionOperator.DSS_MESSAGE_GLOBAL_KEY);
    return ImmutableSet.copyOf(PREDICATE_SPLITTER.splitToList(serializedPredicates));
  }

  public static DssPredicatePrecisionPayload fromContentMap(ImmutableMap<String, String> pContent) {
    ContentReader contentReader =
        ContentReader.read(pContent)
            .pushLevel(DssComponentKey.serializedNameFor(PredicatePrecision.class));
    return new DssPredicatePrecisionPayload(
        parseLocationInstances(contentReader),
        parseLocalPredicates(contentReader),
        parseFunctionPredicates(contentReader),
        parseGlobals(contentReader));
  }
}
