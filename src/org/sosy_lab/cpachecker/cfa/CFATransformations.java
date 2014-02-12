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
package org.sosy_lab.cpachecker.cfa;

import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializers;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSideVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.parser.eclipse.java.CFAGenerationRuntimeException;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;


/******************************************************************+
 * NullPointerDetection
 *
 * Using detectNullPointers, before every occurence of *p we insert a test on
 * p == 0 in order to detect null pointers.
 */
@Options(prefix="cfa.checkNullPointers")
class CFATransformations {

  @Option(description="Whether to have a single target node per function"
      + " for all invalid null pointer dereferences or to have separate nodes for each dereference")
  private boolean singleTargetPerFunction = true;

  private final LogManager logger;

  CFATransformations(LogManager pLogger, Configuration config) throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;
  }

  public void detectNullPointers(final MutableCFA cfa) throws CParserException {

    CBinaryExpressionBuilder binBuilder = new CBinaryExpressionBuilder(cfa.getMachineModel(), logger);

    for (final String function : cfa.getAllFunctionNames()) {

      // This supplier creates the appropriate target nodes that get added
      // to the CFA for the case the dereference fails.
      Supplier<CFANode> targetNodeSupplier = new Supplier<CFANode>() {
        @Override
        public CFANode get() {

          CFANode startNode = new CFANode(0, function);
          CFANode endNode = new CFANode(0, function);
          BlankEdge endEdge = new BlankEdge("null-deref", 0, startNode, endNode, "null-deref");
          CFACreationUtils.addEdgeUnconditionallyToCFA(endEdge);

          BlankEdge loopEdge = new BlankEdge("", 0, endNode, endNode, "");
          CFACreationUtils.addEdgeUnconditionallyToCFA(loopEdge);

          cfa.addNode(startNode);
          cfa.addNode(endNode);
          return startNode;
        }
      };

      if (singleTargetPerFunction) {
        // Only a single target node per function,
        // memoize the first created one and reuse it
        targetNodeSupplier = Suppliers.memoize(targetNodeSupplier);
      }

      for (CFANode node : ImmutableList.copyOf(cfa.getFunctionNodes(function))) {
        switch (node.getNumLeavingEdges()) {
        case 0:
          break;
        case 1:
          handleEdge(node.getLeavingEdge(0), cfa, targetNodeSupplier, binBuilder);
          break;
        case 2:
          if (node.getLeavingEdge(0) instanceof AssumeEdge
              && node.getLeavingEdge(1) instanceof AssumeEdge) {
            // handle only one edge, both contain the same expression
            handleEdge(node.getLeavingEdge(0), cfa, targetNodeSupplier, binBuilder);
          } else {
            handleEdge(node.getLeavingEdge(0), cfa, targetNodeSupplier, binBuilder);
            handleEdge(node.getLeavingEdge(1), cfa, targetNodeSupplier, binBuilder);
          }
          break;
        default:
          throw new CFAGenerationRuntimeException("Too much leaving Edges on CFANode");
        }
      }
    }
  }

  private static void handleEdge(CFAEdge edge, MutableCFA cfa, Supplier<CFANode> targetNode, CBinaryExpressionBuilder builder) throws CParserException {
    ContainsPointerVisitor visitor = new ContainsPointerVisitor();
    if (edge instanceof CReturnStatementEdge) {
      CExpression returnExp = ((CReturnStatementEdge)edge).getExpression();
      if (returnExp != null) {
        returnExp.accept(visitor);
      }
    } else if (edge instanceof CStatementEdge) {
      CStatement stmt = ((CStatementEdge)edge).getStatement();
      if (stmt instanceof CFunctionCallStatement) {
        ((CFunctionCallStatement)stmt).getFunctionCallExpression().accept(visitor);
      } else if (stmt instanceof CExpressionStatement) {
        ((CExpressionStatement)stmt).getExpression().accept(visitor);
      } else if (stmt instanceof CAssignment) {
        ((CAssignment)stmt).getRightHandSide().accept(visitor);
        ((CAssignment)stmt).getLeftHandSide().accept(visitor);
      }
    } else if (edge instanceof CDeclarationEdge) {
      CDeclaration decl = ((CDeclarationEdge)edge).getDeclaration();
      if (!decl.isGlobal() && decl instanceof CVariableDeclaration) {
        try {
          for (CAssignment assignment : CInitializers.convertToAssignments((CVariableDeclaration)decl, edge)) {
            // left-hand side can be ignored (it is the currently declared variable
            assignment.getRightHandSide().accept(visitor);
          }
        } catch (UnrecognizedCCodeException e) {
          throw new CParserException(e);
        }
      }
    } else if (edge instanceof CAssumeEdge) {
      ((CAssumeEdge)edge).getExpression().accept(visitor);
    }

    for (CExpression exp : Lists.reverse(visitor.dereferencedExpressions)) {
      edge = insertNullPointerCheck(edge, exp, cfa, targetNode, builder);
    }
  }

  private static CFAEdge insertNullPointerCheck(CFAEdge edge, CExpression exp, MutableCFA cfa, Supplier<CFANode> targetNode, CBinaryExpressionBuilder binBuilder) {
    CFANode predecessor = edge.getPredecessor();
    CFANode successor = edge.getSuccessor();
    CFACreationUtils.removeEdgeFromNodes(edge);

    CFANode falseNode = new CFANode(edge.getLineNumber(), predecessor.getFunctionName());

    for (CFAEdge otherEdge : leavingEdges(predecessor).toList()) {
      CFAEdge newEdge = createOldEdgeWithNewNodes(falseNode, otherEdge.getSuccessor(), otherEdge);
      CFACreationUtils.removeEdgeFromNodes(otherEdge);
      CFACreationUtils.addEdgeUnconditionallyToCFA(newEdge);
    }

    CBinaryExpression assumeExpression = binBuilder.buildBinaryExpression(exp, new CIntegerLiteralExpression(exp.getFileLocation(), CNumericTypes.INT,BigInteger.valueOf(0)), BinaryOperator.EQUALS);
    AssumeEdge trueEdge = new CAssumeEdge(edge.getRawStatement(),
                                         edge.getLineNumber(),
                                         predecessor, targetNode.get(),
                                         assumeExpression,
                                         true);
    AssumeEdge falseEdge = new CAssumeEdge(edge.getRawStatement(),
                                           edge.getLineNumber(),
                                           predecessor, falseNode,
                                           assumeExpression,
                                           false);

    CFACreationUtils.addEdgeUnconditionallyToCFA(trueEdge);
    CFACreationUtils.addEdgeUnconditionallyToCFA(falseEdge);

    CFAEdge newEdge = createOldEdgeWithNewNodes(falseNode, successor, edge);
    CFACreationUtils.addEdgeUnconditionallyToCFA(newEdge);

    cfa.addNode(falseNode);

    return newEdge;
  }

  private static CFAEdge createOldEdgeWithNewNodes(CFANode predecessor, CFANode successor, CFAEdge edge) {
    switch (edge.getEdgeType()) {
    case AssumeEdge:
      return new CAssumeEdge(edge.getRawStatement(), edge.getLineNumber(),
                             predecessor, successor, ((CAssumeEdge)edge).getExpression(),
                             ((CAssumeEdge)edge).getTruthAssumption());
    case CallToReturnEdge:
      assert(false);
      break;
    case ReturnStatementEdge:
      return new CReturnStatementEdge(edge.getRawStatement(),
                                      ((CReturnStatementEdge)edge).getRawAST().get(),
                                      edge.getLineNumber(), predecessor,
                                      ((CReturnStatementEdge)edge).getSuccessor());
    case StatementEdge:
      return new CStatementEdge(edge.getRawStatement(), ((CStatementEdge)edge).getStatement(),
                                edge.getLineNumber(), predecessor, successor);
    case DeclarationEdge:
      return new CDeclarationEdge(edge.getRawStatement(), edge.getLineNumber(),
                                  predecessor, successor,
                                  ((CDeclarationEdge)edge).getDeclaration());
    }
    throw new CFAGenerationRuntimeException("more edge types valid than expected, more work to do here");
  }


  /**
   * This visitor returns all Expressions where a Pointer is included
   */
  static class ContainsPointerVisitor extends DefaultCExpressionVisitor<Void, CFAGenerationRuntimeException>
                                      implements CRightHandSideVisitor<Void, CFAGenerationRuntimeException> {

    private final List<CExpression> dereferencedExpressions = new ArrayList<>();

    @Override
    public Void visit(CFunctionCallExpression pIastFunctionCallExpression) {
      pIastFunctionCallExpression.getFunctionNameExpression().accept(this);
      for (CExpression param : pIastFunctionCallExpression.getParameterExpressions()) {
        param.accept(this);
      }
      return null;
    }

    @Override
    public Void visit(CArraySubscriptExpression e) {
      e.getArrayExpression().accept(this);
      e.getSubscriptExpression().accept(this);
      return null;
    }
    @Override
    public Void visit(CCastExpression e) {
      return e.getOperand().accept(this);
    }

    @Override
    public Void visit(CComplexCastExpression e) {
      return e.getOperand().accept(this);
    }

    @Override
    public Void visit(CFieldReference e) {
      if (e.isPointerDereference()) {
        dereferencedExpressions.add(e.getFieldOwner());
      }
      return null;
    }

    @Override
    public Void visit(CUnaryExpression e) {
      if (e.getOperator() == UnaryOperator.SIZEOF) {
        // We do not want cases like sizeof(*p)
        return null;
      }
      if (e.getOperator() == UnaryOperator.AMPER) {
        if (e.getOperand() instanceof CFieldReference
            && ((CFieldReference)e.getOperand()).isPointerDereference()) {
          // &(s->f)
          // ignore this dereference and visit "s"
          return ((CFieldReference)e.getOperand()).getFieldOwner().accept(this);
        }
      }
      return e.getOperand().accept(this);
    }

    @Override
    public Void visit(CPointerExpression e) {
      dereferencedExpressions.add(e.getOperand());
      e.getOperand().accept(this);
      return null;
    }

    @Override
    public Void visit(CBinaryExpression pIastbBinaryExpression) {
      pIastbBinaryExpression.getOperand1().accept(this);
      pIastbBinaryExpression.getOperand2().accept(this);
      return null;
    }

    @Override
    protected Void visitDefault(CExpression pExp) throws CFAGenerationRuntimeException {
      return null;
    }

  }
}