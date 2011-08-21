#include <assert.h>


int stopped = 0, driverStoppingFlag = 0, stoppingEvent = 0; // boolean flags
int pendingIo = 1;

inline void IoDec() {
  int PIo = pendingIo-1;
  pendingIo = pendingIo-1;
  if (PIo == 0) { stoppingEvent = 1; }
}

void main() {
  int status; // boolean flag
  /* Begin: IoInc() */
  pendingIo = pendingIo+1;
  if (driverStoppingFlag >= 1) {
    IoDec();
    status = 0;
  } else {
    status = 1;
  }
  /* End: IoInc() */
  if (status >= 1) {
    assert(stopped <= 0);
  }
  IoDec();
}

