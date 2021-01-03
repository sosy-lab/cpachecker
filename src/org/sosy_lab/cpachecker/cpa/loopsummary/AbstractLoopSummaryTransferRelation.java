// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.loopsummary;

import com.google.common.collect.ImmutableSet;
import java.util.Collection;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.AbstractSingleWrapperTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public abstract class AbstractLoopSummaryTransferRelation<EX extends CPAException>
    extends AbstractSingleWrapperTransferRelation implements TransferRelation {

  protected final LogManager logger;
  protected final ShutdownNotifier shutdownNotifier;

  protected AbstractLoopSummaryTransferRelation(
      AbstractLoopSummaryCPA pLoopSummaryCPA, ShutdownNotifier pShutdownNotifier) {
    super(pLoopSummaryCPA.getWrappedCpa().getTransferRelation());
    logger = pLoopSummaryCPA.getLogger();
    shutdownNotifier = pShutdownNotifier;
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge) {

    throw new UnsupportedOperationException("Unimplemented");
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessors(
      final AbstractState pState, final Precision pPrecision) throws InterruptedException {
      return ImmutableSet.of();
  }
}
