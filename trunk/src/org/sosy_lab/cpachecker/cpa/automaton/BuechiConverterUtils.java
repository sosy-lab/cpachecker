// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.automaton;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import jhoafparser.ast.AtomLabel;
import jhoafparser.ast.BooleanExpression;
import jhoafparser.ast.BooleanExpression.Type;
import jhoafparser.storage.StoredAutomaton;
import jhoafparser.storage.StoredEdgeWithLabel;
import jhoafparser.storage.StoredHeader;
import jhoafparser.storage.StoredHeader.NameAndExtra;
import jhoafparser.storage.StoredState;
import org.sosy_lab.common.ShutdownNotifier;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.CParser;
import org.sosy_lab.cpachecker.cfa.ast.AExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpressionBuilder;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.parser.Scope;
import org.sosy_lab.cpachecker.cfa.types.MachineModel;
import org.sosy_lab.cpachecker.cfa.types.c.CProblemType;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.CPAQuery;
import org.sosy_lab.cpachecker.cpa.automaton.AutomatonBoolExpr.MatchCFAEdgeRegEx;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.CParserUtils;
import org.sosy_lab.cpachecker.util.ltl.LtlParseException;

public class BuechiConverterUtils {

  /**
   * Takes a {@link StoredAutomaton} (an automaton in HOA-format) as argument and transforms that
   * into an {@link Automaton}.
   *
   * @param pStoredAutomaton the storedAutomaton created by parsing an input in HOA-format.
   * @param pEntryFunction the name of the entry function of the c program.
   * @return an automaton from the automaton-framework in CPAchecker
   * @throws LtlParseException if the transformation fails either due to some false values in {@code
   *     pStoredAutomaton} or because of an erroneous config.
   */
  public static Automaton convertFromHOAFormat(
      StoredAutomaton pStoredAutomaton,
      String pEntryFunction,
      Configuration pConfig,
      LogManager pLogger,
      MachineModel pMachineModel,
      Scope pScope,
      ShutdownNotifier pShutdownNotifier)
      throws LtlParseException, InterruptedException {
    return new HoaToAutomatonTransformer(
            pStoredAutomaton,
            pEntryFunction,
            pConfig,
            pLogger,
            pMachineModel,
            pScope,
            pShutdownNotifier)
        .doConvert();
  }

  private static class HoaToAutomatonTransformer {

    private static final String AUTOMATON_NAME = "Buechi_Automaton";
    private static final String FALSE = "0";
    private static final String TRUE = "1";

    private final LogManager logger;
    private final MachineModel machineModel;
    private final Scope scope;
    private final CParser parser;

    private final StoredAutomaton storedAutomaton;
    private final Optional<String> entryFunctionOpt;
    private final ShutdownNotifier shutdownNotifier;

    private HoaToAutomatonTransformer(
        StoredAutomaton pStoredAutomaton,
        String pEntryFunction,
        Configuration pConfig,
        LogManager pLogger,
        MachineModel pMachineModel,
        Scope pScope,
        ShutdownNotifier pShutdownNotifier)
        throws LtlParseException {
      storedAutomaton = checkNotNull(pStoredAutomaton);
      checkArgument(!isNullOrEmpty(pEntryFunction));
      entryFunctionOpt = Optional.of(pEntryFunction);

      logger = checkNotNull(pLogger);
      machineModel = checkNotNull(pMachineModel);
      scope = checkNotNull(pScope);
      shutdownNotifier = checkNotNull(pShutdownNotifier);

      try {
        parser =
            CParser.Factory.getParser(
                pLogger, CParser.Factory.getOptions(pConfig), machineModel, shutdownNotifier);
      } catch (InvalidConfigurationException e) {
        throw new LtlParseException(e.getMessage(), e);
      }
    }

    private Automaton doConvert() throws LtlParseException, InterruptedException {

      StoredHeader storedHeader = storedAutomaton.getStoredHeader();

      NameAndExtra<List<Object>> accName =
          Iterables.getOnlyElement(storedHeader.getAcceptanceNames());
      if (!accName.name.equals("Buchi")) {
        throw new LtlParseException(
            String.format(
                "Only 'Buchi'-acceptance is allowed, but instead the following was found: %s (%s)",
                accName.name, accName.extra.toString()));
      }

      String accCond = storedHeader.getAcceptanceCondition().toStringInfix();
      if (!accCond.equals("Inf(0)")) {
        throw new LtlParseException(
            String.format(
                "The only allowed acceptance-condition is %s, but instead the following was found:"
                    + " %s",
                "Inf(0)", accCond));
      }

      ImmutableSet<String> requiredProperties =
          ImmutableSet.of(
              "complete",
              "deterministic",
              "trans-labels",
              "explicit-labels",
              "state-acc",
              "no-univ-branch");
      // The automaton should only contain properties which are included in the above list
      if (!requiredProperties.containsAll(storedHeader.getProperties())) {
        throw new LtlParseException(
            String.format(
                "The storedAutomaton-param may only contain %s as properties, but instead "
                    + "the following were found: %s",
                requiredProperties, storedHeader.getProperties()));
      }

      List<Integer> initStateList = Iterables.getOnlyElement(storedHeader.getStartStates());
      if (initStateList.size() != 1) {
        throw new LtlParseException("Univeral initial states are not supported");
      }

      int numAccSets = storedHeader.getNumberOfAcceptanceSets();
      if (numAccSets != 1) {
        throw new LtlParseException(
            String.format(
                "Only one acceptance set was expected, but instead %d were found", numAccSets));
      }

      try {
        ImmutableList.Builder<AutomatonInternalState> stateListBuilder =
            new ImmutableList.Builder<>();

        StoredState initBuchiState =
            storedAutomaton.getStoredState(Iterables.getOnlyElement(initStateList));
        String initBuechiStateName = getStateName(initBuchiState);

        String initStateName = null;
        if (entryFunctionOpt.isPresent()) {
          initStateName = "[pre-state] global-init";
          addPreBuchiStates(
              stateListBuilder, entryFunctionOpt.orElseThrow(), initStateName, initBuechiStateName);
        } else {
          initStateName = initBuechiStateName;
        }

        for (int i = 0; i < storedAutomaton.getNumberOfStates(); i++) {
          StoredState storedState = storedAutomaton.getStoredState(i);
          List<AutomatonTransition> transitionList = new ArrayList<>();

          for (StoredEdgeWithLabel edge : storedAutomaton.getEdgesWithLabel(i)) {
            int successorStateId = Iterables.getOnlyElement(edge.getConjSuccessors());
            String successorName = getStateName(storedAutomaton.getStoredState(successorStateId));

            transitionList.addAll(getTransitions(edge.getLabelExpr(), successorName));
          }

          boolean isTargetState = false;
          if (storedState.getAccSignature() != null) {
            int accSig = Iterables.getOnlyElement(storedState.getAccSignature());
            isTargetState = accSig == 0;
            if (!isTargetState) {
              throw new LtlParseException(
                  String.format(
                      "Automaton state has an acceptance signature, but the value "
                          + "is different to the exptected value (expected: 0, actual: %d",
                      accSig));
            }
          }

          stateListBuilder.add(
              new AutomatonInternalState(
                  getStateName(storedState), transitionList, isTargetState, true, false));
        }

        return new Automaton(
            AUTOMATON_NAME, ImmutableMap.of(), stateListBuilder.build(), initStateName);

      } catch (InvalidAutomatonException e) {
        throw new RuntimeException(
            "The passed storedAutomaton-parameter produces an inconsistent automaton", e);
      } catch (UnrecognizedCodeException e) {
        throw new LtlParseException(e.getMessage(), e);
      }
    }

    /**
     * Creates pre-states for the Büchi-automaton such that the actual first state of the
     * Büchi-automaton is entered when the function call of the main-method has been made.
     *
     * <p>This differs from the automaton specified in config/specification/MainEntry.spc insofar,
     * as the accepting state in that automaton is an error state (and thus a sink-state), which is
     * however not desired here. The other difference is that - technically speaking - this
     * automaton goes into the accepting state only after the edge 'Function start dummy edge' was
     * passed.
     */
    private void addPreBuchiStates(
        ImmutableList.Builder<AutomatonInternalState> pStateListBuilder,
        String pEntryFunctionName,
        String pInitStateName,
        String pInitBuechiStateName) {

      final String initStateName = pInitStateName;
      final String mainEntryStateName = "[pre-state] main-entry";

      MatchCFAEdgeRegEx matchCFAEdgeRegEx =
          new AutomatonBoolExpr.MatchCFAEdgeRegEx(
              ".*\\s+" + pEntryFunctionName + "\\s*\\(.*\\)\\s*;?.*");

      AutomatonTransition initToMainEntry =
          new AutomatonTransition.Builder(matchCFAEdgeRegEx, mainEntryStateName).build();

      CPAQuery query =
          new AutomatonBoolExpr.CPAQuery("location", "mainEntry==" + pEntryFunctionName);
      AutomatonTransition mainEntryToInitBuechiState =
          new AutomatonTransition.Builder(query, pInitBuechiStateName).build();

      AutomatonInternalState initState =
          new AutomatonInternalState(
              initStateName,
              ImmutableList.of(
                  initToMainEntry, createTransition(ImmutableList.of(), initStateName)),
              false,
              false);

      AutomatonInternalState mainEntryState =
          new AutomatonInternalState(
              mainEntryStateName,
              ImmutableList.of(
                  mainEntryToInitBuechiState,
                  createTransition(ImmutableList.of(), mainEntryStateName)),
              false,
              false);

      pStateListBuilder.add(initState);
      pStateListBuilder.add(mainEntryState);
    }

    private String getStateName(StoredState pState) {
      return pState.getInfo() != null ? pState.getInfo() : String.valueOf(pState.getStateId());
    }

    private List<AutomatonTransition> getTransitions(
        BooleanExpression<AtomLabel> pLabelExpr, String pSuccessorName)
        throws LtlParseException, UnrecognizedCodeException, InterruptedException {
      ImmutableList.Builder<AutomatonTransition> transitions = ImmutableList.builder();

      switch (pLabelExpr.getType()) {
        case EXP_OR:
          transitions.addAll(getTransitions(pLabelExpr.getLeft(), pSuccessorName));
          transitions.addAll(getTransitions(pLabelExpr.getRight(), pSuccessorName));
          break;
        default:
          transitions.add(createTransition(getExpressions(pLabelExpr), pSuccessorName));
          break;
      }

      return transitions.build();
    }

    private List<AExpression> getExpressions(BooleanExpression<AtomLabel> pLabelExpr)
        throws LtlParseException, UnrecognizedCodeException, InterruptedException {
      ImmutableList.Builder<AExpression> expBuilder = ImmutableList.builder();

      Type type = pLabelExpr.getType();
      switch (type) {
        case EXP_TRUE:
          return ImmutableList.of(assume(TRUE));
        case EXP_FALSE:
          return ImmutableList.of(assume(FALSE));
        case EXP_ATOM:
          int apIndex = pLabelExpr.getAtom().getAPIndex();
          return ImmutableList.of(assume(storedAutomaton.getStoredHeader().getAPs().get(apIndex)));
        case EXP_OR:
          expBuilder.addAll(getExpressions(pLabelExpr.getLeft()));
          expBuilder.addAll(getExpressions(pLabelExpr.getRight()));
          break;
        case EXP_AND:
          expBuilder.addAll(getExpressions(pLabelExpr.getLeft()));
          expBuilder.addAll(getExpressions(pLabelExpr.getRight()));
          break;
        case EXP_NOT:
          CBinaryExpressionBuilder b = new CBinaryExpressionBuilder(machineModel, logger);
          CExpression exp =
              (CExpression) Iterables.getOnlyElement(getExpressions(pLabelExpr.getLeft()));
          expBuilder.add(b.negateExpressionAndSimplify(exp));
          break;
        default:
          throw new RuntimeException("Unhandled expression type: " + type);
      }

      return expBuilder.build();
    }

    private CExpression assume(String pExpression) throws LtlParseException, InterruptedException {
      CAstNode sourceAST;
      try {
        sourceAST = CParserUtils.parseSingleStatement(pExpression, parser, scope);
      } catch (InvalidAutomatonException e) {
        throw new LtlParseException(
            String.format("Error in literal of ltl-formula: %s", e.getMessage()), e);
      }
      CExpression expression = ((CExpressionStatement) sourceAST).getExpression();
      if (expression.getExpressionType() instanceof CProblemType) {
        logger.log(
            Level.WARNING,
            "The parsed expression is of type 'CProblemType', "
                + "most likely because it depends on an unresolved name within the c-code");
      }
      return expression;
    }

    private AutomatonTransition createTransition(
        List<AExpression> pAssumptions, String pFollowStateName) {
      return new AutomatonTransition.Builder(AutomatonBoolExpr.TRUE, pFollowStateName)
          .withAssumptions(pAssumptions)
          .build();
    }
  }
}
