// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.base.Function;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage.DssMessageType;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.payloads.DssPrecisionPayload;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializePrecisionOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision.LocationInstance;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

/**
 * Operator to deserialize PredicatePrecisions from DssMessages. The operator deserializes the
 * following parts of a PredicatePrecision:
 *
 * <ul>
 *   <li>Location-instance specific predicates
 *   <li>Location specific predicates
 *   <li>Function specific predicates
 *   <li>Global predicates
 * </ul>
 */
public class DeserializePredicatePrecisionOperator implements DeserializePrecisionOperator {

  private final Function<Integer, CFANode> nodeMapping;
  // Scoped to this solver context: incoming messages repeatedly carry the same predicates.
  private final LoadingCache<String, AbstractionPredicate> parsedPredicates;

  public DeserializePredicatePrecisionOperator(
      final AbstractionManager pAbstractionManager, final Function<Integer, CFANode> pNodeMapping) {
    nodeMapping = pNodeMapping;
    parsedPredicates =
        CacheBuilder.newBuilder()
            .maximumSize(1024)
            .build(CacheLoader.from(pAbstractionManager::parsePredicate));
  }

  private Multimap<LocationInstance, AbstractionPredicate> parseLocationInstances(
      ImmutableList<DssLocationInstancePredicatesPayload> pLocalInstances) {
    ImmutableListMultimap.Builder<LocationInstance, AbstractionPredicate> locationInstances =
        ImmutableListMultimap.builder();
    pLocalInstances.forEach(entry -> {
      DssLocationInstancePayload locationInstancePayload = entry.locationInstance();
      ImmutableSet<String> serializedPredicates = ImmutableSet.copyOf(entry.predicates());
      LocationInstance locationInstance =
          new LocationInstance(
              nodeMapping.apply(locationInstancePayload.nodeId()),
              locationInstancePayload.instance());
      for (String precision : serializedPredicates) {
        locationInstances.put(locationInstance, parsedPredicates.getUnchecked(precision));
      }
    });
    return locationInstances.build();
  }

  private Multimap<CFANode, AbstractionPredicate> parseLocalPredicates(
      ImmutableMap<Integer, ImmutableSet<String>> pLocalPredicates) {
    ImmutableListMultimap.Builder<CFANode, AbstractionPredicate> localPredicates =
        ImmutableListMultimap.builder();
    pLocalPredicates.forEach((location, serializedPrecisions) -> {
      for (String precision : serializedPrecisions) {
        localPredicates.put(
            Objects.requireNonNull(nodeMapping.apply(location)),
            parsedPredicates.getUnchecked(precision));
      }
    });
    return localPredicates.build();
  }

  private Multimap<String, AbstractionPredicate> parseFunctionPredicates(
      ImmutableMap<String, ImmutableSet<String>> pFunctionPredicates) {
    ImmutableListMultimap.Builder<String, AbstractionPredicate> functionPredicates =
        ImmutableListMultimap.builder();
    pFunctionPredicates.forEach((function, serializedPredicates) -> {
      for (String predicate : serializedPredicates) {
        functionPredicates.put(function, parsedPredicates.getUnchecked(predicate));
      }
    });
    return functionPredicates.build();
  }

  private ImmutableSet<AbstractionPredicate> parseGlobals(ImmutableSet<String> pGlobalPredicates) {
    return transformedImmutableSetCopy(
        pGlobalPredicates, predicate -> parsedPredicates.getUnchecked(predicate));
  }

  @Override
  public Precision deserializePrecision(
      DssPrecisionPayload pPrecisionPayload, DssMessageType pMessageType) {
    DssPredicatePrecisionPayload payload = (DssPredicatePrecisionPayload) pPrecisionPayload;
    return new PredicatePrecision(
        parseLocationInstances(payload.locationInstances()),
        parseLocalPredicates(payload.localPredicates()),
        parseFunctionPredicates(payload.functionPredicates()),
        parseGlobals(payload.globalPredicates()));
  }
}
