// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.types.c.CType;

class DefaultRegionManager extends AbstractMemoryRegionManager implements MemoryRegionManager {

  private static final class DefaultMemoryRegion implements MemoryRegion {

    private final CType type;

    DefaultMemoryRegion(CType pType) {
      type = pType;
    }

    @Override
    public CType getType() {
      return type;
    }

    @Override
    public String getName(TypeHandlerWithPointerAliasing typeHandler) {
      return typeHandler.getPointerAccessNameForType(type);
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

  DefaultRegionManager(TypeHandlerWithPointerAliasing pTypeHandler) {
    super(pTypeHandler);
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
