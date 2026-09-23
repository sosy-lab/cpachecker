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

The `sequentialization` dir contains programs that are used to test:
- that a wide variety of algorithm option combinations parses (i.e. is accepted by CPAchecker)
- that the number of threads, memory locations, pointer dereferences, ... identified by the algorithm is correct

All files within `sequentialization` except

- `function-call-entry.i`
- `simple_two.i`
- `outer_inner_empty_struct.i`
- `subsystem.i`

were taken from the 2025 SV-Benchmarks version 
(https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks/-/tree/svcomp25?ref_type=tags).
All preprocessed `.i` files were simplified and 
unnecessary type and function definitions were removed
s.t. file sizes are kept to a minimum.