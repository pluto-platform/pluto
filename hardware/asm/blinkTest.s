li x5, 0x10000000 # led address
li x4, 0 # led state

loop:
li x1, 4 # loop bound
li x2, 0 # loop var
iter:
addi x2, x2, 1
blt x2, x1, iter

not x4, x4
sb x4, 0(x5)
j loop
