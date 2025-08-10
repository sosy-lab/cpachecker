// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Abstract sets of pointer targets (points-to sets) used by the pointer analysis.
 *
 * <p>This package provides an immutable lattice for sets of {@code PointerLocation}s: bottom (⊥),
 * finite explicit sets, and top (⊤). It is the central abstraction used by the transfer relation
 * and location to represent where a symbolic pointer may point.
 *
 * <h2>Design and semantics</h2>
 *
 * <ul>
 *   <li><b>Lattice elements:</b> LocationSetBot encodes “no targets / bottom”, explicit sets encode
 *       a precise finite set of targets, and LocationSetTop encodes “any target / top”.
 *   <li><b>Immutability &amp; thread-safety:</b> all implementations are strictly immutable and
 *       safe for concurrent use. Methods never mutate existing instances.
 *   <li><b>Determinism:</b> explicit sets maintain a total order over {@code PointerLocation}s,
 *       ensuring stable iteration and reproducible output.
 * </ul>
 */
@javax.annotation.ParametersAreNonnullByDefault
@org.sosy_lab.common.annotations.FieldsAreNonnullByDefault
@org.sosy_lab.common.annotations.ReturnValuesAreNonnullByDefault
package org.sosy_lab.cpachecker.cpa.pointer.locationset;
