/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2011  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Vector;

import org.sosy_lab.common.Triple;
import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.ast.IASTSimpleDeclSpecifier;
import org.sosy_lab.cpachecker.cfa.ast.IASTUnaryExpression;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAFunctionDefinitionNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.cfa.objectmodel.c.DeclarationEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;


public class RGCFA extends CFA {

  private final Multimap<CFANode, String> rhsVariables;
  private final Multimap<CFANode, String> lhsVariables;
  private final Set<String> scopedLocalVars;
  private final List<CFANode> noEnvList;
  protected final CFANode executionStartNode;
  protected final CFANode startNode;


  public RGCFA(CFA other, CFANode startNode, CFAFunctionDefinitionNode mainFunction, int tid) throws UnrecognizedCFAEdgeException{
    super(other);

    rhsVariables = HashMultimap.create();
    lhsVariables = HashMultimap.create();
    scopedLocalVars = new HashSet<String>();
    noEnvList = new Vector<CFANode>();
    for (CFANode node : other.cfaNodes.values()){
      for (int i=0; i<node.getNumLeavingEdges(); i++) {
        CFAEdge edge = node.getLeavingEdge(i);
        if (edge.getEdgeType() == CFAEdgeType.DeclarationEdge) {
          DeclarationEdge de = (DeclarationEdge) edge;
          if (!de.isGlobal() && de.getDeclSpecifier() instanceof IASTSimpleDeclSpecifier){
            // TODO use some method
            String function = edge.getPredecessor().getFunctionName();
            String name = function+"::"+de.getName();
            scopedLocalVars.add(name);
          }
        }
        if (edge.getEdgeType() == CFAEdgeType.StatementEdge || edge.getEdgeType() == CFAEdgeType.AssumeEdge ) {
          Triple<Set<String>, Set<String>, Boolean> vars = getReadWriteVariables(edge.getRawAST());
          lhsVariables.putAll(node, vars.getFirst());
          rhsVariables.putAll(node, vars.getSecond());
        }
      }
    }
    // find the inital node
    this.executionStartNode = startNode;
    this.startNode = mainFunction;

  }


  private Triple<Set<String>, Set<String>, Boolean> getReadWriteVariables(IASTNode exp) throws UnrecognizedCFAEdgeException {
    Vector<IASTExpression> toProcess = new Vector<IASTExpression>();
    Set<String> rhsVars = new HashSet<String>();
    Set<String> lhsVars = new HashSet<String>();

    if (exp instanceof IASTExpressionAssignmentStatement){
      IASTExpressionAssignmentStatement e = (IASTExpressionAssignmentStatement) exp;
      IASTExpression rhs = e.getRightHandSide();
      IASTExpression lhs = e.getLeftHandSide();
      String lhsVar = ((IASTIdExpression) lhs).getName();
      lhsVars.add(lhsVar);
      toProcess.add(rhs);
    } else if (exp instanceof IASTBinaryExpression) {
      toProcess.add((IASTBinaryExpression)exp);
    }

    while(! toProcess.isEmpty()){
      IASTExpression e = toProcess.remove(0);
      if (e instanceof IASTIdExpression){
        String var = ((IASTIdExpression) e).getName();
        rhsVars.add(var);
      }
      else if (e instanceof IASTBinaryExpression){
        IASTBinaryExpression bin = (IASTBinaryExpression) e;
        toProcess.add(bin.getOperand1());
        toProcess.add(bin.getOperand2());
      } else if (e instanceof IASTUnaryExpression){
        IASTUnaryExpression unary = (IASTUnaryExpression) e;
        toProcess.add(unary.getOperand());
      }
      else if (e instanceof IASTIntegerLiteralExpression){}
      else if (e instanceof IASTCastExpression){
        IASTCastExpression cast = (IASTCastExpression) e;
        toProcess.add(cast.getOperand());
      }
      else {
        throw new UnrecognizedCFAEdgeException("Unrecognized AST type: "+e.getClass());
      }
    }

    return Triple.of(lhsVars, rhsVars, Boolean.FALSE);
  }


  public Multimap<CFANode, String> getRhsVariables() {
    return rhsVariables;
  }

  public Multimap<CFANode, String> getLhsVariables() {
    return lhsVariables;
  }

  public Set<String> getScopedLocalVars() {
    return scopedLocalVars;
  }


  public  SortedSet<String> getGlobalVariables() {
    SortedSet<String> gvars = new TreeSet<String>();

    for (IASTDeclaration gd : getGlobalDeclarations()){
      if (gd.getDeclSpecifier() != null && gd.getDeclSpecifier() instanceof IASTSimpleDeclSpecifier){
        gvars.add(gd.getName());
      }
    }

    return gvars;
  }


  public CFANode getExecutionStartNode() {
    return executionStartNode;
  }


  public CFANode getStartNode() {
    return startNode;
  }



}
