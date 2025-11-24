// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Optional;

/**
 * Contains mostly methods from the pthread standard, though we also include e.g. {@code
 * __VERIFIER_atomic_begin} here.
 */
public enum PthreadFunctionType {

  // Note that all indices start at 0.
  PTHREAD_BARRIER_INIT("pthread_barrier_init", false, false),
  PTHREAD_BARRIER_WAIT("pthread_barrier_wait", false, false),
  PTHREAD_CANCEL(
      "pthread_cancel",
      false,
      false,
      new PthreadParameterInfo(PthreadObjectType.PTHREAD_T, false, 0)),
  PTHREAD_COND_INIT(
      "pthread_cond_init",
      true,
      false,
      new PthreadParameterInfo(PthreadObjectType.PTHREAD_COND_T, 0)),
  PTHREAD_COND_SIGNAL(
      "pthread_cond_signal",
      true,
      true,
      new PthreadParameterInfo(PthreadObjectType.PTHREAD_COND_T, 0)),
  PTHREAD_COND_WAIT(
      "pthread_cond_wait",
      true,
      true,
      new PthreadParameterInfo(PthreadObjectType.PTHREAD_COND_T, 0),
      new PthreadParameterInfo(PthreadObjectType.PTHREAD_MUTEX_T, 1)),
  PTHREAD_CREATE(
      "pthread_create",
      true,
      true,
      new PthreadParameterInfo(PthreadObjectType.PTHREAD_T, true, 0),
      new PthreadParameterInfo(PthreadObjectType.START_ROUTINE, 2),
      new PthreadParameterInfo(PthreadObjectType.START_ROUTINE_ARGUMENT, 3)),
  PTHREAD_DETACH(
      "pthread_detach",
      false,
      false,
      new PthreadParameterInfo(PthreadObjectType.PTHREAD_T, false, 0)),
  PTHREAD_EQUAL(
      "pthread_equal",
      false,
      false,
      new PthreadParameterInfo(PthreadObjectType.PTHREAD_T, false, 0, 1)),
  PTHREAD_EXIT(
      "pthread_exit", true, true, new PthreadParameterInfo(PthreadObjectType.RETURN_VALUE, 0)),
  PTHREAD_GETSPECIFIC("pthread_getspecific", false, false),
  PTHREAD_JOIN(
      "pthread_join",
      true,
      true,
      new PthreadParameterInfo(PthreadObjectType.PTHREAD_T, false, 0),
      new PthreadParameterInfo(PthreadObjectType.RETURN_VALUE, 1)),
  PTHREAD_KEY_CREATE("pthread_key_create", false, false),
  PTHREAD_KILL(
      "pthread_kill",
      false,
      false,
      new PthreadParameterInfo(PthreadObjectType.PTHREAD_T, false, 0)),
  PTHREAD_MUTEX_DESTROY(
      "pthread_mutex_destroy",
      true,
      false,
      new PthreadParameterInfo(PthreadObjectType.PTHREAD_MUTEX_T, 0)),
  PTHREAD_MUTEX_INIT(
      "pthread_mutex_init",
      true,
      false,
      new PthreadParameterInfo(PthreadObjectType.PTHREAD_MUTEX_T, 0)),
  PTHREAD_MUTEX_LOCK(
      "pthread_mutex_lock",
      true,
      true,
      new PthreadParameterInfo(PthreadObjectType.PTHREAD_MUTEX_T, 0)),
  PTHREAD_MUTEX_TRYLOCK(
      "pthread_mutex_trylock",
      false,
      false,
      new PthreadParameterInfo(PthreadObjectType.PTHREAD_MUTEX_T, 0)),
  PTHREAD_MUTEX_UNLOCK(
      "pthread_mutex_unlock",
      true,
      true,
      new PthreadParameterInfo(PthreadObjectType.PTHREAD_MUTEX_T, 0)),
  PTHREAD_ONCE("pthread_once", false, false),
  PTHREAD_RWLOCK_RDLOCK(
      "pthread_rwlock_rdlock",
      true,
      true,
      new PthreadParameterInfo(PthreadObjectType.PTHREAD_RWLOCK_T, 0)),
  PTHREAD_RWLOCK_TRYRDLOCK(
      "pthread_rwlock_tryrdlock",
      false,
      false,
      new PthreadParameterInfo(PthreadObjectType.PTHREAD_RWLOCK_T, 0)),
  PTHREAD_RWLOCK_TRYWRLOCK(
      "pthread_rwlock_trywrlock",
      false,
      false,
      new PthreadParameterInfo(PthreadObjectType.PTHREAD_RWLOCK_T, 0)),
  PTHREAD_RWLOCK_UNLOCK(
      "pthread_rwlock_unlock",
      true,
      true,
      new PthreadParameterInfo(PthreadObjectType.PTHREAD_RWLOCK_T, 0)),
  PTHREAD_RWLOCK_WRLOCK(
      "pthread_rwlock_wrlock",
      true,
      true,
      new PthreadParameterInfo(PthreadObjectType.PTHREAD_RWLOCK_T, 0)),
  PTHREAD_SELF("pthread_self", false, false),
  PTHREAD_SETSPECIFIC("pthread_setspecific", false, false),
  // pthread_yield is deprecated: https://www.man7.org/linux/man-pages/man3/pthread_yield.3.html
  // __VERIFIER_atomic functions are not part of the pthread standard, but still related to threads
  VERIFIER_ATOMIC_BEGIN("__VERIFIER_atomic_begin", true, true),
  VERIFIER_ATOMIC_END("__VERIFIER_atomic_end", true, true);

  public final String name;

  public final boolean isSupported;

  /**
   * If this function is explicitly handled in the sequentialization, i.e. contains case block code
   * simulating the function's behavior.
   */
  public final boolean isExplicitlyHandled;

  final ImmutableSet<PthreadParameterInfo> parameterInfo;

  PthreadFunctionType(
      String pName,
      boolean pIsSupported,
      boolean pIsExplicitlyHandled,
      PthreadParameterInfo... pParameterInfo) {

    // if the function is not supported, it cannot be explicitly handled
    checkArgument(pIsSupported || !pIsExplicitlyHandled);
    checkArgument(
        arePthreadObjectTypesUnique(pParameterInfo),
        "duplicate PthreadObjectType found in pParameterInfo");

    name = pName;
    isSupported = pIsSupported;
    isExplicitlyHandled = pIsExplicitlyHandled;
    parameterInfo = ImmutableSet.copyOf(pParameterInfo);
  }

  private static boolean arePthreadObjectTypesUnique(PthreadParameterInfo... pParameterInfo) {

    return Arrays.stream(pParameterInfo).map(PthreadParameterInfo::getObjectType).distinct().count()
        == pParameterInfo.length;
  }

  public boolean isPthreadTPointer() {
    checkArgument(
        isParameterPresent(PthreadObjectType.PTHREAD_T),
        "this PthreadFuncType has no pthread_t param");
    return getParameterInfoByObjectType(PthreadObjectType.PTHREAD_T).isPointer();
  }

  public boolean isParameterPresent(PthreadObjectType pObjectType) {
    return tryGetParameterInfoByObjectType(pObjectType).isPresent();
  }

  public int getParameterIndex(PthreadObjectType pObjectType) {
    return getSingleIndex(getParameterInfoByObjectType(pObjectType));
  }

  // Helpers =======================================================================================

  private Optional<PthreadParameterInfo> tryGetParameterInfoByObjectType(
      PthreadObjectType pObjectType) {

    for (PthreadParameterInfo info : parameterInfo) {
      if (info.getObjectType().equals(pObjectType)) {
        return Optional.of(info);
      }
    }
    return Optional.empty();
  }

  private PthreadParameterInfo getParameterInfoByObjectType(PthreadObjectType pObjectType) {
    return tryGetParameterInfoByObjectType(pObjectType).orElseThrow();
  }

  private static int getSingleIndex(PthreadParameterInfo pParameterInfo) {
    checkArgument(
        pParameterInfo.getIndices().size() == 1, "pParameterInfo must have exactly one index");
    return pParameterInfo.getIndices().iterator().next();
  }
}
