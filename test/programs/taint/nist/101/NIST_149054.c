/*PLOVER: CODE.EVAL*/

/*
Description: System() is called with user-provided data.
Keywords: Size0 Complex0 Taint Unsafe
InvalidArg: "';kill -TERM $PPID'"
*/

#include <stdio.h>
#include <stdlib.h>
#include <ctype.h>
#include <string.h>

#define	MAXSIZE		40

int check(const char *str)
{
	int i;

	for(i = 0; i < strlen(str); ++i)
		if(!isalnum(str[i]))
			return -1;

	return 0;
}

void
test(char *str)
{
	char buf[MAXSIZE];

	if(check(str) < 0)
		return;				/* FIX */

	snprintf(buf, sizeof buf, "/bin/echo %s", str);
	if(system(buf) < 0)
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

