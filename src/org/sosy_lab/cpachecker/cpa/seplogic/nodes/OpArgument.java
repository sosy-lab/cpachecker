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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Joiner;


public class OpArgument extends Argument {
  String op;
  List<Argument> args;

  public OpArgument(String pValue, List<Argument> pList) {
    if (pValue.equals("+"))
      pValue = "builtin_plus";
    op = pValue;
    args = pList;
  }

  public OpArgument(String pValue, Argument ... argArray) {
    op = pValue;
    args = new ArrayList<Argument>(Arrays.asList(argArray));
  }

  @Override
  public String toString() {
    return op + "(" + Joiner.on(", ").join(args) + ")";
  }

  @Override
  public SeplogicNode accept(NodeVisitor pVisitor) {
    return pVisitor.visitNode(this);
  }

}
