li x20, 0x30
csrrw x21, mtvec, x20
ecall
j exit
nop
nop
nop
nop
nop
nop
nop
nop
label:
li x1, 0xDEADBEEF
mret

exit:
nop
nop
nop
nop
nop
nop
nop
nop
nop
nop
nop
nop
nop
nop
nop
nop
nop
nop
nop
nop
nop
nop
nop
nop
nop
nop
nop
nop
nop
nop
nop
nop