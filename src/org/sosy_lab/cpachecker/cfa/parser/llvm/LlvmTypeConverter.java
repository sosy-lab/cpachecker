/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.parser.llvm;

import java.util.ArrayList;
import java.util.List;
import org.bridj.IntValuedEnum;
import org.bridj.Pointer;
import org.llvm.TypeRef;
import org.llvm.binding.LLVMLibrary.LLVMTypeKind;
import org.llvm.binding.LLVMLibrary.LLVMTypeRef;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;

/**
 * Converts LLVM types to {@link CType CTypes}.
 */
public class LlvmTypeConverter {

  private final MachineModel machineModel;

  public LlvmTypeConverter(final MachineModel pMachineModel) {
    machineModel = pMachineModel;
  }

  public CType getCType(final TypeRef pLlvmType) {
    final boolean isConst = false;
    final boolean isVolatile = false;

    IntValuedEnum<LLVMTypeKind> typeKind = pLlvmType.getTypeKind();
    long tk = typeKind.value();

    if (tk == LLVMTypeKind.LLVMVoidTypeKind.value()) {
      return CVoidType.VOID;

    } else if (tk == LLVMTypeKind.LLVMFunctionTypeKind.value()) {
      return getFunctionType(pLlvmType);

    } else if (tk == LLVMTypeKind.LLVMIntegerTypeKind.value()) {

      int integerWidth = pLlvmType.getIntTypeWidth();
      return getIntegerType(integerWidth, isConst, isVolatile);

    } else if (tk == LLVMTypeKind.LLVMHalfTypeKind.value()
        || tk == LLVMTypeKind.LLVMFloatTypeKind.value()
        || tk == LLVMTypeKind.LLVMDoubleTypeKind.value()
        || tk == LLVMTypeKind.LLVMPPC_FP128TypeKind.value()
        || tk == LLVMTypeKind.LLVMFP128TypeKind.value()
        || tk == LLVMTypeKind.LLVMX86_FP80TypeKind.value()) {

      return getFloatType(typeKind);

    } else if (tk == LLVMTypeKind.LLVMArrayTypeKind.value()) {
      // TODO
    } else {
        throw new AssertionError("LLVM type of id " + tk + " can't be handled");
    }

    return null;
  }

  private CType getFunctionType(TypeRef pFuncType) {
    final boolean isConst = false;
    final boolean isVolatile = false;

    CType returnType = getCType(pFuncType.getReturnType());

    int paramNumber = pFuncType.countParamTypes();
    List<CType> parameterTypes = new ArrayList<>(paramNumber);

    Pointer<LLVMTypeRef> paramTypes = Pointer.allocateArray(LLVMTypeRef.class, paramNumber);
    pFuncType.getParamTypes(paramTypes);
    for (int i = 0; i < paramNumber; i++) {
      LLVMTypeRef internalType = paramTypes.get(i);
      TypeRef type = new TypeRef(internalType);
      CType cParamType = getCType(type);
      parameterTypes.add(cParamType);
    }

    boolean takesVarArgs = pFuncType.isFunctionVarArg(); // TODO: do we have to call this method on pFuncType directly?

    return new CFunctionType(isConst, isVolatile, returnType, parameterTypes, takesVarArgs);
  }

  private CType getIntegerType(
      final int pIntegerWidth,
      final boolean pIsConst,
      final boolean pIsVolatile
  ) {
    final boolean isSigned = false;
    final boolean isComplex = false;
    final boolean isImaginary = false;
    final boolean isUnsigned = true;

    final boolean isLong = false;
    boolean isShort = false;
    boolean isLonglong = false;

    CBasicType basicType;

    switch (pIntegerWidth) {
      case 1:
        basicType = CBasicType.BOOL;
        break;
      case 2:
        basicType = CBasicType.INT;
        isShort = true;
        break;
      case 4:
        basicType = CBasicType.INT;
        // keep everything set to 'false' for default int
        break;
      case 8:
        basicType = CBasicType.INT;
        // We use long long since it is 8 bytes for both 32 and 64 bit machines
        isLonglong = true;
        break;
      default:
        throw new AssertionError("Unhandled integer bitwidth " + pIntegerWidth);
    }

    return new CSimpleType(
        pIsConst,
        pIsVolatile,
        basicType,
        isLong,
        isShort,
        isSigned,
        isUnsigned,
        isComplex,
        isImaginary,
        isLonglong
    );
  }

  private CType getFloatType(final IntValuedEnum<LLVMTypeKind>  pType) {

    final long tk = pType.value();
    if (tk == LLVMTypeKind.LLVMHalfTypeKind.value()) {
      // FIXME: This is actually wrong, but at the time of this writing
      // we have no way of defining a float of 16bit width
      return getSimplestCType(CBasicType.FLOAT);

    } else if (tk == LLVMTypeKind.LLVMFloatTypeKind.value()) {
      return getSimplestCType(CBasicType.FLOAT);

    } else if (tk == LLVMTypeKind.LLVMDoubleTypeKind.value()) {
      if (machineModel.getSizeofDouble()*8 == 64) {
        return getSimplestCType(CBasicType.DOUBLE);
      } else if (machineModel.getSizeofLongDouble()*8 == 64) {
        return getSimplestCType(CBasicType.DOUBLE, true);

      } else {
        throw new AssertionError(
            "Machine model " + machineModel.name() + " can't handle 64bit float");
      }

    } else if (tk == LLVMTypeKind.LLVMFP128TypeKind.value()
        || tk == LLVMTypeKind.LLVMPPC_FP128TypeKind.value()) {
      if (machineModel.getSizeofLongDouble() * 8 != 128) {
        throw new AssertionError(
            "Machine model " + machineModel.name() + " can't handle 128bit float");

      } else {
        return getSimplestCType(CBasicType.DOUBLE, true);
      }

    } else if (tk == LLVMTypeKind.LLVMX86_FP80TypeKind.value()) {
      throw new AssertionError(
          "Machine model " + machineModel.name() + " can't handle 80bit float");
    } else {
      throw new AssertionError("Unhandled type with id " + tk);
    }
  }

  private CType getSimplestCType(final CBasicType pBasicType) {
    return getSimplestCType(pBasicType, false);
  }

  private CType getSimplestCType(final CBasicType pBasicType, boolean pIsLong) {
    final boolean isConst = false;
    final boolean isVolatile = false;
    final boolean isShort = false;
    final boolean isSigned = false;
    final boolean isUnsigned = false;
    final boolean isComplex = false;
    final boolean isImaginary = false;
    final boolean isLongLong = false;

    return new CSimpleType(
        isConst,
        isVolatile,
        pBasicType,
        pIsLong,
        isShort,
        isSigned,
        isUnsigned,
        isComplex,
        isImaginary,
        isLongLong);
  }

}
