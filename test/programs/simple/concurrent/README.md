<!--
This file is part of CPAchecker,
a tool for configurable software verification:
https://cpachecker.sosy-lab.org

SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>

SPDX-License-Identifier: Apache-2.0
-->

This directory contains concurrent C programs with at least one call to `pthread_create`.

All files except

- `function-call-entry.i`
- `simple_two.i`
- `outer_inner_empty_struct.i`
- `subsystem.i`

were taken from the 2025 SV-Benchmarks version 
(https://gitlab.com/sosy-lab/benchmarking/sv-benchmarks/-/tree/svcomp25?ref_type=tags).
All preprocessed `.i` files were simplified and 
unnecessary type and function definitions were removed
s.t. file sizes are kept to a minimum.
The property paths of all `.yml` files were
changed to point towards the property files
in the CPAchecker repository.