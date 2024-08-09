// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.block;

import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Targetable.TargetInformation;

/**
 * Distributed analyses use independent blocks for analyzing simultaneously. These analyses stop at
 * target locations. This class states whether an analysis reached the entry or exit node of a
 * block. Additionally, the {@link org.sosy_lab.cpachecker.cpa.arg.ARGStopSep} ignores target
 * locations that exclusively contain target information of this type when checking for coverage.
 */
public class ExtendedSpecificationViolationTargetInformation implements TargetInformation {

  private final CFANode node;
  private final boolean violation;

  /**
   * {@link BlockState}s are target states iff either the extended violation is violated or if they
   * reach the final node of a block. This constructor creates a new target information object
   * containing above information.
   *
   * @param pNode The location from which this information originates
   * @param pViolation Whether the extended specification was violated
   */
  public ExtendedSpecificationViolationTargetInformation(CFANode pNode, boolean pViolation) {
    node = pNode;
    violation = pViolation;
  }

  public boolean isViolation() {
    return violation;
  }

  @Override
  public String toString() {
    if (violation) {
      return "Violated extended specification at " + node;
    }
    return "Reached the final node of block at " + node;
  }
}
