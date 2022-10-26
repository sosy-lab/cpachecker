// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.bam;

import com.google.common.base.Preconditions;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class BAMMergeOperator implements MergeOperator {

  private final MergeOperator wrappedMergeOp;
  private @Nullable BAMPCCManager bamPccManager;

  BAMMergeOperator(MergeOperator pWrappedMerge) {
    wrappedMergeOp = pWrappedMerge;
  }

  @CanIgnoreReturnValue
  BAMMergeOperator withBAMPCCManager(BAMPCCManager pBAMPCCManager) {
    Preconditions.checkState(bamPccManager == null);
    bamPccManager = Preconditions.checkNotNull(pBAMPCCManager);
    return this;
  }

  @Override
  public AbstractState merge(AbstractState pState1, AbstractState pState2, Precision pPrecision)
      throws CPAException, InterruptedException {

    // do not merge at block starting states (initial states of a reached-set),
    // because we use this state to identify the block abstraction and the reached-set
    // in the BAMCache and other places.
    if (((ARGState) pState2).getParents().isEmpty()) {
      return pState2;
    }

    AbstractState out = wrappedMergeOp.merge(pState1, pState2, pPrecision);

    if (bamPccManager != null && bamPccManager.isPCCEnabled()) {
      return bamPccManager.attachAdditionalInfoToCallNode(out);
    }

    return out;
  }
}
