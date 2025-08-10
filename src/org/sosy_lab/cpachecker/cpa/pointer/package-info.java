// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Pointer analysis CPA: the entry point and integration layer for pointer-aware verification.
 *
 * <p>This package provides the high-level components that integrate pointer analysis into the CPA
 * framework. It defines:
 *
 * <ul>
 *   <li>{@code PointerAnalysisCPA} – CPA integration, configuration wiring, and factory methods.
 *   <li>{@code PointerAnalysisState} – immutable lattice state mapping {@code PointerLocation}s to
 *       {@code LocationSet}s (absence denotes ⊤);
 *   <li>{@code PointerAnalysisTransferRelation} – flow-sensitive edge semantics covering pointer
 *       assignments, dereferencing, function calls/returns, and (via options) offset sensitivity
 *       and heap-allocation strategies.
 *   <li>{@code PointerAnalysisTransferRelation.PointerTransferOptions} – runtime options (e.g.,
 *       {@code HeapAllocationStrategy} and offset sensitivity) injected from configuration.
 *   <li>{@code StructHandlingStrategy} – strategy enum describing how structs/unions are modeled at
 *       the level of pointer locations (type scope vs. instance scope vs. per-field).
 * </ul>
 */
@javax.annotation.ParametersAreNonnullByDefault
@org.sosy_lab.common.annotations.FieldsAreNonnullByDefault
@org.sosy_lab.common.annotations.ReturnValuesAreNonnullByDefault
package org.sosy_lab.cpachecker.cpa.pointer;
