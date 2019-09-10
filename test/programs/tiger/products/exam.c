extern int __VERIFIER_nondet_int();
extern int input();

int a;
#define size 5
int solution(int a[size], int b, int c) {
int posB = -1;
int posC = -1;
int fB = 0;
int fC = 0;
int i = 0;
while(i < size && (!fB || !fC)){
if(a[i] == b && !fB){
posB = i;
fB = 1;
}
if(a[i] == c && !fC){
posC = i;
fC = 1;
}
i++;
}
if(fB && fC && (posB <= posC)){
return posC - posB;
}
return -1;
}

int student(int a[size], int b, int c) {
int posB = -1;
int posC = -1;
int fB = 0;
int fC = 0;
int i = 0;
while(i < size && (!fB || !fC)){
if(a[i] == b && !fB){
posB = i;
fB = 1;
}
if(a[i] == c){
posC = i;
fC = 1;
}
i++;
}
if(fB && fC && (posB <= posC)){
return posC - posB;
}
return -1;
}

int main() {

	int b = __VERIFIER_nondet_int();
	int c = __VERIFIER_nondet_int();
int a[size];
int i = 0;
while(i < size){
a[i] = __VERIFIER_nondet_int();
i++;
}

if(solution(a,b,c) != student(a,b,c)){

G1: printf("student isnt right");
}
}
