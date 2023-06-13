// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// check for assertions
#include Assertion.spc
// and "ERROR" labels
#include ErrorLabel.spc

// Recognize functions such as exit() and abort() which do not return.
#include TerminatingFunctions.spc
