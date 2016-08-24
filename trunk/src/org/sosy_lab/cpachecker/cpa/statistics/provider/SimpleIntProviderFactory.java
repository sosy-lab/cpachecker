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
import org.sosy_lab.cpachecker.cfa.types.c.CType;
import org.sosy_lab.cpachecker.cpa.statistics.provider.SimpleIntProvider.IntMerger;
import org.sosy_lab.cpachecker.cpa.statistics.provider.SimpleIntProvider.SimpleIntProviderImplementation;
import org.sosy_lab.cpachecker.util.CFAUtils;

import java.util.stream.StreamSupport;

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
  private static int countExpressions(CAstNode pExpression, Counter<CExpression> counter) {
    return StreamSupport.stream(
            CFAUtils.traverseRecursively(pExpression).filter(CExpression.class).spliterator(),
            false)
        .mapToInt(counter::count)
        .sum();
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

    default:
      // no declaration
      break;
    }

    return count;
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
            count += countExpressions(init, counter);
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
        count += countExpressions(stmt, counter);
        break;
    case ReturnStatementEdge:
      CReturnStatementEdge returnEdge = (CReturnStatementEdge) pEdge;

      if (returnEdge.getExpression().isPresent()) {
        count += countExpressions(returnEdge.getExpression().get(), counter);
      }
      break;

    default:
      // no expressions
      break;
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

  private static final SimpleIntProviderImplementation edgeCountProvider = new SimpleIntProviderImplementation() {
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


  private static final SimpleIntProviderImplementation gotoCountProvider = new SimpleIntProviderImplementation() {
    @Override
    public String getPropertyName() {
      return "gotoCount";
    }

    @Override
    public int calculateNext(int pCurrent, CFAEdge edge) {
      if (edge.getEdgeType() == CFAEdgeType.BlankEdge && edge.getDescription().startsWith("Goto: ")) {
        return pCurrent + 1;
      }
      return pCurrent;
    }
  };

  public static SimpleIntProvider getGotoCountProvider(MergeOption option) {
    return new SimpleIntProvider(gotoCountProvider, option, 0);
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
        if (loopHeads.contains(succ)) {
          // Now we have to check that are are not already within the loop
          if (succ.getNodeNumber() > pred.getNodeNumber()) {
            // NOTE: Not really a very sophisticated test, but fast and worked in my test cases
            return pCurrent + 1;
          }
        }
        return pCurrent;
      }
    }, option, 0);
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
        if (stmt instanceof CFunctionCall) {
          count++;
        }
        break;

    default:
      // no function calls
      break;
    }
    return count;
  }
  private static final SimpleIntProviderImplementation functionCallCountProvider = new SimpleIntProviderImplementation() {
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

  private static final SimpleIntProviderImplementation branchCountProvider = new SimpleIntProviderImplementation() {
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

  private static final SimpleIntProviderImplementation jumpCountProvider = new SimpleIntProviderImplementation() {
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


  private static final SimpleIntProviderImplementation functionDefCountProvider = new SimpleIntProviderImplementation() {
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
    return new SimpleIntProvider(functionDefCountProvider, option, 0);
  }


  private static final SimpleIntProviderImplementation localVariablesCountProvider = new SimpleIntProviderImplementation() {
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
    return new SimpleIntProvider(localVariablesCountProvider, option, 0);
  }


  private static final SimpleIntProviderImplementation globalVariablesCountProvider = new SimpleIntProviderImplementation() {
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
    return new SimpleIntProvider(globalVariablesCountProvider, option, 0);
  }



  private static final SimpleIntProviderImplementation structVariablesCountProvider = new SimpleIntProviderImplementation() {
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
    return new SimpleIntProvider(structVariablesCountProvider, option, 0);
  }


  private static final SimpleIntProviderImplementation pointerVariablesCountProvider = new SimpleIntProviderImplementation() {
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
    return new SimpleIntProvider(pointerVariablesCountProvider, option, 0);
  }


  private static final SimpleIntProviderImplementation arrayVariablesCountProvider = new SimpleIntProviderImplementation() {
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
    return new SimpleIntProvider(arrayVariablesCountProvider, option, 0);
  }


  private static final SimpleIntProviderImplementation integerVariablesCountProvider = new SimpleIntProviderImplementation() {
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
    return new SimpleIntProvider(integerVariablesCountProvider, option, 0);
  }


  private static final SimpleIntProviderImplementation floatVariablesCountProvider = new SimpleIntProviderImplementation() {
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
    return new SimpleIntProvider(floatVariablesCountProvider, option, 0);
  }

  private static boolean isBitwiseOperation(CBinaryExpression exp) {
    switch (exp.getOperator()) {
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

  private static final SimpleIntProviderImplementation bitwiseOperationCountProvider = new SimpleIntProviderImplementation() {
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
    return new SimpleIntProvider(bitwiseOperationCountProvider, option, 0);
  }


  private static final SimpleIntProviderImplementation dereferenceCountProvider = new SimpleIntProviderImplementation() {
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
    return new SimpleIntProvider(dereferenceCountProvider, option, 0);
  }


  private static int countAssumeStmts(CFAEdge pEdge) {
    int count = 0;
    switch (pEdge.getEdgeType()) {
    case AssumeEdge:
      count += 1;
      break;

    default:
      // no assume
      break;
    }
    return count;
  }
  private static final SimpleIntProviderImplementation assumeCountProvider = new SimpleIntProviderImplementation() {
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

  private static final SimpleIntProviderImplementation arithmeticOperationCountProvider = new SimpleIntProviderImplementation() {
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
    return new SimpleIntProvider(arithmeticOperationCountProvider, option, 0);
  }
}
