// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.arg;

import org.sosy_lab.cpachecker.core.defaults.SimplePrecisionAdjustment;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.PrecisionAdjustmentResult.Action;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class ARGSimplePrecisionAdjustment extends SimplePrecisionAdjustment {

  private final SimplePrecisionAdjustment wrappedPrecAdjustment;

  public ARGSimplePrecisionAdjustment(SimplePrecisionAdjustment pWrappedPrecAdjustment) {
    wrappedPrecAdjustment = pWrappedPrecAdjustment;
  }

  @Override
  public Action prec(AbstractState pElement, Precision pPrecision) throws CPAException {
    ARGState element = (ARGState) pElement;

    return wrappedPrecAdjustment.prec(element.getWrappedState(), pPrecision);
  }
}
