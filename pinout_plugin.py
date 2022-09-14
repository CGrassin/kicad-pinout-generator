#!/usr/bin/env python

import wx
import os
import pcbnew
import gettext
import re

from . import pinout_generator_result

class PinoutDialog(pinout_generator_result.PinoutDialog):
    def onDeleteClick(self, event):
        return self.EndModal(wx.ID_DELETE)

def pad_is_connected(pad):
    return ('no_connect' not in pad.GetPinType()) and pad.IsConnected()

def pad_is_power(pad):
    return 'power' in pad.GetPinType() 

def pad_is_passive(pad):
    return 'passive' in pad.GetPinType()

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

def get_pins(component):
    pinout = []
    added_pads = [] 
    for pad in component.Pads():
        if pad.GetNumber() not in added_pads: # filter redundant pads (eg.: thermal pad)
            pinout.append(pad)
            added_pads.append(pad.GetNumber())
    return pinout

def get_pin_name_unless_NC(pad):
    return (pad.GetNetname() if pad_is_connected(pad) else 'NC')

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
        output = ""   
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
            if len(self.footprint_selection) > 1:
                self.set_result("This format is not compatible with multiple selection. Only select a single component.")
                return
        elif selection == 6: # Python dict
            output_formater = self.set_python
        elif selection == 7:
            output_formater = self.setXDC
            if len(self.footprint_selection) > 1:
                self.set_result("This format is not compatible with multiple selection. Only select a single component.")
                return
        elif selection == 8:
            output_formater = self.wireviz_format
            output = "connectors:\n"
        elif selection == 9:
            output_formater = self.setPDC
            if len(self.footprint_selection) > 1:
                self.set_result("This format is not compatible with multiple selection. Only select a single component.")
                return

        # checks for format n
        for component in self.footprint_selection:
            output += output_formater(component)
        self.set_result(output)

    def setList(self, component):
        output = ""
        pinout = get_pins(component)
        for pad in pinout:
            output += pad.GetNumber() + "\t" + pad.GetPinFunction() + "\t" + get_pin_name_unless_NC(pad) + "\n"
        return output

    def setCSV(self, component):
        output = ""
        pinout = get_pins(component)
        for pad in pinout:
            output += "\"" + pad.GetNumber() + "\"" + "," + "\"" + pad.GetPinFunction() + "\"" + "," + "\"" +  get_pin_name_unless_NC(pad) + "\"" + "\n"
        return output

    def setHTML(self, component): # FIXME HTML escape chars
        output =  "<p>Pinout for "+component.GetReference()+" ("+component.GetValue()+"):</p>\n"
        output += "<table>\n"
        output += "\t<tr><th>Pin number</th><th>Pin name</th><th>Pin net</th></tr>\n"
        pinout = get_pins(component)
        for pad in pinout:
            output += "\t<tr><td>" + pad.GetNumber() + "</td><td>" + pad.GetPinFunction() + "</td><td>" +  get_pin_name_unless_NC(pad) + "</td></tr>\n"
        output += "</table>\n"
        return output

    def setCCode_enum(self, component):
        added_vars = []
        output = "enum pinout_"+component.GetReference()+"{\n"
        pinout = get_pins(component)
        for pad in pinout:
            var_name = str_to_C_variable(pad.GetNetname())
            if var_name in added_vars or not pad.GetNumber().isdigit() or not pad_is_connected(pad) or pad_is_power(pad):
                 output += "//"
            else:
                 added_vars.append(var_name)
            output += "\t" + var_name + "=" + pad.GetNumber()+",\n" 
        output += "};\n"
        return output

    def setCCode_define(self, component):
        added_vars = []
        output = ""
        pinout = get_pins(component)
        for pad in pinout:
            var_name = str_to_C_define(pad.GetNetname())
            if var_name in added_vars or not pad_is_connected(pad) or pad_is_power(pad):
                 output += "// "
            else:
                 added_vars.append(var_name)
            output += "#define " + var_name + " " + pad.GetNumber()+"\n" 
        return output

    def set_python(self, component):
        added_vars = []
        output = "pinout_"+component.GetReference()+" = {\n"
        pinout = get_pins(component)
        for pad in pinout:
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
        output += "}\n"
        return output

    def setMarkdown(self, component):
        output = "Pinout for "+component.GetReference()+" ("+component.GetValue()+"):\n\n"
        pinout = get_pins(component)
        max_len_num, max_len_name, max_fn_name = len('Pin number'),len('Pin net'), len('Pin name')
        for pad in pinout:
            max_fn_name = max(max_fn_name, len(pad.GetPinFunction()))
            max_len_num = max(max_len_num, len(pad.GetNumber()))
            max_len_name = max(max_len_name, len( get_pin_name_unless_NC(pad)))
        output += "| Pin number" + ' '*(max_len_num-len('Pin number')) + " | Pin name" + ' '*(max_fn_name-len('Pin name')) + " | Pin net" + ' '*(max_len_name-len('Pin net')) + " |\n"
        output +="|---"+'-'*(max_len_num-1)+"|---"+'-'*(max_fn_name-1)+"|---"+'-'*(max_len_name-1)+"|\n"
        
        for pad in pinout:
            output += "| " + pad.GetNumber() + ' '*(max_len_num-len(pad.GetNumber())) + " | "  + pad.GetPinFunction() + ' '*(max_fn_name-len(pad.GetPinFunction())) + " | " +  get_pin_name_unless_NC(pad) + ' '*(max_len_name-len(get_pin_name_unless_NC(pad))) + " |\n"
        return output

    def wireviz_format(self, component):
        output = "    "+component.GetReference()+":\n"
        output += "        type: "+component.GetValue()+"\n"
        pins_output = "        pins: ["
        pinlabels_output = "        pinlabels: ["
        pinout = get_pins(component)
        for pad in pinout:
            pins_output += pad.GetNumber()+", "
            pinlabels_output += pad.GetNetname()+", "
        output += pins_output+"]\n"
        output += pinlabels_output+"]\n"
        return output

    def setXDC(self, component):
        added_vars = []
        output = "## Pinout generated for "+component.GetReference()+"\n"
        pinout = get_pins(component)
        for pad in pinout:
            var_name = str_to_C_variable(pad.GetNetname())
            if var_name in added_vars or not pad_is_connected(pad) or pad_is_power(pad) or pad_is_passive(pad):
                 output += "# "
            else:
                added_vars.append(var_name)
            output += "set_property -dict { PACKAGE_PIN "+pad.GetNumber()+"    IOSTANDARD LVCMOS33 } [get_ports { "+var_name+" }];\n"
        return output

    def setPDC(self, component):
        added_vars = []
        output = "## Pinout generated for "+component.GetReference()+"\n"
        pinout = get_pins(component)
        for pad in pinout:
            var_name = str_to_C_variable(pad.GetNetname())
            if var_name in added_vars or not pad_is_connected(pad) or pad_is_power(pad) or pad.GetNumber() == '' or pad_is_passive(pad):
                 output += "# "
            else:
                added_vars.append(var_name)
            output += "set_io "+var_name+" -pinname "+pad.GetNumber()+" -fixed yes"+"\n"
        return output


    def Run(self):
        # Look for selected FP
        self.footprint_selection = []
        for footprint in pcbnew.GetBoard().GetFootprints():
            if footprint.IsSelected():
                self.footprint_selection .append(footprint)

        # Also check for selected pads, and add parent to selection (UX TBC)
        # try:
        #     for pad in pcbnew.GetBoard().GetPads():
        #         if pad.IsSelected():
        #             if pad.GetParent() not in self.footprint_selection:
        #                 self.footprint_selection .append(pad.GetParent())
        # except Exception: 
        #     pass

        # check selection len
        if len(self.footprint_selection ) < 1:
            wx.MessageBox("Select at least one component!")
            return

        # set up and show the GUI
        dialog = PinoutDialog(None)
        dialog.output_format.Bind( wx.EVT_CHOICE, self.change_format )
        # self.is_pinname_not_number = dialog.pinname_cb.GetValue()
        # dialog.pinname_cb.Bind( wx.EVT_CHECKBOX, self.change_format )
        self.set_result = dialog.result.SetValue
        self.get_selection = dialog.output_format.GetSelection
        self.set_output() # set result to default format
        modal_result = dialog.ShowModal()
        dialog.Destroy()
