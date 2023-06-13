// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.defaults;

import java.util.Collection;
import java.util.Collections;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

/**
 * This transfer relation always returns the state itself as its successor. I.e, the relation
 * contains for all abstract states x and edges e the tuples (x,e,x).
 */
public enum IdentityTransferRelation implements TransferRelation {
  INSTANCE;

  @Override
  public Collection<AbstractState> getAbstractSuccessors(
      AbstractState pState, Precision pPrecision) {
    return Collections.singleton(pState);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge) {
    return Collections.singleton(pState);
  }
}
