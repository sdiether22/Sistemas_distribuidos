all : Socket.o Socket_Servidor.o Servidor

CPPFLAGS = -g -I.

Servidor : Servidor.c
	cc -g -I. Socket.o Socket_Servidor.o Servidor.c -o Servidor

clean :
	rm *.o Servidor
