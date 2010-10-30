/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2010  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.transferrelationmonitor;

import org.sosy_lab.cpachecker.core.interfaces.AbstractElement;
import org.sosy_lab.cpachecker.core.interfaces.ConfigurableProgramAnalysis;
import org.sosy_lab.cpachecker.core.interfaces.MergeOperator;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.exceptions.CPAException;

public class TransferRelationMonitorMerge implements MergeOperator{

  private ConfigurableProgramAnalysis wrappedCpa;

  public TransferRelationMonitorMerge(ConfigurableProgramAnalysis pWrappedCPA) {
    wrappedCpa = pWrappedCPA;
  }

  @Override
  public AbstractElement merge(
      AbstractElement pElement1,
      AbstractElement pElement2, Precision pPrecision)
  throws CPAException {
    TransferRelationMonitorElement transferRelationMonitorElement1= (TransferRelationMonitorElement)pElement1;
    TransferRelationMonitorElement transferRelationMonitorElement2 = (TransferRelationMonitorElement)pElement2;

    MergeOperator mergeOperator = wrappedCpa.getMergeOperator();
    AbstractElement wrappedElement1 = transferRelationMonitorElement1.getWrappedElement();
    AbstractElement wrappedElement2 = transferRelationMonitorElement2.getWrappedElement();
    AbstractElement retElement = mergeOperator.merge(wrappedElement1, wrappedElement2, pPrecision);
    if(retElement.equals(wrappedElement2)){
      return pElement2;
    }


    int pathLength = Math.max(transferRelationMonitorElement1.getNoOfNodesOnPath(),
                              transferRelationMonitorElement2.getNoOfNodesOnPath());
    int branchesOnPath = Math.max(transferRelationMonitorElement1.getNoOfBranchesOnPath(),
                                  transferRelationMonitorElement2.getNoOfBranchesOnPath());
    long totalTimeOnPath = Math.max(transferRelationMonitorElement1.getTotalTimeOnPath(),
                                    transferRelationMonitorElement2.getTotalTimeOnPath());
    
    TransferRelationMonitorElement mergedElement = new TransferRelationMonitorElement(
        retElement, pathLength, branchesOnPath, totalTimeOnPath);

    return mergedElement;
  }

}
