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
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
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

  public abstract Value visit(CSimpleType type);

  public abstract Value visit(CPointerType type);

  public abstract Value visit(CArrayType type);

  public abstract Value visit(CBitFieldType type);

  public abstract Value visit(CCompositeType type);

  public abstract Value visit(CTypedefType type);
}