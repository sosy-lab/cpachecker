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

import org.sosy_lab.cpachecker.cfa.model.c.CFunctionEntryNode;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.util.AbstractStates;

public class Function extends BlockStatement {

  private static int nextId = 0;

  private String name;
  private String id;

  private Function() {}

  public Function(ARGState fState) {
    super(null);

    CFunctionEntryNode entryNode = (CFunctionEntryNode) AbstractStates.extractLocation(fState);

    this.name = entryNode.getFunctionName();

    // don't append id to main method!
    this.id = ("main".equals(entryNode.getFunctionName()) ? "" : getId());
    this.header = new SimpleStatement(entryNode.getFunctionDefinition().getType().toASTString(getName()));
  }

  private static synchronized String getId() {
    String result = "_" + nextId;
    nextId++;

    return result;
  }

  public String getName() {
    return getName(true);
  }

  public String getName(boolean addId) {

    if (addId) {
      return name + id;
    } else {
      return name;
    }
  }

  public String getPrototype() {
    return header + ";";
  }

  @Override
  public Function clone() {
    Function clone = new Function();
    BlockStatement blockClone = super.clone();

    clone.name = this.name;
    clone.id = this.id;
    clone.header = this.header.clone();

    clone.children = blockClone.children;
    clone.parent = blockClone.parent;
    clone.declarations = blockClone.declarations;

    return clone;
  }
}
