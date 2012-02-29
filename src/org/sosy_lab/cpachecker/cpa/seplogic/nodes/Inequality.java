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



public class Inequality extends Formula {
  Argument a1, a2;

  public Inequality(Argument pArgument, Argument pArgument2) {
    a1 = pArgument;
    a2 = pArgument2;
  }

  @Override
  public String toString() {
    return a1.toString() + " != " + a2.toString();
  }

  @Override
  public SeplogicNode accept(NodeVisitor pVisitor) {
    return pVisitor.visitNode(this);
  }

}
