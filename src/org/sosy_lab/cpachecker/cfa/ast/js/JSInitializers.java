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
package org.sosy_lab.cpachecker.cfa.ast.js;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.sosy_lab.cpachecker.cfa.ast.FileLocation;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class JSInitializers {

  /**
   * Take a variable declaration and create a list of assignment statements
   * that assign the value(s) of the initializer to the declared variable.
   *
   * @param decl The variable declaration.
   * @param edge The current CFA edge.
   * @return A (possibly empty) list of assignment statements.
   */
  public static List<JSExpressionAssignmentStatement> convertToAssignments(
      JSVariableDeclaration decl, CFAEdge edge) throws UnrecognizedCodeException {
    final JSLeftHandSide lhs = new JSIdExpression(decl.getFileLocation(), decl);
    final JSInitializer init = decl.getInitializer();
    if (init == null) {
      return ImmutableList.of(
          new JSExpressionAssignmentStatement(
              decl.getFileLocation(), lhs, new JSUndefinedLiteralExpression(FileLocation.DUMMY)));
    } else if (init instanceof JSInitializerExpression) {
      return ImmutableList.of(
          new JSExpressionAssignmentStatement(
              decl.getFileLocation(), lhs, ((JSInitializerExpression) init).getExpression()));
    } else {
      throw new UnrecognizedCodeException("Unknown initializer type", edge, init);
    }
  }

}
