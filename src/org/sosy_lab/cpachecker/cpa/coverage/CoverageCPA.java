// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.coverage;

import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.util.coverage.collectors.CoverageCollectorHandler;

/** CPAs which calculates TDCG and Coverage Measure data during the analysis. */
public interface CoverageCPA extends ConfigurableProgramAnalysis {
  /**
   * Returns a CoverageCollectorHandler which is used to access all relevant coverage measures and
   * TDCGs.
   *
   * @return unique CoverageCollectorHandler which was init in the beginning of the whole analysis
   *     process
   */
  CoverageCollectorHandler getCoverageCollectorHandler();
}
