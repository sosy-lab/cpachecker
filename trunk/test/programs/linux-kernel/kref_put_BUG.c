#include <assert.h>

struct kref {
	int refcount;
};

struct usb_serial {
	struct kref kref;
};

int atomic_dec_and_test(int* cnt);

int kref_put(struct kref *kref, void (*release)(struct kref *kref))
{
        if (atomic_dec_and_test(&kref->refcount)) {
                release(kref);
                return 1;
        }
        return 0;

}

int ldv_lock = 0;

static void destroy_serial(struct kref *kref)
{
	assert(ldv_lock==0);
}

int table_lock;

void spin_lock(int *lock) {
	ldv_lock=1;
}

void spin_unlock(int *lock) {
	ldv_lock=0;
}

void usb_serial_put(struct usb_serial *serial)
{
        spin_lock(&table_lock);
        kref_put(&serial->kref, destroy_serial);
        spin_unlock(&table_lock);
}
 
int main()
{
	struct usb_serial serial;
	usb_serial_put(&serial);
        return 0;
}

