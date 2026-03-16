// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.acsl.annotations;

import com.google.common.base.Preconditions;
import java.util.Objects;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLogicDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.acsl.AcslLogicDefinition;

public final class AcslLogicDefinitionAnnotation extends AAcslAnnotation {

  private final AcslLogicDefinition definition;

  public AcslLogicDefinitionAnnotation(
      FileLocation pFileLocation, AcslLogicDefinition pDefinition) {
    super(pFileLocation);
    Preconditions.checkNotNull(pDefinition);
    definition = pDefinition;
  }

  public AcslLogicDefinition getDefinition() {
    return definition;
  }

  public AcslLogicDeclaration getDeclaration() {
    return definition.getDeclaration();
  }

  @Override
  public boolean equals(Object pO) {
    if (this == pO) {
      return true;
    }
    return pO instanceof AcslLogicDefinitionAnnotation other && definition.equals(other.definition);
  }

  @Override
  public int hashCode() {
    int hash = 7;
    int prime = 31;
    hash = prime * hash * Objects.hashCode(definition);
    return hash;
  }

  @Override
  public String toAstString() {
    return definition.toASTString();
  }
}
