; hello_world.asm — example for asm-flash (OpenTensor tools floppy).
; Assemble and flash:  asm-flash hello_world.asm "ASM BIOS"
; Registers R0-R7, ';' starts a comment, labels end with ':'.
; HALT stops the program; the machine stays ON with this text kept.

START:  MOV R0, 3        ; print the message 3 times
LOOP:   PRINT msg
        SUB R0, 1
        CMP R0, 0
        JGT LOOP
        PUSH 880
        POP R1
        CALL BEEPIT
        HALT

BEEPIT: BEEP R1, 0.2
        RET

msg:    DB "Hello, world!"
