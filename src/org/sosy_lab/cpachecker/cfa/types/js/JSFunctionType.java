/*
 * CPAchecker is a tool for configurable software verification.
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
package org.sosy_lab.cpachecker.cfa.types.js;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import java.util.Collections;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.types.AFunctionType;

public class JSFunctionType extends AFunctionType implements JSType {

  private static final long serialVersionUID = -8864415525596547956L;

  private String name = null;

  /**
   * Every JavaScript function has the same type since they can all take a dynamic count of
   * arguments (of any type) and return anything.
   */
  public static final JSFunctionType instance = new JSFunctionType();

  private JSFunctionType() {
    super(JSAnyType.ANY, Collections.singletonList(JSAnyType.ANY), true);
  }

  @Override
  public JSType getReturnType() {
    return (JSType) super.getReturnType();
  }

  public String getName() {
    return name;
  }

  public void setName(String pName) {
    checkState(name == null);
    name = checkNotNull(pName);
  }

  @SuppressWarnings("unchecked")
  @Override
  public List<JSType> getParameters() {
    return (List<JSType>) super.getParameters();
  }

  @Override
  public <R, X extends Exception> R accept(JSTypeVisitor<R, X> pVisitor) throws X {
    return pVisitor.visit(this);
  }
}
