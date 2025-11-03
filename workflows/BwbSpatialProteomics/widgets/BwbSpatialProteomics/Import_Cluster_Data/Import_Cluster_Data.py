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

class OWImport_Cluster_Data(OWBwBWidget):
    name = "Import_Cluster_Data"
    description = "Cell cluster CSV to QuPath overlay"
    priority = 1
    icon = getIconName(__file__,"qupath_icon.jpeg")
    want_main_area = False
    docker_image_name = "brycenofu/qupath"
    docker_image_tag = "0.5.1"
    inputs = [("qupathdir",str,"handleInputsqupathdir"),("clustering_dir",str,"handleInputsclustering_dir")]
    outputs = [("qupathdir",str)]
    pset=functools.partial(settings.Setting,schema_only=True)
    runMode=pset(0)
    exportGraphics=pset(False)
    runTriggers=pset([])
    triggerReady=pset({})
    inputConnectionsStore=pset({})
    optionsChecked=pset({})
    qupathdir=pset(None)
    qpProjFile=pset(None)
    clustering_dir=pset(None)
    def __init__(self):
        super().__init__(self.docker_image_name, self.docker_image_tag)
        with open(getJsonName(__file__,"Import_Cluster_Data")) as f:
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
    def handleInputsclustering_dir(self, value, *args):
        if args and len(args) > 0: 
            self.handleInputs("clustering_dir", value, args[0][0], test=args[0][3])
        else:
            self.handleInputs("inputFile", value, None, False)
    def handleOutputs(self):
        outputValue=None
        if hasattr(self,"qupathdir"):
            outputValue=getattr(self,"qupathdir")
        self.send("qupathdir", outputValue)
