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
import java.util.Deque;
import java.util.LinkedList;
import java.util.Queue;

import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class Function {

  private String name;
  private String header;
  private Deque<BlockStatement> blocks; // TODO why more than one block?

  public Function(ARGState fState) {
    CFunctionEntryNode entryNode = (CFunctionEntryNode) AbstractStates.extractLocation(fState);

    this.name = entryNode.getFunctionName();
    this.header = entryNode.getFunctionDefinition().getType().toASTString(name);
    this.blocks = new LinkedList<>();
    this.blocks.offerFirst(new BlockStatement(header));
  }

  public void enterBlock(String header) {
    BlockStatement stmt = new BlockStatement(header);
    blocks.peekFirst().append(stmt);
    blocks.offerFirst(stmt);
  }

  public void leaveBlock() {
    assert blocks.size() > 1; // TODO why not > 0?

    blocks.pollFirst();
  }

  public void append(Statement statement) {
    if (statement instanceof SimpleStatement && ((SimpleStatement)statement).getStatement().isEmpty()) {
      return;
    }

    blocks.peekFirst().append(statement); // TODO isn't this more like a preprend?
  }

  public String getName() {
    return name;
  }

  public String getPrototype() {
    return header + ";";
  }

  @Override
  public String toString() {
    assert blocks.size() == 1; // TODO how does this make sense?

    return blocks.peekFirst().toString();
  }
}
