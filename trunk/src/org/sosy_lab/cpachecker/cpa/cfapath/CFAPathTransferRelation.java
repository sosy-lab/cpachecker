// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.cfapath;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

public class CFAPathTransferRelation extends SingleEdgeTransferRelation {

  private static final Set<CFAPathTopState> topStateSingleton = CFAPathTopState.getSingleton();

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState pElement, Precision pPrecision, CFAEdge pCfaEdge) throws CPATransferException {
    if (pElement.equals(CFAPathTopState.getInstance())) {
      return topStateSingleton;
    }

    checkArgument((pElement instanceof CFAPathStandardState));

    CFAPathStandardState lCurrentElement = (CFAPathStandardState) pElement;

    CFAPathStandardState lSuccessor = new CFAPathStandardState(lCurrentElement, pCfaEdge);

    return Collections.singleton(lSuccessor);
  }
}
