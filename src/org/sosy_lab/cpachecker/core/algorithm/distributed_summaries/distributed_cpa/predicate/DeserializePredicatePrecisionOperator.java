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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.deserialize.DeserializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryMessage;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision.LocationInstance;
import org.sosy_lab.cpachecker.util.predicates.AbstractionManager;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.Solver;

public class DeserializePredicatePrecisionOperator implements DeserializePrecisionOperator {

  private final Solver solver;

  private final Function<Integer, CFANode> nodeMapping;
  private final AbstractionManager abstractionManager;

  public DeserializePredicatePrecisionOperator(
      final AbstractionManager pAbstractionManager,
      final Solver pSolver,
      final Function<Integer, CFANode> pNodeMapping) {
    solver = pSolver;
    nodeMapping = pNodeMapping;
    abstractionManager = pAbstractionManager;
  }

  @Override
  public Precision deserializePrecision(BlockSummaryMessage pMessage) {
    Optional<Object> precision = pMessage.getPrecision(PredicatePrecision.class);
    if (precision.isEmpty()) {
      return PredicatePrecision.empty();
    }
    Object extractedPrecision = precision.orElseThrow();
    if (extractedPrecision instanceof Map<?, ?> precisionMap) {
      ImmutableListMultimap.Builder<LocationInstance, AbstractionPredicate> locationInstances =
          ImmutableListMultimap.builder();
      ImmutableListMultimap.Builder<CFANode, AbstractionPredicate> localPredicates =
          ImmutableListMultimap.builder();
      ImmutableListMultimap.Builder<String, AbstractionPredicate> functionPredicates =
          ImmutableListMultimap.builder();

      Map<?, ?> locationInstanceMap = (Map<?, ?>) precisionMap.get("locationInstances");
      locationInstanceMap.forEach(
          (l, p) -> {
            if (p instanceof Iterable<?> iterable) {
              List<String> parts = Splitter.on(",").splitToList(l.toString());
              LocationInstance locationInstance =
                  new LocationInstance(
                      nodeMapping.apply(Integer.parseInt(parts.get(0))),
                      Integer.parseInt(parts.get(1)));
              for (Object o : iterable) {
                locationInstances.put(
                    locationInstance,
                    abstractionManager.makePredicate(
                        solver.getFormulaManager().parse(o.toString())));
              }
            }
          });

      Map<?, ?> localPredicatesMap = (Map<?, ?>) precisionMap.get("localPredicates");
      localPredicatesMap.forEach(
          (l, p) -> {
            if (p instanceof Iterable<?> iterable) {
              for (Object o : iterable) {
                localPredicates.put(
                    nodeMapping.apply(Integer.parseInt(l.toString())),
                    abstractionManager.makePredicate(
                        solver.getFormulaManager().parse(o.toString())));
              }
            }
          });

      Map<?, ?> functionPredicatesMap = (Map<?, ?>) precisionMap.get("functionPredicates");
      functionPredicatesMap.forEach(
          (l, p) -> {
            if (p instanceof Iterable<?> iterable) {
              for (Object o : iterable) {
                functionPredicates.put(
                    l.toString(),
                    abstractionManager.makePredicate(
                        solver.getFormulaManager().parse(o.toString())));
              }
            }
          });

      Collection<?> globalsCollection = (Collection<?>) precisionMap.get("global");
      ImmutableSet<AbstractionPredicate> globals =
          transformedImmutableSetCopy(
              globalsCollection,
              o ->
                  abstractionManager.makePredicate(solver.getFormulaManager().parse(o.toString())));

      return new PredicatePrecision(
          locationInstances.build(), localPredicates.build(), functionPredicates.build(), globals);
    }
    throw new AssertionError(
        "Expected a map describing the precision but got " + extractedPrecision.getClass());
  }
}
