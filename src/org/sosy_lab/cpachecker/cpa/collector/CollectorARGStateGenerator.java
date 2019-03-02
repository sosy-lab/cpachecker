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
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.collector;

import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSetWrapper;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class CollectorARGStateGenerator {
  private final LogManager logger;
  private final UnmodifiableReachedSet reachedSet;
  private Collection<ARGState> reachedcollectionARG = new ArrayList<>();
  public int currentARGStateID;
  private ImmutableList<AbstractState> theStorage;
  private AbstractState first;
  private int idFirst;
  private Object idParents;
  private ARGState simpleARGState;
  private Object idChildren;
  private AbstractState wrappedState;
  private AbstractState wrappedWrappedState;
  private Collection<ARGState> testmalparent;
  private ArrayList parents = new ArrayList<ARGState>();
  private Object firstparent;
  private Object secondparent;


  public CollectorARGStateGenerator(
      LogManager pLogger,
      UnmodifiableReachedSet pReachedSet) {

    logger = pLogger;
    reachedSet = pReachedSet;

    buildGraphData(reachedSet);
  }

  /** Build ARG data for all ARG states in the reached set. */
  private void buildGraphData(UnmodifiableReachedSet reached) {
    for (AbstractState entry : reached.asCollection()) {
      currentARGStateID = ((CollectorState) entry).getStateId();
      simpleARGState = ((CollectorState) entry).getARGState();
      reachedcollectionARG.add(simpleARGState);
      theStorage = ((CollectorState) entry).getStorage();
      if(theStorage != null){
        wrappedState = ((CollectorState) entry).getWrappedState();
        wrappedWrappedState = ((CollectorState) entry).getWrappedWrappedState();
        ARGState wrappedStateARG = (ARGState) wrappedState;
        testmalparent = wrappedStateARG.getParents();
        parents.addAll(testmalparent);
       if (parents.size()> 1) {
         firstparent = parents.get(0);
         ARGState f = (ARGState) firstparent;
         ARGState testmal2 = new ARGState(wrappedWrappedState, f);
         secondparent = parents.get(1);
         ARGState s = (ARGState) secondparent;
         ARGState testmal3 = new ARGState(wrappedWrappedState, s);
         reachedcollectionARG.add(wrappedStateARG);
         //reachedcollectionARG.add(f);
         //reachedcollectionARG.add(s);
         reachedcollectionARG.add(testmal2);
         reachedcollectionARG.add(testmal3);
       }
      }
    }
  }

  public Collection<ARGState> getCollection() { return reachedcollectionARG; }

}

