/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2019  Dirk Beyer
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
 */
package org.sosy_lab.cpachecker.cpa.location;

import org.sosy_lab.cpachecker.core.defaults.AnyCFAEdge;
import org.sosy_lab.cpachecker.core.defaults.EmptyEdge;
import org.sosy_lab.cpachecker.core.defaults.WrapperCFAEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ApplyOperator;

public class LocationApplyOperator implements ApplyOperator {

  @Override
  public AbstractState apply(AbstractState pState1, AbstractState pState2) {
    LocationStateWithEdge state1 = (LocationStateWithEdge) pState1;
    LocationStateWithEdge state2 = (LocationStateWithEdge) pState2;

    if (state2.getAbstractEdge() instanceof WrapperCFAEdge) {
      // Ordinary transition
      return null;
    } else if (state1.getAbstractEdge() == EmptyEdge.getInstance()
        || state2.getAbstractEdge() == EmptyEdge.getInstance()) {
      return null;
    } else {
      return state1.updateEdge(EmptyEdge.getInstance());
    }
  }

  @Override
  public AbstractState project(AbstractState pParent, AbstractState pChild) {
    LocationStateWithEdge state1 = (LocationStateWithEdge) pParent;
    return state1.updateEdge(AnyCFAEdge.getInstance());
  }

  @Override
  public AbstractState project(AbstractState pParent, AbstractState pChild, AbstractEdge pEdge) {
    LocationStateWithEdge state1 = (LocationStateWithEdge) pParent;

    assert pEdge == state1.getAbstractEdge();
    assert pEdge instanceof WrapperCFAEdge;

    // That is important to remove CFAEdge, to avoid considering it
    // Evil hack!
    return state1.updateEdge(AnyCFAEdge.getInstance());
  }

}
