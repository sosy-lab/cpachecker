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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.fshell.fql2.ast.Edges;
import org.sosy_lab.cpachecker.fshell.fql2.ast.Nodes;
import org.sosy_lab.cpachecker.fshell.fql2.ast.Paths;
import org.sosy_lab.cpachecker.fshell.fql2.ast.Predicate;
import org.sosy_lab.cpachecker.fshell.fql2.ast.pathpattern.Concatenation;
import org.sosy_lab.cpachecker.fshell.fql2.ast.pathpattern.PathPattern;
import org.sosy_lab.cpachecker.fshell.fql2.ast.pathpattern.PathPatternVisitor;
import org.sosy_lab.cpachecker.fshell.fql2.ast.pathpattern.Repetition;
import org.sosy_lab.cpachecker.fshell.fql2.ast.pathpattern.Union;
import org.sosy_lab.cpachecker.fshell.targetgraph.Edge;
import org.sosy_lab.cpachecker.fshell.targetgraph.FilterEvaluator;
import org.sosy_lab.cpachecker.fshell.targetgraph.Node;
import org.sosy_lab.cpachecker.fshell.targetgraph.Path;
import org.sosy_lab.cpachecker.fshell.targetgraph.TargetGraph;
import org.sosy_lab.cpachecker.fshell.targetgraph.TargetGraphUtil;
import org.sosy_lab.cpachecker.util.ecp.ECPConcatenation;
import org.sosy_lab.cpachecker.util.ecp.ECPEdgeSet;
import org.sosy_lab.cpachecker.util.ecp.ECPNodeSet;
import org.sosy_lab.cpachecker.util.ecp.ECPPredicate;
import org.sosy_lab.cpachecker.util.ecp.ECPRepetition;
import org.sosy_lab.cpachecker.util.ecp.ECPUnion;
import org.sosy_lab.cpachecker.util.ecp.ElementaryCoveragePattern;

public class PathPatternTranslator {

  private final TargetGraph mTargetGraph;
  private final Visitor mVisitor;
  private final FilterEvaluator mFilterEvaluator;

  public PathPatternTranslator(CFANode pInitialNode) {
    this(TargetGraphUtil.cfa(pInitialNode), TargetGraphUtil.getBasicBlockEntries(pInitialNode));
  }

  public PathPatternTranslator(TargetGraph pTargetGraph, Set<CFAEdge> pBasicBlockEntries) {
    mTargetGraph = pTargetGraph;
    mVisitor = new Visitor();
    mFilterEvaluator = new FilterEvaluator(mTargetGraph, pBasicBlockEntries);
  }

  public int getCacheHits() {
    return mVisitor.mCacheHits;
  }

  public int getCacheMisses() {
    return mVisitor.mCacheMisses;
  }

  public FilterEvaluator getFilterEvaluator() {
    return mFilterEvaluator;
  }

  public ElementaryCoveragePattern translate(PathPattern pPattern) {
    return pPattern.accept(mVisitor);
  }

  public ElementaryCoveragePattern translate(Node pNode) {
    if (!pNode.getPredicates().isEmpty()) {
      return new ECPConcatenation(new ECPNodeSet(pNode.getCFANode()), translate(pNode.getPredicates()));
    }
    else {
      return new ECPNodeSet(pNode.getCFANode());
    }
  }

  public ElementaryCoveragePattern translate(Edge pEdge) {
    Node lSource = pEdge.getSource();
    Node lTarget = pEdge.getTarget();

    List<Predicate> lSourcePredicates = lSource.getPredicates();
    List<Predicate> lTargetPredicates = lTarget.getPredicates();

    ElementaryCoveragePattern lPattern = new ECPEdgeSet(pEdge.getCFAEdge());

    if (!lSourcePredicates.isEmpty()) {
      lPattern = new ECPConcatenation(translate(lSourcePredicates), lPattern);
    }

    if (!lTargetPredicates.isEmpty()) {
      lPattern = new ECPConcatenation(lPattern, translate(lTargetPredicates));
    }

    return lPattern;
  }

  public ElementaryCoveragePattern translate(Path pPath) {
    if (pPath.length() == 0) {
      return translate(pPath.getStartNode());
    }
    else {
      ElementaryCoveragePattern lPathPattern = null;

      for (Edge lEdge : pPath) {
        Node lSource = lEdge.getSource();
        List<Predicate> lSourcePredicates = lSource.getPredicates();

        ElementaryCoveragePattern lPattern = new ECPEdgeSet(lEdge.getCFAEdge());

        if (!lSource.getPredicates().isEmpty()) {
          lPattern = new ECPConcatenation(translate(lSourcePredicates), lPattern);
        }

        if (lPathPattern == null) {
          lPathPattern = lPattern;
        }
        else {
          lPathPattern = new ECPConcatenation(lPathPattern, lPattern);
        }
      }

      Node lEndNode = pPath.getEndNode();
      List<Predicate> lEndPredicates = lEndNode.getPredicates();

      if (!lEndPredicates.isEmpty()) {
        lPathPattern = new ECPConcatenation(lPathPattern, translate(lEndPredicates));
      }

      return lPathPattern;
    }

  }

  public ElementaryCoveragePattern translate(List<Predicate> pPredicates) {
    if (pPredicates.size() == 0) {
      throw new IllegalArgumentException();
    }

    ElementaryCoveragePattern lPredicatePattern = new ECPPredicate(pPredicates.get(0).getPredicate());

    for (int lIndex = 1; lIndex < pPredicates.size(); lIndex++) {
      lPredicatePattern = new ECPConcatenation(new ECPPredicate(pPredicates.get(lIndex).getPredicate()), lPredicatePattern);
    }

    return lPredicatePattern;
  }

  private class Visitor implements PathPatternVisitor<ElementaryCoveragePattern> {

    private final HashMap<PathPattern, ElementaryCoveragePattern> mResultCache = new HashMap<PathPattern, ElementaryCoveragePattern>();

    public int mCacheHits = 0;
    public int mCacheMisses = 0;

    @Override
    public ElementaryCoveragePattern visit(Concatenation pConcatenation) {
      ElementaryCoveragePattern lPattern = mResultCache.get(pConcatenation);

      if (lPattern == null) {
        ElementaryCoveragePattern lFirstSubpattern = pConcatenation.getFirstSubpattern().accept(this);
        ElementaryCoveragePattern lSecondSubpattern = pConcatenation.getSecondSubpattern().accept(this);

        lPattern = new ECPConcatenation(lFirstSubpattern, lSecondSubpattern);

        mResultCache.put(pConcatenation, lPattern);
        mCacheMisses++;
      }
      else {
        mCacheHits++;
      }

      return lPattern;
    }

    @Override
    public ElementaryCoveragePattern visit(Repetition pRepetition) {
      ElementaryCoveragePattern lPattern = mResultCache.get(pRepetition);

      if (lPattern == null) {
        ElementaryCoveragePattern lSubpattern = pRepetition.getSubpattern().accept(this);

        lPattern = new ECPRepetition(lSubpattern);

        mResultCache.put(pRepetition, lPattern);
        mCacheMisses++;
      }
      else {
        mCacheHits++;
      }

      return lPattern;
    }

    @Override
    public ElementaryCoveragePattern visit(Union pUnion) {
      ElementaryCoveragePattern lPattern = mResultCache.get(pUnion);

      if (lPattern == null) {
        ElementaryCoveragePattern lFirstSubpattern = pUnion.getFirstSubpattern().accept(this);
        ElementaryCoveragePattern lSecondSubpattern = pUnion.getSecondSubpattern().accept(this);

        lPattern = new ECPUnion(lFirstSubpattern, lSecondSubpattern);

        mResultCache.put(pUnion, lPattern);
        mCacheMisses++;
      }
      else {
        mCacheHits++;
      }

      return lPattern;
    }

    @Override
    public ElementaryCoveragePattern visit(Edges pEdges) {
      ElementaryCoveragePattern lPattern = mResultCache.get(pEdges);

      if (lPattern == null) {
        TargetGraph lFilteredTargetGraph = mFilterEvaluator.evaluate(pEdges.getFilter());

        Set<CFAEdge> lCFAEdges = new HashSet<CFAEdge>();

        Set<ElementaryCoveragePattern> lToBeUnited = new HashSet<ElementaryCoveragePattern>();

        for (Edge lEdge : lFilteredTargetGraph.getEdges()) {
          if (!lEdge.getSource().getPredicates().isEmpty() || !lEdge.getTarget().getPredicates().isEmpty()) {
            lToBeUnited.add(translate(lEdge));
          }
          else {
            lCFAEdges.add(lEdge.getCFAEdge());
          }
        }

        lPattern = new ECPEdgeSet(lCFAEdges);

        for (ElementaryCoveragePattern lSubpattern : lToBeUnited) {
          lPattern = new ECPUnion(lPattern, lSubpattern);
        }

        mResultCache.put(pEdges, lPattern);
        mCacheMisses++;
      }
      else {
        mCacheHits++;
      }

      return lPattern;
    }

    @Override
    public ElementaryCoveragePattern visit(Nodes pNodes) {
      ElementaryCoveragePattern lPattern = mResultCache.get(pNodes);

      if (lPattern == null) {
        TargetGraph lFilteredTargetGraph = mFilterEvaluator.evaluate(pNodes.getFilter());

        Set<CFANode> lCFANodes = new HashSet<CFANode>();

        Set<ElementaryCoveragePattern> lToBeUnited = new HashSet<ElementaryCoveragePattern>();

        for (Node lNode : lFilteredTargetGraph.getNodes()) {
          if (!lNode.getPredicates().isEmpty()) {
            lToBeUnited.add(translate(lNode));
          }
          else {
            lCFANodes.add(lNode.getCFANode());
          }
        }

        lPattern = new ECPNodeSet(lCFANodes);

        for (ElementaryCoveragePattern lSubpattern : lToBeUnited) {
          lPattern = new ECPUnion(lPattern, lSubpattern);
        }

        mResultCache.put(pNodes, lPattern);
        mCacheMisses++;
      }
      else {
        mCacheHits++;
      }

      return lPattern;
    }

    @Override
    public ElementaryCoveragePattern visit(Paths pPaths) {
      ElementaryCoveragePattern lPattern = mResultCache.get(pPaths);

      if (lPattern == null) {
        TargetGraph lFilteredTargetGraph = mFilterEvaluator.evaluate(pPaths.getFilter());

        Set<Path> lPaths = lFilteredTargetGraph.getBoundedPaths(pPaths.getBound());

        if (lPaths.size() == 0) {
          return ECPNodeSet.EMPTY_NODE_SET;
        }

        ArrayList<ElementaryCoveragePattern> lToBeUnited = new ArrayList<ElementaryCoveragePattern>(lPaths.size());

        for (Path lPath : lFilteredTargetGraph.getBoundedPaths(pPaths.getBound())) {
          lToBeUnited.add(translate(lPath));
        }

        lPattern = lToBeUnited.get(0);

        for (int lIndex = 1; lIndex < lToBeUnited.size(); lIndex++) {
          ElementaryCoveragePattern lSubpattern = lToBeUnited.get(lIndex);
          lPattern = new ECPUnion(lPattern, lSubpattern);
        }

        mResultCache.put(pPaths, lPattern);
        mCacheMisses++;
      }
      else {
        mCacheHits++;
      }

      return lPattern;
    }

    @Override
    public ElementaryCoveragePattern visit(Predicate pPredicate) {
      ElementaryCoveragePattern lPattern = mResultCache.get(pPredicate);

      if (lPattern == null) {
        lPattern = new ECPPredicate(pPredicate.getPredicate());

        mResultCache.put(pPredicate, lPattern);
        mCacheMisses++;
      }
      else {
        mCacheHits++;
      }

      return lPattern;
    }

  }

}
