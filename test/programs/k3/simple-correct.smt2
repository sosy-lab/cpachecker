; This file is part of CPAchecker,
; a tool for configurable software verification:
; https://cpachecker.sosy-lab.org
;
; SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
;
; SPDX-License-Identifier: Apache-2.0

(declare-var w Int)
(declare-var z Int)
(define-proc f1 ((x Int) (y Int)) () () (sequence (assume (= x y)) (! (assume (= x y)) :assert (= x y))))
(verify-call f1 (w z))