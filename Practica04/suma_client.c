/*
 * This is sample code generated by rpcgen.
 * These are only templates and you can use them
 * as a guideline for developing your own functions.
 */

#include "suma.h"


void
suma_prog_1(char *host, int a, int b)
{
	CLIENT *clnt;
	int  *result_1;
	dupla_int  suma_1_arg;

#ifndef	DEBUG
	clnt = clnt_create (host, SUMA_PROG, SUMA_VERS, "udp");
	if (clnt == NULL) {
		clnt_pcreateerror (host);
		exit (1);
	}
#endif	/* DEBUG */

	suma_1_arg.a=a;
	suma_1_arg.b=b;

	result_1 = suma_1(&suma_1_arg, clnt);
	if (result_1 == (int *) NULL) {
		clnt_perror (clnt, "call failed");
	}

	else{
		printf("El resultado es: %d\n",*result_1);	
	}
#ifndef	DEBUG
	clnt_destroy (clnt);
#endif	 /* DEBUG */
}


int
main (int argc, char *argv[])
{
	char *host;

	int a,b;

	
	if (argc < 2) {
		printf ("usage: %s server_host\n", argv[0]);
		exit (1);
	}
	
	
	printf("Ingresa el primer valor: ");
	scanf("%d",&a);
	printf("Ingresa el segundo valor: ");
	scanf("%d",&b);

	host=argv[1];
	suma_prog_1 (host,a,b);
exit (0);
}
