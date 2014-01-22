#include "../constants.h"
#include <assert.h>

static int parse_expression_list(char *str) 
{
  int start=0, i=-1, j=-1, apost=0;
  char str2[EXPRESSION_LENGTH];
	
  if (!str) return -1;

  do {

    /* i only changes here --> it's the "current character" */
    i++;
    switch(str[i]) {
    case '"':	apost = !apost; 
      break;

      /* Comman and NULL are both word terminators, stop parsing if 
         your word terminator is a NULL. */
    case ',':	if (apost) break;
    case EOS:	/* word found */

      /* Skip initial whitespace from start of the word being processed */
      while ((str[start] == ' ') || (str[start] == '\t')) start++;

      /* Skip quote marks */
      if (str[start] == '"') start++;

      /* Set j to point to the end of the current word */
      j = i-1;

      /* Skip over quotes and whitespace at the END of the word */
      while ((0 < j) && ((str[j] == ' ') || (str[j] == '\t'))) j--;
      if ((0 < j) && (str[j] == '"')) j--;

      /* If word not empty.... */
      if (start<=j) {
        /* valid word */
        /* BAD */
        assert (j-start+1 < EXPRESSION_LENGTH);
        r_strncpy(str2, str+start, j-start+1);
        /* BAD */
        str2[j-start+1] = EOS;
      } else {
        /* parsing error */
        return -1;
      }
      /* for the next word */
      start = i+1;
    }
  } while (str[i] != EOS);
	
  return 0;
}

int parse_expression (char *str) {
  char *except;
  char str2 [LINE_LENGTH];

  except = strstr(str, NEEDLE);
  if (except) {
    strncpy(str2, str, except-str);
    str2[except-str] = EOS;
    if (parse_expression_list(except+NEEDLE_SZ)) {
      /* error */
      return -1;
    } 
  }

  return 0;
}

int main ()
{
  char A [LINE_LENGTH+1];
  A[LINE_LENGTH] = EOS;

  parse_expression (A);
  return 0;
}
