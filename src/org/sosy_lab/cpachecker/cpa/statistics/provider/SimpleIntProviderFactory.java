/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2014  Dirk Beyer
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
package org.sosy_lab.cpachecker.cpa.statistics.provider;

import org.sosy_lab.cpachecker.cfa.CFA;
import org.sosy_lab.cpachecker.cfa.ast.c.CArraySubscriptExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CBinaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CComplexCastExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CDesignatedInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CExpressionStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFieldReference;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallAssignmentStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionCallStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CFunctionDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.CIdExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializer;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerList;
import org.sosy_lab.cpachecker.cfa.ast.c.CInitializerVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CPointerExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatement;
import org.sosy_lab.cpachecker.cfa.ast.c.CStatementVisitor;
import org.sosy_lab.cpachecker.cfa.ast.c.CUnaryExpression;
import org.sosy_lab.cpachecker.cfa.ast.c.CVariableDeclaration;
import org.sosy_lab.cpachecker.cfa.ast.c.DefaultCExpressionVisitor;
import org.sosy_lab.cpachecker.cfa.model.CFAEdge;
import org.sosy_lab.cpachecker.cfa.model.CFAEdgeType;
import org.sosy_lab.cpachecker.cfa.model.CFANode;
import org.sosy_lab.cpachecker.cfa.model.MultiEdge;
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
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.statistics.provider.SimpleIntProvider.IntMerger;
import org.sosy_lab.cpachecker.cpa.statistics.provider.SimpleIntProvider.SimpleIntProviderImplementation;

import com.google.common.collect.ImmutableSet;

/**
 * This factory class provides a lot of StatisticsProvider.
 */
public class SimpleIntProviderFactory {

  public static interface Counter<T> {
    int count(T e);
  }
  /**
   * Helper method to count expressions.
   * See the other countExpressions overload for more information.
   */
  private static int countExpressions(CExpression pExpression, Counter<CExpression> counter) {
    int count = counter.count(pExpression);
    if (pExpression instanceof CIdExpression) {
    } else if (pExpression instanceof CBinaryExpression) {
      CBinaryExpression binexp = (CBinaryExpression)pExpression;
      count += countExpressions(binexp.getOperand1(), counter);
      count += countExpressions(binexp.getOperand2(), counter);
    } else if (pExpression instanceof CUnaryExpression) {
      CUnaryExpression unexp = (CUnaryExpression)pExpression;
      count += countExpressions(unexp.getOperand(), counter);
    } else if (pExpression instanceof CCastExpression) {
      CCastExpression castexp = (CCastExpression)pExpression;
      count += countExpressions(castexp.getOperand(), counter);
    } else if (pExpression instanceof CArraySubscriptExpression) {
      CArraySubscriptExpression arraySubExp = (CArraySubscriptExpression)pExpression;
      count += countExpressions(arraySubExp.getArrayExpression(), counter);
      count += countExpressions(arraySubExp.getSubscriptExpression(), counter);
    } else if (pExpression instanceof CComplexCastExpression) {
      CComplexCastExpression castexp = (CComplexCastExpression)pExpression;
      count += countExpressions(castexp.getOperand(), counter);
    } else if (pExpression instanceof CFieldReference) {
      CFieldReference castexp = (CFieldReference)pExpression;
      count += countExpressions(castexp.getFieldOwner(), counter);
    } else if (pExpression instanceof CPointerExpression) {
      CPointerExpression exp = (CPointerExpression)pExpression;
      count += countExpressions(exp.getOperand(), counter);
    }
    return count;
  }
  /**
   * Helper method for counting declarations.
   */
  private static int countDeclarations(CFAEdge pEdge, Counter<CDeclaration> counter) {
    int count = 0;
    switch (pEdge.getEdgeType()) {
    case DeclarationEdge:
      CDeclarationEdge declEdge = (CDeclarationEdge) pEdge;
      CDeclaration decl = declEdge.getDeclaration();
      count += counter.count(decl);
      break;
    case MultiEdge:
      MultiEdge multEdge = (MultiEdge) pEdge;
      for (CFAEdge edge : multEdge) {
        count += countDeclarations(edge, counter);
      }
      break;
    }

    return count;
  }

  public static abstract class DefaultCInitializerVisitor<R, X extends Exception>
    extends DefaultCExpressionVisitor<R, X>
    implements CInitializerVisitor<R, X> {
    @Override
    public R visit(CInitializerExpression pInitializerExpression) throws X {
      return visitDefault(pInitializerExpression.getExpression());
    }

    public abstract R combineMultiple(R left, R right);
    @Override
    public R visit(CInitializerList pInitializerList) throws X {
      R ret = combineMultiple(null, null); // get default value
      for (CInitializer initalizer : pInitializerList.getInitializers()) {
        ret = combineMultiple(ret, initalizer.accept(this));
      }
      return ret;
    }

    @Override
    public R visit(CDesignatedInitializer pCStructInitializerPart) throws X {
      return pCStructInitializerPart.getRightHandSide().accept(this);
    }
  }

  /**
   * Counts some property within the expression tree, note that counter must handle only one single CExpression instance.
   * This method ensures that counter.count is called on every Expression in the current Expression tree (given by pEdge).
   */
  private static int countExpressions(CFAEdge pEdge, final Counter<CExpression> counter) {
    int count = 0;
    switch (pEdge.getEdgeType()) {
    case DeclarationEdge:
      CDeclarationEdge declEdge = (CDeclarationEdge) pEdge;
      CDeclaration decl = declEdge.getDeclaration();
      if (decl instanceof CVariableDeclaration) {
        CVariableDeclaration varDecl = (CVariableDeclaration) decl;
        CInitializer init = varDecl.getInitializer();
        if (init != null) {
          count += init.accept(new DefaultCInitializerVisitor<Integer, RuntimeException>() {
            @Override
            public Integer combineMultiple(Integer pLeft, Integer pRight) {
              if (pLeft == null) {
                if (pRight == null) {
                  return 0;
                }
                return pRight;
              }
              if (pRight == null) {
                return pLeft;
              }
              return pLeft + pRight;
            }

            @Override
            protected Integer visitDefault(CExpression pExp) throws RuntimeException {
              return counter.count(pExp);
            }
          });
        }
      }
      break;
    case AssumeEdge:
      CAssumeEdge assumeEdge = (CAssumeEdge) pEdge;
      count += countExpressions(assumeEdge.getExpression(), counter);
      break;
    case FunctionCallEdge:
      CFunctionCallEdge fcallEdge = (CFunctionCallEdge) pEdge;
      for (CExpression arg : fcallEdge.getArguments()) {
        count += countExpressions(arg, counter);
      }

      break;
    case StatementEdge:
      CStatementEdge stmtEdge = (CStatementEdge) pEdge;

      CStatement stmt = stmtEdge.getStatement();
      count += stmt.accept(new CStatementVisitor<Integer, RuntimeException>() {
        @Override
        public Integer visit(CExpressionStatement pStmt) throws RuntimeException {
          return countExpressions(pStmt.getExpression(), counter);
        }

        @Override
        public Integer visit(CExpressionAssignmentStatement assignment) throws RuntimeException {
          int count = 0;
          count += countExpressions(assignment.getLeftHandSide(), counter);
          count += countExpressions(assignment.getRightHandSide(), counter);
          return count;
        }

        @Override
        public Integer visit(CFunctionCallAssignmentStatement pStmt)
            throws RuntimeException {
          int count = 0;
          CFunctionCallExpression exp = pStmt.getFunctionCallExpression();
          count += countExpressions(exp.getFunctionNameExpression(), counter);
          for (CExpression expresion : exp.getParameterExpressions()) {
            count += countExpressions(expresion, counter);
          }
          count += countExpressions(pStmt.getLeftHandSide(), counter);
          return count;
        }

        @Override
        public Integer visit(CFunctionCallStatement pStmt) throws RuntimeException {
          int count = 0;
          CFunctionCallExpression exp = pStmt.getFunctionCallExpression();
          count += countExpressions(exp.getFunctionNameExpression(), counter);
          for (CExpression expresion : exp.getParameterExpressions()) {
            count += countExpressions(expresion, counter);
          }
          return count;
        }});
      break;
    case ReturnStatementEdge:
      CReturnStatementEdge returnEdge = (CReturnStatementEdge) pEdge;

      if (returnEdge.getExpression().isPresent()) {
        count += countExpressions(returnEdge.getExpression().get(), counter);
      }
      break;

    case MultiEdge:
      MultiEdge multEdge = (MultiEdge) pEdge;
      for (CFAEdge edge : multEdge) {
        count += countExpressions(edge, counter);
      }
      break;
    }
    return count;
  }

  public static IntMerger defaultMerge = new IntMerger(){
    @Override
    public int merge(int s1, int s2) {
      return s1 + s2;
    }
    @Override
    public String toString() {
      return "add";
    }
  };
  public static IntMerger maxMerge = new IntMerger(){
    @Override
    public int merge(int s1, int s2) {
      return Math.max(s1, s2);
    }
    @Override
    public String toString() {
      return "max";
    }
  };

  public static IntMerger minMerge = new IntMerger(){
    @Override
    public int merge(int s1, int s2) {
      return Math.min(s1, s2);
    }
    @Override
    public String toString() {
      return "min";
    }
  };

  public enum MergeOption {
    Min, Max, Add
  }

  public static IntMerger getMerger(MergeOption opt) {
    switch (opt) {
    case Min:
      return minMerge;
    case Max:
      return maxMerge;
    default:
      return defaultMerge;
    }
  }

  public static SimpleIntProviderImplementation edgeCountProvider = new SimpleIntProviderImplementation() {
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
    return new SimpleIntProvider(edgeCountProvider, getMerger(option), 0);
  }


  public static SimpleIntProviderImplementation gotoCountProvider = new SimpleIntProviderImplementation() {
    @Override
    public String getPropertyName() {
      return "gotoCount";
    }

    @Override
    public int calculateNext(int pCurrent, CFAEdge edge) {
      if (edge.getEdgeType() == CFAEdgeType.BlankEdge && edge.getDescription().startsWith("Goto: ")){
        return pCurrent + 1;
      }
      return pCurrent;
    }
  };

  public static SimpleIntProvider getGotoCountProvider(MergeOption option) {
    return new SimpleIntProvider(gotoCountProvider, getMerger(option), 0);
  }

  public static SimpleIntProvider getLoopCountProvider(CFA cfa, MergeOption option) {
    final ImmutableSet<CFANode> loopHeads = cfa.getAllLoopHeads().get();
    return new SimpleIntProvider(new SimpleIntProviderImplementation() {
      @Override
      public String getPropertyName() {
        return "loopCount";
      }

      @Override
      public int calculateNext(int pCurrent, CFAEdge edge) {
        CFANode pred = edge.getPredecessor();
        CFANode succ = edge.getSuccessor();
        // we run into a loop
        if (loopHeads.contains(succ)){
          // Now we have to check that are are not already within the loop
          if (succ.getNodeNumber() > pred.getNodeNumber()) {
            // NOTE: Not really a very sophisticated test, but fast and worked in my test cases
            return pCurrent + 1;
          }
        }
        return pCurrent;
      }
    }, getMerger(option), 0);
  }

  private static int countFunctionCalls(CFAEdge pEdge) {
    int count = 0;
    switch (pEdge.getEdgeType()) {
    case FunctionCallEdge:
      count += 1;
      break;
    case StatementEdge:
      CStatementEdge stmtEdge = (CStatementEdge) pEdge;

      CStatement stmt = stmtEdge.getStatement();
      count += stmt.accept(new CStatementVisitor<Integer, RuntimeException>() {
        @Override
        public Integer visit(CExpressionStatement pStmt) throws RuntimeException {
          return 0;
        }

        @Override
        public Integer visit(CExpressionAssignmentStatement assignment) throws RuntimeException {
          return 0;
        }

        @Override
        public Integer visit(CFunctionCallAssignmentStatement pStmt)
            throws RuntimeException {
          return 1;
        }

        @Override
        public Integer visit(CFunctionCallStatement pStmt) throws RuntimeException {
          return 1;
        }});
      break;
    case MultiEdge:
      MultiEdge multEdge = (MultiEdge) pEdge;
      for (CFAEdge edge : multEdge) {
        count += countFunctionCalls(edge);
      }
      break;
    }
    return count;
  }
  public static SimpleIntProviderImplementation functionCallCountProvider = new SimpleIntProviderImplementation() {
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
    return new SimpleIntProvider(functionCallCountProvider, getMerger(option), 0);
  }

  public static SimpleIntProviderImplementation branchCountProvider = new SimpleIntProviderImplementation() {
    @Override
    public String getPropertyName() {
      return "branchCount";
    }

    @Override
    public int calculateNext(int pCurrent, CFAEdge edge) {
      if (edge.getSuccessor().getNumLeavingEdges() > 1){
        return pCurrent + 1;
      }
      return pCurrent;
    }
  };

  public static SimpleIntProvider getBranchCountProvider(MergeOption option) {
    return new SimpleIntProvider(branchCountProvider, getMerger(option), 0);
  }

  public static SimpleIntProviderImplementation jumpCountProvider = new SimpleIntProviderImplementation() {
    @Override
    public String getPropertyName() {
      return "jumpCount";
    }

    @Override
    public int calculateNext(int pCurrent, CFAEdge edge) {
      if (edge.getPredecessor().getNodeNumber() + 1 != edge.getSuccessor().getNodeNumber()){
        return pCurrent + 1;
      }
      return pCurrent;
    }
  };

  public static SimpleIntProvider getJumpCountProvider(MergeOption option) {
    return new SimpleIntProvider(jumpCountProvider, getMerger(option), 0);
  }


  public static SimpleIntProviderImplementation functionDefCountProvider = new SimpleIntProviderImplementation() {
    @Override
    public String getPropertyName() {
      return "functionDefCount";
    }

    @Override
    public int calculateNext(int pCurrent, CFAEdge pEdge) {
      return pCurrent +
          countDeclarations(pEdge, new Counter<CDeclaration>() {
            @Override
            public int count(CDeclaration pE) {
              if (pE instanceof CFunctionDeclaration) {
                return 1;
              }
              return 0;
            }});
    }
  };

  public static SimpleIntProvider getFunctionDefCountProvider(MergeOption option) {
    return new SimpleIntProvider(functionDefCountProvider, getMerger(option), 0);
  }


  public static SimpleIntProviderImplementation localVariablesCountProvider = new SimpleIntProviderImplementation() {
    @Override
    public String getPropertyName() {
      return "localVariablesCount";
    }

    @Override
    public int calculateNext(int pCurrent, CFAEdge edge) {
      return pCurrent + countDeclarations(edge, new Counter<CDeclaration>() {
        @Override
        public int count(CDeclaration declaration) {
          if (declaration instanceof CVariableDeclaration) {
            if (!declaration.isGlobal()) {
              return 1;
            }
          }
          return 0;
        }
      });
    }
  };

  public static SimpleIntProvider getLocalVariablesCountProvider(MergeOption option) {
    return new SimpleIntProvider(localVariablesCountProvider, getMerger(option), 0);
  }


  public static SimpleIntProviderImplementation globalVariablesCountProvider = new SimpleIntProviderImplementation() {
    @Override
    public String getPropertyName() {
      return "globalVariablesCount";
    }

    @Override
    public int calculateNext(int pCurrent, CFAEdge edge) {
      return pCurrent + countDeclarations(edge, new Counter<CDeclaration>() {
        @Override
        public int count(CDeclaration declaration) {
          if (declaration instanceof CVariableDeclaration) {
            if (declaration.isGlobal()) {
              return 1;
            }
          }
          return 0;
        }
      });
    }
  };

  public static SimpleIntProvider getGlobalVariablesCountProvider(MergeOption option) {
    return new SimpleIntProvider(globalVariablesCountProvider, getMerger(option), 0);
  }



  public static SimpleIntProviderImplementation structVariablesCountProvider = new SimpleIntProviderImplementation() {
    @Override
    public String getPropertyName() {
      return "structVariablesCount";
    }

    @Override
    public int calculateNext(int pCurrent, CFAEdge edge) {
      return pCurrent + countDeclarations(edge, new Counter<CDeclaration>() {
        @Override
        public int count(CDeclaration declaration) {
          if (declaration instanceof CVariableDeclaration) {
            if (declaration.getType().getCanonicalType() instanceof CCompositeType) {
              return 1;
            }
          }
          return 0;
        }
      });
    }
  };

  public static SimpleIntProvider getStructVariablesCountProvider(MergeOption option) {
    return new SimpleIntProvider(structVariablesCountProvider, getMerger(option), 0);
  }


  public static SimpleIntProviderImplementation pointerVariablesCountProvider = new SimpleIntProviderImplementation() {
    @Override
    public String getPropertyName() {
      return "pointerVariablesCount";
    }

    @Override
    public int calculateNext(int pCurrent, CFAEdge edge) {
      return pCurrent + countDeclarations(edge, new Counter<CDeclaration>() {
        @Override
        public int count(CDeclaration declaration) {
          if (declaration instanceof CVariableDeclaration) {
            if (declaration.getType().getCanonicalType() instanceof CPointerType) {
              return 1;
            }
          }
          return 0;
        }
      });
    }
  };

  public static SimpleIntProvider getPointerVariablesCountProvider(MergeOption option) {
    return new SimpleIntProvider(pointerVariablesCountProvider, getMerger(option), 0);
  }


  public static SimpleIntProviderImplementation arrayVariablesCountProvider = new SimpleIntProviderImplementation() {
    @Override
    public String getPropertyName() {
      return "arrayVariablesCount";
    }

    @Override
    public int calculateNext(int pCurrent, CFAEdge edge) {
      return pCurrent + countDeclarations(edge, new Counter<CDeclaration>() {
        @Override
        public int count(CDeclaration declaration) {
          if (declaration instanceof CVariableDeclaration) {
            if (declaration.getType().getCanonicalType() instanceof CArrayType) {
              return 1;
            }
          }
          return 0;
        }
      });
    }
  };

  public static SimpleIntProvider getArrayVariablesCountProvider(MergeOption option) {
    return new SimpleIntProvider(arrayVariablesCountProvider, getMerger(option), 0);
  }


  public static SimpleIntProviderImplementation integerVariablesCountProvider = new SimpleIntProviderImplementation() {
    @Override
    public String getPropertyName() {
      return "integerVariablesCount";
    }

    @Override
    public int calculateNext(int pCurrent, CFAEdge edge) {
      return pCurrent + countDeclarations(edge, new Counter<CDeclaration>() {
        @Override
        public int count(CDeclaration declaration) {
          if (declaration instanceof CVariableDeclaration) {
            CType canonical = declaration.getType().getCanonicalType();
            if (canonical instanceof CSimpleType) {
              CSimpleType simple = (CSimpleType)canonical;
              if (simple.getType() == CBasicType.INT || simple.getType() == CBasicType.CHAR) {
                return 1;
              }
            }
          }
          return 0;
        }
      });
    }
  };

  public static SimpleIntProvider getIntegerVariablesCountProvider(MergeOption option) {
    return new SimpleIntProvider(integerVariablesCountProvider, getMerger(option), 0);
  }


  public static SimpleIntProviderImplementation floatVariablesCountProvider = new SimpleIntProviderImplementation() {
    @Override
    public String getPropertyName() {
      return "floatVariablesCount";
    }

    @Override
    public int calculateNext(int pCurrent, CFAEdge edge) {
      return pCurrent + countDeclarations(edge, new Counter<CDeclaration>() {
        @Override
        public int count(CDeclaration declaration) {
          if (declaration instanceof CVariableDeclaration) {
            CType canonical = declaration.getType().getCanonicalType();
            if (canonical instanceof CSimpleType) {
              CSimpleType simple = (CSimpleType)canonical;
              if (simple.getType() == CBasicType.FLOAT || simple.getType() == CBasicType.DOUBLE) {
                return 1;
              }
            }
          }
          return 0;
        }
      });
    }
  };

  public static SimpleIntProvider getFloatVariablesCountProvider(MergeOption option) {
    return new SimpleIntProvider(floatVariablesCountProvider, getMerger(option), 0);
  }

  private static boolean isBitwiseOperation(CBinaryExpression exp) {
    switch (exp.getOperator()){
    case BINARY_AND:
    case BINARY_OR:
    case BINARY_XOR:
    case SHIFT_LEFT:
    case SHIFT_RIGHT: {
      // TODO: check if custom overload (ie no real bitwise operation) = types are ok
      return true;
    }
    default: break;
    }
    return false;
  }

  public static SimpleIntProviderImplementation bitwiseOperationCountProvider = new SimpleIntProviderImplementation() {
    @Override
    public String getPropertyName() {
      return "bitwiseOperationCount";
    }

    @Override
    public int calculateNext(int pCurrent, CFAEdge edge) {
      return pCurrent +
            countExpressions(edge, new Counter<CExpression>() {
              @Override
              public int count(CExpression pExpression) {
                if (pExpression instanceof CBinaryExpression) {
                  CBinaryExpression binexp = (CBinaryExpression)pExpression;
                  if (isBitwiseOperation(binexp)) {
                    return 1;
                  }
                }
                return 0;
              }
            });
    }
  };
  public static SimpleIntProvider getBitwiseOperationCountProvider(MergeOption option) {
    return new SimpleIntProvider(bitwiseOperationCountProvider, getMerger(option), 0);
  }


  public static SimpleIntProviderImplementation dereferenceCountProvider = new SimpleIntProviderImplementation() {
    @Override
    public String getPropertyName() {
      return "dereferenceCount";
    }

    @Override
    public int calculateNext(int pCurrent, CFAEdge edge) {
      return pCurrent +
            countExpressions(edge, new Counter<CExpression>() {
              @Override
              public int count(CExpression pExpression) {
                if (pExpression instanceof CPointerExpression) {
                  return 1;
                }
                return 0;
              }
            });
    }
  };
  public static SimpleIntProvider getDereferenceCountProvider(MergeOption option) {
    return new SimpleIntProvider(dereferenceCountProvider, getMerger(option), 0);
  }


  private static int countAssumeStmts(CFAEdge pEdge) {
    int count = 0;
    switch (pEdge.getEdgeType()) {
    case AssumeEdge:
      count += 1;
      break;
    case MultiEdge:
      MultiEdge multEdge = (MultiEdge) pEdge;
      for (CFAEdge edge : multEdge) {
        count += countAssumeStmts(edge);
      }
      break;
    }
    return count;
  }
  public static SimpleIntProviderImplementation assumeCountProvider = new SimpleIntProviderImplementation() {
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
    return new SimpleIntProvider(assumeCountProvider, getMerger(option), 0);
  }





  private static boolean isArithmeticOperation(CBinaryExpression exp) {
    switch (exp.getOperator()){
    case DIVIDE:
    case MINUS:
    case MODULO:
    case MULTIPLY:
    case PLUS: {
      // TODO: check if custom overload (ie no real arithmetic operation) = types are ok
      return true;
    }
    default: break;
    }
    return false;
  }

  public static SimpleIntProviderImplementation arithmeticOperationCountProvider = new SimpleIntProviderImplementation() {
    @Override
    public String getPropertyName() {
      return "arithmeticOperationCount";
    }

    @Override
    public int calculateNext(int pCurrent, CFAEdge edge) {
      return pCurrent +
            countExpressions(edge, new Counter<CExpression>() {
              @Override
              public int count(CExpression pExpression) {
                if (pExpression instanceof CBinaryExpression) {
                  CBinaryExpression binexp = (CBinaryExpression)pExpression;
                  if (isArithmeticOperation(binexp)) {
                    return 1;
                  }
                }
                return 0;
              }
            });
    }
  };
  public static SimpleIntProvider getArithmeticOperationCountProvider(MergeOption option) {
    return new SimpleIntProvider(arithmeticOperationCountProvider, getMerger(option), 0);
  }
}
