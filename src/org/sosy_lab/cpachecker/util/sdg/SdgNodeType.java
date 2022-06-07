// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.sdg;

/**
 * Type of a system dependence graph node.
 *
 * <p>C program example to show the different node types (the resulting nodes depend on the used
 * construction method, so real result may differ):
 *
 * <pre>
 * int global = 1;
 *
 * int foo(int p) {
 *   global += p;
 *   return global;
 * }
 *
 * int main() {
 *   int x = 2;
 *   int y = foo(x);
 * }
 * </pre>
 *
 * <table>
 *   <tr>
 *     <th>Type</th>
 *     <th>Procedure</th>
 *     <th>Statement</th>
 *     <th>Variable</th>
 *   </tr>
 *   <tr>
 *   <tr>
 *     <th>{@code STATEMENT}</th>
 *     <th>-</th>
 *     <th>{@code int global = 0;}</th>
 *     <th>-</th>
 *   </tr>
 *   <tr>
 *     <th>{@code ENTRY}</th>
 *     <th>{@code int foo(int)}</th>
 *     <th>-</th>
 *     <th>-</th>
 *   </tr>
 *   <tr>
 *     <th>{@code FORMAL_IN}</th>
 *     <th>{@code int foo(int)}</th>
 *     <th>-</th>
 *     <th>{@code foo::p}</th>
 *   </tr>
 *    <tr>
 *     <th>{@code FORMAL_IN}</th>
 *     <th>{@code int foo(int)}</th>
 *     <th>-</th>
 *     <th>{@code global}</th>
 *   </tr>
 *    <tr>
 *     <th>{@code STATEMENT}</th>
 *     <th>{@code int foo(int)}</th>
 *     <th>{@code global += p;}</th>
 *     <th>-</th>
 *   </tr>
 *    <tr>
 *     <th>{@code STATEMENT}</th>
 *     <th>{@code int foo(int)}</th>
 *     <th>{@code return global;}</th>
 *     <th>-</th>
 *   </tr>
 *   <tr>
 *     <th>{@code FORMAL_OUT}</th>
 *     <th>{@code int foo(int)}</th>
 *     <th>-</th>
 *     <th>{@code foo::__retval__}</th>
 *   </tr>
 *    <tr>
 *     <th>{@code FORMAL_OUT}</th>
 *     <th>{@code int foo(int)}</th>
 *     <th>-</th>
 *     <th>{@code global}</th>
 *   </tr>
 *   <tr>
 *     <th>{@code ENTRY}</th>
 *     <th>{@code int main()}</th>
 *     <th>-</th>
 *     <th>-</th>
 *   </tr>
 *   <tr>
 *     <th>{@code STATEMENT}</th>
 *     <th>{@code int main()}</th>
 *     <th>{@code int x = 5;}</th>
 *     <th>-</th>
 *   </tr>
 *   <tr>
 *     <th>{@code STATEMENT}</th>
 *     <th>{@code int main()}</th>
 *     <th>{@code int y = foo(x);}</th>
 *     <th>-</th>
 *   </tr>
 *   <tr>
 *     <th>{@code ACTUAL_IN}</th>
 *     <th>{@code int main()}</th>
 *     <th>{@code int y = foo(x);}</th>
 *     <th>{@code foo::p}</th>
 *   </tr>
 *   <tr>
 *     <th>{@code ACTUAL_IN}</th>
 *     <th>{@code int main()}</th>
 *     <th>{@code int y = foo(x);}</th>
 *     <th>{@code global}</th>
 *   </tr>
 *   <tr>
 *     <th>{@code ACTUAL_OUT}</th>
 *     <th>{@code int main()}</th>
 *     <th>{@code int y = foo(x);}</th>
 *     <th>{@code foo::__retval__}</th>
 *   </tr>
 *   <tr>
 *     <th>{@code ACTUAL_OUT}</th>
 *     <th>{@code int main()}</th>
 *     <th>{@code int y = foo(x);}</th>
 *     <th>{@code global}</th>
 *   </tr>
 *   <tr>
 *     <th>{@code FORMAL_OUT}</th>
 *     <th>{@code int main()}</th>
 *     <th>-</th>
 *     <th>{@code main::__retval__}</th>
 *   </tr>
 * </table>
 */
public enum SdgNodeType {

  /**
   * Type of procedure entry nodes.
   *
   * <p>Only one node with this type should exist per procedure. Nodes of the procedure should be
   * directly or indirectly control dependent on the entry node.
   *
   * <ul>
   *   <li>Procedure: required
   *   <li>Statement: empty
   *   <li>Variable: empty
   * </ul>
   */
  ENTRY,

  /**
   * Type of regular statement, expression, and declaration nodes.
   *
   * <ul>
   *   <li>Procedure: optional (*)
   *   <li>Statement: required
   *   <li>Variable: empty
   * </ul>
   *
   * (*) If the system dependence graph only contains statements from a single procedure and no
   * other non-statement nodes, the procedure can be omitted (this can be used to represent a
   * program dependence graph (PDG)). Otherwise, the procedure is required.
   */
  STATEMENT,

  /**
   * Type of nodes that represent variables that are visible to or defined by some procedure caller
   * and used inside the procedure.
   *
   * <p>This is the case for e.g. parameters and used global variables.
   *
   * <ul>
   *   <li>Procedure: required
   *   <li>Statement: empty
   *   <li>Variable: required
   * </ul>
   */
  FORMAL_IN,

  /**
   * Type of nodes that represent variables that are visible to some procedure caller and defined
   * inside the procedure.
   *
   * <p>This is the case for e.g. return values and modified global variables.
   *
   * <ul>
   *   <li>Procedure: required
   *   <li>Statement: empty
   *   <li>Variable: required
   * </ul>
   */
  FORMAL_OUT,

  /**
   * Type of nodes that represent variables at a specific call sites and are connected to {@code
   * FORMAL_IN} nodes.
   *
   * <ul>
   *   <li>Procedure: required
   *   <li>Statement: required
   *   <li>Variable: required
   * </ul>
   */
  ACTUAL_IN,

  /**
   * Type of nodes that represent variables at a specific call sites and are connected to {@code
   * FORMAL_OUT} nodes.
   *
   * <ul>
   *   <li>Procedure: required
   *   <li>Statement: required
   *   <li>Variable: required
   * </ul>
   */
  ACTUAL_OUT;
}
