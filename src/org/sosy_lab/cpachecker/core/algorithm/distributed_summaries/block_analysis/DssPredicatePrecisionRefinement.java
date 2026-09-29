// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.block_analysis;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import java.util.HashSet;
import java.util.Map;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision.LocationInstance;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;

/** Applies ordinary predicate-refinement sharing rules to exact DSS boundary interpolants. */
@Options(prefix = "cpa.predicate")
final class DssPredicatePrecisionRefinement {
  @Option(
      secure = true,
      name = "precision.sharing",
      description = "Where to apply the found predicates to?")
  private PredicateSharing predicateSharing = PredicateSharing.LOCATION;

  private enum PredicateSharing {
    GLOBAL,
    SCOPE,
    FUNCTION,
    LOCATION,
    LOCATION_INSTANCE
  }

  DssPredicatePrecisionRefinement(Configuration pConfiguration)
      throws InvalidConfigurationException {
    pConfiguration.inject(this);
  }

  PredicatePrecision addPredicates(
      PredicatePrecision basePrecision,
      Multimap<LocationInstance, AbstractionPredicate> newPredicates) {
    return switch (predicateSharing) {
      case GLOBAL -> basePrecision.addGlobalPredicates(newPredicates.values());
      case SCOPE -> {
        var global = new HashSet<AbstractionPredicate>();
        var local = ArrayListMultimap.<LocationInstance, AbstractionPredicate>create();
        for (var entry : newPredicates.entries()) {
          // Keep the same scope convention as the ordinary predicate refiner: predicates
          // mentioning a local variable stay at their location; global predicates are shared.
          if (entry.getValue().getSymbolicAtom().toString().contains("::")) {
            local.put(entry.getKey(), entry.getValue());
          } else {
            global.add(entry.getValue());
          }
        }
        yield basePrecision
            .addGlobalPredicates(global)
            .addLocalPredicates(mergePredicatesPerLocation(local.entries()));
      }
      case FUNCTION ->
          basePrecision.addFunctionPredicates(
              from(newPredicates.entries())
                  .transform(e -> Maps.immutableEntry(e.getKey().getFunctionName(), e.getValue())));
      case LOCATION ->
          basePrecision.addLocalPredicates(mergePredicatesPerLocation(newPredicates.entries()));
      case LOCATION_INSTANCE ->
          basePrecision.addLocationInstancePredicates(newPredicates.entries());
    };
  }

  private static Iterable<Map.Entry<CFANode, AbstractionPredicate>> mergePredicatesPerLocation(
      Iterable<Map.Entry<LocationInstance, AbstractionPredicate>> predicates) {
    return from(predicates)
        .transform(e -> Maps.immutableEntry(e.getKey().getLocation(), e.getValue()));
  }
}
