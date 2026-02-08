// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.communication.messages.ContentBuilder;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializePrecisionOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

public class SerializePredicatePrecisionOperator implements SerializePrecisionOperator {

  private final FormulaManagerView formulaManagerView;

  /** Keys used in the serialized message for global predicates. */
  public static final String DSS_MESSAGE_GLOBAL_KEY = "global";

  /** Key used in the serialized message for location-instance specific predicates. */
  public static final String DSS_MESSAGE_LOCATION_INSTANCES_KEY = "locationInstances";

  /** Key used in the serialized message for location specific predicates. */
  public static final String DSS_MESSAGE_LOCAL_PREDICATES_KEY = "localPredicates";

  /** Key used in the serialized message for function specific predicates. */
  public static final String DSS_MESSAGE_FUNCTION_PREDICATES_KEY = "functionPredicates";

  public SerializePredicatePrecisionOperator(final FormulaManagerView pFormulaManagerView) {
    formulaManagerView = pFormulaManagerView;
  }

  private String serializeAbstractionPredicate(AbstractionPredicate pPredicate) {
    return formulaManagerView.dumpFormula(pPredicate.getSymbolicAtom()).toString();
  }

  @Override
  public ImmutableMap<String, String> serializePrecision(Precision pPrecision) {
    if (!(pPrecision instanceof PredicatePrecision predicatePrecision)) {
      throw new AssertionError(
          "Cannot serialize a precision that is not of type "
              + PredicatePrecision.class
              + " (got: "
              + pPrecision.getClass()
              + ")");
    }
    ContentBuilder contentBuilder =
        ContentBuilder.builder().pushLevel(PredicatePrecision.class.getName());

    contentBuilder.pushLevel(DSS_MESSAGE_LOCATION_INSTANCES_KEY);
    Multimap<String, String> locationInstancePredicates = ArrayListMultimap.create();
    predicatePrecision
        .getLocationInstancePredicates()
        .forEach(
            (l, p) ->
                locationInstancePredicates.put(
                    l.getLocation().getNodeNumber() + "," + l.getInstance(),
                    serializeAbstractionPredicate(p)));
    for (String key : locationInstancePredicates.keySet()) {
      contentBuilder.put(key, Joiner.on(" , ").join(locationInstancePredicates.get(key)));
    }
    contentBuilder.popLevel();

    contentBuilder.pushLevel(DSS_MESSAGE_LOCAL_PREDICATES_KEY);
    Multimap<String, String> localPredicates = ArrayListMultimap.create();
    predicatePrecision
        .getLocalPredicates()
        .forEach(
            (l, p) ->
                localPredicates.put(
                    Integer.toString(l.getNodeNumber()), serializeAbstractionPredicate(p)));
    for (String key : localPredicates.keySet()) {
      contentBuilder.put(key, Joiner.on(" , ").join(localPredicates.get(key)));
    }
    contentBuilder.popLevel();

    contentBuilder.pushLevel(DSS_MESSAGE_FUNCTION_PREDICATES_KEY);
    Multimap<String, String> functionPredicates = ArrayListMultimap.create();
    predicatePrecision
        .getFunctionPredicates()
        .forEach((l, p) -> functionPredicates.put(l, serializeAbstractionPredicate(p)));
    for (String key : functionPredicates.keySet()) {
      contentBuilder.put(key, Joiner.on(" , ").join(functionPredicates.get(key)));
    }
    contentBuilder.popLevel();

    contentBuilder.put(
        DSS_MESSAGE_GLOBAL_KEY,
        Joiner.on(" , ")
            .join(
                transformedImmutableSetCopy(
                    predicatePrecision.getGlobalPredicates(),
                    this::serializeAbstractionPredicate)));
    return contentBuilder.build();
  }
}
