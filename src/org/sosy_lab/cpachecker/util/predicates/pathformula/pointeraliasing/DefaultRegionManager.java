/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.types.c.CType;


class DefaultRegionManager extends AbstractMemoryRegionManager implements MemoryRegionManager {

  private static class DefaultMemoryRegion implements MemoryRegion {

    private final CType type;

    protected DefaultMemoryRegion(CType pType) {
      this.type = pType;
    }

    @Override
    public CType getType() {
      return type;
    }

    @Override
    public String getName() {
      return CToFormulaConverterWithPointerAliasing.getPointerAccessNameForType(type);
    }

    @Override
    public String toString() {
      return "DefaultMemoryRegion [type=" + type + "]";
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + type.hashCode();
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      DefaultMemoryRegion other = (DefaultMemoryRegion) obj;
      return type.equals(other.type);
    }
  }

  @Override
  public String getPointerAccessName(MemoryRegion pRegion) {
    checkNotNull(pRegion);
    return pRegion.getName();
  }

  @Override
  public MemoryRegion makeMemoryRegion(CType pType) {
    checkNotNull(pType);
    CTypeUtils.checkIsSimplified(pType);
    return new DefaultMemoryRegion(pType);
  }

  @Override
  public MemoryRegion makeMemoryRegion(CType pType, CType pExpressionType, String pFieldName) {
    checkNotNull(pType);
    checkNotNull(pExpressionType);
    checkNotNull(pFieldName);
    CTypeUtils.checkIsSimplified(pExpressionType);
    return new DefaultMemoryRegion(pExpressionType);
  }
}
