/*PLOVER: CODE.EVAL*/

/*
Description: System() is called with user-provided data.
Keywords: Size0 Complex0 Taint Unsafe
InvalidArg: "';kill -TERM $PPID'"
*/

#include <stdio.h>
#include <stdlib.h>

#define	MAXSIZE		40
void
test(char *str)
{
	char buf[MAXSIZE];

	snprintf(buf, sizeof buf, "/bin/echo %s", str);
	if(system(buf) < 0)			/* FLAW */
		fprintf(stderr, "Error running command: %s\n", buf);
}

int
main(int argc, char **argv)
{
	char *userstr;

	if(argc > 1) {
		userstr = argv[1];
		test(userstr);
	}
	return 0;
}

