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
import java.util.Set;
import java.util.Vector;

import org.sosy_lab.cpachecker.cfa.ast.IASTBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.IASTIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.IASTNode;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdge;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.objectmodel.CFANode;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCFAEdgeException;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;


public class RelyGuaranteeCFA extends CFA {

  private final Multimap<CFANode, String> rhsVariables;

  /*public RelyGuaranteeCFA(Map<String, CFAFunctionDefinitionNode> pFunctions, SortedSetMultimap<String, CFANode> pCfaNodes, List<IASTDeclaration> pGlobalDeclarations) {
    super(pFunctions, pCfaNodes, pGlobalDeclarations);
    originalNodes = TreeMultimap.create(pCfaNodes);
  }*/

  public RelyGuaranteeCFA(CFA other) throws UnrecognizedCFAEdgeException{
    super(other);

    rhsVariables = HashMultimap.create();
    for (CFANode node : other.cfaNodes.values()){
      for (int i=0; i<node.getNumLeavingEdges(); i++) {
        CFAEdge edge = node.getLeavingEdge(i);
        if (edge.getEdgeType() == CFAEdgeType.StatementEdge || edge.getEdgeType() == CFAEdgeType.AssumeEdge ) {
          Set<String> vars = getReadVariables(edge.getRawAST());
          rhsVariables.putAll(node, vars);
        }
      }
    }

  }


  private Set<String> getReadVariables(IASTNode exp) throws UnrecognizedCFAEdgeException {
    Vector<IASTExpression> toProcess = new Vector<IASTExpression>();
    Set<String> result = new HashSet<String>();
    if (exp instanceof IASTExpressionAssignmentStatement){
      IASTExpression rhs = ((IASTExpressionAssignmentStatement) exp).getRightHandSide();
      toProcess.add(rhs);
    } else if (exp instanceof IASTBinaryExpression) {
      toProcess.add((IASTBinaryExpression)exp);
    }

    while(! toProcess.isEmpty()){
      IASTExpression e = toProcess.remove(0);
      if (e instanceof IASTIdExpression){
        String var = ((IASTIdExpression) e).getName();
        result.add(var);
      }
      else if (e instanceof IASTBinaryExpression){
        IASTBinaryExpression bin = (IASTBinaryExpression) e;
        toProcess.add(bin.getOperand1());
        toProcess.add(bin.getOperand2());
      }
      else if (e instanceof IASTIntegerLiteralExpression){}
      else {
        throw new UnrecognizedCFAEdgeException("Unrecognized AST type: "+e.getClass());
      }
    }

    return result;
  }

  public Multimap<CFANode, String> getRhsVariables() {
    return rhsVariables;
  }


}
