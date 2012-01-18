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
package org.sosy_lab.cpachecker.fshell.fql2.translators.ecp;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;

import org.sosy_lab.cpachecker.fshell.fql2.ast.Atom;
import org.sosy_lab.cpachecker.fshell.fql2.ast.Paths;
import org.sosy_lab.cpachecker.fshell.fql2.ast.coveragespecification.Concatenation;
import org.sosy_lab.cpachecker.fshell.fql2.ast.coveragespecification.CoverageSpecification;
import org.sosy_lab.cpachecker.fshell.fql2.ast.coveragespecification.Quotation;
import org.sosy_lab.cpachecker.fshell.fql2.ast.coveragespecification.Union;
import org.sosy_lab.cpachecker.fshell.targetgraph.Edge;
import org.sosy_lab.cpachecker.fshell.targetgraph.Node;
import org.sosy_lab.cpachecker.fshell.targetgraph.Occurrences;
import org.sosy_lab.cpachecker.fshell.targetgraph.Path;
import org.sosy_lab.cpachecker.fshell.targetgraph.TargetGraph;
import org.sosy_lab.cpachecker.util.ecp.ECPConcatenation;
import org.sosy_lab.cpachecker.util.ecp.ElementaryCoveragePattern;

public class IncrementalCoverageSpecificationTranslator {

  private final PathPatternTranslator mPathPatternTranslator;
  private final CoverageSpecificationTranslator mCoverageSpecificationTranslator;

  public IncrementalCoverageSpecificationTranslator(PathPatternTranslator pPathPatternTranslator) {
    mPathPatternTranslator = pPathPatternTranslator;
    mCoverageSpecificationTranslator = new CoverageSpecificationTranslator(mPathPatternTranslator);
  }

  public int getNumberOfTestGoals(CoverageSpecification pSpecification) {
    if (pSpecification instanceof Atom || pSpecification instanceof Quotation) {
      // TODO special treatement of PATHS-atom (in quotations?)
      return mCoverageSpecificationTranslator.translate(pSpecification).size();
    }
    else if (pSpecification instanceof Union) {
      Union lUnion = (Union)pSpecification;

      return getNumberOfTestGoals(lUnion.getFirstSubspecification()) + getNumberOfTestGoals(lUnion.getSecondSubspecification());
    }
    else if (pSpecification instanceof Concatenation) {
      Concatenation lConcatenation = (Concatenation)pSpecification;

      return getNumberOfTestGoals(lConcatenation.getFirstSubspecification()) * getNumberOfTestGoals(lConcatenation.getSecondSubspecification());
    }

    throw new RuntimeException();
  }

  public Iterator<ElementaryCoveragePattern> translate(CoverageSpecification pSpecification) {
    if (pSpecification instanceof Paths) {
      Paths lPaths = (Paths)pSpecification;

      TargetGraph lTargetGraph = mPathPatternTranslator.getFilterEvaluator().evaluate(lPaths.getFilter());

      return new PathIterator(lTargetGraph, lPaths.getBound());
    }
    if (pSpecification instanceof Atom || pSpecification instanceof Quotation) {
      return mCoverageSpecificationTranslator.translate(pSpecification).iterator();
    }
    else if (pSpecification instanceof Union) {
      Union lUnion = (Union)pSpecification;

      return new UnionIterator(lUnion.getFirstSubspecification(), lUnion.getSecondSubspecification());
    }
    else if (pSpecification instanceof Concatenation) {
      Concatenation lConcatenation = (Concatenation)pSpecification;

      return new ConcatenationIterator(lConcatenation.getFirstSubspecification(), lConcatenation.getSecondSubspecification());
    }

    throw new RuntimeException();
  }

  private class PathIterator implements Iterator<ElementaryCoveragePattern> {

    /*
     * A single path iterator enumerates all paths starting at the given
     * initial node.
     */
    private class SinglePathIterator implements Iterator<ElementaryCoveragePattern> {


      private LinkedList<Edge> mEdgeSequence;
      private LinkedList<Iterator<Edge>> mIteratorSequence;
      private LinkedList<ElementaryCoveragePattern> mPatternSequence;
      private ElementaryCoveragePattern mCurrentPattern;

      private final Node mInitialNode;

      private final Occurrences mOccurrences;

      public SinglePathIterator(Node pInitialNode) {
        mInitialNode = pInitialNode;
        mCurrentPattern = null;
        mEdgeSequence = new LinkedList<Edge>();
        mIteratorSequence = new LinkedList<Iterator<Edge>>();
        mPatternSequence = new LinkedList<ElementaryCoveragePattern>();

        if (mTargetGraph.isFinalNode(pInitialNode)) {
          mCurrentPattern = mPathPatternTranslator.translate(new Path(pInitialNode, mEdgeSequence));
        }

        // the iterator sequence is always one longer than the edge sequence
        mIteratorSequence.add(mTargetGraph.getOutgoingEdges(mInitialNode).iterator());

        mOccurrences = new Occurrences();
      }

      @Override
      public boolean hasNext() {
        if (mCurrentPattern == null) {
          // we have to determine the next path

          Iterator<Edge> lFrontierIterator = mIteratorSequence.getLast();

          if (lFrontierIterator.hasNext()) {
            Edge lNextEdge = lFrontierIterator.next();

            int lOccurrences = mOccurrences.increment(lNextEdge);

            if (lOccurrences > mBound) {
              mOccurrences.decrement(lNextEdge);

              Edge lLastEdge = mEdgeSequence.getLast();

              if (lLastEdge == null) {
                if (mTargetGraph.isFinalNode(mInitialNode)) {
                  // we initially generated this pattern, so
                  // we do not have to generate it once again

                  return false;
                }
              }
              else {
                if (mTargetGraph.isFinalNode(lLastEdge.getTarget())) {
                  // we have generated this pattern before as
                  // we entered lLastEdge (see below)

                  return hasNext();
                }
              }

              mCurrentPattern = new ECPConcatenation(mPatternSequence);

              return true;
            }

            Node lTarget = lNextEdge.getTarget();

            mEdgeSequence.add(lNextEdge);
            mPatternSequence.add(mPathPatternTranslator.translate(lNextEdge));
            mIteratorSequence.add(mTargetGraph.getOutgoingEdges(lTarget).iterator());

            if (mTargetGraph.isFinalNode(lTarget)) {
              mCurrentPattern = new ECPConcatenation(mPatternSequence);

              return true;
            }

            return hasNext();
          }
          else {
            // backtrack

            mIteratorSequence.removeLast();

            if (mIteratorSequence.isEmpty()) {
              if (!mEdgeSequence.isEmpty()) {
                throw new RuntimeException();
              }

              return false;
            }

            Edge lEdge = mEdgeSequence.removeLast();
            mPatternSequence.removeLast();

            mOccurrences.decrement(lEdge);

            return hasNext();
          }
        }

        return true;
      }

      @Override
      public ElementaryCoveragePattern next() {
        if (hasNext()) {
          ElementaryCoveragePattern lPattern = mCurrentPattern;
          mCurrentPattern = null;
          return lPattern;
        }

        throw new NoSuchElementException();
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException();
      }


    }

    private TargetGraph mTargetGraph;
    private int mBound;
    private Iterator<Node> mInitialNodes;
    private SinglePathIterator mCurrentSinglePathIterator;

    public PathIterator(TargetGraph pTargetGraph, int pBound) {
      mTargetGraph = pTargetGraph;
      mBound = pBound;

      mInitialNodes = mTargetGraph.initialNodes().iterator();

      /* for each initial node, we will generate a SinglePathIterator object */
      if (mInitialNodes.hasNext()) {
        Node lCurrentInitialNode = mInitialNodes.next();

        mCurrentSinglePathIterator = new SinglePathIterator(lCurrentInitialNode);
      }
      else {
        mCurrentSinglePathIterator = null;
      }
    }

    @Override
    public boolean hasNext() {
      if (mCurrentSinglePathIterator == null) {
        return false;
      }

      if (mCurrentSinglePathIterator.hasNext()) {
        return true;
      }

      if (mInitialNodes.hasNext()) {
        Node lCurrentInitialNode = mInitialNodes.next();
        mCurrentSinglePathIterator = new SinglePathIterator(lCurrentInitialNode);
      }
      else {
        mCurrentSinglePathIterator = null;
      }

      return hasNext();
    }

    @Override
    public ElementaryCoveragePattern next() {
      if (hasNext()) {
        return mCurrentSinglePathIterator.next();
      }

      throw new NoSuchElementException();
    }

    @Override
    public void remove() {
      throw new RuntimeException();
    }

  }

  private class UnionIterator implements Iterator<ElementaryCoveragePattern> {

    private final Iterator<ElementaryCoveragePattern> mIterator1;
    private final Iterator<ElementaryCoveragePattern> mIterator2;

    private UnionIterator(CoverageSpecification pSpecification1, CoverageSpecification pSpecification2) {
      mIterator1 = translate(pSpecification1);
      mIterator2 = translate(pSpecification2);
    }

    @Override
    public boolean hasNext() {
      return (mIterator1.hasNext() || mIterator2.hasNext());
    }

    @Override
    public ElementaryCoveragePattern next() {
      if (mIterator1.hasNext()) {
        return mIterator1.next();
      }

      return mIterator2.next();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

  }

  private class ConcatenationIterator implements Iterator<ElementaryCoveragePattern> {

    private final Iterator<ElementaryCoveragePattern> mIterator1;
    private final CoverageSpecification mSpecification2;
    private Iterator<ElementaryCoveragePattern> mIterator2;

    private ElementaryCoveragePattern mPrefix;

    private ConcatenationIterator(CoverageSpecification pSpecification1, CoverageSpecification pSpecification2) {
      mIterator1 = translate(pSpecification1);
      mIterator2 = translate(pSpecification2);
      mSpecification2 = pSpecification2;

      mPrefix = null;

      if (mIterator1.hasNext() && mIterator2.hasNext()) {
        mPrefix = mIterator1.next();
      }
    }

    @Override
    public boolean hasNext() {
      return (mPrefix != null);
    }

    @Override
    public ElementaryCoveragePattern next() {
      ECPConcatenation lResult = new ECPConcatenation(mPrefix, mIterator2.next());

      if (!mIterator2.hasNext()) {
        if (mIterator1.hasNext()) {
          mPrefix = mIterator1.next();
          mIterator2 = translate(mSpecification2);
        }
        else {
          mPrefix = null;
        }
      }

      return lResult;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }

  }

}
