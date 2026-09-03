// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.distributed_cpa.operators.coverage;

import com.google.common.base.Preconditions;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.Precision;

/**
 * Coverage operator for CPAs whose precision carries no information. All {@link
 * SingletonPrecision}s track the same (empty) set of information and, thus, cover each other.
 */
public class SingletonPrecisionCoverageOperator implements PrecisionCoverageOperator {

  @Override
  public boolean isSubsumed(Precision precision1, Precision precision2) {
    Preconditions.checkArgument(
        precision1 instanceof SingletonPrecision && precision2 instanceof SingletonPrecision,
        "Expected SingletonPrecisions, but got %s and %s",
        precision1,
        precision2);
    return true;
  }
}
