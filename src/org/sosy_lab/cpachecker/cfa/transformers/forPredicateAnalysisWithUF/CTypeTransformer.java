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
import java.util.List;

import javax.annotation.Nonnull;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypeVisitor;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;
import org.sosy_lab.cpachecker.cfa.types.c.DefaultCTypeVisitor;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCCodeException;

@Deprecated // This is because caching is mandatory to prevent infinite recursion, so use CachingCtypeTransformer
public class CTypeTransformer extends DefaultCTypeVisitor<CType, UnrecognizedCCodeException>
  implements CTypeVisitor<CType, UnrecognizedCCodeException> {

  public CTypeTransformer(final MachineModel machineModel, final boolean transformUnsizedArrays) {
    this.machineModel = machineModel;
    this.transformUnsizedArrays = transformUnsizedArrays;
  }

  @Override
  public CType visit(final CArrayType t) {
    if (transformUnsizedArrays && t.getLength() == null) {
      if (initializerSize != null) {
        assert fileLocation != null : "Unexpected null file location";
        final CIntegerLiteralExpression length =
          new CIntegerLiteralExpression(fileLocation,
                                        machineModel.getPointerEquivalentSimpleType(),
                                        BigInteger.valueOf(initializerSize));
        initializerSize = null;
        fileLocation = null;
        return new CArrayType(t.isConst(), t.isVolatile(), t.getType(), length);
      } else {
        return  new CPointerType(false, false, t.getType());
      }
    } else {
      return t;
    }
  }

  @Override
  public CType visit(final CCompositeType t) throws UnrecognizedCCodeException {
    List<CCompositeTypeMemberDeclaration> memberDeclarations = null;
    int i = 0;
    for (CCompositeTypeMemberDeclaration oldMemberDeclaration : t.getMembers()) {
      final CType oldMemberType = oldMemberDeclaration.getType();
      final CType memberType = oldMemberType.accept(this);
      if (memberType != oldMemberType && memberDeclarations == null) {
        memberDeclarations = new ArrayList<>();
        memberDeclarations.addAll(t.getMembers().subList(0, i));
      }
      if (memberDeclarations != null && oldMemberType != memberType) {
        memberDeclarations.add(new CCompositeTypeMemberDeclaration(memberType, oldMemberDeclaration.getName()));
      } else if (memberDeclarations != null) {
        memberDeclarations.add(oldMemberDeclaration);
      }
      ++i;
    }

    if (memberDeclarations != null) { // Here CCompositeType mutability is used to prevent infinite recursion
      t.setMembers(memberDeclarations);
    }
    return t;
  }

  @Override
  public CType visit(final CElaboratedType t) throws UnrecognizedCCodeException {
    final CComplexType oldRealType = t.getRealType();
    final CComplexType realType = oldRealType != null ? (CComplexType) oldRealType.accept(this) : null;

    return realType == oldRealType ? t :
           new CElaboratedType(t.isConst(), t.isVolatile(), t.getKind(), t.getName(), realType);
  }

  @Override
  public CType visit(final CPointerType t) throws UnrecognizedCCodeException {
    final CType oldType = t.getType();
    final CType type = oldType.accept(this);

    return type == oldType ? t :
           new CPointerType(t.isConst(), t.isVolatile(), type);
  }

  @Override
  public CType visit(final CTypedefType t) throws UnrecognizedCCodeException {
    final CType oldRealType = t.getRealType();
    final CType realType = oldRealType.accept(this);

    return realType == oldRealType ? t :
           new CTypedefType(t.isConst(), t.isVolatile(), t.getName(), realType);
  }

  @Override
  public CType visit(final CFunctionType t) throws UnrecognizedCCodeException {
    final CType oldReturnType = t.getReturnType();
    final CType returnType = oldReturnType.accept(this);

    List<CType> parameterTypes = null;
    int i = 0;
    for (CType oldType : t.getParameters()) {
      final CType type = oldType.accept(this);
      if (type != oldType && parameterTypes == null) {
        parameterTypes = new ArrayList<>();
        parameterTypes.addAll(t.getParameters().subList(0, i));
      }
      if (parameterTypes != null) {
        parameterTypes.add(type);
      }
      ++i;
    }


    final CFunctionType result = returnType == oldReturnType && parameterTypes == null ? t :
                                   new CFunctionType(t.isConst(),
                                                     t.isVolatile(),
                                                     returnType,
                                                     parameterTypes != null ? parameterTypes : t.getParameters(),
                                                     t.takesVarArgs());
    if ((returnType != oldReturnType || parameterTypes != null) && t.getName() != null) {
      result.setName(t.getName());
    }
    return result;
  }

  @Override
  public CType visitDefault(final CType t) {
    return t;
  }

  public void setInitializerSize(final int initializerSize, @Nonnull final FileLocation fileLocation) {
    this.initializerSize = initializerSize;
    this.fileLocation = fileLocation;
  }

  private final boolean transformUnsizedArrays;
  private final MachineModel machineModel;
  private Integer initializerSize;
  private FileLocation fileLocation;
}
