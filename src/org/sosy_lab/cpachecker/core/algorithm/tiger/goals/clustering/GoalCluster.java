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
package org.sosy_lab.cpachecker.core.algorithm.tiger.goals.clustering;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.ECPConcatenation;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.SingletonECPEdgeSet;

public class GoalCluster {

  private Node mNode;
  private Map<ElementaryCoveragePattern, List<ElementaryCoveragePattern>> mClusterMap;
  private Map<ElementaryCoveragePattern, Integer> mIndexMap;

  public GoalCluster() {
    mClusterMap = new HashMap<>();
    mIndexMap = new HashMap<>();
    mNode = new Node();
  }

  public void add(ElementaryCoveragePattern pPattern) {
    mNode.add(pPattern);
  }

  public List<ElementaryCoveragePattern> getCluster(ElementaryCoveragePattern pPattern) {
    return mClusterMap.get(pPattern);
  }

  public int getPositionInCluster(ElementaryCoveragePattern pPattern) {
    return mIndexMap.get(pPattern);
  }

  public Iterator<ElementaryCoveragePattern> getRemainingCluster(ElementaryCoveragePattern pPattern) {
    Iterator<ElementaryCoveragePattern> lIterator = mClusterMap.get(pPattern).iterator();

    int lIndex = mIndexMap.get(pPattern);

    for (int i = 0; i < lIndex; i++) {
      lIterator.next();
    }

    return lIterator;
  }

  private class Node {

    private List<ElementaryCoveragePattern> mNoDominatorCluster;
    private Map<SingletonECPEdgeSet, List<Node>> mClusters;

    private int mDominatorIndex;

    private ElementaryCoveragePattern mRepresentative;

    //private int mSize = 0;

    public Node() {
      this(0, null, null);
    }

    private Node(int pDominatorIndex, ElementaryCoveragePattern pRepresentative, List<SingletonECPEdgeSet> pDominators) {
      mNoDominatorCluster = new LinkedList<>();
      mClusters = new LinkedHashMap<>();
      mDominatorIndex = pDominatorIndex;
      mRepresentative = pRepresentative;

      if (mRepresentative != null) {
        insert(pRepresentative, pDominators);
      }
    }

    private boolean matches(ElementaryCoveragePattern pPattern) {
      if (mDominatorIndex == 0) {
        return true;
      }

      if (mRepresentative == null) {
        throw new RuntimeException();
      }

      // we know: mDominatorIndex > 0 and therefore mRepresentative must
      // be an ECPConcatenation
      if (mRepresentative.getClass().equals(pPattern.getClass())) {
        ECPConcatenation lRepresentative = (ECPConcatenation)mRepresentative;
        ECPConcatenation lPattern = (ECPConcatenation)pPattern;

        if (lRepresentative.size() != lPattern.size()) {
          return false;
        }

        int lNumberOfDominators = 0;

        for (int i = 0; i < lRepresentative.size(); i++) {
          ElementaryCoveragePattern lPattern1 = lRepresentative.get(i);
          ElementaryCoveragePattern lPattern2 = lPattern.get(i);

          if (!lPattern1.equals(lPattern2)) {
            return false;
          }

          if (lPattern1 instanceof SingletonECPEdgeSet) {
            lNumberOfDominators++;

            if (lNumberOfDominators >= mDominatorIndex) {
              return true;
            }
          }
        }
      }

      return false;
    }

    public void add(ElementaryCoveragePattern pPattern) {
      List<SingletonECPEdgeSet> lDominators = pPattern.accept(ECPDominatorVisitor.INSTANCE);

      insert(pPattern, lDominators);

      if (mRepresentative == null) {
        mRepresentative = pPattern;
      }
    }

    private void insert(ElementaryCoveragePattern pPattern, List<SingletonECPEdgeSet> pDominators) {
      //mSize++;
      // by construction we know that each pattern contained in this GoalCluster object
      // is equivalent up to pDominators.get(mDominatorIndex - 1);

      //if (mDominatorIndex >= pDominators.size()) {
      if (mDominatorIndex >= pDominators.size() - 1) {
        mClusterMap.put(pPattern, mNoDominatorCluster);
        mIndexMap.put(pPattern, mNoDominatorCluster.size());
        mNoDominatorCluster.add(pPattern);
      }
      else {
        SingletonECPEdgeSet lDominator = pDominators.get(mDominatorIndex);

        List<Node> lClusters = mClusters.get(lDominator);

        if (lClusters == null) {
          Node lCluster = new Node(mDominatorIndex + 1, pPattern, pDominators);

          lClusters = new LinkedList<>();
          lClusters.add(lCluster);

          mClusters.put(lDominator, lClusters);
        }
        else {
          boolean lHasCluster = false;

          for (Node lCluster : lClusters) {
            if (lCluster.matches(pPattern)) {
              lHasCluster = true;
              lCluster.insert(pPattern, pDominators);
            }
          }

          if (!lHasCluster) {
            Node lCluster = new Node(mDominatorIndex + 1, pPattern, pDominators);
            lClusters.add(lCluster);
          }
        }
      }
    }

    @Override
    public String toString() {
      String lResult = "[" + this.mNoDominatorCluster.size();

      for (List<Node> lCluster : mClusters.values()) {
        lResult += lCluster.toString();
      }

      lResult += "]";

      //String lResult = "SIZE: " + size();

      return lResult;
    }

    /*public ElementaryCoveragePattern get(int pIndex) {
      if (pIndex < mNoDominatorCluster.size()) {
        return mNoDominatorCluster.get(pIndex);
      }
      else {
        int lIndex = pIndex - mNoDominatorCluster.size();

        for (List<Node> lClusters : mClusters.values()) {
          for (Node lCluster : lClusters) {
            if (lIndex < lCluster.size()) {
              return lCluster.get(lIndex);
            }
          }
        }

        throw new NoSuchElementException();
      }
    }*/

    /*public int size() {
      return mSize;
    }*/

  }

}
