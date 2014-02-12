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
package org.sosy_lab.cpachecker.util.reachingdef;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.FunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;

import com.google.common.collect.ImmutableSet;


public class ReachingDefUtils {

  private static CFANode[] cfaNodes;

  public static CFANode[] getAllNodesFromCFA() {
    return cfaNodes;
  }

  public static Pair<Set<String>, Map<FunctionEntryNode, Set<String>>> getAllVariables(CFANode pMainNode) {
    CFAEdge out;
    Vector<String> globalVariables = new Vector<>();
    Vector<CFANode> nodes = new Vector<>();

    while (!(pMainNode instanceof FunctionEntryNode)) {
      out = pMainNode.getLeavingEdge(0);
      if (out instanceof CDeclarationEdge
          && ((CDeclarationEdge) out).getDeclaration() instanceof CVariableDeclaration) {
        globalVariables.add(((CVariableDeclaration) ((CDeclarationEdge) out).getDeclaration()).getName());
      }
      nodes.add(pMainNode);
      pMainNode = pMainNode.getLeavingEdge(0).getSuccessor();
    }

    Map<FunctionEntryNode, Set<String>> result = new HashMap<>();

    HashSet<FunctionEntryNode> reachedFunctions = new HashSet<>();
    Stack<FunctionEntryNode> functionsToProcess = new Stack<>();

    Stack<CFANode> currentWaitlist = new Stack<>();
    HashSet<CFANode> seen = new HashSet<>();
    Vector<String> localVariables = new Vector<>();
    CFANode currentElement;
    FunctionEntryNode currentFunction;

    reachedFunctions.add((FunctionEntryNode) pMainNode);
    functionsToProcess.add((FunctionEntryNode) pMainNode);

    while (!functionsToProcess.isEmpty()) {
      currentFunction = functionsToProcess.pop();
      currentWaitlist.clear();
      currentWaitlist.add(currentFunction);
      seen.clear();
      seen.add(currentFunction);
      localVariables.clear();

      while (!currentWaitlist.isEmpty()) {
        currentElement = currentWaitlist.pop();
        nodes.add(currentElement);

        for (int i = 0; i < currentElement.getNumLeavingEdges(); i++) {
          out = currentElement.getLeavingEdge(i);

          if (out instanceof FunctionReturnEdge) {
            continue;
          }

          if (out instanceof FunctionCallEdge) {
            if (!reachedFunctions.contains(out.getSuccessor())) {
              functionsToProcess.add((FunctionEntryNode) out.getSuccessor());
              reachedFunctions.add((FunctionEntryNode) out.getSuccessor());
            }
            out = currentElement.getLeavingSummaryEdge();
          }

          if (out instanceof CDeclarationEdge
              && ((CDeclarationEdge) out).getDeclaration() instanceof CVariableDeclaration) {
            if (((CDeclarationEdge) out).getDeclaration().isGlobal()) {
              globalVariables.add(((CVariableDeclaration) ((CDeclarationEdge) out).getDeclaration()).getName());
            } else {
              // do not use qualified names because local and global parameters considered separately
              localVariables.add(((CVariableDeclaration) ((CDeclarationEdge) out).getDeclaration()).getName());
            }
          }

          if (!seen.contains(out.getSuccessor())) {
            currentWaitlist.add(out.getSuccessor());
            seen.add(out.getSuccessor());
          }
        }
      }

      result.put(currentFunction, ImmutableSet.copyOf(localVariables));
    }
    cfaNodes = new CFANode[nodes.size()];
    nodes.toArray(cfaNodes);
    return Pair.of((Set<String>) ImmutableSet.copyOf(globalVariables), result);
  }

  public static class VariableExtractor extends DefaultCExpressionVisitor<String, UnsupportedCCodeException> {

    private CFAEdge edgeForExpression;
    private String warning;

    public void resetWarning() {
      warning = null;
    }

    public String getWarning() {
      return warning;
    }

    public VariableExtractor(CFAEdge pEdgeForExpression) {
      edgeForExpression = pEdgeForExpression;
    }

    @Override
    protected String visitDefault(CExpression pExp) {
      return null;
    }

    @Override
    public String visit(CArraySubscriptExpression pIastArraySubscriptExpression) throws UnsupportedCCodeException {
      warning = "Analysis may be unsound in case of aliasing.";
      return pIastArraySubscriptExpression.getArrayExpression().accept(this);
    }

    @Override
    public String visit(CCastExpression pIastCastExpression) throws UnsupportedCCodeException {
      return pIastCastExpression.getOperand().accept(this);
    }

    @Override
    public String visit(CComplexCastExpression pIastCastExpression) throws UnsupportedCCodeException {
      return pIastCastExpression.getOperand().accept(this);
    }

    @Override
    public String visit(CFieldReference pIastFieldReference) throws UnsupportedCCodeException {
      if (pIastFieldReference.isPointerDereference()) {
        throw new UnsupportedCCodeException(
            "Does not support assignment to dereferenced variable due to missing aliasing support", edgeForExpression,
            pIastFieldReference);
      }
      warning = "Analysis may be unsound in case of aliasing.";
      return pIastFieldReference.getFieldOwner().accept(this);
    }

    @Override
    public String visit(CIdExpression pIastIdExpression) {
      return pIastIdExpression.getName();
    }

    @Override
    public String visit(CUnaryExpression pIastUnaryExpression) throws UnsupportedCCodeException {
      return pIastUnaryExpression.getOperand().accept(this);
    }

    @Override
    public String visit(CPointerExpression pIastUnaryExpression) throws UnsupportedCCodeException {
        throw new UnsupportedCCodeException(
            "Does not support assignment to dereferenced variable due to missing aliasing support", edgeForExpression,
            pIastUnaryExpression);
    }
  }

}
