li x5, 0x10000000 # led address
li x4, 0 # led state
li x1, 10000000 # loop bound
loop:
li x2, 0 # loop var
iter:
addi x2, x2, 1
nop
blt x2, x1, iter

not x4, x4
sw x4, 0(x5)
j loop
