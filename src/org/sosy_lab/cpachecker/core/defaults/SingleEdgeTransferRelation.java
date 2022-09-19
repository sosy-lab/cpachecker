// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import java.util.Collection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

/**
 * Abstract base class for {@link TransferRelation}, which should be used by most CPAs.
 *
 * <p>It eliminates the need to implement a stub for {@link #getAbstractSuccessors(AbstractState,
 * Precision)}.
 */
public abstract class SingleEdgeTransferRelation implements TransferRelation {

  @Override
  public final Collection<? extends AbstractState> getAbstractSuccessors(
      AbstractState pState, Precision pPrecision)
      throws CPATransferException, InterruptedException {

    throw new UnsupportedOperationException(
        "The "
            + this.getClass().getSimpleName()
            + " expects to be called with a CFA edge supplied"
            + " and does not support configuration where it needs to"
            + " return abstract states for any CFA edge.");
  }
}
