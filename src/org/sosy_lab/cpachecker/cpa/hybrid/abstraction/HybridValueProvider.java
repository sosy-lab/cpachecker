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

import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cpa.hybrid.value.HybridValue;
import org.sosy_lab.cpachecker.exceptions.NoException;

public abstract class HybridValueProvider implements CTypeVisitor<HybridValue, NoException> {
    
    public HybridValue delegateVisit(CType type) {

        if(type instanceof CArrayType){
            return visit((CArrayType)type);
        }
        if(type instanceof CCompositeType) {
            return visit((CCompositeType)type);
        }
        if(type instanceof CElaboratedType) {
            return visit((CElaboratedType)type);
        }
        if(type instanceof CEnumType) {
            return visit((CEnumType)type);
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

        // VoidType, FunctionType, CProblemType
        throw new IllegalArgumentException("Only assignable types are applicable for a Hybrid Value Provider.");

    }
}