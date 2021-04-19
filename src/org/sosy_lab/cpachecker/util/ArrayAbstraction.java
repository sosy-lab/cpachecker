// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.CFATerminationNode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CLabelNode;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

public final class ArrayAbstraction {

  private ArrayAbstraction() {}

  private static MemoryLocation getMemoryLocation(CIdExpression pCIdExpression) {

    String qualifiedName = pCIdExpression.getDeclaration().getQualifiedName();

    return MemoryLocation.valueOf(qualifiedName);
  }

  private static CIdExpression createCIdExpression(String pName, CType pType) {

    CVariableDeclaration variableDeclaration =
        new CVariableDeclaration(
            FileLocation.DUMMY, true, CStorageClass.AUTO, pType, pName, pName, pName, null);

    return new CIdExpression(FileLocation.DUMMY, variableDeclaration);
  }

  private static CIdExpression createVariableCIdExpression(TransformableArray pTransformableArray) {
    String name =
        "__array_witness_variable_" + pTransformableArray.getMemoryLocation().getIdentifier();
    return createCIdExpression(name, pTransformableArray.getArrayType().getType());
  }

  private static CIdExpression createIndexCIdExpression(TransformableArray pTransformableArray) {
    String name =
        "__array_witness_index_" + pTransformableArray.getMemoryLocation().getIdentifier();
    return createCIdExpression(
        name,
        new CSimpleType(
            true, false, CBasicType.UNSPECIFIED, false, false, false, true, false, false, true));
  }

  private static CFAEdge createAssignEdge(CIdExpression pLhs, CIdExpression pRhs) {

    CExpressionAssignmentStatement assignmentStatement =
        new CExpressionAssignmentStatement(FileLocation.DUMMY, pLhs, pRhs);

    return new CStatementEdge(
        "",
        assignmentStatement,
        FileLocation.DUMMY,
        CFANode.newDummyCFANode("dummy-predecessor"),
        CFANode.newDummyCFANode("dummy-successor"));
  }

  private static CFAEdge createNondetValueAssignEdge(
      CIdExpression pExpression, VariableNameGenerator pVariableNameGenerator) {

    CIdExpression nondetVariable =
        createCIdExpression(pVariableNameGenerator.next(), pExpression.getExpressionType());
    CExpressionAssignmentStatement assignmentStatement =
        new CExpressionAssignmentStatement(FileLocation.DUMMY, pExpression, nondetVariable);

    return new CStatementEdge(
        "",
        assignmentStatement,
        FileLocation.DUMMY,
        CFANode.newDummyCFANode("dummy-predecessor"),
        CFANode.newDummyCFANode("dummy-successor"));
  }

  private static ImmutableSet<TransformableArray> getTransformableArrays(CFA pCfa) {

    ImmutableSet.Builder<TransformableArray> transformableArraysBuilder = ImmutableSet.builder();

    for (CFANode node : pCfa.getAllNodes()) {
      for (CFAEdge edge : CFAUtils.allLeavingEdges(node)) {
        if (edge instanceof CDeclarationEdge) {
          CDeclaration declaration = ((CDeclarationEdge) edge).getDeclaration();
          CType type = declaration.getType();
          if (declaration instanceof CVariableDeclaration && type instanceof CArrayType) {
            MemoryLocation memoryLocation = MemoryLocation.valueOf(declaration.getQualifiedName());
            CArrayType arrayType = (CArrayType) type;
            transformableArraysBuilder.add(new TransformableArray(memoryLocation, arrayType));
          }
        }
      }
    }

    return transformableArraysBuilder.build();
  }

  private static ImmutableList<CExpression> getConditionExpressions(
      ImmutableSet<TransformableArray> pTransformableArrays, TransformableLoop pTransformableLoop) {

    CBinaryExpression loopCondition =
        (CBinaryExpression) pTransformableLoop.getEnterLoopCfaEdge().getExpression();

    CExpressionAssignmentStatement initStatement =
        (CExpressionAssignmentStatement)
            pTransformableLoop.getInitLoopIndexCfaEdge().getStatement();
    CExpressionAssignmentStatement updateStatement =
        (CExpressionAssignmentStatement)
            pTransformableLoop.getUpdateLoopIndexCfaEdge().getStatement();
    CBinaryExpression.BinaryOperator updateOperator =
        ((CBinaryExpression) updateStatement.getRightHandSide()).getOperator();
    CBinaryExpression.BinaryOperator initConditionOperator =
        updateOperator == CBinaryExpression.BinaryOperator.PLUS
            ? CBinaryExpression.BinaryOperator.GREATER_EQUAL
            : CBinaryExpression.BinaryOperator.LESS_EQUAL;
    CExpression initCondition =
        new CBinaryExpression(
            FileLocation.DUMMY,
            loopCondition.getExpressionType(),
            loopCondition.getCalculationType(),
            pTransformableLoop.getLoopIndexExpression(),
            initStatement.getRightHandSide(),
            initConditionOperator);

    CIntegerLiteralExpression zeroLiteral =
        new CIntegerLiteralExpression(
            FileLocation.DUMMY,
            pTransformableLoop.getLoopIndexExpression().getExpressionType(),
            BigInteger.ZERO);
    CExpression indexGreaterEqualZeroCondition =
        new CBinaryExpression(
            FileLocation.DUMMY,
            zeroLiteral.getExpressionType(),
            zeroLiteral.getExpressionType(),
            pTransformableLoop.getLoopIndexExpression(),
            zeroLiteral,
            CBinaryExpression.BinaryOperator.GREATER_EQUAL);

    ImmutableList.Builder<CExpression> conditionsBuilder = ImmutableList.builder();

    for (TransformableArray transformableArray : pTransformableArrays) {
      ReplaceLoopIndexAstTransformer astTransformer =
          new ReplaceLoopIndexAstTransformer(transformableArray, pTransformableLoop);
      conditionsBuilder.add((CExpression) astTransformer.transform(indexGreaterEqualZeroCondition));
      conditionsBuilder.add((CExpression) astTransformer.transform(initCondition));
      conditionsBuilder.add((CExpression) astTransformer.transform(loopCondition));
    }

    return conditionsBuilder.build();
  }

  private static ImmutableSet<TransformableLoop> getTransformableLoops(CFA pCfa) {

    ImmutableSet.Builder<TransformableLoop> transformableLoopsBuilder = ImmutableSet.builder();

    // TODO: split this function into smaller parts
    // TODO: make loop detection more flexible, e.g. other inital index value

    outer:
    for (CFANode node : pCfa.getAllNodes()) {
      if (node.isLoopStart()) {

        CIdExpression indexCIdExpression = null;
        CExpression upperBoundExpression = null;

        CStatementEdge initializationEdge = null;
        CStatementEdge updateEdge = null;
        CAssumeEdge conditionEdge = null;
        CAssumeEdge negConditionEdge = null;

        for (CFAEdge enteringEdge : CFAUtils.allEnteringEdges(node)) {
          if (enteringEdge instanceof CStatementEdge) {
            CStatementEdge statementEdge = (CStatementEdge) enteringEdge;
            CStatement statement = statementEdge.getStatement();
            if (statement instanceof CExpressionAssignmentStatement) {
              CExpressionAssignmentStatement assignmentStatement =
                  (CExpressionAssignmentStatement) statement;

              CLeftHandSide lhs = assignmentStatement.getLeftHandSide();
              if (lhs instanceof CIdExpression) {
                if (indexCIdExpression == null) {
                  indexCIdExpression = (CIdExpression) lhs;
                } else {
                  if (!getMemoryLocation((CIdExpression) lhs)
                      .equals(getMemoryLocation(indexCIdExpression))) {
                    break outer;
                  }
                }
              } else {
                break outer;
              }

              CExpression rhs = assignmentStatement.getRightHandSide();
              if (rhs instanceof CIntegerLiteralExpression && initializationEdge == null) {
                initializationEdge = statementEdge;
              } else if (rhs instanceof CBinaryExpression && updateEdge == null) {
                CBinaryExpression rhsBinary = (CBinaryExpression) rhs;
                CExpression sndOperand = rhsBinary.getOperand2();

                if ((rhsBinary.getOperator().equals(CBinaryExpression.BinaryOperator.PLUS)
                        || rhsBinary.getOperator().equals(CBinaryExpression.BinaryOperator.MINUS))
                    && rhsBinary.getOperand1().equals(lhs)
                    && sndOperand instanceof CIntegerLiteralExpression
                    && ((CIntegerLiteralExpression) sndOperand).getValue().equals(BigInteger.ONE)) {
                  updateEdge = statementEdge;
                } else {
                  break outer;
                }
              } else {
                break outer;
              }
            } else {
              break outer;
            }
          } else {
            break outer;
          }
        }

        for (CFAEdge leavingEdge : CFAUtils.allLeavingEdges(node)) {
          if (leavingEdge instanceof CAssumeEdge) {
            CAssumeEdge assumeEdge = (CAssumeEdge) leavingEdge;
            CExpression expression = assumeEdge.getExpression();

            if (expression instanceof CBinaryExpression) {
              CBinaryExpression binaryExpression = (CBinaryExpression) expression;
              CExpression fstOperand = binaryExpression.getOperand1();
              CExpression sndOperand = binaryExpression.getOperand2();

              if (binaryExpression.getOperator().equals(CBinaryExpression.BinaryOperator.LESS_THAN)
                  && fstOperand instanceof CIdExpression
                  && getMemoryLocation((CIdExpression) fstOperand)
                      .equals(getMemoryLocation(indexCIdExpression))) {

                if (upperBoundExpression == null) {
                  upperBoundExpression = sndOperand;
                }

                if (sndOperand.equals(upperBoundExpression)) {

                  if (assumeEdge.getTruthAssumption() && conditionEdge == null) {
                    conditionEdge = assumeEdge;
                  } else if (!assumeEdge.getTruthAssumption() && negConditionEdge == null) {
                    negConditionEdge = assumeEdge;
                  } else {
                    break outer;
                  }
                }
              } else {
                break outer;
              }
            } else {
              break outer;
            }
          } else {
            break outer;
          }
        }

        if (upperBoundExpression != null
            && initializationEdge != null
            && updateEdge != null
            && conditionEdge != null
            && negConditionEdge != null) {

          CFAEdge ignoreEdge = negConditionEdge;

          Set<CFANode> visitedNodes = new HashSet<>();
          Set<CFAEdge> visitedEdges = new HashSet<>();

          CFATraversal.dfs()
              .ignoreFunctionCalls()
              .traverse(
                  node,
                  new CFAVisitor() {

                    @Override
                    public TraversalProcess visitEdge(CFAEdge pEdge) {

                      visitedEdges.add(pEdge);

                      return pEdge.equals(ignoreEdge)
                          ? TraversalProcess.SKIP
                          : TraversalProcess.CONTINUE;
                    }

                    @Override
                    public TraversalProcess visitNode(CFANode pNode) {

                      if (visitedNodes.add(pNode)) {
                        return TraversalProcess.CONTINUE;
                      } else {
                        return TraversalProcess.SKIP;
                      }
                    }
                  });

          TransformableLoop transformableLoop =
              new TransformableLoop(
                  node,
                  indexCIdExpression,
                  initializationEdge,
                  updateEdge,
                  conditionEdge,
                  negConditionEdge,
                  ImmutableSet.copyOf(visitedEdges));
          transformableLoopsBuilder.add(transformableLoop);
        }
      }
    }

    return transformableLoopsBuilder.build();
  }

  private static void splitNodeAndInsertEdge(
      CCfaTransformer.Node pNode, CCfaTransformer.Edge pEdge) {

    CCfaTransformer.Node newNode = CCfaTransformer.createNode(pNode.getOldCfaNode());

    for (Iterator<CCfaTransformer.Edge> iterator = pNode.newLeavingIterator();
        iterator.hasNext(); ) {
      CCfaTransformer.Edge leavingEdge = iterator.next();
      iterator.remove();
      CCfaTransformer.attachLeaving(newNode, leavingEdge);
    }

    CCfaTransformer.attachEntering(newNode, pEdge);
    CCfaTransformer.attachLeaving(pNode, pEdge);
  }

  private static void replaceLoopWithBranching(
      CCfaTransformer pTransformer,
      ImmutableSet<TransformableArray> pTransformableArrays,
      TransformableLoop pTransformableLoop) {

    CCfaTransformer.Node loopNode =
        pTransformer.getNode(pTransformableLoop.getLoopCfaNode()).orElseThrow();

    CCfaTransformer.Edge initEdge = null;
    CCfaTransformer.Edge updateEdge = null;
    CCfaTransformer.Edge continueEdge = null;
    CCfaTransformer.Edge breakEdge = null;

    for (CCfaTransformer.Edge edge :
        Iterables.concat(loopNode.iterateEntering(), loopNode.iterateLeaving())) {
      CFAEdge oldCfaEdge = edge.getOldCfaEdge();
      if (oldCfaEdge.equals(pTransformableLoop.getInitLoopIndexCfaEdge())) {
        initEdge = edge;
      } else if (oldCfaEdge.equals(pTransformableLoop.getUpdateLoopIndexCfaEdge())) {
        updateEdge = edge;
      } else if (oldCfaEdge.equals(pTransformableLoop.getEnterLoopCfaEdge())) {
        continueEdge = edge;
      } else if (oldCfaEdge.equals(pTransformableLoop.getExitLoopCfaEdge())) {
        breakEdge = edge;
      }
    }

    assert initEdge != null;
    assert updateEdge != null;
    assert continueEdge != null;
    assert breakEdge != null;

    CCfaTransformer.Node outerBeforeLoop = initEdge.getPredecessor().orElseThrow();
    CCfaTransformer.Node outerAfterLoop = breakEdge.getSuccessor().orElseThrow();
    CCfaTransformer.Node loopBodyFirst = continueEdge.getSuccessor().orElseThrow();
    CCfaTransformer.Node loopBodyLast = updateEdge.getPredecessor().orElseThrow();

    CCfaTransformer.detachAll(initEdge);
    CCfaTransformer.detachAll(updateEdge);
    CCfaTransformer.detachAll(continueEdge);
    CCfaTransformer.detachAll(breakEdge);

    CCfaTransformer.Edge enterUnrolledLoopEdge =
        CCfaTransformer.createEdge(
            new BlankEdge(
                "",
                FileLocation.DUMMY,
                CFANode.newDummyCFANode("dummy-predecessor"),
                CFANode.newDummyCFANode("dummy-successor"),
                "enter-loop-body"));
    CCfaTransformer.attachLeaving(outerBeforeLoop, enterUnrolledLoopEdge);
    CCfaTransformer.attachEntering(loopBodyFirst, enterUnrolledLoopEdge);

    CCfaTransformer.Edge exitUnrolledLoopEdge =
        CCfaTransformer.createEdge(
            new BlankEdge(
                "",
                FileLocation.DUMMY,
                CFANode.newDummyCFANode("dummy-predecessor"),
                CFANode.newDummyCFANode("dummy-successor"),
                "exit-loop-body"));
    CCfaTransformer.attachLeaving(loopBodyLast, exitUnrolledLoopEdge);
    CCfaTransformer.attachEntering(outerAfterLoop, exitUnrolledLoopEdge);

    for (CExpression conditionExpression :
        getConditionExpressions(pTransformableArrays, pTransformableLoop)) {
      splitNodeAndInsertEdge(
          outerBeforeLoop,
          CCfaTransformer.createEdge(
              new CAssumeEdge(
                  "",
                  FileLocation.DUMMY,
                  CFANode.newDummyCFANode("dummy-predecessor"),
                  CFANode.newDummyCFANode("dummy-successor"),
                  conditionExpression,
                  true)));
      CCfaTransformer.Edge skipLoopBody =
          CCfaTransformer.createEdge(
              new CAssumeEdge(
                  "",
                  FileLocation.DUMMY,
                  CFANode.newDummyCFANode("dummy-predecessor"),
                  CFANode.newDummyCFANode("dummy-successor"),
                  conditionExpression,
                  false));
    CCfaTransformer.attachLeaving(outerBeforeLoop, skipLoopBody);
    CCfaTransformer.attachEntering(outerAfterLoop, skipLoopBody);
    }
  }

  public static CFA transformCfa(
      Configuration pConfiguration, LogManager pLogger, CFA pOriginalCfa) {

    Objects.requireNonNull(pConfiguration, "pConfiguration must not be null");
    Objects.requireNonNull(pLogger, "pLogger must not be null");
    Objects.requireNonNull(pOriginalCfa, "pOriginalCfa must not be null");

    ImmutableSet<TransformableArray> transformableArrays =
        ArrayAbstraction.getTransformableArrays(pOriginalCfa);
    ImmutableSet<TransformableLoop> transformableLoops =
        ArrayAbstraction.getTransformableLoops(pOriginalCfa);

    CCfaTransformer cfaTransformer =
        CCfaTransformer.createTransformer(pConfiguration, pLogger, pOriginalCfa);
    VariableNameGenerator variableNameGenerator = new VariableNameGenerator("__nondet_variable_");

    for (TransformableLoop loop : transformableLoops) {
      ImmutableSet.Builder<CIdExpression> loopDefsBuilder = ImmutableSet.builder();
      for (CFAEdge edge : loop.loopEdges) {
        if (edge instanceof CStatementEdge && !edge.equals(loop.getUpdateLoopIndexCfaEdge())) {
          CStatement statement = ((CStatementEdge) edge).getStatement();
          if (statement instanceof CExpressionAssignmentStatement) {
            CLeftHandSide lhs = ((CExpressionAssignmentStatement) statement).getLeftHandSide();
            if (lhs instanceof CIdExpression) {
              loopDefsBuilder.add((CIdExpression) lhs);
            }
          }
        }
      }

      ImmutableSet<CIdExpression> loopDefs = loopDefsBuilder.build();
      CCfaTransformer.Node firstLoopBodyNode =
          cfaTransformer.getNode(loop.getEnterLoopCfaEdge().getSuccessor()).orElseThrow();
      CCfaTransformer.Node lastLoopBodyNode =
          cfaTransformer.getNode(loop.getUpdateLoopIndexCfaEdge().getPredecessor()).orElseThrow();

      for (TransformableArray transformableArray : transformableArrays) {
        splitNodeAndInsertEdge(
            firstLoopBodyNode,
            CCfaTransformer.createEdge(
                createAssignEdge(
                    createIndexCIdExpression(transformableArray), loop.getLoopIndexExpression())));
      }
      transformableArrays.stream()
          .findAny()
          .ifPresent(
              transformableArray ->
                  splitNodeAndInsertEdge(
                      firstLoopBodyNode,
                      CCfaTransformer.createEdge(
                          createAssignEdge(
                              loop.getLoopIndexExpression(),
                              createIndexCIdExpression(transformableArray)))));

      for (CIdExpression loopDef : loopDefs) {
        splitNodeAndInsertEdge(
            firstLoopBodyNode,
            CCfaTransformer.createEdge(
                createNondetValueAssignEdge(loopDef, variableNameGenerator)));
        splitNodeAndInsertEdge(
            lastLoopBodyNode,
            CCfaTransformer.createEdge(
                createNondetValueAssignEdge(loopDef, variableNameGenerator)));
      }

      replaceLoopWithBranching(cfaTransformer, transformableArrays, loop);
    }

    return cfaTransformer.createCfa(
        new ArrayAbstractionNodeTransformer(),
        new ArrayAbstractionEdgeTransformer(transformableArrays, transformableLoops));
  }

  public static final class TransformableArray {

    private final MemoryLocation memoryLocation;
    private final CArrayType type;

    private TransformableArray(MemoryLocation pMemoryLocation, CArrayType pType) {
      memoryLocation = pMemoryLocation;
      type = pType;
    }

    private MemoryLocation getMemoryLocation() {
      return memoryLocation;
    }

    private CArrayType getArrayType() {
      return type;
    }
  }

  public static final class TransformableLoop {

    private final CFANode loopCfaNode;
    private final CIdExpression loopIndexExpression;

    private final CStatementEdge initLoopIndexCfaEdge;
    private final CStatementEdge updateLoopIndexCfaEdge;
    private final CAssumeEdge enterLoopCfaEdge;
    private final CAssumeEdge exitLoopCfaEdge;

    private final ImmutableSet<CFAEdge> loopEdges;

    private TransformableLoop(
        CFANode pLoopCfaNode,
        CIdExpression pLoopIndexExpression,
        CStatementEdge pInitLoopIndexCfaEdge,
        CStatementEdge pUpdateLoopIndexCfaEdge,
        CAssumeEdge pEnterLoopCfaEdge,
        CAssumeEdge pExitLoopCfaEdge,
        ImmutableSet<CFAEdge> pLoopEdges) {
      loopCfaNode = pLoopCfaNode;
      loopIndexExpression = pLoopIndexExpression;
      initLoopIndexCfaEdge = pInitLoopIndexCfaEdge;
      updateLoopIndexCfaEdge = pUpdateLoopIndexCfaEdge;
      enterLoopCfaEdge = pEnterLoopCfaEdge;
      exitLoopCfaEdge = pExitLoopCfaEdge;
      loopEdges = pLoopEdges;
    }

    private CFANode getLoopCfaNode() {
      return loopCfaNode;
    }

    private CIdExpression getLoopIndexExpression() {
      return loopIndexExpression;
    }

    private MemoryLocation getLoopIndexMemoryLocation() {
      return getMemoryLocation(loopIndexExpression);
    }

    private CStatementEdge getInitLoopIndexCfaEdge() {
      return initLoopIndexCfaEdge;
    }

    private CStatementEdge getUpdateLoopIndexCfaEdge() {
      return updateLoopIndexCfaEdge;
    }

    private CAssumeEdge getEnterLoopCfaEdge() {
      return enterLoopCfaEdge;
    }

    private CAssumeEdge getExitLoopCfaEdge() {
      return exitLoopCfaEdge;
    }
  }

  private static final class VariableNameGenerator {

    private final String prefix;
    private int counter;

    private VariableNameGenerator(String pPrefix) {
      prefix = pPrefix;
    }

    private String next() {
      return prefix + counter++;
    }
  }

  private static final class ReplaceLoopIndexAstTransformer
      extends CAstNodeTransformer<CAstNodeTransformer.ImpossibleException> {

    private final TransformableArray transformableArray;
    private final TransformableLoop transformableLoop;

    private ReplaceLoopIndexAstTransformer(
        TransformableArray pTransformableArray, TransformableLoop pTransformableLoop) {
      transformableArray = pTransformableArray;
      transformableLoop = pTransformableLoop;
    }

    @Override
    public CIdExpression visit(CIdExpression pCIdExpression) throws ImpossibleException {

      if (getMemoryLocation(pCIdExpression)
          .equals(transformableLoop.getLoopIndexMemoryLocation())) {
        return createIndexCIdExpression(transformableArray);
      }

      return super.visit(pCIdExpression);
    }
  }

  private static final class ArrayAbstractionAstTransformer
      extends CAstNodeTransformer<CAstNodeTransformer.ImpossibleException> {

    private final ImmutableSet<TransformableArray> transformableArrays;
    private final ImmutableSet<TransformableLoop> transformableLoops;

    private ArrayAbstractionAstTransformer(
        ImmutableSet<TransformableArray> pTransformableArrays,
        ImmutableSet<TransformableLoop> pTransformableLoops) {
      transformableArrays = pTransformableArrays;
      transformableLoops = pTransformableLoops;
    }

    @Override
    public CExpression visit(CArraySubscriptExpression pCArraySubscriptExpression)
        throws ImpossibleException {

      CExpression arrayExpression = pCArraySubscriptExpression.getArrayExpression();
      CExpression subscriptExpression = pCArraySubscriptExpression.getSubscriptExpression();

      if (arrayExpression instanceof CIdExpression
          && subscriptExpression instanceof CIdExpression) {
        CIdExpression arrayIdExpression = (CIdExpression) arrayExpression;
        CIdExpression subscriptIdExpression = (CIdExpression) subscriptExpression;

        for (TransformableArray transformableArray : transformableArrays) {
          if (getMemoryLocation(arrayIdExpression).equals(transformableArray.getMemoryLocation())) {
            for (TransformableLoop transformableLoop : transformableLoops) {
              if (getMemoryLocation(subscriptIdExpression)
                  .equals(transformableLoop.getLoopIndexMemoryLocation())) {
                return createVariableCIdExpression(transformableArray);
              }
            }
          }
        }
      }

      return super.visit(pCArraySubscriptExpression);
    }
  }

  private static final class ArrayAbstractionNodeTransformer
      implements CCfaTransformer.NodeTransformer {

    @Override
    public CFANode transformCfaNode(CFANode pOldCfaNode) {
      return new CFANode(pOldCfaNode.getFunction());
    }

    @Override
    public CFATerminationNode transformCfaTerminationNode(
        CFATerminationNode pOldCfaTerminationNode) {
      return new CFATerminationNode(pOldCfaTerminationNode.getFunction());
    }

    @Override
    public FunctionExitNode transformFunctionExitNode(FunctionExitNode pOldFunctionExitNode) {
      return new FunctionExitNode(pOldFunctionExitNode.getFunction());
    }

    @Override
    public CFunctionEntryNode transformCFunctionEntryNode(
        CFunctionEntryNode pOldCFunctionEntryNode, FunctionExitNode pNewFunctionExitNode) {
      return new CFunctionEntryNode(
          pOldCFunctionEntryNode.getFileLocation(),
          (CFunctionDeclaration) pOldCFunctionEntryNode.getFunction(),
          pNewFunctionExitNode,
          pOldCFunctionEntryNode.getReturnVariable());
    }

    @Override
    public CFANode transformCLabelNode(CLabelNode pOldCLabelNode) {
      return new CLabelNode(pOldCLabelNode.getFunction(), pOldCLabelNode.getLabel());
    }
  }

  private static final class ArrayAbstractionEdgeTransformer
      implements CCfaTransformer.EdgeTransformer {

    private final ArrayAbstractionAstTransformer astTransformer;

    private ArrayAbstractionEdgeTransformer(
        ImmutableSet<TransformableArray> pTransformableArrays,
        ImmutableSet<TransformableLoop> pTransformableLoops) {
      astTransformer =
          new ArrayAbstractionAstTransformer(pTransformableArrays, pTransformableLoops);
    }

    @Override
    public CFAEdge transformBlankEdge(
        BlankEdge pOldBlankEdge, CFANode pNewPredecessor, CFANode pNewSuccessor) {
      return new BlankEdge(
          pOldBlankEdge.getRawStatement(),
          pOldBlankEdge.getFileLocation(),
          pNewPredecessor,
          pNewSuccessor,
          pOldBlankEdge.getDescription());
    }

    @Override
    public CFAEdge transformCAssumeEdge(
        CAssumeEdge pOldCAssumeEdge, CFANode pNewPredecessor, CFANode pNewSuccessor) {

      CExpression newExpression =
          (CExpression) astTransformer.transform(pOldCAssumeEdge.getExpression());

      return new CAssumeEdge(
          pOldCAssumeEdge.getRawStatement(),
          pOldCAssumeEdge.getFileLocation(),
          pNewPredecessor,
          pNewSuccessor,
          newExpression,
          pOldCAssumeEdge.getTruthAssumption(),
          pOldCAssumeEdge.isSwapped(),
          pOldCAssumeEdge.isArtificialIntermediate());
    }

    @Override
    public CFAEdge transformCDeclarationEdge(
        CDeclarationEdge pOldCDeclarationEdge, CFANode pNewPredecessor, CFANode pNewSuccessor) {

      CDeclaration newDeclaration =
          (CDeclaration) astTransformer.transform(pOldCDeclarationEdge.getDeclaration());

      return new CDeclarationEdge(
          pOldCDeclarationEdge.getRawStatement(),
          pOldCDeclarationEdge.getFileLocation(),
          pNewPredecessor,
          pNewSuccessor,
          newDeclaration);
    }

    @Override
    public CFAEdge transformCStatementEdge(
        CStatementEdge pOldCStatementEdge, CFANode pNewPredecessor, CFANode pNewSuccessor) {

      CStatement newStatement =
          (CStatement) astTransformer.transform(pOldCStatementEdge.getStatement());

      return new CStatementEdge(
          pOldCStatementEdge.getRawStatement(),
          newStatement,
          pOldCStatementEdge.getFileLocation(),
          pNewPredecessor,
          pNewSuccessor);
    }

    @Override
    public CFunctionCallEdge transformCFunctionCallEdge(
        CFunctionCallEdge pOldCFunctionCallEdge,
        CFANode pNewPredecessor,
        CFunctionEntryNode pNewSuccessor,
        CFunctionSummaryEdge pNewCFunctionSummaryEdge) {
      return new CFunctionCallEdge(
          pOldCFunctionCallEdge.getRawStatement(),
          pOldCFunctionCallEdge.getFileLocation(),
          pNewPredecessor,
          pNewSuccessor,
          pNewCFunctionSummaryEdge.getExpression(),
          pNewCFunctionSummaryEdge);
    }

    @Override
    public CFunctionReturnEdge transformCFunctionReturnEdge(
        CFunctionReturnEdge pOldCFunctionReturnEdge,
        FunctionExitNode pNewPredecessor,
        CFANode pNewSuccessor,
        CFunctionSummaryEdge pNewCFunctionSummaryEdge) {
      return new CFunctionReturnEdge(
          pOldCFunctionReturnEdge.getFileLocation(),
          pNewPredecessor,
          pNewSuccessor,
          pNewCFunctionSummaryEdge);
    }

    @Override
    public CFunctionSummaryEdge transformCFunctionSummaryEdge(
        CFunctionSummaryEdge pOldCFunctionSummaryEdge,
        CFANode pNewPredecessor,
        CFANode pNewSuccessor,
        CFunctionEntryNode pNewCFunctionEntryNode) {

      CFunctionCall newFunctionCall =
          (CFunctionCall) astTransformer.transform(pOldCFunctionSummaryEdge.getExpression());

      return new CFunctionSummaryEdge(
          pOldCFunctionSummaryEdge.getRawStatement(),
          pOldCFunctionSummaryEdge.getFileLocation(),
          pNewPredecessor,
          pNewSuccessor,
          newFunctionCall,
          pNewCFunctionEntryNode);
    }

    @Override
    public CReturnStatementEdge transformCReturnStatementEdge(
        CReturnStatementEdge pOldCReturnStatementEdge,
        CFANode pNewPredecessor,
        FunctionExitNode pNewSuccessor) {

      Optional<CReturnStatement> optOldReturnStatement =
          pOldCReturnStatementEdge.getRawAST().toJavaUtil();
      CReturnStatement newReturnStatement = null;
      if (optOldReturnStatement.isPresent()) {
        newReturnStatement =
            (CReturnStatement) astTransformer.transform(optOldReturnStatement.orElseThrow());
      }

      return new CReturnStatementEdge(
          pOldCReturnStatementEdge.getRawStatement(),
          newReturnStatement,
          pOldCReturnStatementEdge.getFileLocation(),
          pNewPredecessor,
          pNewSuccessor);
    }

    @Override
    public CFunctionSummaryStatementEdge transformCFunctionSummaryStatementEdge(
        CFunctionSummaryStatementEdge pOldCFunctionSummaryStatementEdge,
        CFANode pNewPredecessor,
        CFANode pNewSuccessor) {

      CStatement newStatement =
          (CStatement) astTransformer.transform(pOldCFunctionSummaryStatementEdge.getStatement());
      CFunctionCall newFunctionCall =
          (CFunctionCall)
              astTransformer.transform(pOldCFunctionSummaryStatementEdge.getFunctionCall());

      return new CFunctionSummaryStatementEdge(
          pOldCFunctionSummaryStatementEdge.getRawStatement(),
          newStatement,
          pOldCFunctionSummaryStatementEdge.getFileLocation(),
          pNewPredecessor,
          pNewSuccessor,
          newFunctionCall,
          pOldCFunctionSummaryStatementEdge.getFunctionName());
    }
  }
}
