// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.tube;

import com.google.common.collect.ImmutableMap;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.CParser.Factory;
import org.sosy_lab.cpachecker.cfa.CProgramScope;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.core.AnalysisDirection;
import org.sosy_lab.cpachecker.core.interfaces.AbstractQueryableState;
import org.sosy_lab.cpachecker.core.interfaces.FormulaReportingState;
import org.sosy_lab.cpachecker.core.interfaces.Graphable;
import org.sosy_lab.cpachecker.core.interfaces.Partitionable;
import org.sosy_lab.cpachecker.cpa.automaton.InvalidAutomatonException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CParserUtils;
import org.sosy_lab.cpachecker.util.CParserUtils.ParserTools;
import org.sosy_lab.cpachecker.util.expressions.ExpressionTrees;
import org.sosy_lab.cpachecker.util.expressions.ToFormulaVisitor;
import org.sosy_lab.cpachecker.util.expressions.ToFormulaVisitor.ToFormulaException;
import org.sosy_lab.cpachecker.util.predicates.pathformula.PathFormulaManagerImpl;
import org.sosy_lab.cpachecker.util.predicates.pathformula.ctoformula.CtoFormulaConverter;
import org.sosy_lab.cpachecker.util.predicates.smt.FormulaManagerView;
import org.sosy_lab.java_smt.api.BooleanFormula;

/** Represents a TubeState object that contains information about the state of a TubeCPA. */
public class TubeState
    implements AbstractQueryableState,
        Partitionable,
        Serializable,
        Graphable,
        FormulaReportingState {
  /** Immutable map containing integer keys and string values representing asserts. */
  private final ImmutableMap<Integer, String> asserts;

  /**
   * Retrieves the LogManager associated with this TubeState object.
   *
   * @return The LogManager instance associated with this TubeState.
   */
  public LogManager getLogManager() {
    return logManager;
  }

  /**
   * Represents a LogManager instance that provides logging functionality for the TubeState class.
   */
  private final LogManager logManager;

  /** Flag indicating whether the variable is negated. */
  private final boolean isNegated;

  /** Represents the boolean expression associated with a TubeState object. */
  private final String booleanExp;

  /** Represents a Control Flow Automata (CFA) edge associated with a TubeState object. */
  private final CFAEdge cfaEdge;

  /** Represents a counter for tracking the number of errors encountered. */
  private int errorCounter;

  /**
   * Represents a Control Flow Automaton associated with a TubeState object. This class provides
   * access to various methods for handling the CFA, such as getting machine model, number of
   * functions, function names, loop structure, variable classification, etc.
   */
  private final CFA cfa;

  /**
   * Retrieves the supplier function used to obtain a {@link CtoFormulaConverter} based on the
   * provided {@link FormulaManagerView}.
   *
   * @return The supplier function used to obtain a {@link CtoFormulaConverter}.
   */
  public Function<FormulaManagerView, CtoFormulaConverter> getSupplier() {
    return supplier;
  }

  /** Represents a supplier of CtoFormulaConverter instances based on a given FormulaManagerView. */
  private final Function<FormulaManagerView, CtoFormulaConverter> supplier;

  /**
   * Constructs a TubeState object with the provided parameters.
   *
   * @param pCFAEdge The CFAEdge associated with the TubeState.
   * @param pAssert A mapping of integers to strings representing asserts.
   * @param exp The boolean expression for the TubeState.
   * @param pIsNegated A boolean indicating whether the TubeState is negated.
   * @param pError_counter The error counter value for the TubeState.
   * @param pSupplier The function supplier for FormulaManagerView.
   * @param pLogManager The log manager for the TubeState.
   * @param pCfa The CFA associated with the TubeState.
   */
  public TubeState(
      CFAEdge pCFAEdge,
      ImmutableMap<Integer, String> pAssert,
      String exp,
      boolean pIsNegated,
      int pError_counter,
      Function<FormulaManagerView, CtoFormulaConverter> pSupplier,
      LogManager pLogManager,
      CFA pCfa) {
    this.cfaEdge = pCFAEdge;
    this.asserts = pAssert;
    this.booleanExp = exp;
    this.errorCounter = pError_counter;
    this.isNegated = pIsNegated;
    this.supplier = pSupplier;
    this.logManager = pLogManager;
    this.cfa = pCfa;
  }

  /**
   * Retrieves the assertion string at the specified line number in the TubeState object.
   *
   * @param lineNumber The line number where the assertion is located.
   * @param negate True if the assertion should be negated, false otherwise.
   * @return The assertion string at the specified line number. If negate is true, the negated form
   *     of the assertion is returned.
   */
  public String getAssertAtLine(int lineNumber, boolean negate) {
    String f = this.asserts.get(lineNumber);
    if (negate) {
      return "!(" + f + ")";
    }
    return f;
  }

  /**
   * Retrieves the CFA associated with the TubeState object.
   *
   * @return The Control Flow Automaton (CFA) object related to the TubeState.
   */
  public CFA getCfa() {
    return cfa;
  }

  /**
   * Retrieves the value indicating whether the TubeState object is negated.
   *
   * @return true if the TubeState object is negated, false otherwise
   */
  public boolean getIsNegated() {
    return isNegated;
  }

  /**
   * Retrieves the CFAEdge object associated with this TubeState.
   *
   * @return The CFAEdge object representing the control flow edge.
   */
  public CFAEdge getCfaEdge() {
    return this.cfaEdge;
  }

  /**
   * Retrieves the asserts stored in the TubeState object.
   *
   * @return The immutable map of integers to strings representing the asserts.
   */
  public ImmutableMap<Integer, String> getAsserts() {
    return this.asserts;
  }

  /**
   * Retrieves the boolean expression associated with the TubeState object.
   *
   * @return The boolean expression stored in the TubeState object.
   */
  public String getBooleanExp() {
    return booleanExp;
  }

  /**
   * Retrieves the error counter value of the TubeState object.
   *
   * @return The value of the error counter for the TubeState object.
   */
  public int getErrorCounter() {
    return this.errorCounter;
  }

  /** Increases the error counter by 1. */
  public void incrementErrorCounter() {
    this.errorCounter += 1;
  }

  /**
   * Retrieves the name of the CPA associated with this AbstractQueryableState.
   *
   * @return The name of the CPA.
   */
  @Override
  public String getCPAName() {
    return getClass().getSimpleName();
  }

  /**
   * Returns the partition key of the object indicating in which part of the partition the object
   * belongs.
   *
   * @return The key indicating the part of the partition this object belongs to.
   */
  @Override
  public @Nullable Object getPartitionKey() {
    return this;
  }

  /**
   * Returns a string representation of the TubeState object.
   *
   * @return A string containing the value of the asserts, isNegated, booleanExp, and errorCounter
   *     fields of the TubeState object.
   */
  @Override
  public String toString() {
    return "TubeState{"
        + "asserts="
        + asserts
        + ", isNegated="
        + isNegated
        + ", booleanExp='"
        + booleanExp
        + '\''
        + ", errorCounter="
        + errorCounter
        + '}';
  }

  /**
   * Retrieves the formula approximation for the TubeState object using the provided
   * FormulaManagerView.
   *
   * @param manager The FormulaManagerView instance to use for obtaining the formula approximation.
   * @return The BooleanFormula approximation of the TubeState object based on the boolean
   *     expression and CFAEdge.
   */
  @Override
  public BooleanFormula getFormulaApproximation(FormulaManagerView manager) {
    String exp = this.getBooleanExp();
    if (exp == null || !asserts.containsKey(cfaEdge.getLineNumber())) {
      return manager.getBooleanFormulaManager().makeTrue();
    }

    BooleanFormula booleanFormula;
    try {
      booleanFormula = manager.uninstantiate(parseFormula(manager, exp, cfaEdge));
    } catch (InvalidAutomatonException
        | InterruptedException
        | UnrecognizedCodeException
        | InvalidConfigurationException
        | ToFormulaException pE) {
      throw new RuntimeException(pE);
    }
    return Objects.requireNonNullElseGet(
        booleanFormula, manager.getBooleanFormulaManager()::makeTrue);
  }

  /**
   * Returns a label representation of the object in the DOT format.
   *
   * @return A string representation of the object.
   */
  @Override
  public String toDOTLabel() {
    return toString();
  }

  /**
   * Indicates whether the method should be highlighted.
   *
   * @return true if the method should be highlighted, false otherwise
   */
  @Override
  public boolean shouldBeHighlighted() {
    return false;
  }

  /**
   * Parses the given entry as a BooleanFormula using the provided FormulaManagerView, CFAEdge and
   * other necessary information.
   *
   * @param pFormulaManagerView The FormulaManagerView instance to use for parsing.
   * @param entry The entry to parse as a BooleanFormula.
   * @param pCFAEdge The CFAEdge related to the entry.
   * @return The parsed BooleanFormula.
   * @throws InvalidAutomatonException If an error occurs during parsing.
   */
  BooleanFormula parseFormula(
      FormulaManagerView pFormulaManagerView, String entry, CFAEdge pCFAEdge)
      throws InvalidAutomatonException,
          InterruptedException,
          UnrecognizedCodeException,
          InvalidConfigurationException,
          ToFormulaException {
    Set<String> entries = new HashSet<>();
    entries.add(entry);
    PathFormulaManagerImpl pathFormulaManager =
        new PathFormulaManagerImpl(
            pFormulaManagerView,
            Configuration.defaultConfiguration(),
            logManager,
            ShutdownNotifier.createDummy(),
            cfa,
            AnalysisDirection.FORWARD);
    return CParserUtils.parseStatementsAsExpressionTree(
            entries,
            Optional.of(entry),
            Factory.getParser(
                logManager,
                Factory.getDefaultOptions(),
                cfa.getMachineModel(),
                ShutdownNotifier.createDummy()),
            new CProgramScope(cfa, logManager),
            ParserTools.create(ExpressionTrees.newFactory(), cfa.getMachineModel(), logManager))
        .accept(
            new ToFormulaVisitor(
                pFormulaManagerView,
                pathFormulaManager,
                pathFormulaManager.makeEmptyPathFormula()));
  }
}
