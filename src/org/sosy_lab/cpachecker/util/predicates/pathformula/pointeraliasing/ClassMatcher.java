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

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The class offers a convenient (but somewhat less efficient) alternative to a chain of {@code instanceof} checks:
 * <pre>
 * if (obj instancef Class1) {
 *   final Class1 casted = (Class1) obj;
 *   // body1 ...
 * } else if (obj instancef Class2) {
 *   final Class2 casted = (Class2) obj;
 *   // body2 ...
 * } else {
 *    // body3 ...
 * }
 * </pre>
 * can be encoded as:
 * <pre>
 * {@code
 * match(obj).with(Class1.class, (casted) -> /* body1 /).or(Class2.class, (casted) -> /* body2 /).orElse(/* body3 /);
 * }
 * </pre>
 */
public class ClassMatcher {
  public static class ClassMatcherWithResult<R> {
    private ClassMatcherWithResult(final Object scrutinee, final R result) {
      this.scrutinee = scrutinee;
      this.result = Optional.of(result);
    }

    private ClassMatcherWithResult(final Object scrutinee) {
      this.scrutinee = scrutinee;
      this.result = Optional.empty();
    }

    public <Y> ClassMatcherWithResult<R> or(
        final Class<Y> targetClass, final Function<? super Y, ? extends R> f) {
      if (result.isPresent()) {
        return this;
      }
      if (targetClass.isInstance(scrutinee)) {
        result = Optional.of(f.apply(targetClass.cast(scrutinee)));
      }
      return this;
    }

    public ClassMatcherWithResult<R> orNull(final Supplier<R> r) {
      if (result.isPresent()) {
        return this;
      }
      if (scrutinee == null) {
        result = Optional.of(r.get());
      }
      return this;
    }

    public R orElse(final R r) {
      return result.orElse(r);
    }

    public R orElseGet(final Supplier<? extends R> s) {
      return result.orElseGet(s);
    }

    public <X extends Throwable> R orElseThrow(final Supplier<X> e) throws X {
      return result.orElseThrow(e);
    }

    public @Nonnull Optional<R> result() {
      return result;
    }

    private final @Nullable Object scrutinee;
    private @Nonnull Optional<R> result = Optional.empty();
  }

  public static class ClassMatcherWithoutResult {
    private ClassMatcherWithoutResult(final Object scrutinee, final boolean matched) {
      this.scrutinee = scrutinee;
      this.matched = matched;
    }

    private ClassMatcherWithoutResult(final Object scrutinee) {
      this.scrutinee = scrutinee;
      this.matched = false;
    }

    public <Y> ClassMatcherWithoutResult or(
        final Class<Y> targetClass, final Consumer<? super Y> f) {
      if (matched) {
        return this;
      }
      if (targetClass.isInstance(scrutinee)) {
        f.accept(targetClass.cast(scrutinee));
        matched = true;
      }
      return this;
    }

    public ClassMatcherWithoutResult orNull(final Runnable r) {
      if (matched) {
        return this;
      }
      if (scrutinee == null) {
        r.run();
        matched = true;
      }
      return this;
    }

    public void orElseRun(final Runnable r) {
      if (!matched) {
        r.run();
      }
    }

    public <X extends Throwable> void orElseThrow(final Supplier<X> e) throws X {
      if (!matched) {
        throw e.get();
      }
    }

    public void end() {}

    public boolean matched() {
      return matched;
    }

    private final @Nullable Object scrutinee;
    private boolean matched;
  }

  private ClassMatcher(Object scrutinee) {
    this.scrutinee = scrutinee;
  }

  public static ClassMatcher match(final @Nullable Object scrutinee) {
    return new ClassMatcher(scrutinee);
  }

  public <Y, R> ClassMatcherWithResult<R> with(
      final Class<Y> targetClass, final Function<? super Y, ? extends R> f) {
    if (targetClass.isInstance(scrutinee)) {
      return new ClassMatcherWithResult<>(scrutinee, f.apply(targetClass.cast(scrutinee)));
    }
    return new ClassMatcherWithResult<>(scrutinee);
  }

  public <Y, R> ClassMatcherWithResult<R> with(
      final Class<Y> targetClass, final Supplier<? extends R> f) {
    if (targetClass.isInstance(scrutinee)) {
      return new ClassMatcherWithResult<>(scrutinee, f.get());
    }
    return new ClassMatcherWithResult<>(scrutinee);
  }

  public <Y> ClassMatcherWithoutResult with_(
      final Class<Y> targetClass, final Consumer<? super Y> f) {
    if (targetClass.isInstance(scrutinee)) {
      f.accept(targetClass.cast(scrutinee));
      return new ClassMatcherWithoutResult(scrutinee, true);
    }
    return new ClassMatcherWithoutResult(scrutinee);
  }

  private final @Nullable Object scrutinee;
}
