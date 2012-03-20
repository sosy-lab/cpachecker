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
package org.sosy_lab.cpachecker.cpa.explicit;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.sosy_lab.cpachecker.cfa.ast.DefaultExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.IASTArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTAssignment;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.RightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;


/**
 * Helper class that collects all "relevant" variables in a given set of nodes, where "relevant" means,
 * that they either appear on the left hand side of an assignment or within an assume edge.
 */
public class AssignedVariablesCollector {
  Set<String> globalVariables = new HashSet<String>();

  public AssignedVariablesCollector() {
  }

  public Multimap<CFAEdge, String> collectVars(Collection<CFAEdge> edges) {
    Multimap<CFAEdge, String> collectedVariables = HashMultimap.create();

    for(CFAEdge edge : edges) {
      collectVariables(edge, collectedVariables);
    }

    return collectedVariables;
  }

  private void collectVariables(CFAEdge edge, Multimap<CFAEdge, String> collectedVariables) {
    String currentFunction = edge.getPredecessor().getFunctionName();

    switch(edge.getEdgeType()) {
    case BlankEdge:
    case CallToReturnEdge:
    case ReturnStatementEdge:
      //nothing to do
      break;

    case DeclarationEdge:
      IASTDeclaration declaration = ((DeclarationEdge)edge).getDeclaration();
      if(declaration.getName() != null && declaration.isGlobal()) {
        globalVariables.add(declaration.getName());
        collectedVariables.put(edge, declaration.getName());
      }
      break;

    case AssumeEdge:
      AssumeEdge assumeEdge = (AssumeEdge)edge;
      collectVariables(assumeEdge, assumeEdge.getExpression(), collectedVariables);
      break;

    case StatementEdge:
      StatementEdge statementEdge = (StatementEdge)edge;
      if(statementEdge.getStatement() instanceof IASTAssignment) {
        IASTAssignment assignment = (IASTAssignment)statementEdge.getStatement();
        String assignedVariable = assignment.getLeftHandSide().toASTString();
        collectedVariables.put(edge, scoped(assignedVariable, currentFunction));
      }
      break;

    case FunctionCallEdge:
      FunctionCallEdge functionCallEdge = (FunctionCallEdge)edge;
      IASTFunctionCall functionCall     = functionCallEdge.getSummaryEdge().getExpression();

      if(functionCall instanceof IASTFunctionCallAssignmentStatement) {
        IASTFunctionCallAssignmentStatement funcAssign = (IASTFunctionCallAssignmentStatement)functionCall;
        String assignedVariable = scoped(funcAssign.getLeftHandSide().toASTString(), currentFunction);

        // track it at return (2nd statement below), not at call (next, commented statement)
        //collectedVariables.put(edge.getSuccessor(), assignedVariable);
        collectedVariables.put(functionCallEdge.getSummaryEdge(), assignedVariable);


        collectedVariables.put(edge, assignedVariable);
        collectVariables(functionCallEdge, funcAssign.getRightHandSide(), collectedVariables);
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

  private void collectVariables(CFAEdge edge, IASTRightHandSide rightHandSide, Multimap<CFAEdge, String> collectedVariables) {
    rightHandSide.accept(new CollectVariablesVisitor(edge, collectedVariables));
  }

  private class CollectVariablesVisitor extends DefaultExpressionVisitor<Void, RuntimeException>
                                               implements RightHandSideVisitor<Void, RuntimeException> {

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
    public Void visit(IASTIdExpression pE) {
      collectVariable(pE.getName());
      return null;
    }

    @Override
    public Void visit(IASTArraySubscriptExpression pE) {
      collectVariable(pE.toASTString());
      pE.getArrayExpression().accept(this);
      pE.getSubscriptExpression().accept(this);
      return null;
    }

    @Override
    public Void visit(IASTBinaryExpression pE) {
      pE.getOperand1().accept(this);
      pE.getOperand2().accept(this);
      return null;
    }

    @Override
    public Void visit(IASTCastExpression pE) {
      pE.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(IASTFieldReference pE) {
      collectVariable(pE.toASTString());
      pE.getFieldOwner().accept(this);
      return null;
    }

    @Override
    public Void visit(IASTFunctionCallExpression pE) {
      pE.getFunctionNameExpression().accept(this);
      for (IASTExpression param : pE.getParameterExpressions()) {
        param.accept(this);
      }
      return null;
    }

    @Override
    public Void visit(IASTUnaryExpression pE) {
      UnaryOperator op = pE.getOperator();

      switch(op) {
      case AMPER:
      case STAR:
        collectVariable(pE.toASTString());
      default:
        pE.getOperand().accept(this);
      }

      return null;
    }

    @Override
    protected Void visitDefault(IASTExpression pExp) {
      return null;
    }
  }
}
