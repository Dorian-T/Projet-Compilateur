EXO1:   XOR R0 R0 R0
        XOR R2 R2 R2
        ADDi R2 R2 48
        XOR R3 R3 R3
        ADDi R3 R3 57
        XOR R0 R0 R0
        XOR R4 R4 R4
        XOR R5 R5 R5
        ADDi R5 R5 9
S:      IN R1
        JSUP R2 R1 F
        JSUP R1 R3 F
        SUBi R1 R1 48
        ADDi R6 R0 0
M:      JEQU R4 R5 F2
        ADD R0 R0 R6
        ADDi R4 R4 1
        JMP M
F2:     XOR R4 R4 R4
        ADD R0 R0 R1
        JMP S
F:      RET