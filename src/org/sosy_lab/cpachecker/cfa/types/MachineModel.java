/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.types;

import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType.CCompositeTypeMemberDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CElaboratedType;
import org.sosy_lab.cpachecker.cfa.types.c.CEnumType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CTypedefType;

/**
 * This enum stores the sizes for all the basic types that exist.
 */
public enum MachineModel {
  /**
   * Machine model representing a 32bit Linux machine
   */
  LINUX32(
      // numeric types
      2,  // short
      4,  // int
      4,  // long int
      8,  // long long int
      4,  // float
      8,  // double
      12, // long double

      // other
      1, // void
      1  // bool
      ),

  /**
   * Machine model representing a 64bit Linux machine
   */
  LINUX64(
      // numeric types
      2,  // short
      4,  // int
      8,  // long int
      8,  // long long int
      4,  // float
      8,  // double
      16, // long double

      // other
      1, // void
      1  // bool
      );

  // numeric types
  private final int     sizeofShort;
  private final int     sizeofInt;
  private final int     sizeofLongInt;
  private final int     sizeofLongLongInt;
  private final int     sizeofFloat;
  private final int     sizeofDouble;
  private final int     sizeofLongDouble;

  // other
  private final int     sizeofVoid;
  private final int     sizeofBool;

  // according to ANSI C, sizeof(char) is always 1
  private final int mSizeofChar = 1;

  private MachineModel(int pSizeofShort, int pSizeofInt, int pSizeofLongInt,
      int pSizeofLongLongInt, int pSizeofFloat, int pSizeofDouble,
      int pSizeofLongDouble, int pSizeofVoid, int pSizeofBool) {
    sizeofShort = pSizeofShort;
    sizeofInt = pSizeofInt;
    sizeofLongInt = pSizeofLongInt;
    sizeofLongLongInt = pSizeofLongLongInt;
    sizeofFloat = pSizeofFloat;
    sizeofDouble = pSizeofDouble;
    sizeofLongDouble = pSizeofLongDouble;
    sizeofVoid = pSizeofVoid;
    sizeofBool = pSizeofBool;
  }

  public int getSizeofShort() {
    return sizeofShort;
  }

  public int getSizeofInt() {
    return sizeofInt;
  }

  public int getSizeofLongInt() {
    return sizeofLongInt;
  }

  public int getSizeofLongLongInt() {
    return sizeofLongLongInt;
  }

  public int getSizeofFloat() {
    return sizeofFloat;
  }

  public int getSizeofDouble() {
    return sizeofDouble;
  }

  public int getSizeofLongDouble() {
    return sizeofLongDouble;
  }

  public int getSizeofVoid() {
    return sizeofVoid;
  }

  public int getSizeofBool() {
    return sizeofBool;
  }

  public int getSizeofChar() {
    return mSizeofChar;
  }

  public int getSizeof(CSimpleType type) {
    switch (type.getType()) {
    case VOID:        return getSizeofVoid();
    case BOOL:        return getSizeofBool();
    case CHAR:        return getSizeofChar();
    case FLOAT:       return getSizeofFloat();
    case UNSPECIFIED: // unspecified is the same as int
    case INT:
      if (type.isLongLong()) {
        return getSizeofLongLongInt();
      } else if (type.isLong()) {
        return getSizeofLongInt();
      } else if (type.isShort()) {
        return getSizeofShort();
      } else {
        return getSizeofInt();
      }
    case DOUBLE:
      if (type.isLong()) {
        return getSizeofLongDouble();
      } else {
        return getSizeofDouble();
      }
    default:
      throw new AssertionError("Unrecognized CBasicType " + type.getType());
    }
  }

  public int getSizeof(CType type){
    if (type instanceof CSimpleType){
      return getSizeof((CSimpleType)type);
    }

    if (type instanceof CCompositeType)
    {
      CCompositeType cType = (CCompositeType)type;
      int size = 0;
      for (CCompositeTypeMemberDeclaration decl : cType.getMembers()) {
        size += getSizeof(decl.getType());
      }
      return size;
    }

    if (type instanceof CEnumType){
      return getSizeofInt();
    }

    // TODO: This has to be checked
    if (type instanceof CFunctionPointerType){
      return getSizeofInt();
    }

    if (type instanceof CFunctionType){
      CFunctionType funcType = (CFunctionType)type;
      return getSizeof(funcType.getReturnType());
    }

    // TODO: This has to be checked (Example: Char*)
    if (type instanceof CPointerType){
      return getSizeofInt();
    }

    // TODO: This has to be checked
    if (type instanceof CElaboratedType){
      CElaboratedType eType = (CElaboratedType)type;
      return getSizeofInt();
    }

    // TODO: This has to be checked (Example: Char pathbuf[1 + 1];)
    if (type instanceof CArrayType){
      return getSizeofInt();
    }

    // TODO: This has to be checked (Example: (sizeof(*buff)))
    if (type instanceof CTypedefType){
      return getSizeofInt();
    }

    throw new IllegalArgumentException("Unknwon CType: " + type.getClass().toString());
  }
}