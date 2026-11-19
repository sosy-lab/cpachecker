// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.combine;

import com.google.common.base.Preconditions;
import java.util.Collection;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

public class CombineSingletonPrecisionOperator implements CombinePrecisionOperator {
  @Override
  public Precision combine(Collection<Precision> precisions) throws InterruptedException {
    Preconditions.checkArgument(precisions.stream().allMatch(SingletonPrecision.class::isInstance));
    return SingletonPrecision.getInstance();
  }
}
