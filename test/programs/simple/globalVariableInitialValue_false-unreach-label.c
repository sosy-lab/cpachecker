// Variables with storage class extern do not have any default value!
extern int i;

int main() {
	if (i == 1) {
ERROR:		goto ERROR;
	}
}
