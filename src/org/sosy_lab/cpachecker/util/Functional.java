package org.sosy_lab.cpachecker.util;

import java.util.Iterator;

import org.sosy_lab.common.Pair;

import com.google.common.base.Function;

public class Functional {

  private Functional() { }
  
  public static interface BinaryOperator<A, B, R> {
    
    R apply(A pArg0, B pArg1);
  }
  
  public static <A, B, R> BinaryOperator<B, A, R> swapArguments(final BinaryOperator<A, B, R> f) {
    return new BinaryOperator<B, A, R>() {
      @Override
      public R apply(B b, A a) {
        return f.apply(a, b);
      };
    };
  }
  
  public static <A, B, R> BinaryOperator<A, B, R> fromFunction(final Function<Pair<A, B>, R> f) {
    return new BinaryOperator<A, B, R>() {
      @Override
      public R apply(A a, B b) {
        return f.apply(Pair.of(a, b));
      };
    };
  }
  
  public static <A, B, R> Function<Pair<A, B>, R> asFunction(final BinaryOperator<A, B, R> f) {
    return new Function<Pair<A, B>, R>() {
      @Override
      public R apply(Pair<A, B> p) {
        return f.apply(p.getFirst(), p.getSecond());
      };
    };
  }
  
  public static <A, B> A foldl(BinaryOperator<A, B, ? extends A> func, A initial, Iterable<? extends B> list) {
    A result = initial;
    for (B element : list) {
      result = func.apply(result, element);
    }
    return result;
  }
  
  
  public static <A, B> B foldr(BinaryOperator<A, B, ? extends B> f, B z, Iterable<? extends A> xs) {
    return foldr(f, z, xs.iterator());
  }
  
  private static <A, B> B foldr(BinaryOperator<A, B, ? extends B> f, B z, Iterator<? extends A> xs) {
    if (xs.hasNext()) {
      return f.apply(xs.next(), foldr(f, z, xs));
    } else {
      return z;
    }
  }
}
