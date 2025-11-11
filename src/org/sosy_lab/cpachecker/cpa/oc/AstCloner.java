// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.oc;

import static org.sosy_lab.cpachecker.cpa.oc.EventType.READ;
import static org.sosy_lab.cpachecker.cpa.oc.EventType.WRITE;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

/** Helper class to clone C ASTs and expressions. Package-private and used by EdgeCloner. */
class AstCloner {

  private static int mutableUniqueCounter = 0;

  private final int threadId;
  private boolean isLhs = false;
  private final List<MemoryEvent> memoryEvents;

  AstCloner(int pThreadId, List<MemoryEvent> pMemoryEvents) {
    this.threadId = pThreadId;
    this.memoryEvents = pMemoryEvents;
  }

  CExpression cloneExpression(CExpression exp) {
    return exp.accept(new CExpressionCloner());
  }

  @SuppressWarnings("unchecked")
  <T extends AAstNode> T cloneAst(T ast) {
    return (T) cloneAstDirect(ast);
  }

  @SuppressWarnings("unchecked")
  <T extends AAstNode> T cloneAstRightSide(T ast) {
    isLhs = false;
    return (T) cloneAstDirect(ast);
  }

  @SuppressWarnings("unchecked")
  <T extends AAstNode> T cloneAstLeftSide(T ast) {
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

    if (ast instanceof CRightHandSide) {

      if (ast instanceof CExpression expr) {
        return expr.accept(new CExpressionCloner());

      } else if (ast instanceof CFunctionCallExpression func) {
        return new CFunctionCallExpression(
            loc,
            func.getExpressionType(),
            cloneAst(func.getFunctionNameExpression()),
            cloneAstList(func.getParameterExpressions()),
            cloneAst(func.getDeclaration()));
      }

    } else if (ast instanceof CInitializer) {

      if (ast instanceof CInitializerExpression initExp) {
        return new CInitializerExpression(loc, cloneAstRightSide(initExp.getExpression()));

      } else if (ast instanceof CInitializerList initList) {
        return new CInitializerList(loc, cloneAstList(initList.getInitializers()));

      } else if (ast instanceof CDesignatedInitializer di) {
        return new CDesignatedInitializer(
            loc, cloneAstList(di.getDesignators()), cloneAstRightSide(di.getRightHandSide()));
      }

    } else if (ast instanceof CSimpleDeclaration) {

      if (ast instanceof CVariableDeclaration decl) {
        CVariableDeclaration newDecl = (CVariableDeclaration) createNewDeclaration(decl);
        newDecl.addInitializer(cloneAstRightSide(decl.getInitializer()));
        return newDecl;

      } else if (ast instanceof CFunctionDeclaration decl) {
        List<CParameterDeclaration> l = new ArrayList<>(decl.getParameters().size());
        for (CParameterDeclaration param : decl.getParameters()) {
          l.add(cloneAstRightSide(param));
        }
        return new CFunctionDeclaration(
            loc, decl.getType(), decl.getName(), decl.getOrigName(), l, decl.getAttributes());

      } else if (ast instanceof CComplexTypeDeclaration decl) {
        return new CComplexTypeDeclaration(loc, decl.isGlobal(), decl.getType());

      } else if (ast instanceof CTypeDefDeclaration decl) {
        return new CTypeDefDeclaration(
            loc, decl.isGlobal(), decl.getType(), decl.getName(), decl.getQualifiedName());

      } else if (ast instanceof CParameterDeclaration decl) {
        CParameterDeclaration newDecl =
            new CParameterDeclaration(loc, decl.getType(), decl.getName());
        newDecl.setQualifiedName(changeQualifiedName(decl, false));
        return newDecl;

      } else if (ast instanceof CEnumerator decl) {
        return new CEnumerator(loc, decl.getName(), decl.getQualifiedName(), decl.getValue());
      }

    } else if (ast instanceof CStatement) {

      if (ast instanceof CFunctionCallAssignmentStatement stat) {
        return new CFunctionCallAssignmentStatement(
            loc,
            cloneAstLeftSide(stat.getLeftHandSide()),
            cloneAstRightSide(stat.getRightHandSide()));

      } else if (ast instanceof CExpressionAssignmentStatement stat) {
        return new CExpressionAssignmentStatement(
            loc,
            cloneAstLeftSide(stat.getLeftHandSide()),
            cloneAstRightSide(stat.getRightHandSide()));

      } else if (ast instanceof CFunctionCallStatement funcCallStmt) {
        return new CFunctionCallStatement(
            loc, cloneAstRightSide(funcCallStmt.getFunctionCallExpression()));

      } else if (ast instanceof CExpressionStatement exprStmt) {
        return new CExpressionStatement(loc, cloneAstRightSide(exprStmt.getExpression()));
      }

    } else if (ast instanceof CReturnStatement ret) {
      Optional<CExpression> returnExp = ret.getReturnValue();
      if (returnExp.isPresent()) {
        returnExp = Optional.of(cloneAstRightSide(returnExp.orElseThrow()));
      }
      Optional<CAssignment> returnAssignment = ret.asAssignment();
      if (returnAssignment.isPresent()) {
        returnAssignment = Optional.of(cloneAst(returnAssignment.orElseThrow()));
      }
      return new CReturnStatement(loc, returnExp, returnAssignment);

    } else if (ast instanceof CDesignator) {

      if (ast instanceof CArrayDesignator arr) {
        return new CArrayDesignator(loc, cloneAstRightSide(arr.getSubscriptExpression()));

      } else if (ast instanceof CArrayRangeDesignator range) {
        return new CArrayRangeDesignator(
            loc,
            cloneAstRightSide(range.getFloorExpression()),
            cloneAstRightSide(range.getCeilExpression()));

      } else if (ast instanceof CFieldDesignator field) {
        return new CFieldDesignator(loc, field.getFieldName());
      }
    }

    throw new AssertionError("unhandled ASTNode " + ast + " of " + ast.getClass());
  }

  private CSimpleDeclaration createNewDeclaration(CSimpleDeclaration cDecl) {
    if (cDecl instanceof CVariableDeclaration decl) {
      FileLocation loc = decl.getFileLocation();
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
        final var type = isLhs ? WRITE : READ;
        final var id = ++mutableUniqueCounter;
        memoryEvents.add(
            new MemoryEvent(
                id,
                MemoryLocation.forDeclaration(decl),
                newDecl.getQualifiedName(),
                Optional.empty(),
                type));
      }
      return newDecl;
    } else {
      // is this always good?
      return cDecl;
    }
  }

  private String changeQualifiedName(CSimpleDeclaration decl, boolean isGlobal) {
    if (isGlobal) {
      final var nextId = ++mutableUniqueCounter;
      return "%s_%d".formatted(decl.getQualifiedName(), nextId);
    }
    return "T%d_%s".formatted(threadId, decl.getQualifiedName());
  }

  public List<MemoryEvent> getMemoryEvents() {
    return memoryEvents;
  }

  private class CExpressionCloner extends DefaultCExpressionVisitor<CExpression, RuntimeException> {

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
            createNewDeclaration(exp.getDeclaration()));
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
