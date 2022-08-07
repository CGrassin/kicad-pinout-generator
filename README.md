# KiCad Pinout Generator

This KiCad plugin generates pinouts from a PCB file to various formats:
* **Text** formats for documentation (HTML table, CSV, Markdown, etc.),
* **Code** (currently supported: C/C++ enums, Xilinx XDC constraint, #define format).

Example applications:
* Automatically create the pin assignement from a microcontroller to C code,
* Generate interface documents for connectors.

![HTML and C sample output](./pictures/sample_output.png)

## Install instructions

This plugin has been developped for KiCad 6.
To install it:
1. Clone or download the repository, and extract it to your KiCad plugin folder. You can find it by opening the PCB editor, and using "Tools" > "Externals Plugins" > "Open Plugin Directory".
2. Refresh the plugins by restarting the PCB editor or using "Tools" > "Externals Plugins" > "Refresh Plugins".

## Usage instructions

To use the plugin:
1. Select one component,
2. Run the plugin by using the toolbar button or the Plugins menu,
3. Choose the desired format with the drop-down menu and copy-paste the result!

**This plugins only gives useful ouput if the pins have net names.** Use labels in the schematic editor to give the nets a human-readable name.

Note: in the generated code, both unconnected and power nets are ignored (commented in the ouput).

## License

This plugin is published under MIT license.
