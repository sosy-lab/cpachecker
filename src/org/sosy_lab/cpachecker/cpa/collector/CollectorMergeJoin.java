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
package org.sosy_lab.cpachecker.cpa.collector;

import static com.google.common.collect.FluentIterable.from;
import static java.util.logging.Level.WARNING;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.core.interfaces.AbstractDomain;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.exceptions.CPAException;

@Options
public class CollectorMergeJoin implements MergeOperator {

  private final MergeOperator wrappedMergeCol;
  private final LogManager logger;
  private ArrayList parents1 = new ArrayList<ARGState>();
  private ArrayList parents2 = new ArrayList<ARGState>();
  private ArrayList children1 = new ArrayList<ARGState>();
  private ArrayList children2 = new ArrayList<ARGState>();
  private ArrayList parentsM = new ArrayList<ARGState>();
  private myARGState myARG1;
  private myARGState myARG2;
  private boolean isMerged = false;
  private boolean toMerge;
  private myARGState myARGmerged;
  Collection<ARGState> childrenOfParent = new ArrayList<ARGState>();


  public CollectorMergeJoin(MergeOperator pWrappedMerge, AbstractDomain pWrappedDomain,
                            Configuration config, LogManager mjLogger)
      throws InvalidConfigurationException {

    wrappedMergeCol = pWrappedMerge;
    config.inject(this);
    logger = mjLogger;

  }

  @Override
  public AbstractState merge(AbstractState pElement1,
                             AbstractState pElement2, Precision pPrecision) throws CPAException, InterruptedException {

    parentsM.clear();
    AbstractState abs1 = pElement1;
    AbstractState abs2 = pElement2;
    ARGState wrappedState1 = (ARGState) ((CollectorState) abs1).getWrappedState();
    ARGState wrappedState2 = (ARGState) ((CollectorState) abs2).getWrappedState();

    //new ARGStates for Storage
    Collection<ARGState> wrappedParent1 = wrappedState1.getParents();
    Collection<ARGState> wrappedParent2 = wrappedState2.getParents();
    parents1.addAll(wrappedParent1);
    parents2.addAll(wrappedParent2);
    Collection<ARGState> wrappedChildren1 = wrappedState1.getChildren();
    Collection<ARGState> wrappedChildren2 = wrappedState2.getChildren();
    children1.addAll(wrappedChildren1);
    children2.addAll(wrappedChildren2);
    toMerge = true;

    if (parents2.size() >= 1 && parents1.size() >=1) {
      Object firstparent = parents2.get(0);
      Object firstparent1 = parents1.get(0);
      ARGState f = (ARGState) firstparent;
      ////do i need them???////////////////////////////////////
      childrenOfParent = ((ARGState) firstparent).getChildren();
      logger.log(Level.INFO, "children of parent:\n" + childrenOfParent);
      ///////////////////////////////////////////////////////////
      ARGState f1 = (ARGState) firstparent1;
      myARG1 = new myARGState(wrappedState1,f1,parents1, children1,toMerge,logger);
      parents1.clear();
      myARG2 = new myARGState(wrappedState2,f,parents2, children2,toMerge, logger);
      parents2.clear();
    } else {
      myARG2 = new myARGState(wrappedState2,null,null,children2,toMerge,logger);
      myARG1 = new myARGState(wrappedState1,null,null,children1,toMerge,logger);
    }

    /////////////if i do need children of parent of mergepartner thats where they are still alive but get destroyed after next step
    ARGState mergedElement = (ARGState) wrappedMergeCol.merge(wrappedState1,wrappedState2,
        pPrecision);

    isMerged = true;

    Collection<ARGState> wrappedParentMerged = mergedElement.getParents();
    parentsM.addAll(wrappedParentMerged);

    myARGmerged = new myARGState(mergedElement,null,parentsM,null,false,logger);

    AbstractState merged = mergedElement;

    CollectorState mergedElementcollector = new CollectorState
        (merged,null, null, isMerged, myARG1, myARG2,myARGmerged,logger);
    AbstractState mergedElementabs = mergedElementcollector;

    return mergedElementabs;
  }
}
