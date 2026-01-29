// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.hb;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedMap;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayRangeDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CEnumerator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.FunctionExitNode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.Pair;

final class HappensBeforeEdgeTools {
  private static final String ONLY_C_SUPPORTED = "only C supported";
  private static final Map<Pair<Integer, Map<String, Integer>>, HappensBeforeEdgeTools> cache =
      new LinkedHashMap<>();

  static CFAEdge clone(
      final CFAEdge pCFAEdge, final int idx, final Map<String, Integer> cssaCounters) {
    return cache
        .computeIfAbsent(
            Pair.of(idx, cssaCounters), pair -> new HappensBeforeEdgeTools(idx, cssaCounters))
        .cloneEdge(pCFAEdge);
  }

  static Map<String, Integer> nextCssaCounters(
      final CFAEdge pCFAEdge, final int idx, final Map<String, Integer> cssaCounters) {
    final var astIdQualifier =
        cache.computeIfAbsent(
            Pair.of(idx, cssaCounters), pair -> new HappensBeforeEdgeTools(idx, cssaCounters));
    astIdQualifier.cloneEdge(pCFAEdge);
    return astIdQualifier.mutableCssaCounters;
  }

  static Pair<List<CVariableDeclaration>, List<CVariableDeclaration>> getAccesses(
      final CFAEdge pCFAEdge, final int idx, final Map<String, Integer> cssaCounters) {
    final var astIdQualifier =
        cache.computeIfAbsent(
            Pair.of(idx, cssaCounters), pair -> new HappensBeforeEdgeTools(idx, cssaCounters));
    astIdQualifier.cloneEdge(pCFAEdge);
    return Pair.of(astIdQualifier.writeAccesses, astIdQualifier.readAccesses);
  }

  static CFAEdge createAssume(CFANode node, ExecutionGraph pG) {
    final var exprs = new ArrayList<CExpression>();
    pG.pendingRf()
        .forEach(
            (r, w) ->
                exprs.add(
                    new CBinaryExpression(
                        node.getFunction().getFileLocation(),
                        CNumericTypes.UNSIGNED_INT,
                        CNumericTypes.UNSIGNED_INT,
                        new CIdExpression(w.var().getFileLocation(), w.var()),
                        new CIdExpression(r.var().getFileLocation(), r.var()),
                        BinaryOperator.EQUALS)));
    final var expr =
        exprs.stream()
            .reduce(
                CIntegerLiteralExpression.ONE,
                (expr1, expr2) ->
                    new CBinaryExpression(
                        node.getFunction().getFileLocation(),
                        CNumericTypes.UNSIGNED_INT,
                        CNumericTypes.UNSIGNED_INT,
                        expr1,
                        expr2,
                        BinaryOperator.BITWISE_AND));
    return new CAssumeEdge(
        expr.toASTString(), node.getFunction().getFileLocation(), node, node, expr, false);
  }

  private final SequencedMap<CFAEdge, CFAEdge> edgeCache = new LinkedHashMap<>();
  private final CExpressionCloner expCloner;
  private final int thredId;
  private final Map<String, Integer> mutableCssaCounters;
  private final List<CVariableDeclaration> writeAccesses = new ArrayList<>();
  private final List<CVariableDeclaration> readAccesses = new ArrayList<>();
  private boolean isLhs = false;

  private HappensBeforeEdgeTools(final int idx, final Map<String, Integer> cssaCounters) {
    thredId = idx;
    mutableCssaCounters = new LinkedHashMap<>(cssaCounters);
    expCloner = new CExpressionCloner();
  }

  private CFAEdge cloneEdge(final CFAEdge pCFAEdge) {
    return edgeCache.computeIfAbsent(pCFAEdge, this::cloneEdgeDirect);
  }

  private CFAEdge cloneEdgeDirect(final CFAEdge edge) {
    final FileLocation loc = edge.getFileLocation();
    final CFANode start = edge.getPredecessor();
    final CFANode end = edge.getSuccessor();
    final String rawStatement = edge.getRawStatement();

    return switch (edge.getEdgeType()) {
      case BlankEdge -> edge;

      case AssumeEdge -> {
        if (edge instanceof CAssumeEdge pCAssumeEdge) {
          final var newAst = cloneAstRightSide(pCAssumeEdge.getExpression());
          if (newAst.equals(pCAssumeEdge.getExpression())) {
            yield edge;
          } else {
            yield new CAssumeEdge(
                rawStatement,
                loc,
                start,
                end,
                newAst,
                pCAssumeEdge.getTruthAssumption(),
                pCAssumeEdge.isSwapped(),
                pCAssumeEdge.isArtificialIntermediate());
          }
        } else {
          throw new AssertionError(ONLY_C_SUPPORTED);
        }
      }
      case StatementEdge -> {
        if (edge instanceof CFunctionSummaryStatementEdge pCFunctionSummaryStatementEdge) {
          final var newStatement = cloneAst(pCFunctionSummaryStatementEdge.getStatement());
          final var newFuncCall = cloneAst(pCFunctionSummaryStatementEdge.getFunctionCall());
          if (newStatement.equals(pCFunctionSummaryStatementEdge.getStatement())
              && newFuncCall.equals(pCFunctionSummaryStatementEdge.getFunctionCall())) {
            yield edge;
          } else {
            yield new CFunctionSummaryStatementEdge(
                rawStatement,
                newStatement,
                loc,
                start,
                end,
                newFuncCall,
                pCFunctionSummaryStatementEdge.getFunctionName());
          }
        } else if (edge instanceof CStatementEdge pCStatementEdge) {
          final var newStatement = cloneAst(pCStatementEdge.getStatement());
          if (newStatement.equals(pCStatementEdge.getStatement())) {
            yield edge;
          } else {
            yield new CStatementEdge(rawStatement, newStatement, loc, start, end);
          }
        } else {
          throw new AssertionError(ONLY_C_SUPPORTED);
        }
      }
      case DeclarationEdge -> {
        if (edge instanceof CDeclarationEdge pCDeclarationEdge) {
          final var newDeclaration = cloneAstLeftSide(pCDeclarationEdge.getDeclaration());
          if (newDeclaration.equals(pCDeclarationEdge.getDeclaration())) {
            yield edge;
          } else {
            yield new CDeclarationEdge(rawStatement, loc, start, end, newDeclaration);
          }
        } else {
          throw new AssertionError(ONLY_C_SUPPORTED);
        }
      }
      case ReturnStatementEdge -> {
        assert end instanceof FunctionExitNode
            : "Expected FunctionExitNode: " + end + ", " + end.getClass();
        if (edge instanceof CReturnStatementEdge pCReturnStatementEdge) {
          final var newStatement = cloneAst(pCReturnStatementEdge.getReturnStatement());
          if (newStatement.equals(pCReturnStatementEdge.getReturnStatement())) {
            yield edge;
          } else {
            yield new CReturnStatementEdge(
                rawStatement, newStatement, loc, start, (FunctionExitNode) end);
          }
        } else {
          throw new AssertionError(ONLY_C_SUPPORTED);
        }
      }
      case FunctionCallEdge -> {
        assert end instanceof CFunctionEntryNode
            : "Expected FunctionExitNode: " + end + ", " + end.getClass();
        if (edge instanceof CFunctionCallEdge pCFunctionCallEdge) {
          final var newAst = cloneAst((CFunctionCall) pCFunctionCallEdge.getRawAST().orElseThrow());
          if (newAst.equals(pCFunctionCallEdge.getRawAST().orElseThrow())) {
            yield edge;
          } else {
            yield new CFunctionCallEdge(
                rawStatement,
                loc,
                start,
                (CFunctionEntryNode) end,
                newAst,
                pCFunctionCallEdge.getSummaryEdge());
          }
        } else {
          throw new AssertionError();
        }
      }
      case FunctionReturnEdge -> {
        if (edge instanceof CFunctionReturnEdge pCFunctionReturnEdge) {
          final var newEdge =
              (CFunctionSummaryEdge) cloneEdgeDirect(pCFunctionReturnEdge.getSummaryEdge());
          if (newEdge.equals(pCFunctionReturnEdge.getSummaryEdge())) {
            yield edge;
          } else {
            yield new CFunctionReturnEdge(loc, (FunctionExitNode) start, end, newEdge);
          }
        } else {
          throw new AssertionError(ONLY_C_SUPPORTED);
        }
      }
      case CallToReturnEdge -> {
        if (edge instanceof CFunctionSummaryEdge pCFunctionSummaryEdge) {
          final var newExpr = cloneAst(pCFunctionSummaryEdge.getExpression());
          if (newExpr.equals(pCFunctionSummaryEdge.getExpression())) {
            yield edge;
          } else {
            yield new CFunctionSummaryEdge(
                rawStatement, loc, start, end, newExpr, pCFunctionSummaryEdge.getFunctionEntry());
          }
        } else {
          throw new AssertionError(ONLY_C_SUPPORTED);
        }
      }
    };
  }

  @SuppressWarnings("unchecked")
  private <T extends AAstNode> T cloneAst(final T ast) {
    return (T) cloneAstDirect(ast);
  }

  @SuppressWarnings("unchecked")
  private <T extends AAstNode> T cloneAstRightSide(final T ast) {
    isLhs = false;
    return (T) cloneAstDirect(ast);
  }

  @SuppressWarnings("unchecked")
  private <T extends AAstNode> T cloneAstLeftSide(final T ast) {
    isLhs = true;
    return (T) cloneAstDirect(ast);
  }

  private <T extends AAstNode> List<T> cloneAstList(final List<T> astList) {
    final List<T> list = new ArrayList<>(astList.size());
    for (T ast : astList) {
      list.add(cloneAstRightSide(ast));
    }
    return list;
  }

  private AAstNode cloneAstDirect(AAstNode ast) {
    if (ast == null) {
      return null;
    }

    final FileLocation loc = ast.getFileLocation();

    return switch (ast) {
      // CRightHandSide
      case CExpression cExpression -> cExpression.accept(expCloner);

      case CFunctionCallExpression func ->
          new CFunctionCallExpression(
              loc,
              func.getExpressionType(),
              cloneAst(func.getFunctionNameExpression()),
              cloneAstList(func.getParameterExpressions()),
              cloneAst(func.getDeclaration()));

      // CInitializer
      case CInitializerExpression cInitializerExpression ->
          new CInitializerExpression(
              loc, cloneAstRightSide(cInitializerExpression.getExpression()));
      case CInitializerList cInitializerList ->
          new CInitializerList(loc, cloneAstList(cInitializerList.getInitializers()));
      case CDesignatedInitializer di ->
          new CDesignatedInitializer(
              loc, cloneAstList(di.getDesignators()), cloneAstRightSide(di.getRightHandSide()));

      // CSimpleDeclaration
      case CVariableDeclaration decl -> {
        CVariableDeclaration newDecl =
            new CVariableDeclaration(
                loc,
                decl.isGlobal(),
                decl.getCStorageClass(),
                decl.getType(),
                decl.getName(),
                decl.getOrigName(),
                changeQualifiedName(decl, decl.isGlobal()),
                null);
        if (decl.isGlobal()) {
          final var list = isLhs ? writeAccesses : readAccesses;
          list.add(decl);
        }
        newDecl.addInitializer(cloneAstRightSide(decl.getInitializer()));
        yield newDecl;
      }
      case CFunctionDeclaration decl -> {
        List<CParameterDeclaration> l = new ArrayList<>(decl.getParameters().size());
        for (CParameterDeclaration param : decl.getParameters()) {
          l.add(cloneAstRightSide(param));
        }
        yield new CFunctionDeclaration(
            loc, decl.getType(), decl.getName(), decl.getOrigName(), l, decl.getAttributes());
      }
      case CComplexTypeDeclaration decl ->
          new CComplexTypeDeclaration(loc, decl.isGlobal(), decl.getType());
      case CTypeDefDeclaration decl ->
          new CTypeDefDeclaration(
              loc, decl.isGlobal(), decl.getType(), decl.getName(), decl.getQualifiedName());
      case CParameterDeclaration decl -> {
        CParameterDeclaration newDecl =
            new CParameterDeclaration(loc, decl.getType(), decl.getName());
        newDecl.setQualifiedName(changeQualifiedName(decl, false));
        yield newDecl;
      }
      case CEnumerator decl ->
          new CEnumerator(loc, decl.getName(), decl.getQualifiedName(), decl.getValue());

      // CStatement stmt
      case CFunctionCallAssignmentStatement stat ->
          new CFunctionCallAssignmentStatement(
              loc,
              cloneAstLeftSide(stat.getLeftHandSide()),
              cloneAstRightSide(stat.getRightHandSide()));
      case CExpressionAssignmentStatement stat ->
          new CExpressionAssignmentStatement(
              loc,
              cloneAstLeftSide(stat.getLeftHandSide()),
              cloneAstRightSide(stat.getRightHandSide()));
      case CFunctionCallStatement cFunctionCallStatement ->
          new CFunctionCallStatement(
              loc, cloneAstRightSide(cFunctionCallStatement.getFunctionCallExpression()));
      case CExpressionStatement cExpressionStatement ->
          new CExpressionStatement(loc, cloneAstRightSide(cExpressionStatement.getExpression()));

      case CReturnStatement cReturnStatement -> {
        Optional<CExpression> returnExp = cReturnStatement.getReturnValue();
        if (returnExp.isPresent()) {
          returnExp = Optional.of(cloneAstRightSide(returnExp.orElseThrow()));
        }
        Optional<CAssignment> returnAssignment = cReturnStatement.asAssignment();
        if (returnAssignment.isPresent()) {
          returnAssignment = Optional.of(cloneAst(returnAssignment.orElseThrow()));
        }
        yield new CReturnStatement(loc, returnExp, returnAssignment);
      }

      // CDesignator designator
      case CArrayDesignator cArrayDesignator ->
          new CArrayDesignator(loc, cloneAstRightSide(cArrayDesignator.getSubscriptExpression()));
      case CArrayRangeDesignator cArrayRangeDesignator ->
          new CArrayRangeDesignator(
              loc,
              cloneAstRightSide(cArrayRangeDesignator.getFloorExpression()),
              cloneAstRightSide(cArrayRangeDesignator.getCeilExpression()));
      case CFieldDesignator cFieldDesignator ->
          new CFieldDesignator(loc, cFieldDesignator.getFieldName());

      default -> throw new AssertionError("unhandled ASTNode " + ast + " of " + ast.getClass());
    };
  }

  private String changeQualifiedName(CSimpleDeclaration decl, boolean isGlobal) {
    if (isGlobal) {
      final var nextId = mutableCssaCounters.computeIfAbsent(decl.getQualifiedName(), s -> 0) + 1;
      mutableCssaCounters.put(decl.getQualifiedName(), nextId);
      return "%s_%d".formatted(decl.getQualifiedName(), nextId);
    }
    return "T%d_%s".formatted(thredId, decl.getQualifiedName());
  }

  private class CExpressionCloner extends DefaultCExpressionVisitor<CExpression, NoException> {

    @Override
    protected CExpression visitDefault(CExpression exp) {
      return exp;
    }

    @Override
    public CExpression visit(CBinaryExpression exp) {
      return new CBinaryExpression(
          exp.getFileLocation(),
          exp.getExpressionType(),
          exp.getCalculationType(),
          exp.getOperand1().accept(this),
          exp.getOperand2().accept(this),
          exp.getOperator());
    }

    @Override
    public CExpression visit(CCastExpression exp) {
      return new CCastExpression(
          exp.getFileLocation(), exp.getExpressionType(), exp.getOperand().accept(this));
    }

    @Override
    public CExpression visit(CUnaryExpression exp) {
      return new CUnaryExpression(
          exp.getFileLocation(),
          exp.getExpressionType(),
          exp.getOperand().accept(this),
          exp.getOperator());
    }

    @Override
    public CExpression visit(CArraySubscriptExpression exp) {
      return new CArraySubscriptExpression(
          exp.getFileLocation(),
          exp.getExpressionType(),
          exp.getArrayExpression().accept(this),
          exp.getSubscriptExpression().accept(this));
    }

    @Override
    public CExpression visit(CFieldReference exp) {
      return new CFieldReference(
          exp.getFileLocation(),
          exp.getExpressionType(),
          exp.getFieldName(),
          exp.getFieldOwner().accept(this),
          exp.isPointerDereference());
    }

    @Override
    public CExpression visit(CIdExpression exp) {
      if (exp.getExpressionType() instanceof CFunctionType) {
        return new CIdExpression(
            exp.getFileLocation(),
            exp.getExpressionType(),
            exp.getName(),
            cloneAst(exp.getDeclaration()));
      } else {
        return new CIdExpression(
            exp.getFileLocation(),
            exp.getExpressionType(),
            exp.getName(),
            cloneAst(exp.getDeclaration()));
      }
    }

    @Override
    public CExpression visit(CPointerExpression exp) {
      return new CPointerExpression(
          exp.getFileLocation(), exp.getExpressionType(), exp.getOperand().accept(this));
    }

    @Override
    public CExpression visit(CComplexCastExpression exp) {
      return new CComplexCastExpression(
          exp.getFileLocation(),
          exp.getExpressionType(),
          exp.getOperand().accept(this),
          exp.getType(),
          exp.isRealCast());
    }
  }
}
