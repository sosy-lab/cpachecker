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

import com.google.common.truth.Truth;
import org.eclipse.wst.jsdt.core.dom.ThisExpression;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.ast.js.JSExpression;

public class ThisExpressionCFABuilderTest extends CFABuilderTestBase {

  @Test
  public void testThisExpression() {
    final ThisExpression thisExpression = parseExpression(ThisExpression.class, "this");
    // expected CFA: <entryNode>

    final JSExpression result = new ThisExpressionCFABuilder().append(builder, thisExpression);

    Truth.assertThat(result).isNotNull();
    Truth.assertThat(entryNode.getNumLeavingEdges()).isEqualTo(0);
  }
}
