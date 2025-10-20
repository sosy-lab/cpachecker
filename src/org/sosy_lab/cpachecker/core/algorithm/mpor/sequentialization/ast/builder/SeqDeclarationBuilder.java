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
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CNumericTypes;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.MPOROptions;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.SequentializationFields;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqExpressions.SeqIdExpression;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqInitializers.SeqInitializer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqArrayType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;

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
        SeqIdExpression.LAST_THREAD.getName(),
        // the initializer is dependent on the number of threads
        pInitializer);
  }

  public static CVariableDeclaration buildNextThreadDeclaration(MPOROptions pOptions) {
    return buildVariableDeclaration(
        true,
        pOptions.nondeterminismSigned ? CNumericTypes.INT : CNumericTypes.UNSIGNED_INT,
        SeqIdExpression.NEXT_THREAD.getName(),
        SeqInitializer.INT_0);
  }

  public static CVariableDeclaration buildRoundMaxDeclaration(MPOROptions pOptions) {
    return buildVariableDeclaration(
        true,
        pOptions.nondeterminismSigned ? CNumericTypes.INT : CNumericTypes.UNSIGNED_INT,
        SeqIdExpression.ROUND_MAX.getName(),
        SeqInitializer.INT_0);
  }

  public static CFunctionDeclaration buildThreadSimulationFunctionDeclaration(int pThreadId) {
    CFunctionType functionType = new CFunctionType(CVoidType.VOID, ImmutableList.of(), false);
    String functionName = SeqNameUtil.buildFunctionName(SeqToken.thread + pThreadId);
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

  public static ImmutableList<CVariableDeclaration> buildPcDeclarations(
      MPOROptions pOptions, SequentializationFields pFields) {

    ImmutableList.Builder<CVariableDeclaration> rDeclarations = ImmutableList.builder();
    if (pOptions.scalarPc) {
      // declare scalar int for each thread: pc0 = 0; pc1 = -1; ...
      for (int i = 0; i < pFields.numThreads; i++) {
        rDeclarations.add(
            SeqDeclarationBuilder.buildVariableDeclaration(
                true,
                CNumericTypes.UNSIGNED_INT,
                pFields.ghostElements.getPcVariables().getPcLeftHandSide(i).toASTString(),
                SeqInitializer.getPcInitializer(i == 0)));
      }
    } else {
      // declare int array: pc[] = { 0, -1, ... };
      ImmutableList.Builder<CInitializer> initializers = ImmutableList.builder();
      for (int i = 0; i < pFields.numThreads; i++) {
        initializers.add(SeqInitializer.getPcInitializer(i == 0));
      }
      CInitializerList initializerList =
          new CInitializerList(FileLocation.DUMMY, initializers.build());
      rDeclarations.add(
          SeqDeclarationBuilder.buildVariableDeclaration(
              true, SeqArrayType.UNSIGNED_INT_ARRAY, SeqToken.pc, initializerList));
    }
    return rDeclarations.build();
  }
}
