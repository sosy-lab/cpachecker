// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.invariants;

import com.google.common.collect.Iterables;
import java.util.Collection;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombinePrecisionOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.invariants.InvariantsPrecision;

public class CombineInvariantsPrecisionOperator implements CombinePrecisionOperator {

  @Override
  public Precision combine(Collection<Precision> precisions) throws InterruptedException {
    if (precisions.isEmpty()) {
      throw new IllegalArgumentException("Cannot combine empty collection of InvariantsPrecisions");
    }
    return InvariantsPrecision.getEmptyPrecision(
        ((InvariantsPrecision) Iterables.get(precisions, 0)).getAbstractionStrategy());
  }
}
