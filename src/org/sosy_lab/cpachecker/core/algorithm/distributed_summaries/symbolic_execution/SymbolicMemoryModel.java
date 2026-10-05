// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.distributed_summaries.symbolic_execution;

import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerBase;
import org.sosy_lab.cpachecker.util.predicates.pathformula.pointeraliasing.PointerTargetSet;

/**
 * The memory of the program as all blocks share it.
 *
 * <p>The pointer-aliasing encoding decides along a path whether a variable lives in memory, by the
 * objects in memory, so-called bases, that the path declared so far. A block that is encoded on its
 * own does not know the bases that earlier blocks declared, and two blocks would then encode the
 * same variable differently. All blocks therefore start from the bases of the whole program, which
 * are collected once by encoding every edge that declares one. This requires the eager encoding,
 * i.e., {@code cpa.predicate.useConstraintOptimization=false}, under which a declaration makes a
 * variable a base right away and not only once its address is taken.
 *
 * <p>The addresses of these bases are constants. The constraints that place them in memory without
 * overlap are emitted once, here, and hold in every query as axioms.
 *
 * <p>Objects that the program allocates at runtime get a fresh address on every execution of the
 * block that allocates them: the address is existentially quantified like a nondeterministic value.
 * So that allocations of different blocks never overlap, the program state carries a ghost
 * variable, {@link #HEAP_TOP}, that bounds all addresses allocated so far. Every allocation lies
 * above its value at the block entry, and a block that allocates raises it at its exit.
 *
 * @param staticBases the bases of all variables of the program; no allocation, and no highest
 *     allocated address, because that is a formula of the solver context that collected them
 * @param staticBaseNames the names of {@code staticBases}, to recognize bases a block introduces
 * @param axioms the constraints that place the static bases in memory, in SMT-LIB
 * @param initialStates the condition on {@link #HEAP_TOP} at the program entry, in interface form
 *     and SMT-LIB
 */
record SymbolicMemoryModel(
    PointerTargetSet staticBases,
    ImmutableSet<PointerBase> staticBaseNames,
    String axioms,
    String initialStates) {

  /** The ghost variable that bounds all addresses allocated so far. */
  static final String HEAP_TOP = "__sbe_heap_top";
}
