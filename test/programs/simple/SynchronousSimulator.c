
// Test case contributed by Cheolgi Kim.

#ifdef USE_BLAST
#define RAND __BLAST_NONDET
#elif defined USE_SATABS
_Bool nondet_bool();
#define RAND nondet_bool();
#elif defined USE_COPPER
#define RAND __COPPER_NONDET__()
#else
#define RAND rand() % 2
#endif

#if (!defined USE_SATABS) && (!defined USE_COPPER)
#include <assert.h>
#endif

#define WAIT 	-1
#define NO_MSG 	-1
#define ACTIVE 	1
#define STANDBY	0

struct SharedVars {
	char side1;
	char side2;
};

int main(int argc, char** argv)
{
  struct SharedVars sharedVars_0;
  struct SharedVars sharedVars_1;

  int __BLAST_NONDET;
  char side1;
  char side2;
#ifdef USE_SATABS
  _Bool side1Failed = 1;
  _Bool side2Failed = 1;
  _Bool manualSelection;
#else
  char side1Failed = 1;
  char side2Failed = 1;
  char manualSelection;
#endif

  sharedVars_0.side1 = -1;
  sharedVars_0.side2 = -1;

  sharedVars_1.side1 = -1;
  sharedVars_1.side2 = -1;

  while(1) {
    side1Failed = RAND;
    side2Failed = RAND;//    		break;
    manualSelection = RAND;

    side1 = sharedVars_0.side1;
    side2 = sharedVars_0.side2;

	if(!side1Failed) {
	  if(side1 == WAIT && side2 == NO_MSG) { // initial decision
	    sharedVars_1.side1 = ACTIVE; // MUST BE ACTIVE
	  } else if (side1 == WAIT) { // side2 is alive, side1 just woke up
	    sharedVars_1.side1 = STANDBY;
	  } else if (side2 == NO_MSG) { // Side2 is down
	    sharedVars_1.side1 =  ACTIVE;
	  } else if(manualSelection == 1) {
		if(side1 == STANDBY) {
			sharedVars_1.side1 = ACTIVE;
		} else { // side1 == ACTIVE
			sharedVars_1.side1 = STANDBY;
		}
	  } else {
	    sharedVars_1.side1 = side1;
	  }
	} else sharedVars_1.side1 = -1;

	if(!side2Failed) {
	  if (side2 == WAIT) { // side2 just woke up
	    sharedVars_1.side2 = STANDBY;
	  } else if (side1 == NO_MSG) { // Side1 is down
	    sharedVars_1.side2 = ACTIVE;
	  } else if(manualSelection == 1) {
	    if(side2 == STANDBY) {
	      sharedVars_1.side2 = ACTIVE;
	    } else { // side2 == ACTIVE
	      sharedVars_1.side2 = STANDBY;
	    }
	  } else sharedVars_1.side2 = side2;
	} else sharedVars_1.side2 = -1;

	sharedVars_0.side1 = sharedVars_1.side1;
	sharedVars_0.side2 = sharedVars_1.side2;

	if(sharedVars_0.side1 == -1 && sharedVars_0.side2 == 0)
	assert(0);

	sharedVars_1.side1 = -1;
	sharedVars_1.side2 = -1;
    }

    return 0;
}
