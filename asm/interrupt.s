li x20, 0x30
csrrw x21, mtvec, x20
li x3, 1
slli x3, x3, 11
csrrs x4, mie, x3
csrrsi x4, mstatus, 0x8
li x1, 0
li x2, 0
loop:
addi x2, x2, 1
nop
beqz x1, loop

j exit

handler:
li x3, 1
slli x3, x3, 11
csrrc x4, mie, x3
li x31, 0xDEADBEEF
li x1, 1
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