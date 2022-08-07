#!/usr/bin/env python

import wx
import os
import pcbnew
import gettext
import re

from . import pinout_generator_result

not_connected_text = 'NC'

class PinoutDialog(pinout_generator_result.PinoutDialog):
    def onDeleteClick(self, event):
        return self.EndModal(wx.ID_DELETE)

def pad_is_connected(pad):
    return ('no_connect' not in pad.GetPinType()) and pad.IsConnected()

def pad_is_power(pad):
    return 'power' in pad.GetPinType()

def str_to_C_variable(string):
    out = "pin_" + string
    out = re.sub(r'[ /]', '_', out)
    out = re.sub(r'-', 'N', out)
    out = re.sub(r'\+', 'P', out)
    out = re.sub(r'[^a-zA-Z0-9\_]', '', out)
    out = re.sub(r'_+', '_', out)
    return out
    
def str_to_C_define(string):
    out = string.upper()
    out = re.sub(r'[ /]', '', out)
    out = re.sub(r'-', 'N', out)
    out = re.sub(r'\+', 'P', out)
    out = re.sub(r'[^a-zA-Z0-9_]', '', out)
    out = re.sub(r'_+', '_', out)
    return out

class PinoutGenerator(pcbnew.ActionPlugin):
    def defaults(self):
        self.name = "Pinout Generator"
        self.category = "Read PCB"
        self.description = "Generates a pinout table from the PCB nets"
        self.show_toolbar_button = True
        self.icon_file_name = os.path.join(os.path.dirname(__file__), 'logo.png')

    def change_format( self, event ):
        self.set_output(self.get_selection())
        event.Skip()

    def set_output(self, selection=0):        
        if selection == 0: #list
            output_formater = self.setList
        if selection == 1: # CSV
            output_formater = self.setCSV
        elif selection == 2: # HTML table
            output_formater = self.setHTML
        elif selection == 3: # MD table
            output_formater = self.setMarkdown
        elif selection == 4: # C/Cpp enum
            output_formater = self.setCCode_enum
        elif selection == 5: # C/Cpp define
            output_formater = self.setCCode_define
        elif selection == 6: # Python dict
            output_formater = self.set_python
        elif selection == 7:
            output_formater = self.setXDC

        # checks for format n
        # output = ""
        output = output_formater()
        # for component in self.components:
        #     output += output_format(component)
        self.set_result(output)

    def setList(self):
        output = ""
        for pad in self.pinout:
            if output != "":
                output += "\n"
            output += pad.GetNumber() + "\t" + (pad.GetNetname() if pad_is_connected(pad) else not_connected_text) + "\t" + pad.GetPinType ()
        return output

    def setCSV(self):
        output = ""
        for pad in self.pinout:
            output += "\"" + pad.GetNumber() + "\"" + "," + "\"" + (pad.GetNetname() if pad_is_connected(pad) else not_connected_text) + "\"" + "\n"
        return output

    def setHTML(self):
        output = "<table>\n"
        output += "\t<tr><th>Pin number</th><th>Pin net</th></tr>\n"
        for pad in self.pinout:
            output += "\t<tr><td>" + pad.GetNumber() + "</td><td>" + (pad.GetNetname() if pad_is_connected(pad) else not_connected_text) + "</td></tr>\n"
        output += "</table>"
        return output

    def setCCode_enum(self):
        added_vars = []
        output = "enum pinout{\n"
        for pad in self.pinout:
            var_name = str_to_C_variable(pad.GetNetname())
            if var_name in added_vars or not pad.GetNumber().isdigit() or not pad_is_connected(pad) or pad_is_power(pad):
                 output += "//"
            else:
                 added_vars.append(var_name)
            output += "\t" + var_name + "=" + pad.GetNumber()+",\n" 
        output += "};"
        return output

    def setCCode_define(self):
        added_vars = []
        output = ""
        for pad in self.pinout:
            var_name = str_to_C_define(pad.GetNetname())
            if var_name in added_vars or not pad_is_connected(pad) or pad_is_power(pad):
                 output += "// "
            else:
                 added_vars.append(var_name)
            output += "#define " + var_name + " " + pad.GetNumber()+"\n" 
        return output

    def set_python(self):
        added_vars = []
        output = "pinout = {\n"
        for pad in self.pinout:
            var_name = str_to_C_variable(pad.GetNetname())
            if var_name in added_vars or not pad_is_connected(pad) or pad_is_power(pad):
                 output += "#"
            else:
                 added_vars.append(var_name)
            output += "    \'" + var_name + "\' : "
            if pad.GetNumber().isdigit():
                output += pad.GetNumber() + ',\n'
            else:
                output += "\'" + pad.GetNumber() + "\'" + ',\n'
        output += "}"
        return output

    def setMarkdown(self):
        output = "| Pin number | Pin net |\n"
        output +="|------------|---------|\n"
        for pad in self.pinout:
            output += "| " + pad.GetNumber() + " | " + (pad.GetNetname() if pad_is_connected(pad) else not_connected_text) + " |\n"
        return output

    # TODO check VHDL port validity
    def setXDC(self):
        added_vars = []
        output = " ## Pinout generated\n"
        for pad in self.pinout:
            var_name = str_to_C_variable(pad.GetNetname())
            if var_name in added_vars or not pad_is_connected(pad) or pad_is_power(pad):
                 output += "#"
            else:
                added_vars.append(var_name)
            output += "set_property -dict { PACKAGE_PIN "+pad.GetNumber()+"    IOSTANDARD LVCMOS33 } [get_ports { "+var_name+" }];\n"
        return output


    def Run(self):
        footprint_selection = []
        for footprint in pcbnew.GetBoard().GetFootprints():
            if footprint.IsSelected():
                footprint_selection.append(footprint)

        # check selection
        if len(footprint_selection) > 1:
            wx.MessageBox("Too many components selected ("+str(len(footprint_selection))+"), only select one!") # TODO: instead, ask for user confirmation?
            return
        elif len(footprint_selection) < 1:
            wx.MessageBox("Select one component!")
            return

        # extract pins
        self.pinout = []
        added_pads = [] 
        for pad in footprint_selection[0].Pads():
            if pad.GetNumber() not in added_pads: # filter redundant pads (eg.: thermal pad)
                self.pinout.append(pad)
                added_pads.append(pad.GetNumber())

        # set up and show the GUI
        a = PinoutDialog(None)
        a.output_format.Bind( wx.EVT_CHOICE, self.change_format )
        self.set_result = a.result.SetValue
        self.get_selection = a.output_format.GetSelection
        self.set_output() # set result to default format
        modal_result = a.ShowModal()
        a.Destroy()
