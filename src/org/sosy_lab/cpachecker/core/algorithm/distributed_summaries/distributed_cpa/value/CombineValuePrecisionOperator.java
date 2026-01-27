// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.value;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombinePrecisionOperator;
import org.sosy_lab.cpachecker.core.defaults.precision.ConfigurablePrecision;
import org.sosy_lab.cpachecker.core.defaults.precision.ScopedRefinablePrecision;
import org.sosy_lab.cpachecker.core.defaults.precision.VariableTrackingPrecision;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class CombineValuePrecisionOperator implements CombinePrecisionOperator {

  @Override
  public Precision combine(Collection<Precision> precisions) throws InterruptedException {

    if (precisions.stream().allMatch(ConfigurablePrecision.class::isInstance)) {
      return precisions.iterator().next();
    }

    Preconditions.checkArgument(
        precisions.stream().allMatch(ScopedRefinablePrecision.class::isInstance));
    List<VariableTrackingPrecision> baselines =
        precisions.stream()
            .map(e -> ((ScopedRefinablePrecision) e).getBaseline())
            .distinct()
            .toList();

    assert baselines.size() == 1 : "Can't combine precisions with different or no baselines";

    Set<MemoryLocation> rawPrecision =
        precisions.stream()
            .map(e -> ((ScopedRefinablePrecision) e).getRawPrecision())
            .flatMap(Set::stream)
            .collect(ImmutableSet.toImmutableSet());

    return new ScopedRefinablePrecision(baselines.getFirst(), rawPrecision);
  }
}
