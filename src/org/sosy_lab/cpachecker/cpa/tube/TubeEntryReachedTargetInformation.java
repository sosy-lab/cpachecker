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
 * This class represents information about reaching the start of a target tube in a control flow
 * graph.
 */
public class TubeEntryReachedTargetInformation implements TargetInformation {

  /** Represents a control flow graph node in the context of reaching the start of a target tube. */
  private final CFANode node;

  /**
   * Construct an object to store information about reaching the start of a target tube in a control
   * flow graph.
   *
   * @param pNode the CFANode that represents the start of the target tube
   */
  public TubeEntryReachedTargetInformation(CFANode pNode) {
    node = pNode;
  }

  @Override
  public String toString() {
    return "Reached tube start at Node " + node;
  }
}
