// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

/**
 * Simple test program for code summaries.
 * Represents a simple incrementation.
 *
 * Invocation:
 *   scripts/cpa.sh -64 -config config/generateCodeSummaries.properties test/programs/codeSummaries/increment.c
 *
 * Produces the following summaries:
 *   inc: (x) -> (x + 1)
 *   main: ( ) -> (z + 17.0 + 1)
 */

extern int __VERIFIER_nondet_double();

unsigned int inc(unsigned int x)
{
  return x + 1;
}

int main()
{
  double d0 = __VERIFIER_nondet_double();
  return inc(d0 + 17.0);
}
