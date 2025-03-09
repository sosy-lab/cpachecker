// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.ast.builder;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.core.algorithm.mpor.sequentialization.strings.SeqNameUtil;

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
}
