#!/usr/bin/env python

import wx
import os
import pcbnew
import gettext
import re

from . import pinout_generator_result

SELECTOR = {
    'list':0,
    'csv':1,
    'html':2,
    'md':3,
    'c_define':4,
    'c_enum':5,
    'python_dict':6,
    'wireviz':7,
    'fpga_xdc':8,
    'fpga_pdc':9,
    'rust':10
}

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
    out = string
    out = re.sub(r'[ /]', '_', out)
    out = re.sub(r'-', 'N', out)
    out = re.sub(r'\+', 'P', out)
    out = re.sub(r'[^a-zA-Z0-9\_]', '', out)
    out = re.sub(r'_+', '_', out)
    return out

def escape_html(string, decorate=False):
    """Escapes the special characters in a HTML string.
    """
    out = re.sub(r'&', '&amp;', string)
    out = re.sub(r'<', '&lt;', out)
    out = re.sub(r'>', '&gt;', out)
    if decorate:
        out = re.sub(r'~{(.+)}', r'<span style="text-decoration:overline">\1</span>', out)
    return out

def escape_markdown(string):
    out = escape_html(string)
    out = re.sub(r'\|', '&#124;', out)
    out = re.sub(r'_', '\\_', out)
    out = re.sub(r'\*', '\\*', out)
    return out

def escape_csv(string,sep=""):
    out = re.sub(r'\"', '""', string)
    out = re.sub(sep, '', out)
    return out

def escape_wireviz(string):
    out = re.sub(r',', '', string)
    return out

def get_pins(component):
    pinout = []
    added_pads = [] 
    for pad in component.Pads():
        if pad.GetNumber() not in added_pads and pad.GetNumber() != '': # filter redundant and bogus pads (eg.: thermal pad)
            pinout.append(pad)
            added_pads.append(pad.GetNumber())
    return pinout

def get_pin_name_unless_NC(pad):
    return (re.sub(r'^/', '', pad.GetNetname()) if pad_is_connected(pad) else 'NC')

def filter_pinname(pinName, filt):
    '''Filters a number out of a pin name'''
    if filt and re.match(r'.*'+filt+'[0-9]+.*', pinName):
        return re.sub(r'.*'+filt+'([0-9]+).*', r'\1', pinName)
    return pinName

class PinoutGenerator(pcbnew.ActionPlugin):
    def defaults(self):
        self.name = "Pinout Generator"
        self.category = "Read PCB"
        self.description = "Generates pinout exports from the PCB nets"
        self.show_toolbar_button = True
        self.icon_file_name = os.path.join(os.path.dirname(__file__), 'logo.png')

    def change_format( self, event ):
        self.enable_cb(self.get_selection() in [SELECTOR['c_enum'], SELECTOR['c_define'], SELECTOR['python_dict']])
        self.enable_filter(self.is_pinname_not_number())
        self.set_output(self.get_selection())
        event.Skip()

    def set_output(self, selection):
        if len(self.footprint_selection) > 1 and selection in [SELECTOR['c_define'], SELECTOR['fpga_xdc'], SELECTOR['fpga_pdc']]:
            self.set_result("This format is not compatible with multiple selection. Only select a single component.")
            return

        output = ""
        if selection == SELECTOR['list']:
            output_formater = self.list_format
        if selection == SELECTOR['csv']:
            output_formater = self.csv_format
        elif selection == SELECTOR['html']:
            output_formater = self.html_format
        elif selection == SELECTOR['md']:
            output_formater = self.markdown_format
        elif selection == SELECTOR['c_enum']:
            output_formater = self.c_enum_format
        elif selection == SELECTOR['c_define']:
            output_formater = self.c_define_format
        elif selection == SELECTOR['python_dict']:
            output_formater = self.python_dict_format
        elif selection == SELECTOR['wireviz']:
            output_formater = self.wireviz_format
            output += "connectors:\n"
        elif selection == SELECTOR['fpga_xdc']:
            output_formater = self.xdc_format
        elif selection == SELECTOR['fpga_pdc']:
            output_formater = self.pdc_format
        elif selection == SELECTOR['rust']:
            output_formater = self.rust_format

        # checks for format n
        for component in self.footprint_selection:
            output += output_formater(component)
        self.set_result(output)

    def csv_format(self, component, sep=",", quote="\""):
        output = ""
        pinout = get_pins(component)
        for pad in pinout:
            if len(self.footprint_selection) > 1:
                output += quote + escape_csv(component.GetReference(),sep) + quote + sep 
            output += (quote + escape_csv(pad.GetNumber(),sep) + quote + sep + quote + 
                escape_csv(pad.GetPinFunction(),sep) + quote + sep + quote +  
                escape_csv(get_pin_name_unless_NC(pad),sep) + quote + "\n")
        return output

    def list_format(self, component):
        return self.csv_format(component, "\t", "")

    def html_format(self, component):
        output =  "<p>Pinout for "+escape_html(component.GetReference())+" ("+escape_html(component.GetValue())+"):</p>\n"
        output += "<table>\n"
        output += "\t<tr><th>Pin number</th><th>Pin name</th><th>Pin net</th></tr>\n"
        pinout = get_pins(component)
        for pad in pinout:
            output += ("\t<tr><td>" + escape_html(pad.GetNumber()) + "</td><td>" + 
                escape_html(pad.GetPinFunction(), True) + "</td><td>" +  
                escape_html(get_pin_name_unless_NC(pad), True) + "</td></tr>\n")
        output += "</table>\n"
        return output

    def c_enum_format(self, component):
        added_vars = []
        output = "enum pinout_" + str_to_C_variable(component.GetReference())  + "{\n" # FIXME unsafe
        pinout = get_pins(component)
        for pad in pinout:
            var_name = str_to_C_variable("pin_" + pad.GetNetname())
            var_value = filter_pinname(pad.GetPinFunction(), self.get_pin_name_filter()) if self.is_pinname_not_number() else pad.GetNumber()

            if var_name in added_vars or not var_value.isdigit() or not pad_is_connected(pad) or pad_is_power(pad):
                 output += "//"
            else:
                 added_vars.append(var_name)
            output += "\t" + var_name + "=" + var_value +",\n" 
        output += "};\n"
        return output

    def c_define_format(self, component):
        added_vars = []
        output = "// Pinout for "+component.GetReference()+" ("+component.GetValue()+")\n"
        pinout = get_pins(component)
        for pad in pinout:
            var_name = str_to_C_variable("pin_" + pad.GetNetname()).upper()
            var_value = filter_pinname(pad.GetPinFunction(), self.get_pin_name_filter()) if self.is_pinname_not_number() else pad.GetNumber()

            if var_name in added_vars or not pad_is_connected(pad) or pad_is_power(pad):
                 output += "// "
            else:
                 added_vars.append(var_name)
            output += "#define " + var_name + " " + var_value +"\n" 
        return output

    def python_dict_format(self, component):
        added_vars = []
        output = "pinout_"+str_to_C_variable(component.GetReference())+" = {\n"
        pinout = get_pins(component)
        for pad in pinout:
            var_name = str_to_C_variable("pin_" + pad.GetNetname())
            var_value = filter_pinname(pad.GetPinFunction(),self.get_pin_name_filter()) if self.is_pinname_not_number() else pad.GetNumber()

            if var_name in added_vars or not pad_is_connected(pad) or pad_is_power(pad):
                 output += "#"
            else:
                 added_vars.append(var_name)
            output += "    \'" + var_name + "\' : "
            if var_value.isdigit():
                output += var_value + ',\n'
            else:
                output += "\'" + var_value + "\'" + ',\n'
        output += "}\n"
        return output

    def markdown_format(self, component):
        output = "Pinout for "+escape_markdown(component.GetReference())+" ("+escape_markdown(component.GetValue())+"):\n\n"
        pinout = get_pins(component)
        max_len_num, max_len_name, max_fn_name = len('Pin number'),len('Pin net'), len('Pin name')
        for pad in pinout:
            max_fn_name = max(max_fn_name, len(escape_markdown(pad.GetPinFunction())))
            max_len_num = max(max_len_num, len(escape_markdown(pad.GetNumber())))
            max_len_name = max(max_len_name, len(escape_markdown(get_pin_name_unless_NC(pad))))

        output += ("| Pin number" + ' '*(max_len_num-len('Pin number')) + " | Pin name" + 
            ' '*(max_fn_name-len('Pin name')) + " | Pin net" + ' '*(max_len_name-len('Pin net')) + " |\n")
        output +="|---"+'-'*(max_len_num-1)+"|---"+'-'*(max_fn_name-1)+"|---"+'-'*(max_len_name-1)+"|\n"
        
        for pad in pinout:
            output += ("| " + escape_markdown(pad.GetNumber()) + ' '*(max_len_num-len(escape_markdown(pad.GetNumber()))) + 
                " | "  + escape_markdown(pad.GetPinFunction()) + ' '*(max_fn_name-len(escape_markdown(pad.GetPinFunction()))) +
                " | " +  escape_markdown(get_pin_name_unless_NC(pad)) + ' '*(max_len_name-len(escape_markdown(get_pin_name_unless_NC(pad)))) +
                " |\n")
        return output

    def wireviz_format(self, component):
        output = "    "+component.GetReference()+":\n"
        output += "        type: "+component.GetValue()+"\n"
        pins_output = "        pins: ["
        pinlabels_output = "        pinlabels: ["
        pinout = get_pins(component)
        for pad in pinout:
            pins_output += escape_wireviz(pad.GetNumber())+", "
            pinlabels_output += re.sub(r'^/', '', escape_wireviz(get_pin_name_unless_NC(pad)))+", "
        output += pins_output+"]\n"
        output += pinlabels_output+"]\n"
        return output

    def xdc_format(self, component):
        added_vars = []
        output = "## Pinout generated for "+component.GetReference()+" ("+component.GetValue()+")\n"
        pinout = get_pins(component)
        for pad in pinout:
            var_name = str_to_C_variable("pin_" + pad.GetNetname())
            if var_name in added_vars or not pad_is_connected(pad) or pad_is_power(pad) or pad_is_passive(pad):
                 output += "# "
            else:
                added_vars.append(var_name)
            output += "set_property -dict { PACKAGE_PIN "+pad.GetNumber()+"    IOSTANDARD LVCMOS33 } [get_ports { "+var_name+" }];\n"
        return output

    def pdc_format(self, component):
        added_vars = []
        output = "## Pinout generated for "+component.GetReference()+" ("+component.GetValue()+")\n"
        pinout = get_pins(component)
        for pad in pinout:
            var_name = str_to_C_variable("pin_" + pad.GetNetname())
            if var_name in added_vars or not pad_is_connected(pad) or pad_is_power(pad) or pad_is_passive(pad):
                 output += "# "
            else:
                added_vars.append(var_name)
            output += "set_io "+var_name+" -pinname "+pad.GetNumber()+" -fixed yes"+"\n"
        return output

    def rust_format(self, component):
        added_vars = []
        output = "// Pinout generated for "+component.GetReference()+" ("+component.GetValue()+")\n"
        output = "pub enum " + component.GetReference() + " {\n"
        pinout = get_pins(component)
        for pad in pinout:
            var_name = str_to_C_variable("pin_" + pad.GetNetname())
            if var_name in added_vars:
                output += "//"
            if pad_is_connected(pad) and not (pad_is_power(pad) or pad_is_passive(pad)):
                output += "\t" + var_name+" = "+pad.GetNumber()+",\n"
                added_vars.append(var_name)
        output += "}\n"
        return output


    def Run(self):
        # Look for selected FP
        self.footprint_selection = []
        for footprint in pcbnew.GetBoard().GetFootprints():
            if footprint.IsSelected():
                self.footprint_selection.append(footprint)

        # Also check for selected pads, and add parent to selection (UX TBC)
        # FIXME this add the FP once per pad?
        # try:
        #     for pad in pcbnew.GetBoard().GetPads():
        #         if pad.IsSelected():
        #             if pad.GetParent() not in self.footprint_selection:
        #                 self.footprint_selection .append(pad.GetParent())
        # except Exception: 
        #     pass

        # Check selection len
        if len(self.footprint_selection ) < 1:
            wx.MessageBox("Select at least one component!")
            return

        # WX setup
        dialog = PinoutDialog(None)
        dialog.output_format.Bind(wx.EVT_CHOICE, self.change_format)
        dialog.pinNameCB.Bind(wx.EVT_CHECKBOX, self.change_format)
        dialog.pinNameFilter.Bind(wx.EVT_TEXT, self.change_format)

        # wx form controls
        self.set_result = dialog.result.SetValue
        self.get_selection = dialog.output_format.GetSelection
        self.enable_cb = dialog.pinNameCB.Enable
        self.is_pinname_not_number = dialog.pinNameCB.GetValue
        self.get_pin_name_filter = dialog.pinNameFilter.GetValue
        self.enable_filter = dialog.pinNameFilter.Enable

        # Init output
        self.set_output(self.get_selection())
        modal_result = dialog.ShowModal()
        dialog.Destroy()
