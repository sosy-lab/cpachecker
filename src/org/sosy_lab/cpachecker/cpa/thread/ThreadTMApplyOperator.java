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
package org.sosy_lab.cpachecker.cpa.thread;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import java.util.Set;
import org.sosy_lab.cpachecker.core.interfaces.AbstractEdge;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.ApplyOperator;
import org.sosy_lab.cpachecker.cpa.thread.ThreadAbstractEdge.ThreadAction;

public class ThreadTMApplyOperator implements ApplyOperator {

  @Override
  public AbstractState apply(AbstractState pState1, AbstractState pState2) {
    ThreadTMState state1 = (ThreadTMState) pState1;
    ThreadTMStateWithEdge state2 = (ThreadTMStateWithEdge) pState2;
    if (state1.isCompatibleWith(state2)) {
      return state1.copyWithEdge(state2.getAbstractEdge());
    }
    return null;
  }

  @Override
  public AbstractState project(AbstractState pParent, AbstractState pChild) {
    ThreadTMState parent = (ThreadTMState) pParent;
    ThreadTMState child = (ThreadTMState) pChild;
    Set<String> parentThreads = parent.getThreadSet().keySet();
    Set<String> childThreads = child.getThreadSet().keySet();
    if (parentThreads.size() == childThreads.size()) {
      // To identify a projection
      return parent.copyWithEdge(null);
    } else {
      Set<String> created = Sets.difference(childThreads, parentThreads);
      Set<String> joined = Sets.difference(parentThreads, childThreads);
      assert (created.size() + joined.size() == 1);
      ThreadAbstractEdge edge;
      if (created.size() == 1) {
        edge = new ThreadAbstractEdge(ThreadAction.CREATE, Iterables.getOnlyElement(created));
      } else if (joined.size() == 1) {
        edge = new ThreadAbstractEdge(ThreadAction.JOIN, Iterables.getOnlyElement(joined));
      } else {
        throw new UnsupportedOperationException("Multiple actions are not supported yet");
      }
      return parent.copyWithEdge(edge);
    }
  }

  @Override
  public AbstractState project(AbstractState pParent, AbstractState pChild, AbstractEdge pEdge) {
    return project(pParent, pChild);
  }

  @Override
  public boolean isInvariantToEffects(AbstractState pState) {
    return false;
  }

  @Override
  public boolean canBeAnythingApplied(AbstractState pState) {
    ThreadState state = (ThreadState) pState;
    return state.getThreadSize() > 0;
  }

}
