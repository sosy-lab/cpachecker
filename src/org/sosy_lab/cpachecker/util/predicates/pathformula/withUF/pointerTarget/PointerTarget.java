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

import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.predicates.pathformula.withUF.PointerTargetSet;

public class PointerTarget implements Serializable {

  /**
   * This constructor is for variables of simple types (e.g. long, char etc.)
   * @param base
   */
  public PointerTarget(String base) {
    this.base = base;
    this.containerType = null;
    this.properOffset = 0;
    this.containerOffset = 0;
  }

  /**
   * This constructor is for structure fields (e.g. s->f) and array elements (e.g. p[5])
   * NOTE: The container (structure or array) must not be contained in any other structure or array
   * @param base
   * @param containerType
   * @param properOffset
   */
  public PointerTarget(String base, CType containerType, int properOffset) {
    this.base = base;
    this.containerType = containerType;
    this.properOffset = properOffset;
    this.containerOffset = 0;
  }

  /**
   * This constructor is for fields of nested structures and arrays
   * @param base
   * @param containerType
   * @param properOffset
   * @param containerOffset
   */
  public PointerTarget(String base, CType containerType, int properOffset, int containerOffset) {
    this.base = base;
    this.containerType = containerType;
    this.properOffset = properOffset;
    this.containerOffset = containerOffset;
  }

  public String getBase() {
    return base;
  }

  public String getBaseName() {
    return PointerTargetSet.getBaseName(base);
  }

  public int getOffset() {
    return containerOffset + properOffset;
  }

  public int getProperOffset() {
    assert containerType != null : "The target's offset is ill-defined";
    return properOffset;
  }

  public boolean isBase() {
    return containerType == null;
  }

  public CType getContainerType() {
    return containerType;
  }

  public int getContainerOffset() {
    assert containerType != null : "The target's container offset is ill-defined";
    return containerOffset;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    } else if (!(other instanceof PointerTarget)) {
      return false;
    } else {
      final PointerTarget o = (PointerTarget) other;
      return properOffset == o.properOffset &&
             containerOffset == o.containerOffset &&
             base.equals(o.base) &&
             (containerType != null ?
                o.containerType != null && containerType.getCanonicalType().equals(o.containerType.getCanonicalType()) :
                o.containerType == null);
    }
  }

  @Override
  public int hashCode() {
    return 31 * base.hashCode() + 17 * containerOffset + properOffset;
  }

  final String base;
  final CType containerType;
  final int properOffset;
  final int containerOffset;

  private static final long serialVersionUID = -1258065871533686442L;
}
