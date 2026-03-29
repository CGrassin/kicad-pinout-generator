#!/usr/bin/env python
"""
pinout_plugin.py – pcbnew extraction + wx UI.

"""

import re
import wx
import os
import pcbnew

from . import pinout_generator_result
from .pinout_data import (
    PinData, ComponentData,
    fmt_list, fmt_csv, fmt_html, fmt_markdown,
    fmt_c_enum, fmt_c_define, fmt_python_dict,
    fmt_wireviz, fmt_xdc, fmt_pdc, fmt_rust, fmt_c_const_int
)

SELECTOR = {
    'list':        0,
    'csv':         1,
    'html':        2,
    'md':          3,
    'c_define':    4,
    'c_enum':      5,
    'python_dict': 6,
    'wireviz':     7,
    'fpga_xdc':    8,
    'fpga_pdc':    9,
    'rust':        10,
    'c_const_int': 11,
}

_SINGLE_ONLY   = {SELECTOR['c_define'], SELECTOR['fpga_xdc'], SELECTOR['fpga_pdc']}
_PIN_NAME_FMTS = {SELECTOR['c_enum'], SELECTOR['c_define'], SELECTOR['c_const_int'], SELECTOR['python_dict'], SELECTOR['rust']}


class PinoutDialog(pinout_generator_result.PinoutDialog):
    def onDeleteClick(self, event):
        return self.EndModal(wx.ID_DELETE)


# ── pcbnew extraction ─────────────────────────────────────────────────────────

def _pad_is_connected(pad) -> bool:
    return ('no_connect' not in pad.GetPinType()) and pad.IsConnected()

def _pad_is_power(pad) -> bool:
    return 'power' in pad.GetPinType()

def _pad_is_passive(pad) -> bool:
    return 'passive' in pad.GetPinType()

def _net_name(pad) -> str:
    return re.sub(r'^/', '', pad.GetNetname()) if _pad_is_connected(pad) else 'NC'

def _extract_component(footprint) -> ComponentData:
    seen, pins = set(), []
    for pad in footprint.Pads():
        num = pad.GetNumber()
        if num == '' or num in seen:
            continue
        seen.add(num)
        pins.append(PinData(
            number=num,
            function=pad.GetPinFunction(),
            net=_net_name(pad),
            type=pad.GetPinType(),
            is_connected=_pad_is_connected(pad),
            is_power=_pad_is_power(pad),
            is_passive=_pad_is_passive(pad),
        ))
    return ComponentData(reference=footprint.GetReference(), value=footprint.GetValue(), pins=pins)


# ── Plugin ────────────────────────────────────────────────────────────────────

class PinoutGenerator(pcbnew.ActionPlugin):

    def defaults(self):
        self.name = "Pinout Generator"
        self.category = "Read PCB"
        self.description = "Generates pinout exports from the PCB nets"
        self.show_toolbar_button = True
        self.icon_file_name = os.path.join(os.path.dirname(__file__), 'logo.png')

    def _build_output(self, sel: int) -> str:
        comps = self._components

        if len(comps) > 1 and sel in _SINGLE_ONLY:
            return "This format is not compatible with multiple selection. Only select a single component."

        use_name  = self._is_pinname_not_number()
        name_filt = self._get_pin_name_filter()

        dispatch = {
            SELECTOR['list']:        lambda: fmt_list(comps),
            SELECTOR['csv']:         lambda: fmt_csv(comps),
            SELECTOR['html']:        lambda: fmt_html(comps),
            SELECTOR['md']:          lambda: fmt_markdown(comps),
            SELECTOR['c_enum']:      lambda: fmt_c_enum(comps, name_filt, use_name),
            SELECTOR['c_define']:    lambda: fmt_c_define(comps, name_filt, use_name),
            SELECTOR['python_dict']: lambda: fmt_python_dict(comps, name_filt, use_name),
            SELECTOR['wireviz']:     lambda: fmt_wireviz(comps),
            SELECTOR['fpga_xdc']:    lambda: fmt_xdc(comps),
            SELECTOR['fpga_pdc']:    lambda: fmt_pdc(comps),
            SELECTOR['rust']:        lambda: fmt_rust(comps, name_filt, use_name),
            SELECTOR['c_const_int']: lambda: fmt_c_const_int(comps, name_filt, use_name),
        }
        return dispatch[sel]()

    def _on_change(self, event):
        sel = self._get_selection()
        self._enable_cb(sel in _PIN_NAME_FMTS)
        self._enable_filter(self._is_pinname_not_number())
        self._set_result(self._build_output(sel))
        event.Skip()

    def Run(self):
        self._components = [
            _extract_component(fp)
            for fp in pcbnew.GetBoard().GetFootprints()
            if fp.IsSelected()
        ]

        if not self._components:
            wx.MessageBox("Select at least one component!")
            return

        dialog = PinoutDialog(None)
        dialog.output_format.Bind(wx.EVT_CHOICE,   self._on_change)
        dialog.pinNameCB.Bind(wx.EVT_CHECKBOX,     self._on_change)
        dialog.pinNameFilter.Bind(wx.EVT_TEXT,     self._on_change)

        self._set_result            = dialog.result.SetValue
        self._get_selection         = dialog.output_format.GetSelection
        self._enable_cb             = dialog.pinNameCB.Enable
        self._is_pinname_not_number = dialog.pinNameCB.GetValue
        self._get_pin_name_filter   = dialog.pinNameFilter.GetValue
        self._enable_filter         = dialog.pinNameFilter.Enable

        self._set_result(self._build_output(self._get_selection()))
        dialog.ShowModal()
        dialog.Destroy()
