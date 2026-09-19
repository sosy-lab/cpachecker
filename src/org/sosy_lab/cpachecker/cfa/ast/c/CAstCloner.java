// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.exceptions.NoException;

/**
 * Deep-copies a C AST. The traversal itself is fixed here; everything a caller may want to change
 * about a copy is an overridable hook:
 *
 * <ul>
 *   <li>renaming: {@link #changeFunctionName}, {@link #changeQualifiedName}, {@link
 *       #changeTypeQualifiedName}, {@link #cloneVariableDeclaration}, {@link #cloneIdDeclaration}
 *   <li>types: {@link #cloneType}, {@link #cloneFunctionType} (both keep the original type by
 *       default, because a copy that does not rename anything inside a type can share it)
 *   <li>caching: {@link #lookupCache}, {@link #storeInCache} (no cache by default, so that a
 *       stateful hook sees every occurrence of a node)
 *   <li>memory accesses: {@link #replaceAccess}, which may swap out a whole dereference, subscript,
 *       field reference or address-of expression
 * </ul>
 *
 * <p>The clone tracks whether the node it is currently visiting is written or read ({@link
 * #isLhs()}, set via {@link #cloneAstLeftSide} and {@link #cloneAstRightSide}). Subclasses that
 * enable the cache must not let hooks depend on that flag, because the cache is not keyed by it.
 */
public abstract class CAstCloner {

  private final CExpressionCloner expCloner = new CExpressionCloner();

  /** Whether the AST currently being cloned is on the left-hand side of an assignment/write. */
  private boolean isLhs = false;

  /** Returns a deep copy of the given AST node, or {@code null} for a {@code null} argument. */
  @SuppressWarnings("unchecked")
  public final @Nullable <T extends AAstNode> T cloneAst(final @Nullable T ast) {
    if (ast == null) {
      return null;
    }

    final AAstNode cached = lookupCache(ast);
    if (cached != null) {
      return (T) cached;
    }

    final AAstNode newAst = cloneAstDirect(ast);

    storeInCache(ast, newAst);

    return (T) newAst;
  }

  /** Returns a deep copy of an AST node that is written, e.g. an assignment's left-hand side. */
  public final @Nullable <T extends AAstNode> T cloneAstLeftSide(final @Nullable T ast) {
    return cloneAst(ast, true);
  }

  /** Returns a deep copy of an AST node that is read, e.g. an assignment's right-hand side. */
  public final @Nullable <T extends AAstNode> T cloneAstRightSide(final @Nullable T ast) {
    return cloneAst(ast, false);
  }

  private <T extends AAstNode> @Nullable T cloneAst(final @Nullable T ast, final boolean pIsLhs) {
    final boolean oldIsLhs = isLhs;
    isLhs = pIsLhs;
    try {
      return cloneAst(ast);
    } finally {
      isLhs = oldIsLhs;
    }
  }

  /** Returns a copy of a sub-expression that is read, bypassing the cache like a visitor does. */
  protected final CExpression cloneRvalue(final CExpression exp) {
    final boolean oldIsLhs = isLhs;
    isLhs = false;
    try {
      return exp.accept(expCloner);
    } finally {
      isLhs = oldIsLhs;
    }
  }

  /** Returns a new list with cloned elements, each cloned as a read. */
  protected final <T extends AAstNode> List<T> cloneAstList(final List<T> astList) {
    final List<T> list = new ArrayList<>(astList.size());
    for (T ast : astList) {
      list.add(cloneAstRightSide(ast));
    }
    return list;
  }

  /** Whether the node currently being cloned is written rather than read. */
  protected final boolean isLhs() {
    return isLhs;
  }

  /** Returns the already existing copy of the given node, or {@code null} if there is none. */
  @SuppressWarnings("unused")
  protected @Nullable AAstNode lookupCache(final AAstNode ast) {
    return null;
  }

  /** Offers a freshly created copy for caching. */
  @SuppressWarnings("unused")
  protected void storeInCache(final AAstNode ast, final AAstNode newAst) {}

  /** Returns the type to use in the copy; the default shares the original type. */
  protected @Nullable <T extends Type> T cloneType(final @Nullable T type) {
    return type;
  }

  /** Returns the type to use for a copied {@link CFunctionDeclaration}. */
  protected CFunctionType cloneFunctionType(final CFunctionType type) {
    return cloneType(type);
  }

  /** Returns the name to use for a copied function; the default keeps the original name. */
  protected String changeFunctionName(final String name) {
    return name;
  }

  /** Returns the qualified name to use for a copied variable or parameter declaration. */
  protected String changeQualifiedName(final CSimpleDeclaration decl) {
    return decl.getQualifiedName();
  }

  /** Returns the qualified name to use for a copied typedef or enum constant. */
  protected String changeTypeQualifiedName(final CSimpleDeclaration decl) {
    return decl.getQualifiedName();
  }

  /**
   * Returns the declaration to use for a copied variable declaration, without its initializer.
   * Returning {@code decl} itself signals that the declaration needs no copy, in which case its
   * initializer is left untouched as well.
   */
  @SuppressWarnings("unused")
  protected CVariableDeclaration cloneVariableDeclaration(
      final CVariableDeclaration decl, final boolean pIsWrite) {
    return new CVariableDeclaration(
        decl.getFileLocation(),
        decl.isGlobal(),
        decl.getCStorageClass(),
        cloneType(decl.getType()),
        decl.getName(),
        decl.getOrigName(),
        changeQualifiedName(decl),
        null);
  }

  /** Returns the declaration to attach to a copied {@link CIdExpression} that is not a function. */
  protected @Nullable CSimpleDeclaration cloneIdDeclaration(
      final @Nullable CSimpleDeclaration decl) {
    return cloneAst(decl);
  }

  /**
   * Offers a replacement for a whole memory access (address-of, dereference, subscript or field
   * reference), called with the original expression before its operands are cloned. Returning
   * {@code null} keeps the default copy.
   */
  @SuppressWarnings("unused")
  protected @Nullable CExpression replaceAccess(final CExpression exp) {
    return null;
  }

  /** Returns a deep copy of the given AST node, without consulting the cache. */
  private AAstNode cloneAstDirect(final AAstNode ast) {
    final FileLocation loc = ast.getFileLocation();

    return switch (ast) {
      // CRightHandSide sub classes
      case CExpression cExpression -> cExpression.accept(expCloner);

      case CFunctionCallExpression func ->
          new CFunctionCallExpression(
              loc,
              cloneType(func.getExpressionType()),
              cloneAst(func.getFunctionNameExpression()),
              cloneAstList(func.getParameterExpressions()),
              cloneAst(func.getDeclaration()));

      // CInitializer sub classes
      case CInitializerExpression cInitializerExpression ->
          new CInitializerExpression(
              loc, cloneAstRightSide(cInitializerExpression.getExpression()));
      case CInitializerList cInitializerList ->
          new CInitializerList(loc, cloneAstList(cInitializerList.getInitializers()));
      case CDesignatedInitializer di ->
          new CDesignatedInitializer(
              loc, cloneAstList(di.getDesignators()), cloneAstRightSide(di.getRightHandSide()));

      // CSimpleDeclaration sub classes
      case CVariableDeclaration decl -> {
        // Declaring a variable is always an initializing write, regardless of the ambient side.
        CVariableDeclaration newDecl = cloneVariableDeclaration(decl, true);
        if (newDecl == decl) {
          yield decl;
        }
        // cache the declaration, then clone the initializer and add it.
        // this is needed for the following code: int x = x;
        storeInCache(ast, newDecl);
        newDecl.addInitializer(cloneAstRightSide(decl.getInitializer()));
        yield newDecl;
      }
      case CFunctionDeclaration decl -> {
        List<CParameterDeclaration> l = new ArrayList<>(decl.getParameters().size());
        for (CParameterDeclaration param : decl.getParameters()) {
          l.add(cloneAstRightSide(param));
        }
        yield new CFunctionDeclaration(
            loc,
            cloneFunctionType(decl.getType()),
            changeFunctionName(decl.getName()),
            decl.getOrigName(),
            l,
            decl.getAttributes());
      }
      case CComplexTypeDeclaration decl ->
          new CComplexTypeDeclaration(loc, decl.isGlobal(), cloneType(decl.getType()));

      case CTypeDefDeclaration decl ->
          new CTypeDefDeclaration(
              loc,
              decl.isGlobal(),
              cloneType(decl.getType()),
              decl.getName(),
              changeTypeQualifiedName(decl));

      case CParameterDeclaration decl -> {
        // we do not cache CParameterDeclaration, but clone it directly,
        // because its equals- and hashcode-Method are insufficient for caching
        // TODO do we need to cache it?
        CParameterDeclaration newDecl =
            new CParameterDeclaration(loc, cloneType(decl.getType()), decl.getName());
        newDecl.setQualifiedName(changeQualifiedName(decl));
        yield newDecl;
      }
      case CEnumerator decl ->
          new CEnumerator(loc, decl.getName(), changeTypeQualifiedName(decl), decl.getValue());

      // CStatement sub classes
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

      case CReturnStatement cReturnStatement -> cloneReturnStatement(loc, cReturnStatement);

      // CDesignator sub classes
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

  private CReturnStatement cloneReturnStatement(
      final FileLocation loc, final CReturnStatement ret) {
    // The return value and the right-hand side of the assignment form are the same expression, so
    // clone it only once: a stateful hook must see each access exactly once.
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
  }

  /**
   * Clones CExpressions and calls cloneAst on non-expression-content. Note: caching sub-expressions
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
      CExpression replacement = replaceAccess(exp);
      if (replacement != null) {
        return replacement;
      }
      return new CUnaryExpression(
          exp.getFileLocation(),
          cloneType(exp.getExpressionType()),
          exp.getOperand().accept(this),
          exp.getOperator());
    }

    @Override
    public CExpression visit(CArraySubscriptExpression exp) {
      CExpression replacement = replaceAccess(exp);
      if (replacement != null) {
        return replacement;
      }
      // the operands are always reads, regardless of the ambient access direction
      return new CArraySubscriptExpression(
          exp.getFileLocation(),
          cloneType(exp.getExpressionType()),
          cloneRvalue(exp.getArrayExpression()),
          cloneRvalue(exp.getSubscriptExpression()));
    }

    @Override
    public CExpression visit(CFieldReference exp) {
      CExpression replacement = replaceAccess(exp);
      if (replacement != null) {
        return replacement;
      }
      return new CFieldReference(
          exp.getFileLocation(),
          cloneType(exp.getExpressionType()),
          exp.getFieldName(),
          cloneRvalue(exp.getFieldOwner()),
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
            changeFunctionName(exp.getName()),
            cloneAst(exp.getDeclaration()));
      } else {
        return new CIdExpression(
            exp.getFileLocation(),
            cloneType(exp.getExpressionType()),
            exp.getName(),
            cloneIdDeclaration(exp.getDeclaration()));
      }
    }

    @Override
    public CExpression visit(CPointerExpression exp) {
      CExpression replacement = replaceAccess(exp);
      if (replacement != null) {
        return replacement;
      }
      // the pointer itself is always read, regardless of the ambient access direction
      return new CPointerExpression(
          exp.getFileLocation(), cloneType(exp.getExpressionType()), cloneRvalue(exp.getOperand()));
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
}
