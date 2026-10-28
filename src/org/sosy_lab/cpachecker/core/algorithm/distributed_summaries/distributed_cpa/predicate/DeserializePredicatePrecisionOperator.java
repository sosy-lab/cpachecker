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
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentReader;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.DssMessage;
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

  /**
   * Splitter for the predicate lists written by {@link SerializePredicatePrecisionOperator}, which
   * joins them with {@code " , "}. {@code omitEmptyStrings()} is essential: joining an empty
   * collection yields {@code ""}, and splitting {@code ""} without it yields a single blank element
   * instead of none. A blank element would reach {@link AbstractionManager#parsePredicate(String)},
   * which maps blanks to {@code false}, so an empty predicate list would deserialize into the
   * "false" predicate and render the whole precision (and, through {@link PredicatePrecision}'s
   * eager propagation of globals, every function and location key) unusable.
   */
  private static final Splitter PREDICATE_SPLITTER = Splitter.on(" , ").omitEmptyStrings();

  private final Function<Integer, CFANode> nodeMapping;
  private final AbstractionManager abstractionManager;

  public DeserializePredicatePrecisionOperator(
      final AbstractionManager pAbstractionManager, final Function<Integer, CFANode> pNodeMapping) {
    nodeMapping = pNodeMapping;
    abstractionManager = pAbstractionManager;
  }

  private Multimap<LocationInstance, AbstractionPredicate> parseLocationInstances(
      ContentReader contentReader) {
    contentReader.pushLevel(SerializePredicatePrecisionOperator.DSS_MESSAGE_LOCATION_INSTANCES_KEY);
    Map<String, String> locationInstanceMap = contentReader.getContent();
    ImmutableListMultimap.Builder<LocationInstance, AbstractionPredicate> locationInstances =
        ImmutableListMultimap.builder();
    locationInstanceMap.forEach(
        (nodeNumberAndLocationInstance, serializedPredicates) -> {
          List<String> splitNodeNumberAndLocationInstance =
              Splitter.on(",").splitToList(nodeNumberAndLocationInstance);
          LocationInstance locationInstance =
              new LocationInstance(
                  nodeMapping.apply(
                      Integer.parseInt(splitNodeNumberAndLocationInstance.getFirst())),
                  Integer.parseInt(splitNodeNumberAndLocationInstance.get(1)));
          for (String precision : PREDICATE_SPLITTER.split(serializedPredicates)) {
            locationInstances.put(locationInstance, abstractionManager.parsePredicate(precision));
          }
        });
    contentReader.popLevel();
    return locationInstances.build();
  }

  private Multimap<CFANode, AbstractionPredicate> parseLocalPredicates(
      ContentReader contentReader) {
    contentReader.pushLevel(SerializePredicatePrecisionOperator.DSS_MESSAGE_LOCAL_PREDICATES_KEY);
    Map<String, String> localPredicatesMap = contentReader.getContent();
    ImmutableListMultimap.Builder<CFANode, AbstractionPredicate> localPredicates =
        ImmutableListMultimap.builder();
    localPredicatesMap.forEach(
        (location, serializedPrecisions) -> {
          for (String precision : PREDICATE_SPLITTER.split(serializedPrecisions)) {
            localPredicates.put(
                Objects.requireNonNull(nodeMapping.apply(Integer.parseInt(location))),
                abstractionManager.parsePredicate(precision));
          }
        });
    contentReader.popLevel();
    return localPredicates.build();
  }

  private Multimap<String, AbstractionPredicate> parseFunctionPredicates(
      ContentReader contentReader) {
    contentReader.pushLevel(
        SerializePredicatePrecisionOperator.DSS_MESSAGE_FUNCTION_PREDICATES_KEY);
    ImmutableListMultimap.Builder<String, AbstractionPredicate> functionPredicates =
        ImmutableListMultimap.builder();
    Map<String, String> functionPredicatesMap = contentReader.getContent();
    functionPredicatesMap.forEach(
        (function, serializedPredicates) -> {
          for (String predicate : PREDICATE_SPLITTER.split(serializedPredicates)) {
            functionPredicates.put(function, abstractionManager.parsePredicate(predicate));
          }
        });
    contentReader.popLevel();
    return functionPredicates.build();
  }

  private ImmutableSet<AbstractionPredicate> parseGlobals(ContentReader contentReader) {
    // An absent key must behave exactly like the empty value the serializer writes for a precision
    // without global predicates.
    String serializedPredicates =
        contentReader.getOrDefault(SerializePredicatePrecisionOperator.DSS_MESSAGE_GLOBAL_KEY, "");
    return transformedImmutableSetCopy(
        PREDICATE_SPLITTER.splitToList(serializedPredicates),
        predicate -> abstractionManager.parsePredicate(predicate));
  }

  @Override
  public Precision deserializePrecision(DssMessage pMessage) {
    ContentReader contentReader = pMessage.getPrecisionContent(PredicatePrecision.class);
    return new PredicatePrecision(
        parseLocationInstances(contentReader),
        parseLocalPredicates(contentReader),
        parseFunctionPredicates(contentReader),
        parseGlobals(contentReader));
  }
}
