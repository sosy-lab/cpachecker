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
 * An operator to combine multiple precisions into a single precision, e.g., the precisions that
 * different blocks send along with their conditions.
 *
 * <p>Contract: the combined precision is at least as strong as every given precision, i.e., it
 * tracks everything that any of them tracks. It must not track more than the given precisions
 * together, so combining a precision with one it already covers yields an equal precision. Dropping
 * any part would force the analysis to refine it again and again.
 */
public interface CombinePrecisionOperator {

  /**
   * Combine the given precisions into their union.
   *
   * @param precisions the precisions to combine, must not be empty
   * @return a precision at least as strong as every precision in {@code precisions}
   */
  Precision combine(Collection<Precision> precisions) throws InterruptedException;
}
