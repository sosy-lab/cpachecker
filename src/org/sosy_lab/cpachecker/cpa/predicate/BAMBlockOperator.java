// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate;

import static com.google.common.base.Preconditions.checkState;

import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.cpachecker.cfa.blocks.BlockPartitioning;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.util.predicates.BlockOperator;

@Options
public class BAMBlockOperator extends BlockOperator {

  private BlockPartitioning partitioning = null;

  void setPartitioning(BlockPartitioning pPartitioning) {
    checkState(partitioning == null);
    partitioning = pPartitioning;
  }

  @Override
  public boolean isBlockEnd(CFANode loc, int thresholdValue) {
    return super.isBlockEnd(loc, thresholdValue)
        || partitioning.isCallNode(loc)
        || partitioning.isReturnNode(loc);
  }

  @Override
  public boolean alwaysReturnsFalse() {
    return super.alwaysReturnsFalse() && partitioning.getBlocks().isEmpty();
  }

  public BlockPartitioning getPartitioning() {
    checkState(partitioning != null);
    return partitioning;
  }
}
