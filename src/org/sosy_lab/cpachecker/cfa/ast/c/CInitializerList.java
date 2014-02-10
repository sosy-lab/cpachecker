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
package org.sosy_lab.cpachecker.cfa.ast.c;

import static com.google.common.collect.Iterables.transform;

import java.util.List;
import java.util.Objects;

import org.sosy_lab.cpachecker.cfa.ast.AInitializer;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;


public class CInitializerList extends AInitializer implements CInitializer, CAstNode {

  private final List<CInitializer> initializerList;

  public CInitializerList(final FileLocation pFileLocation,
                          final List<CInitializer> pInitializerList) {
    super(pFileLocation);
    initializerList = ImmutableList.copyOf(pInitializerList);
  }

  public List<CInitializer> getInitializers() {
    return initializerList;
  }

  @Override
  public String toASTString() {
    StringBuilder lASTString = new StringBuilder();

    lASTString.append("{ ");
    Joiner.on(", ").appendTo(lASTString, transform(initializerList, CInitializer.TO_AST_STRING));
    lASTString.append(" }");

    return lASTString.toString();
  }

  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 7;
    result = prime * result + Objects.hashCode(initializerList);
    result = prime * result + super.hashCode();
    return result;
  }

  /* (non-Javadoc)
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }

    if (!(obj instanceof CInitializerList)
        || !super.equals(obj)) {
      return false;
    }

    CInitializerList other = (CInitializerList) obj;

    return Objects.equals(other.initializerList, initializerList);
  }

  @Override
  public <R, X extends Exception> R accept(CInitializerVisitor<R, X> v) throws X {
    return v.visit(this);
  }
}
