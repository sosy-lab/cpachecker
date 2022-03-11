// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.Predicate;
import java.io.Serializable;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

class PointerTargetPattern implements Serializable, Predicate<PointerTarget> {

  private PointerTargetPattern(
      @Nullable String pBase,
      @Nullable CType pContainerType,
      @Nullable Long pProperOffset,
      @Nullable Long pContainerOffset) {
    base = pBase;
    containerType = pContainerType;
    properOffset = pProperOffset;
    containerOffset = pContainerOffset;
  }

  /** Create PointerTargetPattern matching any possible target. */
  static PointerTargetPattern any() {
    return new PointerTargetPattern(null, null, null, null);
  }

  /**
   * Create PointerTargetPattern matching targets in the memory block with the specified base name
   * and offset 0.
   *
   * @param base the base name specified
   */
  static PointerTargetPattern forBase(String base) {
    return new PointerTargetPattern(base, null, 0L, 0L);
  }

  static Predicate<PointerTarget> forRange(String base, long startOffset, int size) {
    return new RangePointerTargetPattern(base, startOffset, size);
  }

  static PointerTargetPattern forLeftHandSide(
      final CLeftHandSide lhs,
      final TypeHandlerWithPointerAliasing pTypeHandler,
      final CFAEdge pCfaEdge,
      final PointerTargetSetBuilder pPts)
      throws UnrecognizedCodeException {
    LvalueToPointerTargetPatternVisitor v =
        new LvalueToPointerTargetPatternVisitor(pTypeHandler, pCfaEdge, pPts);
    return lhs.accept(v).build();
  }

  Predicate<PointerTarget> withRange(final int size) {
    assert containerOffset != null && properOffset != null : "Starting address is inexact";
    return forRange(base, containerOffset + properOffset, size);
  }

  boolean matches(final PointerTarget target) {
    if (properOffset != null && properOffset != target.properOffset) {
      return false;
    }
    if (containerOffset != null && containerOffset != target.containerOffset) {
      return false;
    }
    if (base != null && !base.equals(target.base)) {
      return false;
    }
    if (containerType != null && !containerType.equals(target.containerType)) {
      if (!(containerType instanceof CArrayType) || !(target.containerType instanceof CArrayType)) {
        return false;
      } else {
        return ((CArrayType) containerType)
            .getType()
            .equals(((CArrayType) target.containerType).getType());
      }
    }
    return true;
  }

  @Override
  @Deprecated // call matches(), it has a better name
  public boolean apply(PointerTarget pInput) {
    return matches(pInput);
  }

  boolean isExact() {
    return base != null && containerOffset != null && properOffset != null;
  }

  boolean isSemiExact() {
    return containerOffset != null && properOffset != null;
  }

  PointerTarget asPointerTarget() {
    checkArgument(isExact());
    return new PointerTarget(base, containerType, properOffset, containerOffset);
  }

  private final @Nullable String base;
  private final @Nullable CType containerType;
  private final @Nullable Long properOffset;
  private final @Nullable Long containerOffset;

  private static final long serialVersionUID = -2918663736813010025L;

  private static class RangePointerTargetPattern implements Predicate<PointerTarget> {

    private final String base;
    private final long startOffset;
    private final long endOffset;

    private RangePointerTargetPattern(
        final String pBase, final long pStartOffset, final int pSize) {
      base = pBase;
      startOffset = pStartOffset;
      endOffset = pStartOffset + pSize;
    }

    @Override
    public boolean apply(final PointerTarget target) {
      final long offset = target.containerOffset + target.properOffset;
      if (offset < startOffset || offset >= endOffset) {
        return false;
      }
      if (base != null && !base.equals(target.base)) {
        return false;
      }
      return true;
    }
  }

  static class PointerTargetPatternBuilder {

    private @Nullable String base = null;
    private @Nullable CType containerType = null;
    private @Nullable Long properOffset = null;
    private @Nullable Long containerOffset = null;

    private PointerTargetPatternBuilder() {}

    static PointerTargetPatternBuilder any() {
      return new PointerTargetPatternBuilder();
    }

    static PointerTargetPatternBuilder forBase(String pBase) {
      PointerTargetPatternBuilder result = new PointerTargetPatternBuilder();
      result.base = pBase;
      result.properOffset = 0L;
      result.containerOffset = 0L;
      return result;
    }

    private PointerTargetPattern build() {
      return new PointerTargetPattern(base, containerType, properOffset, containerOffset);
    }

    void setProperOffset(final long properOffset) {
      this.properOffset = properOffset;
    }

    @Nullable Long getProperOffset() {
      return properOffset;
    }

    @Nullable Long getRemainingOffset(TypeHandlerWithPointerAliasing typeHandler) {
      if (containerType != null && containerOffset != null && properOffset != null) {
        return typeHandler.getSizeof(containerType) - properOffset;
      } else {
        return null;
      }
    }

    /**
     * Increase containerOffset by properOffset, unset properOffset and set containerType. Useful
     * for array subscript visitors.
     */
    void shift(final CType pContainerType) {
      containerType = pContainerType;
      if (containerOffset != null) {
        if (properOffset != null) {
          containerOffset += properOffset;
        } else {
          containerOffset = null;
        }
      }
      properOffset = null;
    }

    /**
     * Increase containerOffset by properOffset, set properOffset and containerType. Useful for
     * field access visitors.
     */
    void shift(final CType pContainerType, final long pProperOffset) {
      shift(pContainerType);
      properOffset = pProperOffset;
    }

    /** Unset everything, except base */
    void retainBase() {
      containerType = null;
      properOffset = null;
      containerOffset = null;
    }

    /** Unset all criteria */
    void clear() {
      base = null;
      containerType = null;
      properOffset = null;
      containerOffset = null;
    }
  }
}
