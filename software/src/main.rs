#![no_std]
#![no_main]


#[no_mangle]
pub extern "C" fn _start() -> ! {
    let a = hello();
    let _b = a + 10;
    loop {}
}
#[no_mangle]
fn hello() -> i32 {
    let a = 1;
    let b = 2;
    a + b
}

use core::panic::PanicInfo;

/// This function is called on panic.
#[panic_handler]
fn panic(_info: &PanicInfo) -> ! {
    loop {}
}