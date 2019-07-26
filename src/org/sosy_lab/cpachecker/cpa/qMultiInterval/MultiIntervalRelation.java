/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2018  Dirk Beyer
 *  All rights reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 *  CPAchecker web page:
 *    http://cpachecker.sosy-lab.org
 */
package org.sosy_lab.cpachecker.cpa.qMultiInterval;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.sosy_lab.common.log.LogManager;
import org.sosy_lab.cpachecker.cfa.ast.c.CAssignment;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression.BinaryOperator;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CIntegerLiteralExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CParameterDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CRightHandSide;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.BlankEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionReturnEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionSummaryEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.core.defaults.ForwardingTransferRelation;
import org.sosy_lab.cpachecker.core.interfaces.AbstractState;
import org.sosy_lab.cpachecker.core.interfaces.Precision;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.ControlDependencyTrackerState;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.DependencyTrackerState;
import org.sosy_lab.cpachecker.cpa.ifcsecurity.dependencytracking.Variable;
import org.sosy_lab.cpachecker.exceptions.CPATransferException;
import org.sosy_lab.cpachecker.exceptions.UnrecognizedCodeException;

public class MultiIntervalRelation
    extends ForwardingTransferRelation<
        MultiIntervalState, MultiIntervalState, Precision> {

  // ================================================================================
  // Vars
  // ================================================================================
  @SuppressWarnings("unused")
  private LogManager logger;

  private TreeMap<Variable, IntervalExt> imap;
  private MultiIntervalMinMaxMapping minEnt;
  // private exitStatus eS = exitStatus.IDLE;
  private TreeSet<Variable> collectible = new TreeSet<>();

  // ================================================================================
  // Constructor
  // ================================================================================
  public MultiIntervalRelation(
      LogManager logger, IntervalMapping imap, MultiIntervalMinMaxMapping minEnt) {
    this.logger = logger;
    this.minEnt = minEnt;
    this.imap = imap.getMap();
    // Log.Disable();
  }

  // ================================================================================
  // Edgehandling
  // ================================================================================

  @Override
  protected MultiIntervalState handleAssumption(
      CAssumeEdge pCfaEdge, CExpression pExpression, boolean pTruthAssumption)
      throws CPATransferException {

    //    if (state.hasToBreakNow()) {
    //      Log.Log2("return null");
    //      return null;
    //    }

    // clone old state
    MultiIntervalState st = state.clone();
    if (state.getLopC().isEmpty()) {
      st.setInnterloop(false);
    }
    st.setAssume();

    // Log.Log2("ass" + state);
    // If we reach a state where the status is Collect we already have collected all necessary data.
    // So if we would go in the loop we return null.
    // So we can only exit the loop. But first we set all used Variables in the Loop to Unbound.

    // Check if we would go another loopdround. Return null if so.
    if (pCfaEdge.getPredecessor().isLoopStart()) {
      if (st.hasToBreakNow()
          && ((pTruthAssumption && st.eS == MultiIntervalState.exitStatus.COLLECT_T)
              || ((!pTruthAssumption) && st.eS == MultiIntervalState.exitStatus.COLLECT_F))) {
        throw new CPATransferException("Non existent Transfer");
      }
      // We have ran one round with exitStatus collect. Now we set all used variables to unbound
      // before we continue
      if (state.hasToExitNow()
          && (st.eS == MultiIntervalState.exitStatus.COLLECT_T
              || st.eS == MultiIntervalState.exitStatus.COLLECT_F)) {
        TreeMap<Variable, Range> temp = new TreeMap<>();
        for (Variable var : collectible) {
          st.removeKey(var);
          temp.put(var, Range.UNBOUND);
        }
        st.eS = MultiIntervalState.exitStatus.EXIT;
        st.combineMaps(temp);
    }
    }
    Log.Log2("sss" + st);
    // if the previous Block was executed we can only leave the loop now
    // ---
    // Set the dependency to the loopstart so we always know in which loop we are
    if (pCfaEdge.getPredecessor().isLoopStart()) {
      state.setCFANode(pCfaEdge.getPredecessor());
    }

    BinaryOperator operator = ((CBinaryExpression) pExpression).getOperator();
    CExpression operand1 = ((CBinaryExpression) pExpression).getOperand1();
    CExpression operand2 = ((CBinaryExpression) pExpression).getOperand2();

    // swap the order of the operands if they are not in the right order for now
    if (operand1 instanceof CIntegerLiteralExpression) {
      CExpression temp = operand1;
      operand1 = operand2;
      operand2 = temp;
      if (!(operator.equals(BinaryOperator.EQUALS) || operator.equals(BinaryOperator.NOT_EQUALS))) {
      operator.getOppositLogicalOperator();
      }
    }

    ExpressionValueRangeVisitor visitor = new ExpressionValueRangeVisitor(st, pCfaEdge);
    Range interval1 = operand1.accept(visitor);
    Range interval2 = operand2.accept(visitor);


    assert !interval1.isEmpty() : operand1;
    assert !interval2.isEmpty() : operand2;

    // we dont support full expressions for now
    assert operand1 instanceof CIdExpression : "STH went wrong" + operand1;
    assert (operand2 instanceof CIntegerLiteralExpression || operand2 instanceof CIdExpression);

    // TODO work with qualified name!
    // even if the operand is just a constant this will work

    Variable var1 = new Variable(operand1.toString());
    Variable var2 = new Variable(operand2.toString());

    // the following lines assume that one of the operands is an identifier
    // and the other one represented with an interval (example "x<[3;5]").
    // If none of the operands is an identifier, nothing is done.

    if (!pTruthAssumption) {
      operator = operator.getOppositLogicalOperator();
    }



    TreeMap<Variable, Range> temp = new TreeMap<>();
    TreeMap<Variable, Range> tempOut = new TreeMap<>();
    temp.put(var1, st.getRangeMap().get(var1));
    temp.put(var2, st.getRangeMap().get(var2));

    // TODO make operant2 always an Interval and let be operant 1 always a Variable.
    // TODO alternative for later: Swap operator1 or refactor it to like c+1 < d => _c_ < d-1
    // && _d_ > c+1 where _c_,_d_ is the variable for which the interval should be saved
    // then much more assumption edges can be handled

    // this switch() is quite messy because of the autoformatting of the CPAchecker
    switch (operator) {
          //, a < const
          case LESS_THAN: {
          for (Entry<Variable, Range> entry : temp.entrySet()) {
            if ((entry
                    .getKey()
                    .equals(
                        new Variable(
                            ((CIdExpression) operand1).getDeclaration().getQualifiedName()))
                && !entry.getValue().limitUpperBound(interval2.minus(1L)).isEmpty())) {
              tempOut.put(
                  new Variable(((CIdExpression) operand1).getDeclaration().getQualifiedName()),
                  interval1.limitUpperBound(interval2.minus(1L)));


            } else if (!(operand2 instanceof CIntegerLiteralExpression)
                && !entry.getValue().limitLowerBound(interval2.plus(1L)).isEmpty()
                && entry
                    .getKey()
                    .equals(
                        new Variable(
                            ((CIdExpression) operand2).getDeclaration().getQualifiedName()))) {
              tempOut.put(
                  new Variable(((CIdExpression) operand2).getDeclaration().getQualifiedName()),
                  interval2.limitLowerBound(interval1.plus(1L)));
            }
            }
          return returnState(st, var1, var2, tempOut);
        }
      case GREATER_THAN:
        {

          for (Entry<Variable, Range> entry : temp.entrySet()) {
            if ((entry
                    .getKey()
                    .equals(
                        new Variable(
                            ((CIdExpression) operand1).getDeclaration().getQualifiedName()))
                && !entry.getValue().limitLowerBound(interval2.plus(1L)).isEmpty())) {
              tempOut.put(
                  new Variable(((CIdExpression) operand1).getDeclaration().getQualifiedName()),
                  interval1.limitLowerBound(interval2.plus(1L)));

            } else if (!(operand2 instanceof CIntegerLiteralExpression)
                && !entry.getValue().limitUpperBound(interval2.minus(1L)).isEmpty()
                && entry
                    .getKey()
                    .equals(
                        new Variable(
                            ((CIdExpression) operand2).getDeclaration().getQualifiedName()))) {
              tempOut.put(
                  new Variable(((CIdExpression) operand2).getDeclaration().getQualifiedName()),
                  interval2.limitUpperBound(interval1.minus(1L)));
            }
          }
          return returnState(st, var1, var2, tempOut);
        }

      case LESS_EQUAL:
        {
          for (Entry<Variable, Range> entry : temp.entrySet()) {
            if ((entry
                    .getKey()
                    .equals(
                        new Variable(
                            ((CIdExpression) operand1).getDeclaration().getQualifiedName()))
                && !entry.getValue().limitUpperBound(interval2).isEmpty())) {
              tempOut.put(
                  new Variable(((CIdExpression) operand1).getDeclaration().getQualifiedName()),
                  interval1.limitUpperBound(interval2));


            } else if (!(operand2 instanceof CIntegerLiteralExpression)
                && !entry.getValue().limitLowerBound(interval2).isEmpty()
                && entry
                    .getKey()
                    .equals(
                        new Variable(
                            ((CIdExpression) operand2).getDeclaration().getQualifiedName()))) {
              tempOut.put(
                  new Variable(((CIdExpression) operand2).getDeclaration().getQualifiedName()),
                  interval2.limitLowerBound(interval1));
            }
          }
          return returnState(st, var1, var2, tempOut);
        }
      case GREATER_EQUAL:
        {
          for (Entry<Variable, Range> entry : temp.entrySet()) {
            if ((entry
                    .getKey()
                    .equals(
                        new Variable(
                            ((CIdExpression) operand1).getDeclaration().getQualifiedName()))
                && !entry.getValue().limitLowerBound(interval2).isEmpty())) {
              tempOut.put(
                  new Variable(((CIdExpression) operand1).getDeclaration().getQualifiedName()),
                  interval1.limitLowerBound(interval2));

            } else if (!(operand2 instanceof CIntegerLiteralExpression)
                && !entry.getValue().limitUpperBound(interval2).isEmpty()
                && entry
                    .getKey()
                    .equals(
                        new Variable(
                            ((CIdExpression) operand2).getDeclaration().getQualifiedName()))) {
              tempOut.put(
                  new Variable(((CIdExpression) operand2).getDeclaration().getQualifiedName()),
                  interval2.limitUpperBound(interval1));
            }
          }
          return returnState(st, var1, var2, tempOut);
        }

        // equals and not equals needs to be refactored a bit
      case EQUALS:
        {
          for (Entry<Variable, Range> entry : temp.entrySet()) {
            if ((entry
                    .getKey()
                    .equals(
                        new Variable(
                            ((CIdExpression) operand1).getDeclaration().getQualifiedName()))
                && !entry.getValue().intersect(interval2).isEmpty())) {
              tempOut.put(
                  new Variable(((CIdExpression) operand1).getDeclaration().getQualifiedName()),
                  entry.getValue().intersect(interval2));
            }
            if ((!(operand2 instanceof CIntegerLiteralExpression))
                && (entry
                        .getKey()
                        .equals(
                            new Variable(
                                ((CIdExpression) operand2).getDeclaration().getQualifiedName()))
                    && !entry.getValue().intersect(interval1).isEmpty())) {
              tempOut.put(
                  new Variable(((CIdExpression) operand2).getDeclaration().getQualifiedName()),
                  entry.getValue().intersect(interval1));
            }
          }
          return returnState(st, var1, var2, tempOut);
        }
      case NOT_EQUALS:
        {
          if ((operand2 instanceof CIdExpression
                  && ((CIdExpression) operand1).getDeclaration().getQualifiedName()
                      == ((CIdExpression) operand2).getDeclaration().getQualifiedName())
              || interval1.equals(interval2) && interval1.size() == 1) {
          throw new CPATransferException("No Sucessor");
          }

          for (Entry<Variable, Range> entry : temp.entrySet()) {
            if ((entry
                    .getKey()
                    .equals(
                        new Variable(
                            ((CIdExpression) operand1).getDeclaration().getQualifiedName()))
                && interval2.size() == 1)) {
              Range r = entry.getValue().clone();
              r.addOut(new IntervalExt(interval2.getLow(), interval2.getHigh()));
              tempOut.put(
                  new Variable(((CIdExpression) operand1).getDeclaration().getQualifiedName()), r);
            }else if ((entry
                .getKey()
                .equals(
                    new Variable(
                        ((CIdExpression) operand1).getDeclaration().getQualifiedName()))
            && interval2.size() != 1)) {

              tempOut.put(
                  new Variable(((CIdExpression) operand1).getDeclaration().getQualifiedName()),
                  entry.getValue());
            } else if ((!(operand2 instanceof CIntegerLiteralExpression))
                && (entry
                        .getKey()
                        .equals(
                            new Variable(
                                ((CIdExpression) operand2).getDeclaration().getQualifiedName()))
                    && interval1.size() == 1)) {
              Range r = entry.getValue().clone();
              r.addOut(new IntervalExt(interval1.getLow(), interval1.getHigh()));
              tempOut.put(
                  new Variable(((CIdExpression) operand2).getDeclaration().getQualifiedName()), r);
            } else if ((!(operand2 instanceof CIntegerLiteralExpression))
                && (entry
                        .getKey()
                        .equals(
                            new Variable(
                                ((CIdExpression) operand2).getDeclaration().getQualifiedName()))
                    && interval1.size() != 1)) {

              tempOut.put(
                  new Variable(((CIdExpression) operand2).getDeclaration().getQualifiedName()),
                  entry.getValue());
            }
          }
          return returnState(st, var1, var2, tempOut);
        }
      default:
        throw new UnrecognizedCodeException(
            "unexpected operator in assumption", pCfaEdge, pExpression);
    }
  }
  /**
   * A delegation from the handleAssumeEdge. Because this has to be called in every swith case this
   * is delegated to this method instead. It replaces all currently used variables in the temporary
   * treeMap with their newer versions.
   *
   * @param st current state
   * @param var1 variable to remove
   * @param var2 variable to remove
   * @param tempMap new values of the removed variables
   * @return null if this state cant be reached (i.e both used variables would have an empty
   *     interval after this edge)and the modified state if it can be reached
   */
  private MultiIntervalState returnState(
      MultiIntervalState st, Variable var1, Variable var2, TreeMap<Variable, Range> tempMap) {

    if (tempMap.isEmpty()) {
      // Dead path
      return null;
    } else {
      // modify the state
      st.removeKey(var1);
      st.removeKey(var2);
      st.combineMaps(tempMap);
      return st;
    }
  }

  @Override
  protected MultiIntervalState handleFunctionCallEdge(
      CFunctionCallEdge pCfaEdge,
      List<CExpression> pArguments,
      List<CParameterDeclaration> pParameters,
      String pCalledFunctionName)
      throws CPATransferException {
    //  TODO
    // clone old state
    MultiIntervalState st = state.clone();
    return st;
  }

  @Override
  protected MultiIntervalState handleFunctionReturnEdge(
      CFunctionReturnEdge pCfaEdge,
      CFunctionSummaryEdge pFnkCall,
      CFunctionCall pSummaryExpr,
      String pCallerFunctionName)
      throws CPATransferException {
    //  TODO
    // clone old state
    MultiIntervalState st = state.clone();
    return st;
  }
  // =================================================

  @Override
  protected MultiIntervalState handleDeclarationEdge(CDeclarationEdge pCfaEdge, CDeclaration pDecl)
      throws CPATransferException {

    // clone old state
    MultiIntervalState st = state.clone();

    if (pCfaEdge.getDeclaration() instanceof CVariableDeclaration) {
      CVariableDeclaration decl = (CVariableDeclaration)pCfaEdge.getDeclaration();

      // ignore pointer variables
      if (decl.getType() instanceof CPointerType) {
        assert false : "No Pointer atm";
        return st;
      }

      // Set the interval to the one specified in the config
      IntervalExt interval;
      interval = imap.get(new Variable(pCfaEdge.getDeclaration().getName()));

      st.addRange(new Variable(decl.getQualifiedName()), new Range(interval));
    }


    return st;
  }

  @Override
  protected MultiIntervalState handleStatementEdge(CStatementEdge pCfaEdge, CStatement pStatement)
      throws CPATransferException {
    // clone old state
    MultiIntervalState st = state.clone();

    if (pStatement instanceof CAssignment) {
      CAssignment assignExpression = (CAssignment) pStatement;
      CExpression op1 = assignExpression.getLeftHandSide();
      CRightHandSide op2 = assignExpression.getRightHandSide();

      Variable var = new Variable(((CIdExpression) op1).getDeclaration().getQualifiedName());
      if (st.eS == MultiIntervalState.exitStatus.COLLECT_T
          || st.eS == MultiIntervalState.exitStatus.COLLECT_F) {
        collectible.add(var);
      }
      // Statements like  a =  (c+d*X)/Y have to be handled by the visitor
      Range interval = evaluateInterval(st, op2, pCfaEdge);

      st.addRange(var, interval);
    }
    return st;
  }

  /**
   * This method calculates the Range in this state given the old Intervals, and the expression.
   *
   * @param readableState the state with the old intervals
   * @param expression the expression which is evaluated on the intervals
   * @param cfaEdge additionally the edge, which is only used to give details if the
   *        UnrecognizedCCodeException is thrown
   * @return the calculated new Range
   * @throws UnrecognizedCodeExceptionself explanatory
   */
  private Range evaluateInterval(
      MultiIntervalState readableState, CRightHandSide expression, CFAEdge cfaEdge)
      throws UnrecognizedCodeException {
    return expression.accept(new ExpressionValueRangeVisitor(readableState, cfaEdge));
  }

  @Override
  protected MultiIntervalState handleReturnStatementEdge(CReturnStatementEdge pCfaEdge)
      throws CPATransferException {
    // clone old state
    MultiIntervalState st = state.clone();
    return st;
  }


  @Override
  protected MultiIntervalState handleBlankEdge(BlankEdge pCfaEdge) {
    // clone old state
    MultiIntervalState st = state.clone();
    st.setDep(state.getDep());
    return st;
  }




  @Override
  protected MultiIntervalState handleFunctionSummaryEdge(CFunctionSummaryEdge pCfaEdge) throws CPATransferException {
    // clone old state
    MultiIntervalState st = state.clone();
    return st;
  }

  // ================================================================================
  // Strengthening
  // ================================================================================

  @SuppressWarnings("unchecked")
  @Override
  public Collection<? extends AbstractState> strengthen(
      AbstractState pState,
      List<AbstractState> pOtherStates,
      CFAEdge pCfaEdge,
      Precision pPrecision)
      throws CPATransferException, InterruptedException {

    assert pState instanceof MultiIntervalState;
    MultiIntervalState state1 = (MultiIntervalState) pState;
    // state1.SendMessage("Strengthened!");

    if (pCfaEdge.getSuccessor().isLoopStart()) {
      state1.setLoopstart();
    }

    ControlDependencyTrackerState contDepTState = null;
    DependencyTrackerState depTState = null;

     for (AbstractState p : pOtherStates) {

       if (p instanceof ControlDependencyTrackerState) {
        contDepTState = (ControlDependencyTrackerState) p;
      } else if (p instanceof DependencyTrackerState) {
        depTState = (DependencyTrackerState) p;
           }
        //else if (p instanceof LocationState) {
        //        LS = (LocationState) p;
      }

    //  strengthenSaveIntervals(state1, IAS);

    // Calculate the Min-Entropy on the last state

    assert contDepTState != null : "The analysis ControlDependencyTracking needs to be run";
    Map<CFANode, SortedSet<Variable>> contexts = contDepTState.getContexts();
    SortedSet<CFANode> rContexts = new TreeSet<>();
    // now look if there is a Context and only look for Elements which have an unempty context.
    for (Entry<CFANode, SortedSet<Variable>> ent : contexts.entrySet()) {
      if (!ent.getValue().isEmpty()) {
        rContexts.add(ent.getKey());

      }
    }

    // Test if the previous state is the loop start.
    if (pCfaEdge.getPredecessor().isLoopStart()) {
      if (state1.hasToExitNow() && state1.eS == MultiIntervalState.exitStatus.IDLE) {
        assert (pCfaEdge instanceof CAssumeEdge);


        if (((CAssumeEdge) pCfaEdge).getTruthAssumption()) {
          state1.eS = MultiIntervalState.exitStatus.COLLECT_T;
        } else {
          state1.eS = MultiIntervalState.exitStatus.COLLECT_F;
        }
      } else if (state1.eS == MultiIntervalState.exitStatus.EXIT) {
        state1.eS = MultiIntervalState.exitStatus.IDLE;
      }

      // test if theres no match to ONE entry, so we just left the loop
      assert !(state1.getLopC().size() > rContexts.size() + 1);
      if (state1.getLopC().size() == rContexts.size() + 1) {

        // We successfully left the loop
        state1.SendMessage("JustLeftOneLoop");
        // Log.Log2("Left");
        Set<CFANode> temp = ((TreeMap<CFANode, Integer>) state1.getLopC().clone()).keySet();
        temp.removeAll(rContexts);
        state1.eS = MultiIntervalState.exitStatus.IDLE;
        assert !temp.isEmpty();

        // just one iteration
        for (CFANode node : temp) {
          state1.resetLoop(node);
        }
      }

      // Test if we are in the inner loop
      for (CFANode cf : rContexts) {
        if (pCfaEdge.getPredecessor().equals(cf)) {
          // We are in the inner loop so we set the flags and add a loop
          state1.setInnterloop(true);
          state1.addLoop(cf);
        }
      }
    }
    if (!(pCfaEdge instanceof BlankEdge)) {

    state1.SendMessage(rContexts);
    state1.setDep(rContexts);
    }

    // this is for calculating the Min-Entropy
    if (pCfaEdge.getSuccessor().getNumLeavingEdges() == 0) {
      state1.SendMessage("LastState!");

      assert depTState != null;
      Map<Variable, SortedSet<Variable>> map = depTState.getDependencies();
      TreeMap<Variable, SortedSet<Variable>> tM = new TreeMap<>();
      tM.putAll(map);
      state1.setLast();
      state1.calculateMinEntropy(minEnt, tM, imap, depTState.getPrec());
    }

    return Collections.singleton(state1);
  }

  // ================================================================================
  // Strengthen delegations
  // ================================================================================

  // ================================================================================
  // Additional Methods
  // ================================================================================

}
