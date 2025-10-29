// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.ication that
// all tags in a K3 program are valid
// category Overflows of th
// Competition on Software Verification.
CONTROL AUTOMATON ValidTags

INITIAL STATE Init;

STATE USEFIRST Init :
  CHECK("valid-tags") -> ERROR("specification violation of program in $location");

END AUTOMATON
