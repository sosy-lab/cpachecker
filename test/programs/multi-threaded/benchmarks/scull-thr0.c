#include <assert.h>
#include "scull.h"

/* =====================================================
   User program calling functions from the device driver
   ===================================================== */
inode i;
int lock = FILE_WITH_LOCK_UNLOCKED;
int __BLAST_NONDET;

void main() {
  scull_init_module();
  scull_cleanup_module();
}


/* =====================================================
   A model for the device-driver functions
   ===================================================== */
/*
 * scull.h -- definitions for the char module
 *
 * Copyright (C) 2001 Alessandro Rubini and Jonathan Corbet
 * Copyright (C) 2001 O'Reilly & Associates
 *
 * The source code in this file can be freely used, adapted,
 * and redistributed in source or binary form, so long as an
 * acknowledgment appears in derived source files.  The citation
 * should list that the code comes from the book "Linux Device
 * Drivers" by Alessandro Rubini and Jonathan Corbet, published
 * by O'Reilly & Associates.   No warranty is attached;
 * we cannot take responsibility for errors or fitness for use.
 *
 * $Id: scull.h,v 1.15 2004/11/04 17:51:18 rubini Exp $
 */

int scull_quantum = SCULL_QUANTUM;
int scull_qset = SCULL_QSET;
int dev_data;
int dev_quantum;
int dev_qset;
unsigned_long dev_size; 
int __X__; //variable to test mutual exclusion 

/*
 * Empty out the scull device; must be called with the device
 * semaphore held.
 */
int scull_trim(scull_dev dev)
{
  int qset = dev_qset;

  dev_size = 0;
  dev_quantum = scull_quantum;
  dev_qset = scull_qset;
  dev_data = NULL;
  return 0;
}

/*
 * Finally, the module stuff
 */

/*
 * The cleanup function is used to handle initialization failures as well.
 * Thefore, it must be careful to work correctly even if some of the items
 * have not been initialized
 */
inline void scull_cleanup_module(void) 
{
  scull_dev dev;
  scull_trim(dev);

}

inline int scull_init_module() 
{
  int result = 0;
  return 0;

 fail:
  scull_cleanup_module();
  return result;
}
