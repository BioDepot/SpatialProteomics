import os
import glob
import sys
import functools
import jsonpickle
from collections import OrderedDict
from Orange.widgets import widget, gui, settings
import Orange.data
from Orange.data.io import FileFormat
from DockerClient import DockerClient
from BwBase import OWBwBWidget, ConnectionDict, BwbGuiElements, getIconName, getJsonName
from PyQt5 import QtWidgets, QtGui

class OWExport_Image_Data(OWBwBWidget):
    name = "Export_Image_Data"
    description = "Cell segmentation data to CSV"
    priority = 1
    icon = getIconName(__file__,"qupath_icon.jpeg")
    want_main_area = False
    docker_image_name = "brycenofu/qupath"
    docker_image_tag = "0.5.1"
    inputs = [("qupathdir",str,"handleInputsqupathdir"),("image_to_export",str,"handleInputsimage_to_export"),("qpprojfile",str,"handleInputsqpprojfile"),("outputDir",str,"handleInputsoutputDir")]
    outputs = [("qupathdir",str),("outputDir",str)]
    pset=functools.partial(settings.Setting,schema_only=True)
    runMode=pset(0)
    exportGraphics=pset(False)
    runTriggers=pset([])
    triggerReady=pset({})
    inputConnectionsStore=pset({})
    optionsChecked=pset({})
    qupathdir=pset(None)
    image_to_export=pset([])
    qpprojfile=pset(None)
    outputDir=pset(None)
    def __init__(self):
        super().__init__(self.docker_image_name, self.docker_image_tag)
        with open(getJsonName(__file__,"Export_Image_Data")) as f:
            self.data=jsonpickle.decode(f.read())
            f.close()
        self.initVolumes()
        self.inputConnections = ConnectionDict(self.inputConnectionsStore)
        self.drawGUI()
    def handleInputsqupathdir(self, value, *args):
        if args and len(args) > 0: 
            self.handleInputs("qupathdir", value, args[0][0], test=args[0][3])
        else:
            self.handleInputs("inputFile", value, None, False)
    def handleInputsimage_to_export(self, value, *args):
        if args and len(args) > 0: 
            self.handleInputs("image_to_export", value, args[0][0], test=args[0][3])
        else:
            self.handleInputs("inputFile", value, None, False)
    def handleInputsqpprojfile(self, value, *args):
        if args and len(args) > 0: 
            self.handleInputs("qpprojfile", value, args[0][0], test=args[0][3])
        else:
            self.handleInputs("inputFile", value, None, False)
    def handleInputsoutputDir(self, value, *args):
        if args and len(args) > 0: 
            self.handleInputs("outputDir", value, args[0][0], test=args[0][3])
        else:
            self.handleInputs("inputFile", value, None, False)
    def handleOutputs(self):
        outputValue=None
        if hasattr(self,"qupathdir"):
            outputValue=getattr(self,"qupathdir")
        self.send("qupathdir", outputValue)
        outputValue=None
        if hasattr(self,"outputDir"):
            outputValue=getattr(self,"outputDir")
        self.send("outputDir", outputValue)
