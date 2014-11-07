package org.sosy_lab.cpachecker.cpa.stator.memory;

import com.google.common.collect.ImmutableSet;

import java.util.Iterator;
import java.util.Set;

public class AbstractMemoryAddress implements Iterable<MemorySegment>{

  public static final AbstractMemoryAddress BOTTOM = new AbstractMemoryAddress(
      ImmutableSet.<MemorySegment>of()
  );

  public static final AbstractMemoryAddress TOP = new AbstractMemoryAddress(
      null
  );

  private final ImmutableSet<MemorySegment> addresses;

  private AbstractMemoryAddress(
      ImmutableSet<MemorySegment> addresses
  ) {
    this.addresses = addresses;
  }

  public static AbstractMemoryAddress ofAddresses(
      Iterable<MemorySegment> addresses) {
    return new AbstractMemoryAddress(ImmutableSet.copyOf(addresses));
  }

  /**
   * @return New {@link AbstractMemoryAddress} representing the merge
   * of two states.
   */
  public AbstractMemoryAddress join(AbstractMemoryAddress o) {
    if (o == TOP || this == TOP) return TOP;
    return new AbstractMemoryAddress(
        ImmutableSet.<MemorySegment>builder()
            .addAll(addresses)
            .addAll(o.addresses)
            .build()
    );
  }

  @SuppressWarnings("unused")
  public boolean containsAddress(MemorySegment address) {
    return this == TOP || addresses.contains(address);
  }

  public boolean isPointerType() {
    // TODO: might not be needed.
    if (this == TOP) return true;
    for (MemorySegment address : addresses) {
      if (!address.isPointerType()) return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    return addresses.hashCode();
  }

  /**
   * Canonical comparison.
   * With the {@code offsetUnknown} set to true the canonicity
   * property is lost, and we must convert to segments before comparing.
   */
  public LatticeComparison compare(AbstractMemoryAddress o) {
    return compareSets(addresses, o.addresses);
  }

  @Override
  public boolean equals(Object o) {
    return o == this ||
             o != null &&
             o instanceof AbstractMemoryAddress &&
             compare((AbstractMemoryAddress)o) == LatticeComparison.EQUAL;
  }

  public boolean isLessOrEqual(AbstractMemoryAddress o) {
    LatticeComparison c = compare(o);
    return c == LatticeComparison.EQUAL || c == LatticeComparison.LESS;
  }

  private static <T> LatticeComparison compareSets(Set<T> lhs, Set<T> rhs) {
    if (lhs.equals(rhs)) return LatticeComparison.EQUAL;
    if (rhs.containsAll(lhs)) return LatticeComparison.LESS;
    if (lhs.containsAll(rhs)) return LatticeComparison.GREATER;
    return LatticeComparison.NON_COMPARABLE;
  }

  @Override
  public Iterator<MemorySegment> iterator() {
    return addresses.iterator();
  }

  public int size() {
    return addresses.size();
  }

  @Override
  public String toString() {
    if (this == TOP) {
      return "TOP";
    }
    return addresses.toString();
  }


  public static enum LatticeComparison {
    LESS,
    EQUAL,
    NON_COMPARABLE,
    GREATER
  }
}
