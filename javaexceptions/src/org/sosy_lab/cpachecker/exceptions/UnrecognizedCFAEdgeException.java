// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.exceptions;

import org.sosy_lab.cpachecker.cfa.model.CFAEdge;

/** Exception thrown if a CPA cannot handle a specific CFAEdge. */
public class UnrecognizedCFAEdgeException extends CPATransferException {

  public UnrecognizedCFAEdgeException(CFAEdge edge) {
    super(createMessage(edge));
  }

  private static String createMessage(CFAEdge edge) {
    return "Unknown CFA edge: " + edge.getEdgeType() + " (" + edge.getDescription() + ")";
  }

  /** auto-generated UID */
  private static final long serialVersionUID = -5106215499745787051L;
}
