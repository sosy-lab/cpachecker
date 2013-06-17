// This file demonstrates a bug in Eclipse CDT at least up to 8.1.2

// With the next line the problem does not occur.
//struct request_queue;

struct gendisk {
 struct request_queue *queue;
};

typedef struct request_queue request_queue_t;

typedef void (request_fn_proc) (request_queue_t *q);

struct request_queue {
    request_fn_proc *request_fn;
};

struct ddv_genhd {
  struct gendisk *gd;
};

int main() {
    struct ddv_genhd genhd_registered;
    struct gendisk *gd = genhd_registered.gd;

    // Without the following line the problem does not occur.
    struct request_queue *queue = gd->queue;

    struct request_queue q;

    // The expression type of the "q.request_fn" IASTFieldReference is a ProblemType.
    request_fn_proc *request_fn = q.request_fn;

    return 0;
}
