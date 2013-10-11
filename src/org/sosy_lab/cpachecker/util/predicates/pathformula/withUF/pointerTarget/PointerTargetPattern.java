/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PointerTargetSet.PointerTargetSetBuilder;

public class PointerTargetPattern implements Serializable {

  /**
   * Constructor for matching any possible target
   */
  public PointerTargetPattern() {
    this.matchRange = false;
  }

  public PointerTargetPattern(final String base) {
    this.base = base;
    this.matchRange = false;
  }

  /**
   * Constructor for matching array elements
   * @param containerType
   */
  public PointerTargetPattern(final CType containerType) {
    this.containerType = containerType;
    this.matchRange = false;
  }

  /**
   * Constructor for matching structure fields
   * @param containerType
   * @param properOffset
   */
  public PointerTargetPattern(final CType containerType, final int properOffset) {
    this.containerType = containerType;
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

  public void setBase(final String base) {
    this.base = base;
  }

  public void setContainerType(final CType containerType) {
    assert !matchRange : "Contradiction in target pattern: containerType";
    this.containerType = containerType;
  }

  public void unsetContainerType() {
    assert !matchRange : "Contradiction in target pattern: containerType";
    containerType = null;
  }

  public void setProperOffset(final int properOffset) {
    assert !matchRange : "Contradiction in target pattern: properOffset";
    this.properOffset = properOffset;
  }

  public void unsetProperOffset() {
    assert !matchRange : "Contradiction in target pattern: properOffset";
    properOffset = null;
  }

  public void setContainerOffset(final int containerOffset) {
    assert !matchRange : "Contradiction in target pattern: containerOffset";
    this.containerOffset = containerOffset;
  }

  public void shiftContainerOffset() {
    assert !matchRange : "Contradiction in target pattern: containerOffset";
    if (properOffset != null) {
      containerOffset += properOffset;
    } else {
      containerOffset = null;
    }
    properOffset = null;
  }

  public void unsetContainerOffset() {
    assert !matchRange : "Contradiction in target pattern: containerOffset";
    containerOffset = null;
  }

  public boolean matches(final PointerTarget target) {
    if (!matchRange) {
      if (properOffset != null && target.properOffset != properOffset) {
        return false;
      }
      if (containerOffset != null && target.containerOffset != containerOffset) {
        return false;
      }
      if (base != null && !target.base.equals(base)) {
        return false;
      }
      if (containerType != null && !target.containerType.equals(containerType)) {
        if (!(target.containerType instanceof CArrayType) || !(containerType instanceof CArrayType)) {
          return false;
        } else {
          return ((CArrayType) target.containerType).getType().equals(((CArrayType) containerType).getType());
        }
      }
    } else {
      final int offset = target.containerOffset + target.properOffset;
      if (offset < containerOffset || offset >= properOffset) {
        return false;
      }
      if (base != null && !target.base.equals(base)) {
        return false;
      }
    }
    return true;
  }

  public Integer getProperOffset() {
    return properOffset;
  }

  public Integer getRemaining(PointerTargetSetBuilder pts) {
    if (containerOffset != null && properOffset != null) {
      return pts.getSize(containerType) - properOffset;
    } else {
      return null;
    }
  }

  private String base = null;
  private CType containerType = null;
  private Integer properOffset = null;
  private Integer containerOffset = null;

  private boolean matchRange = false;

  private static final long serialVersionUID = -2918663736813010025L;
}
