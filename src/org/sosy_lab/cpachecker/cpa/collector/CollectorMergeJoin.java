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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
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
  private final ArrayList<ARGState> parents1 = new ArrayList<>();
  private final ArrayList<ARGState> parents2 = new ArrayList<>();
  private final ArrayList<ARGState> children1 = new ArrayList<>();
  private final ArrayList<ARGState> children2 = new ArrayList<>();
  private final ArrayList<ARGState> parentsM = new ArrayList<>();
  private CollectorState mergedElementabs;


  public CollectorMergeJoin(
      MergeOperator pWrappedMerge, AbstractDomain pWrappedDomain,
      Configuration config, LogManager mjLogger)
      throws InvalidConfigurationException {

    wrappedMergeCol = pWrappedMerge;
    config.inject(this);
    logger = mjLogger;
  }

  @Override
  public AbstractState merge(
      AbstractState pElement1,
      AbstractState pElement2, Precision pPrecision) throws CPAException, InterruptedException {

    parentsM.clear();
    ARGState wrappedState1 = (ARGState) ((CollectorState) pElement1).getWrappedState();
    ARGState wrappedState2 = (ARGState) ((CollectorState) pElement2).getWrappedState();

    //new ARGStates for Storage
    Collection<ARGState> wrappedParent1 = Objects.requireNonNull(wrappedState1).getParents();
    Collection<ARGState> wrappedParent2 = Objects.requireNonNull(wrappedState2).getParents();
    parents1.addAll(wrappedParent1);
    parents2.addAll(wrappedParent2);
    Collection<ARGState> wrappedChildren1 = wrappedState1.getChildren();
    Collection<ARGState> wrappedChildren2 = wrappedState2.getChildren();
    children1.addAll(wrappedChildren1);
    children2.addAll(wrappedChildren2);

    ARGStateView myARG2;
    ARGStateView myARG1;
    if (parents2.size() >= 1 && parents1.size() >= 1) {
      ARGState firstparent = parents2.get(0);
      ARGState firstparent1 = parents1.get(0);

      myARG1 =
          new ARGStateView(wrappedState1, firstparent1, parents1, wrappedChildren1, true, logger);
      parents1.clear();
      myARG2 = new ARGStateView(wrappedState2, firstparent, parents2, wrappedChildren2, true, logger);
      parents2.clear();
    } else {
      myARG2 = new ARGStateView(wrappedState2, null, null, wrappedChildren2, true, logger);
      myARG1 = new ARGStateView(wrappedState1, null, null, wrappedChildren1, true, logger);
    }

    //children of parent of mergepartner are still alive but get destroyed after next step
    ARGState mergedElement = (ARGState) wrappedMergeCol.merge(wrappedState1, wrappedState2,
        pPrecision);

    Collection<ARGState> wrappedParentMerged = mergedElement.getParents();
    parentsM.addAll(wrappedParentMerged);


    ARGStateView myARGmerged;
    if (!mergedElement.equals(wrappedState2)) {
      myARGmerged = new ARGStateView(mergedElement, null, parentsM, null, false, logger);

      mergedElementabs = new CollectorState
          (mergedElement, null, null, true, myARG1, myARG2, myARGmerged, logger);
    }
    if (!mergedElement.equals(wrappedState1)) {
      myARGmerged = new ARGStateView(mergedElement, null, parentsM, null, false, logger);

      mergedElementabs = new CollectorState
          (mergedElement, null, null, true, myARG1, myARG2, myARGmerged, logger);
    }

    return mergedElementabs;
  }
}
