# 1 "struct2/main.c"
# 1 "<eingebaut>"
# 1 "<Kommandozeile>"
# 1 "struct2/main.c"


typedef struct queue {
 int length;
 int size;
 int data[10];
} QUEUE;

QUEUE *queue_create()
{
 QUEUE q;

 q.length = 0;

 return &q;
}

int main() {
 QUEUE *ptr = queue_create();
}
