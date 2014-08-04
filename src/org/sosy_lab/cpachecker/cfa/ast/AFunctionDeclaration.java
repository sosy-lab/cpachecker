/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.ast;

import java.util.List;
import java.util.Objects;

import org.sosy_lab.cpachecker.cfa.types.IAFunctionType;

import com.google.common.collect.ImmutableList;


public abstract class AFunctionDeclaration extends ADeclaration {

  private final List<AParameterDeclaration> parameters;

  public AFunctionDeclaration(FileLocation pFileLocation, IAFunctionType pType, String pName,
      List<? extends AParameterDeclaration> pParameters) {
    super(pFileLocation, true, pType, pName, pName);

    parameters = ImmutableList.copyOf(pParameters);
  }

  @Override
  public IAFunctionType getType() {
    return  (IAFunctionType) super.getType();
  }

  public List<? extends AParameterDeclaration> getParameters() {
    return parameters;
  }

  @Override
  public String getQualifiedName() {
    return getName();
  }

  @Override
  public int hashCode() {
    int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(parameters);
    result = prime * result + super.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof AFunctionDeclaration)
        || !super.equals(obj)) {
      return false;
    }

    AFunctionDeclaration other = (AFunctionDeclaration) obj;

    return Objects.equals(other.parameters, parameters);
  }
}
