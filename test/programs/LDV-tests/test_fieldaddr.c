#include <assert.h>
#include <malloc.h>

int VERDICT_SAFE;
int CURRENTLY_UNSAFE;

int undef_int(void);

struct list_head {
};

struct list_head *elem = NULL;

void list_add(struct list_head *new, struct list_head *head) {
  if(new!=NULL) {
	  assert(new!=elem);
	  if(undef_int())
        	elem = new;
  }
}

struct drm_device {
	struct list_head vmalist;
};

struct drm_vma_entry {
	struct list_head head;
};

void drm_vm_open_locked(struct drm_device *dev)
{
	struct drm_vma_entry *vma_entry;
	vma_entry = malloc(sizeof(*vma_entry)); //kmalloc(sizeof(*vma_entry), GFP_KERNEL);
	if (vma_entry) {
		//vma_entry->vma = vma;
		//vma_entry->pid = current->pid;
		list_add(&vma_entry->head, &dev->vmalist);
	}
}

int main(void) {
	struct drm_device dev;
	drm_vm_open_locked(&dev);
	drm_vm_open_locked(&dev);
	return 0;
}
