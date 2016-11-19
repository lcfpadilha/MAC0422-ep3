#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <strings.h>

int main (void) {
    int i, j, k, n, total, virtual, s, p, t0, tf, b, end;
    char name[1000];
    total = 320;
    virtual = 30720;
    s = 1024;
    p = 32;

    printf ("%d %d %d %d\n", total, virtual, s, p);
    srand(time(NULL));
    for (i = 0; i < 100; i++) {
        for (j = 0; j < 5; j++) {
            t0 = i;
            sprintf(name, "proc%d", i+j);
            tf = t0 + 10 + rand() % 10;
            b = 320;
            printf("%d %s %d %d ", t0, name, tf, b);
            end = 100 + rand() % 80;
            for (k = 0; j < end; j++) {
                int r1 = rand();
                int r2 = rand();
                int fim = t0 + r2 % (tf - t0);
                printf(" %d %d ", r1 % b, fim);
                if (fim > tf) fprintf(stderr, "EIAT\n");
            }
            printf("\n");
        }
    }

    return 0;
}