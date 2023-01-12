// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * The classes that are used to represent single program statements, declarations, and expressions
 * in form of an abstract syntax tree (AST). Sub-packages contain language-specific sub-classes for
 * representation of features of specific languages.
 *
 * <p>The classes in this package have an "A" as prefix to show that they are language-independent,
 * in contrast to the language-specific classes with prefixes like "C" and "J". All classes in this
 * package named "Abstract*" are only relevant for implementing the language-specific sub-classes
 * and should not be used by other code.
 */
package org.sosy_lab.cpachecker.cfa.ast;
