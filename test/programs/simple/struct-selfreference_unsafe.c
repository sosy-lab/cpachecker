
struct list_head {
	struct list_head *next;
	struct list_head *prev;
};

struct list_head l1, l2;

int main(void)
{
	struct list_head *h;
	int condition;
	h = condition ? &l1 : &l2; 
	ERROR:
		goto ERROR;
	return 0;
}
