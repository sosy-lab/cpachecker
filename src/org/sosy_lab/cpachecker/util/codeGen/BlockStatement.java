/*
 * CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2015  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.codeGen;

import java.util.ArrayList;
import java.util.List;

public class BlockStatement extends Statement {
  
  private List<Statement> statements;
  private String header;

  public BlockStatement(String pHeader) {
    this.header = pHeader;
    this.statements = new ArrayList<>();
  }

  public void append(Statement pStatement) {
    statements.add(pStatement);
  }

  @Override
  public String toString() {
    StringBuilder b = new StringBuilder();
    String lineSep = System.lineSeparator();

    b.append(header).append(" {").append(lineSep);
    for (Statement statement : statements) {
      b.append("  ").append(statement).append(lineSep);
    }
    b.append("}").append(lineSep);

    return b.toString();
  }
}
