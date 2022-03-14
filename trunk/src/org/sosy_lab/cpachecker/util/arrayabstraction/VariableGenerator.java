// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.arrayabstraction;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionTypeWithNames;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.util.states.MemoryLocation;

class VariableGenerator {

  private final String prefix;
  private int counter;

  public VariableGenerator(String pPrefix) {
    prefix = pPrefix;
  }

  private static String getNondetFunctionName(CType pType) {

    if (pType instanceof CSimpleType) {
      CSimpleType simpleType = (CSimpleType) pType;
      CBasicType basicType = simpleType.getType();

      // TODO: handle all types that have corresponding `__VERIFIER_nondet_X` functions

      if (basicType == CBasicType.INT) {
        return "__VERIFIER_nondet_int";
      } else if (basicType == CBasicType.UNSPECIFIED && simpleType.isLong()) {
        return "__VERIFIER_nondet_long";
      }
    }

    throw new AssertionError(
        "Unable to find nondet function name (__VERIFIER_nondet_X) for type: " + pType);
  }

  static CFunctionCallExpression createNondetFunctionCallExpression(CType pType) {

    String nondetFunctionName = getNondetFunctionName(pType);
    CFunctionTypeWithNames nondetFunctionType =
        new CFunctionTypeWithNames(pType, ImmutableList.of(), false);
    nondetFunctionType.setName(nondetFunctionName);
    CFunctionDeclaration nondetFunctionDeclaration =
        new CFunctionDeclaration(
            FileLocation.DUMMY,
            nondetFunctionType,
            nondetFunctionName,
            nondetFunctionName,
            ImmutableList.of(),
            ImmutableSet.of());
    CIdExpression nondetFunctionNameExpression =
        new CIdExpression(
            FileLocation.DUMMY, nondetFunctionType, nondetFunctionName, nondetFunctionDeclaration);
    CFunctionCallExpression functionCallExpression =
        new CFunctionCallExpression(
            FileLocation.DUMMY,
            pType,
            nondetFunctionNameExpression,
            ImmutableList.of(),
            nondetFunctionDeclaration);

    return functionCallExpression;
  }

  static CIdExpression createVariableNameExpression(CType pType, MemoryLocation pMemoryLocation) {

    checkNotNull(pType);
    checkNotNull(pMemoryLocation);

    String variableName = pMemoryLocation.getIdentifier();

    CVariableDeclaration variableDeclaration =
        new CVariableDeclaration(
            FileLocation.DUMMY,
            false,
            CStorageClass.AUTO,
            pType,
            variableName,
            variableName,
            pMemoryLocation.getExtendedQualifiedName(),
            null);
    CIdExpression variableNameExpression =
        new CIdExpression(FileLocation.DUMMY, pType, variableName, variableDeclaration);

    return variableNameExpression;
  }

  static CIdExpression createVariableNameExpression(
      CType pType, String pVariableName, Optional<String> pFunctionName) {

    checkNotNull(pType);
    checkNotNull(pVariableName);
    checkNotNull(pFunctionName);

    if (pFunctionName.isPresent()) {
      return createVariableNameExpression(
          pType, MemoryLocation.forLocalVariable(pFunctionName.orElseThrow(), pVariableName));
    } else {
      return createVariableNameExpression(pType, MemoryLocation.forIdentifier(pVariableName));
    }
  }

  static CFAEdge createNondetVariableEdge(
      CType pType, String pVariableName, Optional<String> pFunctionName) {

    checkNotNull(pType);
    checkNotNull(pVariableName);
    checkNotNull(pFunctionName);

    CIdExpression lhs = createVariableNameExpression(pType, pVariableName, pFunctionName);
    CFunctionCallExpression rhs = createNondetFunctionCallExpression(pType);

    CStatement statement = new CFunctionCallAssignmentStatement(FileLocation.DUMMY, lhs, rhs);

    CStatementEdge statementEdge =
        new CStatementEdge(
            "",
            statement,
            FileLocation.DUMMY,
            CFANode.newDummyCFANode("dummy-predecessor"),
            CFANode.newDummyCFANode("dummy-successor"));

    return statementEdge;
  }

  String createNewVariableName() {
    return prefix + counter++;
  }
}
