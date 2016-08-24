/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util;

import static com.google.common.base.Preconditions.checkArgument;

import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.Type;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.java.JBasicType;
import org.sosy_lab.cpachecker.cfa.types.java.JSimpleType;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 * Class providing util methods for the family of {@link Type} classes.
 */
public class Types {

  /**
   * Returns whether the first given type can hold all values the second given typ can hold.
   * The types have to belong to the same language, for example {@link CType}.
   *
   * <p>If only Java types have to be checked, no {@link MachineModel} is necessary.
   * Use {@link #canHoldAllValues(JType, JType)} instead.</p>
   *
   * @param pHoldingType the first type
   * @param pInnerType the second type
   * @param pMachineModel the machine model types are based on
   *
   * @return <code>true</code> if the first given type can hold all values the second given type
   *    can hold
   */
  public static boolean canHoldAllValues(Type pHoldingType, Type pInnerType, MachineModel pMachineModel) {

    if (pHoldingType instanceof CType) {
      checkArgument(pInnerType instanceof CType);
      CSimpleType toType;
      CSimpleType fromType;

      if (pHoldingType instanceof CPointerType) {
        toType = pMachineModel.getPointerEquivalentSimpleType();
      } else if (pHoldingType instanceof CSimpleType) {
        toType = (CSimpleType) pHoldingType;
      } else {
        return pHoldingType.equals(pInnerType);
      }

      if (pInnerType instanceof CPointerType) {
        fromType = pMachineModel.getPointerEquivalentSimpleType();
      } else if (pInnerType instanceof CSimpleType) {
        fromType = (CSimpleType) pInnerType;
      } else {
        return pHoldingType.equals(pInnerType);
      }

      return canHoldAllValues(toType, fromType, pMachineModel);

    } else {
      assert pHoldingType instanceof JType && pInnerType instanceof JType;
      return canHoldAllValues((JType) pHoldingType, (JType) pInnerType);
    }
  }

  private static boolean canHoldAllValues(CSimpleType pHoldingType, CSimpleType pInnerType, MachineModel pMachineModel) {
    final boolean isHoldingTypeSigned = pMachineModel.isSigned(pHoldingType);
    final boolean isInnerTypeSigned = pMachineModel.isSigned(pInnerType);
    final boolean isHoldingTypeFloat = pHoldingType.getType().isFloatingPointType();
    final boolean isInnerTypeFloat = pInnerType.getType().isFloatingPointType();

    if (isInnerTypeSigned && !isHoldingTypeSigned) {
      return false;
    } else if (isInnerTypeFloat && !isHoldingTypeFloat) {
      return false;
    }

    int holdingBitSize = pMachineModel.getSizeof(pHoldingType) * pMachineModel.getSizeofCharInBits();
    int innerBitSize = pMachineModel.getSizeof(pInnerType) * pMachineModel.getSizeofCharInBits();

    if (innerBitSize > holdingBitSize) {
      return false;
    }

    // if inner type is not signed, but holding type is, add a relevant bit to the inner type
    // (adding one to both/removing the sign bit for both does not change anything, so we only
    // change the bits in this case)
    if (isHoldingTypeSigned && !isInnerTypeSigned) {
      innerBitSize++;
    }

    if (isHoldingTypeFloat && !isInnerTypeFloat) {
      holdingBitSize = holdingBitSize / 2;
    }

    return holdingBitSize >= innerBitSize;
  }

  /**
   * Returns whether the first given type can hold all values the second given typ can hold.
   *
   * @param pHoldingType the first type
   * @param pInnerType the second type
   *
   * @return <code>true</code> if the first given type can hold all values the second given type
   *    can hold
   */
  public static boolean canHoldAllValues(JType pHoldingType, JType pInnerType) {
    if (pHoldingType instanceof JSimpleType) {
      checkArgument(pInnerType instanceof JSimpleType);

      return canHoldAllValues((JSimpleType) pHoldingType, (JSimpleType) pInnerType);

    } else {
      return pHoldingType.equals(pInnerType);
    }
  }

  private static boolean canHoldAllValues(JSimpleType pHoldingType, JSimpleType pInnerType) {
    JBasicType holdingType = pHoldingType.getType();
    JBasicType innerType = pInnerType.getType();
    boolean canHold = false;


    switch (innerType) {
      case BOOLEAN:
        return holdingType == JBasicType.BOOLEAN;

      case CHAR:
        return holdingType == JBasicType.CHAR;

      case BYTE:
        canHold |= holdingType == JBasicType.BYTE;
        // $FALL-THROUGH$
      case SHORT:
        canHold |= holdingType == JBasicType.SHORT;
        canHold |= holdingType == JBasicType.FLOAT;
        // $FALL-THROUGH$
      case INT:
        canHold |= holdingType == JBasicType.INT;
        canHold |= holdingType == JBasicType.DOUBLE;
        // $FALL-THROUGH$
      case LONG:
        canHold |= holdingType == JBasicType.LONG;
        break;

      case FLOAT:
        canHold |= holdingType == JBasicType.FLOAT;
        // $FALL-THROUGH$
      case DOUBLE:
        canHold |= holdingType == JBasicType.DOUBLE;
        break;
      default:
        throw new AssertionError("Unhandled type " + pInnerType.getType());
    }

    return canHold;
  }


}
