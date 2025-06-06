// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

package org.sosy_lab.cpachecker.cpa.statistics.provider;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CAstNode;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCall;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.c.CAssumeEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CDeclarationEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CFunctionCallEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CReturnStatementEdge;
import org.sosy_lab.cpachecker.cfa.model.c.CStatementEdge;
import org.sosy_lab.cpachecker.cfa.types.c.CArrayType;
import org.sosy_lab.cpachecker.cfa.types.c.CBasicType;
import org.sosy_lab.cpachecker.cfa.types.c.CCompositeType;
import org.sosy_lab.cpachecker.cfa.types.c.CPointerType;
import org.sosy_lab.cpachecker.cfa.types.c.CSimpleType;
import org.sosy_lab.cpachecker.cpa.statistics.provider.SimpleIntProvider.IntMerger;
import org.sosy_lab.cpachecker.cpa.statistics.provider.SimpleIntProvider.SimpleIntProviderImplementation;
import org.sosy_lab.cpachecker.util.CFAUtils;

/** This factory class provides a lot of StatisticsProvider. */
public class SimpleIntProviderFactory {

  /**
   * Helper method to count matching expressions. See the other countExpressions overload for more
   * information.
   */
  private static int countExpressions(CAstNode pExpression, Predicate<CExpression> matcher) {
    return CFAUtils.traverseRecursively(pExpression)
        .filter(CExpression.class)
        .filter(matcher)
        .size();
  }

  /** Helper method for counting matching declarations. */
  private static int countDeclarations(CFAEdge pEdge, Predicate<CDeclaration> matcher) {
    switch (pEdge.getEdgeType()) {
      case DeclarationEdge -> {
        CDeclarationEdge declEdge = (CDeclarationEdge) pEdge;
        CDeclaration decl = declEdge.getDeclaration();
        if (matcher.apply(decl)) {
          return 1;
        }
      }
      default -> {
        // no declaration
      }
    }

    return 0;
  }

  /**
   * Counts some property within the expression tree, note that matcher must handle only one single
   * CExpression instance. This method ensures that matcher.apply is called on every Expression in
   * the current Expression tree (given by pEdge).
   */
  private static int countExpressions(CFAEdge pEdge, final Predicate<CExpression> matcher) {
    int count = 0;
    switch (pEdge.getEdgeType()) {
      case DeclarationEdge -> {
        CDeclarationEdge declEdge = (CDeclarationEdge) pEdge;
        CDeclaration decl = declEdge.getDeclaration();
        if (decl instanceof CVariableDeclaration varDecl) {
          CInitializer init = varDecl.getInitializer();
          if (init != null) {
            count += countExpressions(init, matcher);
          }
        }
      }
      case AssumeEdge -> {
        CAssumeEdge assumeEdge = (CAssumeEdge) pEdge;
        count += countExpressions(assumeEdge.getExpression(), matcher);
      }
      case FunctionCallEdge -> {
        CFunctionCallEdge fcallEdge = (CFunctionCallEdge) pEdge;
        for (CExpression arg : fcallEdge.getArguments()) {
          count += countExpressions(arg, matcher);
        }
      }
      case StatementEdge -> {
        CStatementEdge stmtEdge = (CStatementEdge) pEdge;

        CStatement stmt = stmtEdge.getStatement();
        count += countExpressions(stmt, matcher);
      }
      case ReturnStatementEdge -> {
        CReturnStatementEdge returnEdge = (CReturnStatementEdge) pEdge;

        if (returnEdge.getExpression().isPresent()) {
          count += countExpressions(returnEdge.getExpression().orElseThrow(), matcher);
        }
      }
      default -> {
        // no expressions
      }
    }
    return count;
  }

  public enum MergeOption implements IntMerger {
    Min() {
      @Override
      public int merge(int s1, int s2) {
        return Math.min(s1, s2);
      }

      @Override
      public String toString() {
        return "min";
      }
    },

    Max() {
      @Override
      public int merge(int s1, int s2) {
        return Math.max(s1, s2);
      }

      @Override
      public String toString() {
        return "max";
      }
    },

    Add() {
      @Override
      public int merge(int s1, int s2) {
        return s1 + s2;
      }

      @Override
      public String toString() {
        return "add";
      }
    },
  }

  private static final SimpleIntProviderImplementation edgeCountProvider =
      new SimpleIntProviderImplementation() {
        @Override
        public String getPropertyName() {
          return "edgeCount";
        }

        @Override
        public int calculateNext(int pCurrent, CFAEdge edge) {
          return pCurrent + 1;
        }
      };

  public static SimpleIntProvider getEdgeCountProvider(MergeOption option) {
    return new SimpleIntProvider(edgeCountProvider, option, 0);
  }

  private static final SimpleIntProviderImplementation gotoCountProvider =
      new SimpleIntProviderImplementation() {
        @Override
        public String getPropertyName() {
          return "gotoCount";
        }

        @Override
        public int calculateNext(int pCurrent, CFAEdge edge) {
          if (edge.getEdgeType() == CFAEdgeType.BlankEdge
              && edge.getDescription().startsWith("Goto: ")) {
            return pCurrent + 1;
          }
          return pCurrent;
        }
      };

  public static SimpleIntProvider getGotoCountProvider(MergeOption option) {
    return new SimpleIntProvider(gotoCountProvider, option, 0);
  }

  public static SimpleIntProvider getLoopCountProvider(CFA cfa, MergeOption option) {
    final ImmutableSet<CFANode> loopHeads = cfa.getAllLoopHeads().orElseThrow();
    return new SimpleIntProvider(
        new SimpleIntProviderImplementation() {
          @Override
          public String getPropertyName() {
            return "loopCount";
          }

          @Override
          public int calculateNext(int pCurrent, CFAEdge edge) {
            CFANode pred = edge.getPredecessor();
            CFANode succ = edge.getSuccessor();
            // we run into a loop
            if (loopHeads.contains(succ)) {
              // Now we have to check that are are not already within the loop
              if (succ.getNodeNumber() > pred.getNodeNumber()) {
                // NOTE: Not really a very sophisticated test, but fast and worked in my test cases
                return pCurrent + 1;
              }
            }
            return pCurrent;
          }
        },
        option,
        0);
  }

  private static int countFunctionCalls(CFAEdge pEdge) {
    int count = 0;
    switch (pEdge.getEdgeType()) {
      case FunctionCallEdge -> count += 1;
      case StatementEdge -> {
        CStatementEdge stmtEdge = (CStatementEdge) pEdge;

        CStatement stmt = stmtEdge.getStatement();
        if (stmt instanceof CFunctionCall) {
          count++;
        }
      }
      default -> {
        // no function calls
      }
    }
    return count;
  }

  private static final SimpleIntProviderImplementation functionCallCountProvider =
      new SimpleIntProviderImplementation() {
        @Override
        public String getPropertyName() {
          return "functionCallCount";
        }

        @Override
        public int calculateNext(int pCurrent, CFAEdge edge) {
          return pCurrent + countFunctionCalls(edge);
        }
      };

  public static SimpleIntProvider getFunctionCallCountProvider(MergeOption option) {
    return new SimpleIntProvider(functionCallCountProvider, option, 0);
  }

  private static final SimpleIntProviderImplementation branchCountProvider =
      new SimpleIntProviderImplementation() {
        @Override
        public String getPropertyName() {
          return "branchCount";
        }

        @Override
        public int calculateNext(int pCurrent, CFAEdge edge) {
          if (edge.getSuccessor().getNumLeavingEdges() > 1) {
            return pCurrent + 1;
          }
          return pCurrent;
        }
      };

  public static SimpleIntProvider getBranchCountProvider(MergeOption option) {
    return new SimpleIntProvider(branchCountProvider, option, 0);
  }

  private static final SimpleIntProviderImplementation jumpCountProvider =
      new SimpleIntProviderImplementation() {
        @Override
        public String getPropertyName() {
          return "jumpCount";
        }

        @Override
        public int calculateNext(int pCurrent, CFAEdge edge) {
          if (edge.getPredecessor().getNodeNumber() + 1 != edge.getSuccessor().getNodeNumber()) {
            return pCurrent + 1;
          }
          return pCurrent;
        }
      };

  public static SimpleIntProvider getJumpCountProvider(MergeOption option) {
    return new SimpleIntProvider(jumpCountProvider, option, 0);
  }

  private static final SimpleIntProviderImplementation functionDefCountProvider =
      new SimpleIntProviderImplementation() {
        @Override
        public String getPropertyName() {
          return "functionDefCount";
        }

        @Override
        public int calculateNext(int pCurrent, CFAEdge pEdge) {
          return pCurrent + countDeclarations(pEdge, CFunctionDeclaration.class::isInstance);
        }
      };

  public static SimpleIntProvider getFunctionDefCountProvider(MergeOption option) {
    return new SimpleIntProvider(functionDefCountProvider, option, 0);
  }

  private static final SimpleIntProviderImplementation localVariablesCountProvider =
      new SimpleIntProviderImplementation() {
        @Override
        public String getPropertyName() {
          return "localVariablesCount";
        }

        @Override
        public int calculateNext(int pCurrent, CFAEdge edge) {
          return pCurrent
              + countDeclarations(
                  edge, decl -> decl instanceof CVariableDeclaration && !decl.isGlobal());
        }
      };

  public static SimpleIntProvider getLocalVariablesCountProvider(MergeOption option) {
    return new SimpleIntProvider(localVariablesCountProvider, option, 0);
  }

  private static final SimpleIntProviderImplementation globalVariablesCountProvider =
      new SimpleIntProviderImplementation() {
        @Override
        public String getPropertyName() {
          return "globalVariablesCount";
        }

        @Override
        public int calculateNext(int pCurrent, CFAEdge edge) {
          return pCurrent
              + countDeclarations(
                  edge, decl -> decl instanceof CVariableDeclaration && decl.isGlobal());
        }
      };

  public static SimpleIntProvider getGlobalVariablesCountProvider(MergeOption option) {
    return new SimpleIntProvider(globalVariablesCountProvider, option, 0);
  }

  private static final SimpleIntProviderImplementation structVariablesCountProvider =
      new SimpleIntProviderImplementation() {
        @Override
        public String getPropertyName() {
          return "structVariablesCount";
        }

        @Override
        public int calculateNext(int pCurrent, CFAEdge edge) {
          return pCurrent
              + countDeclarations(
                  edge,
                  declaration ->
                      declaration instanceof CVariableDeclaration
                          && declaration.getType().getCanonicalType() instanceof CCompositeType);
        }
      };

  public static SimpleIntProvider getStructVariablesCountProvider(MergeOption option) {
    return new SimpleIntProvider(structVariablesCountProvider, option, 0);
  }

  private static final SimpleIntProviderImplementation pointerVariablesCountProvider =
      new SimpleIntProviderImplementation() {
        @Override
        public String getPropertyName() {
          return "pointerVariablesCount";
        }

        @Override
        public int calculateNext(int pCurrent, CFAEdge edge) {
          return pCurrent
              + countDeclarations(
                  edge,
                  declaration ->
                      declaration instanceof CVariableDeclaration
                          && declaration.getType().getCanonicalType() instanceof CPointerType);
        }
      };

  public static SimpleIntProvider getPointerVariablesCountProvider(MergeOption option) {
    return new SimpleIntProvider(pointerVariablesCountProvider, option, 0);
  }

  private static final SimpleIntProviderImplementation arrayVariablesCountProvider =
      new SimpleIntProviderImplementation() {
        @Override
        public String getPropertyName() {
          return "arrayVariablesCount";
        }

        @Override
        public int calculateNext(int pCurrent, CFAEdge edge) {
          return pCurrent
              + countDeclarations(
                  edge,
                  declaration ->
                      declaration instanceof CVariableDeclaration
                          && declaration.getType().getCanonicalType() instanceof CArrayType);
        }
      };

  public static SimpleIntProvider getArrayVariablesCountProvider(MergeOption option) {
    return new SimpleIntProvider(arrayVariablesCountProvider, option, 0);
  }

  private static final SimpleIntProviderImplementation integerVariablesCountProvider =
      new SimpleIntProviderImplementation() {
        @Override
        public String getPropertyName() {
          return "integerVariablesCount";
        }

        @Override
        public int calculateNext(int pCurrent, CFAEdge edge) {
          return pCurrent
              + countDeclarations(
                  edge,
                  decl ->
                      decl instanceof CVariableDeclaration
                          && decl.getType().getCanonicalType() instanceof CSimpleType type
                          && (type.getType() == CBasicType.INT
                              || type.getType() == CBasicType.CHAR));
        }
      };

  public static SimpleIntProvider getIntegerVariablesCountProvider(MergeOption option) {
    return new SimpleIntProvider(integerVariablesCountProvider, option, 0);
  }

  private static final SimpleIntProviderImplementation floatVariablesCountProvider =
      new SimpleIntProviderImplementation() {
        @Override
        public String getPropertyName() {
          return "floatVariablesCount";
        }

        @Override
        public int calculateNext(int pCurrent, CFAEdge edge) {
          return pCurrent
              + countDeclarations(
                  edge,
                  decl ->
                      decl instanceof CVariableDeclaration
                          && decl.getType().getCanonicalType() instanceof CSimpleType type
                          && (type.getType() == CBasicType.FLOAT
                              || type.getType() == CBasicType.DOUBLE));
        }
      };

  public static SimpleIntProvider getFloatVariablesCountProvider(MergeOption option) {
    return new SimpleIntProvider(floatVariablesCountProvider, option, 0);
  }

  private static boolean isBitwiseOperation(CBinaryExpression exp) {
    switch (exp.getOperator()) {
      case BINARY_AND, BINARY_OR, BINARY_XOR, SHIFT_LEFT, SHIFT_RIGHT -> {
        // TODO: check if custom overload (ie no real bitwise operation) = types are ok
        return true;
      }
      default -> {}
    }
    return false;
  }

  private static final SimpleIntProviderImplementation bitwiseOperationCountProvider =
      new SimpleIntProviderImplementation() {
        @Override
        public String getPropertyName() {
          return "bitwiseOperationCount";
        }

        @Override
        public int calculateNext(int pCurrent, CFAEdge edge) {
          return pCurrent
              + countExpressions(
                  edge,
                  expression ->
                      expression instanceof CBinaryExpression binexp && isBitwiseOperation(binexp));
        }
      };

  public static SimpleIntProvider getBitwiseOperationCountProvider(MergeOption option) {
    return new SimpleIntProvider(bitwiseOperationCountProvider, option, 0);
  }

  private static final SimpleIntProviderImplementation dereferenceCountProvider =
      new SimpleIntProviderImplementation() {
        @Override
        public String getPropertyName() {
          return "dereferenceCount";
        }

        @Override
        public int calculateNext(int pCurrent, CFAEdge edge) {
          return pCurrent + countExpressions(edge, CPointerExpression.class::isInstance);
        }
      };

  public static SimpleIntProvider getDereferenceCountProvider(MergeOption option) {
    return new SimpleIntProvider(dereferenceCountProvider, option, 0);
  }

  private static int countAssumeStmts(CFAEdge pEdge) {
    int count = 0;
    switch (pEdge.getEdgeType()) {
      case AssumeEdge -> count += 1;
      default -> {
        // no assume
      }
    }
    return count;
  }

  private static final SimpleIntProviderImplementation assumeCountProvider =
      new SimpleIntProviderImplementation() {
        @Override
        public String getPropertyName() {
          return "assumeCount";
        }

        @Override
        public int calculateNext(int pCurrent, CFAEdge edge) {
          return pCurrent + countAssumeStmts(edge);
        }
      };

  public static SimpleIntProvider getAssumeCountProvider(MergeOption option) {
    return new SimpleIntProvider(assumeCountProvider, option, 0);
  }

  private static boolean isArithmeticOperation(CBinaryExpression exp) {
    switch (exp.getOperator()) {
      case DIVIDE, MINUS, MODULO, MULTIPLY, PLUS -> {
        // TODO: check if custom overload (ie no real arithmetic operation) = types are ok
        return true;
      }
      default -> {}
    }
    return false;
  }

  private static final SimpleIntProviderImplementation arithmeticOperationCountProvider =
      new SimpleIntProviderImplementation() {
        @Override
        public String getPropertyName() {
          return "arithmeticOperationCount";
        }

        @Override
        public int calculateNext(int pCurrent, CFAEdge edge) {
          return pCurrent
              + countExpressions(
                  edge,
                  exp -> exp instanceof CBinaryExpression binexp && isArithmeticOperation(binexp));
        }
      };

  public static SimpleIntProvider getArithmeticOperationCountProvider(MergeOption option) {
    return new SimpleIntProvider(arithmeticOperationCountProvider, option, 0);
  }
}
