// This file is part of the SV-Benchmarks collection of verification tasks:
// https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks
//
// SPDX-FileCopyrightText: 2011-2013 Alexander von Rhein, University of Passau
// SPDX-FileCopyrightText: 2011-2021 The SV-Benchmarks Community
//
// SPDX-License-Identifier: Apache-2.0

extern void abort(void);
void reach_error() {}

extern int __VERIFIER_nondet_int(void);

typedef unsigned int size_t;

int main(void) 
{  
  int methAndRunningLastTime = 0 ;
  int pumpRunning  =    0;
  int methaneLevelCritical  =    0;
  while(1) {
    if (__VERIFIER_nondet_int()) {
      methaneLevelCritical = !methaneLevelCritical;
    }
    if ((! pumpRunning) && (!methaneLevelCritical)) {
        pumpRunning = 1;
    } 
    if (methaneLevelCritical && pumpRunning) {
        if (methAndRunningLastTime) {
          reach_error();abort();
        } else {
          methAndRunningLastTime = 1;
        }
    } else {
      methAndRunningLastTime = 0;
    }
  }
  return 0;
}
