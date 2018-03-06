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

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
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
import org.sosy_lab.cpachecker.cpa.pointer2.PointerState;
import org.sosy_lab.cpachecker.cpa.pointer2.util.ExplicitLocationSet;
import org.sosy_lab.cpachecker.cpa.pointer2.util.LocationSet;
import org.sosy_lab.cpachecker.exceptions.UnsupportedCCodeException;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public class ReachingDefUtils {

  private static List<CFANode> cfaNodes;

  public static List<CFANode> getAllNodesFromCFA() {
    return cfaNodes;
  }

  public static Pair<Set<MemoryLocation>, Map<FunctionEntryNode, Set<MemoryLocation>>>
      getAllVariables(CFANode pMainNode) {
    List<MemoryLocation> globalVariables = new ArrayList<>();
    List<CFANode> nodes = new ArrayList<>();

    assert(pMainNode instanceof FunctionEntryNode);
    Map<FunctionEntryNode, Set<MemoryLocation>> result = new HashMap<>();

    Set<FunctionEntryNode> reachedFunctions = new HashSet<>();
    Deque<FunctionEntryNode> functionsToProcess = new ArrayDeque<>();

    Deque<CFANode> currentWaitlist = new ArrayDeque<>();
    Set<CFANode> seen = new HashSet<>();
    List<MemoryLocation> localVariables = new ArrayList<>();
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

      Optional<? extends AVariableDeclaration> retVar = currentFunction.getReturnVariable();
      if (retVar.isPresent()) {
        localVariables.add(MemoryLocation.valueOf(retVar.get().getQualifiedName()));
      }

      while (!currentWaitlist.isEmpty()) {
        currentElement = currentWaitlist.pop();
        nodes.add(currentElement);

        for (CFAEdge out : CFAUtils.leavingEdges(currentElement)) {
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

          if(out instanceof CDeclarationEdge) {
            handleDeclaration((CDeclarationEdge) out, globalVariables, localVariables);
          }

          if (!seen.contains(out.getSuccessor())) {
            currentWaitlist.add(out.getSuccessor());
            seen.add(out.getSuccessor());
          }
        }
      }

      result.put(currentFunction, ImmutableSet.copyOf(localVariables));
    }
    cfaNodes = ImmutableList.copyOf(nodes);
    return Pair.of(ImmutableSet.copyOf(globalVariables), result);
  }

  private static void handleDeclaration(
      final CDeclarationEdge out,
      final List<MemoryLocation> globalVariables,
      final List<MemoryLocation> localVariables) {
    if (out.getDeclaration() instanceof CVariableDeclaration) {
      if (out.getDeclaration().isGlobal()) {
        globalVariables.add(MemoryLocation.valueOf((out.getDeclaration()).getQualifiedName()));
      } else {
        localVariables.add(MemoryLocation.valueOf((out.getDeclaration().getQualifiedName())));
      }
    }
  }

  public static Set<MemoryLocation> possiblePointees(CExpression pExp, PointerState pPointerState) {
    Set<MemoryLocation> possibleOperands;

    if (pExp instanceof CPointerExpression) {
      possibleOperands = possiblePointees(((CPointerExpression) pExp).getOperand(), pPointerState);

    } else if (pExp instanceof CIdExpression) {
      return Collections.singleton(
          MemoryLocation.valueOf(((CIdExpression) pExp).getDeclaration().getQualifiedName()));

    } else if (pExp instanceof CFieldReference) {
      if (((CFieldReference) pExp).isPointerDereference()) {
        possibleOperands =
            possiblePointees(((CFieldReference) pExp).getFieldOwner(), pPointerState);
      } else {
        return possiblePointees(((CFieldReference) pExp).getFieldOwner(), pPointerState);
      }
    } else if (pExp instanceof CArraySubscriptExpression) {
      return possiblePointees(
          ((CArraySubscriptExpression) pExp).getArrayExpression(), pPointerState);

    } else if (pExp instanceof CCastExpression) {
      return possiblePointees(((CCastExpression) pExp).getOperand(), pPointerState);
    } else {
      return null;
    }

    if (possibleOperands == null) {
      return null;
    }

    Set<MemoryLocation> possiblePointees = new HashSet<>();
    for (MemoryLocation possibleOp : possibleOperands) {
      LocationSet pointedTo = pPointerState.getPointsToSet(possibleOp);

      if (pointedTo == null || pointedTo.isTop()) {
        return null;

      } else if (pointedTo.isBot()) {
        return Collections.emptySet();

      } else {
        assert pointedTo instanceof ExplicitLocationSet;
        ExplicitLocationSet pointees = (ExplicitLocationSet) pointedTo;
        Iterables.addAll(possiblePointees, pointees);
      }
    }
    return possiblePointees;
  }

  public static class VariableExtractor
      extends DefaultCExpressionVisitor<MemoryLocation, UnsupportedCCodeException> {

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
    protected MemoryLocation visitDefault(CExpression pExp) {
      return null;
    }
    // TODO adapt, need more
    @Override
    public MemoryLocation visit(CArraySubscriptExpression pIastArraySubscriptExpression)
        throws UnsupportedCCodeException {
      warning = "Analysis may be unsound in case of aliasing.";
      return pIastArraySubscriptExpression.getArrayExpression().accept(this);
    }

    @Override
    public MemoryLocation visit(CCastExpression pIastCastExpression)
        throws UnsupportedCCodeException {
      return pIastCastExpression.getOperand().accept(this);
    }

    @Override
    public MemoryLocation visit(CComplexCastExpression pIastCastExpression)
        throws UnsupportedCCodeException {
      return pIastCastExpression.getOperand().accept(this);
    }

    @Override
    public MemoryLocation visit(CFieldReference pIastFieldReference)
        throws UnsupportedCCodeException {
      if (pIastFieldReference.isPointerDereference()) {
        throw new UnsupportedCCodeException(
            "Does not support assignment to dereferenced variable due to missing aliasing support", edgeForExpression,
            pIastFieldReference);
      }
      warning = "Analysis may be unsound in case of aliasing.";
      return pIastFieldReference.getFieldOwner().accept(this);
    }

    @Override
    public MemoryLocation visit(CIdExpression pIastIdExpression) {
      return MemoryLocation.valueOf(pIastIdExpression.getDeclaration().getQualifiedName());
    }

    @Override
    public MemoryLocation visit(CUnaryExpression pIastUnaryExpression)
        throws UnsupportedCCodeException {
      return pIastUnaryExpression.getOperand().accept(this);
    }

    @Override
    public MemoryLocation visit(CPointerExpression pIastUnaryExpression)
        throws UnsupportedCCodeException {
        throw new UnsupportedCCodeException(
            "Does not support assignment to dereferenced variable due to missing aliasing support", edgeForExpression,
            pIastUnaryExpression);
    }
  }

}
