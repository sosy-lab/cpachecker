// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.svlib.ast;

import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 * The layout of a part of an SV-LIB script as an S-expression.
 *
 * <p>A list is written on a single line if that line does not exceed {@link #LINE_WIDTH}
 * characters. Otherwise, each of its elements is written on a line of its own: after a keyword as
 * in {@code (sequence ...)} indented by two more characters, and in a list of data as in {@code ((x
 * Int) (y Int))} aligned with its first element.
 */
public sealed interface SvLibSExpression {

  int LINE_WIDTH = 100;

  /** The number of characters of this expression on a single line. */
  int length();

  /** A part of the script that is always written on a single line, such as a term. */
  record Atom(String text) implements SvLibSExpression {
    @Override
    public int length() {
      return text.length();
    }
  }

  /** A list in parentheses. */
  record SList(ImmutableList<SvLibSExpression> elements, int length) implements SvLibSExpression {}

  static SvLibSExpression atom(String pText) {
    return new Atom(pText);
  }

  static SvLibSExpression list(List<SvLibSExpression> pElements) {
    int length = 2 + Math.max(0, pElements.size() - 1);
    for (SvLibSExpression element : pElements) {
      length += element.length();
    }
    return new SList(ImmutableList.copyOf(pElements), length);
  }

  static SvLibSExpression list(SvLibSExpression... pElements) {
    return list(ImmutableList.copyOf(pElements));
  }

  /** The expression on a single line. */
  default String toSingleLine() {
    StringBuilder builder = new StringBuilder(length());
    appendSingleLine(this, builder);
    return builder.toString();
  }

  /** The expression as it is written when it begins in the first column. */
  default String format() {
    StringBuilder builder = new StringBuilder(length());
    append(this, 0, builder);
    return builder.toString();
  }

  private static void appendSingleLine(SvLibSExpression pExpression, StringBuilder pBuilder) {
    switch (pExpression) {
      case Atom atom -> pBuilder.append(atom.text());
      case SList list -> {
        pBuilder.append('(');
        for (int i = 0; i < list.elements().size(); i++) {
          if (i > 0) {
            pBuilder.append(' ');
          }
          appendSingleLine(list.elements().get(i), pBuilder);
        }
        pBuilder.append(')');
      }
    }
  }

  private static void append(SvLibSExpression pExpression, int pColumn, StringBuilder pBuilder) {
    if (!(pExpression instanceof SList list)
        || pColumn + list.length() <= LINE_WIDTH
        || list.elements().isEmpty()) {
      appendSingleLine(pExpression, pBuilder);
      return;
    }
    SvLibSExpression first = list.elements().getFirst();
    if (list.elements().size() == 1) {
      pBuilder.append('(');
      append(first, pColumn + 1, pBuilder);
      pBuilder.append(')');
      return;
    }
    int columnOfElements = first instanceof Atom ? pColumn + 2 : pColumn + 1;
    pBuilder.append('(');
    append(first, pColumn + 1, pBuilder);
    for (SvLibSExpression element : list.elements().subList(1, list.elements().size())) {
      pBuilder.append('\n').append(" ".repeat(columnOfElements));
      append(element, columnOfElements, pBuilder);
    }
    pBuilder.append(')');
  }
}
