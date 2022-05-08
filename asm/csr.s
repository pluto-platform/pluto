li x20, 0x30
csrrw x21, mtvec, x20
ecall
j exit
li x1, 0
li x1, 0
li x1, 0
li x1, 0
li x1, 0
li x1, 0
li x1, 0
li x1, 0
label:
li x1, 0xDEADBEEF
mret

exit:
li x2, 0xFEAC
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