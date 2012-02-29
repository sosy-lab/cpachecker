/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.seplogic.nodes;




public class Variable extends SeplogicNode {
  String name;
  boolean isAny;

  public Variable(String pMatch, boolean pAny) {
    if (pMatch.trim().equals("0")) {
      int i = 0;
      i++;
    }

    name = pMatch.trim(); // XXX fix grammar
    isAny = pAny;
  }

  public Variable(String pVarName) {
    name = pVarName.trim(); // XXX fix grammar
    isAny = false;
  }

  @Override
  public String toString() {
    return (isAny ? "?" : "") + name;
  }

  @Override
  public SeplogicNode accept(NodeVisitor pVisitor) {
    return pVisitor.visitNode(this);
  }

}
