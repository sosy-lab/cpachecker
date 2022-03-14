// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.postprocessing.function;

import static org.sosy_lab.cpachecker.util.CFAUtils.leavingEdges;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFACreationUtils;
import org.sosy_lab.cpachecker.cfa.MutableCFA;
import org.sosy_lab.cpachecker.cfa.ast.AFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
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
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.exceptions.CParserException;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/******************************************************************+
 * NullPointerDetection
 *
 * Using detectNullPointers, before every occurrence of *p we insert a test on
 * p == 0 in order to detect null pointers.
 */
@Options(prefix = "cfa.checkNullPointers")
public class NullPointerChecks {

  @Option(
      secure = true,
      description =
          "Whether to have a single target node per function for all invalid null pointer"
              + " dereferences or to have separate nodes for each dereference")
  private boolean singleTargetPerFunction = true;

  private final LogManager logger;

  public NullPointerChecks(LogManager pLogger, Configuration config)
      throws InvalidConfigurationException {
    config.inject(this);
    logger = pLogger;
  }

  public void addNullPointerChecks(final MutableCFA cfa) throws CParserException {

    CBinaryExpressionBuilder binBuilder =
        new CBinaryExpressionBuilder(cfa.getMachineModel(), logger);

    for (final String functionName : cfa.getAllFunctionNames()) {

      // This supplier creates the appropriate target nodes that get added
      // to the CFA for the case the dereference fails.
      Supplier<CFANode> targetNodeSupplier =
          new Supplier<>() {
            @Override
            public CFANode get() {
              AFunctionDeclaration function =
                  cfa.getFunctionHead(functionName).getFunctionDefinition();
              CFANode startNode = new CFANode(function);
              CFANode endNode = new CFANode(function);
              BlankEdge endEdge =
                  new BlankEdge("null-deref", FileLocation.DUMMY, startNode, endNode, "null-deref");
              CFACreationUtils.addEdgeUnconditionallyToCFA(endEdge);

              BlankEdge loopEdge = new BlankEdge("", FileLocation.DUMMY, endNode, endNode, "");
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

      for (CFANode node : ImmutableList.copyOf(cfa.getFunctionNodes(functionName))) {
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
            throw new AssertionError("Too many leaving edges on CFANode");
        }
      }
    }
  }

  private static void handleEdge(
      CFAEdge edge, MutableCFA cfa, Supplier<CFANode> targetNode, CBinaryExpressionBuilder builder)
      throws CParserException {
    ContainsPointerVisitor visitor = new ContainsPointerVisitor();
    if (edge instanceof CReturnStatementEdge) {
      Optional<CExpression> returnExp = ((CReturnStatementEdge) edge).getExpression();
      if (returnExp.isPresent()) {
        returnExp.orElseThrow().accept(visitor);
      }
    } else if (edge instanceof CStatementEdge) {
      CStatement stmt = ((CStatementEdge) edge).getStatement();
      if (stmt instanceof CFunctionCallStatement) {
        ((CFunctionCallStatement) stmt).getFunctionCallExpression().accept(visitor);
      } else if (stmt instanceof CExpressionStatement) {
        ((CExpressionStatement) stmt).getExpression().accept(visitor);
      } else if (stmt instanceof CAssignment) {
        ((CAssignment) stmt).getRightHandSide().accept(visitor);
        ((CAssignment) stmt).getLeftHandSide().accept(visitor);
      }
    } else if (edge instanceof CDeclarationEdge) {
      CDeclaration decl = ((CDeclarationEdge) edge).getDeclaration();
      if (!decl.isGlobal() && decl instanceof CVariableDeclaration) {
        try {
          for (CAssignment assignment :
              CInitializers.convertToAssignments((CVariableDeclaration) decl, edge)) {
            // left-hand side can be ignored (it is the currently declared variable
            assignment.getRightHandSide().accept(visitor);
          }
        } catch (UnrecognizedCodeException e) {
          throw new CParserException(e);
        }
      }
    } else if (edge instanceof CAssumeEdge) {
      ((CAssumeEdge) edge).getExpression().accept(visitor);
    }

    for (CExpression exp : Lists.reverse(visitor.dereferencedExpressions)) {
      edge = insertNullPointerCheck(edge, exp, cfa, targetNode, builder);
    }
  }

  private static CFAEdge insertNullPointerCheck(
      CFAEdge edge,
      CExpression exp,
      MutableCFA cfa,
      Supplier<CFANode> targetNode,
      CBinaryExpressionBuilder binBuilder) {
    CFANode predecessor = edge.getPredecessor();
    CFANode successor = edge.getSuccessor();
    CFACreationUtils.removeEdgeFromNodes(edge);

    CFANode falseNode = new CFANode(predecessor.getFunction());

    for (CFAEdge otherEdge : leavingEdges(predecessor).toList()) {
      CFAEdge newEdge = createOldEdgeWithNewNodes(falseNode, otherEdge.getSuccessor(), otherEdge);
      CFACreationUtils.removeEdgeFromNodes(otherEdge);
      CFACreationUtils.addEdgeUnconditionallyToCFA(newEdge);
    }

    CBinaryExpression assumeExpression =
        binBuilder.buildBinaryExpressionUnchecked(
            exp,
            new CIntegerLiteralExpression(
                exp.getFileLocation(), CNumericTypes.INT, BigInteger.valueOf(0)),
            BinaryOperator.EQUALS);
    AssumeEdge trueEdge =
        new CAssumeEdge(
            edge.getRawStatement(),
            edge.getFileLocation(),
            predecessor,
            targetNode.get(),
            assumeExpression,
            true);
    AssumeEdge falseEdge =
        new CAssumeEdge(
            edge.getRawStatement(),
            edge.getFileLocation(),
            predecessor,
            falseNode,
            assumeExpression,
            false);

    CFACreationUtils.addEdgeUnconditionallyToCFA(trueEdge);
    CFACreationUtils.addEdgeUnconditionallyToCFA(falseEdge);

    CFAEdge newEdge = createOldEdgeWithNewNodes(falseNode, successor, edge);
    CFACreationUtils.addEdgeUnconditionallyToCFA(newEdge);

    cfa.addNode(falseNode);

    return newEdge;
  }

  private static CFAEdge createOldEdgeWithNewNodes(
      CFANode predecessor, CFANode successor, CFAEdge edge) {
    switch (edge.getEdgeType()) {
      case AssumeEdge:
        return new CAssumeEdge(
            edge.getRawStatement(),
            edge.getFileLocation(),
            predecessor,
            successor,
            ((CAssumeEdge) edge).getExpression(),
            ((CAssumeEdge) edge).getTruthAssumption(),
            ((CAssumeEdge) edge).isSwapped(),
            ((CAssumeEdge) edge).isArtificialIntermediate());
      case ReturnStatementEdge:
        return new CReturnStatementEdge(
            edge.getRawStatement(),
            ((CReturnStatementEdge) edge).getReturnStatement(),
            edge.getFileLocation(),
            predecessor,
            ((CReturnStatementEdge) edge).getSuccessor());
      case StatementEdge:
        return new CStatementEdge(
            edge.getRawStatement(),
            ((CStatementEdge) edge).getStatement(),
            edge.getFileLocation(),
            predecessor,
            successor);
      case DeclarationEdge:
        return new CDeclarationEdge(
            edge.getRawStatement(),
            edge.getFileLocation(),
            predecessor,
            successor,
            ((CDeclarationEdge) edge).getDeclaration());
      case CallToReturnEdge:
        throw new AssertionError();
      default:
        throw new AssertionError("more edge types valid than expected, more work to do here");
    }
  }

  /** This visitor returns all Expressions where a Pointer is included */
  static class ContainsPointerVisitor extends DefaultCExpressionVisitor<Void, NoException>
      implements CRightHandSideVisitor<Void, NoException> {

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
            && ((CFieldReference) e.getOperand()).isPointerDereference()) {
          // &(s->f)
          // ignore this dereference and visit "s"
          return ((CFieldReference) e.getOperand()).getFieldOwner().accept(this);
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
    protected Void visitDefault(CExpression pExp) {
      return null;
    }
  }
}
