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
  
  private List<Statement> children;
  private BlockStatement parent;
  private String header;

  public BlockStatement(BlockStatement parent, String header) {
    this.children = new ArrayList<>();
    this.parent = parent;
    this.header = header;
  }

  public void add(SimpleStatement statement) {
    children.add(statement);
  }
  
  public BlockStatement enterBlock(String header) {
    BlockStatement newBlock = new BlockStatement(this, header);
    children.add(newBlock);
    
    return newBlock;
  }
  
  public BlockStatement leaveBlock() {
    return parent;
  }

  @Override
  protected String toString(String indentation) {
    StringBuilder b = new StringBuilder();
    String lineSep = System.lineSeparator();

    b.append(indentation).append(header).append(" {").append(lineSep);
    for (Statement statement : children) {
      b.append(statement.toString(indentation + "  ")).append(lineSep);
    }
    b.append(indentation).append("}").append(lineSep);

    return b.toString();
  }
}
