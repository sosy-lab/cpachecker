// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.exceptions.CPAException;

/**
 * This class implements the PrecisionAdjustment operator for a CPA, where the precision never
 * changes. It does not make any assumptions about the precision, even not that the precision is
 * non-null.
 */
public class StaticPrecisionAdjustment extends SimplePrecisionAdjustment {

  private StaticPrecisionAdjustment() {}

  @Override
  public Action prec(AbstractState pState, Precision pPrecision) throws CPAException {
    return Action.CONTINUE;
  }

  private static final PrecisionAdjustment instance = new StaticPrecisionAdjustment();

  public static PrecisionAdjustment getInstance() {
    return instance;
  }
}
