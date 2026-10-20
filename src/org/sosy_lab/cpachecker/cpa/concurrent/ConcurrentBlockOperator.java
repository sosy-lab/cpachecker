// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.concurrent;

import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;

@Options
public final class ConcurrentBlockOperator extends BlockOperator {

  @Override
  public boolean isBlockEnd(CFANode loc, int thresholdValue) {
    return super.isBlockEnd(ConcurrentEdgeCloner.getOriginalNode(loc), thresholdValue);
  }
}
