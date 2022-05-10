// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.measures;

/** Coverage Measure used for the case if none measure should be applied */
public class UndefinedCoverageMeasure implements CoverageMeasure {
  /* ##### Interface Implementations ##### */
  @Override
  public double getCoverage() {
    return 0;
  }

  @Override
  public double getCount() {
    return 0;
  }

  @Override
  public double getMaxCount() {
    return 1;
  }
}
