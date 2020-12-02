// This file is part of CPAchecker,
// a tool for configurable software verification:
// https://cpachecker.sosy-lab.org
//
// SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
//
// SPDX-License-Identifier: Apache-2.0

typedef unsigned char __uint8_t;
typedef __uint8_t uint8_t;
struct const_passdb
{
  char const
  *filename;
  char def[9U];
  uint8_t off[9U];
  uint8_t numfields;
  uint8_t size_of;
};
static struct const_passdb const const_pw_db = { . filename = ( char const *) "/etc/passwd" ,
    . def = "SsIIsss" ,
    . off = { [ 0U ] = 0U , 8U , [ 3U ] = 20U , [ 5U ] = 32U ,
              [ 2U ] = 16U , [ 4U ] = 24U , [ 6U ] = 40U } ,
    . numfields = 7U , . size_of = 48U };

int main() {
    char *test;
    test = malloc(10);
    if (test) {
        if ( const_pw_db.off[0] == 0  &&
             const_pw_db.off[1] == 8  &&
             const_pw_db.off[2] == 16 &&
             const_pw_db.off[3] == 20 &&
             const_pw_db.off[4] == 24 &&
             const_pw_db.off[5] == 32 &&
             const_pw_db.off[6] == 40 ) {
            free(test);
        }
    }
    return 0;
}
