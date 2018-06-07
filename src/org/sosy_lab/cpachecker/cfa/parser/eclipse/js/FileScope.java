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

import javax.annotation.Nonnull;

/**
 * The scope of the JavaScript input file. Only single JavaScript input files are supported at the
 * moment. Thus, it is not necessary to include the file name in the name of the scope. It is
 * shorter without it and still unique. Apart from this, some parts of CPAchecker currently do not
 * support the characters contained in the file path.
 */
interface FileScope extends Scope {

  String getFileName();

  @Nonnull
  @Override
  default String getNameOfScope() {
    // Some parts of CPAchecker currently do not support the characters contained in the file path.
    // Further, it is not necessary to include the file name in the name of the scope at the
    // moment (see description of interface FileScope).
    // Replace the following return statement with the return statement below it, when file name
    // should be part of the scope name.
    return "";
    //    return getFileName();
  }
}
