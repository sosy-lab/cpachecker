// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Simple test program for code summaries.
 * Represents a number of different atomic operations
 * available in C.
 *
 * Invocation:
 *   scripts/cpa.sh -64 -config config/generateCodeSummaries.properties test/programs/codeSummaries/operations.c
 *
 * Produces the following summaries:
 *   add: (x,y) -> (x + y)
 *   subtract: (x,y) -> (x - y)
 *   multiply: (x,y) -> (x * y)
 *   divide: (x,y) -> (x / y)
 *   mod: (x,y) -> (x % y)
 *   binaryAnd: (x,y) -> (x & y)
 *   binaryNot: (x) ->  ~x
 *   binaryOr: (x,y) -> (x | y)
 *   binaryXor: (x,y) -> (x ^ y)
 *   shiftRight: (x,y) -> (x >> y)
 *   shiftLeft: (x,y) -> (x << y)
 *   logicalNot: (x) -> (0 == x)
 *   lessThan: (x,y) -> (x < y)
 *   lessThanOrEqual: (x,y) -> (x <= y)
 *   greaterThan: (x,y) -> (y < x)
 *   greaterThanOrEqual: (x,y) -> (y <= x)
 *   equals: (x) -> (x == 12)
 *   7 times: cast: (x) -> x
 *   7 times: dereference: (x) -> __retval__
 *   7 times: address: (x) -> __retval__
 *   7 times: not: (x) -> (0 == x)
 */

extern int __VERIFIER_nondet_unsigned_int();
extern int __VERIFIER_nondet_double();

double add(double x, double y)
{
  return x + y;
}

double subtract(double x, double y)
{
    return x - y;
}

double multiply(double x, double y)
{
    return x * y;
}

double divide(double x, double y)
{
    return x / y;
}

double mod(double x, double y)
{
    return x % y;
}

unsigned int binaryAnd(unsigned int x, unsigned int y)
{
    return x & y;
}

unsigned int binaryNot(unsigned int x)
{
    return ~x;
}

unsigned int binaryOr(unsigned int x, unsigned int y)
{
    return x | y;
}

unsigned int binaryXor(unsigned int x, unsigned int y)
{
    return x ^ y;
}

unsigned int shiftLeft(unsigned int x, unsigned int y)
{
    return x << y;
}

unsigned int shiftRight(unsigned int x, unsigned int y)
{
    return x >> y;
}

unsigned int logicalNot(unsigned int x)
{
    return !x;
}

unsigned int lessThanOrEqual(unsigned int x, unsigned int y)
{
    return x <= y;
}

unsigned int lessThan(unsigned int x, unsigned int y)
{
    return x < y;
}

unsigned int greaterThanOrEqual(unsigned int x, unsigned int y)
{
    return x >= y;
}

unsigned int greaterThan(unsigned int x, unsigned int y)
{
    return x > y;
}

unsigned int equals(unsigned int x, unsigned int y)
{
    return x == y;
}

unsigned int or(unsigned int x, unsigned int y)
{
    return x || y;
}

unsigned int and(unsigned int x, unsigned int y)
{
    return x && y;
}

double cast(unsigned int x)
{
    return (double) x;
}

/**
 * Apparently not supported by symbolic execution.
 * Just produces a ConstantSymbolicExpression for a SymbolicIdentifier at
 * location
 *    dereference::__retval__
 * without further information available.
 * As a result, the generated summary is
 *    dereference: (x) -> __retval__
 */
unsigned int dereference(unsigned int * x)
{
    return *x;
}

/**
 * Apparently not supported by symbolic execution.
 * Just produces a ConstantSymbolicExpression for a SymbolicIdentifier at
 * location
 *    address::__retval__
 * without further information available.
 * As a result, the generated summary is
 *    address: (x) -> __retval__
 */
unsigned int * address(unsigned int x)
{
    return &x;
}

unsigned int not(unsigned int x)
{
    return !x;
}

int main()
{
  unsigned int ui0 = __VERIFIER_nondet_unsigned_int();
  unsigned int ui1 = __VERIFIER_nondet_unsigned_int();

  double d0 = __VERIFIER_nondet_double();
  double d1 = __VERIFIER_nondet_double();

  add(ui0 + 11.0, ui1 + 5.0);
  subtract(d0, d1);
  multiply(d0, d1);
  divide(d0, d1);
  mod(ui0, ui1);

  binaryAnd(ui0, ui1);
  binaryNot(ui0);
  binaryOr(ui0, ui1);
  binaryXor(ui0, ui1);

  shiftRight(ui0, ui1);
  shiftLeft(ui0, ui1);

  logicalNot(ui0);

  lessThan(ui0, ui1);
  lessThanOrEqual(ui0, ui1);
  greaterThan(ui0, ui1);
  greaterThanOrEqual(ui0, ui1);
  equals(ui0, 12);

  /**
   * For 'and' and 'or', currently no summaries are produced.
   * The reason is that these operations introduce path branching
   * and path assumptions, as they are represented with a distinction
   * of their three or four underlying cases.
   * For each of them, the abstract state just represents the function
   * as returning the result as a constant (e.g. '1').
   * Functions only returning constants are currently not supported
   * by summary creation, and so they are simply ignored.
   */
  or(ui0, ui1);
  and(ui0, ui1);

  /**
   * For the following operations, a similar situation to the one of
   * 'and' and 'or' occurs.
   * During symbolic execution, each of them is transformed into a
   * branching in the reachability tree, and their results can differ
   * in a finite number of ways. However, for these operations, the
   * return value is not constant after selecting on of the different
   * cases, but instead still depends on the function parameter(s). As a
   * result, summaries are produced and logged. However, because these
   * function calls are represented by multiple abstract states,
   * *multiple* summaries are created.
   *
   * For memory-related operations (adress-of operator, pointer
   * dereferentiation operator), symbolic execution seems to not track
   * the underlying variable. Therefore, the produced summaries
   * are not correct, and usually look like
   *    address: (x) -> __retval__
   */
  cast(ui0);
  dereference(&ui0);
  address(ui0);
  not(ui0);

  return 0;
}
