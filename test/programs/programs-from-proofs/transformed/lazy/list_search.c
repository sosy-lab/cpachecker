typedef unsigned long int size_t;
extern void __VERIFIER_error() __attribute__ ((__noreturn__));
void * malloc(size_t __size);
void free(void *__ptr);

void __VERIFIER_assert(int cond) {
  if (!(cond)) {
    ERROR: __VERIFIER_error();
  }
  return;
}

void exit(int p)
{
  EXIT_: goto EXIT_;
}

int flag = 1;

typedef struct list {
	int key;
	struct list *next;
} mlist;

mlist *head;

mlist* search_list(mlist *l, int k){
	while(l!=0 && l->key!=k) {
		l = l->next;
	}
	return l;
}

int delete_list(mlist *l){
	mlist *tmp;
	tmp = head;
	if (head != l) {
	    flag = tmp;
		while(tmp->next!=l) {
			tmp = tmp->next;
			flag=tmp;
		}
	} else {
	    flag = l;
		head = l->next;
	}
	flag = tmp;
	flag=l;
	tmp->next = l->next;
	free(l);
	return 0;
}

int insert_list(mlist *l, int k){

	l = (mlist*)malloc(sizeof(mlist));
	
	if(l==0){
      exit(0);
    }

    flag = l;
	if (head==0) {
		l->key = k;
		l->next = 0;
	} else {
		l->key = k;
		l->next = head;
	}
	head = l;
	
	return 0;	
}

int main(void){

	int i;
	mlist *mylist, *temp;

	insert_list(mylist,2);
	insert_list(mylist,5);
	insert_list(mylist,1);
	insert_list(mylist,3);

	mylist = head;
	while(mylist) {
		mylist = mylist->next;
	}

	temp = search_list(mylist,2);
	flag = temp;
	__VERIFIER_assert(temp->key==2);
	delete_list(temp);

	mylist = head;

	while(mylist) {
		mylist = mylist->next;
	}
	return 0;
}

