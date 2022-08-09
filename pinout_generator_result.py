# -*- coding: utf-8 -*-

###########################################################################
## Python code generated with wxFormBuilder (version 3.10.1-0-g8feb16b)
## http://www.wxformbuilder.org/
##
## PLEASE DO *NOT* EDIT THIS FILE!
###########################################################################

import wx
import wx.xrc

###########################################################################
## Class PinoutDialog
###########################################################################

class PinoutDialog ( wx.Dialog ):

	def __init__( self, parent ):
		wx.Dialog.__init__ ( self, parent, id = wx.ID_ANY, title = u"Pinout result", pos = wx.DefaultPosition, size = wx.Size( 500,400 ), style = wx.DEFAULT_DIALOG_STYLE )

		self.SetSizeHints( wx.DefaultSize, wx.DefaultSize )

		gbSizer1 = wx.GridBagSizer( 0, 0 )
		gbSizer1.SetFlexibleDirection( wx.BOTH )
		gbSizer1.SetNonFlexibleGrowMode( wx.FLEX_GROWMODE_SPECIFIED )

		self.result = wx.TextCtrl( self, wx.ID_ANY, wx.EmptyString, wx.DefaultPosition, wx.DefaultSize, wx.TE_MULTILINE|wx.TE_READONLY|wx.VSCROLL )
		self.result.SetFont( wx.Font( 10, wx.FONTFAMILY_TELETYPE, wx.FONTSTYLE_NORMAL, wx.FONTWEIGHT_NORMAL, False, "Monospace" ) )
		self.result.SetToolTip( u"Result (copy-paste this)" )

		gbSizer1.Add( self.result, wx.GBPosition( 0, 0 ), wx.GBSpan( 1, 1 ), wx.ALL|wx.EXPAND, 5 )

		bSizer4 = wx.BoxSizer( wx.HORIZONTAL )

		self.m_staticText1 = wx.StaticText( self, wx.ID_ANY, u"Select output format", wx.DefaultPosition, wx.DefaultSize, 0 )
		self.m_staticText1.Wrap( -1 )

		bSizer4.Add( self.m_staticText1, 0, wx.ALIGN_CENTER|wx.ALL, 5 )

		output_formatChoices = [ u"List", u"CSV", u"HTML table", u"Markdown table", u"C/C++ code (enum)", u"C/C++ code (define)", u"Python (dict)", u"VHDL (XDC format)", u"WireViz ", u"VHDL (PDC format)" ]
		self.output_format = wx.Choice( self, wx.ID_ANY, wx.DefaultPosition, wx.DefaultSize, output_formatChoices, 0 )
		self.output_format.SetSelection( 0 )
		bSizer4.Add( self.output_format, 0, wx.ALL|wx.EXPAND, 5 )


		gbSizer1.Add( bSizer4, wx.GBPosition( 1, 0 ), wx.GBSpan( 1, 1 ), wx.EXPAND, 5 )


		gbSizer1.AddGrowableCol( 0 )
		gbSizer1.AddGrowableRow( 0 )

		self.SetSizer( gbSizer1 )
		self.Layout()

		self.Centre( wx.BOTH )

	def __del__( self ):
		pass


