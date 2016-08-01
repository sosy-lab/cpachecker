/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * The class contains utility functions allowing to workaround (circumvent) Java method typing regarding checked
 * exceptions (and throws clauses), especially for lambda functions. Normally a lambda functions potentially throwing a
 * checked (non-runtime) exception can't be passed to a higher-order method declaring a non-throwing function as its
 * parameter. The class offers the following workaround for this case:
 *  <pre>
 *  {@code
 *  rethrow(CheckedException.class, methodRequiringNonThrowingFunction(catchAll(() -> /* lambda body ... /)));
 *  }
 *  </pre>
 */
public class ExceptionWrapper {

  @FunctionalInterface
  public interface ThrowingConsumer<T> {
     void accept(T t) throws Exception;
  }

  @FunctionalInterface
  public interface ThrowingBiConsumer<T1, T2> {
     void accept(T1 t1, T2 t2) throws Exception;
  }

  @FunctionalInterface
  public interface ThrowingRunnable<E extends Exception> {
    void run() throws E;
  }

  @FunctionalInterface
  public interface ThrowingRunnable2<E1 extends Exception, E2 extends Exception> {
    void run() throws E1, E2;
  }

  private static class WrappedException extends RuntimeException {

    private WrappedException(final Exception e) {
      this.e = e;
    }

    public Exception getException() {
      return e;
    }

    private final Exception e;
    private static final long serialVersionUID = -4533358885010669201L;
  }

  public static <S> Consumer<S> catchAll(final ThrowingConsumer<S> c) {
    return (x) -> {
      try {
        c.accept(x);
      } catch (Exception e) {
        throw new WrappedException(e);
      }};
  }

  public static <S1, S2> BiConsumer<S1, S2> catchAll(final ThrowingBiConsumer<S1, S2> c) {
    return (x, y) -> { try { c.accept(x, y); } catch (Exception e) { throw new WrappedException(e);}};
  }

  public static Runnable catchAll(final ThrowingRunnable<? extends Exception> r) {
    return () -> {
      try {
        r.run();
      } catch (Exception e) {
        throw new WrappedException(e);
      }};
  }

  public static <E extends Exception> void rethrow(final Class<E> cl, final ThrowingRunnable<E> a) throws E {
    try {
      a.run();
    } catch (WrappedException e) {
      final Exception ex = e.getException();
      if (cl.isInstance(ex)) {
        throw cl.cast(ex);
      } else {
        if (ex instanceof RuntimeException) {
          throw (RuntimeException) ex;
        } else {
          throw e;
        }
      }
    }
  }

  public static <E1 extends Exception, E2 extends Exception> void rethrow2(final Class<E1> cl1,
                                                                           final Class<E2> cl2,
                                                                           final ThrowingRunnable2<E1, E2> a)
                                                                               throws E1, E2 {
    try {
      a.run();
    } catch (WrappedException e) {
      final Exception ex = e.getException();
      if (cl1.isInstance(ex)) {
        throw cl1.cast(ex);
      } else {
        if (cl2.isInstance(ex)) {
          throw cl2.cast(ex);
        } else {
          if (ex instanceof RuntimeException) {
            throw (RuntimeException) ex;
          } else {
            throw e;
          }
        }
      }
    }
  }
}
