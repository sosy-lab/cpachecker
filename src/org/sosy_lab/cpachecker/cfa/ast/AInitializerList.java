/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2013  Dirk Beyer
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

import static com.google.common.collect.Iterables.transform;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;

public abstract class AInitializerList extends Initializer {

  private final List<? extends IAInitializer> initializerList;

  protected AInitializerList(final FileLocation pFileLocation,
                             final List<? extends IAInitializer> pInitializerList) {
    super(pFileLocation);
    initializerList = ImmutableList.copyOf(pInitializerList);
  }

  public List<? extends IAInitializer> getInitializers() {
    return    initializerList;
  }

  @Override
  public String toASTString() {
    StringBuilder lASTString = new StringBuilder();

    lASTString.append("{ ");
    Joiner.on(", ").appendTo(lASTString, transform(initializerList, AstNode.TO_AST_STRING));
    lASTString.append(" }");

    return lASTString.toString();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((initializerList == null) ? 0 : initializerList.hashCode());
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) { return true; }
    if (obj == null) { return false; }
    if (!(obj instanceof AInitializerList)) { return false; }
    AInitializerList other = (AInitializerList) obj;
    if (initializerList == null) {
      if (other.initializerList != null) { return false; }
    } else if (!initializerList.equals(other.initializerList)) { return false; }
    return true;
  }

}
