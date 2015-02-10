package org.sosy_lab.cpachecker.util.precondition.segkro;

/* Copyright 2012 LinkedIn Corp.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;

/**
 * Implements the Cartesian product of ordered collections.
 *
 * @author <a href="mailto:jmkristian@gmail.com?subject=Cartesian.product">John
 *         Kristian</a>
 */
@SuppressWarnings({"unused", "unchecked"})
public class Cartesian {
  /**
   * Generate the <a href="http://en.wikipedia.org/wiki/Cartesian_product">Cartesian
   * product</a> of the given axes. For axes [[a1, a2 ...], [b1, b2 ...], [c1, c2 ...]
   * ...] the product is [{a1, b1, c1 ...} ... {a1, b1, c2 ...} ... {a1, b2, c1 ...} ...
   * {aN, bN, cN ...}]. In other words, the results are generated in same order as these
   * nested loops:
   *
   * <pre>
   * for (T a : [a1, a2 ...])
   *   for (T b : [b1, b2 ...])
   *     for (T c : [c1, c2 ...])
   *       ...
   *         result = new T[]{a, b, c ...};
   * </pre>
   *
   * Each result is a new array of T, whose elements refer to the elements of the axes.
   * <p>
   * Don't change the axes while iterating over their product, as a rule. Changes to an
   * axis can affect the product or cause iteration to fail (which is usually bad). To
   * prevent this, you can pass clones of your axes to this method.
   * <p>
   * The implementation is lazy. This method iterates over the axes, and returns an
   * Iterable that contains a reference to each axis. Iterating over the product causes
   * iteration over each axis. Methods of each axis are called as late as practical. The
   * Iterator constructs just one result at a time and doesn't retain references to them.
   * For large axes, this uses much less memory than a collection of the results.
   */
  public static <T> Iterable<T[]> product(Class<T> resultType,
                                          Iterable<? extends Iterable<? extends T>> axes) {
    return new Product<T>(resultType, newArray(Iterable.class, axes));
  }

  /**
   * Like {@link #product(Class,Iterable) product}(resultType, Arrays.asList(axes)), but
   * slightly more efficient.
   */
  public static <T> Iterable<T[]> product(Class<T> resultType, Iterable<? extends T>... axes) {
    return new Product<T>(resultType, axes.clone());
  }

  /**
   * Like {@link #product(Class,Iterable) product}(T.class, axes), except each result is a
   * List instead of an array. So, the result element type can be generic (unlike an
   * array).
   */
  public static <T> Iterable<List<T>> product(Iterable<? extends Iterable<? extends T>> axes) {
    return asLists(product(Object.class, axes));
    // The internal data structures are untyped, but the result is type safe.
  }

  /**
   * Like {@link #product(Iterable) product}(Arrays.asList(axes)), but slightly more
   * efficient.
   */
  public static <T> Iterable<List<T>> product(Iterable<? extends T>... axes) {
    return asLists(product(Object.class, axes));
  }

  // Don't make this public. It's really dangerous.
  private static <T> Iterable<List<T>> asLists(Iterable<Object[]> arrays) {
    return Iterables.transform(arrays, new AsList<T>());
  }

  // Don't make this public. It's really dangerous.
  private static class AsList<T> implements Function<Object[], List<T>> {
    @Override
    @SuppressWarnings("unchecked")
    public List<T> apply(Object[] array) {
      return Arrays.asList((T[]) array);
    }
  }

  /** Create a generic array containing references to the given objects. */
  private static <T> T[] newArray(Class<? super T> elementType, Iterable<? extends T> from) {
    List<T> list = new ArrayList<T>();
    for (T f : from) {
      list.add(f);
    }
    return list.toArray(newArray(elementType, list.size()));
  }

  /** Create a generic array. */
  @SuppressWarnings("unchecked")
  private static <T> T[] newArray(Class<? super T> elementType, int length) {
    return (T[]) Array.newInstance(elementType, length);
  }

  private static class Product<T> implements Iterable<T[]> {
    private final Class<T> _resultType;
    private final Iterable<? extends T>[] _axes;

    /** Caution: the given array of axes is contained by reference, not cloned. */
    Product(Class<T> resultType, Iterable<? extends T>[] axes) {
      _resultType = resultType;
      _axes = axes;
    }

    @Override
    public Iterator<T[]> iterator() {
      if (_axes.length <= 0) {
        return Collections.singletonList(newArray(_resultType, 0)).iterator();
      }
      return new ProductIterator<T>(_resultType, _axes);
    }

    @Override
    public String toString() {
      return "Cartesian.product(" + Arrays.toString(_axes) + ")";
    }

    private static class ProductIterator<T> implements Iterator<T[]> {
      private final Iterable<? extends T>[] _axes;
      private final Iterator<? extends T>[] _iterators; // one per axis
      private final T[] _result; // a copy of the last result
      /**
       * The minimum index such that this.next() will return an array that contains
       * _iterators[index].next(). There are some special sentinel values: NEW means this
       * is a freshly constructed iterator, DONE means all combinations have been
       * exhausted (so this.hasNext() == false) and _iterators.length means the value is
       * unknown (to be determined by this.hasNext).
       */
      private int _nextIndex = NEW;
      private static final int NEW = -2;
      private static final int DONE = -1;

      /** Caution: the given array of axes is contained by reference, not cloned. */
      ProductIterator(Class<T> resultType, Iterable<? extends T>[] axes) {
        _axes = axes;
        _iterators = Cartesian.<Iterator<? extends T>> newArray(Iterator.class, _axes.length);
        for (int a = 0; a < _axes.length; ++a) {
          _iterators[a] = axes[a].iterator();
        }
        _result = newArray(resultType, _iterators.length);
      }

      private void close() {
        _nextIndex = DONE;
        // Release references, to encourage garbage collection:
        Arrays.fill(_iterators, null);
        Arrays.fill(_result, null);
      }

      @Override
      public boolean hasNext() {
        if (_nextIndex == NEW) { // This is the first call to hasNext().
          _nextIndex = 0; // start here
          for (Iterator<? extends T> iter : _iterators) {
            if (!iter.hasNext()) {
              close(); // no combinations
              break;
            }
          }
        } else if (_nextIndex >= _iterators.length) {
          // This is the first call to hasNext() after next() returned a result.
          // Determine the _nextIndex to be used by next():
          for (_nextIndex = _iterators.length - 1; _nextIndex >= 0; --_nextIndex) {
            Iterator<? extends T> iter = _iterators[_nextIndex];
            if (iter.hasNext()) {
              break; // start here
            }
            if (_nextIndex == 0) { // All combinations have been generated.
              close();
              break;
            }
            // Repeat this axis, with the next value from the previous axis.
            iter = _axes[_nextIndex].iterator();
            _iterators[_nextIndex] = iter;
            if (!iter.hasNext()) { // Oops; this axis can't be repeated.
              close(); // no more combinations
              break;
            }
          }
        }
        return _nextIndex >= 0;
      }

      @Override
      public T[] next() {
        if (!hasNext()) {
          throw new NoSuchElementException("!hasNext");
        }
        for (; _nextIndex < _iterators.length; ++_nextIndex) {
          _result[_nextIndex] = _iterators[_nextIndex].next();
        }
        return _result.clone();
      }

      @Override
      public void remove() {
        for (Iterator<? extends T> iter : _iterators) {
          iter.remove();
        }
      }

      @Override
      public String toString() {
        return "Cartesian.product(" + Arrays.toString(_axes) + ").iterator()";
      }
    }
  }
}
