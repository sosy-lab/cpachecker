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
  PTHREAD_BARRIER_T("pthread_barrier_t", ImmutableSet.of()),
  PTHREAD_COND_INITIALIZER("PTHREAD_COND_INITIALIZER", ImmutableSet.of()),
  PTHREAD_COND_T(
      "pthread_cond_t",
      ImmutableSet.of(
          PthreadObjectSubstitutions.COND_COMPOSITE_TYPE,
          PthreadObjectSubstitutions.COND_ELABORATED_TYPE,
          PthreadObjectSubstitutions.COND_TYPEDEF_TYPE)),
  PTHREAD_KEY_T("pthread_key_t", ImmutableSet.of()),
  PTHREAD_MUTEX_INITIALIZER("PTHREAD_MUTEX_INITIALIZER", ImmutableSet.of()),
  PTHREAD_MUTEX_T(
      "pthread_mutex_t",
      ImmutableSet.of(
          PthreadObjectSubstitutions.MUTEX_COMPOSITE_TYPE,
          PthreadObjectSubstitutions.MUTEX_ELABORATED_TYPE,
          PthreadObjectSubstitutions.MUTEX_TYPEDEF_TYPE)),
  PTHREAD_ONCE_T("pthread_once_t", ImmutableSet.of()),
  PTHREAD_RWLOCK_T(
      "pthread_rwlock_t",
      ImmutableSet.of(
          PthreadObjectSubstitutions.RWLOCK_COMPOSITE_TYPE,
          PthreadObjectSubstitutions.RWLOCK_ELABORATED_TYPE,
          PthreadObjectSubstitutions.RWLOCK_TYPEDEF_TYPE)),
  PTHREAD_T("pthread_t", ImmutableSet.of()),
  RETURN_VALUE("", ImmutableSet.of()),
  START_ROUTINE("", ImmutableSet.of()),
  START_ROUTINE_ARGUMENT("", ImmutableSet.of());

  public final String name;

  @SuppressWarnings("Immutable")
  public final ImmutableSet<CType> substituteTypes;

  PthreadObjectType(String pName, ImmutableSet<CType> pSubstituteTypes) {
    name = pName;
    substituteTypes = pSubstituteTypes;
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

    static final String INNER_LIST_NAME = "inner_list";

    // general CCompositeTypeMemberDeclaration

    private static final CCompositeTypeMemberDeclaration DUMMY_A_MEMBER_DECLARATION =
        new CCompositeTypeMemberDeclaration(CNumericTypes.UNSIGNED_CHAR, "dummyA");

    private static final CCompositeTypeMemberDeclaration DUMMY_B_MEMBER_DECLARATION =
        new CCompositeTypeMemberDeclaration(CNumericTypes.UNSIGNED_CHAR, "dummyB");

    private static final CCompositeTypeMemberDeclaration DUMMY_C_MEMBER_DECLARATION =
        new CCompositeTypeMemberDeclaration(CNumericTypes.UNSIGNED_CHAR, "dummyC");

    private static final CCompositeTypeMemberDeclaration DUMMY_D_MEMBER_DECLARATION =
        new CCompositeTypeMemberDeclaration(CNumericTypes.UNSIGNED_CHAR, "dummyD");

    private static final CCompositeTypeMemberDeclaration DUMMY_E_MEMBER_DECLARATION =
        new CCompositeTypeMemberDeclaration(CNumericTypes.UNSIGNED_CHAR, "dummyE");

    private static final CCompositeTypeMemberDeclaration DUMMY_F_MEMBER_DECLARATION =
        new CCompositeTypeMemberDeclaration(CNumericTypes.UNSIGNED_CHAR, "dummyF");

    private static final CCompositeTypeMemberDeclaration DUMMY_G_MEMBER_DECLARATION =
        new CCompositeTypeMemberDeclaration(CNumericTypes.UNSIGNED_CHAR, "dummyG");

    private static final CCompositeTypeMemberDeclaration DUMMY_H_MEMBER_DECLARATION =
        new CCompositeTypeMemberDeclaration(CNumericTypes.UNSIGNED_CHAR, "dummyH");

    private static final CCompositeTypeMemberDeclaration DUMMY_I_MEMBER_DECLARATION =
        new CCompositeTypeMemberDeclaration(CNumericTypes.UNSIGNED_CHAR, "dummyI");

    // pthread_mutex_t

    private static final String MUTEX_NAME = "pthread_mutex_t";

    private static final String INNER_INNER_LIST_NAME = "inner_inner_list";

    private static final String INNER_INNER_INNER_LIST_NAME = "inner_inner_inner_list";

    private static final String MUTEX_SUBSTITUTION_NAME =
        Sequentialization.MPOR_PREFIX + MUTEX_NAME;

    private static final String MUTEX_INNER_LIST_SUBSTITUTION_NAME =
        MUTEX_SUBSTITUTION_NAME + "_" + INNER_LIST_NAME;

    private static final String MUTEX_INNER_INNER_LIST_SUBSTITUTION_NAME =
        MUTEX_SUBSTITUTION_NAME + "_" + INNER_INNER_LIST_NAME;

    private static final String MUTEX_INNER_INNER_INNER_LIST_SUBSTITUTION_NAME =
        MUTEX_SUBSTITUTION_NAME + "_" + INNER_INNER_INNER_LIST_NAME;

    static final CCompositeTypeMemberDeclaration MUTEX_LOCKED_MEMBER_DECLARATION =
        new CCompositeTypeMemberDeclaration(CNumericTypes.UNSIGNED_CHAR, "LOCKED");

    private static final CCompositeType MUTEX_INNER_INNER_INNER_LIST_COMPOSITE_TYPE =
        new CCompositeType(
            CTypeQualifiers.NONE,
            ComplexTypeKind.STRUCT,
            ImmutableList.of(DUMMY_A_MEMBER_DECLARATION, DUMMY_B_MEMBER_DECLARATION),
            MUTEX_INNER_INNER_INNER_LIST_SUBSTITUTION_NAME,
            MUTEX_INNER_INNER_INNER_LIST_SUBSTITUTION_NAME);

    private static final CCompositeType MUTEX_INNER_INNER_LIST_COMPOSITE_TYPE =
        new CCompositeType(
            CTypeQualifiers.NONE,
            ComplexTypeKind.STRUCT,
            ImmutableList.of(
                new CCompositeTypeMemberDeclaration(
                    MUTEX_INNER_INNER_INNER_LIST_COMPOSITE_TYPE, INNER_INNER_INNER_LIST_NAME)),
            MUTEX_INNER_INNER_LIST_SUBSTITUTION_NAME,
            MUTEX_INNER_INNER_LIST_SUBSTITUTION_NAME);

    private static final CCompositeTypeMemberDeclaration MUTEX_INNER_INNER_LIST_MEMBER_DECLARATION =
        new CCompositeTypeMemberDeclaration(
            MUTEX_INNER_INNER_LIST_COMPOSITE_TYPE, INNER_INNER_LIST_NAME);

    private static final CCompositeType MUTEX_INNER_LIST_COMPOSITE_TYPE =
        new CCompositeType(
            CTypeQualifiers.NONE,
            ComplexTypeKind.STRUCT,
            ImmutableList.of(
                MUTEX_LOCKED_MEMBER_DECLARATION,
                DUMMY_A_MEMBER_DECLARATION,
                DUMMY_B_MEMBER_DECLARATION,
                DUMMY_C_MEMBER_DECLARATION,
                DUMMY_D_MEMBER_DECLARATION,
                MUTEX_INNER_INNER_LIST_MEMBER_DECLARATION),
            MUTEX_INNER_LIST_SUBSTITUTION_NAME,
            MUTEX_INNER_LIST_SUBSTITUTION_NAME);

    static final CElaboratedType MUTEX_INNER_LIST_ELABORATED_TYPE =
        new CElaboratedType(
            CTypeQualifiers.NONE,
            ComplexTypeKind.STRUCT,
            MUTEX_INNER_LIST_SUBSTITUTION_NAME,
            MUTEX_INNER_LIST_SUBSTITUTION_NAME,
            MUTEX_INNER_LIST_COMPOSITE_TYPE);

    static final CCompositeTypeMemberDeclaration MUTEX_INNER_LIST_MEMBER_DECLARATION =
        new CCompositeTypeMemberDeclaration(MUTEX_INNER_LIST_COMPOSITE_TYPE, INNER_LIST_NAME);

    private static final CCompositeType MUTEX_COMPOSITE_TYPE =
        new CCompositeType(
            CTypeQualifiers.NONE,
            ComplexTypeKind.STRUCT,
            ImmutableList.of(MUTEX_INNER_LIST_MEMBER_DECLARATION),
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

    // pthread_cond_t

    private static final String COND_NAME = "pthread_cond_t";

    private static final String COND_SUBSTITUTION_NAME = Sequentialization.MPOR_PREFIX + COND_NAME;

    static final CCompositeTypeMemberDeclaration COND_MEMBER_DECLARATION =
        new CCompositeTypeMemberDeclaration(CNumericTypes.UNSIGNED_CHAR, "SIGNALED");

    private static final CCompositeType COND_COMPOSITE_TYPE =
        new CCompositeType(
            CTypeQualifiers.NONE,
            ComplexTypeKind.STRUCT,
            ImmutableList.of(COND_MEMBER_DECLARATION),
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

    // pthread_rwlock_t

    private static final String RWLOCK_NAME = "pthread_rwlock_t";

    private static final String RWLOCK_SUBSTITUTION_NAME =
        Sequentialization.MPOR_PREFIX + RWLOCK_NAME;

    private static final String RWLOCK_INNER_LIST_SUBSTITUTION_NAME =
        RWLOCK_SUBSTITUTION_NAME + "_" + INNER_LIST_NAME;

    // NUM_READERS is an unsigned int because it can be incremented to any number
    static final CCompositeTypeMemberDeclaration RWLOCK_NUM_READERS_MEMBER_DECLARATION =
        new CCompositeTypeMemberDeclaration(CNumericTypes.UNSIGNED_INT, "NUM_READERS");

    static final CCompositeTypeMemberDeclaration RWLOCK_NUM_WRITERS_MEMBER_DECLARATION =
        new CCompositeTypeMemberDeclaration(CNumericTypes.UNSIGNED_CHAR, "NUM_WRITERS");

    private static final CCompositeType RWLOCK_INNER_LIST_COMPOSITE_TYPE =
        new CCompositeType(
            CTypeQualifiers.NONE,
            ComplexTypeKind.STRUCT,
            ImmutableList.of(
                RWLOCK_NUM_READERS_MEMBER_DECLARATION,
                RWLOCK_NUM_WRITERS_MEMBER_DECLARATION,
                DUMMY_A_MEMBER_DECLARATION,
                DUMMY_B_MEMBER_DECLARATION,
                DUMMY_C_MEMBER_DECLARATION,
                DUMMY_D_MEMBER_DECLARATION,
                DUMMY_E_MEMBER_DECLARATION,
                DUMMY_F_MEMBER_DECLARATION,
                DUMMY_G_MEMBER_DECLARATION,
                DUMMY_H_MEMBER_DECLARATION,
                DUMMY_I_MEMBER_DECLARATION),
            RWLOCK_INNER_LIST_SUBSTITUTION_NAME,
            RWLOCK_INNER_LIST_SUBSTITUTION_NAME);

    static final CElaboratedType RWLOCK_INNER_LIST_ELABORATED_TYPE =
        new CElaboratedType(
            CTypeQualifiers.NONE,
            ComplexTypeKind.STRUCT,
            RWLOCK_INNER_LIST_SUBSTITUTION_NAME,
            RWLOCK_INNER_LIST_SUBSTITUTION_NAME,
            RWLOCK_INNER_LIST_COMPOSITE_TYPE);

    static final CCompositeTypeMemberDeclaration RWLOCK_INNER_LIST_MEMBER_DECLARATION =
        new CCompositeTypeMemberDeclaration(RWLOCK_INNER_LIST_COMPOSITE_TYPE, INNER_LIST_NAME);

    private static final CCompositeType RWLOCK_COMPOSITE_TYPE =
        new CCompositeType(
            CTypeQualifiers.NONE,
            ComplexTypeKind.STRUCT,
            ImmutableList.of(RWLOCK_INNER_LIST_MEMBER_DECLARATION),
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
  }
}
