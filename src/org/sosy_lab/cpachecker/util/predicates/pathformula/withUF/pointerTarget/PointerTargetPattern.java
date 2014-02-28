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
package org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.pointerTarget;

import java.io.Serializable;

import javax.annotation.Nonnull;

import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaTypeHandler;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PointerTargetSetBuilder;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PointerTargetSetManager;

import com.google.common.base.Predicate;

public class PointerTargetPattern implements Serializable, Predicate<PointerTarget> {

  /**
   * Constructor for matching any possible target
   */
  public PointerTargetPattern() {
    this.matchRange = false;
  }

  /**
   * Constructor for matching targets in the memory block with the specified base name
   * @param base the base name specified
   */
  public PointerTargetPattern(final String base) {
    this.base = base;
    this.matchRange = false;
  }

  public PointerTargetPattern(final String base, final int containerOffset, final int properOffset) {
    this.base = base;
    this.containerOffset = containerOffset;
    this.properOffset = properOffset;
    this.matchRange = false;
  }

  /**
   * Constructor for matching several adjacent targets at once
   * @param base
   * @param startOffset
   * @param endOffset
   */
  public PointerTargetPattern(final int startOffset, final int endOffset) {
    this.containerOffset = startOffset;
    this.properOffset = endOffset;
    this.matchRange = true;
  }

  public static PointerTargetPattern forLeftHandSide(final CLeftHandSide lhs,
      final CtoFormulaTypeHandler pTypeHandler,
      final PointerTargetSetManager pPtsMgr,
      final CFAEdge pCfaEdge,
      final PointerTargetSetBuilder pPts) throws UnrecognizedCCodeException {
    LvalueToPointerTargetPatternVisitor v = new LvalueToPointerTargetPatternVisitor(pTypeHandler, pPtsMgr, pCfaEdge, pPts);
    return lhs.accept(v);
  }

  public void setBase(final String base) {
    this.base = base;
  }

  public void setRange(final int startOffset, final int size) {
    this.containerOffset = startOffset;
    this.properOffset = startOffset + size;
    this.matchRange = true;
    this.containerType = null;
  }

  public void setRange(final int size) {
    assert containerOffset != null && properOffset != null : "Strating address is inexact";
    this.containerOffset += properOffset;
    this.properOffset = containerOffset + size;
    this.matchRange = true;
    this.containerType = null;
  }

  public void setProperOffset(final int properOffset) {
    assert !matchRange : "Contradiction in target pattern: properOffset";
    this.properOffset = properOffset;
  }

  public Integer getProperOffset() {
    assert !matchRange : "Contradiction in target pattern: properOffset";
    return properOffset;
  }

  public Integer getRemainingOffset(PointerTargetSetManager ptsMgr) {
    assert !matchRange : "Contradiction in target pattern: remaining offset";
    if (containerType != null && containerOffset != null && properOffset != null) {
      return ptsMgr.getSize(containerType) - properOffset;
    } else {
      return null;
    }
  }

  /**
   * Increase containerOffset by properOffset, unset properOffset and set containerType.
   * Useful for array subscript visitors.
   * @param containerType
   */
  public void shift(final CType containerType) {
    assert !matchRange : "Contradiction in target pattern: shift";
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
   * @param containerType
   */
  public void shift(final CType containerType, final int properOffset) {
    shift(containerType);
    this.properOffset = properOffset;
  }

  /**
   * Unset everything, except base
   */
  public void retainBase() {
    assert !matchRange : "Contradiction in target pattern: retainBase";
    containerType = null;
    properOffset = null;
    containerOffset = null;
  }

  /**
   * Unset all criteria
   */
  public void clear() {
    assert !matchRange : "Contradiction in target pattern: clear";
    base = null;
    containerType = null;
    properOffset = null;
    containerOffset = null;
  }

  public boolean matches(final @Nonnull PointerTarget target) {
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

  public boolean isExact() {
    return base != null && containerOffset != null && properOffset != null;
  }

  public boolean isSemiexact() {
    return containerOffset != null && properOffset != null;
  }

  private String base = null;
  private CType containerType = null;
  private Integer properOffset = null;
  private Integer containerOffset = null;

  private boolean matchRange = false;

  private static final long serialVersionUID = -2918663736813010025L;
}
