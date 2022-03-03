#![no_std]
#![no_main]


#[no_mangle]
pub extern "C" fn _start() -> ! {
    let raw = 0x10000 as *mut u8;
    let mut state = 0x00u8;
    
    loop {
        delay();
        unsafe{ core::ptr::write_volatile(raw, state) }
        state = !state;
    }
}

fn delay() {
    for _ in 0..10000 {
        unsafe{ core::ptr::write_volatile(0 as *mut u8, 0) }
    }
}

use core::panic::PanicInfo;

/// This function is called on panic.
#[panic_handler]
fn panic(_info: &PanicInfo) -> ! {
    loop {}
}