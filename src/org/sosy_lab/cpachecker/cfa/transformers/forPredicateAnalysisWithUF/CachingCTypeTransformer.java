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
package org.sosy_lab.cpachecker.cfa.transformers.forPredicateAnalysisWithUF;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

@SuppressWarnings("deprecation") // Deprecation is the superclass means this class should be used instead
public class CachingCTypeTransformer extends CTypeTransformer {

   public CachingCTypeTransformer(final MachineModel machineModel, final boolean transformUnsizedArrays) {
     super(machineModel, transformUnsizedArrays);
   }

   @Override
   public CType visit(final CArrayType t) {
     final CType cachedType = typeCache.get(t);
     if (cachedType != null) {
       return cachedType;
     } else {
       final CType result = super.visit(t);
       typeCache.put(t, result);
       return result;
     }
   }

  @Override
  public CType visit(final CCompositeType t) throws UnrecognizedCCodeException {
    final CType cachedType = typeCache.get(t);
    if (cachedType != null) {
      return cachedType;
    } else {
      typeCache.put(t, t); // This prevents infinite recursion
      return super.visit(t);
    }
  }

  @Override
  public CType visit(final CElaboratedType t) throws UnrecognizedCCodeException {
    final CType cachedType = typeCache.get(t);
    if (cachedType != null) {
      return cachedType;
    } else {
      final CType result = super.visit(t);
      typeCache.put(t, result);
      return result;
    }
  }

  @Override
  public CType visit(final CFunctionType t) throws UnrecognizedCCodeException {
    final CType cachedType = typeCache.get(t);
    if (cachedType != null) {
      return cachedType;
    } else {
      final CType result = super.visit(t);
      typeCache.put(t, result);
      return result;
    }
  }

  @Override
  public CType visit(final CPointerType t) throws UnrecognizedCCodeException {
    final CType cachedType = typeCache.get(t);
    if (cachedType != null) {
      return cachedType;
    } else {
      final CType result = super.visit(t);
      typeCache.put(t, result);
      return result;
    }
  }

  @Override
  public CType visit(final CTypedefType t) throws UnrecognizedCCodeException {
    final CType cachedType = typeCache.get(t);
    if (cachedType != null) {
      return cachedType;
    } else {
      final CType result = super.visit(t);
      typeCache.put(t, result);
      return result;
    }
  }

  @Override
  public CType visitDefault(final CType t) {
    final CType cachedType = typeCache.get(t);
    if (cachedType != null) {
      return cachedType;
    } else {
      final CType result = super.visitDefault(t);
      typeCache.put(t, result);
      return result;
    }
  }

  @Override
  public void setInitializerSize(final int size, final @Nonnull FileLocation fileLocation) {
    super.setInitializerSize(size, fileLocation);
  }

  private final Map<CType, CType> typeCache = new HashMap<>();
}
