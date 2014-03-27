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
package org.sosy_lab.cpachecker.cpa.value.refiner.utils;

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
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisTransferRelation;

/**
 * Helper class that collects the set of variables on which all assume edges in the given path depend on (i.e. the transitive closure).
 */
 public class AssumptionUseDefinitionCollector {
  /**
   * the set of global variables declared in the given path
   */
  private final Set<String> globalVariables = new HashSet<>();

  /**
   * the set of variables in the transitive-closure of the assume edges
   */
  private final Set<String> collectedVariables = new HashSet<>();

  /**
   * the set of variables for which to find the referencing ones
   */
  private final Set<String> dependingVariables = new HashSet<>();

  /**
   * the last traversed function return edge - needed as we go backwards through the edges to obtain the
   * FunctionSummaryEdge corresponding to a currently visited ReturnStatementEdge
   */
  private FunctionReturnEdge previousFunctionReturnEdge = null;

  /**
   * This method acts as the constructor of the class.
   */
  public AssumptionUseDefinitionCollector() { }

  /**
   * This method collects the respective referenced variables in the given path.
   *
   * @param path the path to analyze
   * @return the mapping of location to referenced variables in the given path
   */
  public Set<String> obtainUseDefInformation(List<CFAEdge> path) {

    dependingVariables.clear();
    collectedVariables.clear();

    determineGlobalVariables(path);

    for (int i = path.size() - 1; i >= 0; i--) {
      CFAEdge edge = path.get(i);
      collectVariables(edge, collectedVariables);
    }

    // for full paths, the set of depending variables always has be empty at this point,
    // but sometimes, the use-def information is derived from incomplete paths,
    // and for those it can happen that not all depending variables are consumed
    assert dependingVariables.size() == 0 || isIncompletePath(path);

    // add the remaining depending variables to the set of collectedVariables
    collectedVariables.addAll(dependingVariables);

    return collectedVariables;
  }

  /**
   * This method determines if the given path is only a suffix of a complete path.
   *
   * @param path the path to check
   * @return true, if the path is incomplete, else false
   */
  private boolean isIncompletePath(List<CFAEdge> path) {
    return path.get(0).getPredecessor().getNumEnteringEdges() > 0;
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
      } else if (edge.getEdgeType() == CFAEdgeType.MultiEdge) {
        determineGlobalVariables(((MultiEdge)edge).getEdges());
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
  private void collectVariables(CFAEdge edge, Set<String> collectedVariables) {
    String currentFunction = edge.getPredecessor().getFunctionName();

    switch (edge.getEdgeType()) {
    case BlankEdge:
    case CallToReturnEdge:
      //nothing to do
      break;

    case FunctionReturnEdge:
      previousFunctionReturnEdge = (FunctionReturnEdge)edge;
      break;

    case DeclarationEdge:
      CDeclaration declaration = ((CDeclarationEdge)edge).getDeclaration();

      if (declaration.getName() != null) {
        String variableName = declaration.getName();

        if(!declaration.isGlobal()) {
          variableName = scoped(variableName, currentFunction);
        }

        if (dependingVariables.contains(variableName)) {
          dependingVariables.remove(variableName);
          collectedVariables.add(variableName);

          if(((CVariableDeclaration)declaration).getInitializer() instanceof CInitializerExpression) {
            CInitializerExpression initializer = ((CInitializerExpression)((CVariableDeclaration)declaration).getInitializer());
            if(initializer != null) {
              collectVariables(edge, initializer.getExpression());
            }
          }
        }
      }
      break;

    case ReturnStatementEdge:
        CReturnStatementEdge returnStatementEdge = (CReturnStatementEdge)edge;

        // for cases where error path ends with a return statement
        if(previousFunctionReturnEdge == null) {
          break;
        }

        CFunctionSummaryEdge cFunctionSummaryEdge = (CFunctionSummaryEdge)previousFunctionReturnEdge.getSummaryEdge();

        CFunctionCall functionCall = cFunctionSummaryEdge.getExpression();

        if (functionCall instanceof CFunctionCallAssignmentStatement) {
          CFunctionCallAssignmentStatement funcAssign = (CFunctionCallAssignmentStatement)functionCall;
          String assignedVariable = scoped(funcAssign.getLeftHandSide().toASTString(), previousFunctionReturnEdge.getSuccessor().getFunctionName());

          if (dependingVariables.contains(assignedVariable)) {
            dependingVariables.remove(assignedVariable);

            collectedVariables.add(assignedVariable);
            // also add special FUNCTION_RETURN_VAR as relevant variable
            collectedVariables.add(scoped(ValueAnalysisTransferRelation.FUNCTION_RETURN_VAR, returnStatementEdge.getPredecessor().getFunctionName()));
            collectVariables(returnStatementEdge, returnStatementEdge.getExpression());
          }
        }

        previousFunctionReturnEdge = null;

        break;

    case FunctionCallEdge:
      CFunctionCallEdge functionCallEdge = (CFunctionCallEdge)edge;
      CFunctionDeclaration functionDefinition = functionCallEdge.getSuccessor().getFunctionDefinition();

      int j = 0;
      for (CParameterDeclaration parameter : functionDefinition.getParameters()) {
        // collect the formal parameter, and make the argument a depending variable
        if (dependingVariables.contains(parameter.getQualifiedName())) {
          dependingVariables.remove(parameter.getQualifiedName());
          collectedVariables.add(parameter.getQualifiedName());

          collectVariables(functionCallEdge, functionCallEdge.getArguments().get(j));
        }
        j++;
      }
      break;

    case AssumeEdge:
      CAssumeEdge assumeEdge = (CAssumeEdge)edge;
      collectVariables(assumeEdge, assumeEdge.getExpression());
      break;

    case StatementEdge:
      CStatementEdge statementEdge = (CStatementEdge)edge;
      if (statementEdge.getStatement() instanceof CAssignment) {
        CAssignment assignment = (CAssignment)statementEdge.getStatement();
        String assignedVariable = scoped(assignment.getLeftHandSide().toASTString(), currentFunction);

        if (dependingVariables.contains(assignedVariable)) {
          dependingVariables.remove(assignedVariable);
          collectedVariables.add(assignedVariable);
          collectVariables(statementEdge, assignment.getRightHandSide());
        }
      }
      break;

    case MultiEdge:
      List<CFAEdge> edges = ((MultiEdge)edge).getEdges();

      // process MultiEdges also in reverse order
      for(int i = edges.size() - 1; i >= 0; i--) {
        collectVariables(edges.get(i), collectedVariables);
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
   */
  private void collectVariables(CFAEdge edge, CRightHandSide rightHandSide) {
    rightHandSide.accept(new CollectVariablesVisitor(edge));
  }

  /**
   * the visitor responsible for the actual collecting job
   */
  private class CollectVariablesVisitor extends DefaultCExpressionVisitor<Void, RuntimeException>
                                               implements CRightHandSideVisitor<Void, RuntimeException> {
    /**
     * the current assignment edge
     */
    private final CFAEdge currentEdge;

    /**
     * This method acts as the constructor of the class.
     *
     * @param currentEdge the assignment edge to analyze
     * @param collectedVariables the mapping of locations to variable names up to the current edge
     */
    public CollectVariablesVisitor(CFAEdge currentEdge) {
      this.currentEdge = currentEdge;
    }

    private void collectVariables(String variableName) {
      dependingVariables.add(scoped(variableName, currentEdge.getPredecessor().getFunctionName()));
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
        collectVariables(pE.toASTString());
        //$FALL-THROUGH$
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