// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

struct gsmi_buf {
   int *start ;
};
struct gsmi_device {
   struct gsmi_buf *name_buf ;
   struct gsmi_buf *param_buf ;
   int *class_cache[2U];
} gsmi_dev;

extern void *__VERIFIER_alloc(int size ) ;

static int gsmi_get_variable() 
{ 
  memset((gsmi_dev.name_buf), 0, (gsmi_dev.name_buf));
  return (0);
}

int main(void) 
{ 
  gsmi_dev.name_buf = __VERIFIER_alloc(sizeof(struct gsmi_buf));
  if (gsmi_dev.param_buf != 0) {
    ERROR: goto ERROR;
  }
  return (0);
}
