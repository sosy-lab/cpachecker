/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.fshell.clustering;

import java.util.List;
import java.util.ListIterator;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.util.ecp.ECPVisitor;
import org.sosy_lab.cpachecker.util.ecp.ElementaryCoveragePattern;

public class ClusteredElementaryCoveragePattern implements
    ElementaryCoveragePattern {

  private final ElementaryCoveragePattern mWrappedPattern;
  private final List<ClusteredElementaryCoveragePattern> mCluster;
  private final int mPositionInCluster;
  private final CFANode mCrucialNode;
  private final CFAEdge mLastSingletonCFAEdge;

  public ClusteredElementaryCoveragePattern(ElementaryCoveragePattern pWrappedPattern, List<ClusteredElementaryCoveragePattern> pCluster, int pPositionInCluster, CFANode pCrucialNode, CFAEdge pLastSingletonCFAEdge) {
    mWrappedPattern = pWrappedPattern;
    mCluster = pCluster;
    mPositionInCluster = pPositionInCluster;
    mCrucialNode = pCrucialNode;
    mLastSingletonCFAEdge = pLastSingletonCFAEdge;
  }

  public ElementaryCoveragePattern getWrappedPattern() {
    return mWrappedPattern;
  }

  public ListIterator<ClusteredElementaryCoveragePattern> getRemainingElementsInCluster() {
    System.out.println("mPositionInCluster = " + mPositionInCluster);
    System.out.println("mCluster.size() = " + mCluster.size());

    return mCluster.listIterator(mPositionInCluster + 1);
  }

  public int getNumberOfRemainingElementsInCluster() {
    return (mCluster.size() - 1 - mPositionInCluster);
  }

  public CFANode getCFANode() {
    return mCrucialNode;
  }

  public CFAEdge getLastSingletonCFAEdge() {
    return mLastSingletonCFAEdge;
  }

  @Override
  public <T> T accept(ECPVisitor<T> pVisitor) {
    return mWrappedPattern.accept(pVisitor);
  }

}
