#include <stdlib.h> 
#include <string.h> 
#include <stdio.h> 
  
#define BUFSIZE 256 
  
int main(int argc, char **argv)  
{ 
    char *buf; 
    buf = (char *)malloc(BUFSIZE); 
    if (buf == NULL) 
        {printf("Memory allocation problem"); return 1;} 
  
    if (argc > 1) 
    { 
        strcpy(buf, argv[1]);                           /* FLAW */
        printf("buf = %s\n", buf); 
    } 
    free(buf); 
    return 0; 
} 