// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqIdExpressions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqInitializers;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;

public class SeqDeclarationBuilder {

  public static CFunctionDeclaration buildFunctionDeclarationWithoutParameters(
      CFunctionType pFunctionType, String pFunctionName) {

    return new CFunctionDeclaration(
        FileLocation.DUMMY, pFunctionType, pFunctionName, ImmutableList.of(), ImmutableSet.of());
  }

  public static CVariableDeclaration buildLastThreadDeclaration(CInitializer pInitializer) {
    return buildVariableDeclaration(
        true,
        CNumericTypes.UNSIGNED_INT,
        SeqIdExpressions.LAST_THREAD.getName(),
        // the initializer is dependent on the number of threads
        pInitializer);
  }

  public static CVariableDeclaration buildNextThreadDeclaration(MPOROptions pOptions) {
    return buildVariableDeclaration(
        true,
        pOptions.nondeterminismSigned() ? CNumericTypes.INT : CNumericTypes.UNSIGNED_INT,
        SeqIdExpressions.NEXT_THREAD.getName(),
        SeqInitializers.INT_0);
  }

  public static CVariableDeclaration buildRoundMaxDeclaration(MPOROptions pOptions) {
    return buildVariableDeclaration(
        true,
        pOptions.nondeterminismSigned() ? CNumericTypes.INT : CNumericTypes.UNSIGNED_INT,
        SeqIdExpressions.ROUND_MAX.getName(),
        SeqInitializers.INT_0);
  }

  public static CFunctionDeclaration buildThreadSimulationFunctionDeclaration(
      MPOROptions pOptions, int pThreadId) {

    CFunctionType functionType = new CFunctionType(CVoidType.VOID, ImmutableList.of(), false);
    String functionName = SeqNameUtil.buildThreadPrefix(pOptions, pThreadId) + "_sequentialized";
    return buildFunctionDeclarationWithoutParameters(functionType, functionName);
  }

  public static CVariableDeclaration buildVariableDeclaration(
      boolean pIsGlobal, CType pCType, String pName, CInitializer pInitializer) {

    return new CVariableDeclaration(
        FileLocation.DUMMY,
        pIsGlobal,
        CStorageClass.AUTO,
        pCType,
        pName,
        pName,
        SeqNameUtil.buildDummyQualifiedName(pName),
        pInitializer);
  }
}
