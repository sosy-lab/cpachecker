#include <stdlib.h>

extern int __VERIFIER_nondet_int();

int flag = 1;

typedef struct node {
  int h;
  struct node *n;
} *List;

float harmonicMean(List l){
    int neg = 0;
    int length = 0;
    float sum = 0;

   while(l!=NULL){ 
	if(l->h==0)
	    return -1;      	
	if(l->h<0)
     	    neg = 1;
        flag = l->h;
	sum = 1/(l->h);
	length = length +1;
    }


    if(neg==0){
        flag = sum;
    	return length/sum;
    }
    if(sum!=0){
    	flag = sum;
        return length/sum;
    }
    return -1;    
}

int main(){
   List l = malloc(sizeof(struct node));
   List temp = l;
   if(temp== NULL){
	   return -1;
   }
	
	int size = __VERIFIER_nondet_int();
	int i=0;
	List next;
	while(i<size){
		next = (List) malloc(sizeof(struct node));
		if(next!=NULL){
			next->n=l;
			l = next;
		}
		i=i+1;
	}
  harmonicMean(l);
  return 0;
}
