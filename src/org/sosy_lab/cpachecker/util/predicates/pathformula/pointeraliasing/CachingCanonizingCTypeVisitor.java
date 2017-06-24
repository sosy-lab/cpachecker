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
package org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.cfa.types.c.DefaultCTypeVisitor;

class CachingCanonizingCTypeVisitor extends DefaultCTypeVisitor<CType, RuntimeException> {

  private class CTypeTransformerVisitor implements CTypeVisitor<CType, RuntimeException> {

    private CTypeTransformerVisitor(final boolean ignoreConst, final boolean ignoreVolatile) {
      this.ignoreConst = ignoreConst;
      this.ignoreVolatile = ignoreVolatile;
    }

    @Override
    public CType visit(final CArrayType t) {
      final CType oldType = t.getType();
      final CType type = oldType.accept(CachingCanonizingCTypeVisitor.this);
      return type == oldType && (!t.isConst() || !ignoreConst) && (!t.isVolatile() || !ignoreVolatile) ? t :
        new CArrayType(!ignoreConst && t.isConst(),
                       !ignoreVolatile && t.isVolatile(),
                       type,
                       t.getLength());
    }

    @Override
    public CCompositeType visit(final CCompositeType t) {
      throw new AssertionError("should never be called");
    }

    @Override
    public CElaboratedType visit(final CElaboratedType t) {
      final CComplexType oldRealType = t.getRealType();
      final CComplexType realType = oldRealType != null ?
                                      (CComplexType) oldRealType.accept(CachingCanonizingCTypeVisitor.this) :
                                      null;

      return realType == oldRealType && (!ignoreConst || !t.isConst()) && (!ignoreVolatile || !t.isVolatile()) ? t :
             new CElaboratedType(!ignoreConst && t.isConst(),
                                 !ignoreVolatile && t.isVolatile(),
                                 t.getKind(),
                                 t.getName(),
                                 t.getOrigName(),
                                 realType);
    }

    @Override
    public CPointerType visit(final CPointerType t) {
      final CType oldType = t.getType();
      final CType type = oldType.accept(CachingCanonizingCTypeVisitor.this);

      return type == oldType && (!ignoreConst || !t.isConst()) && (!ignoreVolatile || !t.isVolatile()) ? t :
             new CPointerType(!ignoreConst && t.isConst(),
                              !ignoreVolatile && t.isVolatile(),
                              type);
    }

    @Override
    public CTypedefType visit(final CTypedefType t) {
      final CType oldRealType = t.getRealType();
      final CType realType = oldRealType.accept(CachingCanonizingCTypeVisitor.this);

      return realType == oldRealType && (!ignoreConst || !t.isConst()) && (!ignoreVolatile || !t.isVolatile()) ? t :
             new CTypedefType(!ignoreConst && t.isConst(), !ignoreConst && t.isVolatile(), t.getName(), realType);
    }

    @Override
    public CFunctionType visit(final CFunctionType t) {
      final CType oldReturnType = t.getReturnType();
      final CType returnType = oldReturnType.accept(CachingCanonizingCTypeVisitor.this);

      List<CType> parameterTypes = null;
      int i = 0;
      for (CType oldType : t.getParameters()) {
        final CType type = oldType.accept(CachingCanonizingCTypeVisitor.this);
        if (type != oldType && parameterTypes == null) {
          parameterTypes = new ArrayList<>();
          parameterTypes.addAll(t.getParameters().subList(0, i));
        }
        if (parameterTypes != null) {
          parameterTypes.add(type);
        }
        ++i;
      }


      final CFunctionType result;
      if (returnType == oldReturnType &&
          parameterTypes == null &&
          (!ignoreConst || !t.isConst()) &&
          (!ignoreVolatile || !t.isVolatile())) {
        result = t;
      } else {
        result = new CFunctionType(!ignoreConst && t.isConst(),
                                   !ignoreVolatile && t.isVolatile(),
                                   returnType,
                                   parameterTypes != null ? parameterTypes : t.getParameters(),
                                   t.takesVarArgs());
        if (t.getName() != null) {
          result.setName(t.getName());
        }
      }

      return result;
    }

    @Override
    public CEnumType visit(final CEnumType t) {
      return (!ignoreConst || !t.isConst()) && (!ignoreVolatile || !t.isVolatile())
          ? t
          : new CEnumType(
              !ignoreConst && t.isConst(),
              !ignoreVolatile && t.isVolatile(),
              t.getEnumerators(),
              t.getName(),
              t.getOrigName());
    }

    @Override
    public CProblemType visit(final CProblemType t) {
      return t;
    }

    @Override
    public CSimpleType visit(final CSimpleType t) {
      return (!ignoreConst || !t.isConst()) && (!ignoreVolatile || !t.isVolatile())
          ? t
          : new CSimpleType(
              !ignoreConst && t.isConst(),
              !ignoreVolatile && t.isVolatile(),
              t.getType(),
              t.isLong(),
              t.isShort(),
              t.isSigned(),
              t.isUnsigned(),
              t.isComplex(),
              t.isImaginary(),
              t.isLongLong());
    }

    @Override
    public CType visit(CBitFieldType pCBitFieldType) throws RuntimeException {
      CType type = pCBitFieldType.getType().accept(this);
      if (type != pCBitFieldType.getType()) {
        return new CBitFieldType(type, pCBitFieldType.getBitFieldSize());
      }
      return pCBitFieldType;
    }

    @Override
    public CType visit(CVoidType t) {
      return CVoidType.create(!ignoreConst && t.isConst(),
                              !ignoreVolatile && t.isVolatile());
    }

    private final boolean ignoreConst;
    private final boolean ignoreVolatile;
  }

  CachingCanonizingCTypeVisitor(final boolean ignoreConst, final boolean ignoreVolatile) {
    typeVisitor = new CTypeTransformerVisitor(ignoreConst, ignoreVolatile);
  }

  @Override
  public CCompositeType visit(final CCompositeType t) {
    final CCompositeType result = (CCompositeType) typeCache.get(t);
    if (result != null) {
      return result;
    } else {
      CCompositeType canonicalType = t.getCanonicalType();
      List<CCompositeTypeMemberDeclaration> oldMembers = canonicalType.getMembers();

      // Need to create our own instance because we will modify it to prevent recursion.
      canonicalType =
          new CCompositeType(
              !typeVisitor.ignoreConst && canonicalType.isConst(),
              !typeVisitor.ignoreVolatile && canonicalType.isVolatile(),
              canonicalType.getKind(),
              canonicalType.getName(),
              canonicalType.getOrigName());
      typeCache.put(t, canonicalType);

      List<CCompositeTypeMemberDeclaration> memberDeclarations = new ArrayList<>(oldMembers.size());
      for (CCompositeTypeMemberDeclaration oldMemberDeclaration : oldMembers) {
        final CType oldMemberType = oldMemberDeclaration.getType();
        final CType memberType = oldMemberType.accept(this);
        if (memberType != oldMemberType) {
          memberDeclarations.add(
              new CCompositeTypeMemberDeclaration(memberType, oldMemberDeclaration.getName()));
        } else {
          memberDeclarations.add(oldMemberDeclaration);
        }
      }

      // Here CCompositeType mutability is used to prevent infinite recursion
      canonicalType.setMembers(memberDeclarations);
      return canonicalType;
    }
  }

  @Override
  public CType visitDefault(final CType t) {
    CType result = typeCache.get(t);
    if (result != null) {
      return result;
    } else {
      result = t.getCanonicalType();
      if (!(result instanceof CCompositeType)) {
        result = result.accept(typeVisitor);
      } else {
        result = visit((CCompositeType) result);
      }
      typeCache.put(t, result);
      return result;
    }
  }

  private final Map<CType, CType> typeCache = new HashMap<>();
  private final CTypeTransformerVisitor typeVisitor;
}
