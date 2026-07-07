// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.por;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;

/**
 * AST cloner for POR. Locals and parameters are always renamed to {@code
 * T{threadId}_{qualifiedName}}. Globals keep their original name unless a {@link
 * GlobalAccessRenamer} is supplied, in which case every single access to a global variable is
 * instead renamed to a fresh, access-specific name ("concurrent SSA"); with no renamer, cloning is
 * bit-identical to the plain thread-ID renaming.
 */
class PorAstCloner {

  private final int threadId;
  private final @Nullable GlobalAccessRenamer globalRenamer;

  /** Whether the AST currently being cloned is on the left-hand side of an assignment/write. */
  private boolean isLhs = false;

  PorAstCloner(int pThreadId) {
    this.threadId = pThreadId;
    this.globalRenamer = null;
  }

  PorAstCloner(int pThreadId, GlobalAccessRenamer pGlobalRenamer) {
    this.threadId = pThreadId;
    this.globalRenamer = checkNotNull(pGlobalRenamer);
  }

  @SuppressWarnings("unchecked")
  <T extends AAstNode> T cloneAst(T ast) {
    return (T) cloneAstDirect(ast);
  }

  /** Clones a sub-expression as an rvalue (read); exposed to a {@link GlobalAccessRenamer}. */
  private CExpression cloneRvalue(CExpression pExpression) {
    return cloneAstRightSide(pExpression);
  }

  @SuppressWarnings("unchecked")
  <T extends AAstNode> T cloneAstRightSide(T ast) {
    boolean oldIsLhs = isLhs;
    isLhs = false;
    try {
      return (T) cloneAstDirect(ast);
    } finally {
      isLhs = oldIsLhs;
    }
  }

  @SuppressWarnings("unchecked")
  <T extends AAstNode> T cloneAstLeftSide(T ast) {
    boolean oldIsLhs = isLhs;
    isLhs = true;
    try {
      return (T) cloneAstDirect(ast);
    } finally {
      isLhs = oldIsLhs;
    }
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
        // Declaring a variable is always an initializing write, regardless of the ambient isLhs
        // (a caller may clone the declaration via the plain cloneAst entry point).
        CVariableDeclaration newDecl = (CVariableDeclaration) createNewDeclaration(decl, true);
        if (decl == newDecl) {
          return decl;
        }
        newDecl.addInitializer(cloneAstRightSide(decl.getInitializer()));
        return newDecl;

      } else if (ast instanceof CFunctionDeclaration decl) {
        List<CParameterDeclaration> l = new ArrayList<>(decl.getParameters().size());
        for (CParameterDeclaration param : decl.getParameters()) {
          l.add(cloneAstRightSide(param));
        }
        return new CFunctionDeclaration(
            loc,
            cloneFunctionType(decl.getType()),
            decl.getName(),
            decl.getOrigName(),
            l,
            decl.getAttributes());

      } else if (ast instanceof CComplexTypeDeclaration decl) {
        return new CComplexTypeDeclaration(loc, decl.isGlobal(), decl.getType());

      } else if (ast instanceof CTypeDefDeclaration decl) {
        return new CTypeDefDeclaration(
            loc, decl.isGlobal(), decl.getType(), decl.getName(), decl.getQualifiedName());

      } else if (ast instanceof CParameterDeclaration decl) {
        CParameterDeclaration newDecl =
            new CParameterDeclaration(loc, decl.getType(), decl.getName());
        newDecl.setQualifiedName(changeQualifiedName(decl));
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
      // Clone the assignment form only once and reuse its right-hand side as the return value:
      // both refer to the same expression, and a stateful renamer must see each access only once.
      Optional<CAssignment> returnAssignment = ret.asAssignment();
      if (returnAssignment.isPresent()
          && returnAssignment.orElseThrow().getRightHandSide() instanceof CExpression) {
        CAssignment clonedAssignment = cloneAst(returnAssignment.orElseThrow());
        Optional<CExpression> returnExp =
            ret.getReturnValue().isPresent()
                ? Optional.of((CExpression) clonedAssignment.getRightHandSide())
                : Optional.empty();
        return new CReturnStatement(loc, returnExp, Optional.of(clonedAssignment));
      }
      Optional<CExpression> returnExp = ret.getReturnValue();
      if (returnExp.isPresent()) {
        returnExp = Optional.of(cloneAstRightSide(returnExp.orElseThrow()));
      }
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

  private CFunctionType cloneFunctionType(CFunctionType type) {
    if (!(type instanceof CFunctionTypeWithNames functionTypeWithNames)) {
      return type;
    }

    Collection<CParameterDeclaration> parameters = functionTypeWithNames.getParameterDeclarations();
    List<CParameterDeclaration> l = new ArrayList<>(parameters.size());
    for (CParameterDeclaration param : parameters) {
      l.add(cloneAstRightSide(param));
    }
    return new CFunctionTypeWithNames(type.getReturnType(), l, type.takesVarArgs());
  }

  private CSimpleDeclaration createNewDeclaration(CSimpleDeclaration cDecl) {
    return createNewDeclaration(cDecl, isLhs);
  }

  /**
   * Creates the renamed declaration to use for one access to {@code cDecl}. Locals and parameters
   * always get the thread-ID prefix. Globals keep their original name unless {@link #globalRenamer}
   * is set, in which case {@code pIsWrite} is forwarded to it so every single access gets its own
   * fresh name.
   */
  private CSimpleDeclaration createNewDeclaration(CSimpleDeclaration cDecl, boolean pIsWrite) {
    FileLocation loc = cDecl.getFileLocation();
    if (cDecl instanceof CVariableDeclaration decl && decl.isGlobal()) {
      if (globalRenamer == null) {
        return decl;
      }
      return new CVariableDeclaration(
          loc,
          true,
          decl.getCStorageClass(),
          decl.getType(),
          decl.getName(),
          decl.getOrigName(),
          globalRenamer.freshName(decl, pIsWrite),
          null);
    } else if (cDecl instanceof CVariableDeclaration decl) {
      CVariableDeclaration newDecl =
          new CVariableDeclaration(
              loc,
              false,
              decl.getCStorageClass(),
              decl.getType(),
              decl.getName(),
              decl.getOrigName(),
              changeQualifiedName(decl),
              null);
      return newDecl;
    } else if (cDecl instanceof CParameterDeclaration param) {
      final CParameterDeclaration newParam =
          new CParameterDeclaration(loc, param.getType(), param.getName());
      newParam.setQualifiedName(changeQualifiedName(param));
      return newParam;
    } else {
      return cDecl;
    }
  }

  /**
   * Renames a local variable or parameter with a thread-ID prefix. Never called for globals: those
   * are either left unchanged or renamed via {@link #globalRenamer} instead.
   */
  private String changeQualifiedName(CSimpleDeclaration decl) {
    return "T%d_%s".formatted(threadId, decl.getQualifiedName());
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
      if (globalRenamer != null && exp.getOperator() == CUnaryExpression.UnaryOperator.AMPER) {
        CExpression replacement = globalRenamer.replaceAddressOf(exp);
        if (replacement != null) {
          return replacement;
        }
      }
      return new CUnaryExpression(
          exp.getFileLocation(),
          exp.getExpressionType(),
          exp.getOperand().accept(this),
          exp.getOperator());
    }

    @Override
    public CExpression visit(CArraySubscriptExpression exp) {
      boolean accessIsWrite = isLhs;
      if (globalRenamer != null) {
        CIdExpression replacement =
            globalRenamer.replaceAliasedAccess(exp, accessIsWrite, PorAstCloner.this::cloneRvalue);
        if (replacement != null) {
          return replacement;
        }
      }
      // the operands are always reads regardless of the ambient access direction
      return new CArraySubscriptExpression(
          exp.getFileLocation(),
          exp.getExpressionType(),
          cloneAstRightSide(exp.getArrayExpression()),
          cloneAstRightSide(exp.getSubscriptExpression()));
    }

    @Override
    public CExpression visit(CFieldReference exp) {
      boolean accessIsWrite = isLhs;
      if (globalRenamer != null) {
        CIdExpression replacement =
            globalRenamer.replaceAliasedAccess(exp, accessIsWrite, PorAstCloner.this::cloneRvalue);
        if (replacement != null) {
          return replacement;
        }
      }
      return new CFieldReference(
          exp.getFileLocation(),
          exp.getExpressionType(),
          exp.getFieldName(),
          cloneAstRightSide(exp.getFieldOwner()),
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
      boolean accessIsWrite = isLhs;
      if (globalRenamer != null) {
        CIdExpression replacement =
            globalRenamer.replaceAliasedAccess(exp, accessIsWrite, PorAstCloner.this::cloneRvalue);
        if (replacement != null) {
          return replacement;
        }
      }
      // the pointer itself is always read regardless of the ambient access direction
      return new CPointerExpression(
          exp.getFileLocation(), exp.getExpressionType(), cloneAstRightSide(exp.getOperand()));
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
