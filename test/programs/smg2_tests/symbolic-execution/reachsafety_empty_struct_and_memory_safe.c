// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2023 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

#include <stdlib.h>
#include <assert.h>
#include <stdio.h>

extern int __VERIFIER_nondet_int(void);
extern void abort(void);

struct mbuf;
struct sockaddr;
struct socket;
struct domain;
struct proc;

struct protosw {
  short pr_type;
  struct domain *pr_domain;
  short pr_protocol;
  short pr_flags;
  int (*pr_input)(struct mbuf **, int *, int, int);
  int (*pr_output)(struct mbuf *, struct socket *, struct sockaddr *,
                   struct mbuf *);
  void (*pr_ctlinput)(int, struct sockaddr *, unsigned int, void *);
  int (*pr_ctloutput)(int, struct socket *, int, int, struct mbuf *);
  int (*pr_usrreq)(struct socket *, int, struct mbuf *, struct mbuf *,
                   struct mbuf *, struct proc *);
  int (*pr_attach)(struct socket *, int);
  void (*pr_init)(void);
  void (*pr_fasttimo)(void);
  void (*pr_slowtimo)(void);
  void (*pr_drain)(void);
  int (*pr_sysctl)(int *, unsigned int, void *, size_t *, void *, size_t);
};

// null initialized array size 0 == null
int garr_0[] = {};

// null initialized array size 10
int garr_10[10] = {};
    
// null initialized struct of type protosw
struct protosw gempty_struct = {};

// Array of size 1 with 1 null initialized struct of type protosw inside
struct protosw glist_of_empty_struct[] = {{}};

// Test empty struct/memory etc. init
// Note: we want the array size info ALWAYS in the CFA!
// If we encounter a scenario in which this is not the case, report to Philipp/issue!

// This file is invalid in strict C11 but ok for GCC
// Note: check this file with -Wpedantic -std=c11 to see that all empty initializers are not part of the C standard (they are in C23 ;D)
// Check with -Wgnu-empty-initializer to see the extension used
int main() {

    assert(sizeof(garr_0) == 0);
    assert(garr_0 != 0);

    for (int i = 0; i < 10; i++) {
        assert(garr_10[i] == 0);
    }
    assert(sizeof(garr_10) == 10*sizeof(int));
    assert(garr_10 != 0);
    
    assert(gempty_struct.pr_type == 0);
    assert(gempty_struct.pr_sysctl == 0);

    assert(glist_of_empty_struct[0].pr_type == 0);
    assert(glist_of_empty_struct[0].pr_sysctl == 0);
    assert(sizeof(glist_of_empty_struct) == sizeof(struct protosw));

    // array length 0 with valid address (not null)
    int arr_0[] = {};
    // printf("%ld", sizeof(arr_0)); // 0
    // printf("%d", arr_0 == 0); // 0
    // printf("%ld", (unsigned long) arr_0); // some address

    assert(sizeof(arr_0) == 0);
    assert(arr_0 != 0);

    // null initialized array length 10
	int arr_10[10] = {};
    for (int i = 0; i < 10; i++) {
        assert(arr_10[i] == 0);
    }
    assert(sizeof(arr_10) == 10*sizeof(int));
    assert(arr_10 != 0);
    
    // null initialized struct of type protosw
    // padding is also 0
    struct protosw empty_struct = {};

    assert(empty_struct.pr_type == 0);
    assert(empty_struct.pr_sysctl == 0);

    // Array of size 1 with 1 null initialized struct of type protosw inside
    struct protosw list_of_empty_struct[] = {{}};
    
    assert(list_of_empty_struct[0].pr_type == 0);
    assert(list_of_empty_struct[0].pr_sysctl == 0);
    assert(sizeof(list_of_empty_struct) == sizeof(struct protosw));

    // Use pointers to these (valid) constructs

    assert(sizeof(*&arr_0) == 0);
    assert(*&arr_0 != 0);

    for (int i = 0; i < 10; i++) {
        assert(*&arr_10[i] == 0);
    }
    assert(sizeof(*&arr_10) == 10*sizeof(int));
    assert(*&arr_10 != 0);
    
    assert(*&empty_struct.pr_type == 0);
    assert(*&empty_struct.pr_sysctl == 0);

    assert(*&list_of_empty_struct[0].pr_type == 0);
    assert(*&list_of_empty_struct[0].pr_sysctl == 0);
    assert(sizeof(*&list_of_empty_struct) == sizeof(struct protosw));

    // SAFE
	return 0;	
}
