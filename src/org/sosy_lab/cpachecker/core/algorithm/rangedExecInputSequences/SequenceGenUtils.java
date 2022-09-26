// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2022 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.core.algorithm.rangedExecInputSequences;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.io.IO;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.AssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cpa.arg.ARGState;
import org.sosy_lab.cpachecker.cpa.arg.path.ARGPath;
import org.sosy_lab.cpachecker.cpa.arg.path.PathIterator;
import org.sosy_lab.cpachecker.cpa.predicate.PredicateAbstractState;
import org.sosy_lab.cpachecker.exceptions.CPAException;
import org.sosy_lab.cpachecker.util.AbstractStates;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormula;
import org.sosy_lab.java_smt.api.Formula;
import org.sosy_lab.java_smt.api.Model;
import org.sosy_lab.java_smt.api.SolverException;

public class SequenceGenUtils {
  private static final Integer DEFAULT_INT = 0;
  private static final CharSequence DELIMITER =System.lineSeparator() ;
  private ImmutableSet<String> namesOfRandomFunctions;
  private LogManager logger;

  public SequenceGenUtils(ImmutableSet<String> pNamesOfRandomFunctions, LogManager pLogger) {

    namesOfRandomFunctions = pNamesOfRandomFunctions;
    logger = pLogger;
  }
  public List<Boolean> computeSequenceForLoopbound(ARGPath pARGPath)
      throws InterruptedException, SolverException {

    // Check, if the given path is sat by conjoining the path formulae of the abstraction locations.
    // If not, cut off the last part and recursively continue.
    List<Boolean> decisionNodesTaken = new ArrayList<>();

    for (CFAEdge edge : pARGPath.getFullPath()) {
      if (edge instanceof AssumeEdge) {
        AssumeEdge assumeEdge = (AssumeEdge) edge;
        if (assumeEdge.getTruthAssumption()) {
          if (assumeEdge.isSwapped()) {
            decisionNodesTaken.add(false);
          } else {

            decisionNodesTaken.add(true);
          }
        } else {
          if (assumeEdge.isSwapped()) {
            decisionNodesTaken.add(true);
          } else {

            decisionNodesTaken.add(false);
          }
        }
      }
}
    logger.log(Level.INFO, decisionNodesTaken);
    return decisionNodesTaken;
  }

  public void printFileToOutput(List<Boolean> pInputs, Path testcaseName)
      throws IOException {

    logger.logf(Level.INFO, "Storing the testcase at %s", testcaseName.toAbsolutePath().toString());
    List<String> content = new ArrayList<>();

    content.add(String.join(DELIMITER, pInputs.stream().map(b -> b.toString()).collect(ImmutableList.toImmutableList())));
    IO.writeFile(testcaseName, Charset.defaultCharset(), Joiner.on("\n").join(content));
  }

  private boolean isRandomFctCall(CFunctionCallExpression pRightHandSide) {
    String name = pRightHandSide.getDeclaration().getName();
    return this.namesOfRandomFunctions.stream().anyMatch(s -> name.contains(s));
  }

  private boolean leftIsVar(CLeftHandSide pLeftHandSide) {
    return pLeftHandSide instanceof CIdExpression;
  }
}
