// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.composite;

import com.google.common.base.Preconditions;
import java.util.List;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.DistributedConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage.PrecisionCoverageOperator;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.composite.CompositePrecision;

public class CompositePrecisionCoverageOperator implements PrecisionCoverageOperator {

  private final List<ConfigurableProgramAnalysis> wrapped;

  public CompositePrecisionCoverageOperator(List<ConfigurableProgramAnalysis> pWrapped) {
    wrapped = pWrapped;
  }

  @Override
  public boolean isSubsumed(Precision precision1, Precision precision2) {
    CompositePrecision compositePrecision1 = (CompositePrecision) precision1;
    CompositePrecision compositePrecision2 = (CompositePrecision) precision2;
    Preconditions.checkArgument(
        compositePrecision1.getWrappedPrecisions().size()
                == compositePrecision2.getWrappedPrecisions().size()
            && compositePrecision1.getWrappedPrecisions().size() == wrapped.size(),
        "Composite precisions must have the same number of wrapped precisions for coverage check.");
    for (int i = 0; i < wrapped.size(); i++) {
      Precision wrappedPrecision1 = compositePrecision1.getWrappedPrecisions().get(i);
      Precision wrappedPrecision2 = compositePrecision2.getWrappedPrecisions().get(i);
      // TODO: Handle cases where the wrapped analysis is no DCPA and, thus, has no operator
      if (wrapped.get(i) instanceof DistributedConfigurableProgramAnalysis dcpa
          && !dcpa.getPrecisionCoverageOperator()
              .isSubsumed(wrappedPrecision1, wrappedPrecision2)) {
        return false;
      }
    }
    return true;
  }
}
