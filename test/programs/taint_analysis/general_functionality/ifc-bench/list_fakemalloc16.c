// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2024 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Benchmark was collected from http://www.cs.princeton.edu/∼aartig/benchmarks/ifc bench.zip.
// from the paper "Lazy Self-composition for Security Verification"

//---------------------------------------------------------------------------//
// verifier functions                                                        //
//---------------------------------------------------------------------------//

extern void __VERIFIER_error (void);
extern void ifc_set_secret(int c, ...);
extern void ifc_check_out(int c, ...);
extern void ifc_set_low(int c, ...);
extern void ifc_check_live(int c, ...);
extern void __VERIFIER_assume(int);
extern int __VERIFIER_NONDET();
#define nd __VERIFIER_NONDET
#define assume(X) __VERIFIER_assume(X)
#define assert(X) if(!(X)) __VERIFIER_error();

#define NULL 0

//---------------------------------------------------------------------------//
// types                                                                     //
//---------------------------------------------------------------------------//
typedef int pid_t;

struct tcb_node_t 
{
    pid_t pid;
    int data; // some fake data.
    struct tcb_node_t* next;
};

//---------------------------------------------------------------------------//
// globals                                                                   //
//---------------------------------------------------------------------------//
pid_t current_pid;
struct tcb_node_t* tcb_head = NULL;
struct tcb_node_t* tcb_tail = NULL;

#define MAX_NODES (3*16)
struct tcb_node_t list_nodes[MAX_NODES];
int list_node_ptr = 0;

//---------------------------------------------------------------------------//
// functions                                                                 //
//---------------------------------------------------------------------------//
struct tcb_node_t* fake_malloc()
{
    int index = list_node_ptr;
    assert (index < MAX_NODES);
    list_node_ptr++;

    return &list_nodes[index];
}

void add_thread(int thread_info_data) {
    // create a new node.
    struct tcb_node_t * node = fake_malloc();
    assume (node != NULL);
    // fill-in fields.
    node->pid = current_pid;
    node->data = thread_info_data;
    node->next = NULL;
    // insert to list.
    if (tcb_head == NULL) {
        // alloc head.
        tcb_head = tcb_tail = node;
    } else {
        tcb_tail->next = node;
        tcb_tail = node;
    }
}

int count_my_threads() {
    struct tcb_node_t* node = tcb_head;
    int cnt = 0;

    // traverse list.
    for (;node != NULL; node = node->next) {
        if (node->pid == current_pid) { cnt += 1; }
    }
    return cnt;
}

int main() {
    int N = 4;
    ifc_check_live(1, N);
    int list_entries[N];
    int list_size;
    int i, mypid, result;

//    mypid = nd()nd();
    mypid = __VERIFIER_nondet_int();
    ifc_set_low(1, mypid);
//    list_size = nd();
    list_size = __VERIFIER_nondet_int();
    int a0, a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15;

    a0 = __VERIFIER_nondet_int();
    a1 = __VERIFIER_nondet_int();
    a2 = __VERIFIER_nondet_int();
    a3 = __VERIFIER_nondet_int();
    a4 = __VERIFIER_nondet_int();
    a5 = __VERIFIER_nondet_int();
    a6 = __VERIFIER_nondet_int();
    a7 = __VERIFIER_nondet_int();
    a8 = __VERIFIER_nondet_int();
    a9 = __VERIFIER_nondet_int();
    a10 = __VERIFIER_nondet_int();
    a11 = __VERIFIER_nondet_int();
    a12 = __VERIFIER_nondet_int();
    a13 = __VERIFIER_nondet_int();
    a14 = __VERIFIER_nondet_int();
    a15 = __VERIFIER_nondet_int();
    __VERIFIER_set_public(a0, 0);
    __VERIFIER_set_public(a1, 0);
    __VERIFIER_set_public(a2, 0);
    __VERIFIER_set_public(a3, 0);
    __VERIFIER_set_public(a4, 0);
    __VERIFIER_set_public(a5, 0);
    __VERIFIER_set_public(a6, 0);
    __VERIFIER_set_public(a7, 0);
    __VERIFIER_set_public(a8, 0);
    __VERIFIER_set_public(a9, 0);
    __VERIFIER_set_public(a10, 0);
    __VERIFIER_set_public(a11, 0);
    __VERIFIER_set_public(a12, 0);
    __VERIFIER_set_public(a13, 0);
    __VERIFIER_set_public(a14, 0);
    __VERIFIER_set_public(a15, 0);
    list_entries[0] = a0;
    list_entries[1] = a1;
    list_entries[2] = a2;
    list_entries[3] = a3;
    list_entries[4] = a4;
    list_entries[5] = a5;
    list_entries[6] = a6;
    list_entries[7] = a7;
    list_entries[8] = a8;
    list_entries[9] = a9;
    list_entries[10] = a10;
    list_entries[11] = a11;
    list_entries[12] = a12;
    list_entries[13] = a13;
    list_entries[14] = a14;
    list_entries[15] = a15;

//    ifc_set_secret(1, list_size);
    __VERIFIER_set_public(list_size, 0);

    assume(list_size < 16);
    current_pid = __VERIFIER_nondet_int();
    ifc_set_low(1, mypid);
    assume(current_pid != mypid);
    for(i=0; i < list_size; i++) {
        add_thread(list_entries[i]);
    }

    // now the low code runs.
    current_pid = mypid;
//    while (nd()) {
    while (__VERIFIER_nondet_int()) {
//        add_thread(nd());
        add_thread(__VERIFIER_nondet_int());
    }
    // result must be public.
    result = count_my_threads();
//    ifc_check_out(1, result);
    __VERIFIER_is_public(result, 1);
}

