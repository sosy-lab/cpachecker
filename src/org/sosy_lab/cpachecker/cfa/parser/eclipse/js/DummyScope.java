/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
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
package org.sosy_lab.cpachecker.cfa.parser.eclipse.js;

import java.util.Optional;
import javax.annotation.Nonnull;
import org.sosy_lab.cpachecker.cfa.ast.js.JSSimpleDeclaration;

class DummyScope implements Scope {
  static final DummyScope instance = new DummyScope();

  private DummyScope() {}

  @Override
  public Scope getParentScope() {
    return null;
  }

  @Nonnull
  @Override
  public String getNameOfScope() {
    return "";
  }

  @Override
  public void addDeclaration(final JSSimpleDeclaration pDeclaration) {
    throw new RuntimeException("addDeclaration of DummyScope should not be called");
  }

  @Override
  public Optional<? extends JSSimpleDeclaration> findDeclaration(final String pIdentifier) {
    return Optional.empty();
  }
}
