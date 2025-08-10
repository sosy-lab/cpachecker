// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Modular transfer relation for the Pointer Analysis CPA.
 *
 * <p>This package contains a decomposed implementation of the pointer-analysis transfer relation.
 *
 * <h2>Key classes</h2>
 *
 * <ul>
 *   <li>{@link org.sosy_lab.cpachecker.cpa.pointer.transfer.PointerAnalysisTransferRelation}: the
 *       entry point used by the CPA framework. For each CFA edge, it dispatches to a handler
 *       produced by {@link
 *       org.sosy_lab.cpachecker.cpa.pointer.transfer.TransferRelationEdgeHandlerFactory} and
 *       converts {@code BOTTOM_STATE} into an empty successor set.
 *   <li>{@link org.sosy_lab.cpachecker.cpa.pointer.transfer.TransferRelationEdgeHandlerFactory}: a
 *       switch-based factory that returns the appropriate handler for the current edge kind.
 *   <li>{@link org.sosy_lab.cpachecker.cpa.pointer.transfer.TransferRelationEdgeHandler}: a sealed
 *       interface implemented by the concrete handlers: {@link
 *       org.sosy_lab.cpachecker.cpa.pointer.transfer.AssumeEdgeHandler}, {@link
 *       org.sosy_lab.cpachecker.cpa.pointer.transfer.BlankEdgeHandler}, {@link
 *       org.sosy_lab.cpachecker.cpa.pointer.transfer.CallToReturnEdgeHandler}, {@link
 *       org.sosy_lab.cpachecker.cpa.pointer.transfer.DeclarationEdgeHandler}, {@link
 *       org.sosy_lab.cpachecker.cpa.pointer.transfer.FunctionCallEdgeHandler}, {@link
 *       org.sosy_lab.cpachecker.cpa.pointer.transfer.FunctionReturnEdgeHandler}, {@link
 *       org.sosy_lab.cpachecker.cpa.pointer.transfer.ReturnStatementEdgeHandler}, {@link
 *       org.sosy_lab.cpachecker.cpa.pointer.transfer.StatementEdgeHandler}.
 *   <li>{@link org.sosy_lab.cpachecker.cpa.pointer.transfer.PointerTransferOptions} and {@link
 *       org.sosy_lab.cpachecker.cpa.pointer.transfer.HeapAllocationStrategy}: configuration for
 *       heap-site modeling, struct/union handling strategy, and offset-sensitivity.
 * </ul>
 *
 * <h2>Design and semantics</h2>
 *
 * <ul>
 *   <li><b>Single-responsibility handlers:</b> each handler implements the transfer for one edge
 *       category (e.g., declarations, statements, assumptions, function call/return, etc.).
 *   <li><b>State flow:</b> analysis state is never mutated in-place. Handlers take a {@code
 *       PointerAnalysisState} and return a new state (or the same instance) according to the
 *       lattice operations used by the pointer analysis.
 *   <li><b>Referenced locations:</b> expression-to-points-to resolution (dereference, address-of,
 *       field/array access, pointer arithmetic, string literals, and casts) is handled centrally by
 *       {@code utils.ReferenceLocationsResolver} to keep handlers small and consistent.
 *   <li><b>Heap modeling:</b> under {@link
 *       org.sosy_lab.cpachecker.cpa.pointer.transfer.HeapAllocationStrategy#PER_CALL}, unique heap
 *       sites are generated via a per-analysis counter owned by {@link
 *       org.sosy_lab.cpachecker.cpa.pointer.transfer.PointerAnalysisTransferRelation} and passed
 *       into {@link org.sosy_lab.cpachecker.cpa.pointer.transfer.StatementEdgeHandler}. The handler
 *       consumes indices (post-increment semantics) to ensure monotonic, deterministic labeling
 *       across edges. {@code SINGLE} and {@code PER_LINE} behave as specified by the corresponding
 *       factory methods on {@code HeapLocation}.
 *   <li><b>Invalid/exceptional cases:</b> unsupported constructs (e.g., address-of-label) raise a
 *       {@code CPATransferException}. Dereferencing NULL yields an {@code
 *       InvalidLocation(NULL_DEREFERENCE)}; freeing non-heap targets emits a warning and leaves the
 *       map consistent.
 * </ul>
 *
 * <h2>Thread-safety and determinism</h2>
 *
 * <h2>Extensibility</h2>
 *
 * <ul>
 *   <li>To add a new edge kind, implement {@link
 *       org.sosy_lab.cpachecker.cpa.pointer.transfer.TransferRelationEdgeHandler} for the new
 *       {@code CFAEdge} subtype and extend the switch in {@link
 *       org.sosy_lab.cpachecker.cpa.pointer.transfer.TransferRelationEdgeHandlerFactory}.
 *   <li>Handlers should remain free of global mutable state. If shared, per-analysis state is
 *       needed, keep ownership in {@code PointerAnalysisTransferRelation} and pass narrowly scoped
 *       holders (as with the heap counter) to the handler constructor.
 * </ul>
 */
@javax.annotation.ParametersAreNonnullByDefault
@org.sosy_lab.common.annotations.FieldsAreNonnullByDefault
@org.sosy_lab.common.annotations.ReturnValuesAreNonnullByDefault
package org.sosy_lab.cpachecker.cpa.pointer.transfer;
