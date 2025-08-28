// This file is part of the SV-Benchmarks collection of verification tasks:
// https://github.com/sosy-lab/sv-benchmarks
//
// SPDX-FileCopyrightText: 2011-2020 The SV-Benchmarks community
// SPDX-FileCopyrightText: 2020 The ESBMC project
//
// SPDX-License-Identifier: Apache-2.0

extern void abort(void);
void assume_abort_if_not(int cond) {
  if(!cond) {abort();}
}
extern unsigned long __VERIFIER_nondet_ulong(void);
extern int __VERIFIER_nondet_int();
// Copyright (c) 2015 Michael Tautschnig <michael.tautschnig@qmul.ac.uk>
// 
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
//     http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.


/*
VerifyThis ETAPS 2015, Challenge 1
Challenge submitted by Thomas Genet (http://www.irisa.fr/celtique/genet/)

RELAXED PREFIX (60 minutes)
===========================


Description
-----------

Verify a function isRelaxedPrefix determining if a list _pat_ (for
pattern) is a relaxed prefix of another list _a_.

The relaxed prefix property holds iff _pat_ is a prefix of _a_ after
removing at most one element from _pat_.


Examples
--------

pat = {1,3}   is a relaxed prefix of a = {1,3,2,3} (standard prefix)

pat = {1,2,3} is a relaxed prefix of a = {1,3,2,3} (remove 2 from pat)

pat = {1,2,4} is not a relaxed prefix of a = {1,3,2,3}.


Implementation notes
--------------------

You can implement lists as arrays, e.g., of integers. A reference
implementation is given below. It may or may not contain errors.


public class Relaxed {

    public static boolean isRelaxedPrefix(int[] pat, int[] a) {
        int shift = 0;

        for(int i=0; i<pat.length; i++) {
            if (pat[i]!=a[i-shift]) 
                if (shift==0) shift=1;
                    else return false;
        }
        return true;
    }
    
    
    public static void main(String[] argv) {
        int[] pat = {1,2,3};
        int[] a1 = {1,3,2,3};
        System.out.println(isRelaxedPrefix(pat, a1));
    }

}



Advanced verification task (if you get bored)
---------------------------------------------

Implement and verify a function relaxedContains(pat, a) returning
whether _a_ contains _pat_ in the above relaxed sense, i.e., whether
_pat_ is a relaxed prefix of any suffix of _a_.
*/

extern void abort(void);
#include <assert.h>
void reach_error() { assert(0); }

void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: {reach_error();abort();}
  }
  return;
}

#include <stdlib.h>

_Bool is_relaxed_prefix(
  int pat[], unsigned long pat_length,
  int a[], unsigned long a_length)
{
  if(pat_length>a_length+1)
    return 0;

  int shift=0;

  for(int i=0; i<pat_length && i<a_length; i++)
  {
    if(pat[i]!=a[i-shift])
    {
      if(shift==0)
        shift=1;
      else
        return 0;
    }
  }

  if(pat_length>a_length && shift==1)
    return 0;
  else
    return 1;
}

int main()
{
  unsigned long pat_len = __VERIFIER_nondet_ulong(), a_len = __VERIFIER_nondet_ulong();

  int *pat=malloc(sizeof(int)*pat_len);
  int *a=malloc(sizeof(int)*a_len);
  //int pat[]={1,3};
  //int pat[]={1,2,3};
  //int pat[]={1,2,4};
  //int a[]={1,3,2,3};

  if(is_relaxed_prefix(pat, pat_len, a, a_len))
  {
    __VERIFIER_assert(pat_len<=a_len+1);
    unsigned long different = __VERIFIER_nondet_ulong();
    if(pat_len>a_len)
      different=pat_len-1;
    for(int i=0; i<pat_len && i<a_len; i++)
    {
		    pat[i] = __VERIFIER_nondet_int();
				a[i] = __VERIFIER_nondet_int();
        if(i<different)
          assume_abort_if_not(pat[i]==a[i]);
        else if(i==different)
          assume_abort_if_not(pat[i]!=a[i]);
        else if(i>different)
          __VERIFIER_assert(pat[i]==a[i-1]);
    }
  }
  else if(pat_len<=a_len+1)
  {
    unsigned long differences=0;
    for(int i=0; i<pat_len && i<a_len; i++)
    {
      if(pat[i]!=a[i-differences])
        ++differences;
    }
    if(pat_len>a_len)
      ++differences;
    __VERIFIER_assert(differences>1);
  }

  free(pat);
  free(a);
  return 0;
}

