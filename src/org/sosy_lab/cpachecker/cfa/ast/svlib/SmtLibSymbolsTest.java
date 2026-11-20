// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.ast.svlib;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableSet;
import org.antlr.v4.runtime.Vocabulary;
import org.junit.Test;
import org.sosy_lab.cpachecker.cfa.parser.svlib.antlr.generated.SvLibLexer;

public class SmtLibSymbolsTest {

  @Test
  public void reservedWordsAreTheKeywordsOfTheGrammar() {
    // A keyword of the lexer that looks like a simple symbol is not a symbol of the grammar, except
    // for the predefined symbols.
    Vocabulary vocabulary = SvLibLexer.VOCABULARY;
    ImmutableSet.Builder<String> keywords = ImmutableSet.builder();
    for (int token = 1; token <= vocabulary.getMaxTokenType(); token++) {
      String literal = vocabulary.getLiteralName(token);
      String symbolicName = vocabulary.getSymbolicName(token);
      if (literal == null || (symbolicName != null && symbolicName.startsWith("PS_"))) {
        continue;
      }
      String word = literal.substring(1, literal.length() - 1);
      if (SmtLibSymbols.isSimpleSymbol(word)) {
        keywords.add(word);
      }
    }
    assertThat(SmtLibSymbols.RESERVED_WORDS).containsExactlyElementsIn(keywords.build());
  }

  @Test
  public void quoteOnlyWhereNeeded() {
    assertThat(SmtLibSymbols.quoteIfNeeded("x")).isEqualTo("x");
    assertThat(SmtLibSymbols.quoteIfNeeded("main::x")).isEqualTo("|main::x|");
    assertThat(SmtLibSymbols.quoteIfNeeded("*(int)[10]")).isEqualTo("|*(int)[10]|");
    assertThat(SmtLibSymbols.quoteIfNeeded("1x")).isEqualTo("|1x|");
    assertThat(SmtLibSymbols.quoteIfNeeded("label")).isEqualTo("|label|");
  }
}
