/*
 *  CPAchecker is a tool for configurable software verification.
 *  This file is part of CPAchecker.
 *
 *  Copyright (C) 2007-2016  Dirk Beyer
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
package org.sosy_lab.cpachecker.util.expressions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import com.google.common.collect.Ordering;



public final class ExpressionTrees {

  public static Predicate<ExpressionTree> IS_CONSTANT = new Predicate<ExpressionTree>() {

    @Override
    public boolean apply(ExpressionTree pExpressionTree) {
      return isConstant(pExpressionTree);
    }

  };

  public static Predicate<ExpressionTree> IS_LEAF = new Predicate<ExpressionTree>() {

    @Override
    public boolean apply(ExpressionTree pExpressionTree) {
      return isLeaf(pExpressionTree);
    }

  };

  public static Predicate<ExpressionTree> IS_AND = new Predicate<ExpressionTree>() {

    @Override
    public boolean apply(ExpressionTree pExpressionTree) {
      return isAnd(pExpressionTree);
    }

  };

  public static Predicate<ExpressionTree> IS_OR = new Predicate<ExpressionTree>() {

    @Override
    public boolean apply(ExpressionTree pExpressionTree) {
      return isOr(pExpressionTree);
    }

  };

  private ExpressionTrees() {

  }

  public static boolean isConstant(ExpressionTree pExpressionTree) {
    return pExpressionTree.accept(new DefaultExpressionTreeVisitor<Boolean>() {

      @Override
      protected Boolean visitDefault(ExpressionTree pExpressionTree) {
        return false;
      }

      @Override
      public Boolean visitTrue() {
        return true;
      }

      @Override
      public Boolean visitFalse() {
        return true;
      }

    });
  }

  public static boolean isLeaf(ExpressionTree pExpressionTree) {
    return pExpressionTree.accept(new DefaultExpressionTreeVisitor<Boolean>() {

      @Override
      protected Boolean visitDefault(ExpressionTree pExpressionTree) {
        return false;
      }

      @Override
      public Boolean visit(LeafExpression pLeafExpression) {
        return true;
      }

      @Override
      public Boolean visitTrue() {
        return true;
      }

      @Override
      public Boolean visitFalse() {
        return true;
      }

    });
  }

  public static boolean isOr(ExpressionTree pExpressionTree) {

    return pExpressionTree.accept(new DefaultExpressionTreeVisitor<Boolean>() {

      @Override
      protected Boolean visitDefault(ExpressionTree pExpressionTree) {
        return false;
      }

      @Override
      public Boolean visit(Or pOr) {
        return true;
      }
    });
  }

  public static boolean isAnd(ExpressionTree pExpressionTree) {

    return pExpressionTree.accept(new DefaultExpressionTreeVisitor<Boolean>() {

      @Override
      protected Boolean visitDefault(ExpressionTree pExpressionTree) {
        return false;
      }

      @Override
      public Boolean visit(And pAnd) {
        return true;
      }
    });
  }

  public static boolean isInCNF(ExpressionTree pExpressionTree) {
    return pExpressionTree.accept(new ExpressionTreeVisitor<Boolean>() {

      @Override
      public Boolean visit(And pAnd) {
        return getChildren(pAnd).allMatch(new Predicate<ExpressionTree>() {

          @Override
          public boolean apply(ExpressionTree pClause) {
            // A clause may be a single literal or a disjunction of literals
            assert !isAnd(pClause) : "A conjunction must not contain child conjunctions";
            return isCNFClause(pClause);
          }

        });
      }

      @Override
      public Boolean visit(Or pOr) {
        return getChildren(pOr).allMatch(IS_LEAF);
      }

      @Override
      public Boolean visit(LeafExpression pLeafExpression) {
        // Check: One clause with one literal
        return true;
      }

      @Override
      public Boolean visitTrue() {
        // Check: One clause with one literal
        return true;
      }

      @Override
      public Boolean visitFalse() {
        // Check: One clause with one literal
        return true;
      }

    });
  }

  public static boolean isInDNF(ExpressionTree pExpressionTree) {
    return pExpressionTree.accept(new ExpressionTreeVisitor<Boolean>() {

      @Override
      public Boolean visit(And pAnd) {
        // Check: One clause with more than one literal
        return getChildren(pAnd).allMatch(IS_LEAF);
      }

      @Override
      public Boolean visit(Or pOr) {
        return getChildren(pOr).allMatch(new Predicate<ExpressionTree>() {

          @Override
          public boolean apply(ExpressionTree pClause) {
            // A clause may be a single literal or a conjunction of literals
            assert !isOr(pClause) : "A disjunction must not contain child disjunctions";
            return isDNFClause(pClause);
          }

        });
      }

      @Override
      public Boolean visit(LeafExpression pLeafExpression) {
        // Check: One clause with one literal
        return true;
      }

      @Override
      public Boolean visitTrue() {
        // Check: One clause with one literal
        return true;
      }

      @Override
      public Boolean visitFalse() {
        // Check: One clause with one literal
        return true;
      }

    });
  }

  public static Function<ExpressionTree, ExpressionTree> TO_DNF = new Function<ExpressionTree, ExpressionTree>() {

    @Override
    public ExpressionTree apply(ExpressionTree pExpressionTree) {
      return toDNF(pExpressionTree);
    }

  };

  public static ExpressionTree toDNF(ExpressionTree pExpressionTree) {
    if (isInDNF(pExpressionTree)) {
      return pExpressionTree;
    }
    if (isOr(pExpressionTree)) {
      return Or.of(getChildren(pExpressionTree).transform(TO_DNF));
    }
    assert isAnd(pExpressionTree);
    Iterator<ExpressionTree> elementIterator = getChildren(pExpressionTree).iterator();
    if (!elementIterator.hasNext()) {
      return ExpressionTree.TRUE;
    }
    ExpressionTree first = elementIterator.next();
    if (!elementIterator.hasNext()) {
      return first;
    }
    Collection<ExpressionTree> rest = new ArrayList<>();
    Iterators.addAll(rest, elementIterator);
    ExpressionTree firstDNF = toDNF(first);
    ExpressionTree restDNF = toDNF(And.of(rest));
    final Iterable<ExpressionTree> combinatorsA;
    if (isLeaf(firstDNF)) {
      combinatorsA = Collections.singleton(firstDNF);
    } else {
      combinatorsA = getChildren(firstDNF);
    }
    final Iterable<ExpressionTree> combinatorsB;
    if (isLeaf(restDNF)) {
      combinatorsB = Collections.singleton(restDNF);
    } else {
      combinatorsB = getChildren(restDNF);
    }
    Collection<ExpressionTree> newClauses = new ArrayList<>();
    for (ExpressionTree combinatorA : combinatorsA) {
      for (ExpressionTree combinatorB : combinatorsB) {
        newClauses.add(And.of(ImmutableList.<ExpressionTree>of(combinatorA, combinatorB)));
      }
    }
    return Or.of(newClauses);
  }

  public static ExpressionTree toCNF(ExpressionTree pExpressionTree) {
    if (isInCNF(pExpressionTree)) {
      return pExpressionTree;
    }
    if (isAnd(pExpressionTree)) {
      return And.of(getChildren(pExpressionTree).transform(TO_DNF));
    }
    assert isOr(pExpressionTree);
    Iterator<ExpressionTree> elementIterator = getChildren(pExpressionTree).iterator();
    if (!elementIterator.hasNext()) {
      return ExpressionTree.FALSE;
    }
    ExpressionTree first = elementIterator.next();
    if (!elementIterator.hasNext()) {
      return first;
    }
    Collection<ExpressionTree> rest = new ArrayList<>();
    Iterators.addAll(rest, elementIterator);
    ExpressionTree firstCNF = toCNF(first);
    ExpressionTree restCNF = toCNF(And.of(rest));
    final Iterable<ExpressionTree> combinatorsA;
    if (isLeaf(firstCNF)) {
      combinatorsA = Collections.singleton(firstCNF);
    } else {
      combinatorsA = getChildren(firstCNF);
    }
    final Iterable<ExpressionTree> combinatorsB;
    if (isLeaf(restCNF)) {
      combinatorsB = Collections.singleton(restCNF);
    } else {
      combinatorsB = getChildren(restCNF);
    }
    Collection<ExpressionTree> newClauses = new ArrayList<>();
    for (ExpressionTree combinatorA : combinatorsA) {
      for (ExpressionTree combinatorB : combinatorsB) {
        newClauses.add(Or.of(ImmutableList.<ExpressionTree>of(combinatorA, combinatorB)));
      }
    }
    return And.of(newClauses);
  }

  public static boolean isCNFClause(ExpressionTree pExpressionTree) {
    return isLeaf(pExpressionTree) || (isOr(pExpressionTree) && getChildren(pExpressionTree).allMatch(IS_LEAF));
  }

  public static boolean isDNFClause(ExpressionTree pExpressionTree) {
    return isLeaf(pExpressionTree) || (isAnd(pExpressionTree) && getChildren(pExpressionTree).allMatch(IS_LEAF));
  }

  public static FluentIterable<ExpressionTree> getChildren(ExpressionTree pExpressionTree) {
    return FluentIterable.from(pExpressionTree.accept(new DefaultExpressionTreeVisitor<Iterable<ExpressionTree>>() {

      @Override
      protected Iterable<ExpressionTree> visitDefault(ExpressionTree pExpressionTree) {
        return Collections.emptySet();
      }

      @Override
      public Iterable<ExpressionTree> visit(And pAnd) {
        return pAnd;
      }

      @Override
      public Iterable<ExpressionTree> visit(Or pOr) {
        return pOr;
      }

    }));
  }

  public static final Comparator<ExpressionTree> COMPARATOR = new Comparator<ExpressionTree>() {

    @Override
    public int compare(final ExpressionTree pO1, final ExpressionTree pO2) {
      int typeOrder1 = pO1.accept(TYPE_ORDER_VISITOR);
      int typeOrder2 = pO2.accept(TYPE_ORDER_VISITOR);
      final int typeOrderComp = typeOrder1 - typeOrder2;
      final Ordering<Iterable<ExpressionTree>> lexicographicalOrdering = Ordering.<ExpressionTree>from(this).lexicographical();
      return pO1.accept(new ExpressionTreeVisitor<Integer>() {

        @Override
        public Integer visit(And pAnd) {
          if (pO2 instanceof And) {
            And other = (And) pO2;
            return lexicographicalOrdering.compare(pAnd, other);
          }
          return typeOrderComp;
        }

        @Override
        public Integer visit(Or pOr) {
          if (pO2 instanceof Or) {
            Or other = (Or) pO2;
            return lexicographicalOrdering.compare(pOr, other);
          }
          return typeOrderComp;
        }

        @Override
        public Integer visit(LeafExpression pLeafExpression) {
          if (pO2 instanceof LeafExpression) {
            LeafExpression other = (LeafExpression) pO2;
            return pLeafExpression.toString().compareTo(other.toString());
          }
          return typeOrderComp;
        }

        @Override
        public Integer visitTrue() {
          if (pO2.equals(ExpressionTree.TRUE)) {
            return 0;
          }
          return typeOrderComp;
        }

        @Override
        public Integer visitFalse() {
          if (pO2.equals(ExpressionTree.FALSE)) {
            return 0;
          }
          return typeOrderComp;
        }

      });
    }

  };

  private static final ExpressionTreeVisitor<Integer> TYPE_ORDER_VISITOR = new ExpressionTreeVisitor<Integer>() {

    @Override
    public Integer visitFalse() {
      return 0;
    }

    @Override
    public Integer visitTrue() {
      return 1;
    }

    @Override
    public Integer visit(LeafExpression pLeafExpression) {
      return 2;
    }

    @Override
    public Integer visit(Or pOr) {
      return 3;
    }

    @Override
    public Integer visit(And pAnd) {
      return 4;
    }

  };

}
