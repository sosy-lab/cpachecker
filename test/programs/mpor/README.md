<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

This directory contains C programs to unit test the sequentialization implementation.

The `input_rejections` dir contains custom programs used to test if certain program characteristics 
are correctly identified and rejected by the implementation.

The `sequentialization` dir contains programs that are used to test :
- that a wide variety of algorithm option combinations parses (i.e. is accepted by CPAchecker)
- that the number of threads, memory locations, pointer dereferences, ... identified by the algorithm is correct

All files within `sequentialization` except

- `function-call-entry.i`
- `simple_two.i`
- `outer_inner_empty_struct.i`
- `subsystem.i`

were taken from the 2025 SV-Benchmarks version 
(https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks/-/tree/svcomp25?ref_type=tags):

- `13-privatized_04-priv_multi_true.i`: https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks/-/blob/svcomp25/c/goblint-regression/13-privatized_04-priv_multi_true.i?ref_type=tags
- `13-privatized_69-refine-protected-loop-interval_true.i`: https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks/-/blob/svcomp25/c/goblint-regression/13-privatized_69-refine-protected-loop-interval_true.i?ref_type=tags
- `28-race_reach_45-escape_racing.i`: https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks/-/blob/svcomp25/c/goblint-regression/28-race_reach_45-escape_racing.i?ref_type=tags
- `36-apron_41-threadenter-no-locals_unknown_1_pos.i`: https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks/-/blob/svcomp25/c/goblint-regression/36-apron_41-threadenter-no-locals_unknown_1_pos.i?ref_type=tags
- `array-eq-symm.wvr.c`: https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks/-/blob/svcomp25/c/weaver/array-eq-symm.wvr.c?ref_type=tags
- `chl-match-symm.wvr.c`: https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks/-/blob/svcomp25/c/weaver/chl-match-symm.wvr.c?ref_type=tags
- `fib_safe-7.i`: https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks/-/blob/svcomp25/c/pthread/fib_safe-7.i?ref_type=tags
- `lazy01.i`: https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks/-/blob/svcomp25/c/pthread/lazy01.i?ref_type=tags
- `queue_longest.i`: https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks/-/blob/svcomp25/c/pthread/queue_longest.i?ref_type=tags
- `singleton_with-uninit-problems-b.i`: https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks/-/blob/svcomp25/c/pthread/singleton_with-uninit-problems-b.i?ref_type=tags
- `stack-1.i`: https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks/-/blob/svcomp25/c/pthread/stack-1.i?ref_type=tags

All preprocessed `.i` files were simplified and 
unnecessary type and function definitions were removed
s.t. file sizes are kept to a minimum.