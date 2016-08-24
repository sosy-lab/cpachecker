

void *malloc(unsigned long size);

struct a {
	int a;
};

struct b {
	int a, b;
};


int main(void)
{
	int c, size;
	void *p, *op;
	if (c) {
		size = sizeof (struct a);
	} else {
		size = sizeof (struct b);
	}
	
	p = malloc(size);
        if (p == 0) return 1;
	
	if (c) {
		((struct a *) p)->a = 0;
	} else {
		((struct b *) p)->b = 0;
	}

	op = p;
	p = &c;
	*(int *)p = c;

        p = malloc(sizeof (struct b));
        if (p == 0) return 1;
        ((struct a *) p)->a = 1;
        ((struct b *) p)->b = 1;
 
	if ((c && ((struct a *) op)->a == 0) ||
            (!c && ((struct b *) op)->b == 0)) {
		return 0;
	} else {
		ERROR: return 1;
	}
}
