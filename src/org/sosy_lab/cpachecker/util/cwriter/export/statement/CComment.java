// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.util.cwriter.export.statement;

import org.sosy_lab.cpachecker.cfa.ast.AAstNode.AAstNodeRepresentation;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

/**
 * Represents a single-line C comment. Example:
 *
 * <pre>{@code // this is a comment}</pre>
 *
 * <p>A comment is not a statement in the C standard, but still added separately here so that it is
 * not placed inside {@link CExportStatement} (e.g. via an {@code abstract class} and a base
 * attribute that defines a list of {@link CComment}) so that the classes that implement {@link
 * CExportStatement} can be {@code record}, and do not have to be {@code class}.
 */
public record CComment(String comment) implements CExportStatement {

  @Override
  public String toASTString(AAstNodeRepresentation pAAstNodeRepresentation)
      throws UnrecognizedCodeException {

    return "// " + comment;
  }
}
