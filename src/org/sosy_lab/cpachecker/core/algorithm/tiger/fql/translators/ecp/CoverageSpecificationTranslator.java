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
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.Edges;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.Nodes;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.Paths;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.Predicate;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.coveragespecification.Concatenation;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.coveragespecification.CoverageSpecification;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.coveragespecification.CoverageSpecificationVisitor;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.coveragespecification.Quotation;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.coveragespecification.Union;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ast.pathpattern.PathPattern;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.ECPConcatenation;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.ECPPredicate;
import org.sosy_lab.cpachecker.core.algorithm.tiger.fql.ecp.ElementaryCoveragePattern;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.targetgraph.Edge;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.targetgraph.Node;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.targetgraph.Path;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.targetgraph.TargetGraph;
import org.sosy_lab.cpachecker.core.algorithm.tiger.goals.targetgraph.TargetGraphUtil;

public class CoverageSpecificationTranslator {

  private final Visitor mVisitor;
  public final PathPatternTranslator mPathPatternTranslator;
  private final HashMap<CoverageSpecification, Collection<ElementaryCoveragePattern>> mResultCache;

  public CoverageSpecificationTranslator(CFANode pInitialNode) {
    this(TargetGraphUtil.cfa(pInitialNode), TargetGraphUtil.getBasicBlockEntries(pInitialNode));
  }

  public CoverageSpecificationTranslator(TargetGraph pTargetGraph, Set<CFAEdge> pBasicBlockEntries) {
    this (new PathPatternTranslator(pTargetGraph, pBasicBlockEntries));
  }

  public CoverageSpecificationTranslator(PathPatternTranslator pPatternTranslator) {
    mVisitor = new Visitor();
    mPathPatternTranslator = pPatternTranslator;
    mResultCache = new HashMap<>();
  }

  public int getOverallCacheHits() {
    return mVisitor.mCacheHits + mPathPatternTranslator.getCacheHits();
  }

  public int getOverallCacheMisses() {
    return mVisitor.mCacheMisses + mPathPatternTranslator.getCacheMisses();
  }

  public Collection<ElementaryCoveragePattern> translate(CoverageSpecification pCoverageSpecification) {
    return pCoverageSpecification.accept(mVisitor);
  }

  public ElementaryCoveragePattern translate(PathPattern pPathPattern) {
    return mPathPatternTranslator.translate(pPathPattern);
  }

  private class Visitor implements CoverageSpecificationVisitor<Collection<ElementaryCoveragePattern>> {

    public int mCacheHits = 0;
    public int mCacheMisses = 0;

    @Override
    public Collection<ElementaryCoveragePattern> visit(Concatenation pConcatenation) {
      Collection<ElementaryCoveragePattern> lResultSet = mResultCache.get(pConcatenation);

      if (lResultSet == null) {
        Collection<ElementaryCoveragePattern> lPrefixSet = pConcatenation.getFirstSubspecification().accept(this);
        Collection<ElementaryCoveragePattern> lSuffixSet = pConcatenation.getSecondSubspecification().accept(this);

        lResultSet = new ArrayList<>(lPrefixSet.size() * lSuffixSet.size());

        for (ElementaryCoveragePattern lPrefix : lPrefixSet) {
          for (ElementaryCoveragePattern lSuffix : lSuffixSet) {
            lResultSet.add(new ECPConcatenation(lPrefix, lSuffix));
          }
        }

        mResultCache.put(pConcatenation, lResultSet);
        mCacheMisses++;
      }
      else {
        mCacheHits++;
      }

      return lResultSet;
    }

    @Override
    public Collection<ElementaryCoveragePattern> visit(Quotation pQuotation) {
      Collection<ElementaryCoveragePattern> lResultSet = mResultCache.get(pQuotation);

      if (lResultSet == null) {
        ElementaryCoveragePattern lPattern = mPathPatternTranslator.translate(pQuotation.getPathPattern());

        lResultSet = Collections.singleton(lPattern);

        mResultCache.put(pQuotation, lResultSet);
        mCacheMisses++;
      }
      else {
        mCacheHits++;
      }

      return lResultSet;
    }

    @Override
    public Collection<ElementaryCoveragePattern> visit(Union pUnion) {
      Collection<ElementaryCoveragePattern> lResultSet = mResultCache.get(pUnion);

      if (lResultSet == null) {
        Collection<ElementaryCoveragePattern> lPatterns1 = pUnion.getFirstSubspecification().accept(this);
        Collection<ElementaryCoveragePattern> lPatterns2 = pUnion.getSecondSubspecification().accept(this);

        lResultSet = new ArrayList<>(lPatterns1.size() + lPatterns2.size());

        lResultSet.addAll(lPatterns1);
        lResultSet.addAll(lPatterns2);

        mResultCache.put(pUnion, lResultSet);
        mCacheMisses++;
      }
      else {
        mCacheHits++;
      }

      return lResultSet;
    }

    @Override
    public Collection<ElementaryCoveragePattern> visit(Edges pEdges) {
      TargetGraph lFilteredTargetGraph = mPathPatternTranslator.getFilterEvaluator().evaluate(pEdges.getFilter());

      Collection<ElementaryCoveragePattern> lResultSet = mResultCache.get(pEdges);

      if (lResultSet == null) {
        lResultSet = new ArrayList<>(lFilteredTargetGraph.getEdges().size());

        for (Edge lEdge : lFilteredTargetGraph.getEdges()) {
          ElementaryCoveragePattern lPattern = mPathPatternTranslator.translate(lEdge);
          lResultSet.add(lPattern);
        }

        mResultCache.put(pEdges, lResultSet);
        mCacheMisses++;
      }
      else {
        mCacheHits++;
      }

      return lResultSet;
    }

    @Override
    public Collection<ElementaryCoveragePattern> visit(Nodes pNodes) {
      TargetGraph lFilteredTargetGraph = mPathPatternTranslator.getFilterEvaluator().evaluate(pNodes.getFilter());

      Collection<ElementaryCoveragePattern> lResultSet = mResultCache.get(pNodes);

      if (lResultSet == null) {
        lResultSet = new ArrayList<>(lFilteredTargetGraph.getNodes().size());

        for (Node lNode : lFilteredTargetGraph.getNodes()) {
          lResultSet.add(mPathPatternTranslator.translate(lNode));
        }

        mResultCache.put(pNodes, lResultSet);
        mCacheMisses++;
      }
      else {
        mCacheHits++;
      }

      return lResultSet;
    }

    @Override
    public Collection<ElementaryCoveragePattern> visit(Paths pPaths) {
      TargetGraph lFilteredTargetGraph = mPathPatternTranslator.getFilterEvaluator().evaluate(pPaths.getFilter());

      Collection<ElementaryCoveragePattern> lResultSet = mResultCache.get(pPaths);

      if (lResultSet == null) {
        Collection<Path> lPaths = lFilteredTargetGraph.getBoundedPaths(pPaths.getBound());

        lResultSet = new ArrayList<>(lPaths.size());

        for (Path lPath : lPaths) {
          lResultSet.add(mPathPatternTranslator.translate(lPath));
        }

        mResultCache.put(pPaths, lResultSet);
        mCacheMisses++;
      }
      else {
        mCacheHits++;
      }

      return lResultSet;
    }

    @Override
    public Collection<ElementaryCoveragePattern> visit(Predicate pPredicate) {
      Collection<ElementaryCoveragePattern> lResultSet = mResultCache.get(pPredicate);

      if (lResultSet == null) {
        ElementaryCoveragePattern lPattern = new ECPPredicate(pPredicate.getPredicate());
        lResultSet = Collections.singleton(lPattern);
        mResultCache.put(pPredicate, lResultSet);
        mCacheMisses++;
      }
      else {
        mCacheHits++;
      }

      return lResultSet;
    }

  }

}
