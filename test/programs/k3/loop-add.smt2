; This file is part of CPAchecker,
; a tool for configurable software verification:
; https://cpachecker.sosy-lab.org
;
; SPDX-FileCopyrightText: 2025 Dirk Beyer <https://www.sosy-lab.org>
;
; SPDX-License-Identifier: Apache-2.0

(set-logic LIA)

(define-proc
    add
    ((x0 Int) (y0 Int))
    ((x Int))
    ((y Int))
    (!
        (sequence
            (assign (x x0) (y y0))
            (!
                (while
                    (< 0 y)
                    (assign
                        (x (+ x 1))
                        (y (- y 1))))
                :tag while-1))
        :tag proc-add))

(annotate-tag
    proc-add
    :requires (<= 0 y0)
    :ensures  (= x (+ x0 y0)))

(declare-const x0 Int)
(declare-const y0 Int)

(verify-call
    add (x0 y0))
(get-proof)
