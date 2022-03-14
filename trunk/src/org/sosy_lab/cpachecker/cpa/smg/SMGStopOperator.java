// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg;

import java.util.Collection;
import org.sosy_lab.cpachecker.core.defaults.StopSepOperator;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class SMGStopOperator extends StopSepOperator {
  /** Creates a stop-sep operator based on the given partial order */
  public SMGStopOperator(AbstractDomain d) {
    super(d);
  }

  @Override
  public boolean stop(AbstractState el, Collection<AbstractState> reached, Precision precision)
      throws CPAException, InterruptedException {
    if (el instanceof SMGState && !((UnmodifiableSMGState) el).isBlockEnded()) {
      return false;
    }
    return super.stop(el, reached, precision);
  }
}
