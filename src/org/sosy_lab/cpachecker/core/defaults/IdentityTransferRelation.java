/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.core.defaults;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.TransferRelation;

/**
 * This transfer relation always returns the state itself as its successor.
 * I.e, the relation contains for all abstract states x and edges e the tuples
 * (x,e,x).
 */
public enum IdentityTransferRelation implements TransferRelation {

  INSTANCE;

  @Override
  public Collection<AbstractState> getAbstractSuccessors(AbstractState pState, Precision pPrecision, CFAEdge pCfaEdge) {
    return Collections.singleton(pState);
  }

  @Override
  public Collection<AbstractState> strengthen(AbstractState pState,
      List<AbstractState> pOtherStates, CFAEdge pCfaEdge, Precision pPrecision) {

    return null;
  }
}
