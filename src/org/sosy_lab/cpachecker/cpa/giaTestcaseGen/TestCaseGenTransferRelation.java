// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.giaTestcaseGen;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.FileOption;
import org.sosy_lab.common.configuration.FileOption.Type;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.io.PathTemplate;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CCharLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFloatLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLiteralExpression;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.algorithm.giageneration.GIAGenerator;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonState;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;

@Options(prefix = "cpa.testcasegen")
public class TestCaseGenTransferRelation extends SingleEdgeTransferRelation {

  @Option(secure = true, description = "Filename format for Testcase TEST-COMP output dumps")
  @FileOption(Type.OUTPUT_FILE)
  private PathTemplate exportPath = PathTemplate.ofFormatString("output/testcase-%d.xml");

  int numberOfTestcases = 0;

  private static final String ASSUMPTION_AUTOMATON_NAME = "AssumptionAutomaton";
  private final LogManager logger;
  private boolean mayHaveAssumption = false;

  public TestCaseGenTransferRelation(LogManager pLogger, Configuration pConfiguration)
      throws InvalidConfigurationException {
    this.logger = pLogger;
    pConfiguration.inject(this);
  }

  @Override
  public Collection<? extends AbstractState> getAbstractSuccessorsForEdge(
      AbstractState state, Precision precision, CFAEdge cfaEdge)
      throws CPATransferException, InterruptedException {
    this.mayHaveAssumption = false;
    if (cfaEdge instanceof CStatementEdge) {
      mayHaveAssumption = true;
    }
    if (state instanceof TestCaseGenState) {
      TestCaseGenState tcState = ((TestCaseGenState) state);

      if (tcState.isNewTestCaseState()) {
        logger.logf(
            Level.INFO,
            "Stopping exploration at Edge %s, as %s is a %s-state",
            cfaEdge,
            tcState.toString(),
            GIAGenerator.NAME_OF_NEWTESTINPUT_STATE);

        try {
          tcState.dumpToTestcase(this.exportPath.getPath(this.numberOfTestcases));
        } catch (IOException pE) {
          logger.logf(
              Level.WARNING,
              "Unable to generate a testcase for %s due to %s",
              tcState.toString(),
              Throwables.getStackTraceAsString(pE));
        }
        numberOfTestcases = numberOfTestcases + 1;
        return ImmutableList.of();
      }
      return Collections.singleton(tcState.copy());
    }
    return Collections.singleton(state);
  }

  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState state,
      Iterable<AbstractState> otherStates,
      @Nullable CFAEdge cfaEdge,
      Precision precision)
      throws CPATransferException, InterruptedException {
    assert state instanceof TestCaseGenState;
    TestCaseGenState testcaseStae = (TestCaseGenState) state;

    for (AbstractState other : otherStates) {
      if (other instanceof AutomatonState
          && ((AutomatonState) other).getOwningAutomatonName().equals(ASSUMPTION_AUTOMATON_NAME)) {
        AutomatonState autoState = (AutomatonState) other;
        if (mayHaveAssumption) {
          if (!autoState.getAssumptions().isEmpty()) {
            for (AExpression assumption : autoState.getAssumptions()) {
              if (assumption instanceof CBinaryExpression
                  && ((CBinaryExpression) assumption).getOperator().equals(BinaryOperator.EQUALS)) {
                // We have an assumption that we can work with
                CBinaryExpression eq = (CBinaryExpression) assumption;
                if (eq.getOperand1() instanceof CLiteralExpression
                    && eq.getOperand2() instanceof CIdExpression) {
                  TestcaseEntry entry =
                      constructTestcaseEntry(
                          (CLiteralExpression) eq.getOperand1(), (CIdExpression) eq.getOperand2());
                  testcaseStae.addNewTestcase(entry);
                  logger.logf(Level.INFO, "Adding assumption %s", entry.toXMLTestcaseLine());
                } else if (eq.getOperand2() instanceof CLiteralExpression
                    && eq.getOperand1() instanceof CIdExpression) {
                  TestcaseEntry entry =
                      constructTestcaseEntry(
                          (CLiteralExpression) eq.getOperand2(), (CIdExpression) eq.getOperand1());
                  testcaseStae.addNewTestcase(entry);
                  logger.logf(Level.INFO, "Adding assumption %s", entry.toXMLTestcaseLine());
                }
              }
            }
          }
        }
        testcaseStae = testcaseStae.setAutomatonState(Optional.of(autoState));
      }
    }
    return Collections.singleton(testcaseStae);
  }

  private TestcaseEntry constructTestcaseEntry(CLiteralExpression pLiteral, CIdExpression pVar) {
    String value = "";
    if (pLiteral instanceof CIntegerLiteralExpression) {
      value = ((CIntegerLiteralExpression) pLiteral).getValue().toString();
    } else if (pLiteral instanceof CFloatLiteralExpression) {
      value = ((CFloatLiteralExpression) pLiteral).getValue().toString();
    } else if (pLiteral instanceof CCharLiteralExpression) {
      value = String.valueOf(((CCharLiteralExpression) pLiteral).getCharacter());
    } else {
      // TODO: Add parsing for floats, imaginary numbers and strings
      logger.logf(
          Level.WARNING, "Dont know how to parse %s of type %s", pLiteral, pLiteral.getClass());
    }

    return new TestcaseEntry(
        value,
        Optional.of(pVar.getDeclaration().getName()),
        Optional.of(pVar.getExpressionType().toString()));
  }
}
