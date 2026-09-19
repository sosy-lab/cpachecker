// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.concurrent;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstCloner;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;

/**
 * AST cloner for POR. Locals, parameters and {@code __thread} variables are always renamed to
 * {@code T{threadId}_{qualifiedName}}. Ordinary globals keep their original name unless a {@link
 * GlobalAccessRenamer} is supplied, in which case every single access to a global variable is
 * instead renamed to a fresh, access-specific name ("concurrent SSA"); with no renamer, cloning is
 * bit-identical to the plain thread-ID renaming.
 *
 * <p>A {@code __thread} variable is deliberately renamed like a local despite living at file scope:
 * each thread has its own copy, so it is not shared state. Privatizing it here is only half of the
 * story — a spawned thread never runs the file-scope declaration edge, so both analyses inject the
 * per-thread initialization at each {@code pthread_create} (see {@link
 * ThreadFunctions#threadLocalGlobals}).
 *
 * <p>Types are never cloned: no name inside a type changes, so the original type can be shared. The
 * AST cache of {@link CAstCloner} stays disabled, because a {@link GlobalAccessRenamer} is stateful
 * and has to see every single occurrence of a variable.
 */
class ConcurrentAstCloner extends CAstCloner {

  private final int threadId;
  private final @Nullable GlobalAccessRenamer globalRenamer;

  ConcurrentAstCloner(int pThreadId) {
    this.threadId = pThreadId;
    this.globalRenamer = null;
  }

  ConcurrentAstCloner(int pThreadId, GlobalAccessRenamer pGlobalRenamer) {
    this.threadId = pThreadId;
    this.globalRenamer = checkNotNull(pGlobalRenamer);
  }

  /** Only the parameter declarations are rebuilt, to give them the per-thread names. */
  @Override
  protected CFunctionType cloneFunctionType(CFunctionType type) {
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

  /**
   * Creates the renamed declaration to use for one access to {@code decl}. Locals always get the
   * thread-ID prefix. Globals keep their original name unless {@link #globalRenamer} is set, in
   * which case {@code pIsWrite} is forwarded to it so every single access gets its own fresh name.
   */
  @Override
  protected CVariableDeclaration cloneVariableDeclaration(
      CVariableDeclaration decl, boolean pIsWrite) {
    FileLocation loc = decl.getFileLocation();
    if (globalRenamer != null
        && ((decl.isGlobal() && !decl.isThreadLocal())
            || globalRenamer.treatsLocalAsRegion(decl))) {
      // globals, and address-taken locals the renamer wants in the aliasing regime, get a fresh
      // per-access name so every access becomes its own tracked memory event. A __thread global
      // is excluded here despite decl.isGlobal(): each thread has its own private copy, so it must
      // fall through to the local-renaming branch below like an ordinary local, never becoming a
      // shared/aliased access.
      return new CVariableDeclaration(
          loc,
          decl.isGlobal(),
          decl.getCStorageClass(),
          decl.getType(),
          decl.getName(),
          decl.getOrigName(),
          globalRenamer.freshName(decl, pIsWrite),
          null);
    } else if (decl.isGlobal() && !decl.isThreadLocal()) {
      return decl; // no renamer: non-thread-local globals keep their original name
    } else {
      return new CVariableDeclaration(
          loc,
          false,
          decl.getCStorageClass(),
          decl.getType(),
          decl.getName(),
          decl.getOrigName(),
          changeQualifiedName(decl),
          null);
    }
  }

  @Override
  protected @Nullable CSimpleDeclaration cloneIdDeclaration(@Nullable CSimpleDeclaration decl) {
    if (decl instanceof CVariableDeclaration varDecl) {
      return cloneVariableDeclaration(varDecl, isLhs());
    } else if (decl instanceof CParameterDeclaration param) {
      final CParameterDeclaration newParam =
          new CParameterDeclaration(param.getFileLocation(), param.getType(), param.getName());
      newParam.setQualifiedName(changeQualifiedName(param));
      return newParam;
    } else {
      return decl;
    }
  }

  /**
   * Renames a local variable, parameter, or {@code __thread} variable with a thread-ID prefix.
   * Never called for an ordinary global: those are either left unchanged or renamed via {@link
   * #globalRenamer} instead. Typedefs and enum constants are not per-thread state and keep their
   * qualified name, which is why {@code changeTypeQualifiedName} stays at its default.
   */
  @Override
  protected String changeQualifiedName(CSimpleDeclaration decl) {
    return ThreadFunctions.perThreadName(threadId, decl.getQualifiedName());
  }

  @Override
  protected @Nullable CExpression replaceAccess(CExpression exp) {
    if (globalRenamer == null) {
      return null;
    }
    if (exp instanceof CUnaryExpression unary) {
      return unary.getOperator() == CUnaryExpression.UnaryOperator.AMPER
          ? globalRenamer.replaceAddressOf(unary, this::cloneRvalue)
          : null;
    }
    if (exp instanceof CArraySubscriptExpression
        || exp instanceof CFieldReference
        || exp instanceof CPointerExpression) {
      return globalRenamer.replaceAliasedAccess(exp, isLhs(), this::cloneRvalue);
    }
    return null;
  }
}
