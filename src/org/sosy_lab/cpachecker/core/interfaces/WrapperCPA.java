// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.interfaces;

import org.checkerframework.checker.nullness.qual.Nullable;

/** Interface for classes that are wrapping CPAs (like composite CPAs) */
public interface WrapperCPA {

  /**
   * Retrieve one of the wrapped CPAs by type. If the hierarchy of (wrapped) CPAs has several
   * levels, this method searches through them recursively.
   *
   * <p>The type does not need to match exactly, the returned element has just to be a subtype of
   * the type passed as argument.
   *
   * @param <T> The type of the wrapped element.
   * @param type The class object of the type of the wrapped element.
   * @return An instance of an element with type T or null if there is none.
   */
  @Nullable <T extends ConfigurableProgramAnalysis> T retrieveWrappedCpa(Class<T> type);

  /**
   * Retrieve all wrapped CPAs contained directly in this object (not recursively).
   *
   * @return A non-empty unmodifiable list of CPAs.
   */
  Iterable<ConfigurableProgramAnalysis> getWrappedCPAs();
}
