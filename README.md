# FingerReader_SerialTest

A simple Android app used to test the UART serial communication of the Qualcomm 410 board used in FingerReader.

### Prerequisites

Ensure Android device contains root access. Supports Android devices >= API level 21 (Lollipop)

### Getting started

The UART0 port of the Variscite Carrier board can be used to assess functionality.
Please check out the [product page](https://www.variscite.com/product/system-on-module-som/cortex-a53-krait/dart-sd410-qualcomm-snapdragon-410/) and [schematics](https://www.variscite.com/wp-content/uploads/2017/12/VAR-SD410CustomBoard-Schematics.pdf) for details.

The app creates a feedback loop to test the functionality of the UART0 serial device(i.e. what is written gets read back).
Tie the Tx and Rx connections of the UART0 together to enable this.

## Running the tests through ADB (Android Debug Bridge)

Access to serial port(from Windows):
```
adb shell su
cd /dev/ttyHS4
```

To write from serial port:
```
echo message > /dev/ttyHS4
```

To read from serial port:
```
read X < ttyHS4
echo $X
```

## Built With

* [android-serialport-api](https://code.google.com/archive/p/android-serialport-api/) -  API to connect, read and write data through serial ports.
* [serialport-api-sourcecode](https://github.com/cepr/android-serialport-api/) - Source code of the Serialport-API
* [Usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android) - Android USB host serial driver library (functionality implemented but never used).

