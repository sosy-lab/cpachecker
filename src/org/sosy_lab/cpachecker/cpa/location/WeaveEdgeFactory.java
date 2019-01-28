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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cpa.location.LocationTransferRelation.WeavingType;
import org.sosy_lab.cpachecker.cpa.multigoal.MultiGoalState;
import org.sosy_lab.cpachecker.util.Pair;

public class WeaveEdgeFactory {
  private class WeavedVariable {
    public WeavedVariable(
        CVariableDeclaration pVarDecl,
        CExpressionAssignmentStatement pIncrement,
        CExpression pAssumption) {
      this.varDecl = pVarDecl;
      this.increment = pIncrement;
      this.assumption = pAssumption;
    }

    CVariableDeclaration varDecl;
    CExpressionAssignmentStatement increment;
    CExpression assumption;

    public CVariableDeclaration getVarDecl() {
      return varDecl;
    }

    public CExpressionAssignmentStatement getIncrement() {
      return increment;
    }

    public CExpression getAssumption() {
      return assumption;
    }

  }
  private class WeavingCacheKey {
    CFANode initialCFANode;
    CFANode finalCFANode;
    ImmutableSet<Pair<CFAEdge, WeavingType>> weavingEdges;

    public WeavingCacheKey(
        CFANode pInitialCFANode,
        CFANode pFinalCFANode,
        ImmutableSet<Pair<CFAEdge, WeavingType>> pWeavingEdges) {
      initialCFANode = pInitialCFANode;
      finalCFANode = pFinalCFANode;
      weavingEdges = pWeavingEdges;
    }

    @Override
    public int hashCode() {
      int hashCode = weavingEdges.hashCode();
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
      Iterator<Pair<CFAEdge, WeavingType>> iter1 = weavingEdges.iterator();
      Iterator<Pair<CFAEdge, WeavingType>> iter2 = wck.weavingEdges.iterator();
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

  private Map<CFAEdge, WeavedVariable> edgeToVar;
  private Map<WeavingCacheKey, List<CFAEdge>> weavingCache;
  private Map<CFANode, LocationState> locations;

  public WeaveEdgeFactory() {
    weavingCache = new HashMap<>();
    edgeToVar = new HashMap<>();
    locations = new HashMap<>();
  }

  private String WeavingEdgeToVarName(CFAEdge edge) {
    return "Edge_" + edge.getDescription() + "_" + Integer.toString(edge.hashCode());
  }

  private CFAEdge
      createWeaveEdge(Pair<CFAEdge, WeavingType> pair, CFANode successor, CFANode predecessor) {
    CFAEdge edge = pair.getFirst();
    if (!edgeToVar.containsKey(edge)) {
      createVariableForEdge(edge);
    }

    WeavedVariable var = edgeToVar.get(edge);
    CFAEdge weaveEdge = null;
    if (pair.getSecond() == WeavingType.DECLARATION) {
      weaveEdge =
          new CDeclarationEdge(
              var.getVarDecl().toASTString(),
              FileLocation.DUMMY,
              predecessor,
              successor,
              var.getVarDecl());
    } else if (pair.getSecond() == WeavingType.ASSUMPTION) {
      weaveEdge =
          new CAssumeEdge(
              var.getAssumption().toASTString(),
              FileLocation.DUMMY,
              predecessor,
              successor,
              var.getAssumption(),
              true);

    } else if (pair.getSecond() == WeavingType.ASSIGNMENT) {
      weaveEdge =
          new CStatementEdge(
              var.getIncrement().toASTString(),
              var.getIncrement(),
              FileLocation.DUMMY,
              predecessor,
              successor);
    }
    return weaveEdge;
  }

  private void createVariableForEdge(CFAEdge pEdge) {
    String name = WeavingEdgeToVarName(pEdge);
    CVariableDeclaration varDecl =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            true,
            CStorageClass.AUTO,
            CNumericTypes.INT,
            name,
            name,
            name,
            new CInitializerExpression(FileLocation.DUMMY, CIntegerLiteralExpression.ZERO));

    CIdExpression variable =
        new CIdExpression(FileLocation.DUMMY, CNumericTypes.INT, name, varDecl);

    CExpressionAssignmentStatement increment =
        new CExpressionAssignmentStatement(
            FileLocation.DUMMY,
            variable,
            CIntegerLiteralExpression.ONE);

    CExpression assumption =
        new CBinaryExpression(
            FileLocation.DUMMY,
            CNumericTypes.INT,
            CNumericTypes.INT,
            variable,
            CIntegerLiteralExpression.ONE,
            CBinaryExpression.BinaryOperator.EQUALS);

    edgeToVar.put(pEdge, new WeavedVariable(varDecl, increment, assumption));

  }

  private LocationState getStateForNode(CFANode node) {
    if (!locations.containsKey(node)) {
      locations.put(node, new LocationState(node, false));
    }
    return locations.get(node);
  }

  public LocationState create(MultiGoalState mgState, CFAEdge pCfaEdge) {
    String functionName = pCfaEdge.getPredecessor().getFunctionName();

    WeavingCacheKey wck =
        new WeavingCacheKey(
            pCfaEdge.getPredecessor(),
            pCfaEdge.getSuccessor(),
            mgState.getEdgesToWeave());
    if (!weavingCache.containsKey(wck)) {
      Iterator<Pair<CFAEdge, WeavingType>> iter = mgState.getEdgesToWeave().iterator();
      List<CFAEdge> weavedEdges = new ArrayList<>();

      CFANode predecessor = new CFANode(functionName);
      CFAEdge initialEdge = new BlankEdge(
          "weaved: " + pCfaEdge.getRawStatement(),
          FileLocation.DUMMY,
          pCfaEdge.getPredecessor(),
          predecessor,
              "weaved: " + pCfaEdge.getDescription());
      predecessor.addEnteringEdge(initialEdge);
      weavedEdges.add(initialEdge);
      CFANode successor = null;
      while (iter.hasNext()) {

        Pair<CFAEdge, WeavingType> pair = iter.next();
        if (iter.hasNext()) {
          successor = new CFANode(functionName);
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

    Iterator<CFAEdge> iter = weavingCache.get(wck).iterator();
    // skip first edge, which is not weaved
    if (iter.hasNext()) {
      iter.next();
    }
    while (iter.hasNext()) {
      mgState.addWeavedEdge(iter.next());
    }

    return getStateForNode(weavingCache.get(wck).get(0).getSuccessor());
  }

}
