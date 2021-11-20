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

public class BlockStartReachedTargetInformation implements TargetInformation {

  private final CFANode node;

  public BlockStartReachedTargetInformation(CFANode pNode) {
    node = pNode;
  }

  @Override
  public String toString() {
    return "Reached block start at Node " + node;
  }
}
