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

import org.sosy_lab.cpachecker.cfa.types.c.CType;

import com.google.common.base.Preconditions;


public class PointerTargetPattern implements Serializable {

  /**
   * Constructor for matching any possible target
   */
  public PointerTargetPattern() {
    this.matchBase = false;
    this.matchContainerType = false;
    this.matchProperOffset = false;
    this.matchContainerOffset = false;
    this.matchOffset = false;
  }

  /**
   * Constructor for matching array elements
   * @param containerType
   */
  public PointerTargetPattern(CType containerType) {
    this.matchBase = false;
    this.containerType = containerType;
    this.matchContainerType = true;
    this.matchProperOffset = false;
    this.matchContainerOffset = false;
    this.matchOffset = false;
  }

  /**
   * Constructor for matching structure fields
   * @param containerType
   * @param properOffset
   */
  public PointerTargetPattern(CType containerType, int properOffset) {
    this.matchBase = false;
    this.containerType = containerType;
    this.matchContainerType = true;
    this.properOffset = properOffset;
    this.matchProperOffset = true;
    this.matchContainerOffset = false;
    this.matchOffset = false;
  }

  /**
   * Constructor for matching several adjacent targets at once
   * @param base
   * @param startOffset
   * @param endOffset
   */
  public PointerTargetPattern(int startOffset, int endOffset) {
    this.matchBase = false;
    this.matchContainerType = false;
    this.matchProperOffset = false;
    this.matchContainerOffset = false;
    this.containerOffset = startOffset;
    this.properOffset = endOffset;
    this.matchOffset = true;
  }

  public void setBase(String base) {
    assert !matchBase : "Contradiction in target pattern: base";
    this.base = base;
    this.matchBase = true;
  }

  public void setContainerType(CType containerType) {
    assert !matchContainerType && !matchOffset : "Contradiction in target pattern: containerType";
    this.containerType = containerType;
    this.matchContainerType = true;
  }

  public void setProperOffset(int properOffset) {
    assert !matchProperOffset && !matchOffset : "Contradiction in target pattern: properOffset";
    this.properOffset = properOffset;
    this.matchProperOffset = true;
  }

  public void setContainerOffset(int containerOffset) {
    assert !matchContainerOffset && !matchOffset : "Contradiction in target pattern: containerOffset";
    this.containerOffset = containerOffset;
    this.matchContainerOffset = true;
  }

  public void addContainerOffset(int dContainerOffset) {
    assert !matchOffset : "Contradiction in target pattern: containerOffset";
    Preconditions.checkArgument(dContainerOffset > 0);
    if (matchContainerOffset) {
      containerOffset += dContainerOffset;
    } else {
      containerOffset = dContainerOffset;
      matchContainerOffset = true;
    }
  }

  public void unsetContainerOffset() {
    assert matchContainerOffset && !matchOffset : "Contradiction in target pattern: containerOffset";
    matchContainerOffset = false;
  }

  public boolean matches(PointerTarget target) {
    if (!matchOffset) {
      if (matchProperOffset && target.properOffset != properOffset) {
        return false;
      }
      if (matchContainerOffset && target.containerOffset != containerOffset) {
        return false;
      }
      if (matchBase && !target.base.equals(base)) {
        return false;
      }
      if (matchContainerType && !target.containerType.equals(containerType)) {
        return false;
      }
    } else {
      final int offset = target.containerOffset + target.properOffset;
      if (offset < containerOffset || offset >= properOffset) {
        return false;
      }
      if (matchBase && !target.base.equals(base)) {
        return false;
      }
    }
    return true;
  }

  private String base = null;
  private CType containerType = null;
  private int properOffset = 0;
  private int containerOffset = 0;

  private boolean matchBase = false;
  private boolean matchContainerType = false;
  private boolean matchProperOffset = false;
  private boolean matchContainerOffset = false;
  private boolean matchOffset = false;

  private static final long serialVersionUID = -2918663736813010025L;
}
