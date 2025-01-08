// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2021 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.smg.graph;

/** Specifier for Points-to-Edges (pointers). */
public enum SMGTargetSpecifier {
  /**
   * This specifier symbolises the Points-to-Edge towards a concrete memory region. The
   * nesting-level is equal to the target object.
   */
  IS_REGION,
  /**
   * This specifier symbolises the Points-to-Edge towards the FIRST list element of an abstracted
   * linked-list. The nesting-level is equal to the target object.
   */
  IS_FIRST_POINTER,
  /**
   * This specifier symbolises the Points-to-Edge towards the LAST list element of an abstracted
   * linked-list. The nesting-level is equal to the target object.
   */
  IS_LAST_POINTER,
  /**
   * This specifier symbolises abstracted Points-to-Edges towards abstracted memory. These PTEs are
   * materialized such that there is a PTE towards ALL list segments. Nesting-Level of the
   * Points-to-Edge P is 1 smaller than the target object o(P(a)) Page 8,
   * 10.1007/978-3-642-38856-9_13: "On the other hand, addresses with the all target go up one level
   * in the nesting hierarchy, i.e., ∀a ∈ A : tg(P(a)) == all ⇒ level(a) == level(o(P(a))) + 1."
   */
  IS_ALL_POINTER
}
