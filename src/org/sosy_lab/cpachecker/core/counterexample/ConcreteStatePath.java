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
package org.sosy_lab.cpachecker.core.counterexample;

import java.util.Iterator;
import java.util.List;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.core.counterexample.ConcreteStatePath.ConcerteStateCFAEdgePair;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;


public class ConcreteStatePath implements Iterable<ConcerteStateCFAEdgePair> {

  private final List<ConcerteStateCFAEdgePair> list;

  public ConcreteStatePath(List<ConcerteStateCFAEdgePair> pList) {
    list = ImmutableList.copyOf(pList);
  }

  @Override
  public final Iterator<ConcerteStateCFAEdgePair> iterator() {
    return list.iterator();
  }

  public static ConcerteStateCFAEdgePair valueOf(ConcreteState pConcreteState, CFAEdge cfaEdge) {
    return new ConcerteStateCFAEdgePair(pConcreteState, cfaEdge);
  }

  public static final class ConcerteStateCFAEdgePair {

    private final ConcreteState concreteState;
    private final CFAEdge cfaEdge;

    public ConcerteStateCFAEdgePair(ConcreteState pConcreteState, CFAEdge pCfaEdge) {
      Preconditions.checkArgument(pCfaEdge.getEdgeType() != CFAEdgeType.MultiEdge);
      concreteState = pConcreteState;
      cfaEdge = pCfaEdge;
    }

    public ConcreteState getConcreteState() {
      return concreteState;
    }

    public CFAEdge getCfaEdge() {
      return cfaEdge;
    }
  }
}