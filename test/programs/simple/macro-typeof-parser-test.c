// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

void main(char *opts, ...) {
  void **optarg;
  __builtin_va_list p;
  __builtin_va_start(p,opts);
  optarg=__builtin_va_arg(p,__typeof__(optarg));
  __builtin_va_end(p);
}
