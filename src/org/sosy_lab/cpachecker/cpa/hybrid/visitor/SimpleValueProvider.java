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
package org.sosy_lab.cpachecker.cpa.hybrid.visitor;

import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cpa.hybrid.abstraction.HybridValueProvider;
import org.sosy_lab.cpachecker.cpa.hybrid.value.HybridValue;
import org.sosy_lab.cpachecker.exceptions.NoException;

/**
 * This class provides a random strategy for generating actual values
 * for simple c types
 * 
 * Mind: no support of structs, arrays ...
 */
public class SimpleValueProvider extends HybridValueProvider{

    @Override
    public HybridValue visit(CArrayType pArrayType) throws NoException {
        return null;
    }

    @Override
    public HybridValue visit(CCompositeType pCompositeType) throws NoException {
        return null;
    }

    @Override
    public HybridValue visit(CElaboratedType pElaboratedType) throws NoException {
        return null;
    }

    @Override
    public HybridValue visit(CEnumType pEnumType) throws NoException {
        return null;
    }

    @Override
    public HybridValue visit(CFunctionType pFunctionType) throws NoException {
        return null;
    }

    @Override
    public HybridValue visit(CPointerType pPointerType) throws NoException {
        return null;
    }

    @Override
    public HybridValue visit(CProblemType pProblemType) throws NoException {
        return null;
    }

    @Override
    public HybridValue visit(CSimpleType pSimpleType) throws NoException {
        return null;
    }

    @Override
    public HybridValue visit(CTypedefType pTypedefType) throws NoException {
        return null;
    }

    @Override
    public HybridValue visit(CVoidType pVoidType) throws NoException {
        // probably nothing to do here
        return null;
    }

    @Override
    public HybridValue visit(CBitFieldType pCBitFieldType) throws NoException {
        return null;
    }

}