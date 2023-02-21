# KiCad Pinout Generator

**Compatibility:** KiCad 6.0, KiCad 7.0.

## Plugin presentation

This KiCad plugin generates pinouts of components from a PCB file to various formats:
* **Text** formats for documentation:
    * Markdown table
    * HTML table
    * CSV
    * List (tab separated)
    * [WireViz](https://github.com/formatc1702/WireViz)
* **Code** format for software development: 
    * C/C++ enums
    * C/C++ #define
    * Python/MicroPython dict.
    * Xilinx XDC constraint (FPGA)
    * Microsemi PDC constraint (FPGA)

Example applications:
* Automatically create the pin assignement from a microcontroller to C code,
* Generate interface documents for connectors.

![HTML and C sample output](./pictures/sample_output.png)
*Example of a export as HTML (left) and C/C++ enum (right)*

## Install instructions

This plugin has been developped for KiCad 6.
To install it:
1. Clone or download the repository and extract it to your KiCad plugin folder. You can find it by opening the PCB editor, and using "Tools" > "Externals Plugins" > "Open Plugin Directory".
2. Refresh the plugins by restarting the PCB editor or using "Tools" > "Externals Plugins" > "Refresh Plugins".

*The toolbar button can be shown or hidden in "Preferences" > "Action Plugins".*

## Usage instructions

To use the plugin:
1. Select one component (or more),
2. Run the plugin by using the toolbar button or the Plugins menu,
3. Choose the desired format with the drop-down menu and copy-paste the result!

**This plugins only gives useful ouput if the pins have net names.** Use labels in the schematic editor to give the nets a human-readable name.

Note: in the generated code, both unconnected and power nets are ignored (commented in the ouput).

For C/C++ and Python format, there is a checkbox to use the pin name istead of pin number as the variable value. When checked, a filter can also be entered:
* If the filter is left empty, the full pin name is used.
* If something is entered, the plugin looks for this in the pin name, and extract the number after this string. For instance, if a pin name is "GPIO64/SPI", entering "GPIO" as filter will result in "64" in the output.

## License

This plugin is published under MIT license.
