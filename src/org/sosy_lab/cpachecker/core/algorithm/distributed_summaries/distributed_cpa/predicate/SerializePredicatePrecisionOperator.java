// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializePrecisionOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

public class SerializePredicatePrecisionOperator implements SerializePrecisionOperator {

  private final Map<CFANode, Integer> nodeToId;
  private final FormulaManagerView formulaManagerView;

  /** Keys used in the serialized message for global predicates. */
  public static final String DSS_MESSAGE_GLOBAL_KEY = "global";

  /** Key used in the serialized message for location-instance specific predicates. */
  public static final String DSS_MESSAGE_LOCATION_INSTANCES_KEY = "locationInstances";

  /** Key used in the serialized message for location specific predicates. */
  public static final String DSS_MESSAGE_LOCAL_PREDICATES_KEY = "localPredicates";

  /** Key used in the serialized message for function specific predicates. */
  public static final String DSS_MESSAGE_FUNCTION_PREDICATES_KEY = "functionPredicates";

  public SerializePredicatePrecisionOperator(
      final FormulaManagerView pFormulaManagerView, Map<CFANode, Integer> pNodeToId) {
    nodeToId = pNodeToId;
    formulaManagerView = pFormulaManagerView;
  }

  private String serializeAbstractionPredicate(AbstractionPredicate pPredicate) {
    return formulaManagerView.dumpFormula(pPredicate.getSymbolicAtom()).toString();
  }

  @Override
  public DssPredicatePrecisionPayload serializePrecision(Precision pPrecision) {
    if (!(pPrecision instanceof PredicatePrecision predicatePrecision)) {
      throw new AssertionError(
          "Cannot serialize a precision that is not of type "
              + PredicatePrecision.class
              + " (got: "
              + pPrecision.getClass()
              + ")");
    }

    Multimap<DssLocationInstancePayload, String> locationInstancePredicatesMultimap =
        ArrayListMultimap.create();
    predicatePrecision
        .getLocationInstancePredicates()
        .forEach(
            (l, p) ->
                locationInstancePredicatesMultimap.put(
                    new DssLocationInstancePayload(nodeToId.get(l.getLocation()), l.getInstance()),
                    serializeAbstractionPredicate(p)));
    ImmutableMap.Builder<DssLocationInstancePayload, ImmutableSet<String>>
        locationInstancePredicates = ImmutableMap.builder();
    locationInstancePredicatesMultimap
        .asMap()
        .forEach((l, p) -> locationInstancePredicates.put(l, ImmutableSet.copyOf(p)));

    Multimap<Integer, String> localPredicatesMultimap = ArrayListMultimap.create();
    predicatePrecision
        .getLocalPredicates()
        .forEach(
            (l, p) ->
                localPredicatesMultimap.put(nodeToId.get(l), serializeAbstractionPredicate(p)));
    ImmutableMap.Builder<Integer, ImmutableSet<String>> localPredicates = ImmutableMap.builder();
    localPredicatesMultimap
        .asMap()
        .forEach((l, p) -> localPredicates.put(l, ImmutableSet.copyOf(p)));

    Multimap<String, String> functionPredicatesMultimap = ArrayListMultimap.create();
    predicatePrecision
        .getFunctionPredicates()
        .forEach((l, p) -> functionPredicatesMultimap.put(l, serializeAbstractionPredicate(p)));
    ImmutableMap.Builder<String, ImmutableSet<String>> functionPredicates = ImmutableMap.builder();
    functionPredicatesMultimap
        .asMap()
        .forEach((l, p) -> functionPredicates.put(l, ImmutableSet.copyOf(p)));

    ImmutableSet<String> globalPredicates =
        transformedImmutableSetCopy(
            predicatePrecision.getGlobalPredicates(), this::serializeAbstractionPredicate);

    return new DssPredicatePrecisionPayload(
        locationInstancePredicates.build(),
        localPredicates.build(),
        functionPredicates.build(),
        globalPredicates);
  }
}
