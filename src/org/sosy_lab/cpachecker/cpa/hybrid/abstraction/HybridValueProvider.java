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
package org.sosy_lab.cpachecker.cpa.hybrid.abstraction;

import javax.annotation.Nullable;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cpa.value.type.BooleanValue;
import org.sosy_lab.cpachecker.cpa.value.type.NumericValue;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

public abstract class HybridValueProvider {

  @Nullable
  public Value delegateVisit(CType type) {

    if(type instanceof CArrayType){
        return visit((CArrayType)type);
    }
    if(type instanceof CCompositeType) {
        return visit((CCompositeType)type);
    }
    if(type instanceof CPointerType) {
        return visit((CPointerType)type);
    }
    if(type instanceof CSimpleType) {
        return visit((CSimpleType)type);
    }
    if(type instanceof CTypedefType) {
        return visit((CTypedefType)type);
    }
    if(type instanceof CBitFieldType) {
        return visit((CBitFieldType)type);
    }

    return null;
  }

  @Nullable
  protected abstract Value visit(CSimpleType type);

  @Nullable
  protected abstract Value visit(CPointerType type);

  @Nullable
  protected abstract Value visit(CArrayType type);

  @Nullable
  protected abstract Value visit(CBitFieldType type);

  @Nullable
  protected abstract Value visit(CCompositeType type);

  @Nullable
  protected abstract Value visit(CTypedefType type);

  /**
   * Checks whether a value for this type can be generated 
   * For fine grained behaviour this basic methid should be overridden by the sub-classes
   * @param pType The type to check
   * @return True, if the value provider can create a value for the given type, else false
   */
  protected boolean isApplicableForValue(CType pType) {

    if(pType instanceof CSimpleType) {
        return true;
    }

    if(pType instanceof CBitFieldType) {
        // in sub-classes a check for CEnumType and CElaborateType might be sensible
        return ((CBitFieldType) pType).getType().getCanonicalType() instanceof CSimpleType;
    }

    if(pType instanceof CPointerType) {
        // basic behaviour, because all value providers should be able to provide a value for a char pointer
        return ((CPointerType) pType).getType() == CNumericTypes.CHAR;
    }

    if(pType instanceof CArrayType) {
        CType type = ((CArrayType) pType).getType();
        // for arrays containing the basic types, also every provider should be able to create values
        return CSimpleType.class.isInstance(type);
    }

    // in deriving classes this checks can be expanded
    return false;
  }

  @Nullable
  protected Class<? extends Value> getClassForCType(CType pType) {

    if(pType instanceof CSimpleType) {
        return getSimpleClassForType((CSimpleType)pType);
    }

    if(pType instanceof CBitFieldType) {
        CBitFieldType bitFieldType = (CBitFieldType) pType;
        CType typeOfBitField = bitFieldType.getType();
        if(typeOfBitField instanceof CSimpleType) {
            return getSimpleClassForType((CSimpleType) typeOfBitField);
        }
    }

    if(pType instanceof CPointerType) {
        return getClassForCType(((CPointerType)pType).getType());
    }

    if(pType instanceof CArrayType) {
        return getClassForCType(((CArrayType)pType).getType());
    }

    return null;
  } 

  @Nullable
  protected Class<? extends Value> getSimpleClassForType(CSimpleType pSimpleType) {
    switch(pSimpleType.getType()) {
        case BOOL : return BooleanValue.class;
        case INT : 
        case FLOAT : 
        case DOUBLE : 
        case CHAR : return NumericValue.class;
        default: return null;
    }
  }
}