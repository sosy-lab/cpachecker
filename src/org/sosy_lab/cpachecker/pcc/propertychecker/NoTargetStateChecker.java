// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.pcc.propertychecker;

import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Targetable;

/**
 * Implementation of a property checker which does not accept abstract states representing some kind
 * of "target" or "error" abstract state. Accepts every abstract state which is not a target
 * abstract state and every set of states which does not contain a target abstract state.
 */
public class NoTargetStateChecker extends PerElementPropertyChecker {

  @Override
  public boolean satisfiesProperty(AbstractState pElemToCheck)
      throws UnsupportedOperationException {
    return (!(pElemToCheck instanceof Targetable) || !((Targetable) pElemToCheck).isTarget());
  }
}
