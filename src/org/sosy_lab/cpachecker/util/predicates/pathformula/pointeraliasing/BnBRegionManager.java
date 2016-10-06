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
import static org.sosy_lab.cpachecker.util.statistics.StatisticsUtils.valueWithPercentage;

import com.google.common.collect.Multimap;
import java.io.PrintStream;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.VariableClassification;

/**
 * Class implements so called B&B memory model.
 * B&B stands for Rod Burstall and Richard Bornat.
 *
 * In B&B model fields of struct-types can exist in separate memory space from the other fields and
 * pointers of the same type if there is no address-taking of the field in program. Otherwise the
 * field would exist in the memory space with entities mentioned above. That allows to eliminate
 * false positives that are caused by the assumptions made by the analysis due to unknown body
 * of the memory-returning functions.
 *
 */
class BnBRegionManager extends AbstractMemoryRegionManager implements MemoryRegionManager {
  private static final String GLOBAL = "global";
  private static final String SEPARATOR = "_";


  private static class GlobalBnBRegion implements MemoryRegion {
    @Override
    public String toString() {
      return "GlobalBnBRegion [type=" + type + "]";
    }

    private final CType type;

    protected GlobalBnBRegion(CType pType) {
      this.type = pType;
    }

    @Override
    public CType getType() {
      return type;
    }

    @Override
    public String getName() {
      return
          CToFormulaConverterWithPointerAliasing.getPointerAccessNameForType(type)
          + SEPARATOR
          + GLOBAL;
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
      GlobalBnBRegion other = (GlobalBnBRegion) obj;
      return type.equals(other.type);
    }

  }

  private static class FieldBnBRegion implements MemoryRegion {

    private final CType fieldOwnerType;
    private final CType fieldType;
    private final String fieldName;

    protected FieldBnBRegion(CType pFieldOwnerType, CType pFieldType, String pFieldName) {
      fieldOwnerType = pFieldOwnerType;
      fieldType = pFieldType;
      fieldName = pFieldName;
    }

    @Override
    public CType getType() {
      return fieldType;
    }

    @Override
    public String getName() {
      return CToFormulaConverterWithPointerAliasing.getPointerAccessNameForType(fieldOwnerType)
      + SEPARATOR
      + fieldName;
    }

    @Override
    public String toString() {
      return "FieldBnBRegion [fieldOwnerType=" + fieldOwnerType
          + ", fieldType=" + fieldType
          + ", fieldName=" + fieldName + "]";
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + fieldName.hashCode();
      result = prime * result + fieldType.hashCode();
      result = prime * result + fieldOwnerType.hashCode();
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
      FieldBnBRegion other = (FieldBnBRegion) obj;
      return fieldName.equals(other.fieldName)
          && fieldType.equals(other.fieldType)
          && fieldOwnerType.equals(other.fieldOwnerType);
    }
  }

  private final Optional<VariableClassification> varClassification;
  private final Multimap<CType, String> fieldRegions;

  BnBRegionManager(Optional<VariableClassification> var, Multimap<CType, String> fieldRegions) {
    this.fieldRegions = fieldRegions;
    this.varClassification = var;
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
    return new GlobalBnBRegion(pType);
  }

  @Override
  public MemoryRegion makeMemoryRegion(CType pFieldOwnerType, CType pFieldType,
      String pFieldName) {
    checkNotNull(pFieldOwnerType);
    checkNotNull(pFieldType);
    checkNotNull(pFieldName);
    CTypeUtils.checkIsSimplified(pFieldOwnerType);
    CTypeUtils.checkIsSimplified(pFieldType);
    if(fieldRegions.containsEntry(pFieldOwnerType, pFieldName)) {
      //common case - likely
      return new FieldBnBRegion(pFieldOwnerType, pFieldType, pFieldName);
    } else {
      //field inside global region - unlikely
      return new GlobalBnBRegion(pFieldType);
    }
  }

  @Override
  public void printStatistics(PrintStream out) {
    super.printStatistics(out);

    String bnbSize;
    if(varClassification.isPresent()) {
      VariableClassification var = varClassification.get();
      int relevantSize = var.getRelevantFields().size();
      int addressedSize = var.getAddressedFields().size();
      out.println("Number of relevant fields:    " + relevantSize);
      out.println("Number of addressed fields:   " + addressedSize);
      bnbSize = valueWithPercentage(fieldRegions.size(), relevantSize);
    } else {
      bnbSize = fieldRegions.size() + " ()";
    }
    out.println("Number of BnB memory regions: " + bnbSize);
    out.println();
  }
}
