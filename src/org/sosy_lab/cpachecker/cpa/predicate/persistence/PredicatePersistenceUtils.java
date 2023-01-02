// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.predicate.persistence;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Verify.verify;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.AbstractionPredicate;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

public class PredicatePersistenceUtils {

  public enum PredicateDumpFormat {
    PLAIN,
    SMTLIB2
  }

  public static final Splitter LINE_SPLITTER = Splitter.on('\n').omitEmptyStrings();
  public static final Joiner LINE_JOINER = Joiner.on('\n');

  public static class PredicateParsingFailedException extends CPAException {
    private static final long serialVersionUID = 5034288100943314517L;

    public PredicateParsingFailedException(String msg, String source, int lineNo) {
      super("Parsing failed in line " + lineNo + " of " + source + ": " + msg);
    }

    public PredicateParsingFailedException(Throwable cause, String source, int lineNo) {
      this(cause.getMessage(), source, lineNo);
      initCause(cause);
    }
  }

  public static Pair<String, List<String>> splitFormula(FormulaManagerView fmgr, BooleanFormula f) {
    String out = fmgr.dumpFormula(f).toString();

    int splitIdx = out.indexOf("(assert");
    verify(splitIdx >= 0, "Unexpected formula format without '(assert': %s", out);
    String declarationsString = out.substring(0, splitIdx);
    String formulaString = out.substring(splitIdx).replace("\n", "");

    List<String> declarations;

    if (formulaString.isEmpty()) {
      if (fmgr.getBooleanFormulaManager().isTrue(f)) {
        declarations = ImmutableList.of();
        formulaString = "(assert true)";
      } else {
        throw new AssertionError();
      }
    } else {
      declarations = LINE_SPLITTER.splitToList(declarationsString);
    }

    assert formulaString.startsWith("(assert ") && formulaString.endsWith(")")
        : "Unexpected formula format: " + formulaString;

    return Pair.of(formulaString, declarations);
  }

  static void writeSetOfPredicates(
      Appendable sb,
      String key,
      Collection<AbstractionPredicate> predicates,
      Map<AbstractionPredicate, String> predToString)
      throws IOException {
    if (!predicates.isEmpty()) {
      sb.append(key);
      sb.append(":\n");
      for (AbstractionPredicate pred : predicates) {
        sb.append(checkNotNull(predToString.get(pred)));
        sb.append('\n');
      }
      sb.append('\n');
    }
  }

  static Pair<Integer, String> parseCommonDefinitions(
      BufferedReader reader, String sourceIdentifier)
      throws PredicateParsingFailedException, IOException {
    // first, read first section with initial set of function definitions
    StringBuilder functionDefinitionsBuffer = new StringBuilder();

    int lineNo = 0;
    String currentLine;
    while ((currentLine = reader.readLine()) != null) {
      currentLine = currentLine.trim();
      lineNo++;

      if (currentLine.isEmpty()) {
        break;
      }

      if (currentLine.startsWith("//")) {
        // comment
        continue;
      }

      if (currentLine.startsWith("(") && currentLine.endsWith(")")) {
        functionDefinitionsBuffer.append(currentLine);
        functionDefinitionsBuffer.append('\n');

      } else {
        throw new PredicateParsingFailedException(
            currentLine + " is not a valid SMTLIB2 definition", sourceIdentifier, lineNo);
      }
    }

    return Pair.of(lineNo, functionDefinitionsBuffer.toString());
  }
}
