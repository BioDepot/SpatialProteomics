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

class OWQuPath_Segmentation(OWBwBWidget):
    name = "QuPath_Segmentation"
    description = "StarDist cell segmentation"
    priority = 1
    icon = getIconName(__file__,"qupath_icon.jpeg")
    want_main_area = False
    docker_image_name = "brycenofu/qupath"
    docker_image_tag = "0.5.1"
    inputs = [("qupathdir",str,"handleInputsqupathdir"),("core_to_segment",str,"handleInputscore_to_segment"),("resolution",str,"handleInputsresolution"),("image_to_segment",str,"handleInputsimage_to_segment"),("qpprojfile",str,"handleInputsqpprojfile")]
    outputs = [("qupathdir",str)]
    pset=functools.partial(settings.Setting,schema_only=True)
    runMode=pset(0)
    exportGraphics=pset(False)
    runTriggers=pset([])
    triggerReady=pset({})
    inputConnectionsStore=pset({})
    optionsChecked=pset({})
    qupathdir=pset(None)
    core_to_segment=pset([])
    resolution=pset("0.5")
    image_to_segment=pset([])
    qpprojfile=pset(None)
    def __init__(self):
        super().__init__(self.docker_image_name, self.docker_image_tag)
        with open(getJsonName(__file__,"QuPath_Segmentation")) as f:
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
    def handleInputscore_to_segment(self, value, *args):
        if args and len(args) > 0: 
            self.handleInputs("core_to_segment", value, args[0][0], test=args[0][3])
        else:
            self.handleInputs("inputFile", value, None, False)
    def handleInputsresolution(self, value, *args):
        if args and len(args) > 0: 
            self.handleInputs("resolution", value, args[0][0], test=args[0][3])
        else:
            self.handleInputs("inputFile", value, None, False)
    def handleInputsimage_to_segment(self, value, *args):
        if args and len(args) > 0: 
            self.handleInputs("image_to_segment", value, args[0][0], test=args[0][3])
        else:
            self.handleInputs("inputFile", value, None, False)
    def handleInputsqpprojfile(self, value, *args):
        if args and len(args) > 0: 
            self.handleInputs("qpprojfile", value, args[0][0], test=args[0][3])
        else:
            self.handleInputs("inputFile", value, None, False)
    def handleOutputs(self):
        outputValue=None
        if hasattr(self,"qupathdir"):
            outputValue=getattr(self,"qupathdir")
        self.send("qupathdir", outputValue)
