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
package org.sosy_lab.cpachecker.cfa.blocks.builder;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.blocks.ReferencedVariable;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.util.CFAUtils;


/**
 * Helper class that collects all <code>ReferencedVariable</code>s in a given set of nodes.
 *
 * This is actually some kind of @link{VariableClassification}
 * for a limited set of nodes (all nodes of one BAM-block).
 */
public class ReferencedVariablesCollector {

  // filled as 'last step' during build-process
  final Map<String, ReferencedVariable> collectedVars = new HashMap<>();

  final Set<String> allVars = new HashSet<>();
  final Set<String> varsInConditions = new HashSet<>();

  // needs to be a Multimap, because there could be more than one LHS for a variable, if there are several edges.
  final Multimap<String,String> varsToRHS = HashMultimap.create();

  public ReferencedVariablesCollector(Collection<CFANode> mainNodes) {
    collectVars(mainNodes);
  }

  public Set<ReferencedVariable> getVars() {
    return new HashSet<>(collectedVars.values());
  }

  private void collectVars(Collection<CFANode> nodes) {

    // collect information
    for (CFANode node : nodes) {
      for (CFAEdge leavingEdge : CFAUtils.leavingEdges(node)) {
        if (nodes.contains(leavingEdge.getSuccessor()) || (leavingEdge instanceof CFunctionCallEdge)) {
          collectVars(leavingEdge);
        }
      }
    }

    // create Wrapper-Objects
    for (String var : allVars) {
      final ReferencedVariable ref = new ReferencedVariable(
              var,
              varsInConditions.contains(var),
              new HashSet<ReferencedVariable>() // cross-references filled later
              );
      collectedVars.put(var, ref);
    }

    // build cross-references between variables
    for (ReferencedVariable ref : collectedVars.values()) {
      for (String rhs : varsToRHS.get(ref.getName())) {
        ref.getInfluencingVariables().add(collectedVars.get(rhs));
      }
    }
  }

  private void collectVars(final CFAEdge edge) {

    switch (edge.getEdgeType()) {
      case AssumeEdge: {
        CAssumeEdge assumeEdge = (CAssumeEdge) edge;
        Set<String> vars = collectVars(assumeEdge.getExpression());
        varsInConditions.addAll(vars);
        allVars.addAll(vars);
        break;
      }
      case DeclarationEdge: {
        CDeclaration declaration = ((CDeclarationEdge) edge).getDeclaration();
        String lhsVarName = declaration.getQualifiedName();
        if (declaration instanceof CVariableDeclaration) {
          allVars.add(lhsVarName);
          CInitializer init = ((CVariableDeclaration) declaration).getInitializer();
          if (init instanceof CInitializerExpression) {
            Set<String> vars = collectVars(((CInitializerExpression) init).getExpression());
            varsToRHS.putAll(lhsVarName, vars);
            allVars.addAll(vars);
          }
        }
        break;
      }
      case FunctionCallEdge: {
        CFunctionCallEdge functionCallEdge = (CFunctionCallEdge) edge;
        for (CExpression argument : functionCallEdge.getArguments()) {
          Set<String> vars = collectVars(argument);
          allVars.addAll(vars);
        }
        break;
      }
      case StatementEdge: {
        CStatement statement = ((CStatementEdge) edge).getStatement();
        if (statement instanceof CAssignment) {
          CAssignment assignment = (CAssignment) statement;
          String lhsVarName = getVarname(assignment.getLeftHandSide());
          Set<String> vars = collectVars(assignment.getRightHandSide());
          varsToRHS.putAll(lhsVarName, vars);
          allVars.add(lhsVarName);
          allVars.addAll(vars);
        } else {
          // other statements are considered side-effect free, ignore variable occurrences in them
        }
        break;
      }
      case BlankEdge:
      case CallToReturnEdge:
      case FunctionReturnEdge:
      case ReturnStatementEdge:
        //nothing to do
        break;
      default:
        throw new AssertionError("unhandled type of edge: " + edge.getEdgeType());
    }
  }

  private Set<String> collectVars(CRightHandSide pNode) {
    CollectVariablesVisitor cvv = new CollectVariablesVisitor();
    pNode.accept(cvv);
    return cvv.vars;
  }

  private String getVarname(CLeftHandSide pNode) {
    if (pNode instanceof CIdExpression) {
      return ((CIdExpression) pNode).getDeclaration().getQualifiedName();
    }

    CExpression expr;
    if (pNode instanceof CArraySubscriptExpression) {
      expr = ((CArraySubscriptExpression) pNode).getArrayExpression();
    } else if (pNode instanceof CPointerExpression) {
      expr =  ((CPointerExpression) pNode).getOperand();
    } else {
      // TODO implement retrieval of deeper nested varnames, or use visitor?
      return pNode.toASTString();
    }

    if (expr instanceof CLeftHandSide) {
      return getVarname((CLeftHandSide)expr);
    } else {
      return expr.toASTString();
    }
  }

  private static class CollectVariablesVisitor extends DefaultCExpressionVisitor<Void, RuntimeException>
                                               implements CRightHandSideVisitor<Void, RuntimeException> {

    Set<String> vars = new HashSet<>();

    private void collectVar(String var) {
      vars.add(var);
    }

    @Override
    public Void visit(CIdExpression pE) {
      if (pE.getDeclaration() != null) {
        collectVar(pE.getDeclaration().getQualifiedName());
      }
      return null;
    }

    @Override
    public Void visit(CArraySubscriptExpression pE) {
      collectVar(pE.toASTString()); // TODO do we need this?
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
    public Void visit(CComplexCastExpression pE) {
      pE.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(CFieldReference pE) {
      collectVar(pE.toASTString()); // TODO do we need this?
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
        collectVar(pE.toASTString()); // TODO do we need this?
        //$FALL-THROUGH$
      default:
        pE.getOperand().accept(this);
      }
      return null;
    }

    @Override
    public Void visit(CPointerExpression pE) {
      collectVar(pE.toASTString()); // TODO do we need this?
      pE.getOperand().accept(this);
      return null;
    }

    @Override
    protected Void visitDefault(CExpression pExp) {
      return null;
    }
  }
}
