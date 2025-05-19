import qupath.lib.objects.PathCellObject
import qupath.lib.gui.commands.SummaryMeasurementTableCommand
import qupath.lib.gui.prefs.PathPrefs
import qupath.lib.gui.measure.ObservableMeasurementTableData
import qupath.fx.dialogs.FileChoosers
import qupath.fx.dialogs.Dialogs

def project = getProject()
def images_to_export = project.getImageList()

def separator = ","
def export_type = PathCellObject.class

print "Generating table data..."

def model = new ObservableMeasurementTableData()

print "${project.getImageList().size()} images"

// Get a specific environment variable
def imageToExport = System.getenv("image_to_export")
println "MY_ENV_VAR: $imageToExport"

def image_list = project.getImageList()
println "Image List: " + image_list
def image = image_list.find { it.getImageName() == imageToExport }
/*def image = Dialogs.showChoiceDialog(
        "Select Image",
        "Select image to to export measurements from",
        image_list,
        image_list.head(),
)*/
/*println "Image List: " + image_list

// Convert the options list into a comma-separated string
def optionsStr = image_list.join(";")

println "Options String: "+ optionsStr

// Execute the expect script with the options as an argument
def command = "expect /data/choose_image_to_export.exp " + optionsStr
def process = command.execute()

// Handle the output and input streams
process.in.eachLine { line -> println line }

// Wait for the process to complete and capture the user's choice
process.waitFor()
def userInput = process.in.text.trim()

// Print the user's choice
println "User selected: $userInput"*/

print "${image}"

if (image == null) {
    throw new Exception("No non-empty image found")
}

def image_data = image.readImageData()
path_objects = image_data.getHierarchy().getObjects(null, export_type)

print "using image with ${path_objects.size()} path objects"

/*def output_file = FileChoosers.promptToSaveFile(
        "Choose output. csv. file",
        new File("all-cell-measuremnts.csv"),
        FileChoosers.createExtensionFilter("csv", "*.csv"))*/
def output_file = new File("all-cell-measurements.csv")

model.setImageData(image_data, path_objects)

println "Measurement columns: ${model.getMeasurementNames().getSize()}"
println "Metadata columns: ${model.getMetadataNames().getSize()}"
println "Measure columns: ${model.getMetadataNames()}"

def default_delim = PathPrefs.tableDelimiterProperty().get()
PathPrefs.tableDelimiterProperty().set(",")

print "Writing to file..."

def excluded_columns = ["Leiden Cluster Label"]//model.getMetadataNames()
def data = SummaryMeasurementTableCommand.saveTableModel(
        model, output_file, excluded_columns)

PathPrefs.tableDelimiterProperty().set(default_delim)

print "Done!"