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

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
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
import org.sosy_lab.cpachecker.cfa.types.c.DefaultCTypeVisitor;

public class CachingCTypeTransformer extends DefaultCTypeVisitor<CType, RuntimeException> {

  private class CTypeTransformer implements CTypeVisitor<CType, RuntimeException> {

    public CTypeTransformer(final MachineModel machineModel,
                            final boolean transformUnsizedArrays,
                            final boolean ignoreConst,
                            final boolean ignoreVolatile) {
      this.machineModel = machineModel;
      this.transformUnsizedArrays = transformUnsizedArrays;
      this.ignoreConst = ignoreConst;
      this.ignoreVolatile = ignoreVolatile;
    }

    @Override
    public CType visit(final CArrayType t) {
      final CType oldType = t.getType();
      if (transformUnsizedArrays && t.getLength() == null) {
        if (initializerSize != null) {
          assert fileLocation != null : "Unexpected null file location";
          final CIntegerLiteralExpression length =
            new CIntegerLiteralExpression(fileLocation,
                                          machineModel.getPointerEquivalentSimpleType(),
                                          BigInteger.valueOf(initializerSize));
          return new CArrayType(!ignoreConst && t.isConst(),
                                !ignoreVolatile && t.isVolatile(),
                                oldType.accept(CachingCTypeTransformer.this),
                                length);
        } else {
          return new CPointerType(!ignoreConst && t.isConst(),
                                  !ignoreVolatile && t.isVolatile(),
                                  oldType.accept(CachingCTypeTransformer.this));
        }
      } else {
        final CType type = oldType.accept(CachingCTypeTransformer.this);
        return type == oldType && (!t.isConst() || !ignoreConst) && (!t.isVolatile() || !ignoreVolatile) ? t :
          new CArrayType(!ignoreConst && t.isConst(),
                         !ignoreVolatile && t.isVolatile(),
                         type,
                         t.getLength());
      }
    }

    @Override
    public CCompositeType visit(final CCompositeType t) {
      List<CCompositeTypeMemberDeclaration> memberDeclarations = null;
      int i = 0;
      for (CCompositeTypeMemberDeclaration oldMemberDeclaration : t.getMembers()) {
        final CType oldMemberType = oldMemberDeclaration.getType();
        final CType memberType = oldMemberType.accept(CachingCTypeTransformer.this);
        if (memberType != oldMemberType && memberDeclarations == null) {
          memberDeclarations = new ArrayList<>();
          memberDeclarations.addAll(t.getMembers().subList(0, i));
        }
        if (memberDeclarations != null) {
          if (memberType != oldMemberType) {
            memberDeclarations.add(new CCompositeTypeMemberDeclaration(memberType, oldMemberDeclaration.getName()));
          } else {
            memberDeclarations.add(oldMemberDeclaration);
          }
        }
        ++i;
      }

      if (memberDeclarations != null) { // Here CCompositeType mutability is used to prevent infinite recursion
        t.setMembers(memberDeclarations);
      }
      return t;
    }

    @Override
    public CElaboratedType visit(final CElaboratedType t) {
      final CComplexType oldRealType = t.getRealType();
      final CComplexType realType = oldRealType != null ?
                                      (CComplexType) oldRealType.accept(CachingCTypeTransformer.this) :
                                      null;

      return realType == oldRealType && (!ignoreConst || !t.isConst()) && (!ignoreVolatile || !t.isVolatile()) ? t :
             new CElaboratedType(!ignoreConst && t.isConst(),
                                 !ignoreVolatile && t.isVolatile(),
                                 t.getKind(),
                                 t.getName(),
                                 realType);
    }

    @Override
    public CPointerType visit(final CPointerType t) {
      final CType oldType = t.getType();
      final CType type = oldType.accept(CachingCTypeTransformer.this);

      return type == oldType && (!ignoreConst || !t.isConst()) && (!ignoreVolatile || !t.isVolatile()) ? t :
             new CPointerType(!ignoreConst && t.isConst(),
                              !ignoreVolatile && t.isVolatile(),
                              type);
    }

    @Override
    public CTypedefType visit(final CTypedefType t) {
      final CType oldRealType = t.getRealType();
      final CType realType = oldRealType.accept(CachingCTypeTransformer.this);

      return realType == oldRealType && (!ignoreConst || !t.isConst()) && (!ignoreVolatile || !t.isVolatile()) ? t :
             new CTypedefType(!ignoreConst && t.isConst(), !ignoreConst && t.isVolatile(), t.getName(), realType);
    }

    @Override
    public CFunctionType visit(final CFunctionType t) {
      final CType oldReturnType = t.getReturnType();
      final CType returnType = oldReturnType.accept(CachingCTypeTransformer.this);

      List<CType> parameterTypes = null;
      int i = 0;
      for (CType oldType : t.getParameters()) {
        final CType type = oldType.accept(CachingCTypeTransformer.this);
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
        result.setName(t.getName());
      }

      return result;
    }

    @Override
    public CEnumType visit(final CEnumType t) {
      return (!ignoreConst || !t.isConst()) && (!ignoreVolatile || !t.isVolatile()) ? t :
             new CEnumType(!ignoreConst && t.isConst(),
                           !ignoreVolatile && t.isVolatile(),
                           t.getEnumerators(),
                           t.getName());
    }

    @Override
    public CProblemType visit(final CProblemType t) {
      return t;
    }

    @Override
    public CSimpleType visit(final CSimpleType t) {
      return (!ignoreConst || !t.isConst()) && (!ignoreVolatile || !t.isVolatile()) ? t :
              new CSimpleType(!ignoreConst && t.isConst(),
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

    public void setInitializerSize(final int initializerSize, @Nonnull final FileLocation fileLocation) {
      this.initializerSize = initializerSize;
      this.fileLocation = fileLocation;
    }

    public void unsetInitializerSize() {
      this.initializerSize = null;
      this.fileLocation = null;
    }

    private final boolean transformUnsizedArrays;
    private final boolean ignoreConst;
    private final boolean ignoreVolatile;
    private final MachineModel machineModel;
    private Integer initializerSize;
    private FileLocation fileLocation;
  }

  public CachingCTypeTransformer(final MachineModel machineModel,
                                 final boolean transformUnsizedArrays,
                                 final boolean ignoreConst,
                                 final boolean ignoreVolatile) {
    typeVisitor = new CTypeTransformer(machineModel,
                                       transformUnsizedArrays,
                                       ignoreConst,
                                       ignoreVolatile);
  }

  @Override
  public CCompositeType visit(final CCompositeType t) {
    final CCompositeType result = (CCompositeType) typeCache.get(t);
    if (result != null) {
      return result;
    } else {
      // This prevents infinite recursion
      if ((!typeVisitor.ignoreConst || !t.isConst()) && (!typeVisitor.ignoreVolatile || !t.isVolatile())) {
        typeCache.put(t, t);
      } else {
        typeCache.put(t, new CCompositeType(!typeVisitor.ignoreConst && !t.isConst(),
                                            !typeVisitor.ignoreVolatile && !t.isVolatile(),
                                            t.getKind(),
                                            t.getMembers(),
                                            t.getName()));
      }
      typeVisitor.unsetInitializerSize();
      return typeVisitor.visit(t);
    }
  }

  @Override
  public CType visitDefault(final CType t) {
    CType result;
    if (!(t instanceof CArrayType) ||
        ((CArrayType) t).getLength() != null) {
      result = typeCache.get(t);
    } else {
      result = null;
    }
    if (result != null) {
      return result;
    } else {
      if (!(t instanceof CArrayType)) {
        typeVisitor.unsetInitializerSize();
      }
      result = t.accept(typeVisitor);
      typeCache.put(t, result);
      return result;
    }
  }

  public void setInitializerSize(final int size, final @Nonnull FileLocation fileLocation) {
    typeVisitor.setInitializerSize(size, fileLocation);
  }

  private final Map<CType, CType> typeCache = new HashMap<>();
  private final CTypeTransformer typeVisitor;
}
