#ifndef _CONSTANTS_H
#define _CONSTANTS_H

#include "lib/stubs.h"

#define EXPRESSION_LENGTH BASE_SZ
#define NEEDLE "EX"
#define NEEDLE_SZ 2

/* Enough to fill a buffer of size EXPRESSION_LENGTH, enough to
 * contain the needle, and enough to overflow the buffer. */
#define LINE_LENGTH EXPRESSION_LENGTH + NEEDLE_SZ + 4

#endif
