XOR R0 R0 R0
ADDi R1 R0 1
XOR R2 R2 R2
CALL main
STOP
main:
ADD R3 R3 R0
ADDi R4 R0 1
ADDi R2 R2 1
ST R4 R2
RET
