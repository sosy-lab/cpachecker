// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.pthreads;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.Optional;

/**
 * Contains mostly methods from the pthread standard, though we also include e.g. {@code
 * __VERIFIER_atomic_begin} here.
 */
public enum PthreadFunctionType {

  // TODO create barrier logic, see e.g. pthread-divine/barrier_2t.i

  PTHREAD_BARRIER_INIT(
      "pthread_barrier_init",
      false,
      false,
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_BARRIER_WAIT(
      "pthread_barrier_wait",
      false,
      false,
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_CANCEL(
      "pthread_cancel",
      false,
      false,
      Optional.of(0),
      Optional.of(false),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_COND_SIGNAL(
      "pthread_cond_signal",
      false,
      false,
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_COND_WAIT(
      "pthread_cond_wait",
      false,
      false,
      Optional.empty(),
      Optional.empty(),
      Optional.of(1),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_CREATE(
      "pthread_create",
      true,
      true,
      Optional.of(0),
      Optional.of(true),
      Optional.empty(),
      Optional.of(2),
      Optional.of(3),
      Optional.empty()),
  PTHREAD_DETACH(
      "pthread_detach",
      false,
      false,
      Optional.of(0),
      Optional.of(false),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_EQUAL(
      "pthread_equal",
      false,
      false,
      // TODO this method has pthread_t at index 1 and 2 -> add list as param later
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_EXIT(
      "pthread_exit",
      true,
      true,
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.of(0)),
  PTHREAD_GETSPECIFIC(
      "pthread_getspecific",
      false,
      false,
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_JOIN(
      "pthread_join",
      true,
      true,
      Optional.of(0),
      Optional.of(false),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.of(1)),
  PTHREAD_KEY_CREATE(
      "pthread_key_create",
      false,
      false,
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_KILL(
      "pthread_kill",
      false,
      false,
      // TODO actually want Optional.of(0) here, but it causes an ExceptionInInitializerError
      //  -> fix once we support pthread_kill
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_MUTEX_DESTROY(
      "pthread_mutex_destroy",
      true,
      false,
      Optional.empty(),
      Optional.empty(),
      Optional.of(0),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_MUTEX_INIT(
      "pthread_mutex_init",
      true,
      false,
      Optional.empty(),
      Optional.empty(),
      Optional.of(0),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_MUTEX_LOCK(
      "pthread_mutex_lock",
      true,
      true,
      Optional.empty(),
      Optional.empty(),
      Optional.of(0),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_MUTEX_TRYLOCK(
      "pthread_mutex_trylock",
      false,
      false,
      Optional.empty(),
      Optional.empty(),
      Optional.of(0),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_MUTEX_UNLOCK(
      "pthread_mutex_unlock",
      true,
      true,
      Optional.empty(),
      Optional.empty(),
      Optional.of(0),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_ONCE(
      "pthread_once",
      false,
      false,
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_RWLOCK_RDLOCK(
      "pthread_rwlock_rdlock",
      false,
      false,
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_RWLOCK_TRYRDLOCK(
      "pthread_rwlock_tryrdlock",
      false,
      false,
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_RWLOCK_UNLOCK(
      "pthread_rwlock_unlock",
      false,
      false,
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_RWLOCK_WRLOCK(
      "pthread_rwlock_wrlock",
      false,
      false,
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_SELF(
      "pthread_self",
      false,
      false,
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  PTHREAD_SETSPECIFIC(
      "pthread_setspecific",
      false,
      false,
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  __VERIFIER_ATOMIC_BEGIN(
      "__VERIFIER_atomic_begin",
      true,
      true,
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()),
  __VERIFIER_ATOMIC_END(
      "__VERIFIER_atomic_end",
      true,
      true,
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty());

  // TODO unsure about pthread_yield
  //  pthread_barrier stuff
  //  etc. probably a lot more things

  public final String name;

  public final boolean isSupported;

  /**
   * If this function is explicitly handled in the sequentialization, i.e. contains case block code
   * simulating the function's behavior.
   */
  public final boolean isExplicitlyHandled;

  /** The index of the pthread_t param if present. */
  public final Optional<Integer> pthreadTIndex;

  public final Optional<Boolean> isPthreadTPointer;

  /** The index of the pthread_mutex_t param if present. */
  public final Optional<Integer> pthreadMutexTIndex;

  public final Optional<Integer> startRoutineIndex;

  public final Optional<Integer> startRoutineArgumentIndex;

  public final Optional<Integer> returnValueIndex;

  // TODO maybe its best to create a class that states the type of the pthread object, the index /
  //  indices and whether it is never / always / sometimes a pointer
  //  then just create an immutablelist with the desired properties of the function
  PthreadFunctionType(
      String pName,
      boolean pIsSupported,
      boolean pIsExplicitlyHandled,
      Optional<Integer> pPthreadTIndex,
      Optional<Boolean> pIsPthreadTPointer,
      Optional<Integer> pPthreadMutexTIndex,
      Optional<Integer> pStartRoutineIndex,
      Optional<Integer> pStartRoutineArgumentIndex,
      Optional<Integer> pReturnValueIndex) {

    // pPthreadTIndex and pIsPthreadTPointer have to be equivalent (both empty or both present)
    checkArgument(pPthreadTIndex.isEmpty() || pIsPthreadTPointer.isPresent());
    checkArgument(pIsPthreadTPointer.isEmpty() || pPthreadTIndex.isPresent());
    // if the function is not supported, it cannot be explicitly handled
    checkArgument(pIsSupported || !pIsExplicitlyHandled);

    name = pName;
    isSupported = pIsSupported;
    isExplicitlyHandled = pIsExplicitlyHandled;
    pthreadTIndex = pPthreadTIndex;
    isPthreadTPointer = pIsPthreadTPointer;
    pthreadMutexTIndex = pPthreadMutexTIndex;
    startRoutineIndex = pStartRoutineIndex;
    startRoutineArgumentIndex = pStartRoutineArgumentIndex;
    returnValueIndex = pReturnValueIndex;
  }

  public boolean hasPthreadTIndex() {
    return pthreadTIndex.isPresent();
  }

  public int getPthreadTIndex() {
    checkArgument(pthreadTIndex.isPresent(), "this PthreadFuncType has no pthread_t param");
    return pthreadTIndex.orElseThrow();
  }

  // TODO problem: even if the function declaration states that pthread_t is a pointer, the address
  //  may not necessarily be passed on afaik. need more tests here (same with start_routines)
  public boolean isPthreadTPointer() {
    checkArgument(pthreadTIndex.isPresent(), "this PthreadFuncType has no pthread_t param");
    return isPthreadTPointer.orElseThrow();
  }

  public boolean hasPthreadMutexTIndex() {
    return pthreadMutexTIndex.isPresent();
  }

  public int getPthreadMutexTIndex() {
    checkArgument(
        pthreadMutexTIndex.isPresent(), "this PthreadFuncType has no pthread_mutex_t param");
    return pthreadMutexTIndex.orElseThrow();
  }

  public int getStartRoutineIndex() {
    checkArgument(startRoutineIndex.isPresent(), "this PthreadFuncType has no start_routine param");
    return startRoutineIndex.orElseThrow();
  }

  public int getStartRoutineArgumentIndex() {
    checkArgument(
        startRoutineArgumentIndex.isPresent(),
        "this PthreadFuncType has no start_routine arg param");
    return startRoutineArgumentIndex.orElseThrow();
  }

  public int getReturnValueIndex() {
    checkArgument(returnValueIndex.isPresent(), "this PthreadFuncType has no retval param");
    return returnValueIndex.orElseThrow();
  }
}
