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
package org.sosy_lab.cpachecker.util.ltl;

import org.sosy_lab.cpachecker.util.ltl.generated.LtlGrammarParser.InitFunctionContext;
import org.sosy_lab.cpachecker.util.ltl.generated.LtlGrammarParser.LtlPropertyContext;
import org.sosy_lab.cpachecker.util.ltl.generated.LtlGrammarParser.PropertyContext;
import org.sosy_lab.cpachecker.util.ltl.generated.LtlGrammarParserBaseVisitor;

public class SyntaxStringTreeVisitor extends LtlGrammarParserBaseVisitor<String> {

  @Override
  public String visitProperty(PropertyContext ctx) {
    // For a valid syntax, the context-param has to provide the following expressions in the given
    // order: CHECK LPAREN initFunction COMMA ltlProperty RPAREN EOF
    int childCount = ctx.getChildCount();
    if (childCount != 7) {
      throw new RuntimeException(
          String.format(
              "Invalid input provided. Expected %d child-nodes in param 'ctx', however, %d were found",
              7, childCount));
    }

    StringBuilder sb = new StringBuilder(childCount - 1);

    sb.append(ctx.CHECK());
    sb.append(ctx.LPAREN());
    sb.append(visit(ctx.initFunction()));
    sb.append(ctx.COMMA());
    sb.append(visit(ctx.ltlProperty()));
    sb.append(ctx.RPAREN());

    return sb.toString();
  }

  @Override
  public String visitInitFunction(InitFunctionContext ctx) {
    // The function-name of the C-method is deliberately left out in the result here
    return String.format(" %s%s  %s", ctx.INIT(), ctx.LPAREN(), ctx.RPAREN());
  }

  @Override
  public String visitLtlProperty(LtlPropertyContext ctx) {
    // The ltl-property is deliberately left out in the result here
    return String.format(" %s%s  %s ", ctx.LTL(), ctx.LPAREN(), ctx.RPAREN());
  }
}
