#include<stdlib.h>
#include<stdio.h>

struct ssl3_state_st {
   int next_state;
};

struct ssl_st {
   struct ssl3_state_st *s3;
};

typedef struct ssl_st SSL;

int main() {
   int state;
   SSL *s;
   s = (SSL *) malloc(sizeof(struct ssl_st));
   s->s3 = (struct ssl3_state_st *) malloc(sizeof(struct ssl3_state_st));

   (s->s3)->next_state = 3;
   state = (s->s3)->next_state;

   if(state != 4432) {
     goto END;
   }

   goto ERROR;
   ERROR:
   printf("ERROR REACHED\n");
   return(-1);

   END:
   printf("PROGRAM TERMINATED\n");
   return (0);
}
