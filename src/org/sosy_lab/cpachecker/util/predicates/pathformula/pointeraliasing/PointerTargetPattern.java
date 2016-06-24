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

import com.google.common.base.Predicate;

import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

import java.io.Serializable;

import javax.annotation.Nullable;

class PointerTargetPattern implements Serializable, Predicate<PointerTarget> {

  private PointerTargetPattern(final String base, final int startOffset, final int size) {
    this.base = base;
    this.containerOffset = startOffset;
    this.properOffset = startOffset + size;
    this.matchRange = true;
    this.containerType = null;
  }

  private PointerTargetPattern(
      @Nullable String pBase,
      @Nullable CType pContainerType,
      @Nullable Integer pProperOffset,
      @Nullable Integer pContainerOffset) {
    matchRange = false;
    base = pBase;
    containerType = pContainerType;
    properOffset = pProperOffset;
    containerOffset = pContainerOffset;
  }

  /**
   * Create PointerTargetPattern matching any possible target.
   */
  static PointerTargetPattern any() {
    return new PointerTargetPattern(null, null, null, null);
  }

  /**
   * Create PointerTargetPattern matching targets in the memory block with the specified base name
   * and offset 0.
   * @param base the base name specified
   */
  static PointerTargetPattern forBase(String base) {
    return new PointerTargetPattern(base, null, 0, 0);
  }

  static PointerTargetPattern forRange(String base, int startOffset, int size) {
    return new PointerTargetPattern(base, startOffset, size);
  }

  static PointerTargetPattern forLeftHandSide(
      final CLeftHandSide lhs,
      final TypeHandlerWithPointerAliasing pTypeHandler,
      final CFAEdge pCfaEdge,
      final PointerTargetSetBuilder pPts)
      throws UnrecognizedCCodeException {
    LvalueToPointerTargetPatternVisitor v =
        new LvalueToPointerTargetPatternVisitor(pTypeHandler, pCfaEdge, pPts);
    return lhs.accept(v).build();
  }

  PointerTargetPattern withRange(final int size) {
    assert containerOffset != null && properOffset != null : "Starting address is inexact";
    return forRange(base, containerOffset + properOffset, size);
  }

  boolean matches(final PointerTarget target) {
    if (!matchRange) {
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
          return ((CArrayType) containerType).getType().equals(((CArrayType) target.containerType).getType());
        }
      }
    } else {
      final int offset = target.containerOffset + target.properOffset;
      if (offset < containerOffset || offset >= properOffset) {
        return false;
      }
      if (base != null && !base.equals(target.base)) {
        return false;
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

  private final @Nullable String base;
  private final @Nullable CType containerType;
  private final @Nullable Integer properOffset;
  private final @Nullable Integer containerOffset;

  private final boolean matchRange;

  private static final long serialVersionUID = -2918663736813010025L;

  static class PointerTargetPatternBuilder {

    private @Nullable String base = null;
    private @Nullable CType containerType = null;
    private @Nullable Integer properOffset = null;
    private @Nullable Integer containerOffset = null;

    private PointerTargetPatternBuilder() {}

    static PointerTargetPatternBuilder any() {
      return new PointerTargetPatternBuilder();
    }

    static PointerTargetPatternBuilder forBase(String pBase) {
      PointerTargetPatternBuilder result = new PointerTargetPatternBuilder();
      result.base = pBase;
      return result;
    }

    private PointerTargetPattern build() {
      return new PointerTargetPattern(base, containerType, properOffset, containerOffset);
    }

    void setProperOffset(final int properOffset) {
      this.properOffset = properOffset;
    }

    @Nullable
    Integer getProperOffset() {
      return properOffset;
    }

    @Nullable
    Integer getRemainingOffset(TypeHandlerWithPointerAliasing typeHandler) {
      if (containerType != null && containerOffset != null && properOffset != null) {
        return typeHandler.getSizeof(containerType) - properOffset;
      } else {
        return null;
      }
    }

    /**
     * Increase containerOffset by properOffset, unset properOffset and set containerType.
     * Useful for array subscript visitors.
     */
    void shift(final CType containerType) {
      this.containerType = containerType;
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
     * Increase containerOffset by properOffset, set properOffset and containerType.
     * Useful for field access visitors.
     */
    void shift(final CType containerType, final int properOffset) {
      shift(containerType);
      this.properOffset = properOffset;
    }

    /**
     * Unset everything, except base
     */
    void retainBase() {
      containerType = null;
      properOffset = null;
      containerOffset = null;
    }

    /**
     * Unset all criteria
     */
    void clear() {
      base = null;
      containerType = null;
      properOffset = null;
      containerOffset = null;
    }
  }
}
