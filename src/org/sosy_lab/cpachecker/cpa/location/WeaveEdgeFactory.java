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
 */
package org.sosy_lab.cpachecker.cpa.location;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.UnmodifiableIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.util.Pair;

public class WeaveEdgeFactory {

  private static class WeavingCacheKey {
    CFANode initialCFANode;
    CFANode finalCFANode;
    ImmutableSet<Pair<WeavingVariable, WeavingType>> weavingEdges;

    public WeavingCacheKey(
        CFANode pInitialCFANode,
        CFANode pFinalCFANode,
        ImmutableSet<Pair<WeavingVariable, WeavingType>> pWeavingEdges) {
      initialCFANode = pInitialCFANode;
      finalCFANode = pFinalCFANode;
      weavingEdges = pWeavingEdges;
    }

    @Override
    public int hashCode() {
      int hashCode = 0;
      UnmodifiableIterator<Pair<WeavingVariable, WeavingType>> i = weavingEdges.iterator();
      while (i.hasNext()) {
        Pair<WeavingVariable, WeavingType> obj = i.next();
        if (obj != null) {
          hashCode += obj.hashCode();
        }
      }
      hashCode = 37 * hashCode + initialCFANode.hashCode();
      hashCode = 37 * hashCode + finalCFANode.hashCode();
      // TODO Auto-generated method stub
      return hashCode;
    }

    @Override
    public boolean equals(Object pObj) {
      if (!(pObj instanceof WeavingCacheKey)) {
        return false;
      }
      WeavingCacheKey wck = (WeavingCacheKey) pObj;
      if (wck.initialCFANode != initialCFANode) {
        return false;
      }
      if (wck.finalCFANode != finalCFANode) {
        return false;
      }
      if (wck.weavingEdges.size() != weavingEdges.size()) {
        return false;
      }
      Iterator<Pair<WeavingVariable, WeavingType>> iter1 = weavingEdges.iterator();
      Iterator<Pair<WeavingVariable, WeavingType>> iter2 = wck.weavingEdges.iterator();
      while (iter1.hasNext()) {
        if (!iter2.hasNext()) {
          return false;
        }
        if (!iter1.next().equals(iter2.next())) {
          return false;
        }
      }

      return true;
    }

  }

  private Map<WeavingCacheKey, List<CFAEdge>> weavingCache;
  private Map<CFANode, LocationState> locations;
  Map<CFAEdge, CFAEdge> weavedEdgesToOriginalEdgesMap;
  static WeaveEdgeFactory singleton;

  private WeaveEdgeFactory() {
    weavingCache = new HashMap<>();
    locations = new HashMap<>();
    weavedEdgesToOriginalEdgesMap = new HashMap<>();
  }

  public static WeaveEdgeFactory getSingleton() {
    if (singleton == null) {
      singleton = new WeaveEdgeFactory();
    }
    return singleton;
  }



  private CFAEdge
      createWeaveEdge(
          Pair<WeavingVariable, WeavingType> pair,
          CFANode successor,
          CFANode predecessor) {
    WeavingVariable var = pair.getFirst();
    CFAEdge weaveEdge = null;
    if (pair.getSecond() == WeavingType.DECLARATION) {
      weaveEdge =
          new CDeclarationEdge(
              "weaved_" + var.getVarDecl().toASTString(),
              FileLocation.DUMMY,
              predecessor,
              successor,
              var.getVarDecl());
    } else if (pair.getSecond() == WeavingType.ASSUMPTION) {
      weaveEdge =
          new CAssumeEdge(
              "weaved_" + var.getAssumption().toASTString(),
              FileLocation.DUMMY,
              predecessor,
              successor,
              var.getAssumption(),
              true);

    } else if (pair.getSecond() == WeavingType.NEGATEDASSUMPTION) {
      weaveEdge =
          new CAssumeEdge(
              "weaved_" + var.getAssumption().toASTString(),
              FileLocation.DUMMY,
              predecessor,
              successor,
              var.getAssumption(),
              false);

    } else if (pair.getSecond() == WeavingType.ASSIGNMENT) {
      weaveEdge =
          new CStatementEdge(
              "weaved_" + var.getAssignment().toASTString(),
              var.getAssignment(),
              FileLocation.DUMMY,
              predecessor,
              successor);
    }

    return weaveEdge;
  }



  private LocationState getStateForNode(CFANode node) {
    if (!locations.containsKey(node)) {
      locations.put(node, new LocationState(node, false));
    }
    return locations.get(node);
  }

  private CFAEdge copy(CFAEdge edge, CFANode successor) {

    if (edge.getEdgeType() == CFAEdgeType.FunctionReturnEdge) {
      return new CFunctionReturnEdge(
          edge.getFileLocation(),
          ((CFunctionReturnEdge) edge).getPredecessor(),
          successor,
          ((CFunctionReturnEdge) edge).getSummaryEdge());
    }

    if (edge.getEdgeType() == CFAEdgeType.AssumeEdge) {
      return new CAssumeEdge(
          "weaved: " + edge.getRawStatement(),
          edge.getFileLocation(),
          edge.getPredecessor(),
          successor,
          ((CAssumeEdge) edge).getExpression(),
          ((CAssumeEdge) edge).getTruthAssumption());
    }
    if (edge.getEdgeType() == CFAEdgeType.BlankEdge) {
      return new BlankEdge(
              "weaved: " + edge.getRawStatement(),
              edge.getFileLocation(),
          edge.getPredecessor(),
              successor,
              "weaved: " + edge.getDescription());
    }
    if (edge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
      CDeclarationEdge declEdge = (CDeclarationEdge) edge;
      return new CDeclarationEdge(
          declEdge.getRawStatement(),
          declEdge.getFileLocation(),
          edge.getPredecessor(),
          successor,
          declEdge.getDeclaration());
    }

    if (edge.getEdgeType() == CFAEdgeType.StatementEdge) {
      return new CStatementEdge(
          edge.getRawStatement(),
          ((CStatementEdge) edge).getStatement(),
          edge.getFileLocation(),
          edge.getPredecessor(),
          successor);
    }
    throw new RuntimeException("Not handled Edge for weaving " + edge.toString());
  }

  public Map<CFAEdge, CFAEdge> getWeavedEdgesToOriginalEdgesMap() {
    return weavedEdgesToOriginalEdgesMap;
  }

  private void
      create(
          WeavingState wState,
          CFAEdge pCfaEdge,
          WeavingCacheKey wck) {
    Iterator<Pair<WeavingVariable, WeavingType>> iter = wState.getEdgesToWeave().iterator();
    // TODO important to weave NEGATEDASSUMPTION last!
    List<CFAEdge> weavedEdges = new ArrayList<>();
    AFunctionDeclaration function = pCfaEdge.getPredecessor().getFunction();
    CFANode predecessor = new CFANode(function);
    int reversePostOrderId = pCfaEdge.getPredecessor().getReversePostorderId();
    predecessor.setReversePostorderId(reversePostOrderId);
    CFAEdge initialEdge = copy(pCfaEdge, predecessor);
    weavedEdgesToOriginalEdgesMap.put(initialEdge, pCfaEdge);

    predecessor.addEnteringEdge(initialEdge);
    weavedEdges.add(initialEdge);
    CFANode successor = null;
    while (iter.hasNext()) {

      Pair<WeavingVariable, WeavingType> pair = iter.next();
      if (iter.hasNext()) {
        successor = new CFANode(function);
        successor.setReversePostorderId(reversePostOrderId);
      } else {
        successor = pCfaEdge.getSuccessor();
      }
      CFAEdge weaveEdge = createWeaveEdge(pair, successor, predecessor);
      weavedEdges.add(weaveEdge);

      predecessor.addLeavingEdge(weaveEdge);
      // only add weaved edge to nodes that have been newly created
      // therefore we won't modify the original cfa
      if (iter.hasNext()) {
        successor.addEnteringEdge(weaveEdge);
    }
      predecessor = successor;
  }
    weavingCache.put(wck, weavedEdges);
  }

  public LocationState create(WeavingState wState, CFAEdge pCfaEdge) {


    WeavingCacheKey wck =
        new WeavingCacheKey(
            pCfaEdge.getPredecessor(),
            pCfaEdge.getSuccessor(),
            wState.getEdgesToWeave());
    if (!weavingCache.containsKey(wck)) {
      // if (mgState.getEdgesToWeave().asList().get(0).getSecond() == WeavingType.NEGATEDASSUMPTION)
      // {
      // createNegatedEdges(mgState, pCfaEdge, functionName, wck);
      // } else {
      create(wState, pCfaEdge, wck);
      // }
    }

    Iterator<CFAEdge> iter = weavingCache.get(wck).iterator();
    // skip first edge, which is not weaved
    if (iter.hasNext()) {
      iter.next();
    }
    while (iter.hasNext()) {
      wState.addWeavedEdge(iter.next());
    }

    return getStateForNode(weavingCache.get(wck).get(0).getSuccessor());
  }

}
