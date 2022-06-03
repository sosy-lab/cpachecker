// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.llvm;

import static com.google.common.base.Preconditions.checkState;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CBitFieldType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType;
import org.sosy_lab.cpachecker.cfa.types.c.CComplexType.ComplexTypeKind;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.llvm_j.TypeRef;
import org.sosy_lab.llvm_j.TypeRef.TypeKind;

/** Converts LLVM types to {@link CType CTypes}. */
public class LlvmTypeConverter {

  private static final String PREFIX_LITERAL_STRUCT = "lit_struc_";
  private static final String PREFIX_STRUCT_MEMBER = "elem_";
  private static final CSimpleType ARRAY_LENGTH_TYPE = CNumericTypes.LONG_LONG_INT;

  private static int structCount = 0;

  private final MachineModel machineModel;
  private final LogManager logger;

  private final Map<Integer, CType> typeCache = new HashMap<>();

  public LlvmTypeConverter(final MachineModel pMachineModel, final LogManager pLogger) {
    machineModel = pMachineModel;
    logger = pLogger;
  }

  public @Nullable CType getCType(final TypeRef pLlvmType) {
    return getCType(pLlvmType, /* isUnsigned = */ false);
  }

  public @Nullable CType getCType(final TypeRef pLlvmType, final boolean isUnsigned) {
    final boolean isConst = false;
    final boolean isVolatile = false;
    TypeKind typeKind = pLlvmType.getTypeKind();
    switch (typeKind) {
      case Void:
        return CVoidType.VOID;

      case Half:
      case Float:
      case Double:
      case X86_FP80:
      case FP128:
      case PPC_FP128:
        return getFloatType(typeKind, isUnsigned);
      case Integer:
        int integerWidth = pLlvmType.getIntTypeWidth();
        return getIntegerType(integerWidth, isUnsigned);

      case Function:
        return getFunctionType(pLlvmType);

      case Struct:
        return createStructType(pLlvmType);

      case Array:
        CIntegerLiteralExpression arrayLength =
            new CIntegerLiteralExpression(
                FileLocation.DUMMY,
                ARRAY_LENGTH_TYPE,
                BigInteger.valueOf(pLlvmType.getArrayLength()));

        return new CArrayType(
            isConst, isVolatile, getCType(pLlvmType.getElementType(), isUnsigned), arrayLength);

      case Pointer:
        if (pLlvmType.getPointerAddressSpace() != 0) {
          logger.log(Level.WARNING, "Pointer address space not considered.");
        }
        return new CPointerType(
            isConst, isVolatile, getCType(pLlvmType.getElementType(), isUnsigned));

      case Vector:
        CIntegerLiteralExpression vectorLength =
            new CIntegerLiteralExpression(
                FileLocation.DUMMY,
                ARRAY_LENGTH_TYPE,
                BigInteger.valueOf(pLlvmType.getVectorSize()));

        return new CArrayType(
            isConst, isVolatile, getCType(pLlvmType.getElementType(), isUnsigned), vectorLength);
      case Label:
      case Metadata:
      case X86_MMX:
      case Token:
        logger.log(Level.FINE, "Ignoring type kind", typeKind);
        return null;

      default:
        throw new AssertionError("Unhandled type kind " + typeKind);
    }
  }

  private CType createStructType(final TypeRef pStructType) {
    final boolean isConst = false;
    final boolean isVolatile = false;

    if (pStructType.isOpaqueStruct()) {
      logger.log(Level.INFO, "Ignoring opaque struct");
    }

    String structName = getStructName(pStructType);
    String origName = structName;

    if (typeCache.containsKey(pStructType.hashCode())) {
      return new CElaboratedType(
          false,
          false,
          ComplexTypeKind.STRUCT,
          structName,
          origName,
          (CComplexType) typeCache.get(pStructType.hashCode()));
    }

    CCompositeType cStructType =
        new CCompositeType(isConst, isVolatile, ComplexTypeKind.STRUCT, structName, origName);
    typeCache.put(pStructType.hashCode(), cStructType);

    List<TypeRef> memberTypes = pStructType.getStructElementTypes();
    List<CCompositeTypeMemberDeclaration> members = new ArrayList<>(memberTypes.size());

    for (int i = 0; i < memberTypes.size(); i++) {
      String memberName = getMemberName(i);
      TypeRef memType = memberTypes.get(i);
      CType cMemType = getCType(memType);
      CCompositeTypeMemberDeclaration memDecl =
          new CCompositeTypeMemberDeclaration(cMemType, memberName);
      members.add(memDecl);
    }

    cStructType.setMembers(members);
    return cStructType;
  }

  private String getStructName(TypeRef pStructType) {
    if (pStructType.isStructNamed()) {
      /* . is not a valid character for a name in C (but in LLVM yes),
       * so replace it by _ */
      return pStructType.getStructName().replace(".", "_");

    } else {
      return getLiteralStructName();
    }
  }

  private String getLiteralStructName() {
    structCount++;
    return PREFIX_LITERAL_STRUCT + structCount;
  }

  private String getMemberName(int pI) {
    return PREFIX_STRUCT_MEMBER + pI;
  }

  private CType getFunctionType(TypeRef pFuncType) {
    CType returnType = getCType(pFuncType.getReturnType());

    int paramNumber = pFuncType.countParamTypes();
    List<CType> parameterTypes = new ArrayList<>(paramNumber);

    List<TypeRef> paramTypes = pFuncType.getParamTypes();
    for (TypeRef type : paramTypes) {
      CType cParamType = getCType(type);
      parameterTypes.add(cParamType);
    }

    boolean takesVarArgs =
        pFuncType.isFunctionVarArg(); // TODO: do we have to call this method on pFuncType directly?

    return new CFunctionType(returnType, parameterTypes, takesVarArgs);
  }

  private CType getIntegerType(final int pIntegerWidth, final boolean isUnsigned) {
    final int sizeOfChar = machineModel.getSizeofCharInBits();
    if (machineModel.getSizeofInt() * sizeOfChar == pIntegerWidth) {
      if (isUnsigned) {
        return CNumericTypes.UNSIGNED_INT;
      } else {
        return CNumericTypes.SIGNED_INT;
      }

    } else if (machineModel.getSizeofLongInt() * sizeOfChar == pIntegerWidth) {
      if (isUnsigned) {
        return CNumericTypes.UNSIGNED_LONG_INT;
      } else {
        return CNumericTypes.SIGNED_LONG_INT;
      }

    } else if (machineModel.getSizeofLongLongInt() * sizeOfChar == pIntegerWidth) {
      if (isUnsigned) {
        return CNumericTypes.UNSIGNED_LONG_LONG_INT;
      } else {
        return CNumericTypes.SIGNED_LONG_LONG_INT;
      }

    } else {
      return new CBitFieldType(
          isUnsigned ? CNumericTypes.UNSIGNED_INT : CNumericTypes.SIGNED_INT, pIntegerWidth);
    }
  }

  private CType getFloatType(final TypeKind pType, final boolean isUnsigned) {

    switch (pType) {
      case Half:
        // FIXME: This is actually wrong, but at the time of this writing
        // we have no way of defining a float of 16bit width
        return getSimplestCType(CBasicType.FLOAT, isUnsigned);

      case Float:
        return getSimplestCType(CBasicType.FLOAT, isUnsigned);

      case Double:
        if (machineModel.getSizeofDouble() * 8 == 64) {
          return getSimplestCType(CBasicType.DOUBLE, isUnsigned);
        } else if (machineModel.getSizeofLongDouble() * 8 == 64) {
          return getSimplestCType(CBasicType.DOUBLE, isUnsigned, /* pIsLong = */ true);

        } else {
          throw new AssertionError(
              "Machine model " + machineModel.name() + " can't handle 64bit float");
        }

      case FP128:
      case PPC_FP128:
        checkState(machineModel.getSizeofFloat128() * 8 == 128);
        return getSimplestCType(CBasicType.FLOAT128, isUnsigned);

      case X86_FP80:
        throw new AssertionError(
            "Machine model " + machineModel.name() + " can't handle 80bit float");
      default:
        throw new AssertionError("Unhandled float type " + pType);
    }
  }

  private CType getSimplestCType(final CBasicType pBasicType, final boolean isUnsigned) {
    return getSimplestCType(pBasicType, isUnsigned, /* pIsLong = */ false);
  }

  private CType getSimplestCType(
      final CBasicType pBasicType, final boolean isUnsigned, boolean pIsLong) {
    final boolean isConst = false;
    final boolean isVolatile = false;
    final boolean isShort = false;
    final boolean isSigned = false;
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
