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
}
