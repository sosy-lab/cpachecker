// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Abstract memory locations used by the pointer analysis.
 *
 * <p>This package defines the sealed hierarchy of {@code PointerLocation}s—immutable identifiers
 * for abstract memory objects such as declared variables, heap allocations, and scoped struct/union
 * elements. Special locations capture null and invalidated targets (with an explicit reason),
 * enabling precise modeling of dereference failures and lifetime issues.
 *
 * <p>Locations provide:
 *
 * <ul>
 *   <li><b>Value semantics</b>: equality/hash based on the underlying identity (e.g., variable,
 *       allocation, or aggregate scope), suitable for use as keys in persistent maps and for
 *       membership in explicit location sets.
 *   <li><b>Total ordering</b>: a deterministic {@code Comparable} implementation with a
 *       well-defined cross-type fallback, ensuring stable iteration and canonicalization.
 *   <li><b>Scope awareness</b>: struct/union modeling distinguishes type-, instance-, and
 *       field-level scopes and records optional function scope; helper predicates indicate whether
 *       a location remains valid across function boundaries.
 * </ul>
 *
 * <p>These abstractions are consumed by the pointer-analysis state and transfer relation to
 * represent points-to targets and to reason about dereferencing, invalidation, and aggregate
 * accesses in a flow-sensitive manner.
 */
@javax.annotation.ParametersAreNonnullByDefault
@org.sosy_lab.common.annotations.FieldsAreNonnullByDefault
@org.sosy_lab.common.annotations.ReturnValuesAreNonnullByDefault
package org.sosy_lab.cpachecker.cpa.pointer.location;
