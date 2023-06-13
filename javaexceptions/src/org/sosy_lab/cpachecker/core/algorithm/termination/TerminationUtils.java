// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.termination;

import static com.google.common.base.Preconditions.checkArgument;

import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.c.CSimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.types.c.CFunctionType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CStorageClass;
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cfa.types.c.CVoidType;

public class TerminationUtils {

  private static final String PRIMED_VARIABLE_POSTFIX = "__TERMINATION_PRIMED";

  private static final String DEREFERENCE_POSTFIX = "__TERMINATION_DEREFERENCED";

  private TerminationUtils() {}

  public static CVariableDeclaration createPrimedVariable(CVariableDeclaration pVariableDecl) {
    return new CVariableDeclaration(
        FileLocation.DUMMY,
        false,
        CStorageClass.AUTO,
        pVariableDecl.getType(),
        pVariableDecl.getName() + PRIMED_VARIABLE_POSTFIX,
        pVariableDecl.getOrigName() + PRIMED_VARIABLE_POSTFIX,
        pVariableDecl.getQualifiedName() + PRIMED_VARIABLE_POSTFIX,
        null);
  }

  public static CVariableDeclaration createDereferencedVariable(CSimpleDeclaration pVariableDecl) {
    CType type = pVariableDecl.getType();
    if (type instanceof CPointerType) {
      CType innerType = ((CPointerType) type).getType();
      checkArgument(!(innerType instanceof CVoidType));
      checkArgument(!(innerType instanceof CFunctionType));

      return new CVariableDeclaration(
          FileLocation.DUMMY,
          false,
          CStorageClass.AUTO,
          innerType,
          pVariableDecl.getName() + DEREFERENCE_POSTFIX,
          pVariableDecl.getOrigName() + DEREFERENCE_POSTFIX,
          pVariableDecl.getQualifiedName() + DEREFERENCE_POSTFIX,
          null);

    } else {
      throw new IllegalArgumentException(type + " is not a pointer type");
    }
  }
}
