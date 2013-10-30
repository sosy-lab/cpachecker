/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.explicit.refiner.utils;

import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cpa.arg.Path;
import org.sosy_lab.cpachecker.cpa.explicit.ExplicitTransferRelation;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;


/**
 * Helper class that collects all "relevant" variables in a given set of nodes, where "relevant" means,
 * that they either appear on the left hand side of an assignment or within an assume edge.
 */
public class AssignedVariablesCollector {
  private Set<String> globalVariables = new HashSet<String>();

  private CFAEdge successorEdge = null;

  public AssignedVariablesCollector() {
  }

  public Multimap<CFAEdge, String> collectVars(Path currentErrorPath) {
    Multimap<CFAEdge, String> collectedVariables = HashMultimap.create();

    for (int i = 0; i < currentErrorPath.size() - 1; i++) {
      successorEdge = (i == currentErrorPath.size() - 1) ? null : currentErrorPath.get(i + 1).getSecond();

      collectVariables(currentErrorPath.get(i).getSecond(), collectedVariables);
    }

    return collectedVariables;
  }

  private void collectVariables(CFAEdge edge, Multimap<CFAEdge, String> collectedVariables) {
    String currentFunction = edge.getPredecessor().getFunctionName();

    switch (edge.getEdgeType()) {
    case BlankEdge:
    case CallToReturnEdge:
      //nothing to do
      break;

    case ReturnStatementEdge:
      CReturnStatementEdge returnStatementEdge = (CReturnStatementEdge)edge;

      CFunctionReturnEdge returnEdge2 = (CFunctionReturnEdge)successorEdge;

      CFunctionSummaryEdge cFunctionSummaryEdge2 = returnEdge2.getSummaryEdge();

      CFunctionCall functionCall2 = cFunctionSummaryEdge2.getExpression();

      if (functionCall2 instanceof CFunctionCallAssignmentStatement) {
        CFunctionCallAssignmentStatement funcAssign = (CFunctionCallAssignmentStatement)functionCall2;
        String assignedVariable = scoped(funcAssign.getLeftHandSide().toASTString(), successorEdge.getSuccessor().getFunctionName());


        collectedVariables.put(cFunctionSummaryEdge2, assignedVariable);
        collectVariables(returnStatementEdge, returnStatementEdge.getExpression(), collectedVariables);
        collectVariables(returnStatementEdge, new CIdExpression(returnStatementEdge.getExpression().getFileLocation(),
            null,
            ExplicitTransferRelation.FUNCTION_RETURN_VAR,
            null), collectedVariables);
      }

      break;

    case DeclarationEdge:
      CDeclaration declaration = ((CDeclarationEdge)edge).getDeclaration();
      if (declaration instanceof CVariableDeclaration && declaration.getName() != null && declaration.isGlobal()) {
        globalVariables.add(declaration.getName());
        collectedVariables.put(edge, declaration.getName());
      }
      break;

    case AssumeEdge:
      CAssumeEdge assumeEdge = (CAssumeEdge)edge;
      collectVariables(assumeEdge, assumeEdge.getExpression(), collectedVariables);
      break;

    case StatementEdge:
      CStatementEdge statementEdge = (CStatementEdge)edge;
      if (statementEdge.getStatement() instanceof CAssignment) {
        CAssignment assignment = (CAssignment)statementEdge.getStatement();
        String assignedVariable = assignment.getLeftHandSide().toASTString();
        collectedVariables.put(edge, scoped(assignedVariable, currentFunction));
      }
      break;

    case FunctionCallEdge:
      CFunctionCallEdge functionCallEdge = (CFunctionCallEdge)edge;
      CFunctionCall functionCall     = functionCallEdge.getSummaryEdge().getExpression();

      if (functionCall instanceof CFunctionCallAssignmentStatement) {
        CFunctionCallAssignmentStatement funcAssign = (CFunctionCallAssignmentStatement)functionCall;
        String assignedVariable = scoped(funcAssign.getLeftHandSide().toASTString(), currentFunction);

        // track it at return (2nd statement below), not at call (next, commented statement)
        //collectedVariables.put(edge.getSuccessor(), assignedVariable);
        collectedVariables.put(functionCallEdge.getSummaryEdge(), assignedVariable);


        collectedVariables.put(edge, assignedVariable);
        collectVariables(functionCallEdge, funcAssign.getRightHandSide(), collectedVariables);
      }

      String functionName = functionCallEdge.getSuccessor().getFunctionDefinition().getName();
      for (CParameterDeclaration parameter : functionCallEdge.getSuccessor().getFunctionDefinition().getType().getParameters()) {
        String parameterName = functionName + "::" + parameter.getName();

        // collect the formal parameter, and make the argument a depending variable
        collectedVariables.put(functionCallEdge, parameterName);
      }

      break;
    }
  }

  /**
   * This method prefixes the name of a non-global variable with a given function name.
   *
   * @param variableName the variable name
   * @param functionName the function name
   * @return the prefixed variable name
   */
  private String scoped(String variableName, String functionName) {
    if (globalVariables.contains(variableName)) {
      return variableName;
    } else {
      return functionName + "::" + variableName;
    }
  }

  private void collectVariables(CFAEdge edge, CRightHandSide rightHandSide, Multimap<CFAEdge, String> collectedVariables) {
    rightHandSide.accept(new CollectVariablesVisitor(edge, collectedVariables));
  }

  private class CollectVariablesVisitor extends DefaultCExpressionVisitor<Void, RuntimeException>
                                               implements CRightHandSideVisitor<Void, RuntimeException> {

    private final CFAEdge currentEdge;
    private final Multimap<CFAEdge, String> collectedVariables;

    public CollectVariablesVisitor(CFAEdge edge, Multimap<CFAEdge, String> collectedVariables) {
      this.currentEdge          = edge;
      this.collectedVariables   = collectedVariables;
    }

    private void collectVariable(String var) {
      collectedVariables.put(currentEdge, scoped(var, currentEdge.getPredecessor().getFunctionName()));
    }

    @Override
    public Void visit(CIdExpression pE) {
      /*if(!(pE.getExpressionType() instanceof CFunctionPointerType))*/ {
        collectVariable(pE.getName());
      }

      return null;
    }

    @Override
    public Void visit(CArraySubscriptExpression pE) {
      collectVariable(pE.toASTString());
      pE.getArrayExpression().accept(this);
      pE.getSubscriptExpression().accept(this);
      return null;
    }

    @Override
    public Void visit(CBinaryExpression pE) {
      pE.getOperand1().accept(this);
      pE.getOperand2().accept(this);
      return null;
    }

    @Override
    public Void visit(CCastExpression pE) {
      pE.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(CFieldReference pE) {
      collectVariable(pE.toASTString());
      pE.getFieldOwner().accept(this);
      return null;
    }

    @Override
    public Void visit(CFunctionCallExpression pE) {
      pE.getFunctionNameExpression().accept(this);
      for (CExpression param : pE.getParameterExpressions()) {
        param.accept(this);
      }
      return null;
    }

    @Override
    public Void visit(CUnaryExpression pE) {
      UnaryOperator op = pE.getOperator();

      switch (op) {
      case AMPER:
      case STAR:
        collectVariable(pE.toASTString());
        break;
      default:
        pE.getOperand().accept(this);
      }

      return null;
    }

    @Override
    protected Void visitDefault(CExpression pExp) {
      return null;
    }
  }
}
