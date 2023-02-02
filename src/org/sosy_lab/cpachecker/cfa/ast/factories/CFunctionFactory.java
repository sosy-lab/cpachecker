// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.factories;

import com.google.common.collect.ImmutableSet;
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
          nondetFunctionName = "__VERIFIER_nondet_bool";
          break;
        case CHAR:
          nondetFunctionName = "__VERIFIER_nondet_char";
          break;
        case DOUBLE:
          nondetFunctionName = "__VERIFIER_nondet_double";
          break;
        case FLOAT:
        case FLOAT128:
          nondetFunctionName = "__VERIFIER_nondet_float";
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
              nondetFunctionName = "__VERIFIER_nondet_int";
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
    if (functionDeclaration == null) {
      return null;
    }
    return new CFunctionCallExpression(
        FileLocation.DUMMY,
        pType,
        new CIdExpression(FileLocation.DUMMY, functionDeclaration),
        new ArrayList<CExpression>(),
        functionDeclaration);
  }

  public CFunctionDeclaration declareNondetFunction(CType pType) {
    String nondetFunctionName = this.getNondetFunctionName(pType);
    if (nondetFunctionName == null) {
      return null;
    }
    return new CFunctionDeclaration(
        FileLocation.DUMMY,
        new CFunctionTypeWithNames(pType, new ArrayList<CParameterDeclaration>(), false),
        nondetFunctionName,
        new ArrayList<CParameterDeclaration>(),
        ImmutableSet.of());
  }

  public boolean isUserDefined(CFunctionCallExpression pFunctionCall) {
    CType functionReturnType = pFunctionCall.getDeclaration().getType().getReturnType();
    String nonDetFunctionWithType = this.getNondetFunctionName(functionReturnType);
    if (nonDetFunctionWithType == null) {
      // TODO: Improve recognition of assert functions. Which may be wrongly detected because of
      // shadowing
      if (pFunctionCall.getDeclaration().getOrigName().equals("assert")
          && pFunctionCall.getDeclaration().getName().equals("assert")
          && pFunctionCall.getDeclaration().isGlobal()
          && pFunctionCall.getDeclaration().getParameters().size() == 1
          && pFunctionCall
              .getDeclaration()
              .getParameters()
              .get(0)
              .getQualifiedName()
              .equals("assert::arg")) {
        return false;
      } else {
        return true;
      }
    } else if (nonDetFunctionWithType.equals(pFunctionCall.getDeclaration().getOrigName())) {
      return false;
    } else {
      return true;
    }
  }
}
