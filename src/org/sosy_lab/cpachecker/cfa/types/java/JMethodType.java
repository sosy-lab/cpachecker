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
package org.sosy_lab.cpachecker.cfa.types.java;

import java.util.List;

import org.sosy_lab.cpachecker.cfa.ast.java.JParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.types.AFunctionType;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;


public class JMethodType extends AFunctionType implements JType {

  private final List<JParameterDeclaration> parameters;

  public JMethodType(JType pReturnType, List<JParameterDeclaration> pParameters, boolean pTakesVarArgs) {
    super(pReturnType,
        FluentIterable.from(pParameters).transform(new Function<JParameterDeclaration, JType>() {
          @Override
          public JType apply(JParameterDeclaration pInput) {
            return pInput.getType();
          }
        }).toList(),
        pTakesVarArgs);

    parameters = ImmutableList.copyOf(pParameters);
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<JType> getParameters() {
    return (List<JType>) super.getParameters();
  }

  public List<JParameterDeclaration> getParameterDeclarations() {
    return parameters;
  }
}
