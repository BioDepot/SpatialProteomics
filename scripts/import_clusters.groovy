//Script to import a folder of CytoMAP CSV files into their correct QuPath images
//Select one particular folder that *only has CSV files exported from CytoMAP in it,* and import them into their matching QuPath images.
// Version 3, dramatic speed increase and error checking.
// Modified from Michael Nelson February 2021.
def project = getProject()
// def folder = new File("/data/clustering_data_export")
def folder = new File(System.getenv("clustering_dir"))
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
    println measurementNames
    println "Adding results from " + file
    println "This may take some time, please be patient"
    csv = []
    Set imageList = []
    while ((row = csvReader.readLine()) != null) {
        toAdd = row.split(',')
        imageList.add(toAdd[2])
        csv << toAdd
    }
    int t = 0
    int z = 0


    println imageList
    imageList.each { image ->
        entry = project.getImageList().find { it.getImageName() == image }

        if (entry == null) {
            println "NO ENTRIES FOR IMAGE " + image; return;
        }
        imageData = entry.readImageData()
        hierarchy = imageData.getHierarchy()
        pixelSize = imageData.getServer().getPixelCalibration().getAveragedPixelSizeMicrons()

        csvSubset = csv.findAll { it[2] == image }
        //println("csv subset "+csvSubset)
        objects = hierarchy.getDetectionObjects()//.findAll{it.getPathClass() == getPathClass("Islet")}
        ob = new ObservableMeasurementTableData();
        ob.setImageData(imageData, objects);
        
	cellnum = 1

        csvSubset.each { line ->
            println "Cell #" + cellnum
            cellnum += 1
            x = line[0] as double
            y = line[1] as double
            clusterValue = line[3] as int
            // Location-based lookup
            object = PathObjectTools
                .getObjectsForLocation(hierarchy, x / pixelSize, y / pixelSize, t, z, -1)
                .find { it.isDetection() }
            if (object == null) {
                print "ERROR, OBJECT NOT FOUND AT " + x + "," + y; return
            }

            // Proper output is "Cell", not "null"
            println "pre-object: $object"
            
            // OLD METHOD OF CHECKING; DELETE THIS WHEN READY
            if (round(ob.getNumericValue(object, "Centroid X µm")) != x || round(ob.getNumericValue(object, "Centroid Y µm")) != y) {
                println "OBJECT X & Y EXACT MATCH NOT FOUND: TEST FOR CLOSEBY CENTROID CANDIDATES"
                // object = objects.find { round(ob.getNumericValue(it, "Centroid X µm")) == x && round(ob.getNumericValue(it, "Centroid Y µm")) == y }
            }

            // CSV's x & y rounded to 1st decimal, while object has more decimals.
            // Test if “object” is already within tolerance (`tol`) of the CSV centroid.
            double tol = 0.1   // µm
            double objX = ob.getNumericValue(object, "Centroid X µm")
            double objY = ob.getNumericValue(object, "Centroid Y µm")
            

            // If it’s more than tol away, find a better match
            if (Math.abs(objX - x) > tol || Math.abs(objY - y) > tol) {
                println "object's initial x & y: " + objX + ", " + objY
                println "Out of tolerance (dx=${objX-x}, dy=${objY-y}); searching candidates…"
                def candidate = objects.find { det ->
                    Math.abs(ob.getNumericValue(det, "Centroid X µm") - x) < tol &&
                    Math.abs(ob.getNumericValue(det, "Centroid Y µm") - y) < tol
                }
                if (candidate != null) {
                    object = candidate
                    // recompute for logging
                    objX = ob.getNumericValue(object, "Centroid X µm")
                    objY = ob.getNumericValue(object, "Centroid Y µm")
                    println "Switched to candidate at $objX, $objY"
                } else {
                    println "No candidate within +/- $tol µm; keeping original"
                }
            }

            // if (Math.abs(objX - x) > tol || Math.abs(objY - y) > tol) {
            //     // try to find a better match
            //     println "COORDINATE DIFFERENCES ABOVE TOLERANCE THRESHOLD, CREATING A CANDIDATE..."
            //     def candidate = objects.find { det ->
            //         double dx = Math.abs(ob.getNumericValue(det, "Centroid X µm") - x)
            //         double dy = Math.abs(ob.getNumericValue(det, "Centroid Y µm") - y)
            //         return dx < tol && dy < tol
            //     }
            //     if (candidate != null) {
            //         println "SWITCHING OBJECT TO CLOSEBY CANDIDATE"
            //         object = candidate
            //     }  // otherwise keep the original “object”
            // }

            // Assign cluster-specific color dynamically
            if (!clusterColorMap.containsKey(clusterValue)) {
                def clusterColor = getColorFromHex(line[4])
                clusterColorMap[clusterValue] = PathClass.getInstance("Cluster ${clusterValue}", clusterColor)
            }


            def pathClass = clusterColorMap[clusterValue]
            // println "CSV's x & y: $x, $y"
            // println "object's x & y: $objX, $objY"
            // println "cluster value: " + clusterValue
            // println "image data: " + imageData
            // println "hierarchy: " + hierarchy
            // println "pixel size: " + pixelSize
            // println "object: " + object

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
