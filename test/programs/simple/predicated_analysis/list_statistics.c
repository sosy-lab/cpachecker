extern int __VERIFIER_nondet_int();

#include <stdlib.h>

typedef struct node {
  int h;
  struct node *n;
} *List;

int num;
int num2;
int flag1;
int sum;
float mean;
int flag = 1;
int inter;
List successor;
List newHead;

int is_empty(List head){
	if(head == NULL){
		return 1;
	}
	return 0;
}

List extract_even(List head){
	newHead = NULL;
	while(head != NULL){
		if(head->h % 2 == 0){
			successor = newHead;
			newHead = (List) malloc(sizeof(struct node));
			if(newHead == NULL){
				newHead = successor;
			}else{
				newHead->n = successor;
			}
		}
	}
	return newHead;
}

float calc_mean(List head){
	num = 0;
	sum = 0;
	while(!head==NULL){
		sum = sum + head->h;
		head = head->n;
		num = num +1;
	}
	flag = num;
	return ((float)sum)/num;
}

float variance(List head){
	mean = calc_mean(head);
	num = 0;
	flag1 = 1;
	sum = 0;

	while(!head==NULL){
		if(flag1){
	        flag1=0;
		}else{
		    num = num +1;	
		}
		inter = head->h - mean;
		sum = sum + inter*inter;
		head = head->n;
	}
	
	if(flag1){
		flag = num;
		return sum/num;
	}else{
		return 0;
	}
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
   
   
   List le = extract_even(l);

  float m = calc_mean(l);
   m = variance(l);
   
  if(!is_empty(le)){
		m = calc_mean(le);
		m = variance(le);
   }
return 0;
}
