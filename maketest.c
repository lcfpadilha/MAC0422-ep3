#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <strings.h>

int main (void) {
    int i, j, n, total, virtual, s, p, t0, tf, b, end;
    char name[1000];
    n = 150;
    total = 16000;
    virtual = 32000;
    s = 1024;
    p = 128;

    printf ("%d %d %d %d\n", total, virtual, s, p);

    for (i = 0; i < n; i++) {
        t0 = i + rand() % 100;
        sprintf(name, "proc%d", i);
        tf = rand() % 20;
        b = 128 + rand() % 100;
        end = 20 + rand() % 80;
        printf("%d %s %d %d ", t0, name, t0 + tf, b);
        for (j = 0; j < end; j++) {
            int r1 = rand();
            int r2 = rand();
            printf(" %d %d ", r1 % b, t0 + r2 % (tf - t0));
        }
        printf("\n");
    }

    return 0;
}