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
package org.sosy_lab.cpachecker.cfa.types;


import java.io.Serializable;

public interface Type extends Serializable {


  /**
   * Return a string representation of a variable declaration with a given name
   * and this type.
   *
   * Example:
   * If this type is array of int, and we call <code>toASTString("foo")</code>,
   * the result is <pre>int foo[]</pre>.
   *
   * @param declarator The name of the variable to declare.
   * @return A string representation of this type.
   */
  public  String toASTString(String declarator);

}
