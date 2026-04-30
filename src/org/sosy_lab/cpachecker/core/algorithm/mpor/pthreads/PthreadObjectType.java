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
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeQualifiers;

public enum PthreadObjectType {
  PTHREAD_BARRIER_T("pthread_barrier_t", Optional.empty()),
  PTHREAD_COND_INITIALIZER("PTHREAD_COND_INITIALIZER", Optional.empty()),
  PTHREAD_COND_T("pthread_cond_t", Optional.of(Substitutions.COND_ELABORATED_TYPE)),
  PTHREAD_KEY_T("pthread_key_t", Optional.empty()),
  PTHREAD_MUTEX_INITIALIZER("PTHREAD_MUTEX_INITIALIZER", Optional.empty()),
  PTHREAD_MUTEX_T("pthread_mutex_t", Optional.of(Substitutions.MUTEX_ELABORATED_TYPE)),
  PTHREAD_ONCE_T("pthread_once_t", Optional.empty()),
  PTHREAD_RWLOCK_T("pthread_rwlock_t", Optional.of(Substitutions.RWLOCK_ELABORATED_TYPE)),
  PTHREAD_T("pthread_t", Optional.empty()),
  RETURN_VALUE("", Optional.empty()),
  START_ROUTINE("", Optional.empty()),
  START_ROUTINE_ARGUMENT("", Optional.empty());

  public final String name;

  public final Optional<CElaboratedType> substituteType;

  PthreadObjectType(String pName, Optional<CElaboratedType> pSubstituteType) {
    name = pName;
    substituteType = pSubstituteType;
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
  private static final class Substitutions {
    private static final String SUBSTITUTION_PREFIX = "__substitution__";

    // pthread_mutex_t

    private static final String MUTEX_SUBSTITUTION_NAME = SUBSTITUTION_PREFIX + "pthread_mutex_t";

    private static final CCompositeTypeMemberDeclaration MUTEX_MEMBER_DECLARATION =
        new CCompositeTypeMemberDeclaration(CNumericTypes.UNSIGNED_CHAR, "LOCKED");

    private static final CCompositeType MUTEX_COMPOSITE_TYPE =
        new CCompositeType(
            CTypeQualifiers.NONE,
            ComplexTypeKind.STRUCT,
            ImmutableList.of(MUTEX_MEMBER_DECLARATION),
            MUTEX_SUBSTITUTION_NAME,
            MUTEX_SUBSTITUTION_NAME);

    private static final CElaboratedType MUTEX_ELABORATED_TYPE =
        new CElaboratedType(
            CTypeQualifiers.NONE,
            ComplexTypeKind.STRUCT,
            MUTEX_SUBSTITUTION_NAME,
            MUTEX_SUBSTITUTION_NAME,
            MUTEX_COMPOSITE_TYPE);

    // pthread_cond_t

    private static final String COND_SUBSTITUTION_NAME = SUBSTITUTION_PREFIX + "pthread_cond_t";

    private static final CCompositeTypeMemberDeclaration COND_MEMBER_DECLARATION =
        new CCompositeTypeMemberDeclaration(CNumericTypes.UNSIGNED_CHAR, "SIGNALED");

    private static final CCompositeType COND_COMPOSITE_TYPE =
        new CCompositeType(
            CTypeQualifiers.NONE,
            ComplexTypeKind.STRUCT,
            ImmutableList.of(COND_MEMBER_DECLARATION),
            COND_SUBSTITUTION_NAME,
            COND_SUBSTITUTION_NAME);

    private static final CElaboratedType COND_ELABORATED_TYPE =
        new CElaboratedType(
            CTypeQualifiers.NONE,
            ComplexTypeKind.STRUCT,
            COND_SUBSTITUTION_NAME,
            COND_SUBSTITUTION_NAME,
            COND_COMPOSITE_TYPE);

    // pthread_rwlock_t

    private static final String RWLOCK_SUBSTITUTION_NAME = SUBSTITUTION_PREFIX + "pthread_rwlock_t";

    private static final CCompositeTypeMemberDeclaration RWLOCK_NUM_READERS_MEMBER_DECLARATION =
        new CCompositeTypeMemberDeclaration(CNumericTypes.UNSIGNED_CHAR, "NUM_READERS");

    private static final CCompositeTypeMemberDeclaration RWLOCK_NUM_WRITERS_MEMBER_DECLARATION =
        new CCompositeTypeMemberDeclaration(CNumericTypes.UNSIGNED_CHAR, "NUM_WRITERS");

    private static final CCompositeType RWLOCK_COMPOSITE_TYPE =
        new CCompositeType(
            CTypeQualifiers.NONE,
            ComplexTypeKind.STRUCT,
            ImmutableList.of(
                RWLOCK_NUM_READERS_MEMBER_DECLARATION, RWLOCK_NUM_WRITERS_MEMBER_DECLARATION),
            RWLOCK_SUBSTITUTION_NAME,
            RWLOCK_SUBSTITUTION_NAME);

    private static final CElaboratedType RWLOCK_ELABORATED_TYPE =
        new CElaboratedType(
            CTypeQualifiers.NONE,
            ComplexTypeKind.STRUCT,
            RWLOCK_SUBSTITUTION_NAME,
            RWLOCK_SUBSTITUTION_NAME,
            RWLOCK_COMPOSITE_TYPE);
  }
}
