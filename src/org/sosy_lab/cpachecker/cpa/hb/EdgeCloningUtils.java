// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.hb;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArrayRangeDesignator;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexTypeDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignator;
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
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CReturnStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CTypeDefDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
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
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.DefaultCTypeVisitor;
import org.sosy_lab.cpachecker.exceptions.NoException;
import org.sosy_lab.cpachecker.util.Pair;

final class EdgeCloningUtils {
  private static final String ONLY_C_SUPPORTED = "only C supported";
  private static final Map<Integer, EdgeCloningUtils> lut = new LinkedHashMap<>();
  private static final Map<Pair<CFAEdge, Integer>, CFAEdge> clones = new LinkedHashMap<>();

  private static EdgeCloningUtils create(final int pIdx) {
    return lut.computeIfAbsent(pIdx, (idx) -> new EdgeCloningUtils(idx));
  }

  static CFAEdge clone(final CFAEdge pCFAEdge, final int idx) {
    if (idx == 0) {
      return pCFAEdge;
    } else {
      return clones.computeIfAbsent(Pair.of(pCFAEdge, idx), (pair) -> EdgeCloningUtils.create(pair.getSecondNotNull()).cloneEdge(pair.getFirstNotNull()));
    }
  }

  private final int idx;

  private final IdentityHashMap<AAstNode, AAstNode> astCache = new IdentityHashMap<>();
  private final IdentityHashMap<Type, Type> typeCache = new IdentityHashMap<>();
  private final CExpressionCloner expCloner = new CExpressionCloner();
  private final CTypeCloner typeCloner = new CTypeCloner();

  private EdgeCloningUtils(final int pIdx) {
    idx = pIdx;
  }

  private CFAEdge cloneEdge(final CFAEdge edge) {

    final FileLocation loc = edge.getFileLocation();
    final CFANode start = edge.getPredecessor();
    final CFANode end = edge.getSuccessor();
    final String rawStatement = edge.getRawStatement();

    switch (edge.getEdgeType()) {
      case BlankEdge: return new BlankEdge(rawStatement, loc, start, end, edge.getDescription());
      case AssumeEdge: if (edge instanceof CAssumeEdge pCAssumeEdge) {
          return new CAssumeEdge(
                  rawStatement,
                  loc,
                  start,
                  end,
                  cloneAst(pCAssumeEdge.getExpression()),
                  pCAssumeEdge.getTruthAssumption(),
                  pCAssumeEdge.isSwapped(),
                  pCAssumeEdge.isArtificialIntermediate());
        } else {
          throw new AssertionError(ONLY_C_SUPPORTED);
        }
      case StatementEdge:
      {
        if (edge instanceof CFunctionSummaryStatementEdge pCFunctionSummaryStatementEdge) {
          return new CFunctionSummaryStatementEdge(
              rawStatement,
              cloneAst(pCFunctionSummaryStatementEdge.getStatement()),
              loc,
              start,
              end,
              cloneAst(pCFunctionSummaryStatementEdge.getFunctionCall()),
              pCFunctionSummaryStatementEdge.getFunctionName()
          );
        } else if (edge instanceof CStatementEdge pCStatementEdge) {
          return new CStatementEdge(
                  rawStatement,
                  cloneAst(pCStatementEdge.getStatement()),
                  loc,
                  start,
                  end);
        } else {
          throw new AssertionError(ONLY_C_SUPPORTED);
        }
      }

      case DeclarationEdge:
      {
        if (edge instanceof CDeclarationEdge pCDeclarationEdge) {
          return new CDeclarationEdge(
                  rawStatement,
                  loc,
                  start,
                  end,
                  cloneAst(pCDeclarationEdge.getDeclaration()));
        } else {
          throw new AssertionError(ONLY_C_SUPPORTED);
        }
      }

      case ReturnStatementEdge:
      {
        assert end instanceof FunctionExitNode
            : "Expected FunctionExitNode: " + end + ", " + end.getClass();
        if (edge instanceof CReturnStatementEdge pCReturnStatementEdge) {
          return new CReturnStatementEdge(
                  rawStatement,
                  cloneAst(pCReturnStatementEdge.getReturnStatement()),
                  loc,
                  start,
                  (FunctionExitNode) end);
        } else {
          throw new AssertionError(ONLY_C_SUPPORTED);
        }
      }

      case FunctionCallEdge:
      {
        assert end instanceof CFunctionEntryNode
            : "Expected FunctionExitNode: " + end + ", " + end.getClass();
        if (edge instanceof CFunctionCallEdge pCFunctionCallEdge) {
          return new CFunctionCallEdge(
              rawStatement,
              loc,
              start,
              (CFunctionEntryNode) end,
              cloneAst((CFunctionCall) pCFunctionCallEdge.getRawAST().get()),
              pCFunctionCallEdge.getSummaryEdge());
        } else {
          throw new AssertionError();
        }
      }

      case FunctionReturnEdge:
      {
        if (edge instanceof CFunctionReturnEdge pCFunctionReturnEdge) {
          return new CFunctionReturnEdge(
              loc,
              (FunctionExitNode) start,
              end,
              (CFunctionSummaryEdge) cloneEdge(pCFunctionReturnEdge.getSummaryEdge()));
        } else {
          throw new AssertionError(ONLY_C_SUPPORTED);
        }
      }

      case CallToReturnEdge:
      {
        if (edge instanceof CFunctionSummaryEdge pCFunctionSummaryEdge) {
          return new CFunctionSummaryEdge(
              rawStatement,
              loc,
              start,
              end,
              cloneAst(pCFunctionSummaryEdge.getExpression()),
              pCFunctionSummaryEdge.getFunctionEntry());
        } else {
          throw new AssertionError(ONLY_C_SUPPORTED);
        }
      }

      default:
        throw new AssertionError("unhandled type of edge: " + edge.getEdgeType());
    }
  }

  private  @Nullable <T extends AAstNode> T cloneAst(final T ast) {

    if (ast == null) {
      return null;
    }

    if (astCache.containsKey(ast)) {
      return (T) astCache.get(ast);
    }

    final AAstNode newAst = cloneAstDirect(ast);

    astCache.put(ast, newAst);

    return (T) newAst;
  }

  private <T extends AAstNode> List<T> cloneAstList(final List<T> astList) {
    final List<T> list = new ArrayList<>(astList.size());
    for (T ast : astList) {
      list.add(cloneAst(ast));
    }
    return list;
  }

  /** returns a deep copy of the ast-node, and changes old functionname to new one, if needed. */
  private AAstNode cloneAstDirect(AAstNode ast) {

    final FileLocation loc = ast.getFileLocation();

    if (ast instanceof CRightHandSide) {

      if (ast instanceof CExpression) {
        return ((CExpression) ast).accept(expCloner);

      } else if (ast instanceof CFunctionCallExpression func) {
        return new CFunctionCallExpression(
            loc,
            cloneType(func.getExpressionType()),
            cloneAst(func.getFunctionNameExpression()),
            cloneAstList(func.getParameterExpressions()),
            cloneAst(func.getDeclaration()));
      }

    } else if (ast instanceof CInitializer) {

      if (ast instanceof CInitializerExpression) {
        return new CInitializerExpression(
            loc, cloneAst(((CInitializerExpression) ast).getExpression()));

      } else if (ast instanceof CInitializerList) {
        return new CInitializerList(loc, cloneAstList(((CInitializerList) ast).getInitializers()));

      } else if (ast instanceof CDesignatedInitializer di) {
        return new CDesignatedInitializer(
            loc, cloneAstList(di.getDesignators()), cloneAst(di.getRightHandSide()));
      }

    } else if (ast instanceof CSimpleDeclaration) {

      if (ast instanceof CVariableDeclaration decl) {
        CVariableDeclaration newDecl =
            new CVariableDeclaration(
                loc,
                decl.isGlobal(),
                decl.getCStorageClass(),
                cloneType(decl.getType()),
                decl.getName(),
                decl.getOrigName(),
                changeQualifiedName(decl.getQualifiedName()),
                null);
        // cache the declaration, then clone the initializer and add it.
        // this is needed for the following code: int x = x;
        astCache.put(ast, newDecl);
        newDecl.addInitializer(cloneAst(decl.getInitializer()));
        return newDecl;

      } else if (ast instanceof CFunctionDeclaration decl) {
        List<CParameterDeclaration> l = new ArrayList<>(decl.getParameters().size());
        for (CParameterDeclaration param : decl.getParameters()) {
          l.add(cloneAst(param));
        }
        return new CFunctionDeclaration(
            loc,
            cloneType(decl.getType()),
            decl.getName(),
            decl.getOrigName(),
            l,
            decl.getAttributes());

      } else if (ast instanceof CComplexTypeDeclaration decl) {
        return new CComplexTypeDeclaration(loc, decl.isGlobal(), cloneType(decl.getType()));

      } else if (ast instanceof CTypeDefDeclaration decl) {
        return new CTypeDefDeclaration(
            loc,
            decl.isGlobal(),
            cloneType(decl.getType()),
            decl.getName(),
            changeQualifiedName(decl.getQualifiedName()));

      } else if (ast instanceof CParameterDeclaration decl) {
        // we do not cache CParameterDeclaration, but clone it directly,
        // because its equals- and hashcode-Method are insufficient for caching
        // TODO do we need to cache it?
        CParameterDeclaration newDecl =
            new CParameterDeclaration(loc, cloneType(decl.getType()), decl.getName());
        newDecl.setQualifiedName(changeQualifiedName(decl.getQualifiedName()));
        return newDecl;

      } else if (ast instanceof CEnumerator decl) {
        return new CEnumerator(
            loc, decl.getName(), changeQualifiedName(decl.getQualifiedName()), decl.getValue());
      }

    } else if (ast instanceof CStatement) {

      if (ast instanceof CFunctionCallAssignmentStatement stat) {
        return new CFunctionCallAssignmentStatement(
            loc, cloneAst(stat.getLeftHandSide()), cloneAst(stat.getRightHandSide()));

      } else if (ast instanceof CExpressionAssignmentStatement stat) {
        return new CExpressionAssignmentStatement(
            loc, cloneAst(stat.getLeftHandSide()), cloneAst(stat.getRightHandSide()));

      } else if (ast instanceof CFunctionCallStatement) {
        return new CFunctionCallStatement(
            loc, cloneAst(((CFunctionCallStatement) ast).getFunctionCallExpression()));

      } else if (ast instanceof CExpressionStatement) {
        return new CExpressionStatement(
            loc, cloneAst(((CExpressionStatement) ast).getExpression()));
      }

    } else if (ast instanceof CReturnStatement) {
      Optional<CExpression> returnExp = ((CReturnStatement) ast).getReturnValue();
      if (returnExp.isPresent()) {
        returnExp = Optional.of(cloneAst(returnExp.orElseThrow()));
      }
      Optional<CAssignment> returnAssignment = ((CReturnStatement) ast).asAssignment();
      if (returnAssignment.isPresent()) {
        returnAssignment = Optional.of(cloneAst(returnAssignment.orElseThrow()));
      }
      return new CReturnStatement(loc, returnExp, returnAssignment);

    } else if (ast instanceof CDesignator) {

      if (ast instanceof CArrayDesignator) {
        return new CArrayDesignator(
            loc, cloneAst(((CArrayDesignator) ast).getSubscriptExpression()));

      } else if (ast instanceof CArrayRangeDesignator) {
        return new CArrayRangeDesignator(
            loc,
            cloneAst(((CArrayRangeDesignator) ast).getFloorExpression()),
            cloneAst(((CArrayRangeDesignator) ast).getCeilExpression()));

      } else if (ast instanceof CFieldDesignator) {
        return new CFieldDesignator(loc, ((CFieldDesignator) ast).getFieldName());
      }
    }

    throw new AssertionError("unhandled ASTNode " + ast + " of " + ast.getClass());
  }

  private String changeQualifiedName(String pQualifiedName) {
    return new StringBuilder()
        .append("T")
        .append(idx)
        .append("::")
        .append(pQualifiedName)
        .toString();
  }

  @SuppressWarnings("unchecked")
  private @Nullable <T extends Type> T cloneType(T type) {

    if (type == null) {
      return null;
    }

    if (typeCache.containsKey(type)) {
      return (T) typeCache.get(type);
    }

    final Type newType = cloneTypeDirect(type);

    typeCache.put(type, newType);

    return (T) newType;
  }

  private Type cloneTypeDirect(Type type) {
    if (type instanceof CType) {
      return ((CType) type).accept(typeCloner);
    }

    throw new AssertionError("unhandled Type " + type + " of " + type.getClass());
  }

  /**
   * clones CExpressions and calls cloneAst on non-expression-content. Note: caching sub-expressions
   * is useless because of the location, that is different for each expression.
   */
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
          exp.getFileLocation(), cloneType(exp.getExpressionType()), exp.getOperand().accept(this));
    }

    @Override
    public CExpression visit(CUnaryExpression exp) {
      return new CUnaryExpression(
          exp.getFileLocation(),
          cloneType(exp.getExpressionType()),
          exp.getOperand().accept(this),
          exp.getOperator());
    }

    @Override
    public CExpression visit(CArraySubscriptExpression exp) {
      return new CArraySubscriptExpression(
          exp.getFileLocation(),
          cloneType(exp.getExpressionType()),
          exp.getArrayExpression().accept(this),
          exp.getSubscriptExpression().accept(this));
    }

    @Override
    public CExpression visit(CFieldReference exp) {
      return new CFieldReference(
          exp.getFileLocation(),
          cloneType(exp.getExpressionType()),
          exp.getFieldName(),
          exp.getFieldOwner().accept(this),
          exp.isPointerDereference());
    }

    @Override
    public CExpression visit(CIdExpression exp) {
      // check for self-recursion --> replace self-calling functioncalls with new self-calling
      // functioncalls
      if (exp.getExpressionType() instanceof CFunctionType) {
        return new CIdExpression(
            exp.getFileLocation(),
            cloneType(exp.getExpressionType()),
            exp.getName(),
            cloneAst(exp.getDeclaration()));
      } else {
        return new CIdExpression(
            exp.getFileLocation(),
            cloneType(exp.getExpressionType()),
            exp.getName(),
            cloneAst(exp.getDeclaration()));
      }
    }

    @Override
    public CExpression visit(CPointerExpression exp) {
      return new CPointerExpression(
          exp.getFileLocation(), cloneType(exp.getExpressionType()), exp.getOperand().accept(this));
    }

    @Override
    public CExpression visit(CComplexCastExpression exp) {
      return new CComplexCastExpression(
          exp.getFileLocation(),
          cloneType(exp.getExpressionType()),
          exp.getOperand().accept(this),
          exp.getType(),
          exp.isRealCast());
    }
  }

  private class CTypeCloner extends DefaultCTypeVisitor<CType, NoException> {

    @Override
    public CType visitDefault(CType t) {
      return t;
    }

    @Override
    public CType visit(CArrayType type) {
      return new CArrayType(
          type.isConst(), type.isVolatile(), type.getType().accept(this), type.getLength());
    }

    @Override
    public CType visit(CCompositeType type) {
      // possible problem: compositeType contains itself again -> recursion
      // solution: cache the empty compositeType and fill it later.
      CCompositeType comp =
          new CCompositeType(
              type.isConst(),
              type.isVolatile(),
              type.getKind(),
              type.getName(),
              type.getOrigName());
      typeCache.put(type, comp);

      // convert members and set them
      List<CCompositeTypeMemberDeclaration> l = new ArrayList<>(type.getMembers().size());
      for (CCompositeTypeMemberDeclaration decl : type.getMembers()) {
        l.add(new CCompositeTypeMemberDeclaration(decl.getType().accept(this), decl.getName()));
      }
      comp.setMembers(l);

      return comp;
    }

    @Override
    public CType visit(CElaboratedType type) {
      return new CElaboratedType(
          type.isConst(),
          type.isVolatile(),
          type.getKind(),
          type.getName(),
          type.getOrigName(),
          cloneType(type.getRealType()));
    }

    @Override
    public CType visit(CEnumType type) {
      List<CEnumerator> l = new ArrayList<>(type.getEnumerators().size());
      for (CEnumerator e : type.getEnumerators()) {
        l.add(
            new CEnumerator(
                e.getFileLocation(),
                e.getName(),
                changeQualifiedName(e.getQualifiedName()),
                e.getValue()));
      }
      CEnumType enumType =
          new CEnumType(
              type.isConst(),
              type.isVolatile(),
              type.getCompatibleType(),
              l,
              type.getName(),
              type.getOrigName());
      l.forEach(e -> e.setEnum(enumType));
      return enumType;
    }

    @Override
    public CType visit(CFunctionType type) {
      final CFunctionType funcType;
      if (type instanceof CFunctionTypeWithNames) {
        List<CParameterDeclaration> l = new ArrayList<>(type.getParameters().size());
        for (CParameterDeclaration param :
            ((CFunctionTypeWithNames) type).getParameterDeclarations()) {
          l.add(cloneAst(param));
        }
        funcType = new CFunctionTypeWithNames(type.getReturnType(), l, type.takesVarArgs());
      } else {
        assert type.getClass() == CFunctionType.class;
        List<CType> l = new ArrayList<>(type.getParameters().size());
        for (CType param : type.getParameters()) {
          l.add(cloneType(param));
        }
        funcType = new CFunctionType(type.getReturnType(), l, type.takesVarArgs());
      }
      if (type.getName() != null) {
        funcType.setName(type.getName());
      }
      return funcType;
    }

    @Override
    public CType visit(CPointerType type) {
      return new CPointerType(type.isConst(), type.isVolatile(), type.getType().accept(this));
    }

    @Override
    public CType visit(CTypedefType type) {
      return new CTypedefType(
          type.isConst(), type.isVolatile(), type.getName(), type.getRealType().accept(this));
    }

    @Override
    public CType visit(CBitFieldType pCBitFieldType) {
      return new CBitFieldType(
          pCBitFieldType.getType().accept(this), pCBitFieldType.getBitFieldSize());
    }
  }




}
