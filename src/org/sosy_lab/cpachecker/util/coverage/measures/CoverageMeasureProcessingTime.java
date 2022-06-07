// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.measures;

/**
 * Specify if the measure needs coverage data collection during the analysis or after the analysis
 * is done.
 */
public enum CoverageMeasureProcessingTime {
  AFTER_ANALYSIS,
  DURING_ANALYSIS
}
