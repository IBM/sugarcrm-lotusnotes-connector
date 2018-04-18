dependencies = {
	
	action: "release",
	cssOptimize: "comments",
	mini: true,
	optimize: "closure",
	layerOptimize: "closure",
	stripConsole: "all",
	selectorEngine: "acme",

	layers: [
		{
			// This is a specially named layer, literally 'dojo.js'
			// adding dependencies to this layer will include the modules
			// in addition to the standard dojo.js base APIs.
			name: "dojo.js",
			dependencies: [
				"dojo.parser",
			        "dijit.form.Form",
			        "dijit.form.DateTextBox",
			        "dijit.form.SimpleTextarea",
			        "dijit.form.ValidationTextBox",
			        "dijit.form.TextBox",
			        "dijit.form.NumberTextBox",
			        "dijit.form.Select",
			        "dijit.form.MultiSelect",
			        "dijit.form.Button",
			        "dijit.form.FilteringSelect",
			        "dojo.data.ItemFileReadStore",
			        "dijit.Tooltip",
			        "dojo.ready",
				"dojo.require",
			        "dijit.registry",
			        "dijit.Dialog",
			        "dojo.dom-construct",
				//stuff that we don't reference and should have been picked up automatically, but wasn't
				"dijit.form.ComboButton",
				"dijit.form.ToggleButton",
				"dijit.form._ToggleButtonMixin",
				"dijit.PopupMenuItem",
				"dijit.TooltipDialog",
				"dijit._Widget",
				"dijit.CheckedMenuItem",
                                "dijit._base"
			]
		}
	],

	prefixes: [
		[ "dijit", "../dijit" ],
		[ "dojox", "../dojox" ]
	]
}
