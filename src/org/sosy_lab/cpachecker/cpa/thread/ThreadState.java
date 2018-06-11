/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.thread;

import static com.google.common.collect.FluentIterable.from;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.c.CThreadOperationStatement.CThreadCreateStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CThreadOperationStatement.CThreadJoinStatement;
import org.sosy_lab.cpachecker.core.defaults.LatticeAbstractState;
import org.sosy_lab.cpachecker.cpa.thread.ThreadLabel.LabelStatus;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleNode;
import org.sosy_lab.cpachecker.cpa.usage.CompatibleState;
import org.sosy_lab.cpachecker.exceptions.HandleCodeException;
import org.sosy_lab.cpachecker.util.Pair;

public class ThreadState implements LatticeAbstractState<ThreadState>, CompatibleNode {

  public static class ThreadStateBuilder {
    private final List<ThreadLabel> tSet;
    private final List<ThreadLabel> rSet;

    private ThreadStateBuilder(ThreadState state) {
      tSet = new ArrayList<>(state.threadSet);
      rSet = new ArrayList<>(state.removedSet);
    }

    public void handleParentThread(CThreadCreateStatement tCall) throws HandleCodeException {
      createThread(tCall, LabelStatus.PARENT_THREAD);
    }

    public void handleChildThread(CThreadCreateStatement tCall) throws HandleCodeException {
      createThread(tCall, tCall.isSelfParallel() ? LabelStatus.SELF_PARALLEL_THREAD : LabelStatus.CREATED_THREAD);
    }

    private void createThread(CThreadCreateStatement tCall, LabelStatus pParentThread)
        throws HandleCodeException {
      final String pVarName = tCall.getVariableName();
      //Just to info
      final String pFunctionName = tCall.getFunctionCallExpression().getFunctionNameExpression().toASTString();

      if (from(tSet)
          .anyMatch(l -> l.getName().equals(pFunctionName) && l.getVarName().equals(pVarName))) {
        throw new HandleCodeException(
            "Can not create thread " + pFunctionName + ", it was already created");
      }

      ThreadLabel label = new ThreadLabel(pFunctionName, pVarName, pParentThread);
      if (!tSet.isEmpty() && tSet.get(tSet.size() - 1).isSelfParallel()) {
        //Can add only the same status
        label = label.toSelfParallelLabel();
      }
      tSet.add(label);
    }

    public ThreadState build() {
      return new ThreadState(tSet, rSet);
    }

    public boolean joinThread(CThreadJoinStatement jCall) {
      // If we found several labels for different functions
      // it means, that there are several thread created for one thread variable.
      // Not a good situation, but it is not forbidden, so join the last assigned thread
      Optional<ThreadLabel> result =
          from(tSet).filter(l -> l.getVarName().equals(jCall.getVariableName())).last();
      // Do not self-join
      if (result.isPresent() && !result.get().isCreatedThread()) {
        return tSet.remove(result.get());
      } else {
        return false;
      }
    }

    public int getThreadSize() {
      //Only for statistics
      return tSet.size();
    }
  }

  private final ImmutableList<ThreadLabel> threadSet;
  // The removedSet is useless now, but it will be used in future in more complicated cases
  // Do not remove it now
  private final ImmutableList<ThreadLabel> removedSet;

  private ThreadState(List<ThreadLabel> Tset, List<ThreadLabel> Rset) {
    threadSet = ImmutableList.copyOf(Tset);
    removedSet = ImmutableList.copyOf(Rset);
  }

  @Override
  public int hashCode() {
    return Objects.hash(removedSet, threadSet);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null ||
        getClass() != obj.getClass()) {
      return false;
    }
    ThreadState other = (ThreadState) obj;
    return Objects.equals(removedSet, other.removedSet)
        && Objects.equals(threadSet, other.threadSet);
  }

  @Override
  public int compareTo(CompatibleState pOther) {
    ThreadState other = (ThreadState) pOther;
    int result = 0;

    result = other.threadSet.size() - this.threadSet.size(); //decreasing queue

    if (result != 0) {
      return result;
    }

    //Sizes are equal
    for (Pair<ThreadLabel, ThreadLabel> pair : Pair.zipList(threadSet, other.threadSet)) {
      result = pair.getFirst().compareTo(pair.getSecond());
      if (result != 0) {
        return result;
      }
    }
    return 0;
  }

  @Override
  public boolean isCompatibleWith(CompatibleState state) {
    Preconditions.checkArgument(state instanceof ThreadState);
    ThreadState other = (ThreadState) state;
    for (ThreadLabel label : threadSet) {
      for (ThreadLabel otherLabel : other.threadSet) {
        if (label.isCompatibleWith(otherLabel)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public ThreadState prepareToStore() {
    return new ThreadState(this.threadSet, Collections.emptyList());
  }

  public ThreadStateBuilder getBuilder() {
    return new ThreadStateBuilder(this);
  }

  public static ThreadState emptyState() {
    return new ThreadState(Collections.emptyList(), Collections.emptyList());
  }

  @Override
  public String toString() {
    // Info method, in difficult cases may be wrong
    Optional<ThreadLabel> createdThread =
        from(threadSet).filter(ThreadLabel::isCreatedThread).last();

    if (createdThread.isPresent()) {
      return createdThread.get().getName();
    } else {
      return "";
    }
  }

  @Override
  public boolean cover(CompatibleNode pNode) {
    return ((ThreadState)pNode).isLessOrEqual(this);
  }

  @Override
  public ThreadState join(ThreadState pOther) {
    throw new UnsupportedOperationException("Join is not implemented for ThreadCPA");
  }

  @Override
  public boolean isLessOrEqual(ThreadState pOther) {
    return Objects.equals(removedSet, pOther.removedSet)
        && pOther.threadSet.containsAll(threadSet);
  }
}
