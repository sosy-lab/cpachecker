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
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.MultiEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.CStatementEdge;

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
        CDeclaration declaration = ((CDeclarationEdge)edge).getDeclaration();
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

    switch(edge.getEdgeType()) {
    case StatementEdge:
      CStatementEdge statementEdge = (CStatementEdge)edge;
      if (statementEdge.getStatement() instanceof CAssignment) {
        CAssignment assignment = (CAssignment)statementEdge.getStatement();
        String assignedVariable = scoped(assignment.getLeftHandSide().toASTString(), currentFunction);

        // assigned variable is tracked, then also track assigning variables
        if(dependingVariables.contains(assignedVariable)) {
          collectedVariables.put(edge.getSuccessor(), assignedVariable);
          collectVariables(statementEdge, assignment.getRightHandSide(), collectedVariables);
        }
      }
      break;

    case FunctionCallEdge:
      CFunctionCallEdge functionCallEdge = (CFunctionCallEdge)edge;
      CFunctionCall functionCall     = functionCallEdge.getSummaryEdge().getExpression();

      if(functionCall instanceof CFunctionCallAssignmentStatement) {
        CFunctionCallAssignmentStatement funcAssign = (CFunctionCallAssignmentStatement)functionCall;
        String assignedVariable = scoped(funcAssign.getLeftHandSide().toASTString(), currentFunction);

        // assigned variable is tracked, then also track variables of function call
        if(dependingVariables.contains(assignedVariable)) {
          collectedVariables.put(functionCallEdge.getSummaryEdge().getSuccessor(), assignedVariable);
          collectVariables(functionCallEdge, funcAssign.getRightHandSide(), collectedVariables);
        }
      }
      break;

    case AssumeEdge:
      CAssumeEdge assumeEdge = (CAssumeEdge)edge;
      CExpression assumeExpression = assumeEdge.getExpression();

      // always inspect assume edges
      collectVariables(assumeEdge, assumeExpression, collectedVariables);
      break;


    case FunctionReturnEdge:
      CFunctionReturnEdge returnEdge = (CFunctionReturnEdge)edge;

      CFunctionSummaryEdge cFunctionSummaryEdge = returnEdge.getSummaryEdge();

      CFunctionCall functionCall2 = cFunctionSummaryEdge.getExpression();

      if(functionCall2 instanceof CFunctionCallAssignmentStatement) {
        CFunctionCallAssignmentStatement funcAssign = (CFunctionCallAssignmentStatement)functionCall2;
        String assignedVariable = scoped(funcAssign.getLeftHandSide().toASTString(), returnEdge.getSuccessor().getFunctionName());

        if(dependingVariables.contains(assignedVariable)) {
          // is this needed here?
          // for test/programs/benchmarks/ldv-regression/rule60_list2.c-unsafe_1.cil.c this is not needed
          // for tracking my_malloc::tmp
          collectedVariables.put(cFunctionSummaryEdge.getSuccessor(), assignedVariable);


          CReturnStatementEdge returnStatementEdge;
          CFAEdge currentEdge = null;
          CFAEdge enteringEdge = returnEdge.getPredecessor().getEnteringEdge(0);

          if(enteringEdge instanceof MultiEdge) {
            for(CFAEdge singleEdge : (MultiEdge)enteringEdge) {
              currentEdge = singleEdge;
            }

            enteringEdge = currentEdge;
          }

          assert(enteringEdge instanceof CReturnStatementEdge);

          returnStatementEdge = (CReturnStatementEdge)enteringEdge;

          collectVariables(returnStatementEdge, returnStatementEdge.getExpression(), collectedVariables);
        }
      }
      break;

    case DeclarationEdge:
      //System.out.println("inspecting edge " + edge.getRawStatement());
      CDeclaration declaration = ((CDeclarationEdge)edge).getDeclaration();
      if(declaration.getName() != null && declaration.isGlobal()) {
        globalVariables.add(declaration.getName());

        if(dependingVariables.contains(declaration.getName()))
          collectedVariables.put(edge.getSuccessor(), declaration.getName());
      }
      break;
    case ReturnStatementEdge:
      if(succ == null)
        break;
      CReturnStatementEdge returnStatementEdge = (CReturnStatementEdge)edge;

      CFunctionReturnEdge returnEdge2 = (CFunctionReturnEdge)succ;

      CFunctionSummaryEdge cFunctionSummaryEdge2 = returnEdge2.getSummaryEdge();

      CFunctionCall functionCall3 = cFunctionSummaryEdge2.getExpression();

      if(functionCall3 instanceof CFunctionCallAssignmentStatement) {
        CFunctionCallAssignmentStatement funcAssign = (CFunctionCallAssignmentStatement)functionCall3;
        String assignedVariable = scoped(funcAssign.getLeftHandSide().toASTString(), succ.getSuccessor().getFunctionName());

        if(dependingVariables.contains(assignedVariable)) {
          collectedVariables.put(cFunctionSummaryEdge2.getSuccessor(), assignedVariable);
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
  private void collectVariables(CFAEdge edge, CRightHandSide rightHandSide, Multimap<CFANode, String> collectedVariables) {
    rightHandSide.accept(new CollectVariablesVisitor(edge, collectedVariables));
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

      switch(op) {
      case AMPER:
      case STAR:
        // we want to know the name of the identifier, if any
        // whether or not it is a pointer (dereference) is not important here
        //collectVariables(pE.getOperand().toASTString());
        pE.getOperand().accept(this);
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
