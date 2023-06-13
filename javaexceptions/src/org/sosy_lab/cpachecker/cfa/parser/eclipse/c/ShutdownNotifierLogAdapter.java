// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import com.google.common.base.Preconditions;
import org.eclipse.cdt.core.parser.AbstractParserLogService;
import org.eclipse.cdt.internal.core.util.ICancelable;
import org.eclipse.cdt.internal.core.util.ICanceler;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.ShutdownNotifier.ShutdownRequestListener;

/**
 * A simple adapter class that can be used as logger for the Eclipse parser and forwards our
 * shutdown requests into the parser classes.
 */
class ShutdownNotifierLogAdapter extends AbstractParserLogService
    implements ICanceler, ShutdownRequestListener {

  private final ShutdownNotifier shutdownNotifier;
  private ICancelable cancelable; // the eclipse parser will be stored here

  ShutdownNotifierLogAdapter(ShutdownNotifier pShutdownNotifier) {
    shutdownNotifier = pShutdownNotifier;
    shutdownNotifier.register(this);
  }

  @Override
  public void setCancelable(ICancelable pCancelable) {
    synchronized (this) {
      Preconditions.checkState(
          pCancelable == null || cancelable == null, "either set or unset, there is no override");
      cancelable = pCancelable;
    }
  }

  @Override
  public void setCanceled(boolean cancel) {
    synchronized (this) {
      if (cancel && cancelable != null) {
        cancelable.cancel();
      }
    }
  }

  @Override
  public boolean isCanceled() {
    return shutdownNotifier.shouldShutdown();
  }

  @Override
  public void shutdownRequested(String pReason) {
    setCanceled(true);
  }
}
