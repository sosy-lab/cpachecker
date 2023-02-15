// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.predicate;

import static org.sosy_lab.common.collect.Collections3.transformedImmutableSetCopy;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.serialize.SerializePrecisionOperator;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.BlockSummaryMessagePayload;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.predicate.PredicatePrecision;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;

public class SerializePredicatePrecisionOperator implements SerializePrecisionOperator {

  private final FormulaManagerView formulaManagerView;

  public SerializePredicatePrecisionOperator(final FormulaManagerView pFormulaManagerView) {
    formulaManagerView = pFormulaManagerView;
  }

  private String serializeAbstractionPredicate(AbstractionPredicate pPredicate) {
    return formulaManagerView.dumpArbitraryFormula(pPredicate.getSymbolicAtom());
  }

  @Override
  public BlockSummaryMessagePayload serializePrecision(Precision pPrecision) {
    if (pPrecision instanceof PredicatePrecision predicatePrecision) {
      ImmutableSetMultimap.Builder<String, String> locationInstances =
          ImmutableSetMultimap.builder();
      predicatePrecision
          .getLocationInstancePredicates()
          .forEach(
              (l, p) ->
                  locationInstances.put(
                      l.getLocation().getNodeNumber() + "," + l.getInstance(),
                      serializeAbstractionPredicate(p)));
      ImmutableSetMultimap.Builder<String, String> localPredicates = ImmutableSetMultimap.builder();
      predicatePrecision
          .getLocalPredicates()
          .forEach(
              (l, p) ->
                  localPredicates.put(
                      Integer.toString(l.getNodeNumber()), serializeAbstractionPredicate(p)));
      ImmutableSetMultimap.Builder<String, String> functionPredicates =
          ImmutableSetMultimap.builder();
      predicatePrecision
          .getFunctionPredicates()
          .forEach((l, p) -> functionPredicates.put(l, serializeAbstractionPredicate(p)));
      ImmutableSet<String> globalPredicates =
          transformedImmutableSetCopy(
              predicatePrecision.getGlobalPredicates(), p -> serializeAbstractionPredicate(p));
      ImmutableMap<String, Object> serialized =
          ImmutableMap.<String, Object>builder()
              .put("locationInstances", locationInstances.build().asMap())
              .put("localPredicates", localPredicates.build().asMap())
              .put("functionPredicates", functionPredicates.build().asMap())
              .put("global", globalPredicates)
              .buildOrThrow();
      return BlockSummaryMessagePayload.builder()
          .addEntry(PredicatePrecision.class.getName(), serialized)
          .buildPayload();
    }
    throw new AssertionError(
        "Cannot serialize a precision that is not of type "
            + PredicatePrecision.class
            + " (got: "
            + pPrecision.getClass()
            + ")");
  }
}
