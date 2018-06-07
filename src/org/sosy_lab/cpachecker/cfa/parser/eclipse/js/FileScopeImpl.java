/*
 * CPAchecker is a tool for configurable software verification.
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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import org.sosy_lab.cpachecker.cfa.ast.js.JSSimpleDeclaration;

class FileScopeImpl implements FileScope {
  private final String fileName;

  /** Global declarations in the file mapped by their original name. */
  private final Map<String, JSSimpleDeclaration> localDeclarations = new HashMap<>();

  FileScopeImpl(final String pFileName) {
    fileName = pFileName;
  }

  @Override
  public String getFileName() {
    return fileName;
  }

  @Override
  public Scope getParentScope() {
    return null;
  }

  @Override
  public void addDeclaration(@Nonnull final JSSimpleDeclaration pDeclaration) {
    final String origName = pDeclaration.getOrigName();
    assert !localDeclarations.containsKey(origName);
    localDeclarations.put(origName, pDeclaration);
  }

  @Override
  public Optional<? extends JSSimpleDeclaration> findDeclaration(
      @Nonnull final String pIdentifier) {
    return Optional.ofNullable(localDeclarations.get(pIdentifier));
  }
}
