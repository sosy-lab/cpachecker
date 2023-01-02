// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.lock;

import static com.google.common.collect.FluentIterable.from;
import static org.sosy_lab.common.collect.Collections3.transformedImmutableListCopy;

import com.google.common.base.Joiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.sosy_lab.common.configuration.Configuration;
import org.sosy_lab.common.configuration.InvalidConfigurationException;
import org.sosy_lab.common.configuration.Option;
import org.sosy_lab.common.configuration.Options;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CLeftHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.core.CPAcheckerResult.Result;
import org.sosy_lab.cpachecker.core.defaults.SingleEdgeTransferRelation;
import org.sosy_lab.cpachecker.core.defaults.SingletonPrecision;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.core.interfaces.Statistics;
import org.sosy_lab.cpachecker.core.reachedset.UnmodifiableReachedSet;
import org.sosy_lab.cpachecker.cpa.lock.effects.AbstractLockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.AcquireLockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.CheckLockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.LockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.ReleaseLockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.ResetLockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.RestoreAllLockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.RestoreLockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.SaveStateLockEffect;
import org.sosy_lab.cpachecker.cpa.lock.effects.SetLockEffect;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;
import org.sosy_lab.cpachecker.util.Pair;
import org.sosy_lab.cpachecker.util.Precisions;
import org.sosy_lab.cpachecker.util.identifiers.AbstractIdentifier;
import org.sosy_lab.cpachecker.util.identifiers.IdentifierCreator;
import org.sosy_lab.cpachecker.util.identifiers.SingleIdentifier;
import org.sosy_lab.cpachecker.util.statistics.StatInt;
import org.sosy_lab.cpachecker.util.statistics.StatKind;
import org.sosy_lab.cpachecker.util.statistics.StatTimer;
import org.sosy_lab.cpachecker.util.statistics.StatisticsWriter;

@Options(prefix = "cpa.lock")
public class LockTransferRelation extends SingleEdgeTransferRelation {

  public static class LockStatistics implements Statistics {

    private final StatTimer transferTimer = new StatTimer("Time for transfer");
    private final StatTimer operationsTimer = new StatTimer("Time for extracting effects");
    private final StatTimer filteringTimer = new StatTimer("Time for filtering effects");
    private final StatTimer applyTimer = new StatTimer("Time for applying effects");
    private final StatInt lockEffects = new StatInt(StatKind.SUM, "Number of effects");
    private final StatInt locksInState = new StatInt(StatKind.AVG, "Number of locks in state");
    private final StatInt locksInStateWithLocks =
        new StatInt(StatKind.AVG, "Number of locks in state with locks");

    @Override
    public void printStatistics(PrintStream pOut, Result pResult, UnmodifiableReachedSet pReached) {
      StatisticsWriter w =
          StatisticsWriter.writingStatisticsTo(pOut)
              .put(transferTimer)
              .beginLevel()
              .put(operationsTimer)
              .put(filteringTimer)
              .put(applyTimer)
              .endLevel()
              .put(lockEffects)
              .put(locksInState)
              .put(locksInStateWithLocks);

      Precision p = pReached.getPrecision(pReached.getFirstState());
      LockPrecision lockPrecision = Precisions.extractPrecisionByType(p, LockPrecision.class);
      if (lockPrecision != null) {
        w.put("Number of considered lock operations", lockPrecision.getKeySize())
            .put("Considered lock identifiers", lockPrecision.getValues());
      }
    }

    @Override
    public @Nullable String getName() {
      return "LockCPA";
    }
  }

  private final Map<String, AnnotationInfo> annotatedFunctions;

  private final LockInfo lockDescription;
  private final LogManager logger;
  private final LockStatistics stats;

  @Option(
      name = "stopAfterLockLimit",
      description = "stop path exploration if a lock limit is reached",
      secure = true)
  private boolean stopAfterLockLimit = false;

  public LockTransferRelation(Configuration config, LogManager logger)
      throws InvalidConfigurationException {
    this.logger = logger;

    config.inject(this);
    ConfigurationParser parser = new ConfigurationParser(config);

    lockDescription = parser.parseLockInfo();
    annotatedFunctions = parser.parseAnnotatedFunctions();
    assert annotatedFunctions != null;

    stats = new LockStatistics();
  }

  @Override
  public Collection<AbstractLockState> getAbstractSuccessorsForEdge(
      AbstractState element, Precision pPrecision, CFAEdge cfaEdge)
      throws UnrecognizedCodeException {

    AbstractLockState lockStatisticsElement = (AbstractLockState) element;

    stats.transferTimer.start();

    stats.operationsTimer.start();
    // First, determine operations with locks
    List<AbstractLockEffect> toProcess = determineOperations(cfaEdge);
    stats.lockEffects.setNextValue(toProcess.size());
    stats.operationsTimer.stop();

    if (pPrecision instanceof SingletonPrecision) {
      // From refiner
    } else {
      LockPrecision lockPrecision = (LockPrecision) pPrecision;
      stats.filteringTimer.start();
      toProcess = lockPrecision.filter(cfaEdge.getPredecessor(), toProcess);
      stats.filteringTimer.stop();
    }

    stats.applyTimer.start();
    AbstractLockState successor = applyEffects(lockStatisticsElement, toProcess);
    stats.applyTimer.stop();

    stats.transferTimer.stop();

    if (successor != null) {
      int locks = successor.getSize();
      stats.locksInState.setNextValue(locks);
      if (locks > 0) {
        stats.locksInStateWithLocks.setNextValue(locks);
      }
      return Collections.singleton(successor);
    } else {
      return ImmutableSet.of();
    }
  }

  public AbstractLockState applyEffects(
      AbstractLockState oldState, List<AbstractLockEffect> toProcess) {
    final AbstractLockStateBuilder builder = oldState.builder();
    toProcess.forEach(e -> e.effect(builder));
    return builder.build();
  }

  public Set<LockIdentifier> getAffectedLocks(CFAEdge cfaEdge) {
    return getLockEffects(cfaEdge).transform(LockEffect::getAffectedLock).toSet();
  }

  public FluentIterable<LockEffect> getLockEffects(CFAEdge cfaEdge) {
    try {
      return from(determineOperations(cfaEdge)).filter(LockEffect.class);
    } catch (UnrecognizedCodeException e) {
      logger.log(Level.WARNING, "The code " + cfaEdge + " is not recognized");
      return FluentIterable.of();
    }
  }

  public List<AbstractLockEffect> determineOperations(CFAEdge cfaEdge)
      throws UnrecognizedCodeException {

    switch (cfaEdge.getEdgeType()) {
      case FunctionCallEdge:
        return handleFunctionCall((CFunctionCallEdge) cfaEdge);

      case FunctionReturnEdge:
        return handleFunctionReturnEdge((CFunctionReturnEdge) cfaEdge);

      case StatementEdge:
        return handleStatement((CStatementEdge) cfaEdge);
      case AssumeEdge:
        return handleAssumption((CAssumeEdge) cfaEdge);

      case BlankEdge:
      case ReturnStatementEdge:
      case DeclarationEdge:
      case CallToReturnEdge:
        break;

      default:
        throw new UnrecognizedCodeException("Unknown edge type", cfaEdge);
    }
    return ImmutableList.of();
  }

  private List<AbstractLockEffect> handleAssumption(CAssumeEdge cfaEdge) {
    CExpression assumption = cfaEdge.getExpression();

    if (assumption instanceof CBinaryExpression) {
      CBinaryExpression binExpression = (CBinaryExpression) assumption;
      IdentifierCreator creator = new IdentifierCreator(cfaEdge.getSuccessor().getFunctionName());
      AbstractIdentifier varId = creator.createIdentifier(binExpression.getOperand1(), 0);
      if (varId instanceof SingleIdentifier) {
        String varName = ((SingleIdentifier) varId).getName();
        if (lockDescription.getVariableEffectDescription().containsKey(varName)) {
          CExpression val = binExpression.getOperand2();
          if (val instanceof CIntegerLiteralExpression) {
            if (binExpression.getOperator() == BinaryOperator.EQUALS) {
              LockIdentifier id = lockDescription.getVariableEffectDescription().get(varName);
              int level = ((CIntegerLiteralExpression) val).getValue().intValue();
              AbstractLockEffect e =
                  CheckLockEffect.createEffectForId(level, cfaEdge.getTruthAssumption(), id);
              return Collections.singletonList(e);
            } else {
              logger.log(
                  Level.WARNING,
                  "Unknown binary operator " + binExpression.getOperator() + ", skip it");
            }
          }
        }
      }
    }
    return ImmutableList.of();
  }

  private ImmutableList<? extends AbstractLockEffect> convertAnnotationToLockEffect(
      Set<LockIdentifier> pAnnotatedIds, LockEffect pEffect) {
    return transformedImmutableListCopy(pAnnotatedIds, pEffect::cloneWithTarget);
  }

  private List<AbstractLockEffect> handleFunctionReturnEdge(CFunctionReturnEdge cfaEdge) {
    // CFANode tmpNode = cfaEdge.getSummaryEdge().getPredecessor();
    String fName =
        cfaEdge
            .getSummaryEdge()
            .getExpression()
            .getFunctionCallExpression()
            .getFunctionNameExpression()
            .toASTString();
    if (annotatedFunctions.containsKey(fName)) {
      ImmutableList.Builder<AbstractLockEffect> result = ImmutableList.builder();

      AnnotationInfo currentAnnotation = annotatedFunctions.get(fName);
      if (currentAnnotation.getRestoreLocks().isEmpty()
          && currentAnnotation.getFreeLocks().isEmpty()
          && currentAnnotation.getResetLocks().isEmpty()
          && currentAnnotation.getCaptureLocks().isEmpty()) {
        // Not specified annotations are considered to be totally restoring
        AbstractLockEffect restoreAll = RestoreAllLockEffect.getInstance();
        return Collections.singletonList(restoreAll);
      } else {
        if (!currentAnnotation.getRestoreLocks().isEmpty()) {
          result.addAll(
              convertAnnotationToLockEffect(
                  currentAnnotation.getRestoreLocks(), RestoreLockEffect.getInstance()));
        }
        if (!currentAnnotation.getFreeLocks().isEmpty()) {
          result.addAll(
              convertAnnotationToLockEffect(
                  currentAnnotation.getFreeLocks(), ReleaseLockEffect.getInstance()));
        }
        if (!currentAnnotation.getResetLocks().isEmpty()) {
          result.addAll(
              convertAnnotationToLockEffect(
                  currentAnnotation.getResetLocks(), ResetLockEffect.getInstance()));
        }
        if (!currentAnnotation.getCaptureLocks().isEmpty()) {
          for (LockIdentifier targetId : currentAnnotation.getCaptureLocks()) {
            result.add(
                AcquireLockEffect.createEffectForId(
                    targetId, lockDescription.getMaxLevel(targetId.getName()), stopAfterLockLimit));
          }
        }
        return result.build();
      }
    }
    return ImmutableList.of();
  }

  private List<AbstractLockEffect> handleFunctionCallExpression(CFunctionCallExpression function) {
    String functionName = function.getFunctionNameExpression().toASTString();
    if (!lockDescription.getFunctionEffectDescription().containsKey(functionName)) {
      return ImmutableList.of();
    }
    Pair<LockEffect, LockIdUnprepared> locksWithEffect =
        lockDescription.getFunctionEffectDescription().get(functionName);
    LockEffect effect = locksWithEffect.getFirst();
    LockIdUnprepared uId = locksWithEffect.getSecond();

    ImmutableList.Builder<AbstractLockEffect> result = ImmutableList.builder();

    if (effect == SetLockEffect.getInstance()) {
      CExpression expression = function.getParameterExpressions().get(0);
      // Replace it by parametrical one
      if (expression instanceof CIntegerLiteralExpression) {
        int newValue = ((CIntegerLiteralExpression) expression).getValue().intValue();
        int max = lockDescription.getMaxLevel(uId.getName());
        if (max < newValue) {
          newValue = max;
        }
        effect = SetLockEffect.createEffectForId(newValue, uId.apply(null));
      } else {
        // We can not process not integers
        return result.build();
      }
    }

    LockIdentifier id = uId.apply(function.getParameterExpressions());
    if (effect == AcquireLockEffect.getInstance()) {
      effect =
          AcquireLockEffect.createEffectForId(
              id, lockDescription.getMaxLevel(uId.getName()), stopAfterLockLimit);
    } else {
      effect = effect.cloneWithTarget(id);
    }
    result.add(effect);
    return result.build();
  }

  private List<AbstractLockEffect> handleStatement(CStatementEdge statementEdge) {

    CStatement statement = statementEdge.getStatement();
    if (statement instanceof CAssignment) {
      /*
       * level = intLock();
       */
      CRightHandSide op2 = ((CAssignment) statement).getRightHandSide();

      if (op2 instanceof CFunctionCallExpression) {
        CFunctionCallExpression function = (CFunctionCallExpression) op2;
        return handleFunctionCallExpression(function);
      } else {
        /*
         * threadDispatchLevel = 1;
         */
        CLeftHandSide leftSide = ((CAssignment) statement).getLeftHandSide();
        CRightHandSide rightSide = ((CAssignment) statement).getRightHandSide();
        IdentifierCreator creator =
            new IdentifierCreator(statementEdge.getSuccessor().getFunctionName());
        AbstractIdentifier varId = creator.createIdentifier(leftSide, 0);
        if (varId instanceof SingleIdentifier) {
          String varName = ((SingleIdentifier) varId).getName();
          if (lockDescription.getVariableEffectDescription().containsKey(varName)) {
            if (rightSide instanceof CIntegerLiteralExpression) {
              LockIdentifier id = lockDescription.getVariableEffectDescription().get(varName);
              int level = ((CIntegerLiteralExpression) rightSide).getValue().intValue();
              AbstractLockEffect e = SetLockEffect.createEffectForId(level, id);
              return Collections.singletonList(e);
            }
          }
        }
      }

    } else if (statement instanceof CFunctionCallStatement) {
      /*
       * queLock(que);
       */
      CFunctionCallStatement funcStatement = (CFunctionCallStatement) statement;
      return handleFunctionCallExpression(funcStatement.getFunctionCallExpression());
    }
    // No lock-relating operations
    return ImmutableList.of();
  }

  private List<AbstractLockEffect> handleFunctionCall(CFunctionCallEdge callEdge) {
    List<AbstractLockEffect> result = new ArrayList<>();
    if (annotatedFunctions.containsKey(callEdge.getSuccessor().getFunctionName())) {
      AbstractLockEffect saveState = SaveStateLockEffect.getInstance();
      result.add(saveState);
    }
    result.addAll(
        handleFunctionCallExpression(
            callEdge.getSummaryEdge().getExpression().getFunctionCallExpression()));
    return result;
  }

  /**
   * Used in UsageStatisticsCPAStatistics In true case the current line should be highlighted in the
   * final report
   *
   * @param pEdge edge to check
   * @return the verdict
   */
  public String doesChangeTheState(CFAEdge pEdge) {
    return getLockEffects(pEdge).join(Joiner.on(","));
  }

  public Statistics getStatistics() {
    return stats;
  }
}
