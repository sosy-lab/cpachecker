// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2026 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

// Array designators may be arbitrary integer constant expressions, e.g., enumerators
// with an offset like in the error tables of the AWS C Common library.

enum error_code { ERROR_SUCCESS = 0, ERROR_OOM, ERROR_UNKNOWN, ERROR_END };

struct error_info {
  int code;
  const char *name;
};

static struct error_info errors[] = {
  [(ERROR_SUCCESS)-0x0000] = { .code = ERROR_SUCCESS, .name = "ERROR_SUCCESS" },
  [(ERROR_UNKNOWN)-0x0000] = { .code = ERROR_UNKNOWN, .name = "ERROR_UNKNOWN" },
  [(ERROR_OOM)-0x0000] = { .code = ERROR_OOM, .name = "ERROR_OOM" },
};

int main(void) {
  // Only the size is checked for range designators, the values are not supported precisely yet.
  int range[] = { [ERROR_OOM ... (ERROR_END)-0x0000] = 1 };
  int values[8] = { [2 * ERROR_OOM + 2] = 3, [ERROR_OOM] = 1, [ERROR_UNKNOWN] = 2, [ERROR_END] = 4 };

  if (sizeof(errors) != ERROR_END * sizeof(struct error_info)
      || sizeof(range) != (ERROR_END + 1) * sizeof(int)) {
    goto ERROR;
  }
  if (errors[ERROR_SUCCESS].code != ERROR_SUCCESS || errors[ERROR_OOM].code != ERROR_OOM
      || errors[ERROR_UNKNOWN].code != ERROR_UNKNOWN) {
    goto ERROR;
  }
  if (values[0] != 0 || values[1] != 1 || values[2] != 2 || values[3] != 4 || values[4] != 3 || values[5] != 0) {
    goto ERROR;
  }
  return 0;

ERROR:
  return 1;
}
