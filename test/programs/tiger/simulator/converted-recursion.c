extern int __VERIFIER_nondet_int();
extern int input();

void invoke(int a){
	if(a != 5){
 	methodController(a);
	}
}


void methodController(int a){
 	invoke(a+1);
}

int main() {
	invoke(5);
int i;
	G5: i = 0;
}
