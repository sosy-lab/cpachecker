// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Collection;
import java.util.List;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine.CombinePrecisionOperator;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;

public class CombineCompositePrecisionOperator implements CombinePrecisionOperator {

  private final List<ConfigurableProgramAnalysis> wrapped;

  public CombineCompositePrecisionOperator(List<ConfigurableProgramAnalysis> pWrapped) {
    wrapped = pWrapped;
  }

  @Override
  public Precision combine(Collection<Precision> precisions) throws InterruptedException {
    Preconditions.checkArgument(!precisions.isEmpty(), "States cannot be empty");
    Preconditions.checkArgument(
        precisions.stream().allMatch(CompositePrecision.class::isInstance),
        "All precisions must be of type CompositePrecision but are %s",
        precisions);
    Preconditions.checkArgument(
        precisions.stream()
            .allMatch(
                c -> ((CompositePrecision) c).getWrappedPrecisions().size() == wrapped.size()),
        "All precisions must have the same number of wrapped precisions");
    ImmutableList.Builder<Precision> wrappedPrecisions = ImmutableList.builder();
    for (int i = 0; i < wrapped.size(); i++) {
      ImmutableList.Builder<Precision> precisionsToCombine =
          ImmutableList.builderWithExpectedSize(precisions.size());
      for (Precision precision : precisions) {
        CompositePrecision compositePrecision = (CompositePrecision) precision;
        Precision wrappedPrecision = compositePrecision.getWrappedPrecisions().get(i);
        precisionsToCombine.add(wrappedPrecision);
      }
      ImmutableList<Precision> preparedPrecisions = precisionsToCombine.build();
      if (wrapped.get(i) instanceof DistributedConfigurableProgramAnalysis dcpa) {
        Precision combinePrecision = dcpa.getCombinePrecisionOperator().combine(preparedPrecisions);
        Preconditions.checkState(
            combinePrecision.getClass().equals(preparedPrecisions.getFirst().getClass()));
        wrappedPrecisions.add(combinePrecision);
      } else {
        // TODO: Handle cases where the wrapped analysis does not implement CombinePrecisionOperator
        wrappedPrecisions.add(preparedPrecisions.getFirst());
      }
    }
    return new CompositePrecision(wrappedPrecisions.build());
  }
}
