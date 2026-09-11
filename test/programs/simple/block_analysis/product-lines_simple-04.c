// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2011-2013 Alexander von Rhein, University of Passau
// SPDX-FileCopyrightText: 2011-2021 The SV-Benchmarks Community
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0
//
// This is a heavily modified and simplified abstraction of the "email" product
// line in SV-Benchmarks, closest to
// product-lines/email_spec9_productSimulator.cil.c

extern void __assert_fail(const char *, const char *, unsigned int, const char *);
extern int __VERIFIER_nondet_int(void);

void reach_error() {
  __assert_fail("0", "reduced.c", 5, "reach_error");
}

// features
int f_encrypt;
int f_forward;
int f_signed;

// state
int msg;
int signed_flag;
int processed;

int valid_product() {
  if (!f_signed && !f_forward)
    return 0;
  return 1;
}

void send() {
  if (msg == 0 && (f_encrypt || f_forward))
    msg = 1;
}

void sign() {
  if (msg == 1 && f_signed)
    signed_flag = 1;
}

void forward() {
  if (msg == 1 && f_forward)
    msg = 2;
}

void process() {
  if (msg == 2 && signed_flag)
    processed = 1;
}

int main() {
  f_encrypt = __VERIFIER_nondet_int();
  f_forward = __VERIFIER_nondet_int();
  f_signed  = __VERIFIER_nondet_int();

  msg = 0;
  signed_flag = 0;
  processed = 0;

  if (!valid_product())
    return 0;

  int i = 0;
  while (i < 5) {
    int choice = __VERIFIER_nondet_int();

    if (choice == 0)
      send();
    else if (choice == 1)
      sign();
    else if (choice == 2)
      forward();
    else
      process();

    i++;
  }

  /* spec: processed messages must be encrypted */
  if (processed && !f_encrypt) {
    reach_error();
  }

  return 0;
}
