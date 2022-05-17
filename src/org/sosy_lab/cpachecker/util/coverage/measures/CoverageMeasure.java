// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.coverage.measures;

/**
 * A coverage measure is used to determine the verification coverage of the analyzed program
 * depending on specific criteria, e.g., Line-based or Location-based.
 */
public interface CoverageMeasure {
  /**
   * Getter method for the coverage value of the analyzed program-
   *
   * @return the normalized coverage value which can be a double between 0 and 1.
   */
  double getCoverage();

  /**
   * Getter method for the total count of some verification coverage data.
   *
   * @return the coverage value without normalization, which is therefore just the count of some
   *     specific verification coverage data analyzed.
   */
  double getCount();

  /**
   * Getter method for the maximum possible count of some verification coverage data. Normally used
   * for normalization.
   *
   * @return the highest possible count for this measure
   */
  double getMaxCount();
}
