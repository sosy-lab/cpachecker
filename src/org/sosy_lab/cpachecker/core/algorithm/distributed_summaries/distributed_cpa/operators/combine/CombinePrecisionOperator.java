// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine;

import java.util.Collection;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

/**
 * An operator to combine multiple precisions into a single precision. This is useful in distributed
 * CPA settings where precisions from different analysis nodes need to be merged.
 *
 * <p>The resulting precision must follow the contract that the least upper bound of the transfer
 * from one state to another with each individual precision is equivalent to the transfer with the
 * combined precision.
 */
public interface CombinePrecisionOperator {

  Precision combine(Collection<Precision> precisions) throws InterruptedException;

  /**
   * The union of the given precisions: everything any of them tracks is tracked by the result.
   *
   * <p>Unlike {@link #combine}, which may leave out parts of the given precisions to keep the
   * result small, this never loses anything. Use it to accumulate a precision, e.g., over the
   * explorations of one block, where losing a part means refining it again and again.
   */
  default Precision union(Collection<Precision> precisions) throws InterruptedException {
    return combine(precisions);
  }

  /** Whether the second precision already tracks everything represented by the first. */
  default boolean isCoveredBy(Precision pPrecision, Precision pOther) throws InterruptedException {
    return pPrecision.equals(pOther);
  }
}
