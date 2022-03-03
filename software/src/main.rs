#![no_std]
#![no_main]


#[no_mangle]
pub extern "C" fn _start() -> ! {
    let a = hello(2048);
    let _b = a + 10;
    loop {}
}

fn hello(a: i32) -> i32 {
    let b = 2;
    a + b
}

use core::panic::PanicInfo;

/// This function is called on panic.
#[panic_handler]
fn panic(_info: &PanicInfo) -> ! {
    loop {}
}