// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.smg2.util;

import com.google.common.base.Preconditions;
import java.util.Optional;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cpa.value.type.Value;

public class CFunctionDeclarationAndOptionalValue {

  private final CFunctionDeclaration stackFrameFunctionDecl;

  private final Optional<Value> returnValue;

  private CFunctionDeclarationAndOptionalValue(
      CFunctionDeclaration funcDecl, Optional<Value> maybeReturnValue) {
    Preconditions.checkNotNull(funcDecl);
    Preconditions.checkNotNull(maybeReturnValue);
    stackFrameFunctionDecl = funcDecl;
    returnValue = maybeReturnValue;
  }

  public static CFunctionDeclarationAndOptionalValue of(
      CFunctionDeclaration funcDecl, Optional<Value> maybeReturnValue) {
    return new CFunctionDeclarationAndOptionalValue(funcDecl, maybeReturnValue);
  }

  public CFunctionDeclaration getCFunctionDeclaration() {
    return stackFrameFunctionDecl;
  }

  /**
   * @return the Value if hasReturnValue() is true, else Exception.
   */
  public Value getReturnValue() {
    return returnValue.orElseThrow();
  }

  public boolean hasReturnValue() {
    return returnValue.isPresent();
  }
}
