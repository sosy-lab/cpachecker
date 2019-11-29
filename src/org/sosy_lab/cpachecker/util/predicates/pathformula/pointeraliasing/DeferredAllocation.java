/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Optional;
import javax.annotation.concurrent.Immutable;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.globalinfo.GlobalInfo;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.Formula;

/**
 * This class is used to temporarily keep data specifying an already performed, but deferred
 * memory allocation of unknown type, e.g.
 *
 *   <pre>
 *   void *tmp_0 = malloc(size); // tmp_0 is a pointer variable, allocation type is unknown
 *   ...
 *   void *tmp_2 = tmp_0; // Now tmp_2 is also a pointer variable corresponding to the same allocation
 *   struct s* ps = (struct s*)tmp_2; // Now the actual type of the allocation is revealed
 *   </pre>
 * <p>
 * Deferring the allocation makes the analysis a lot more precise since the tracked memory locations are separated by
 * type and (optionally) structure fields, so to handle heap updates correctly the precise type of the allocated
 * objects must be known.
 * </p>
 *
 * <p>
 * As a reasonable approximation we assume a temporarily unallocated object can be pointed by one or several
 * variables and/or structure fields (over-approximated across concrete structure instances).
 * This should arguably provide acceptable precision in determining the actual type of the object.
 * </p>
 *
 * <p>
 * When the type of the allocation is revealed (by the context in which one of the void pointer
 * variables/fields is used), the actual allocation occurs (the address disjointness constraint is added to the path
 * formula and pointer targets are added to the pointer target set).
 * </p>
 *
 * <p>
 * The mapping between void pointer variables/fields and deferred allocation pools corresponding to yet unallocated
 * objects is many-to-many relation. All the necessary over-approximations e.g. merges should be done externally, this
 * class instances should only keep data about a single object whose allocation is deferred.
 * </p>
 */
@Immutable
class DeferredAllocation implements Serializable {
  private static final long serialVersionUID = -6882598785306470437L;

  DeferredAllocation(
      final String base,
      final Optional<CIntegerLiteralExpression> size,
      final Formula pSizeExp,
      final boolean isZeroed) {
    this.isZeroed = isZeroed;
    this.size = size;
    sizeExp = pSizeExp;
    this.base = base;
  }

  String getBase() {
    return base;
  }

  boolean isZeroed() {
    return isZeroed;
  }

  boolean hasKnownSize() {
    return size.isPresent();
  }

  Optional<CIntegerLiteralExpression> getSize() {
    return size;
  }

  Formula getSizeExpression() {
    return sizeExp;
  }

  @Override
  public boolean equals(final Object other) {
    if (this == other) {
      return true;
    }
    if (!(other instanceof DeferredAllocation)) {
      return false;
    }
    final DeferredAllocation otherPool = (DeferredAllocation) other;
    // isZeroed and size can be different in case of merging two paths with different allocations,
    // currently base indices are not globally unique (see #215 for why it should be this way)
    if (base.equals(otherPool.base)
        && isZeroed == otherPool.isZeroed
        && sizeExp.equals(otherPool.sizeExp)
        && size.equals(otherPool.size)) {
      // pointedBy is not counted as this is a helper field, not a characteristic of the allocation
      return true;
    } else {
      return false;
    }
  }

  @Override
  public int hashCode() {
    int result = 0;
    result += base.hashCode() * 997;
    result += size.hashCode() * 617;
    result += sizeExp.hashCode() * 617;
    result += isZeroed ? 307 : 0;
    return result;
  }

  private final boolean isZeroed;
  private final Optional<CIntegerLiteralExpression> size;
  private final Formula sizeExp;
  private final String base;

  private Object writeReplace() {
    return new SerializationProxy(this);
  }

  /**
   * javadoc to remove unused parameter warning
   *
   * @param in the input stream
   */
  @SuppressWarnings("UnusedVariable") // parameter is required by API
  private void readObject(ObjectInputStream in) throws IOException {
    throw new InvalidObjectException("Proxy required");
  }

  private static class SerializationProxy implements Serializable {

    private static final long serialVersionUID = 4850967154964188729L;
    private final boolean isZeroed;
    private final String base;
    private final long size;
    private final @Nullable CType sizeType;
    private final String sizeExp;

    private SerializationProxy(DeferredAllocation pDeferredAllocationPool) {
      isZeroed = pDeferredAllocationPool.isZeroed;
      base = pDeferredAllocationPool.base;
      FormulaManagerView mgr = GlobalInfo.getInstance().getPredicateFormulaManagerView();
      sizeExp = mgr.dumpArbitraryFormula(pDeferredAllocationPool.sizeExp);
      if (pDeferredAllocationPool.size.isPresent()) {
        size = pDeferredAllocationPool.size.orElseThrow().asLong();
        sizeType = pDeferredAllocationPool.size.orElseThrow().getExpressionType();
      } else {
        size = -1;
        sizeType = null;
      }
    }

    private Object readResolve() {
      return new DeferredAllocation(
          base,
          sizeType != null
              ? Optional.of(CIntegerLiteralExpression.createDummyLiteral(size, sizeType))
              : Optional.empty(),
          GlobalInfo.getInstance().getPredicateFormulaManagerView().parseArbitraryFormula(sizeExp),
          isZeroed);
    }
  }
}
