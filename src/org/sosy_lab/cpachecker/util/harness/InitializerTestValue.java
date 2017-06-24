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

import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.AAstNode;
import org.sosy_lab.cpachecker.cfa.ast.AInitializer;

public class InitializerTestValue extends TestValue {

  private final AInitializer value;

  private InitializerTestValue(ImmutableList<AAstNode> pAuxiliaryStatements, AInitializer pValue) {
    super(pAuxiliaryStatements, pValue);
    value = pValue;
  }

  @Override
  public AInitializer getValue() {
    return value;
  }

  public static InitializerTestValue of(AInitializer pValue) {
    return of(Collections.emptyList(), pValue);
  }

  public static InitializerTestValue of(List<AAstNode> pAuxiliaryStatments, AInitializer pValue) {
    return new InitializerTestValue(ImmutableList.copyOf(pAuxiliaryStatments), pValue);
  }
}
