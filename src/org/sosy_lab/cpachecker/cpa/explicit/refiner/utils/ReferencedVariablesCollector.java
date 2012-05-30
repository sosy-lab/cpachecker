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
import org.sosy_lab.cpachecker.cfa.ast.IASTFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.RightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.MultiEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CallToReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.FunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.ReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.StatementEdge;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/**
 * Helper class that collects the set of variables on which the initial given set depend on (i.e. the transitive closure).
 *
 * This step is necessary for handling programs like this:
 * <code>
 *  x = 1; // <- this location will not have any associated predicates
 *  y = x;
 *  z = x;
 *  if(y != z)
 *    goto ERROR;
 * </code>
 *
 * Something similar might be needed for programs, like this, where x is a global variable. This is not handled yet.
 * <code>
 *  x = 1;
 *  y = getX();
 *  z = getX();
 *  if(y != z)
 *    goto ERROR;
 * </code>
 */
public class ReferencedVariablesCollector {
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
   *
   * @param initalVariables the initial set of depending variables
   */
  public ReferencedVariablesCollector(Collection<String> initalVariables) {
    this.dependingVariables.addAll(initalVariables);
  }

  /**
   * This method collects the respective referenced variables in the given path.
   *
   * @param path the path to analyze
   * @return the mapping of location to referenced variables in the given path
   */
  public Multimap<CFANode, String> collectVariables(List<CFAEdge> path) {

    determineGlobalVariables(path);

    Multimap<CFANode, String> collectedVariables = HashMultimap.create();
    for(int i = path.size() - 1; i >= 0; i--) {
      CFAEdge succ = (i == path.size() - 1) ? null : path.get(i + 1);
      collectVariables(path.get(i), collectedVariables, succ);
    }

    return collectedVariables;
  }

  /**
   * This method determines the set of global variables declared in the given path.
   *
   * @param path the path to analyze
   */
  private void determineGlobalVariables(List<CFAEdge> path) {
    for(CFAEdge edge : path) {
      if(edge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
        IASTDeclaration declaration = ((DeclarationEdge)edge).getDeclaration();
        if(isGlobalVariableDeclaration(declaration)) {
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
  private boolean isGlobalVariableDeclaration(IASTDeclaration declaration) {
    return declaration.isGlobal() && !(declaration instanceof IASTFunctionDeclaration);
  }

  /**
   * This method collects the referenced variables in a edge into the mapping of collected variables.
   *
   * @param edge the edge to analyze
   * @param collectedVariables the mapping of collected variables
   */
  private void collectVariables(CFAEdge edge, Multimap<CFANode, String> collectedVariables, CFAEdge succ) {
    String currentFunction = edge.getPredecessor().getFunctionName();

    switch(edge.getEdgeType()) {
    case StatementEdge:
      StatementEdge statementEdge = (StatementEdge)edge;
      if (statementEdge.getStatement() instanceof IASTAssignment) {
        IASTAssignment assignment = (IASTAssignment)statementEdge.getStatement();
        String assignedVariable = scoped(assignment.getLeftHandSide().toASTString(), currentFunction);

        // assigned variable is tracked, then also track assigning variables
        if(dependingVariables.contains(assignedVariable)) {
          collectedVariables.put(edge.getSuccessor(), assignedVariable);
          collectVariables(statementEdge, assignment.getRightHandSide(), collectedVariables);
        }
      }
      break;

    case FunctionCallEdge:
      FunctionCallEdge functionCallEdge = (FunctionCallEdge)edge;
      IASTFunctionCall functionCall     = functionCallEdge.getSummaryEdge().getExpression();

      if(functionCall instanceof IASTFunctionCallAssignmentStatement) {
        IASTFunctionCallAssignmentStatement funcAssign = (IASTFunctionCallAssignmentStatement)functionCall;
        String assignedVariable = scoped(funcAssign.getLeftHandSide().toASTString(), currentFunction);

        // assigned variable is tracked, then also track variables of function call
        if(dependingVariables.contains(assignedVariable)) {
          collectedVariables.put(functionCallEdge.getSummaryEdge().getSuccessor(), assignedVariable);
          collectVariables(functionCallEdge, funcAssign.getRightHandSide(), collectedVariables);
        }
      }
      break;

    case AssumeEdge:
      AssumeEdge assumeEdge = (AssumeEdge)edge;
      IASTExpression assumeExpression = assumeEdge.getExpression();

      // always inspect assume edges
      collectVariables(assumeEdge, assumeExpression, collectedVariables);
      break;


    case FunctionReturnEdge:
      FunctionReturnEdge returnEdge = (FunctionReturnEdge)edge;

      CallToReturnEdge callToReturnEdge = returnEdge.getSummaryEdge();

      IASTFunctionCall functionCall2 = callToReturnEdge.getExpression();

      if(functionCall2 instanceof IASTFunctionCallAssignmentStatement) {
        IASTFunctionCallAssignmentStatement funcAssign = (IASTFunctionCallAssignmentStatement)functionCall2;
        String assignedVariable = scoped(funcAssign.getLeftHandSide().toASTString(), returnEdge.getSuccessor().getFunctionName());

        if(dependingVariables.contains(assignedVariable)) {
          // is this needed here?
          // for test/programs/benchmarks/ldv-regression/rule60_list2.c-unsafe_1.cil.c this is not needed
          // for tracking my_malloc::tmp
          //collectedVariables.put(callToReturnEdge.getSuccessor(), assignedVariable);


          ReturnStatementEdge returnStatementEdge;
          CFAEdge currentEdge = null;
          CFAEdge enteringEdge = returnEdge.getPredecessor().getEnteringEdge(0);

          if(enteringEdge instanceof MultiEdge) {
            for(CFAEdge singleEdge : (MultiEdge)enteringEdge) {
              currentEdge = singleEdge;
            }

            enteringEdge = currentEdge;
          }

          assert(enteringEdge instanceof ReturnStatementEdge);

          returnStatementEdge = (ReturnStatementEdge)enteringEdge;

          collectVariables(returnStatementEdge, returnStatementEdge.getExpression(), collectedVariables);
        }
      }
      break;

    case DeclarationEdge:
      //System.out.println("inspecting edge " + edge.getRawStatement());
      IASTDeclaration declaration = ((DeclarationEdge)edge).getDeclaration();
      if(declaration.getName() != null && declaration.isGlobal()) {
        globalVariables.add(declaration.getName());

        if(dependingVariables.contains(declaration.getName()))
          collectedVariables.put(edge.getSuccessor(), declaration.getName());
      }
      break;
    case ReturnStatementEdge:
      if(succ == null)
        break;
      ReturnStatementEdge returnStatementEdge = (ReturnStatementEdge)edge;

      FunctionReturnEdge returnEdge2 = (FunctionReturnEdge)succ;

      CallToReturnEdge callToReturnEdge2 = returnEdge2.getSummaryEdge();

      IASTFunctionCall functionCall3 = callToReturnEdge2.getExpression();

      if(functionCall3 instanceof IASTFunctionCallAssignmentStatement) {
        IASTFunctionCallAssignmentStatement funcAssign = (IASTFunctionCallAssignmentStatement)functionCall3;
        String assignedVariable = scoped(funcAssign.getLeftHandSide().toASTString(), succ.getSuccessor().getFunctionName());

        if(dependingVariables.contains(assignedVariable)) {
          collectedVariables.put(callToReturnEdge2.getSuccessor(), assignedVariable);
          collectVariables(returnStatementEdge, returnStatementEdge.getExpression(), collectedVariables);
        }
      }
      break;

    default:
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
  private void collectVariables(CFAEdge edge, IASTRightHandSide rightHandSide, Multimap<CFANode, String> collectedVariables) {
    rightHandSide.accept(new CollectVariablesVisitor(edge, collectedVariables));
  }

  /**
   * the visitor responsible for the actual collecting job
   */
  private class CollectVariablesVisitor extends DefaultExpressionVisitor<Void, RuntimeException>
                                               implements RightHandSideVisitor<Void, RuntimeException> {
    /**
     * the current mapping of locations to variable names up to the current edge
     */
    private final Multimap<CFANode, String> collectedVariables;

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
    public CollectVariablesVisitor(CFAEdge currentEdge, Multimap<CFANode, String> collectedVariables) {
      this.currentEdge         = currentEdge;
      this.collectedVariables  = collectedVariables;
    }

    private void collectVariables(String variableName) {

      String scopedVariableName = scoped(variableName, currentEdge.getPredecessor().getFunctionName());

      // only collect variables of assume edges if they are depended on
      if(currentEdge.getEdgeType() == CFAEdgeType.AssumeEdge) {
        // @TODO: maybe try tracking "all" of these - or at least, when one variable is tracked, also track the other ones in an assume edge (if any)
        // or track only those that are conjuncted (&&), but not those that are disjuncted (||)
        if(dependingVariables.contains(scopedVariableName)) {
          collectedVariables.put(currentEdge.getSuccessor(), scopedVariableName);
        }
      }
      // for other CFA edge types, collect the variables (selection done beforehand based on assigned variable, see above)
      else {
        dependingVariables.add(scopedVariableName);

        collectedVariables.put(currentEdge.getSuccessor(), scopedVariableName);
      }
    }

    @Override
    public Void visit(IASTIdExpression pE) {
      collectVariables(pE.getName());
      return null;
    }

    @Override
    public Void visit(IASTArraySubscriptExpression pE) {
      collectVariables(pE.toASTString());
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
      collectVariables(pE.toASTString());
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
        collectVariables(pE.toASTString());
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
