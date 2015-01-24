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

import static com.google.common.collect.FluentIterable.from;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.sosy_lab.common.Pair;
import org.sosy_lab.cpachecker.cfa.ast.AVariableDeclaration;
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
import org.sosy_lab.cpachecker.cpa.arg.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.MutableARGPath;
import org.sosy_lab.cpachecker.cpa.value.ValueAnalysisState.MemoryLocation;

import com.google.common.base.Optional;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Helper class that collects the set of variables on which all assume edges in the given path depend on (i.e. the transitive closure).
 */
 public class AssumptionUseDefinitionCollector {

  /**
   * the set of variables in the transitive-closure of the assume edges
   */
  private final Set<String> collectedVariables = new HashSet<>();

  /**
   * the set of variables for which to find the referencing ones
   */
  private final Set<String> dependingVariables = new HashSet<>();


  private HashMultimap<ARGState, MemoryLocation> fakeInterpolants = HashMultimap.create();

  /**
   * after the assumption closure has been determined, this value states at which offset all dependencies are resolved
   */
  private int dependenciesResolvedOffset = 0;

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

    for (int i = path.size() - 1; i >= 0; i--) {
      CFAEdge edge = path.get(i);
      collectVariables(null, edge, collectedVariables);

      if(Iterables.getLast(path).getEdgeType() == CFAEdgeType.AssumeEdge
          && dependingVariables.isEmpty()) {
        dependenciesResolvedOffset = i;
        return collectedVariables;
      }
    }

    // for full paths, the set of depending variables always has be empty at this point,
    // but sometimes, the use-def information is derived from incomplete paths,
    // and for those it can happen that not all depending variables are consumed
    // disabled again, because handling for pointers is incomplete
    // assert dependingVariables.size() == 0 || isIncompletePath(path);

    // add the remaining depending variables to the set of collectedVariables
    collectedVariables.addAll(dependingVariables);

    return collectedVariables;
  }

  /**
   * This method collects the respective referenced variables in the given path.
   *
   * @param path the path to analyze
   * @return the mapping of location to referenced variables in the given path
   */
  public Multimap<ARGState, MemoryLocation> obtainFakeInterpolants(ARGPath path) {

    dependingVariables.clear();
    collectedVariables.clear();

    fakeInterpolants.clear();

    List<CFAEdge> edgesList = path.asEdgesList();
    List<ARGState> statesList = path.asStatesList();

    int i = path.size() - 1;

    for (; i >= 0; i--) {
      collectVariables(statesList.get(i), edgesList.get(i), collectedVariables);

      if(Iterables.getLast(edgesList).getEdgeType() == CFAEdgeType.AssumeEdge
          && dependingVariables.isEmpty()) {
        dependenciesResolvedOffset = i;
        return fakeInterpolants;
      }
    }

    return fakeInterpolants;
  }

  /**
   * This method collects the respective referenced variables in the given ARG path.
   *
   * @param path the path to analyze
   * @return the mapping of location to referenced variables in the given path
   */
  public Set<String> obtainUseDefInformation(MutableARGPath pFullArgPath) {
    return obtainUseDefInformation(from(pFullArgPath).transform(Pair.<CFAEdge>getProjectionToSecond()).toList());
  }

  public int getDependenciesResolvedOffset() {
    return dependenciesResolvedOffset;
  }

  /**
   * This method determines if the given path is only a suffix of a complete path.
   *
   * @param path the path to check
   * @return true, if the path is incomplete, else false
   */
  @SuppressWarnings("unused")
  private boolean isIncompletePath(List<CFAEdge> path) {
    return path.get(0).getPredecessor().getNumEnteringEdges() > 0;
  }

  /**
   * This method collects the referenced variables in a edge into the mapping of collected variables.
   *
   * @param edge the edge to analyze
   * @param collectedVariables the mapping of collected variables
   */
  private void collectVariables(ARGState state, CFAEdge edge, Set<String> collectedVariables) {

    switch (edge.getEdgeType()) {
    case BlankEdge:
    case CallToReturnEdge:
      //nothing to do
      break;

    case FunctionReturnEdge:
      previousFunctionReturnEdge = (FunctionReturnEdge)edge;
      fakeInterpolants.put(state, MemoryLocation.valueOf(edge.getPredecessor().getFunctionName() + "::__retval__"));
      break;

    case DeclarationEdge:
      CDeclaration declaration = ((CDeclarationEdge)edge).getDeclaration();

      if(declaration instanceof CVariableDeclaration) {
        if (declaration.getName() != null) {
          String variableName = declaration.getQualifiedName();

          if (dependingVariables.contains(variableName)) {
            dependingVariables.remove(variableName);
            collectedVariables.add(variableName);

            if (((CVariableDeclaration)declaration).getInitializer() instanceof CInitializerExpression) {
              CInitializerExpression initializer = ((CInitializerExpression)((CVariableDeclaration)declaration).getInitializer());
              if (initializer != null) {
                collectVariables(edge, initializer.getExpression());
              }
            }
          }
        }
      }
      break;

    case ReturnStatementEdge:
        CReturnStatementEdge returnStatementEdge = (CReturnStatementEdge)edge;

        // for cases where error path ends with a return statement
        if (previousFunctionReturnEdge == null) {
          break;
        }

        CFunctionSummaryEdge cFunctionSummaryEdge = (CFunctionSummaryEdge)previousFunctionReturnEdge.getSummaryEdge();

        CFunctionCall functionCall = cFunctionSummaryEdge.getExpression();

        if (functionCall instanceof CFunctionCallAssignmentStatement) {
          CFunctionCallAssignmentStatement funcAssign = (CFunctionCallAssignmentStatement)functionCall;

          if(funcAssign.getLeftHandSide() instanceof CIdExpression) {
            String assignedVariable = ((CIdExpression)(funcAssign.getLeftHandSide())).getDeclaration().getQualifiedName();

            if (dependingVariables.contains(assignedVariable)) {
              dependingVariables.remove(assignedVariable);

              collectedVariables.add(assignedVariable);

              // also add special function return variable as relevant variable
              Optional<? extends AVariableDeclaration> returnVarName = returnStatementEdge.
                  getSuccessor().getEntryNode().getReturnVariable();
              if(returnVarName.isPresent()) {
                collectedVariables.add(returnVarName.get().getQualifiedName());
                fakeInterpolants.put(state, MemoryLocation.valueOf(returnVarName.get().getQualifiedName()));
              }

              if (returnStatementEdge.getExpression().isPresent()) {
                collectVariables(returnStatementEdge, returnStatementEdge.getExpression().get());
              }
            }
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
      handleAssumption(edge);
      break;

    case StatementEdge:
      CStatementEdge statementEdge = (CStatementEdge)edge;
      if (statementEdge.getStatement() instanceof CAssignment) {
        CAssignment assignment = (CAssignment)statementEdge.getStatement();

        if(assignment.getLeftHandSide() instanceof CIdExpression) {
          String assignedVariable = ((CIdExpression)(assignment.getLeftHandSide())).getDeclaration().getQualifiedName();

          if (dependingVariables.contains(assignedVariable)) {
            dependingVariables.remove(assignedVariable);
            collectedVariables.add(assignedVariable);
            collectVariables(statementEdge, assignment.getRightHandSide());
          }
        }
      }
      break;

    case MultiEdge:
      List<CFAEdge> edges = ((MultiEdge)edge).getEdges();

      // process MultiEdges also in reverse order
      for (int i = edges.size() - 1; i >= 0; i--) {
        collectVariables(state, edges.get(i), collectedVariables);
      }
      break;
    }

    for(String var : dependingVariables) {
      fakeInterpolants.put(state, MemoryLocation.valueOf(var));
    }
  }

  /**
   * This method collects variables from a given assume edge.
   *
   * @param edge the assume edge from which to collect variables
   */
  protected void handleAssumption(CFAEdge edge) {
    CAssumeEdge assumeEdge = (CAssumeEdge)edge;
    collectVariables(assumeEdge, assumeEdge.getExpression());
  }

  /**
   * This method delegates the collecting job to the CollectVariablesVisitor.
   *
   * @param edge the edge to analyze
   * @param rightHandSide the right hand side of the assignment
   */
  protected void collectVariables(CFAEdge edge, CRightHandSide rightHandSide) {
    rightHandSide.accept(new CollectVariablesVisitor(edge));
  }

  /**
   * the visitor responsible for the actual collecting job
   */
  private class CollectVariablesVisitor extends DefaultCExpressionVisitor<Void, RuntimeException>
                                               implements CRightHandSideVisitor<Void, RuntimeException> {

    /**
     * This method acts as the constructor of the class.
     *
     * @param currentEdge the assignment edge to analyze
     * @param collectedVariables the mapping of locations to variable names up to the current edge
     */
    public CollectVariablesVisitor(CFAEdge currentEdge) {
    }

    private void collectVariables(String variableName) {
      dependingVariables.add(variableName);
    }

    @Override
    public Void visit(CIdExpression pE) {
      collectVariables(pE.getDeclaration().getQualifiedName());

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
    @SuppressFBWarnings(value = "SF_SWITCH_NO_DEFAULT", justification = "bug in FindBugs")
    public Void visit(CUnaryExpression pE) {
      UnaryOperator op = pE.getOperator();

      switch (op) {
      case AMPER:
        collectVariables(pE.toASTString());
        //$FALL-THROUGH$
      default:
        pE.getOperand().accept(this);
        break;
      }

      return null;
    }

    @Override
    protected Void visitDefault(CExpression pExp) {
      return null;
    }
  }
}
