"""
pinout_data.py – PCB-agnostic data model and formatters.

Author: Charles Grassin
License: MIT

"""

import re
from dataclasses import dataclass, field


# ── Data model ────────────────────────────────────────────────────────────────

@dataclass
class PinData:
    number: str
    function: str       # pad.GetPinFunction()
    net: str            # resolved net name, or "NC"
    type: str
    is_connected: bool
    is_power: bool
    is_passive: bool


@dataclass
class ComponentData:
    reference: str      # e.g. "U1"
    value: str          # e.g. "STM32F405"
    pins: list[PinData] = field(default_factory=list)


# ── Escape helpers ────────────────────────────────────────────────────────────

def _to_c_variable(s: str) -> str:
    s = re.sub(r'[ /]', '_', s)
    s = re.sub(r'-', 'N', s)
    s = re.sub(r'\+', 'P', s)
    s = re.sub(r'[^a-zA-Z0-9_]', '', s)
    return re.sub(r'_+', '_', s)

def _escape_html(s: str, decorate: bool = False) -> str:
    s = s.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')
    if decorate:
        s = re.sub(r'~{(.+)}', r'<span style="text-decoration:overline">\1</span>', s)
    return s

def _escape_markdown(s: str) -> str:
    s = _escape_html(s)
    s = re.sub(r'\|', '&#124;', s)
    s = re.sub(r'_', '\\_', s)
    return re.sub(r'\*', '\\*', s)

def _escape_csv(s: str, sep: str = ',') -> str:
    return s.replace('"', '""').replace(sep, '')

def _escape_wireviz(s: str) -> str:
    return s.replace(',', '')

def _filter_pinname(name: str, filt: str) -> str:
    if filt and re.match(r'.*' + filt + r'[0-9]+.*', name):
        return re.sub(r'.*' + filt + r'([0-9]+).*', r'\1', name)
    return name


# ── Formatters ────────────────────────────────────────────────────────────────

def fmt_csv(components: list[ComponentData], sep: str = ',', quote: str = '"') -> str:
    multi = len(components) > 1
    lines = []
    for comp in components:
        for pin in comp.pins:
            parts = []
            if multi:
                parts.append(f"{quote}{_escape_csv(comp.reference, sep)}{quote}")
            parts += [
                f"{quote}{_escape_csv(pin.number, sep)}{quote}",
                f"{quote}{_escape_csv(pin.function, sep)}{quote}",
                f"{quote}{_escape_csv(pin.net, sep)}{quote}",
                #f"{quote}{_escape_csv(pin.type, sep)}{quote}",
            ]
            lines.append(sep.join(parts))
    return '\n'.join(lines) + '\n' if lines else ''


def fmt_list(components: list[ComponentData]) -> str:
    return fmt_csv(components, sep='\t', quote='')


def fmt_html(components: list[ComponentData]) -> str:
    out = []
    for comp in components:
        out.append(f"<p>Pinout for {_escape_html(comp.reference)} ({_escape_html(comp.value)}):</p>")
        out.append("<table>")
        out.append("\t<tr><th>Pin number</th><th>Pin name</th><th>Pin net</th></tr>")
        #out.append("\t<tr><th>Pin number</th><th>Pin name</th><th>Pin net</th><th>Pin type</th></tr>")
        for pin in comp.pins:
            out.append(
                f"\t<tr><td>{_escape_html(pin.number)}</td>"
                f"<td>{_escape_html(pin.function, True)}</td>"
                f"<td>{_escape_html(pin.net, True)}</td>"
                # f"<td>{_escape_html(pin.type, True)}</td>"
                "</tr>"
            )
        out.append("</table>")
    return '\n'.join(out) + '\n'


def fmt_markdown(components: list[ComponentData]) -> str:
    out = []
    for comp in components:
        out.append(f"Pinout for {_escape_markdown(comp.reference)} ({_escape_markdown(comp.value)}):\n")
        col_num  = max(len('Pin number'), *(len(_escape_markdown(p.number))   for p in comp.pins))
        col_name = max(len('Pin name'),   *(len(_escape_markdown(p.function)) for p in comp.pins))
        col_net  = max(len('Pin net'),    *(len(_escape_markdown(p.net))      for p in comp.pins))
        # col_type  = max(len('Pin type'),    *(len(_escape_markdown(p.type))      for p in comp.pins))

        def row(a, b, c):
            return f"| {a:<{col_num}} | {b:<{col_name}} | {c:<{col_net}} |" # f"  {d:<{col_type}} |"

        out.append(row('Pin number', 'Pin name', 'Pin net'))
        #out.append(row('Pin number', 'Pin name', 'Pin net', 'Pin type'))
        out.append(f"|{'-'*(col_num+2)}|{'-'*(col_name+2)}|{'-'*(col_net+2)}|")
        for pin in comp.pins:
            out.append(row(
                _escape_markdown(pin.number),
                _escape_markdown(pin.function),
                _escape_markdown(pin.net),
            ))
        out.append('')
    return '\n'.join(out)

def fmt_c_enum(
    components: list[ComponentData],
    pin_name_filter: str = '',
    use_pin_name: bool = False,
) -> str:
    out = []
    for comp in components:
        seen = set()
        out.append(f"// Pinout generated for {comp.reference} ({comp.value})")
        out.append(f"enum pinout_{_to_c_variable(comp.reference)}{{")
        for pin in comp.pins:
            var   = _to_c_variable('pin_' + pin.net)
            value = _filter_pinname(pin.function, pin_name_filter) if use_pin_name else pin.number
            skip  = var in seen or not value.isdigit() or not pin.is_connected or pin.is_power
            if not skip:
                seen.add(var)
            out.append(('\t//' if skip else '\t') + f"{var}={value},")
        out.append('};\n')
    return '\n'.join(out)

def fmt_c_define(
    components: list[ComponentData],
    pin_name_filter: str = '',
    use_pin_name: bool = False,
) -> str:
    out = []
    for comp in components:
        seen = set()
        out.append(f"// Pinout generated for {comp.reference} ({comp.value})")
        for pin in comp.pins:
            var   = _to_c_variable('pin_' + pin.net).upper()
            value = _filter_pinname(pin.function, pin_name_filter) if use_pin_name else pin.number
            skip  = var in seen or not pin.is_connected or pin.is_power
            if not skip:
                seen.add(var)
            out.append(f"{'// ' if skip else ''}#define {var} {value}")
        out.append('')
    return '\n'.join(out)

def fmt_c_const_int(
    components: list[ComponentData],
    pin_name_filter: str = '',
    use_pin_name: bool = False,
) -> str:
    out = []
    for comp in components:
        seen = set()
        out.append(f"// Pinout generated for {comp.reference} ({comp.value})")
        for pin in comp.pins:
            var   = _to_c_variable('pin_' + pin.net)
            value = _filter_pinname(pin.function, pin_name_filter) if use_pin_name else pin.number
            skip  = var in seen or not value.isdigit() or not pin.is_connected or pin.is_power
            if not skip:
                seen.add(var)
            out.append(('//' if skip else '') + f"const int {var} = {value},")
        out.append('')
    return '\n'.join(out)

def fmt_python_dict(
    components: list[ComponentData],
    pin_name_filter: str = '',
    use_pin_name: bool = False,
) -> str:
    out = []
    for comp in components:
        seen = set()
        out.append(f"# Pinout generated for {comp.reference} ({comp.value})")
        out.append(f"pinout_{_to_c_variable(comp.reference)} = {{")
        for pin in comp.pins:
            var   = _to_c_variable('pin_' + pin.net)
            value = _filter_pinname(pin.function, pin_name_filter) if use_pin_name else pin.number
            skip  = var in seen or not pin.is_connected or pin.is_power
            if not skip:
                seen.add(var)
            val_repr = value if value.isdigit() else f"'{value}'"
            out.append(f"{'#' if skip else ''}    '{var}': {val_repr},")
        out.append('}\n')
    return '\n'.join(out)


def fmt_wireviz(components: list[ComponentData]) -> str:
    out = ["connectors:"]
    for comp in components:
        out.append(f"    {comp.reference}:")
        out.append(f"        type: {comp.value}")
        pins   = ', '.join(_escape_wireviz(p.number) for p in comp.pins)
        labels = ', '.join(_escape_wireviz(re.sub(r'^/', '', p.net)) for p in comp.pins)
        out.append(f"        pins: [{pins}]")
        out.append(f"        pinlabels: [{labels}]")
    return '\n'.join(out) + '\n'


def fmt_xdc(components: list[ComponentData]) -> str:
    comp = components[0]
    seen = set()
    out = [f"## Pinout generated for {comp.reference} ({comp.value})"]
    for pin in comp.pins:
        var  = _to_c_variable('pin_' + pin.net)
        skip = var in seen or not pin.is_connected or pin.is_power or pin.is_passive
        if not skip:
            seen.add(var)
        out.append(f"{'# ' if skip else ''}set_property -dict {{ PACKAGE_PIN {pin.number}    IOSTANDARD LVCMOS33 }} [get_ports {{ {var} }}];")
    return '\n'.join(out) + '\n'


def fmt_pdc(components: list[ComponentData]) -> str:
    comp = components[0]
    seen = set()
    out = [f"## Pinout generated for {comp.reference} ({comp.value})"]
    for pin in comp.pins:
        var  = _to_c_variable('pin_' + pin.net)
        skip = var in seen or not pin.is_connected or pin.is_power or pin.is_passive
        if not skip:
            seen.add(var)
        out.append(f"{'# ' if skip else ''}set_io {var} -pinname {pin.number} -fixed yes")
    return '\n'.join(out) + '\n'

def fmt_rust(
    components: list[ComponentData],
    pin_name_filter: str = '',
    use_pin_name: bool = False,
) -> str:
    out = []
    for comp in components:
        seen = set()
        out.append(f"// Pinout generated for {comp.reference} ({comp.value})")
        out.append(f"pub enum pinout_{_to_c_variable(comp.reference)}{{")
        for pin in comp.pins:
            var   = _to_c_variable('pin_' + pin.net)
            value = _filter_pinname(pin.function, pin_name_filter) if use_pin_name else pin.number
            skip  = var in seen or not value.isdigit() or not pin.is_connected or pin.is_power
            if not skip:
                seen.add(var)
            out.append(('\t//' if skip else '\t') + f"{var} = {value},")
        out.append('}\n')
    return '\n'.join(out)
