// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.sdg;

/** Type for system dependence graph edges. */
public enum SdgEdgeType {

  /**
   * Type for flow dependencies.
   *
   * <p>Edges with this type should always be intra-procedural.
   */
  FLOW_DEPENDENCY,

  /**
   * Type for control dependencies.
   *
   * <p>Edges with this type should always be intra-procedural.
   */
  CONTROL_DEPENDENCY,

  /**
   * Type for declaration dependency edges.
   *
   * <p>Edges with this type can be intra-procedural or inter-procedural.
   */
  DECLARATION_EDGE,

  /**
   * Type for procedure call edges.
   *
   * <p>Edges with this type should always be inter-procedural.
   */
  CALL_EDGE,

  /**
   * Type for parameter edges from actual-in to formal-in or from formal-out to actual-out nodes.
   *
   * <p>Edges with this type should always be inter-procedural.
   */
  PARAMETER_EDGE,

  /**
   * Type for summary edges from actual-in to actual-out nodes.
   *
   * <p>Edges with this type should always be intra-procedural.
   */
  SUMMARY_EDGE;
}
