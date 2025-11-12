import qupath.lib.projects.*
import qupath.lib.scripting.QP
import qupath.ext.stardist.StarDist2D
import qupath.lib.objects.PathObjects
import qupath.lib.io.PathIO
import qupath.lib.gui.QuPathGUI
import groovy.time.*
import qupath.lib.gui.scripting.QPEx
import javafx.stage.Stage

guiscript = true
def imageName = '240508_QCTMA2_Scan1.qptiff'
// Open the project
def project = getProject()
if (project == null) {
    println 'Error: Could not load project!'
    return
}

imageName = System.getenv("image_to_segment")

// Find the specified image in the project
def entry = project.getImageList().find { it.getImageName() == imageName }
if (entry == null) {
    println "Error: Could not find image '${imageName}' in the project!"
    return
}

// Load the image data
def imageData = entry.readImageData()
if (imageData == null) {
    println 'Error: Could not load image data!'
    return
}

QP.setDefaultImageData(imageData)

// Define the StarDist model path
def pathModel = '/data/scripts/stardist_cell_seg_model.pb'
def resolution = 0.5
if(System.getenv("resolution") != null) {
    resolution = Double.parseDouble(System.getenv("resolution"))
}
// Build the StarDist model
def stardist = StarDist2D.builder(pathModel)
        .threshold(0.5)              // Probability (detection) threshold
        .channels(0)                 // Select detection channel
        .normalizePercentiles(1, 99) // Percentile normalization
        .pixelSize(resolution)              // Resolution for detection
        .cellExpansion(5.0)          // Approximate cells based upon nucleus expansion
        .cellConstrainScale(1.5)     // Constrain cell expansion using nucleus size
        .measureShape()              // Add shape measurements
        .measureIntensity()          // Add cell measurements (in all compartments)
        .includeProbability(true)    // Add probability as a measurement (enables later filtering)
        .build()

// Run detection for the entire image
def hierarchy = imageData.getHierarchy()
def annotations = hierarchy.getAnnotationObjects()
def numAnnotations = annotations.size()
println "Number of annotations: " + numAnnotations
def timeStartCellDetection = new Date()

// Run StarDist
println("StarDist is detecting cells - please wait...")

def coreToSegment = System.getenv("core_to_segment")
println "MY_ENV_VAR:coreToSegment $coreToSegment"

for (annotation in annotations) {
    hierarchy.getSelectionModel().clearSelection()
    hierarchy.getSelectionModel().setSelectedObject(annotation)
//  println '# of Selected objects: ' + hierarchy.getSelectionModel().getSelectedObjects().size()
//  println 'Selected object empty: ' + hierarchy.getSelectionModel().getSelectedObjects().isEmpty()
    if(coreToSegment != null && coreToSegment != "") {
        if(annotation.getName() == "$coreToSegment") {
            println annotation.getName()
            stardist.detectObjects(imageData, hierarchy.getSelectionModel().getSelectedObjects())
            break
        }
    } else {
        println annotation.getName()
        stardist.detectObjects(imageData, hierarchy.getSelectionModel().getSelectedObjects())
    }
}

TimeDuration CellDetectionDuration = TimeCategory.minus(new Date(), timeStartCellDetection)
def numCells = getCellObjects().size()
println("StarDist detected " + numCells + " cells in " + CellDetectionDuration)
//println("Done!")

println 'Segmentation done!'
entry.saveImageData(imageData)
project.syncChanges()

println 'Project last synchronized: ' + project.getModificationTimestamp()
