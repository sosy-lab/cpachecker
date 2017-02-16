/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2017  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.harness;

import java.io.IOException;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.ARightHandSide;
import org.sosy_lab.cpachecker.cfa.types.Type;

class CodeAppender implements Appendable {

  private final Appendable appendable;

  public CodeAppender(Appendable pAppendable) {
    appendable = Objects.requireNonNull(pAppendable);
  }

  @Override
  public String toString() {
    return appendable.toString();
  }

  public CodeAppender appendVectorIndexDeclaration(String pInputFunctionVectorIndexName) throws IOException {
    appendable.append("  static unsigned int ");
    appendable.append(pInputFunctionVectorIndexName);
    appendln(";");
    return this;
  }

  public CodeAppender appendDeclaration(Type pType, String pName)
      throws IOException {
    appendable.append(pType.toASTString(pName));
    appendln(";");
    return this;
  }

  public CodeAppender appendAssignment(String pRetvalName, ARightHandSide pValue)
      throws IOException {
    appendable.append(pRetvalName);
    appendable.append(" = ");
    appendable.append(pValue.toASTString());
    return this;
  }

  public CodeAppender appendAssignment(String pRetvalName, TestValue pValue)
      throws IOException {
    boolean hasAuxiliaryStatmenets = pValue.getAuxiliaryStatements().size() > 0;
    if (hasAuxiliaryStatmenets) {
      appendable.append("{ ");
      for (AAstNode auxiliaryStatement : pValue.getAuxiliaryStatements()) {
        appendable.append(auxiliaryStatement.toASTString());
        appendable.append(" ");
      }
    }
    appendable.append(pRetvalName);
    appendable.append(" = ");
    appendable.append(pValue.getValue().toASTString());
    appendable.append(";");
    if (hasAuxiliaryStatmenets) {
      appendable.append(" }");
    }
    return this;
  }

  public CodeAppender appendln(String pLine) throws IOException {
    appendable.append(pLine);
    appendln();
    return this;
  }

  public Appendable appendln() throws IOException {
    appendable.append(System.lineSeparator());
    return this;
  }

  @Override
  public CodeAppender append(CharSequence pCsq) throws IOException {
    appendable.append(pCsq);
    return this;
  }

  @Override
  public CodeAppender append(char pChar) throws IOException {
    appendable.append(pChar);
    return this;
  }

  @Override
  public CodeAppender append(CharSequence pCsq, int pStart, int pEnd) throws IOException {
    appendable.append(pCsq, pStart, pEnd);
    return this;
  }

}
