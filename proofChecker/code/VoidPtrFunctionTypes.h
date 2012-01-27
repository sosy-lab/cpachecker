
#ifndef VoidPtrFUNCTIONTYPES_H
#define VoidPtrFUNCTIONTYPES_H

#include "eliproto.h"

/* include header file defining VoidPtr if VoidPtr is set: */
#define EMPTYVoidPtrHDR
#ifndef EMPTYHDR
#include "VoidPtr.h"
#endif
#undef EMPTYVoidPtrHDR

typedef int (*VoidPtrCmpFctType) ELI_ARG((VoidPtr, VoidPtr));
/* Functions that compare two VoidPtr values
 *   If the left argument is less than the right then on exit-
 *     VoidPtrCmpFctType = -1
 *   Else if the arguments are equal then on exit-
 *     VoidPtrCmpFctType = 0
 *   Else on exit-
 *     VoidPtrCmpFctType = 1
 ***/

typedef VoidPtr (*VoidPtrMapFct) ELI_ARG((VoidPtr));
/* Functions that map one VoidPtr value into another
 *   On exit-
 *     VoidPtrMapFct = image of the argument under the map
 ***/

typedef VoidPtr (*VoidPtrSumFct) ELI_ARG((VoidPtr, VoidPtr));
/* Functions that combine two VoidPtr values
 *   On exit-
 *     VoidPtrSumFct = combination of the two arguments
 ***/

#endif
