#include <stdlib.h>
typedef struct {
    void (*handler)(void *);
} Parser;

void dummy_handler(void *buf) {}

int main() {
    Parser *parser = malloc(sizeof(Parser));
    parser->handler = dummy_handler;
    void *array = NULL; // For demonstration only
    free(parser);
    parser->handler(array); // Use after free
    return 0;
}