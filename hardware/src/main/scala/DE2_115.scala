case class Pin(name: String) {
  override def toString = s"PIN_$name"
}

case class Pins(names: String*) {
  val ps = names.map(Pin)
  def apply(idx: Int) = ps(idx)
}

object DE2_115 {
  object UART {
    object RXD extends Pin("G12") // UART Receiver
    object TXD extends Pin("G9")  // UART Transmitter
    object CTS extends Pin("G14") // UART Clear to Send
    object RTS extends Pin("J13") // UART Request to Send
  }
  object VGA {
    object R extends Pins("E12","E11","D10","F12","G10","J12","H8","H10") // VGA Red
    object G extends Pins("G8","G11","F8","H12","C8","B8","F10","C9") // VGA Green
    object B extends Pins("B10","A10","C11","B11","A11","C12","D11","D12") // VGA Blue
    object CLK extends Pin("A12") // VGA Clock
    object BLANK_N extends Pin("F11") // VGA Blank
    object HS extends Pin("G13") // VGA H_Sync
    object VS extends Pin("C13") // VGA V_Sync
    object SYNC_N extends Pin("C10") // VGA Sync
  }
  object LCD {
    object DATA extends Pins("L3","L1","L2","K7","K1","K2","M3","M5") // LCD Data
    object EN extends Pin("L4") // LCD Enable
    object RW extends Pin("M1") // LCD Read/Write Select, 0 = Write, 1 = Read
    object RS extends Pin("M2") // LCD Command/Data Select, 0 = Command, 1 = Data
    object ON extends Pin("L5") // LCD Power ON/OFF
    object BLON extends Pin("L6") // LCD Back Light ON/OFF
  }
  object CLOCK {
    object CLK_50 extends Pin("Y2") // 50 MHz Clock Input
    object CLK2_50 extends Pin("AG14") // 50 MHz Clock Input
    object CLK3_50 extends Pin("AG15") // 50 MHz Clock Input
    object SMA_CLKOUT extends Pin("AE23") // External (SMA) Clock Output
    object SMA_CLKIN extends Pin("AH14") // External (SMA) Clock Input
  }
  object SEVENSEGMENT {
    object HEX0 extends Pins("G18","F22","E17","L26","L25","J22","H22") // Seven Segment Digit 0
    object HEX1 extends Pins("M24","Y22","W21","W22","W25","U23","U24") // Seven Segment Digit 1
    object HEX2 extends Pins("AA25","AA26","Y25","W26","Y26","W27","W28") // Seven Segment Digit 2
    object HEX3 extends Pins("V21","U21","AB20","AA21","AD24","AF23","Y19") // Seven Segment Digit 3
    object HEX4 extends Pins("AB19","AA19","AG21","AH21","AE19","AF19","AE18") // Seven Segment Digit 4
    object HEX5 extends Pins("AD18","AC18","AB18","AH19","AG19","AF18","AH18") // Seven Segment Digit 5
    object HEX6 extends Pins("AA17","AB16","AA16","AB17","AB15","AA15","AC17") // Seven Segment Digit 6
    object HEX7 extends Pins("AD17","AE17","AG17","AH17","AF17","AG18","AA14") // Seven Segment Digit 7
  }
  object SW extends Pins("AB28","AC28","AC27","AD27","AB27","AC26","AD26","AB26","AC25","AB25","AC24","AB24","AB23","AA24","AA23","AA22","Y24","Y23") // Slide Switches (18)
  object KEY_N extends Pins("M23","M21","N21","R24") // Push-buttons (4)
  object LED {
    object R extends Pins("G19","F19","E19","F21","F18","E18","J19","H19","J17","G17","J15","H16","J16","H17","F15","G15","G16","H15") // LED Red (18)
    object G extends Pins("E21","E22","E25","E24","H21","G20","G22","G21","F17") // LED Green (9)
  }
  object SRAM {
    object ADDR extends Pins("AB7","AD7","AE7","AC7","AB6","AE6","AB5","AC5","AF5","T7","AF2","AD3","AB4","AC3","AA4","AB11","AC11","AB9","AB8","T8") // SRAM Address (20-bit)
    object DQ extends Pins("AH3","AF4","AG4","AH4","AF6","AG6","AH6","AF7","AD1","AD2","AE2","AE1","AE3","AE4","AF3","AG3") // SRAM Data (16-bit)
    object OE_N extends Pin("AD5") // SRAM Output Enable
    object WE_N extends Pin("AE8") // SRAM Write Enable
    object CE_N extends Pin("AF8") // SRAM Chip Select
    object STRB_N extends Pins("AD4","AC4") // SRAM Strobe
  }
  object SDRAM {
    object ADDR extends Pins("R6","V8","U8","P1","V5","W8","W7","AA7","Y5","Y6","R5","AA5","Y7") // SDRAM Address (13-bit)
    object DQ extends Pins("W3","W2","V4","W1","V3","V2","V1","U3","Y3","Y4","AB1","AA3","AB2","AC1","AB3","AC2","M8","L8","P2","N3","N4","M4","M7","L7","U5","R7","R1","R2","R3","T3","U4","U1") // SDRAM Data (32-bit)
    object BA extends Pins("U7","R4") // SDRAM Bank Address (2-bit)
    object DQM extends Pins("U2","W4","K8","N8") // SDRAM Byte Data Mask (4-bit)
    object RAS_N extends Pin("U6") // SDRAM Row Address Strobe
    object CAS_N extends Pin("V7") // SDRAM Column Address Strobe
    object CKE extends Pin("AA6") // SDRAM Clock Enable
    object CLK extends Pin("AE5") // SDRAM Clock
    object WE_N extends Pin("V6") // SDRAM Write Enable
    object CS_N extends Pin("T4") // SDRAM Chip Select
  }
  object FLASH {
    object ADDR extends Pins("AG12","AH7","Y13","Y14","Y12","AA13","AA12","AB13","AB12","AB10","AE9","AF9","AA10","AD8","AC8","Y10","AA8","AH12","AC12","AD12","AE10","AD10","AD11") // FLASH Address (23-bit)
    object DQ extends Pins("AH8","AF10","AG10","AH10","AF11","AG11","AH11","AF12") // FLASH Data (8-bit)
    object CE_N extends Pin("AG7") // FLASH Chip Enable
    object OE_N extends Pin("AG8") // FLASH Output Enable
    object RST_N extends Pin("AE11") // FLASH Reset
    object RY extends Pin("Y1") // FLASH Ready/Busy Output
    object WE_N extends Pin("AC10") // FLASH Write Enable
    object WP_N extends Pin("AE12") // FLASH Write Protect/Programming Acceleration
  }
  object EEPROM {
    object I2C_SCLK extends Pin("D14") // EEPROM Clock
    object I2C_SDAT extends Pin("E14") // EEPROM Data
  }
  object SDCARD {
    object CLK extends Pin("AE13") // SD Clock
    object CMD extends Pin("AD14") // SD Command Line
    object DAT extends Pins("AE14","AF13","AB14","AC14") // SD Data (4-bit)
    object WP_N extends Pin("AF14") // SD Write Protect
  }
}