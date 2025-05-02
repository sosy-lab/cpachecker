// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder;

import com.google.common.collect.ImmutableList;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqInitializers.SeqInitializer;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqArrayType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.constants.SeqTypes.SeqSimpleType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ghost_variables.pc.PcVariables;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.hard_coded.SeqToken;

public class SeqDeclarationBuilder {

  // TODO SubstituteBuilder.substituteVarDec also uses CVariableDeclaration constructor
  public static CVariableDeclaration buildVariableDeclaration(
      boolean pIsGlobal, CType pCType, String pName, CInitializer pInitializer) {

    return new CVariableDeclaration(
        FileLocation.DUMMY,
        pIsGlobal,
        CStorageClass.AUTO,
        pCType,
        pName,
        pName,
        SeqNameUtil.buildQualifiedName(pName),
        pInitializer);
  }

  public static ImmutableList<CVariableDeclaration> buildPcDeclarations(
      PcVariables pPcVariables, int pNumThreads, boolean pScalarPc) {

    ImmutableList.Builder<CVariableDeclaration> rDeclarations = ImmutableList.builder();
    if (pScalarPc) {
      // declare scalar int for each thread: pc0 = 0; pc1 = -1; ...
      for (int i = 0; i < pNumThreads; i++) {
        rDeclarations.add(
            SeqDeclarationBuilder.buildVariableDeclaration(
                false,
                SeqSimpleType.INT,
                pPcVariables.get(i).toASTString(),
                i == 0 ? SeqInitializer.INT_0 : SeqInitializer.INT_MINUS_1));
      }
    } else {
      // declare int array: pc[] = { 0, -1, ... };
      ImmutableList.Builder<CInitializer> initializers = ImmutableList.builder();
      for (int i = 0; i < pNumThreads; i++) {
        initializers.add(i == 0 ? SeqInitializer.INT_0 : SeqInitializer.INT_MINUS_1);
      }
      CInitializerList initializerList =
          new CInitializerList(FileLocation.DUMMY, initializers.build());
      rDeclarations.add(
          SeqDeclarationBuilder.buildVariableDeclaration(
              false, SeqArrayType.INT_ARRAY, SeqToken.pc, initializerList));
    }
    return rDeclarations.build();
  }
}
