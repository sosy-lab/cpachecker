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
package org.sosy_lab.cpachecker.cfa.ast.java;

import org.sosy_lab.cpachecker.cfa.ast.IASimpleDeclaration;
import org.sosy_lab.cpachecker.cfa.types.java.JType;

/**
 * This class represents the core components that occur in each declaration:
 * a type and an (optional) name.
 *
 * It is part of the declaration of types and variables (see {@link JDeclaration})
 * and methods (see {@link JMethodDeclaration}).
 * It is also used stand-alone for the declaration of function parameters.
 */
public interface JSimpleDeclaration extends IASimpleDeclaration, JAstNode {

  @Override
  public JType getType();

}
