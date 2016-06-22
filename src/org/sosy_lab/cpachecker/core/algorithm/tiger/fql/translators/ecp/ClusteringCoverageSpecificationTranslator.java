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
package org.sosy_lab.cpachecker.core.algorithm.tiger.fql.translators.ecp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.Edges;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.coveragespecification.CoverageSpecification;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.filter.Identity;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.pathpattern.Repetition;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.ECPConcatenation;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.SingletonECPEdgeSet;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.clustering.ClusteredElementaryCoveragePattern;

public class ClusteringCoverageSpecificationTranslator {

  private final PathPatternTranslator mPathPatternTranslator;
  private final CoverageSpecificationTranslator mCoverageSpecificationTranslator;
  private ArrayList<Collection<ElementaryCoveragePattern>> mSubgoalSequence;
  private int mNumberOfTestGoals;
  private final ElementaryCoveragePattern mIDStar;
  private final CFANode mInitialNode;

  private boolean mInsertReverted = false;

  public ClusteringCoverageSpecificationTranslator(PathPatternTranslator pPathPatternTranslator, LinkedList<Edges> pCoverageSequence, CFANode pInitialNode) {
    mPathPatternTranslator = pPathPatternTranslator;

    if (pCoverageSequence == null || pCoverageSequence.isEmpty()) {
      throw new IllegalArgumentException();
    }

    mCoverageSpecificationTranslator = new CoverageSpecificationTranslator(mPathPatternTranslator);

    mSubgoalSequence = new ArrayList<>(pCoverageSequence.size());

    mNumberOfTestGoals = 1;

    for (Edges lEdges : pCoverageSequence) {
      CoverageSpecification lSpecification = lEdges;
      Collection<ElementaryCoveragePattern> lSubgoals = mCoverageSpecificationTranslator.translate(lSpecification);
      mNumberOfTestGoals *= lSubgoals.size();
      mSubgoalSequence.add(lSubgoals);
    }

    Edges lID = new Edges(Identity.getInstance());
    Repetition lRepetition = new Repetition(lID);
    mIDStar = mCoverageSpecificationTranslator.translate(lRepetition);

    mInitialNode = pInitialNode;
  }

  public int getNumberOfTestGoals() {
    return mNumberOfTestGoals;
  }

  public ElementaryCoveragePattern[] createElementaryCoveragePatternsAndClusters() {
    ElementaryCoveragePattern[] lCoveragePatterns = new ElementaryCoveragePattern[getNumberOfTestGoals()];

    if (lCoveragePatterns.length == 0) {
      return lCoveragePatterns; // nothing has to be done
    }

    LinkedList<Iterator<ElementaryCoveragePattern>> lIteratorStack = new LinkedList<>();
    LinkedList<ElementaryCoveragePattern> lPatternStack = new LinkedList<>();

    int lCurrentStackDepth = 0;

    int lECPCounter = 0;

    int lClusterSize = mSubgoalSequence.get(mSubgoalSequence.size() - 1).size();

    while (lCurrentStackDepth >= 0) {
      if (lCurrentStackDepth == mSubgoalSequence.size() - 1) {
        ArrayList<ClusteredElementaryCoveragePattern> lCluster = new ArrayList<>(lClusterSize);

        CFANode lInitialNode;

        if (lPatternStack.isEmpty()) {
          lInitialNode = mInitialNode;
        }
        else {
          ElementaryCoveragePattern lPattern = lPatternStack.getLast();

          SingletonECPEdgeSet lEdgeSet = (SingletonECPEdgeSet)lPattern;

          lInitialNode = lEdgeSet.getCFAEdge().getSuccessor();
        }

        LinkedList<ElementaryCoveragePattern> lSequence = new LinkedList<>();

        lSequence.add(mIDStar);

        for (ElementaryCoveragePattern lSubgoal : lPatternStack) {
          lSequence.add(lSubgoal);
          lSequence.add(mIDStar);
        }

        for (ElementaryCoveragePattern lLastSubgoal : mSubgoalSequence.get(mSubgoalSequence.size() - 1)) {
          lSequence.add(lLastSubgoal);
          lSequence.add(mIDStar);

          SingletonECPEdgeSet lEdgeSet = (SingletonECPEdgeSet)lLastSubgoal;

          CFAEdge lLastSingletonCFAEdge = lEdgeSet.getCFAEdge();

          ECPConcatenation lGoal = new ECPConcatenation(lSequence);
          ClusteredElementaryCoveragePattern lClusteredGoal;

          if (mInsertReverted) {
            lClusteredGoal = new ClusteredElementaryCoveragePattern(lGoal, lCluster, lClusterSize - lCluster.size() - 1, lInitialNode, lLastSingletonCFAEdge);
          }
          else {
            lClusteredGoal = new ClusteredElementaryCoveragePattern(lGoal, lCluster, lCluster.size(), lInitialNode, lLastSingletonCFAEdge);
          }
          lCluster.add(lClusteredGoal);

          if (mInsertReverted) {
            lCoveragePatterns[lCoveragePatterns.length - lECPCounter - 1] = lClusteredGoal;
          }
          else {
            lCoveragePatterns[lECPCounter] = lClusteredGoal;
          }

          lECPCounter++;

          lSequence.removeLast();
          lSequence.removeLast();
        }

        if (mInsertReverted) {
          ClusteredElementaryCoveragePattern[] lClusterArray = new ClusteredElementaryCoveragePattern[lCluster.size()];

          lClusterArray = lCluster.toArray(lClusterArray);

          for (int i = 0; i < lClusterArray.length; i++) {
            lCluster.set(lClusterSize - i - 1, lClusterArray[i]);
          }
        }

        lCurrentStackDepth--;
      }
      else {
        Iterator<ElementaryCoveragePattern> lCurrentIterator;

        if (lCurrentStackDepth == lIteratorStack.size()) {
          lCurrentIterator = mSubgoalSequence.get(lCurrentStackDepth).iterator();
          lIteratorStack.add(lCurrentIterator);
        }
        else {
          lCurrentIterator = lIteratorStack.get(lCurrentStackDepth);
          lPatternStack.removeLast();
        }

        if (lCurrentIterator.hasNext()) {
          lPatternStack.add(lCurrentIterator.next());
          lCurrentStackDepth++;
        }
        else {
          lIteratorStack.removeLast();
          lCurrentStackDepth--;
        }
      }
    }

    if (lECPCounter != getNumberOfTestGoals()) {
      throw new RuntimeException();
    }

    return lCoveragePatterns;
  }

}
