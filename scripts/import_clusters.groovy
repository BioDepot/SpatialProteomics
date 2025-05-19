//Script to import a folder of CytoMAP CSV files into their correct QuPath images
//Select one particular folder that *only has CSV files exported from CytoMAP in it,* and import them into their matching QuPath images.
// Version 3, dramatic speed increase and error checking.
// Modified from Michael Nelson February 2021.
def project = getProject()
def folder = new File("/data/clustering_data_export")
// Create map for dynamic cluster-to-color mapping
def clusterColorMap = [:]
folder.listFiles().each { file ->
    // Create BufferedReader
    def csvReader = new BufferedReader(new FileReader(file));


    //The rest of the script assumes the X coordinate is in column1, Y in column2, and all other columns are to be imported.
    row = csvReader.readLine() // first row (header)
    measurementNames = row.split(',')
    length = row.split(',').size()
    //measurementNames -= 'X'
    //measurementNames -= 'Y'
    print measurementNames
    print "Adding results from " + file
    print "This may take some time, please be patient"
    csv = []
    Set imageList = []
    while ((row = csvReader.readLine()) != null) {
        toAdd = row.split(',')
        imageList.add(toAdd[2])
        csv << toAdd
    }
    int t = 0
    int z = 0


    print imageList
    imageList.each { image ->
        entry = project.getImageList().find { it.getImageName() == image }

        if (entry == null) {
            print "NO ENTRIES FOR IMAGE " + image; return;
        }
        imageData = entry.readImageData()
        hierarchy = imageData.getHierarchy()
        pixelSize = imageData.getServer().getPixelCalibration().getAveragedPixelSizeMicrons()

        csvSubset = csv.findAll { it[2] == image }
        //println("csv subset "+csvSubset)
        objects = hierarchy.getDetectionObjects()//.findAll{it.getPathClass() == getPathClass("Islet")}
        ob = new ObservableMeasurementTableData();
        ob.setImageData(imageData, objects);

        csvSubset.each { line ->
            x = line[0] as double
            y = line[1] as double
            clusterValue = line[3] as int
            object = PathObjectTools.getObjectsForLocation(hierarchy, x / pixelSize, y / pixelSize, t, z, -1).find { it.isDetection() }
            if (object == null) {
                print "ERROR, OBJECT NOT FOUND AT " + x + "," + y; return
            }
            if (round(ob.getNumericValue(object, "Centroid X µm")) != x || round(ob.getNumericValue(object, "Centroid Y µm")) != y) {
                object = objects.find { round(ob.getNumericValue(it, "Centroid X µm")) == x && round(ob.getNumericValue(it, "Centroid Y µm")) == y }
            }
            // Assign cluster-specific color dynamically
            if (!clusterColorMap.containsKey(clusterValue)) {
                def clusterColor = getColorFromHex(line[4])
                clusterColorMap[clusterValue] = PathClass.getInstance("Cluster ${clusterValue}", clusterColor)
            }


            def pathClass = clusterColorMap[clusterValue]
            object.setPathClass(pathClass)

            i = 3 //skip the X Y and Image entries

            while (i < length-1) {
                //toAdd = row.split(',')[i] as double
                object.getMeasurementList().putMeasurement(measurementNames[i], line[i] as double)
                i++
            }
            objects.remove(object)
        }
        entry.saveImageData(imageData)
    }

    // Output the cluster-color mapping
    println "Cluster-color mapping for ${file.getName()}:"
    clusterColorMap.each { cluster, pathClass ->
        println "Cluster ${cluster}: Color ${pathClass.getColor()}"
    }
}

print "Done with all images!"

def round(double number) {
    BigDecimal bd = new BigDecimal(number)
    def result
    if (number < 100) {
        bd = bd.round(new MathContext(4))
        result = bd.doubleValue()
    } else if (number < 1000) {
        result = number.round(2)
    } else {
        result = number.round(1)
    }

    return result
}


def getColorFromHex(hexCode) {
    // Remove '#' if present
    hexCode = hexCode.replace("#", "")

    // Parse RGB components
    def r = Integer.parseInt(hexCode.substring(0, 2), 16)
    def g = Integer.parseInt(hexCode.substring(2, 4), 16)
    def b = Integer.parseInt(hexCode.substring(4, 6), 16)

    // Return the equivalent QuPath color
    return ColorTools.makeRGB(r, g, b)
}

import qupath.lib.gui.measure.ObservableMeasurementTableData
import qupath.lib.objects.classes.PathClass
import qupath.lib.common.ColorTools
import java.math.MathContext
import java.util.Random
