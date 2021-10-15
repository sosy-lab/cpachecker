// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.arrayabstraction;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CAddressOfLabelExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CImaginaryLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStringLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression.UnaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.CFATraversal;
import org.sosy_lab.cpachecker.util.CFATraversal.CFAVisitor;
import org.sosy_lab.cpachecker.util.CFATraversal.TraversalProcess;
import org.sosy_lab.cpachecker.util.CFAUtils;
import org.sosy_lab.cpachecker.util.LoopStructure;
import org.sosy_lab.cpachecker.util.LoopStructure.Loop;
import org.sosy_lab.cpachecker.util.dependencegraph.Dominance;
import org.sosy_lab.cpachecker.util.dependencegraph.DominanceUtils;
import org.sosy_lab.cpachecker.util.dependencegraph.EdgeDefUseData;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;
import org.sosy_lab.cpachecker.util.variableclassification.VariableClassification;

public final class TransformableLoop {

  private final LoopStructure.Loop loop;
  private final CFANode loopNode;

  private final CSimpleDeclaration indexVariable;
  private final CFAEdge initializeIndexEdge;
  private final CFAEdge updateIndexEdge;

  public TransformableLoop(
      Loop pLoop,
      CFANode pLoopNode,
      CSimpleDeclaration pIndexVariable,
      CFAEdge pInitializeIndexEdge,
      CFAEdge pUpdateIndexEdge) {
    loop = pLoop;
    loopNode = pLoopNode;
    indexVariable = pIndexVariable;
    initializeIndexEdge = pInitializeIndexEdge;
    updateIndexEdge = pUpdateIndexEdge;
  }

  public CFANode getLoopNode() {
    return loopNode;
  }

  public CSimpleDeclaration getIndexVariable() {
    return indexVariable;
  }

  public CFAEdge getInitializeIndexEdge() {
    return initializeIndexEdge;
  }

  public CFAEdge getUpdateIndexEdge() {
    return updateIndexEdge;
  }

  public ImmutableSet<CFAEdge> getInnerLoopEdges() {
    return loop.getInnerLoopEdges();
  }

  public CFAEdge getIncomingEdge() {
    return loop.getIncomingEdges().stream().findAny().orElseThrow();
  }

  public CFAEdge getOutgoingEdge() {
    return loop.getOutgoingEdges().stream().findAny().orElseThrow();
  }

  private static ImmutableSet<CFAEdge> getDominatedInnerLoopEdges(
      CFAEdge pEdge, LoopStructure.Loop pLoop, CFANode pLoopStart) {

    checkArgument(pLoop.getLoopNodes().contains(pLoopStart));
    checkArgument(pLoop.getInnerLoopEdges().contains(pEdge));

    ImmutableSet.Builder<CFAEdge> builder = ImmutableSet.builder();

    CFANode startNode = pEdge.getSuccessor();
    Dominance.DomTree<CFANode> domTree =
        DominanceUtils.createFunctionDomTree(startNode, ImmutableSet.of(pLoopStart));
    int startId = domTree.getId(startNode);
    for (int id = 0; id < domTree.getNodeCount(); id++) {
      if (id == startId || domTree.isAncestorOf(startId, id)) {
        builder.addAll(CFAUtils.leavingEdges(domTree.getNode(id)));
      }
    }

    return builder.build();
  }

  private static ImmutableSet<CFAEdge> getPostDominatedInnerLoopEdges(
      CFAEdge pEdge, LoopStructure.Loop pLoop, CFANode pLoopStart) {

    checkArgument(pLoop.getLoopNodes().contains(pLoopStart));
    checkArgument(pLoop.getInnerLoopEdges().contains(pEdge));

    ImmutableSet.Builder<CFAEdge> builder = ImmutableSet.builder();

    CFANode startNode = pEdge.getPredecessor();
    Dominance.DomTree<CFANode> postDomTree =
        DominanceUtils.createFunctionPostDomTree(startNode, ImmutableSet.of(pLoopStart));
    int startId = postDomTree.getId(startNode);
    for (int id = 0; id < postDomTree.getNodeCount(); id++) {
      if (id == startId || postDomTree.isAncestorOf(startId, id)) {
        builder.addAll(CFAUtils.enteringEdges(postDomTree.getNode(id)));
      }
    }

    return builder.build();
  }

  private static boolean isExecutedEveryIteration(
      CFAEdge pEdge, LoopStructure.Loop pLoop, CFANode pLoopNode) {

    Set<CFAEdge> dominatedEdges = new HashSet<>();
    dominatedEdges.add(pEdge);
    dominatedEdges.addAll(getDominatedInnerLoopEdges(pEdge, pLoop, pLoopNode));
    dominatedEdges.addAll(getPostDominatedInnerLoopEdges(pEdge, pLoop, pLoopNode));

    return Sets.difference(pLoop.getInnerLoopEdges(), dominatedEdges).isEmpty();
  }

  private static ImmutableSet<CFAEdge> getIncomingDefs(
      CFAEdge pIncomingEdge, CSimpleDeclaration pVariableDeclaration) {

    ImmutableSet.Builder<CFAEdge> builder = ImmutableSet.builder();

    MemoryLocation memoryLocation = MemoryLocation.forDeclaration(pVariableDeclaration);
    EdgeDefUseData.Extractor extractor = EdgeDefUseData.createExtractor(false);
    CFAEdge incomingEdge = pIncomingEdge;

    if (extractor.extract(incomingEdge).getDefs().contains(memoryLocation)) {
      builder.add(incomingEdge);
    }

    CFATraversal.dfs()
        .backwards()
        .traverseOnce(
            incomingEdge.getPredecessor(),
            new CFAVisitor() {

              @Override
              public TraversalProcess visitEdge(CFAEdge pEdge) {

                if (extractor.extract(pEdge).getDefs().contains(memoryLocation)) {
                  builder.add(pEdge);

                  return TraversalProcess.SKIP;
                }

                return TraversalProcess.CONTINUE;
              }

              @Override
              public TraversalProcess visitNode(CFANode pNode) {
                return TraversalProcess.CONTINUE;
              }
            });

    return builder.build();
  }

  private static ImmutableSet<CFAEdge> getOutgoingUses(
      CFAEdge pOutgoingEdge, CSimpleDeclaration pVariableDeclaration) {

    ImmutableSet.Builder<CFAEdge> builder = ImmutableSet.builder();

    MemoryLocation memoryLocation = MemoryLocation.forDeclaration(pVariableDeclaration);
    EdgeDefUseData.Extractor extractor = EdgeDefUseData.createExtractor(false);
    CFAEdge outgoingEdge = pOutgoingEdge;

    CFATraversal.dfs()
        .traverseOnce(
            outgoingEdge.getSuccessor(),
            new CFAVisitor() {

              @Override
              public TraversalProcess visitEdge(CFAEdge pEdge) {

                EdgeDefUseData edgeDefUseData = extractor.extract(pEdge);

                if (edgeDefUseData.getUses().contains(memoryLocation)) {
                  builder.add(pEdge);
                }

                if (edgeDefUseData.getDefs().contains(memoryLocation)) {
                  return TraversalProcess.SKIP;
                }

                return TraversalProcess.CONTINUE;
              }

              @Override
              public TraversalProcess visitNode(CFANode pNode) {
                return TraversalProcess.CONTINUE;
              }
            });

    return builder.build();
  }

  private static int countInnerLoopDefs(LoopStructure.Loop pLoop, CSimpleDeclaration pDeclaration) {

    MemoryLocation memoryLocation = MemoryLocation.forDeclaration(pDeclaration);
    EdgeDefUseData.Extractor extractor = EdgeDefUseData.createExtractor(false);

    int count = 0;
    for (CFAEdge edge : pLoop.getInnerLoopEdges()) {
      if (extractor.extract(edge).getDefs().contains(memoryLocation)) {
        count++;
      }
    }

    return count;
  }

  private static boolean isConstantExpression(CExpression pExpression) {

    CExpressionVisitor<Boolean, NoException> expressionVisitor =
        new CExpressionVisitor<>() {

          @Override
          public Boolean visit(CArraySubscriptExpression pIastArraySubscriptExpression) {
            return false;
          }

          @Override
          public Boolean visit(CFieldReference pIastFieldReference) {
            return false;
          }

          @Override
          public Boolean visit(CIdExpression pIastIdExpression) {
            return false;
          }

          @Override
          public Boolean visit(CPointerExpression pPointerExpression) {
            return false;
          }

          @Override
          public Boolean visit(CComplexCastExpression pComplexCastExpression) {
            return false;
          }

          @Override
          public Boolean visit(CBinaryExpression pIastBinaryExpression) {

            CExpression operand1 = pIastBinaryExpression.getOperand1();
            CExpression operand2 = pIastBinaryExpression.getOperand2();

            return operand1.accept(this) && operand2.accept(this);
          }

          @Override
          public Boolean visit(CCastExpression pIastCastExpression) {
            return pIastCastExpression.getOperand().accept(this);
          }

          @Override
          public Boolean visit(CCharLiteralExpression pIastCharLiteralExpression) {
            return true;
          }

          @Override
          public Boolean visit(CFloatLiteralExpression pIastFloatLiteralExpression) {
            return true;
          }

          @Override
          public Boolean visit(CIntegerLiteralExpression pIastIntegerLiteralExpression) {
            return true;
          }

          @Override
          public Boolean visit(CStringLiteralExpression pIastStringLiteralExpression) {
            return false;
          }

          @Override
          public Boolean visit(CTypeIdExpression pIastTypeIdExpression) {
            return false;
          }

          @Override
          public Boolean visit(CUnaryExpression pIastUnaryExpression) {

            if (pIastUnaryExpression.getOperator() == UnaryOperator.MINUS) {
              return pIastUnaryExpression.getOperand().accept(this);
            }

            return false;
          }

          @Override
          public Boolean visit(CImaginaryLiteralExpression PIastLiteralExpression) {
            return true;
          }

          @Override
          public Boolean visit(CAddressOfLabelExpression pAddressOfLabelExpression) {
            return false;
          }
        };

    return pExpression.accept(expressionVisitor);
  }

  private static Optional<TransformableLoop> of(CFA pCfa, LoopStructure.Loop pLoop) {

    if (pLoop.getIncomingEdges().size() != 1 || pLoop.getOutgoingEdges().size() != 1) {
      return Optional.empty();
    }

    CFAEdge incomingEdge = pLoop.getIncomingEdges().stream().findAny().orElseThrow();
    CFAEdge outgoingEdge = pLoop.getOutgoingEdges().stream().findAny().orElseThrow();

    if (!incomingEdge.getSuccessor().equals(outgoingEdge.getPredecessor())) {
      return Optional.empty();
    }

    CFANode loopNode = incomingEdge.getSuccessor();

    for (CFANode node : pLoop.getLoopNodes()) {
      if (!node.equals(loopNode) && node.isLoopStart()) {
        return Optional.empty();
      }
    }

    Optional<LoopConditionEdge> optLoopConditionEdge = createLoopConditionEdge(outgoingEdge);
    if (optLoopConditionEdge.isEmpty()) {
      return Optional.empty();
    }

    CSimpleDeclaration loopIndexDeclaration = optLoopConditionEdge.orElseThrow().getDeclaration();
    MemoryLocation loopIndexMemoryLocation = MemoryLocation.forDeclaration(loopIndexDeclaration);

    CFAEdge updateIndexEdge = null;
    EdgeDefUseData.Extractor defUseExtractor = EdgeDefUseData.createExtractor(false);
    ImmutableSet<CFAEdge> innerLoopEdges = pLoop.getInnerLoopEdges();
    for (CFAEdge innerLoopEdge : innerLoopEdges) {

      EdgeDefUseData edgeDefUseData = defUseExtractor.extract(innerLoopEdge);
      if (!edgeDefUseData.getDefs().contains(loopIndexMemoryLocation)) {
        continue;
      }

      Optional<IncDecEdge> optIncDecEdge = createIncDecEdge(innerLoopEdge);
      if (optIncDecEdge.isEmpty()) {
        continue;
      }

      IncDecEdge incDecEdge = optIncDecEdge.orElseThrow();
      assert incDecEdge.getDeclaration().equals(loopIndexDeclaration);

      if (!isExecutedEveryIteration(innerLoopEdge, pLoop, loopNode)) {
        continue;
      }

      updateIndexEdge = innerLoopEdge;
      break;
    }

    VariableClassification variableClassification = pCfa.getVarClassification().orElseThrow();
    if (variableClassification
        .getAddressedVariables()
        .contains(loopIndexDeclaration.getQualifiedName())) {
      return Optional.empty();
    }

    if (updateIndexEdge == null) {
      return Optional.empty();
    }

    if (!getOutgoingUses(outgoingEdge, loopIndexDeclaration).isEmpty()) {
      return Optional.empty();
    }

    ImmutableSet<CFAEdge> incomingDefs = getIncomingDefs(incomingEdge, loopIndexDeclaration);
    if (incomingDefs.size() != 1) {
      return Optional.empty();
    }

    CFAEdge indexDefEdge = incomingDefs.stream().findAny().orElseThrow();
    Optional<ConstantAssignEdge> optConstantAssignEdge = createConstantAssignEdge(indexDefEdge);
    if (optConstantAssignEdge.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(
        new TransformableLoop(
            pLoop, loopNode, loopIndexDeclaration, indexDefEdge, updateIndexEdge));
  }

  /** Returns all transformable loops in the specified CFA. */
  public static ImmutableSet<TransformableLoop> getTransformableLoops(CFA pCfa) {

    ImmutableSet.Builder<TransformableLoop> transformableLoops = ImmutableSet.builder();
    LoopStructure loopStructure = pCfa.getLoopStructure().orElseThrow();

    for (LoopStructure.Loop loop : loopStructure.getAllLoops()) {
      Optional<TransformableLoop> optTransformableLoop = TransformableLoop.of(pCfa, loop);
      if (optTransformableLoop.isPresent()) {
        transformableLoops.add(optTransformableLoop.orElseThrow());
      }
    }

    return transformableLoops.build();
  }

  private static Optional<SimpleAssignEdge> createSimpleAssignEdge(CFAEdge pEdge) {

    if (pEdge instanceof CDeclarationEdge) {

      CDeclarationEdge declarationEdge = (CDeclarationEdge) pEdge;
      CDeclaration declaration = declarationEdge.getDeclaration();

      if (declaration instanceof CVariableDeclaration) {
        CInitializer initializer = ((CVariableDeclaration) declaration).getInitializer();
        if (initializer instanceof CInitializerExpression) {
          CExpression expression = ((CInitializerExpression) initializer).getExpression();
          return Optional.of(new SimpleAssignEdge(declaration, expression));
        }
      }
    }

    if (pEdge instanceof CStatementEdge) {

      CStatementEdge statementEdge = (CStatementEdge) pEdge;
      CStatement statement = statementEdge.getStatement();

      if (statement instanceof CExpressionAssignmentStatement) {

        CExpressionAssignmentStatement assignmentStatement =
            (CExpressionAssignmentStatement) statement;

        CLeftHandSide lhs = assignmentStatement.getLeftHandSide();

        if (lhs instanceof CIdExpression) {

          CIdExpression lhsIdExpression = (CIdExpression) lhs;
          CSimpleDeclaration variableDeclaration = lhsIdExpression.getDeclaration();
          CExpression rhs = assignmentStatement.getRightHandSide();

          return Optional.of(new SimpleAssignEdge(variableDeclaration, rhs));
        }
      }
    }

    return Optional.empty();
  }

  private static Optional<LoopConditionEdge> createLoopConditionEdge(CFAEdge pEdge) {

    if (pEdge instanceof CAssumeEdge) {

      CAssumeEdge assumeEdge = (CAssumeEdge) pEdge;
      CExpression expression = assumeEdge.getExpression();

      if (expression instanceof CBinaryExpression) {

        CBinaryExpression binaryExpression = (CBinaryExpression) expression;
        CExpression operand1 = binaryExpression.getOperand1();
        CExpression operand2 = binaryExpression.getOperand2();
        BinaryOperator operator = binaryExpression.getOperator();

        if (operator == BinaryOperator.LESS_THAN
            || operator == BinaryOperator.GREATER_THAN
            || operator == BinaryOperator.LESS_EQUAL
            || operator == BinaryOperator.GREATER_EQUAL) {

          if (operand1 instanceof CIdExpression && isConstantExpression(operand2)) {

            CSimpleDeclaration variableDeclaration = ((CIdExpression) operand1).getDeclaration();
            return Optional.of(
                new LoopConditionEdge(pEdge, variableDeclaration, operand2, operator));
          }
        }
      }
    }

    return Optional.empty();
  }

  public static Optional<IncDecEdge> createIncDecEdge(CFAEdge pEdge) {

    Optional<SimpleAssignEdge> optSimpleAssignEdge = createSimpleAssignEdge(pEdge);

    if (optSimpleAssignEdge.isPresent()) {

      SimpleAssignEdge simpleAssignEdge = optSimpleAssignEdge.orElseThrow();
      CExpression expression = simpleAssignEdge.getExpression();

      if (expression instanceof CBinaryExpression) {

        CBinaryExpression binaryExpression = (CBinaryExpression) expression;
        CExpression operand1 = binaryExpression.getOperand1();
        CExpression operand2 = binaryExpression.getOperand2();
        BinaryOperator operator = binaryExpression.getOperator();

        if (operator == BinaryOperator.PLUS || operator == BinaryOperator.MINUS) {
          if (operand1 instanceof CIdExpression && operand2 instanceof CIntegerLiteralExpression) {

            CSimpleDeclaration simpleDeclaration = ((CIdExpression) operand1).getDeclaration();
            BigInteger stepValue = ((CIntegerLiteralExpression) operand2).getValue();

            if (simpleDeclaration.equals(simpleAssignEdge.getVariableDeclaration())) {
              return Optional.of(new IncDecEdge(pEdge, simpleDeclaration, stepValue, operator));
            }
          }
        }
      }
    }

    return Optional.empty();
  }

  private static Optional<ConstantAssignEdge> createConstantAssignEdge(CFAEdge pEdge) {

    Optional<SimpleAssignEdge> optSimpleAssignEdge = createSimpleAssignEdge(pEdge);

    if (optSimpleAssignEdge.isPresent()) {

      SimpleAssignEdge simpleAssignEdge = optSimpleAssignEdge.orElseThrow();
      CExpression expression = simpleAssignEdge.getExpression();

      if (expression instanceof CIntegerLiteralExpression) {
        BigInteger value = ((CIntegerLiteralExpression) expression).getValue();
        CSimpleDeclaration variableDeclaration = simpleAssignEdge.getVariableDeclaration();
        return Optional.of(new ConstantAssignEdge(pEdge, variableDeclaration, value));
      }
    }

    return Optional.empty();
  }

  public ImmutableSet<CFAEdge> getDominatedInnerLoopEdges(CFAEdge pEdge) {
    return getDominatedInnerLoopEdges(pEdge, loop, loopNode);
  }

  public ImmutableSet<CFAEdge> getPostDominatedInnerLoopEdges(CFAEdge pEdge) {
    return getPostDominatedInnerLoopEdges(pEdge, loop, loopNode);
  }

  public boolean isExecutedEveryIteration(CFAEdge pEdge) {
    return isExecutedEveryIteration(pEdge, loop, loopNode);
  }

  public boolean hasOutgoingUses(CSimpleDeclaration pDeclaration) {
    return !getOutgoingUses(getOutgoingEdge(), pDeclaration).isEmpty();
  }

  public int countInnerLoopDefs(CSimpleDeclaration pDeclaration) {
    return countInnerLoopDefs(loop, pDeclaration);
  }

  private static class SimpleAssignEdge {

    private final CSimpleDeclaration variableDeclaration;
    private final CExpression expression;

    private SimpleAssignEdge(CSimpleDeclaration pVariableDeclaration, CExpression pExpression) {
      variableDeclaration = pVariableDeclaration;
      expression = pExpression;
    }

    private CSimpleDeclaration getVariableDeclaration() {
      return variableDeclaration;
    }

    private CExpression getExpression() {
      return expression;
    }
  }

  public static final class ConstantAssignEdge {

    private final CFAEdge edge;
    private final CSimpleDeclaration variableDeclaration;
    private final BigInteger constant;

    private ConstantAssignEdge(
        CFAEdge pEdge, CSimpleDeclaration pVariableDeclaration, BigInteger pConstant) {
      edge = pEdge;
      variableDeclaration = pVariableDeclaration;
      constant = pConstant;
    }

    public CFAEdge getEdge() {
      return edge;
    }

    public CSimpleDeclaration getVariableDeclaration() {
      return variableDeclaration;
    }

    public BigInteger getConstant() {
      return constant;
    }
  }

  public static final class IncDecEdge {

    private final CFAEdge edge;
    private final CSimpleDeclaration declaration;
    private final BigInteger constant;
    private final BinaryOperator operator;

    public IncDecEdge(
        CFAEdge pEdge,
        CSimpleDeclaration pDeclaration,
        BigInteger pConstant,
        BinaryOperator pOperator) {
      edge = pEdge;
      declaration = pDeclaration;
      constant = pConstant;
      operator = pOperator;
    }

    public CFAEdge getEdge() {
      return edge;
    }

    public CSimpleDeclaration getDeclaration() {
      return declaration;
    }

    public BigInteger getConstant() {
      return constant;
    }

    public BinaryOperator getOperator() {
      return operator;
    }
  }

  public static final class LoopConditionEdge {

    private final CFAEdge edge;
    private final CSimpleDeclaration declaration;
    private final CExpression expression;
    private final BinaryOperator operator;

    private LoopConditionEdge(
        CFAEdge pEdge,
        CSimpleDeclaration pDeclaration,
        CExpression pExpression,
        BinaryOperator pOperator) {
      edge = pEdge;
      declaration = pDeclaration;
      expression = pExpression;
      operator = pOperator;
    }

    public CFAEdge getEdge() {
      return edge;
    }

    public CSimpleDeclaration getDeclaration() {
      return declaration;
    }

    public CExpression getExpression() {
      return expression;
    }

    public BinaryOperator getOperator() {
      return operator;
    }
  }
}
