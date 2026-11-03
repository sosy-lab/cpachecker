// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.Sequentialization;

public enum PthreadObjectType {
  COUNT("", true, ImmutableSet.of(), Optional.empty()),
  PTHREAD_BARRIER_T(
      "pthread_barrier_t",
      true,
      ImmutableSet.of(
          PthreadObjectSubstitutions.BARRIER_COMPOSITE_TYPE,
          PthreadObjectSubstitutions.BARRIER_ELABORATED_TYPE,
          PthreadObjectSubstitutions.BARRIER_TYPEDEF_TYPE),
      Optional.empty()),
  PTHREAD_COND_T(
      "pthread_cond_t",
      true,
      ImmutableSet.of(
          PthreadObjectSubstitutions.COND_COMPOSITE_TYPE,
          PthreadObjectSubstitutions.COND_ELABORATED_TYPE,
          PthreadObjectSubstitutions.COND_TYPEDEF_TYPE),
      Optional.of(PthreadObjectSubstitutions.COND_INITIALIZER)),
  PTHREAD_KEY_T("pthread_key_t", false, ImmutableSet.of(), Optional.empty()),
  PTHREAD_MUTEX_T(
      "pthread_mutex_t",
      true,
      ImmutableSet.of(
          PthreadObjectSubstitutions.MUTEX_COMPOSITE_TYPE,
          PthreadObjectSubstitutions.MUTEX_ELABORATED_TYPE,
          PthreadObjectSubstitutions.MUTEX_TYPEDEF_TYPE),
      Optional.of(PthreadObjectSubstitutions.MUTEX_INITIALIZER)),
  PTHREAD_ONCE_T(
      "pthread_once_t",
      true,
      ImmutableSet.of(PthreadObjectSubstitutions.ONCE_TYPEDEF_TYPE),
      Optional.of(PthreadObjectSubstitutions.ONCE_INITIALIZER)),
  PTHREAD_RWLOCK_T(
      "pthread_rwlock_t",
      true,
      ImmutableSet.of(
          PthreadObjectSubstitutions.RWLOCK_COMPOSITE_TYPE,
          PthreadObjectSubstitutions.RWLOCK_ELABORATED_TYPE,
          PthreadObjectSubstitutions.RWLOCK_TYPEDEF_TYPE),
      Optional.of(PthreadObjectSubstitutions.RWLOCK_INITIALIZER)),
  PTHREAD_T("pthread_t", false, ImmutableSet.of(), Optional.empty()),
  RETURN_VALUE("", true, ImmutableSet.of(), Optional.empty()),
  START_ROUTINE("", true, ImmutableSet.of(), Optional.empty()),
  START_ROUTINE_ARGUMENT("", true, ImmutableSet.of(), Optional.empty());

  private final String name;

  private final boolean isArraySupported;

  @SuppressWarnings("Immutable")
  private final ImmutableSet<CType> substituteTypes;

  PthreadObjectType(
      String pName,
      boolean pIsArraySupported,
      ImmutableSet<CType> pSubstituteTypes,
      Optional<CInitializer> pSubstituteInitializer) {
    name = pName;
    isArraySupported = pIsArraySupported;
    substituteTypes = pSubstituteTypes;
  }

  public String getName() {
    return name;
  }

  public boolean isArraySupported() {
    return isArraySupported;
  }

  public ImmutableSet<CType> getSubstituteTypes() {
    return substituteTypes;
  }

  public boolean equalsType(CType pType) {
    // there seems no better way than comparing by string, unfortunately
    return this.name.equals(pType.toASTString("").strip());
  }

  /**
   * Returns an {@link ImmutableSet} of all non-empty string representations associated with {@link
   * PthreadObjectType}.
   */
  public static ImmutableSet<String> getAllPthreadObjectTypeNames() {
    return Arrays.stream(values())
        .map(type -> type.name)
        .filter(name -> !name.isEmpty())
        .collect(ImmutableSet.toImmutableSet());
  }

  /** A private class to define final variables that can be used as attributes in the enum. */
  static final class PthreadObjectSubstitutions {

    // pthread_barrier_t

    private static final String BARRIER_NAME = "pthread_barrier_t";

    private static final String BARRIER_SUBSTITUTION_NAME =
        Sequentialization.MPOR_PREFIX + BARRIER_NAME;

    static final CCompositeTypeMemberDeclaration BARRIER_NUM_WAITERS_MEMBER_DECLARATION =
        new CCompositeTypeMemberDeclaration(CNumericTypes.UNSIGNED_INT, "NUM_WAITERS");

    static final CCompositeTypeMemberDeclaration BARRIER_COUNT_MEMBER_DECLARATION =
        new CCompositeTypeMemberDeclaration(CNumericTypes.UNSIGNED_INT, "COUNT");

    private static final CCompositeType BARRIER_COMPOSITE_TYPE =
        new CCompositeType(
            CTypeQualifiers.NONE,
            ComplexTypeKind.STRUCT,
            ImmutableList.of(
                BARRIER_COUNT_MEMBER_DECLARATION, BARRIER_NUM_WAITERS_MEMBER_DECLARATION),
            BARRIER_SUBSTITUTION_NAME,
            BARRIER_SUBSTITUTION_NAME);

    static final CElaboratedType BARRIER_ELABORATED_TYPE =
        new CElaboratedType(
            CTypeQualifiers.NONE,
            ComplexTypeKind.STRUCT,
            BARRIER_SUBSTITUTION_NAME,
            BARRIER_SUBSTITUTION_NAME,
            BARRIER_COMPOSITE_TYPE);

    private static final CTypedefType BARRIER_TYPEDEF_TYPE =
        new CTypedefType(CTypeQualifiers.NONE, BARRIER_NAME, BARRIER_ELABORATED_TYPE);

    // pthread_cond_t

    private static final String COND_NAME = "pthread_cond_t";

    private static final String COND_SUBSTITUTION_NAME = Sequentialization.MPOR_PREFIX + COND_NAME;

    static final CCompositeTypeMemberDeclaration COND_SIGNALED_MEMBER_DECLARATION =
        new CCompositeTypeMemberDeclaration(CNumericTypes.UNSIGNED_CHAR, "SIGNALED");

    private static final CCompositeType COND_COMPOSITE_TYPE =
        new CCompositeType(
            CTypeQualifiers.NONE,
            ComplexTypeKind.STRUCT,
            ImmutableList.of(COND_SIGNALED_MEMBER_DECLARATION),
            COND_SUBSTITUTION_NAME,
            COND_SUBSTITUTION_NAME);

    static final CElaboratedType COND_ELABORATED_TYPE =
        new CElaboratedType(
            CTypeQualifiers.NONE,
            ComplexTypeKind.STRUCT,
            COND_SUBSTITUTION_NAME,
            COND_SUBSTITUTION_NAME,
            COND_COMPOSITE_TYPE);

    private static final CTypedefType COND_TYPEDEF_TYPE =
        new CTypedefType(CTypeQualifiers.NONE, COND_NAME, COND_ELABORATED_TYPE);

    private static final CInitializer COND_SIGNALED_INITIALIZER =
        new CInitializerExpression(FileLocation.DUMMY, CIntegerLiteralExpression.ZERO);

    private static final CInitializerList COND_INITIALIZER =
        new CInitializerList(FileLocation.DUMMY, ImmutableList.of(COND_SIGNALED_INITIALIZER));

    // pthread_mutex_t

    private static final String MUTEX_NAME = "pthread_mutex_t";

    private static final String MUTEX_SUBSTITUTION_NAME =
        Sequentialization.MPOR_PREFIX + MUTEX_NAME;

    static final CCompositeTypeMemberDeclaration MUTEX_LOCKED_MEMBER_DECLARATION =
        new CCompositeTypeMemberDeclaration(CNumericTypes.UNSIGNED_CHAR, "LOCKED");

    private static final CCompositeType MUTEX_COMPOSITE_TYPE =
        new CCompositeType(
            CTypeQualifiers.NONE,
            ComplexTypeKind.STRUCT,
            ImmutableList.of(MUTEX_LOCKED_MEMBER_DECLARATION),
            MUTEX_SUBSTITUTION_NAME,
            MUTEX_SUBSTITUTION_NAME);

    static final CElaboratedType MUTEX_ELABORATED_TYPE =
        new CElaboratedType(
            CTypeQualifiers.NONE,
            ComplexTypeKind.STRUCT,
            MUTEX_SUBSTITUTION_NAME,
            MUTEX_SUBSTITUTION_NAME,
            MUTEX_COMPOSITE_TYPE);

    private static final CTypedefType MUTEX_TYPEDEF_TYPE =
        new CTypedefType(CTypeQualifiers.NONE, MUTEX_NAME, MUTEX_ELABORATED_TYPE);

    private static final CInitializer MUTEX_LOCKED_INITIALIZER =
        new CInitializerExpression(FileLocation.DUMMY, CIntegerLiteralExpression.ZERO);

    private static final CInitializerList MUTEX_INITIALIZER =
        new CInitializerList(FileLocation.DUMMY, ImmutableList.of(MUTEX_LOCKED_INITIALIZER));

    // pthread_once_t

    private static final String ONCE_NAME = "pthread_once_t";

    private static final CTypedefType ONCE_TYPEDEF_TYPE =
        new CTypedefType(CTypeQualifiers.NONE, ONCE_NAME, CNumericTypes.UNSIGNED_INT);

    private static final CInitializer ONCE_INITIALIZER =
        new CInitializerExpression(FileLocation.DUMMY, CIntegerLiteralExpression.ZERO);

    // pthread_rwlock_t

    private static final String RWLOCK_NAME = "pthread_rwlock_t";

    private static final String RWLOCK_SUBSTITUTION_NAME =
        Sequentialization.MPOR_PREFIX + RWLOCK_NAME;

    // NUM_READERS is an unsigned int because it can be incremented to any number
    static final CCompositeTypeMemberDeclaration RWLOCK_NUM_READERS_MEMBER_DECLARATION =
        new CCompositeTypeMemberDeclaration(CNumericTypes.UNSIGNED_INT, "NUM_READERS");

    static final CCompositeTypeMemberDeclaration RWLOCK_NUM_WRITERS_MEMBER_DECLARATION =
        new CCompositeTypeMemberDeclaration(CNumericTypes.UNSIGNED_CHAR, "NUM_WRITERS");

    private static final CCompositeType RWLOCK_COMPOSITE_TYPE =
        new CCompositeType(
            CTypeQualifiers.NONE,
            ComplexTypeKind.STRUCT,
            ImmutableList.of(
                RWLOCK_NUM_READERS_MEMBER_DECLARATION, RWLOCK_NUM_WRITERS_MEMBER_DECLARATION),
            RWLOCK_SUBSTITUTION_NAME,
            RWLOCK_SUBSTITUTION_NAME);

    static final CElaboratedType RWLOCK_ELABORATED_TYPE =
        new CElaboratedType(
            CTypeQualifiers.NONE,
            ComplexTypeKind.STRUCT,
            RWLOCK_SUBSTITUTION_NAME,
            RWLOCK_SUBSTITUTION_NAME,
            RWLOCK_COMPOSITE_TYPE);

    private static final CTypedefType RWLOCK_TYPEDEF_TYPE =
        new CTypedefType(CTypeQualifiers.NONE, RWLOCK_NAME, RWLOCK_ELABORATED_TYPE);

    private static final CInitializer RWLOCK_NUM_READERS_INITIALIZER =
        new CInitializerExpression(FileLocation.DUMMY, CIntegerLiteralExpression.ZERO);

    private static final CInitializer RWLOCK_NUM_WRITERS_INITIALIZER =
        new CInitializerExpression(FileLocation.DUMMY, CIntegerLiteralExpression.ZERO);

    private static final CInitializerList RWLOCK_INITIALIZER =
        new CInitializerList(
            FileLocation.DUMMY,
            ImmutableList.of(RWLOCK_NUM_READERS_INITIALIZER, RWLOCK_NUM_WRITERS_INITIALIZER));
  }
}
