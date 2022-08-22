// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa;

import java.util.Collection;
import java.util.stream.Stream;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.exchange.actor_messages.BlockSummaryErrorConditionMessage;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public interface BlockSummaryErrorConditionTracker {

  /**
   * Returns the closest approximation of the latest {@link BlockSummaryErrorConditionMessage} This
   * method guarantees to return an uninstantiated {@link BooleanFormula} in the context of {@code
   * pFormulaManagerView}.
   *
   * @param pFormulaManagerView {@link FormulaManagerView} that provides the context for the boolean
   *     formula
   * @return the error condition as {@link BooleanFormula}
   */
  BooleanFormula getErrorCondition(FormulaManagerView pFormulaManagerView);

  void setErrorCondition(BooleanFormula pFormula);

  /**
   * Updates the error condition with a newly received {@link BlockSummaryErrorConditionMessage}.
   *
   * @param pMessage Received {@link BlockSummaryErrorConditionMessage}
   */
  void updateErrorCondition(BlockSummaryErrorConditionMessage pMessage);

  /**
   * Returns the current error condition and resets it to top.
   *
   * @return The error condition before the reset.
   */
  BooleanFormula resetErrorCondition(FormulaManagerView pFormulaManagerView);

  /**
   * Convert the given collection to a collection of {@link BlockSummaryErrorConditionTracker}.
   * Filters all objects in the collection that are not an instance of {@link
   * BlockSummaryErrorConditionTracker}.
   *
   * @param pCollection Collection of arbitrary objects.
   * @return Stream of {@link BlockSummaryErrorConditionTracker}s.
   */
  static Stream<BlockSummaryErrorConditionTracker> trackersFrom(Collection<?> pCollection) {
    return pCollection.stream()
        .filter(element -> element instanceof BlockSummaryErrorConditionTracker)
        .map(element -> (BlockSummaryErrorConditionTracker) element);
  }

  // TODO: maybe make generic and implement it in the style of precision (composite precision,
  // strengthening)
}
