// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.c;

import static com.google.common.base.Preconditions.checkNotNull;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;

public final class ACSLTemporaryDeclaration extends CVariableDeclaration implements CDeclaration {

  public ACSLTemporaryDeclaration(
      FileLocation pFileLocation,
      boolean pIsGlobal,
      CStorageClass pCStorageClass,
      CType pType,
      String pName,
      String pOrigName,
      String pQualifiedName,
      CInitializer pInitializer) {
    super(
        pFileLocation,
        pIsGlobal,
        pCStorageClass,
        pType,
        pName,
        pOrigName,
        pQualifiedName,
        pInitializer);
  }
}
