extern int __VERIFIER_nondet_int();
// original
int find_last_orig (int x[], int y) {
	int pos = 0;
	for (int i = 2; i > 0; i--) { //bug
		if (x[i] <= y) { //bug
			pos = i;
			return pos;
		}
	}
	return -1;
}

// patch0
int find_last_patch0 (int x[], int y) {
	int pos = 0;
	for (int i = 2; i >= 0; i--) {//bug
		if (x[i] <= y) { //fixed
			pos = i;
			return pos;
		}
	}
	return -1;
}

int find_last_patch1 (int x[], int y) {
	int pos = 0;
	for (int i = 2; i >= 0; i--) {//fixed
		if (x[i] == y) { //fixed
			pos = i;
			return pos;
		}
	}
	return -1;
}

int find_last_patch0_1 (int x[], int y) {
	int pos = 0;
	for (int i = 2; i > 0; i--) {//fixed
		if (x[i] == y) { //bug
			pos = i;
			return pos;
		}
	}
	return -1;
}

int main(){
int y =__VERIFIER_nondet_int();
int x[3];

for(int i = 0; i < 3;i++){
x[i] = __VERIFIER_nondet_int();
} 


if(find_last_orig(x, y) != find_last_patch0(x,y)){
	if(find_last_patch1(x,y) == find_last_patch0_1(x,y)){
	G1: return 0;
}
return 0;
}


}


