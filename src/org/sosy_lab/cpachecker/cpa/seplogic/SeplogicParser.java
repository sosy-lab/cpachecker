/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2012  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.seplogic;

import java.util.ArrayList;
import java.util.List;

import org.parboiled.BaseParser;
import org.parboiled.Rule;
import org.parboiled.annotations.BuildParseTree;
import org.parboiled.annotations.MemoMismatches;
import org.parboiled.annotations.SuppressNode;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.Argument;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.BooleanValue;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.Disjunction;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.Empty;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.Equality;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.Formula;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.Inequality;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.Intermediate;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.OpArgument;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.PurePredicate;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.SeparatingConjunction;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.SeplogicNode;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.SpatialPredicate;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.StringArgument;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.VarArgument;
import org.sosy_lab.cpachecker.cpa.seplogic.nodes.Variable;

@BuildParseTree
public class SeplogicParser extends BaseParser<SeplogicNode> {
  Rule Identifier() {
      return Sequence(TestNot(Keyword()), Letter(), ZeroOrMore(LetterOrDigit()), Spacing());
  }

  @SuppressNode
  Rule Spacing() {
    return ZeroOrMore(FirstOf(
      // whitespace
      OneOrMore(AnyOf(" \t\r\n\f").label("Whitespace")),

      // traditional comment
      Sequence("/*", ZeroOrMore(TestNot("*/"), ANY), "*/"),

      // end of line comment
      Sequence(
              "//",
              ZeroOrMore(TestNot(AnyOf("\r\n")), ANY),
              FirstOf("\r\n", '\r', '\n', EOI)
      )
    ));
  }

  @MemoMismatches
  Rule Keyword() {
      return Sequence(
              String("Emp"),
              String("False"),
              TestNot(LetterOrDigit())
      );
  }

  Rule Letter() {
    return FirstOf(CharRange('a', 'z'), CharRange('A', 'Z'), '_', '$');
  }

  Rule LetterOrDigit() {
    return FirstOf(CharRange('a', 'z'), CharRange('A', 'Z'), CharRange('0', '9'), '_', '$');
  }

  Rule Binop() {
    return FirstOf("builtin_div", "builtin_minus", "builtin_plus", "+");
  }

  Rule Cmpop() {
    return FirstOf("builtin_le", "builtin_lt", "builtin_ge", "builtin_gt");
  }

  Rule Lvariable_npv() {
    return Sequence(Identifier(), push(new Variable(match(), false)));
  }

  @SuppressWarnings("unchecked")
  Rule Term_npv() {
    return FirstOf(
        Sequence(Identifier(), push(new Intermediate(match())), "(", Term_npv_list(), ")",
            push(new OpArgument((String)((Intermediate)pop(1)).getValue(), ((List<Argument>)((Intermediate)pop()).getValue())))),
        Sequence(Lvariable_npv(), push(new VarArgument((Variable) pop()))),
        Sequence("(", Term_npv(), Binop(), push(new Intermediate(match())), Term_npv(), ")",
            push(createOpArgumentFromBinop())),
        StringConstant()
        );
  }

  SeplogicNode createOpArgumentFromBinop() {
    List<Argument> list = new ArrayList<Argument>();
    list.add((Argument) pop(2));
    list.add((Argument) pop());
    return new OpArgument((String)((Intermediate)pop()).getValue(), list);
  }

  Rule Term_npv_list_ne() {
    return FirstOf(Sequence(Term_npv(), convertSingleElementToList(), ",", Term_npv_list(), extendList()),
                   Sequence(Term_npv(), convertSingleElementToList()));
  }


  @SuppressWarnings("unchecked")
  Rule Term_npv_list() {
    return Sequence(push(new Intermediate(new ArrayList<Argument>())), Optional(Term_npv_list_ne(), extendList()));
  }

  Rule Formula_npv() {
    return Sequence(Formula_npv_inner(), ZeroOrMore(FirstOf("*", "||"), push(new Intermediate(matchOrDefault("*"))),
        Formula_npv_inner(), ((String)((Intermediate)pop(1)).getValue()).charAt(0) == '*' ? push(new SeparatingConjunction((Formula) pop(1), (Formula) pop())) : push(new Disjunction((Formula) pop(1), (Formula) pop()))));
  }

  @SuppressWarnings("unchecked")
  Rule Formula_npv_inner() {
    return FirstOf(
        Sequence(Term_npv(), "!=", Term_npv(), push(new Inequality((Argument) pop(1), (Argument) pop()))),
        Sequence(Term_npv(), "=", Term_npv(), push(new Equality((Argument) pop(1), (Argument) pop()))),
        Sequence(Term_npv(), Cmpop(), push(new Intermediate(match())), Term_npv(),
            push(new PurePredicate((String) ((Intermediate) pop(1)).getValue(), ((List<Argument>)((Intermediate)pop()).getValue())))),
        Sequence(Identifier(), push(new SpatialPredicate(match(), null)), "(", Term_npv_list(), ((SpatialPredicate)peek(1)).setIntermediateArguments(pop()), ")"),
        Sequence("(", Formula_npv(), ")"),
        Sequence("!", Identifier(), push(new Intermediate(match())), "(", Term_npv_list(), ")",
            push(new PurePredicate((String) ((Intermediate) pop(1)).getValue(), ((List<Argument>)((Intermediate)pop()).getValue())))),
        Sequence("Emp", push(new Empty())),
        Sequence("False", push(new BooleanValue(false))),
        Sequence(EMPTY, push(new Empty()))
        );
  }

  public Rule Formula_npv_input() {
    return Sequence(Formula_npv(), EOI);
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  boolean extendList() {
    ((List)((Intermediate)peek(1)).getValue()).addAll(((List)((Intermediate)pop()).getValue()));
    return true;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  boolean appendElementToList() {
    ((List)((Intermediate)peek(1)).getValue()).add(pop());
    return true;
  }

  @SuppressWarnings("unchecked")
  boolean convertSingleElementToList() {
    @SuppressWarnings("rawtypes")
    List list = new ArrayList();
    list.add(pop());
    return push(new Intermediate(list));
  }

  Rule StringConstant() {
    return Sequence(
      '"',
      ZeroOrMore(
        FirstOf(
          Sequence('\\', AnyOf("\"\'\\")),
          Sequence(TestNot(AnyOf("\r\n\"\\")), ANY)
        )
      ).suppressSubnodes(), push(new StringArgument(match())),
      '"',
      Optional(Spacing())
    );
}

  @Override
  protected Rule fromStringLiteral(String string) {
    return Sequence(String(string), Optional(Spacing())).label(string);
  }
}

/*
spatial_at:
| identifier L_PAREN term_list R_PAREN { mkSPred($1,$3) }
;
spatial_list_ne:
| spatial_at MULT spatial_list_ne  { pconjunction $1 $3 }
| spatial_at    { $1 }
;
spatial_list:
| spatial_list_ne { $1 }
|   { mkEmpty }
;

-- Sequents and rules

sequent:
| spatial_list OR formula VDASH formula { ($1,$3,$5,mkEmpty) }
| spatial_list OR formula VDASH formula DASHV formula { ($1,$3,$5,$7) }
;

-- Input files

question:
| IMPLICATION COLON formula_npv VDASH formula_npv {Implication($3,$5)}
| INCONSISTENCY COLON formula_npv {Inconsistency($3)}
| FRAME COLON formula_npv VDASH formula_npv {Frame($3,$5)}
| ABDUCTION COLON formula_npv VDASH formula_npv {Abduction($3,$5)}
| ABSRULE COLON formula_npv { Abs($3) }
;




fldlist_npv:
 | identifier EQUALS term_npv { [($1,$3)] }
 |  { [] }
 | identifier EQUALS term_npv SEMICOLON fldlist_npv  { ($1,$3) :: $5 }
;

fldlist:
 | identifier EQUALS term { [($1,$3)] }
 |  { [] }
 | identifier EQUALS term SEMICOLON fldlist  { ($1,$3) :: $5 }
;
*/
