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
public class BlockTargetInformation implements TargetInformation {

  private final CFANode node;
  private final boolean abstraction;

  public BlockTargetInformation(CFANode pNode, boolean pAbstraction) {
    node = pNode;
    abstraction = pAbstraction;
  }

  public boolean isAbstraction() {
    return abstraction;
  }

  @Override
  public String toString() {
    if (abstraction) {
      return "Reached an abstraction location at " + node;
    }
    return "Reached an entry point of a BlockNode at " + node;
  }
}
