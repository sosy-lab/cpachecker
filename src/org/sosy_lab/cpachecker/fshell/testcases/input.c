#include <stdio.h>
#include <stdlib.h>

int input()
{
	static FILE* testcase = NULL;
	char line[100];
	static int next_value;
	int current_value;
	static char already_read = 0;

	if (testcase == NULL)
	{
		if (already_read)
		{
          FILE* errorfile;
          testcase = fopen(INPUTFILE, "r");
          char errorfilename[] = "output/errorXXXXXX";

          mkstemp(errorfilename);

          errorfile = fopen(errorfilename, "w");

          while (fgets(line, 100, testcase) != NULL)
          {
        	  fputs(line, errorfile);
          }

          fclose(testcase);
          fclose(errorfile);

          fprintf(stderr, "[ERROR] #1 No input available. Wrote erroneous test case to %s.\n", errorfilename);
		  exit(-1);
		}

		testcase = fopen(INPUTFILE, "r");
		
		if (testcase == NULL)
		{
		  fprintf(stderr, "[ERROR] #2 Failed opening the testcase file.\n");
		  exit(-1);
		}
		
		fgets(line, 100, testcase);
		
		if (line == NULL)
		{
          fclose(testcase);
		  fprintf(stderr, "[ERROR] #3 No input available.\n");
		  exit(-1);
		}
		
		next_value = atoi(line);
	}

	current_value = next_value;

	if (fgets(line, 100, testcase) == NULL)
	{
		fclose(testcase);
		
		testcase = NULL;
		
		already_read = 1;
	}
    else
    {
      next_value = atoi(line);
    }
		
	return current_value;
}

