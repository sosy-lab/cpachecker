// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast;

import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.types.IAFunctionType;

public abstract class AFunctionDeclaration extends AbstractDeclaration {

  private static final long serialVersionUID = -4385134795747669972L;
  private final List<AParameterDeclaration> parameters;

  protected AFunctionDeclaration(
      FileLocation pFileLocation,
      IAFunctionType pType,
      String pName,
      String pOrigName,
      List<? extends AParameterDeclaration> pParameters) {
    super(pFileLocation, true, pType, pName, pOrigName);

    parameters = ImmutableList.copyOf(pParameters);
  }

  @Override
  public IAFunctionType getType() {
    return (IAFunctionType) super.getType();
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
    return 31 * Objects.hashCode(parameters) + super.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof AFunctionDeclaration) || !super.equals(obj)) {
      return false;
    }

    AFunctionDeclaration other = (AFunctionDeclaration) obj;

    return Objects.equals(other.parameters, parameters);
  }
}
