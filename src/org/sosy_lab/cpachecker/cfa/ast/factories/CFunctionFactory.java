// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.factories;

import java.util.ArrayList;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public class CFunctionFactory {

  public String getNondetFunctionName(CType pType) {
    // TODO Finish for other types
    String nondetFunctionName = null;
    if (pType instanceof CSimpleType) {
      switch (((CSimpleType) pType).getType()) {
        case BOOL:
          break;
        case CHAR:
          break;
        case DOUBLE:
          break;
        case FLOAT:
          break;
        case FLOAT128:
          break;
        case INT:
        case INT128:
          if (((CSimpleType) pType).isLong()) {
            if (((CSimpleType) pType).isUnsigned()) {
              nondetFunctionName = "__VERIFIER_nondet_ulong";
            } else {
              nondetFunctionName = "__VERIFIER_nondet_long";
            }
          } else if (((CSimpleType) pType).isLongLong()) {
            if (((CSimpleType) pType).isUnsigned()) {
              nondetFunctionName = "__VERIFIER_nondet_ulonglong";
            } else {
              nondetFunctionName = "__VERIFIER_nondet_longlong";
            }
          } else {
            if (((CSimpleType) pType).isUnsigned()) {
              nondetFunctionName = "__VERIFIER_nondet_uint";
            } else {
              nondetFunctionName = "__VERIFIER_nondet_longlong";
            }
          }
          break;
        case UNSPECIFIED:
          break;
        default:
          break;

      }
    }
    return nondetFunctionName;
  }

  public CFunctionCallExpression callNondetFunction(CType pType) {
    CFunctionDeclaration functionDeclaration = this.declareNondetFunction(pType);
    return new CFunctionCallExpression(
        FileLocation.DUMMY,
        pType,
        new CIdExpression(FileLocation.DUMMY, functionDeclaration),
        new ArrayList<CExpression>(),
        functionDeclaration);
  }

  public CFunctionDeclaration declareNondetFunction(CType pType) {
    String nondetFunctionName = this.getNondetFunctionName(pType);
    return new CFunctionDeclaration(
        FileLocation.DUMMY,
        new CFunctionTypeWithNames(pType, new ArrayList<CParameterDeclaration>(), false),
        nondetFunctionName,
        new ArrayList<CParameterDeclaration>());
  }
}
