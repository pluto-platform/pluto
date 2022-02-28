

- block all csr instructions when one is in the pipeline
  - use `isCsrAccess`

- decode should produce alufun and memaccesswidth again since alufun should always be add for mem stuff
- alufun has to be zero for jal (jalr has funct3 0 and should be fine)
- auipc needs alufun 0
- lui needs passthrough -> we have got pc+4 in decode (need channel from fetch) -> pass it
- 