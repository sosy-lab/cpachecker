#include <stdio.h>
#include <stdlib.h>

#define STR(x) #x
#define INPUTFILE_STR(x) STR(x)

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
          testcase = fopen(INPUTFILE_STR(INPUTFILE), "r");
          char errorfilename[] = "test/output/errorXXXXXX";

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

		testcase = fopen(INPUTFILE_STR(INPUTFILE), "r");
		
		if (testcase == NULL)
		{
		  fprintf(stderr, "[ERROR] #2 Failed opening the testcase file %s.\n", INPUTFILE_STR(INPUTFILE));
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

	//fprintf(stdout, "Read value %d.\n", current_value);
		
	return current_value;
}

