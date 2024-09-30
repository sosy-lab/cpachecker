// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.tube;


import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.interfaces.Targetable.TargetInformation;

/**
 * Distributed analyses use independent blocks for analyzing simultaneously. These analyses stop at
 * target locations. This class states whether an analysis reached the entry or exit node of a
 * block.
 */
public class TubeEntryReachedTargetInformation implements TargetInformation {

  private final CFANode node;

  public TubeEntryReachedTargetInformation(CFANode pNode) {
    node = pNode;
  }

  @Override
  public String toString() {
    return "Reached tube start at Node " + node;
  }
}
