// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cfa.parser.eclipse.c;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rewrites the atomic type specifier {@code _Atomic(type-name)} (C23 § 6.7.3.5 and footnote 147,
 * #1667) so that it is macro-expanded separately from the {@code _Atomic} qualifier, which {@link
 * EclipseCdtWrapper} already substitutes for an attribute (#1253). A single macro name cannot mean
 * both "substitute unconditionally" (the qualifier) and "substitute only when followed by {@code
 * (}" (the specifier), so the specifier form is renamed to {@link
 * EclipseCdtWrapper#ATOMIC_SPECIFIER_MACRO}, a distinct macro that expands the type-name via {@code
 * __typeof__} instead of trying to classify it here. That lets CDT's own preprocessor and type
 * system resolve the type-name (plain, pointer, function-pointer, or hidden behind another macro)
 * rather than a hand-rolled scanner.
 *
 * <p>Occurrences inside comments or string/character literals are not excluded; renaming text there
 * is harmless since it does not change where those spans begin or end.
 */
final class AtomicTypeSpecifierRewriter {

  private static final Pattern ATOMIC_SPECIFIER = Pattern.compile("\\b_Atomic\\b(?=\\s*\\()");

  private static final String REPLACEMENT =
      Matcher.quoteReplacement(EclipseCdtWrapper.ATOMIC_SPECIFIER_MACRO);

  private AtomicTypeSpecifierRewriter() {}

  /** Rewrite all atomic type specifiers in the given code as described in the class javadoc. */
  static String rewrite(final String pCode) {
    return ATOMIC_SPECIFIER.matcher(pCode).replaceAll(REPLACEMENT);
  }
}
