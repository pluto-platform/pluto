li x5, 0x10000 # led address
li x6, 0x20000 # uart address
li x4, 0 # led state
li x1, 4  # loop bound
li x30, 1
sw x30, 4(x6)
loop:
li x2, 0 # loop var
iter:
addi x2, x2, 1
blt x2, x1, iter

not x4, x4
sw x4, 0(x5)
bne x4, x0, notzero
li x20, 0x30
j write
notzero:
li x20, 0x31
write:
sb x20, 8(x6)
j loop
