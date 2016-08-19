/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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

  private final static String PRIMED_VARIABLE_POSTFIX = "__TERMINATION_PRIMED";

  private final static String DEREFERENCE_POSTFIX = "__TERMINATION_DEREFERENCED";

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
