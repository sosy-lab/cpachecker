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

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Helper class that collects the set of variables on which all assume edges in the given path depend on (i.e. the transitive closure).
 */
 public class AssumptionVariablesCollector {
  /**
   * the set of global variables declared in the given path
   */
  Set<String> globalVariables           = new HashSet<String>();

  /**
   * the set of variables for which to find the referencing ones
   */
  Collection<String> dependingVariables = new HashSet<String>();

  /**
   * This method acts as the constructor of the class.
   */
  public AssumptionVariablesCollector() { }

  /**
   * This method collects the respective referenced variables in the given path.
   *
   * @param path the path to analyze
   * @return the mapping of location to referenced variables in the given path
   */
  public Multimap<CFANode, String> collectVariables(List<CFAEdge> path) {

    determineGlobalVariables(path);

    Multimap<CFANode, String> collectedVariables = HashMultimap.create();
    for (int i = path.size() - 1; i >= 0; i--) {
      CFAEdge edge = path.get(i);
      CFAEdge succ = (i == path.size() - 1) ? null : path.get(i + 1);
      collectVariables(edge, collectedVariables, succ);
    }

    return collectedVariables;
  }

  /**
   * This method determines the set of global variables declared in the given path.
   *
   * @param path the path to analyze
   */
  private void determineGlobalVariables(List<CFAEdge> path) {
    for (CFAEdge edge : path) {
      if (edge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
        CDeclaration declaration = ((CDeclarationEdge)edge).getDeclaration();
        if (isGlobalVariableDeclaration(declaration)) {
          globalVariables.add(declaration.getName());
        }
      }
    }
  }

  /**
   * This method decides whether or not a declaration is a global (variable) declaration or not.
   *
   * @param declaration the declaration to analyze
   * @return true if the declaration is a global (variable) declaration or not, else false
   */
  private boolean isGlobalVariableDeclaration(CDeclaration declaration) {
    return declaration.isGlobal() && !(declaration instanceof CFunctionDeclaration);
  }

  /**
   * This method collects the referenced variables in a edge into the mapping of collected variables.
   *
   * @param edge the edge to analyze
   * @param collectedVariables the mapping of collected variables
   */
  private void collectVariables(CFAEdge edge, Multimap<CFANode, String> collectedVariables, CFAEdge succ) {
    String currentFunction = edge.getPredecessor().getFunctionName();

    switch (edge.getEdgeType()) {
    case BlankEdge:
    case CallToReturnEdge:
      //nothing to do
      break;

    case FunctionReturnEdge:
      CFunctionReturnEdge returnEdge = (CFunctionReturnEdge)edge;

      CFunctionSummaryEdge cFunctionSummaryEdge = returnEdge.getSummaryEdge();

      CFunctionCall functionCall = cFunctionSummaryEdge.getExpression();

      if (functionCall instanceof CFunctionCallAssignmentStatement) {
        CFunctionCallAssignmentStatement funcAssign = (CFunctionCallAssignmentStatement)functionCall;
        String assignedVariable = scoped(funcAssign.getLeftHandSide().toASTString(), returnEdge.getSuccessor().getFunctionName());

        if (dependingVariables.contains(assignedVariable)) {
          collectedVariables.put(cFunctionSummaryEdge.getSuccessor(), assignedVariable);


          CReturnStatementEdge returnStatementEdge;
          CFAEdge currentEdge = null;
          CFAEdge enteringEdge = returnEdge.getPredecessor().getEnteringEdge(0);

          if (enteringEdge instanceof MultiEdge) {
            for (CFAEdge singleEdge : (MultiEdge)enteringEdge) {
              currentEdge = singleEdge;
            }

            enteringEdge = currentEdge;
          }

          assert(enteringEdge instanceof CReturnStatementEdge);

          returnStatementEdge = (CReturnStatementEdge)enteringEdge;

          collectVariables(returnStatementEdge, returnStatementEdge.getExpression(), collectedVariables, true);
        }
      }
      break;

    case ReturnStatementEdge:
      if (succ == null) {
        break;
      }
      CReturnStatementEdge returnStatementEdge = (CReturnStatementEdge)edge;

      CFunctionReturnEdge returnEdge2 = (CFunctionReturnEdge)succ;

      CFunctionSummaryEdge cFunctionSummaryEdge2 = returnEdge2.getSummaryEdge();

      CFunctionCall functionCall2 = cFunctionSummaryEdge2.getExpression();

      if (functionCall2 instanceof CFunctionCallAssignmentStatement) {
        CFunctionCallAssignmentStatement funcAssign = (CFunctionCallAssignmentStatement)functionCall2;
        String assignedVariable = scoped(funcAssign.getLeftHandSide().toASTString(), succ.getSuccessor().getFunctionName());

        if (dependingVariables.contains(assignedVariable)) {
          collectedVariables.put(cFunctionSummaryEdge2.getSuccessor(), assignedVariable);
          collectVariables(returnStatementEdge, returnStatementEdge.getExpression(), collectedVariables, true);
          collectVariables(returnStatementEdge, new CIdExpression(returnStatementEdge.getExpression().getFileLocation(),
              null,
              "___cpa_temp_result_var_",
              null), collectedVariables, true);
        }
      }

      break;

    case DeclarationEdge:
      //System.out.println("inspecting edge " + edge.getRawStatement());
      CDeclaration declaration = ((CDeclarationEdge)edge).getDeclaration();
      if (declaration.getName() != null && declaration.isGlobal()) {
        globalVariables.add(declaration.getName());

        if (dependingVariables.contains(declaration.getName())) {
          collectedVariables.put(edge.getSuccessor(), declaration.getName());
        }
      }
      break;

    case FunctionCallEdge:
      //System.out.println("inspecting edge " + edge.getRawStatement());
      CFunctionCallEdge functionCallEdge = (CFunctionCallEdge)edge;

      CFunctionCall func = functionCallEdge.getSummaryEdge().getExpression();

      if (func instanceof CFunctionCallAssignmentStatement) {
        CFunctionCallAssignmentStatement funcAssign = (CFunctionCallAssignmentStatement)func;
        String assignedVariable = scoped(funcAssign.getLeftHandSide().toASTString(), currentFunction);

        if (dependingVariables.contains(assignedVariable)) {
          collectedVariables.put(functionCallEdge.getSummaryEdge().getSuccessor(), assignedVariable);
          collectVariables(functionCallEdge, funcAssign.getRightHandSide(), collectedVariables, true);
        }
      }

      String functionName = functionCallEdge.getSuccessor().getFunctionDefinition().getName();

      int i = 0;
      for (CParameterDeclaration parameter : functionCallEdge.getSuccessor().getFunctionDefinition().getType().getParameters()) {
        String parameterName = functionName + "::" + parameter.getName();

        // collect the formal parameter, and make the argument a depending variable
        if (dependingVariables.contains(parameterName)) {
          collectedVariables.put(functionCallEdge.getSuccessor(), parameterName);
          dependingVariables.add(scoped(functionCallEdge.getArguments().get(i).toASTString(), functionCallEdge.getPredecessor().getFunctionName()));
        }
        i++;
      }

      break;

    case AssumeEdge:
      // System.out.println("inspecting edge " + edge.getRawStatement());
      CAssumeEdge assumeEdge = (CAssumeEdge)edge;
      collectVariables(assumeEdge, assumeEdge.getExpression(), collectedVariables, true);
      break;

    case StatementEdge:
      //System.out.println("inspecting edge " + edge.getRawStatement());
      CStatementEdge statementEdge = (CStatementEdge)edge;
      if (statementEdge.getStatement() instanceof CAssignment) {
        CAssignment assignment = (CAssignment)statementEdge.getStatement();
        String assignedVariable = scoped(assignment.getLeftHandSide().toASTString(), currentFunction);

        if (dependingVariables.contains(assignedVariable)) {
          collectedVariables.put(edge.getSuccessor(), assignedVariable);
          collectVariables(statementEdge, assignment.getRightHandSide(), collectedVariables, false);
        }
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

  /**
   * This method delegates the collecting job to the CollectVariablesVisitor.
   *
   * @param edge the edge to analyze
   * @param rightHandSide the right hand side of the assignment
   * @param collectedVariables the current mapping of locations to variable names up to the current edge
   */
  private void collectVariables(CFAEdge edge,
                                CRightHandSide rightHandSide,
                                Multimap<CFANode, String> collectedVariables,
                                boolean dependOnly) {
    rightHandSide.accept(new CollectVariablesVisitor(edge, collectedVariables, dependOnly));
  }

  /**
   * the visitor responsible for the actual collecting job
   */
  private class CollectVariablesVisitor extends DefaultCExpressionVisitor<Void, RuntimeException>
                                               implements CRightHandSideVisitor<Void, RuntimeException> {
    /**
     * the current mapping of locations to variable names up to the current edge
     */
    private final Multimap<CFANode, String> collectedVariables;

    /**
     * the current assignment edge
     */
    private final CFAEdge currentEdge;

    boolean doCollect = true;
    /**
     * This method acts as the constructor of the class.
     *
     * @param currentEdge the assignment edge to analyze
     * @param collectedVariables the mapping of locations to variable names up to the current edge
     */
    public CollectVariablesVisitor(CFAEdge currentEdge, Multimap<CFANode, String> collectedVariables, boolean dependOnly) {
      this.currentEdge         = currentEdge;
      this.collectedVariables  = collectedVariables;
      this.doCollect           = dependOnly;
    }

    private void collectVariables(String variableName) {
      //System.out.println("adding new depending variable " + scoped(variableName, currentEdge.getPredecessor().getFunctionName()));

      dependingVariables.add(scoped(variableName, currentEdge.getPredecessor().getFunctionName()));

      if (doCollect) {
        collectedVariables.put(currentEdge.getSuccessor(), scoped(variableName, currentEdge.getPredecessor().getFunctionName()));
      }
    }

    @Override
    public Void visit(CIdExpression pE) {
      collectVariables(pE.getName());
      return null;
    }

    @Override
    public Void visit(CArraySubscriptExpression pE) {
      collectVariables(pE.toASTString());
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
      collectVariables(pE.toASTString());
      pE.getFieldOwner().accept(this);
      return null;
    }

    @Override
    public Void visit(CFunctionCallExpression pE) {
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
        collectVariables(pE.toASTString());
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
