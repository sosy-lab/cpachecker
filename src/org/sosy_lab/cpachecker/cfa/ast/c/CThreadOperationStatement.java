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
package org.sosy_lab.cpachecker.cfa.ast.c;

import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

@SuppressWarnings("EqualsGetClass")
// FIXME: This class is broken, because comparing a CThreadOperationStatement with an
// CFunctionCallStatement is not symmetric
public class CThreadOperationStatement extends CFunctionCallStatement {

  private static final long serialVersionUID = -7543988390816591658L;

  public static class CThreadCreateStatement extends CThreadOperationStatement {

    private static final long serialVersionUID = -1211707394397959801L;

    public CThreadCreateStatement(FileLocation pFileLocation, CFunctionCallExpression pFunctionCall,
        boolean pSelfParallel, String pVarName) {
      super(pFileLocation, pFunctionCall, pSelfParallel, pVarName);
    }
  }

  public static class CThreadJoinStatement extends CThreadOperationStatement {

    private static final long serialVersionUID = -2328781305617198230L;

    public CThreadJoinStatement(FileLocation pFileLocation, CFunctionCallExpression pFunctionCall,
        boolean pSelfParallel, String pVarName) {
      super(pFileLocation, pFunctionCall, pSelfParallel, pVarName);
    }
  }

  private final boolean isSelfParallel;
  private final String assosiatedVariable;

  public CThreadOperationStatement(FileLocation pFileLocation, CFunctionCallExpression pFunctionCall
      , boolean selfParallel, String varName) {
    super(pFileLocation, pFunctionCall);
    isSelfParallel = selfParallel;
    assosiatedVariable = varName;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + Objects.hashCode(assosiatedVariable);
    result = prime * result + (isSelfParallel ? 1231 : 1237);
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    CThreadOperationStatement other = (CThreadOperationStatement) obj;
    return isSelfParallel == other.isSelfParallel
        && Objects.equals(assosiatedVariable, other.assosiatedVariable);
  }

  public boolean isSelfParallel() {
    return isSelfParallel;
  }

  public String getVariableName() {
    return assosiatedVariable;
  }
}
