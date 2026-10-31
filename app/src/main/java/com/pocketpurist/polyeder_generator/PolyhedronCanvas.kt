package com.pocketpurist.polyeder_generator

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.Log
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

class PolyhedronCanvas {
    /** Data & methods for the canvas the polyhedron is drawn on.
     */
    class UserInterface {
        val positiveInf = Float.POSITIVE_INFINITY;  val negativeInf = Float.NEGATIVE_INFINITY
        val generalSettings = GeneralSettings()
        private val axesIndicator = AxesIndicator()
        val pointDummy3D = Point3D(positiveInf, positiveInf, positiveInf, "dummy!")  // "3D Dummy Point".
        val polyhedron = Polyhedron()
        val edgeDummy = polyhedron.Edge(pointDummy3D, pointDummy3D)  // "Dummy Edge".

        /** Draws/Redraws all graphical objects (polyhedron, axes indicator, ...)
         *  on given canvas [canvas].
         */
        fun update(canvas: Canvas) {
            if (generalSettings.showAxesIndicator) axesIndicator.drawOnCanvas(canvas)
            polyhedron.drawOnCanvas(canvas)
        }

        /** App-wide settings.
         *  TODO The variable ones should be stored as preferences.
         *  Specified in pixel if no other unit is specified in the comments.
         */
        class GeneralSettings {
            // Input settings:
            val detectPointsWithinTapDiameter = dpToPixel(60f)  // Diameter around touching the canvas will be sought for existing points.  TODO consider as user setting.
            // Appearance settings:
            var faceOpacity = 0.2f  // Opacity (alpha channel) of polyhedron's (color-picked) faces:  0..1
            var showInvisibleEdges = true  // true => show (parts of) edges that are hidden behind other parts (dashed!).
            val edgesStrokeWidth = 6f  // If set variable on runtime: calling paints.update() is necessary!
            val factorDashedEdgesStroke = 0.6f  // Factor the dashed edges are thinner than the solid ones (with full edgesStrokeWidth).
            val colorEdges = 0xFF000000.toInt()  // If set variable on runtime: calling paints.update() is necessary!
            val colorPoints = colorEdges
            val pointSize = 16f  // Points' diameter.
            var rangeCoordinates = 0..25  // Range of valid X/Y/Z coordinate values.
            var viewAngleAxisX = 300f  // Angle of X axis: 0 = "towards right edge of app" / 90 = "towards upper edge of app" etc.
                set(value) {
                    field = value
                    depthFactor.update()
                }
            var viewAngleAxisY =  20f  // Angle of Y axis
                set(value) {
                    field = value
                    depthFactor.update()
                }
            var viewAngleAxisZ =  90f  // Angle of Z axis
                set(value) {
                    field = value
                    depthFactor.update()
                }
            var showAxisX = true  // false => x axis gets not included in drawing.
            var showAxisY = true  // false => y axis gets not included in drawing.
            var showAxisZ = true  // false => z axis gets not included in drawing.
            val axesMarginLeft = dpToPixel(12.5f)   // Polygon axes' upper left corner: Below the axis indicator (if shown).
            val axesMarginTop = dpToPixel(12.5f)
            val axesMarginRight = dpToPixel(12.5f)  // Polygon axes' lower right edge.
            val axesMarginBottom = dpToPixel(12.5f)
            val axesStrokeWidth = 4f
            val lengthOfAxisArrowArea = dpToPixel(32.5f)  // Distance from ruler's end to end of an axis.
            val lengthOfLateralDartPart = dpToPixel(15f)  // Length of the "inclined part" of dart ends.
            val angleDartPart = 35  // Angle of a dart end in degrees ("how pointy the dart should be").
            // factorAxisX..Z:  Smaller than the largest "factorAxis?" means: Axis is compressed. 0 => compressed to 0. Supposed to be in range: 0f..1f
            var factorAxisX = 0.64f
                set(value) {
                    field = value
                    depthFactor.update()
                }
            var factorAxisY = 0.64f
                set(value) {
                    field = value
                    depthFactor.update()
                }
            var factorAxisZ = 1f
                set(value) {
                    field = value
                    depthFactor.update()
                }
            var showPointCaptions = true
            var showAxesIndicator = true  // Visibility of the "little indicator" of x/y/z axis orientation (next to the polyhedron).
            val axesIndicatorSize = dpToPixel(35f)  // Size of every axis (of the axis indicator).
            val axesIndicatorMarginLeft = dpToPixel(20f)  // Axis Indicator's upper left corner.
            val axesIndicatorMarginTop = dpToPixel(0f)
            val axesIndicatorStrokeWidth = 4f
            val colorAxisX = 0xFFFF0000.toInt()  // Color of X axis.
            val colorAxisY = 0xFF009900.toInt()  // Color of Y axis.
            val colorAxisZ = 0xFF4444FF.toInt()  // Color of Z axis.
            var showPointData = false  // true => include point data chart in drawing.

            private fun dpToPixel(dpValue: Float) = (dpValue * pixelsPerDP + 0.5f).toInt()
            val depthFactor = DepthFactor()
            val paints = Paints()

            /** Depth factor of axes x..z: Delivers a depth factor for coordinates on 2D canvas -
             *  calculated from [viewAngleAxisX]..[viewAngleAxisZ] and [factorAxisX]..[factorAxisZ].
             *  Method [getForPoint] delivers such a factor for a specified 3D point.
             */
            inner class DepthFactor {
                var axesFactorMax = 1f  // Current maximum within factorAxisX .. factorAxisZ  // TODO delete if considered obsolete.
                var axisX = 0.0;  var axisY = 0.0;  var axisZ = 0.0  // "Depth Factor Axis x .. z"
                var correctionFactorXvsY = 1000.0 // Correction-factor of x-axis vs. Y-axis' depth factor  // TODO delete if obsolete.
                init {
                    update()
                }
                fun update() {
                    axesFactorMax = maxOf(factorAxisX, factorAxisY, factorAxisZ)
                    //val depthFactorX = 1f - factorAxisX / axesFactorMax;  axisX = depthFactorX * sinDegrees(viewAngleAxisX.toDouble())  // TODO delete if obsolete.
                    //val depthFactorY = 1f - factorAxisY / axesFactorMax;  axisY = depthFactorY * sinDegrees(viewAngleAxisY.toDouble())  // TODO delete if obsolete.
                    //val depthFactorZ = 1f - factorAxisZ / axesFactorMax;  axisZ = depthFactorZ * sinDegrees(viewAngleAxisZ.toDouble())  // TODO delete if obsolete.

                    // TODO evaluate if the algorithm should be according to the following thoughts:
                    /* 2D-Approach; Depth value calculations of a point along the 3 axes:
                       ((Distance value along e.g. Y axis = hypotenuse = cY = factorAxisY))
                       (X axis):  aX = factorAxisX * sin(viewAngleAxisX)
                       (Y axis) = opposite aY = factorAxisY * sin(viewAngleAxisY)
                       (Z axis):  aZ = factorAXisZ * cos(viewAngleAxisZ)
                    */

                    /** TODO "cook up" angle correction - OR: Delete if obsolete!
                     *    to obtain positive angles < 90 for [viewAngleOfAxis] being within quadrants I & II
                     *    and negative angles < 90 for [viewAngleOfAxis] being within quadrants III & IV.
                     *    RIGHT NOW REDUNDANT:  sin(angleForDepth(viewAngleOfAxis)) == sin(viewAngleOfAxis)
                     */
                    fun angleForDepth(viewAngleOfAxis: Float) = when (viewAngleOfAxis) {
                        in 0.0..<90.0    -> viewAngleOfAxis.toDouble()   // Quadrant I
                        in 90.0..<180.0  -> 180.0 - viewAngleOfAxis
                        in 180.0..<270.0 -> 180.0 - viewAngleOfAxis
                        else -> viewAngleOfAxis - 360.0  // Quadrant IV
                    }
                    axisX = /* factorAxisX * */ sinDegrees(viewAngleAxisX.toDouble())
                    axisY = /* factorAxisY * */ sinDegrees(viewAngleAxisY.toDouble())
                    axisZ = /* factorAxisZ * */ cosDegrees(viewAngleAxisZ.toDouble())
                }

                /** Returns the "depth of a 3D point projected on canvas" for the specified [point3D].
                 */
                fun getForPoint(point3D: Point3D) = point3D.x * axisX * correctionFactorXvsY + point3D.y * axisY + point3D.z * axisZ
            }  // inner class DepthFactorAxes

            /** Stores paints for drawing paths - following the color- and stroke-settings.
             */
            inner class Paints {
                val paintEdgeSolid = Paint()
                val paintEdgeDashed = Paint()
                init {
                    update()
                }
                fun update() {
                    val intervals = FloatArray(2)
                    intervals[0] = 20f; intervals[1] = 7.5f  // Declaration of line type: "dashed".
                    //intervals[0] = 5f; intervals[1] = 10f; intervals[2] = 15f; intervals[3] = 20f  // Declaration of line type:  "dot-dashed".
                    paintEdgeSolid.color = colorEdges
                    paintEdgeSolid.isAntiAlias = true
                    paintEdgeSolid.style = Paint.Style.STROKE
                    paintEdgeSolid.strokeWidth = edgesStrokeWidth

                    paintEdgeDashed.color = colorEdges
                    paintEdgeDashed.isAntiAlias = true
                    paintEdgeDashed.style = Paint.Style.STROKE
                    paintEdgeDashed.strokeWidth = edgesStrokeWidth * factorDashedEdgesStroke
                    paintEdgeDashed.pathEffect = DashPathEffect(intervals,0f)
                }
            }  // inner class Paints
        }  // class GeneralSettings

        /** Integer point coordinates in pixel.
         */
        class PointOnCanvas2D(val x: Int, val y: Int) {
            operator fun component1() = x
            operator fun component2() = y
        }

        /** Float point coordinates in pixel.
         */
        class PointOnCanvasF(val x: Float, val y: Float) {
            operator fun component1() = x
            operator fun component2() = y

            /** Returns true if this PointOnCanvasF instance is a "dummy":
             *  One or both of its coordinates equal [Float.POSITIVE_INFINITY] or [Float.NEGATIVE_INFINITY].
             */
            fun isDummy(): Boolean {
                val positiveInf = Float.POSITIVE_INFINITY;  val negativeInf = Float.NEGATIVE_INFINITY
                return x == positiveInf || x == negativeInf || y == positiveInf || y == negativeInf
            }

            /** Returns distance to a second point [pointSecond] in pixel.
             *  Returns [Float.POSITIVE_INFINITY] if one or more  of the 2 points' coordinates is
             *  [Float.POSITIVE_INFINITY] or [Float.NEGATIVE_INFINITY].
             */
            fun getDistance(pointSecond: PointOnCanvasF): Float {
                var distance = Float.POSITIVE_INFINITY
                if (!isDummy() && !pointSecond.isDummy()) {
                    val xD = x - pointSecond.x;  val yD = y - pointSecond.y
                    distance = sqrt(xD * xD + yD * yD)
                }
                return distance
            }

            /** Returns [x] coordinate as a String with [digits] decimals.
             *  Returns its rounded integer value as a String for [digits] <= 0.
             */
            fun getFormattedX(digits: Int = 2) = x.format(digits)

            /** Returns [y] coordinate as a String with [digits] decimals.
             *  Returns its rounded integer value as a String for [digits] <= 0.
             */
            fun getFormattedY(digits: Int = 2) = y.format(digits)
        }

        /** 3D point coordinates [x]/[y]/[z] & method to get its 2D coordinates projected onto canvas.
         *  [caption]: Optional for naming the point.
         *  [indexOfPoint]: Optional for storing a point's index within an array.
         */
        inner class Point3D(val x: Float, val y: Float, val z: Float, val caption: String = "", val indexOfPoint: Int = -1) {
            operator fun component1() = x
            operator fun component2() = y
            operator fun component3() = z

            /** Returns the 3D point projected onto a "normed 2D canvas": Considering
             *  generalSettings.viewAngleAxisX..Z and generalSettings.factorAxisX..Z.
             */
            fun projectedOntoCanvasF(): PointOnCanvasF {
                val vectorSum: MyMath.Vector
                val vectorPartX = MyMath.Vector((x * generalSettings.factorAxisX).toDouble(), 0.0)
                val vectorPartY = MyMath.Vector((y * generalSettings.factorAxisY).toDouble(), 0.0)
                val vectorPartZ = MyMath.Vector((z * generalSettings.factorAxisZ).toDouble(), 0.0)
                vectorPartX.rotate(generalSettings.viewAngleAxisX.toInt())
                vectorPartY.rotate(generalSettings.viewAngleAxisY.toInt())
                vectorPartZ.rotate(generalSettings.viewAngleAxisZ.toInt())
                vectorSum = MyMath.Vector(vectorPartX)
                vectorSum.add(vectorPartY)
                vectorSum.add(vectorPartZ)
                return PointOnCanvasF(vectorSum.get_dX().toFloat(), vectorSum.get_dY().toFloat())
            }
            fun projectedOntoCanvas(): PointOnCanvas2D {
                val point2D = projectedOntoCanvasF()
                return PointOnCanvas2D(point2D.x.roundToInt(), point2D.y.roundToInt())
            }

            /** Returns true if data for this point projected onto the canvas already exists.
             */
            fun hasCorrespondingCanvasData() = indexOfPoint in polyhedron.polyhedronOnCanvas.polyhedronOnCanvasPointsF.indices

            /** Returns already generated "2D projection point" on a "normed 2D canvas".
             *  Returns a dummy point if it does not exist ( => isDummy() == true ).
             */
            fun getOnCanvas(): PointOnCanvasF {
                var pointOnCanvasF = PointOnCanvasF(positiveInf, positiveInf)
                if (hasCorrespondingCanvasData())
                    pointOnCanvasF = polyhedron.polyhedronOnCanvas.polyhedronOnCanvasPointsF[indexOfPoint]
                return pointOnCanvasF
            }

            /** Returns already generated depth of its "2D projection point" on a "normed 2D canvas".
             *  Returns 0.0 if its depth is zero or if it does not exist.
             */
            fun getDepthOnCanvas(): Double {
                var depthOnCanvas = 0.0
                if (hasCorrespondingCanvasData())
                    depthOnCanvas = polyhedron.polyhedronOnCanvas.polyhedronOnCanvasPointsDepth[indexOfPoint]
                return depthOnCanvas
            }

            /** Returns true if this Point3D instance is a "dummy":
             *  All 3 coordinates equal [Float.POSITIVE_INFINITY] or [Float.NEGATIVE_INFINITY].
             */
            fun isDummy() =
                (x == positiveInf || x == negativeInf) && (y == positiveInf || y == negativeInf) && (z == positiveInf || z == negativeInf)
        }  // inner class Point3D(val x: Float, val y: Float, val z: Float)

        /** Calculates & stores the "dimensional claim" of 3D objects on (2D!) canvas
         *  (projecting them onto the canvas).
         *  (With the possibility to directly specify "2D canvas points" too.)
         */
        inner class DimensionalClaim {
            var extraMarginLeft = 0f  // "extraMargin" means: From point of origin (0,0)
                private set           // needed area in the specified direction.
            var extraMarginTop = 0f
                private set
            var extraMarginRight = 0f
                private set
            var extraMarginBottom = 0f
                private set
            var extraWidth = 0f
                private set
            var extraHeight = 0f
                private set
            fun addPointOnCanvas(point2D: PointOnCanvasF) {
                if (point2D.x < 0) {
                    if (-point2D.x > extraMarginLeft) extraMarginLeft = -point2D.x
                } else if (point2D.x > extraMarginRight) extraMarginRight = point2D.x
                if (point2D.y < 0) {
                    if (-point2D.y > extraMarginTop) extraMarginTop = -point2D.y
                } else if (point2D.y > extraMarginBottom) extraMarginBottom = point2D.y
                extraWidth = extraMarginLeft + extraMarginRight
                extraHeight = extraMarginTop + extraMarginBottom
            }
            fun addPointOnCanvas(point2D: PointOnCanvas2D) {
                val pointF = PointOnCanvasF(point2D.x.toFloat(), point2D.y.toFloat())
                addPointOnCanvas(pointF)
            }
            fun addVectorEndPoint3D(endPoint: Point3D) {
                addPointOnCanvas(endPoint.projectedOntoCanvasF())
            }
            fun reset() {
                extraMarginLeft = 0f;  extraMarginTop = 0f
                extraMarginRight = 0f; extraMarginBottom = 0f
                extraWidth = 0f; extraHeight = 0f
            }
        }  // inner class DimensionalClaim

        /* Draws the little axes indicator on given canvas: x/y/z axis.
         * TODO
         *   add axis arrows & captions.
         *   add (most recent) left / right / top / bottom / width / height
         */
        inner class AxesIndicator {
            var left = 0f
                private set
            var top = 0f
                private set
            var right = 0f
                private set
            var bottom = 0f
                private set
            var width = 0f
                private set
            var height = 0f
                private set
            var originLeft = 0f
                private set
            var originTop = 0f
                private set
            private val paintAxis: Paint = Paint()
            init {
                paintAxis.isAntiAlias = true
                paintAxis.style = Paint.Style.STROKE
                paintAxis.strokeWidth = generalSettings.axesIndicatorStrokeWidth
            }

            /** Updates the instance's properties.
             * [marginLeftToTake] equals the x-coordinate of the axes indicator's origin etc.
             */
            private fun updateDimensions(marginLeftToTake: Int, marginTopToTake: Int, endPointAxisX: PointOnCanvas2D, endPointAxisY: PointOnCanvas2D, endPointAxisZ: PointOnCanvas2D) {
                val dimensionalClaim = DimensionalClaim()
                dimensionalClaim.addPointOnCanvas(PointOnCanvasF(0f, 0f))
                dimensionalClaim.addPointOnCanvas(endPointAxisX)
                dimensionalClaim.addPointOnCanvas(endPointAxisY)
                dimensionalClaim.addPointOnCanvas(endPointAxisZ)
                left = marginLeftToTake - dimensionalClaim.extraMarginLeft
                top = marginTopToTake - dimensionalClaim.extraMarginTop
                width = dimensionalClaim.extraWidth
                height = dimensionalClaim.extraHeight
                right = left + width
                bottom = top + height
                originLeft = marginLeftToTake.toFloat()
                originTop = marginTopToTake.toFloat()
            }

            fun drawOnCanvas(canvas: Canvas) {
                val path = Path()
                var leftestIndicatorPoint = 0
                var upperIndicatorPoint = 0
                var marginLeftToTake = generalSettings.axesIndicatorMarginLeft  // Counters possible negative x-values of axes-ends.
                var marginTopToTake = generalSettings.axesIndicatorMarginTop    // Counters possible negative y-values of axes-ends.
                val axisSize = generalSettings.axesIndicatorSize.toFloat()
                val endPointAxisX = Point3D(axisSize,0f, 0f).projectedOntoCanvas()
                val endPointAxisY = Point3D(0f, axisSize,0f).projectedOntoCanvas()
                val endPointAxisZ = Point3D(0f,0f, axisSize).projectedOntoCanvas()
                // Calculate the correct left/top margin on canvas - find out the very left and upper endpoint of the 3 axes:
                if (endPointAxisX.x < leftestIndicatorPoint) leftestIndicatorPoint = endPointAxisX.x
                if (endPointAxisY.x < leftestIndicatorPoint) leftestIndicatorPoint = endPointAxisY.x
                if (endPointAxisZ.x < leftestIndicatorPoint) leftestIndicatorPoint = endPointAxisZ.x
                if (endPointAxisX.y < upperIndicatorPoint) upperIndicatorPoint = endPointAxisX.y
                if (endPointAxisY.y < upperIndicatorPoint) upperIndicatorPoint = endPointAxisY.y
                if (endPointAxisZ.y < upperIndicatorPoint) upperIndicatorPoint = endPointAxisZ.y
                if (leftestIndicatorPoint < 0) marginLeftToTake -= leftestIndicatorPoint
                if (upperIndicatorPoint < 0) marginTopToTake -= upperIndicatorPoint
                updateDimensions(marginLeftToTake, marginTopToTake, endPointAxisX, endPointAxisY, endPointAxisZ)
                // Draw x-axis on canvas:
                paintAxis.color = generalSettings.colorAxisX
                path.moveTo(marginLeftToTake.toFloat(), marginTopToTake.toFloat())  // margin... serve as "axis origin coordinates" as well.
                path.lineTo((marginLeftToTake + endPointAxisX.x).toFloat(), (marginTopToTake + endPointAxisX.y).toFloat())
                canvas.drawPath(path, paintAxis)
                path.reset()
                // Draw Y-axis on canvas:
                paintAxis.color = generalSettings.colorAxisY
                path.moveTo(marginLeftToTake.toFloat(), marginTopToTake.toFloat())  // margin... serve as "axis origin coordinates" as well.
                path.lineTo((marginLeftToTake + endPointAxisY.x).toFloat(), (marginTopToTake + endPointAxisY.y).toFloat())
                canvas.drawPath(path, paintAxis)
                path.reset()
                // Draw z-axis on canvas:
                paintAxis.color = generalSettings.colorAxisZ
                path.moveTo(marginLeftToTake.toFloat(), marginTopToTake.toFloat())  // margin... serve as "axis origin coordinates" as well.
                path.lineTo((marginLeftToTake + endPointAxisZ.x).toFloat(), (marginTopToTake + endPointAxisZ.y).toFloat())
                canvas.drawPath(path, paintAxis)
            }

            /** TODO delete when considered obsolete.
             */
            fun drawOnCanvas_old(canvas: Canvas) {
                val path = Path()
                val vectorAxisX = MyMath.Vector((generalSettings.axesIndicatorSize * generalSettings.factorAxisX).toDouble(), 0.0)
                val vectorAxisY = MyMath.Vector((generalSettings.axesIndicatorSize * generalSettings.factorAxisY).toDouble(), 0.0)
                val vectorAxisZ = MyMath.Vector((generalSettings.axesIndicatorSize * generalSettings.factorAxisZ).toDouble(), 0.0)
                var leftestIndicatorPoint = 0
                var upperIndicatorPoint = 0
                var marginLeftToTake = generalSettings.axesIndicatorMarginLeft  // Counters possible negative x-values of axes-ends.
                var marginTopToTake = generalSettings.axesIndicatorMarginTop    // Counters possible negative y-values of axes-ends.
                // Ensure that dX & dY are the endpoints of the axes on (2D) canvas:
                vectorAxisX.rotate(generalSettings.viewAngleAxisX.toInt())
                vectorAxisY.rotate(generalSettings.viewAngleAxisY.toInt())
                vectorAxisZ.rotate(generalSettings.viewAngleAxisZ.toInt())
                // Calculate the correct left/top margin on canvas - find out the very left and upper endpoint of the 3 axes:
                if (vectorAxisX.get_dX() < leftestIndicatorPoint) leftestIndicatorPoint = vectorAxisX.get_dX().toInt()
                if (vectorAxisY.get_dX() < leftestIndicatorPoint) leftestIndicatorPoint = vectorAxisY.get_dX().toInt()
                if (vectorAxisZ.get_dX() < leftestIndicatorPoint) leftestIndicatorPoint = vectorAxisZ.get_dX().toInt()
                if (vectorAxisX.get_dY() < upperIndicatorPoint) upperIndicatorPoint = vectorAxisX.get_dY().toInt()
                if (vectorAxisY.get_dY() < upperIndicatorPoint) upperIndicatorPoint = vectorAxisY.get_dY().toInt()
                if (vectorAxisZ.get_dY() < upperIndicatorPoint) upperIndicatorPoint = vectorAxisZ.get_dY().toInt()
                if (leftestIndicatorPoint < 0) marginLeftToTake -= leftestIndicatorPoint
                if (upperIndicatorPoint < 0) marginTopToTake -= upperIndicatorPoint
                // Draw x-axis on canvas:
                paintAxis.color = generalSettings.colorAxisX
                path.moveTo(marginLeftToTake.toFloat(), marginTopToTake.toFloat())  // margin... serve as "axis origin coordinates" as well.
                path.lineTo((marginLeftToTake + vectorAxisX.get_dX()).toFloat(), (marginTopToTake + vectorAxisX.get_dY()).toFloat())
                canvas.drawPath(path, paintAxis)
                path.reset()
                // Draw Y-axis on canvas:
                paintAxis.color = generalSettings.colorAxisY
                path.moveTo(marginLeftToTake.toFloat(), marginTopToTake.toFloat())  // margin... serve as "axis origin coordinates" as well.
                path.lineTo((marginLeftToTake + vectorAxisY.get_dX()).toFloat(), (marginTopToTake + vectorAxisY.get_dY()).toFloat())
                canvas.drawPath(path, paintAxis)
                path.reset()
                // Draw z-axis on canvas:
                paintAxis.color = generalSettings.colorAxisZ
                path.moveTo(marginLeftToTake.toFloat(), marginTopToTake.toFloat())  // margin... serve as "axis origin coordinates" as well.
                path.lineTo((marginLeftToTake + vectorAxisZ.get_dX()).toFloat(), (marginTopToTake + vectorAxisZ.get_dY()).toFloat())
                canvas.drawPath(path, paintAxis)
            }
        }  // inner class AxesIndicator

        /** For creating a polyhedron plus displaying axes: Adding points - combining point pairs
         *  to an edge - creating resulting faces with the option to specify colors for them.
         *  Drawing it on a given canvas is possible.
         */
        inner class Polyhedron {
            // Defined by user:
            val points = mutableListOf<Point3D>()
            val edges = Edges()  // Caution: Don't confuse with Polygon.edges!
            // Filled with auto-generated data:
            val polygons = Polygons()
            val axisInfo = AxisInfo()
            val polyhedronOnCanvas = PolyhedronOnCanvas()

            /** Reset (clear) all elements of the current polyhedron.
             */
            fun reset() {
                polyhedronOnCanvas.reset()
                polygons.clear()
                edges.clear()
                points.clear()
                axisInfo.update()
            }

            /** Adds a 3D point to the polyhedron.
             *  Giving it a [caption] is optional.
             */
            fun addPoint(x: Float, y: Float, z: Float, caption: String = "") {
                val nextIndex = points.size
                points.add(Point3D(x, y, z, caption, nextIndex))
                axisInfo.update()
                polyhedronOnCanvas.update()
            }

            /** Adds a 3D point with integer coordinates [x]/[y]/[z] to the polyhedron.
             *  Giving it a [caption] is optional.
             */
            fun addPoint(x: Int, y: Int, z: Int, caption: String= "") {
                addPoint(x.toFloat(), y.toFloat(), z.toFloat(), caption)
            }

            /** Returns the 3D point from [points] picked by its [caption].
             *  With [ignoreCase] = true the case sensitivity can be turned off.
             *  Returns a "dummy point" ( .isDummy() == true ) if not found.
             */
            fun getPointByCaption(caption: String, ignoreCase: Boolean = false): Point3D {
                var point3D = pointDummy3D
                for (point in points) {
                    if (point.caption.equals(caption, ignoreCase)) {
                        point3D = point
                        break
                    }
                }
                return point3D
            }

            /** Returns the list [points] extended (if necessary): Ensuring that the locations of
             *  the axes x/y/z are included (for the axes that are configured to be displayed).
             *  AND: That the location of at least 1 "ruler unit" on every displayed axis is included as well.
             */
            fun pointsWithAxesMinimumDimensions(): List<Point3D> {
                var x: Int;  var y: Int;  var z: Int
                val pointsWithAxesMinDimensions = mutableListOf<Point3D>()  // Actual points plus those that may be required for minimal axes dimensions.
                // Generate list of 3D points with all points "rounded up" (or negative values "rounded down"):
                for (point3D in points) {
                    x = 0;  y = 0;  z = 0
                    if (point3D.x > 0) x = ceil(point3D.x.toDouble()).toInt()
                    else if (point3D.x < 0) x = floor(point3D.x.toDouble()).toInt()
                    if (point3D.y > 0) y = ceil(point3D.y.toDouble()).toInt()
                    else if (point3D.y < 0) y = floor(point3D.y.toDouble()).toInt()
                    if (point3D.z > 0) z = ceil(point3D.z.toDouble()).toInt()
                    else if (point3D.z < 0) z = floor(point3D.z.toDouble()).toInt()
                    pointsWithAxesMinDimensions.add(Point3D(x.toFloat(), y.toFloat(), z.toFloat()))
                }
                val showAxisX = generalSettings.showAxisX; val showAxisY = generalSettings.showAxisY
                val showAxisZ = generalSettings.showAxisZ
                if (showAxisX || showAxisY || showAxisZ) {
                    // Add extra points (if necessary) to ensure that the "normed canvas" has enough room for the axes.
                    // (1) Ensure the existence of at least "one ruler unit" on every axis:
                    val pointMostPositiveCorner: Point3D = axisInfo.mostPositiveCorner(pointsWithAxesMinDimensions)
                    val pointMostNegativeCorner: Point3D = axisInfo.mostNegativeCorner(pointsWithAxesMinDimensions)
                    if (showAxisX && pointMostPositiveCorner.x == 0f && pointMostNegativeCorner.x == 0f) pointsWithAxesMinDimensions.add(Point3D(1f,0f,0f))
                    if (showAxisY && pointMostPositiveCorner.y == 0f && pointMostNegativeCorner.y == 0f) pointsWithAxesMinDimensions.add(Point3D(0f,1f,0f))
                    if (showAxisZ && pointMostPositiveCorner.z == 0f && pointMostNegativeCorner.z == 0f) pointsWithAxesMinDimensions.add(Point3D(0f,0f,1f))
                    // (2) Ensure that the location 0 exists as a point on every axis (ALWAYS including it):
                    if (!axisInfo.isPointZeroAvailableOnAxisX() || !axisInfo.isPointZeroAvailableOnAxisY() || !axisInfo.isPointZeroAvailableOnAxisZ())
                        pointsWithAxesMinDimensions.add(Point3D(0f,0f,0f))
                }
                return pointsWithAxesMinDimensions.toList()
            }

            /** Meaning: "Normed (2D!) canvas for existing polyhedron points"
             *  Returns a rectangle with "normed unit dimensions":
             *  Generating projection of all points using factorAxisX/Y/Z multiplied with the
             *  actual 3D values. So it delivers the necessary value pool for the actual drawn
             *  polyhedron. As long e.g. showAxisX == true it is considered that the 0 point of
             *  the x axis needs to fit onto the canvas as well as at least the ruler point "1":
             *  Even if the one point that got added has the x value 0. if no point exists returns:
             *  RectF(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
             */
            fun pointsNormedCanvas(): RectF {
                var x1: Float;  var y1: Float
                var x2: Float;  var y2: Float
                var pointOnCanvasF: PointOnCanvasF
                var rectangleOfCanvas = RectF(negativeInf, negativeInf, positiveInf, positiveInf)
                val pointsWithAxesMinDimensions = pointsWithAxesMinimumDimensions()  // Actual points plus those that may be required for minimal axes dimensions.
                if (pointsWithAxesMinDimensions.isNotEmpty()) {
                    pointOnCanvasF = pointsWithAxesMinDimensions[0].projectedOntoCanvasF()
                    rectangleOfCanvas = RectF(pointOnCanvasF.x, pointOnCanvasF.y, pointOnCanvasF.x, pointOnCanvasF.y)
                    for (point3D in pointsWithAxesMinDimensions) {
                        x1 = rectangleOfCanvas.left;  y1 = rectangleOfCanvas.top
                        x2 = rectangleOfCanvas.right; y2 = rectangleOfCanvas.bottom
                        pointOnCanvasF = point3D.projectedOntoCanvasF()
                        if (pointOnCanvasF.x < x1) x1 = pointOnCanvasF.x
                        if (pointOnCanvasF.y < y1) y1 = pointOnCanvasF.y
                        if (pointOnCanvasF.x > x2) x2 = pointOnCanvasF.x
                        if (pointOnCanvasF.y > y2) y2 = pointOnCanvasF.y
                        rectangleOfCanvas = RectF(x1, y1, x2, y2)
                    }
                }
                return rectangleOfCanvas
            }

            /** Returns true if e.g. point x|y|z = 4|5|6 is requested and a point
             *  4.0|5.0|6.0 actually exists.
             *  Specify [Int.MAX_VALUE] for one or more parameters to ignore that coordinate
             *  within the checks: (2, Int.MAX_VALUE, 3) returns true if point with that x and z
             *  coordinate is found.
             */
            fun doesIntegerPointExist(x: Int, y: Int, z: Int): Boolean {
                var doesExist = false
                val maxVal = Int.MAX_VALUE
                for (point in points) {
                    if ((x == maxVal || point.x == x.toFloat()) && (y == maxVal || point.y == y.toFloat()) && (z == maxVal || point.z == z.toFloat())) {
                        doesExist = true
                        break
                    }
                }
                return doesExist
            }

            /** Returns lowest "not already taken" positive (non zero!) integer x-coordinate.
             */
            fun lowestFreePositiveIntegerCoordinateX(): Int {
                var freeCoordinateX = 1
                while (true) {
                    if (!doesIntegerPointExist(freeCoordinateX, Int.MAX_VALUE, Int.MAX_VALUE)) break
                    freeCoordinateX++
                }
                return freeCoordinateX
            }
            /** Returns lowest "not already taken" positive (non zero!) integer y-coordinate.
             */
            fun lowestFreePositiveIntegerCoordinateY(): Int {
                var freeCoordinateY = 1
                while (true) {
                    if (!doesIntegerPointExist(Int.MAX_VALUE, freeCoordinateY, Int.MAX_VALUE)) break
                    freeCoordinateY++
                }
                return freeCoordinateY
            }
            /** Returns lowest "not already taken" positive (non zero!) integer z-coordinate.
             */
            fun lowestFreePositiveIntegerCoordinateZ(): Int {
                var freeCoordinateZ = 1
                while (true) {
                    if (!doesIntegerPointExist(Int.MAX_VALUE, Int.MAX_VALUE, freeCoordinateZ)) break
                    freeCoordinateZ++
                }
                return freeCoordinateZ
            }

            /** Returns the (appearance of an) intersection point of 2 edges on canvas [edge1C] & [edge2C]
             *  on current canvas ((considering viewAngleAxisX/Y/Z)).
             *  Returns PointOnCanvasF(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
             *  if the 2 edges do NOT intersect when [isPointOutsideEdgesAllowed] == false.
             *  Caution: If the given edges share a point, they supposed to return that
             *  "intersection point" as well!
             *  Returns PointOnCanvasF(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)
             *  if one or more of the edge's containing points are "dummies".
             */
            fun getIntersectionPointOnCanvas(edge1C: EdgeOnCanvas, edge2C: EdgeOnCanvas, isPointOutsideEdgesAllowed: Boolean = false): PointOnCanvasF {
                val pointIntersection: PointOnCanvasF
                if (!edge1C.isDummy() && !edge2C.isDummy()) {
                    val (p11, p12) = edge1C  // points of 1st line on canvas.
                    val (p21, p22) = edge2C  // points of 2nd line on canvas.
                    val a1 = p12.y - p11.y
                    val b1 = p11.x - p12.x
                    val c1 = a1 * p11.x + b1 * p11.y
                    val a2 = p22.y - p21.y
                    val b2 = p21.x - p22.x
                    val c2 = a2 * p21.x + b2 * p21.y
                    val delta = a1 * b2 - a2 * b1
                    val intersectionX = (b2 * c1 - b1 * c2) / delta
                    val intersectionY = (a1 * c2 - a2 * c1) / delta
                    var areIntersectingWithinBoundaries = false
                    if (!isPointOutsideEdgesAllowed) {
                        // Evaluate if that "line intersection point" is actually located
                        // on both of the edges:
                        val line1MinX = minOf(p11.x, p12.x);  val line1MaxX = maxOf(p11.x, p12.x)
                        val line1MinY = minOf(p11.y, p12.y);  val line1MaxY = maxOf(p11.y, p12.y)
                        val line2MinX = minOf(p21.x, p22.x);  val line2MaxX = maxOf(p21.x, p22.x)
                        val line2MinY = minOf(p21.y, p22.y);  val line2MaxY = maxOf(p21.y, p22.y)
                        areIntersectingWithinBoundaries =
                            intersectionX in line1MinX..line1MaxX &&
                            intersectionX in line2MinX..line2MaxX &&
                            intersectionY in line1MinY..line1MaxY &&
                            intersectionY in line2MinY..line2MaxY
                    }
                    pointIntersection = if (areIntersectingWithinBoundaries || isPointOutsideEdgesAllowed) {
                        PointOnCanvasF(intersectionX, intersectionY)
                    } else PointOnCanvasF(positiveInf, positiveInf)
                } else {
                    pointIntersection = PointOnCanvasF(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)
                }
                return pointIntersection
            }

            /** Returns the (appearance of an) intersection point of 2 edges [edge1] & [edge2]
             *  on current canvas ((considering viewAngleAxisX/Y/Z)).
             *  Returns PointOnCanvasF(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
             *  if the 2 edges do NOT intersect when [isPointOutsideEdgesAllowed] == false.
             *  Caution: If the given edges share a point, they supposed to return that
             *  "intersection point" as well!
             *  Returns PointOnCanvasF(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)
             *  if one or more of the specified 3D points don't have an index equivalent in:
             *  polyhedronOnCanvas.polyhedronOnCanvasPointsF (e.g.: Point3D.indexOfPoint == -1)
             */
            fun getIntersectionPointOnCanvas(edge1: Edge, edge2: Edge, isPointOutsideEdgesAllowed: Boolean = false): PointOnCanvasF {
                return getIntersectionPointOnCanvas(edge1.getCanvasProjection(), edge2.getCanvasProjection(), isPointOutsideEdgesAllowed)
            }

            /** Obtains information "around the 3 axes".
             */
            inner class AxisInfo {
                val dimensionalClaimArrowArea = DimensionalClaim()  // Axes' usage of 2D area. Updated with updateDimensionalClaimArrowArea()
                val fullRulerUnitsAxisX = AxisFullRulerUnits()  // Current (necessary) count of
                val fullRulerUnitsAxisY = AxisFullRulerUnits()  // axes' full ruler units.
                val fullRulerUnitsAxisZ = AxisFullRulerUnits()  // Get updated with updateAxesFullRulerUnits()

                /** Returns the corner of the axes with all 3 axes' maximum value.
                 *  (Derived from the given list of polyhedron points.)
                 *  Returns Point3D(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)
                 *  if no point exists.
                 */
                fun mostPositiveCorner(pointsToAnalyse: MutableList<Point3D> = points): Point3D {
                    var isToAlter: Boolean
                    var x: Float;  var y: Float;  var z: Float
                    var maxPoint = Point3D(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY)

                    if (pointsToAnalyse.size > 0) {
                        for (nextPoint in pointsToAnalyse) {
                            isToAlter = false
                            if (nextPoint.x > maxPoint.x) {
                                x = nextPoint.x
                                isToAlter = true
                            } else x = maxPoint.x
                            if (nextPoint.y > maxPoint.y) {
                                y = nextPoint.y
                                isToAlter = true
                            } else y = maxPoint.y
                            if (nextPoint.z > maxPoint.z) {
                                z = nextPoint.z
                                isToAlter = true
                            } else z = maxPoint.z
                            if (isToAlter) maxPoint = Point3D(x, y, z)
                        }
                    }
                    return maxPoint
                }

                /** Returns the corner of the axes with all 3 axes' minimum value.
                 *  (Derived from the given list of polyhedron points.)
                 *  Returns "Dummy Point" ( .isDummy() == true) if no point exists.
                 */
                fun mostNegativeCorner(pointsToAnalyse: MutableList<Point3D> = points): Point3D {
                    var isToAlter: Boolean
                    var x: Float;  var y: Float;  var z: Float
                    var minPoint = pointDummy3D

                    if (pointsToAnalyse.size > 0) {
                        for (nextPoint in pointsToAnalyse) {
                            isToAlter = false
                            if (nextPoint.x < minPoint.x) {
                                x = nextPoint.x
                                isToAlter = true
                            } else x = minPoint.x
                            if (nextPoint.y < minPoint.y) {
                                y = nextPoint.y
                                isToAlter = true
                            } else y = minPoint.y
                            if (nextPoint.z < minPoint.z) {
                                z = nextPoint.z
                                isToAlter = true
                            } else z = minPoint.z
                            if (isToAlter) minPoint = Point3D(x, y, z)
                        }
                    }
                    return minPoint
                }

                /** Returns true if a polyhedron point exists with x == 0.0
                 */
                fun isPointZeroAvailableOnAxisX(): Boolean {
                    var isAvailable = false
                    for (point in points) {
                        if (point.x == 0f) {
                            isAvailable = true
                            break
                        }
                    }
                    return isAvailable
                }

                /** Returns true if a polyhedron point exists with y == 0.0
                 */
                fun isPointZeroAvailableOnAxisY(): Boolean {
                    var isAvailable = false
                    for (point in points) {
                        if (point.y == 0f) {
                            isAvailable = true
                            break
                        }
                    }
                    return isAvailable
                }

                /** Returns true if a polyhedron point exists with z == 0.0
                 */
                fun isPointZeroAvailableOnAxisZ(): Boolean {
                    var isAvailable = false
                    for (point in points) {
                        if (point.z == 0f) {
                            isAvailable = true
                            break
                        }
                    }
                    return isAvailable
                }

                /** Stores an axis' minimum and maximum demanded number of ruler units
                 *  "to fit the polyhedron" and (if shown) the 3 axes' places.
                 *  If not yet set (or reset() was called):
                 *  minimum = Int.MAX_VALUE;  maximum = Int.MIN_VALUE
                 */
                inner class AxisFullRulerUnits {
                    var minimum = Int.MAX_VALUE
                    var maximum = Int.MIN_VALUE

                    fun reset() {
                        minimum = Int.MAX_VALUE;  maximum = Int.MIN_VALUE
                    }

                    /** Returns true if minimum != Int.MAX_VALUE etc.
                     */
                    fun isValid() = minimum < maximum
                }

                fun update() {
                    updateDimensionalClaimArrowArea()
                    updateAxesFullRulerUnits()
                }

                /** Updates the "2D area" the dart ends will currently claim (considering the "dart angle").
                 *  (NOT considering the negative axis directions that may exist!)  TODO implement that.
                 */
                private fun updateDimensionalClaimArrowArea() {
                    fun getLateralDartPartEndpointsAxis(viewAngleAxis: Int): Pair<PointOnCanvasF, PointOnCanvasF> {
                        val lateralLen = generalSettings.lengthOfLateralDartPart.toDouble()
                        val pointEndPointLeft: PointOnCanvasF;  val pointEndPointRight: PointOnCanvasF
                        val vectorDartPartLeft = MyMath.Vector(generalSettings.lengthOfAxisArrowArea.toDouble(), 0.0)
                        vectorDartPartLeft.rotate(viewAngleAxis)
                        val vectorDartPartRight = vectorDartPartLeft.copy()
                        vectorDartPartLeft.add(lateralLen,viewAngleAxis - 180 + generalSettings.angleDartPart / 2)
                        vectorDartPartRight.add(lateralLen,viewAngleAxis - 180 - generalSettings.angleDartPart / 2)
                        pointEndPointLeft = PointOnCanvasF(vectorDartPartLeft.get_dX().toFloat(), vectorDartPartLeft.get_dY().toFloat())
                        pointEndPointRight = PointOnCanvasF(vectorDartPartRight.get_dX().toFloat(), vectorDartPartRight.get_dY().toFloat())
                        return pointEndPointLeft to pointEndPointRight
                    }
                    dimensionalClaimArrowArea.reset()
                    if (generalSettings.showAxisX) {
                        dimensionalClaimArrowArea.addVectorEndPoint3D(Point3D(generalSettings.lengthOfAxisArrowArea.toFloat(),0f,0f))
                        val (pointEndPointLeftY, pointEndPointRightY) = getLateralDartPartEndpointsAxis(generalSettings.viewAngleAxisX.toInt())
                        dimensionalClaimArrowArea.addPointOnCanvas(pointEndPointLeftY)
                        dimensionalClaimArrowArea.addPointOnCanvas(pointEndPointRightY)
                    }
                    if (generalSettings.showAxisY) {
                        dimensionalClaimArrowArea.addVectorEndPoint3D(Point3D(0f, generalSettings.lengthOfAxisArrowArea.toFloat(),0f))
                        val (pointEndPointLeftY, pointEndPointRightY) = getLateralDartPartEndpointsAxis(generalSettings.viewAngleAxisY.toInt())
                        dimensionalClaimArrowArea.addPointOnCanvas(pointEndPointLeftY)
                        dimensionalClaimArrowArea.addPointOnCanvas(pointEndPointRightY)
                    }
                    if (generalSettings.showAxisZ) {
                        dimensionalClaimArrowArea.addVectorEndPoint3D(Point3D(0f,0f, generalSettings.lengthOfAxisArrowArea.toFloat()))
                        val (pointEndPointLeftZ, pointEndPointRightZ) = getLateralDartPartEndpointsAxis(generalSettings.viewAngleAxisZ.toInt())
                        dimensionalClaimArrowArea.addPointOnCanvas(pointEndPointLeftZ)
                        dimensionalClaimArrowArea.addPointOnCanvas(pointEndPointRightZ)
                    }
                }

                // Updates the instances that define the axes' currently necessary lengths (in "full ruler units").
                private fun updateAxesFullRulerUnits() {
                    val showAxisX = generalSettings.showAxisX
                    val showAxisY = generalSettings.showAxisY
                    val showAxisZ = generalSettings.showAxisZ
                    fullRulerUnitsAxisX.reset(); fullRulerUnitsAxisY.reset(); fullRulerUnitsAxisZ.reset()
                    for (point in points) {
                        if (point.x < fullRulerUnitsAxisX.minimum) fullRulerUnitsAxisX.minimum = floor(point.x).toInt()
                        if (point.x > fullRulerUnitsAxisX.maximum) fullRulerUnitsAxisX.maximum = ceil(point.x).toInt()
                        if (point.y < fullRulerUnitsAxisY.minimum) fullRulerUnitsAxisY.minimum = floor(point.y).toInt()
                        if (point.y > fullRulerUnitsAxisY.maximum) fullRulerUnitsAxisY.maximum = ceil(point.y).toInt()
                        if (point.z < fullRulerUnitsAxisZ.minimum) fullRulerUnitsAxisZ.minimum = floor(point.z).toInt()
                        if (point.z > fullRulerUnitsAxisZ.maximum) fullRulerUnitsAxisZ.maximum = ceil(point.z).toInt()
                    }

                    if (showAxisX) {  // On every shown axis ensure...
                        // ... 1.) that at least "one ruler unit" is demanded:
                        if (fullRulerUnitsAxisX.minimum == 0 && fullRulerUnitsAxisX.maximum == 0)
                            fullRulerUnitsAxisX.maximum = 1
                        // ... 2.) that on every shown axis either .minimum <= 0 or .maximum >= 0
                        //         (so that this axis itself has room on the canvas):
                        if (fullRulerUnitsAxisX.minimum > 0) fullRulerUnitsAxisX.minimum = 0
                        else if (fullRulerUnitsAxisX.maximum < 0) fullRulerUnitsAxisX.maximum = 0
                    }
                    if (showAxisY) {
                        if (fullRulerUnitsAxisY.minimum == 0 && fullRulerUnitsAxisY.maximum == 0)
                            fullRulerUnitsAxisY.maximum = 1
                        if (fullRulerUnitsAxisY.minimum > 0) fullRulerUnitsAxisY.minimum = 0
                        else if (fullRulerUnitsAxisY.maximum < 0) fullRulerUnitsAxisY.maximum = 0
                    }
                    if (showAxisZ) {
                        if (fullRulerUnitsAxisZ.minimum == 0 && fullRulerUnitsAxisZ.maximum == 0)
                            fullRulerUnitsAxisZ.maximum = 1
                        if (fullRulerUnitsAxisZ.minimum > 0) fullRulerUnitsAxisZ.minimum = 0
                        else if (fullRulerUnitsAxisZ.maximum < 0) fullRulerUnitsAxisZ.maximum = 0
                    }
                }
            }  // inner class AxisInfo

            /** Contains data for a (3D) edge [point1]/[point2] - including:
             *  [countPolygonsItIsPartOf]:  Number of polygons the edge is part of.
             *  [positionsOfEdgeCrossings]: Positions of (frontal!) crossing edges relative to [point1].
             *  [edgeFragments]: Parts of the edge with specified visibility:  .isVisible
             */
            inner class Edge(val point1: Point3D, val point2: Point3D) {  // inner class Edge
                var countPolygonsItIsPartOf = 0
                var isInvisible = false  // true => completely invisible edge. Overruling edgeFragments[].isVisible
                val positionsOfEdgeCrossings = mutableListOf<Float>()  // (At those positions the edge is certainly invisible.)
                val edgeFragments = mutableListOf<EdgeFragment>()  // Stores visibility data for parts of the edge. Supposed to build a continuous line.
                operator fun component1() = point1
                operator fun component2() = point2

                fun getPoint1OnCanvas(): PointOnCanvasF {
                    var pointOnCanvasF = PointOnCanvasF(positiveInf, positiveInf)
                    if (hasCorrespondingCanvasPoint1Data())
                        pointOnCanvasF = polyhedronOnCanvas.polyhedronOnCanvasPointsF[point1.indexOfPoint]
                    return pointOnCanvasF
                }
                fun getPoint2OnCanvas(): PointOnCanvasF {
                    var pointOnCanvasF = PointOnCanvasF(positiveInf, positiveInf)
                    if (hasCorrespondingCanvasPoint2Data())
                        pointOnCanvasF = polyhedronOnCanvas.polyhedronOnCanvasPointsF[point2.indexOfPoint]
                    return pointOnCanvasF
                }

                /** Updates fragments' visibility of the edge stored within [edgeFragments]
                 *  (plus [positionsOfEdgeCrossings]).
                 */
                fun updateVisibility() {
                    updateEdgeCrossings()
                    updateEdgeFragments()
                }

                /** Updates frontal crossings of other edges with this edge -
                 *  renewing this positions within  [positionsOfEdgeCrossings]
                 *  (leaving this array empty if no crossings at all were found).
                 *  TODO evaluate crossing counts of edges with wrong visibility - e.g.:
                 *       "G17 L-M" supposed to have ZERO frontal crossings.
                 *       "G17 E-F"                  ONE
                 *       "G17 N-O"                  ONE
                 */
                private fun updateEdgeCrossings() {
                    val point1C = getPoint1OnCanvas()
                    val positionsOfEdgeCrossingsTemp = mutableListOf<Float>()
                    for (e2 in edges.singleEdges) {  // e2 = "Edge #2"
                        if (e2.contains(point1) || e2.contains(point2)) continue  // Skip edges that cannot actually cross this one.
                        val pointIntersection = getIntersectionPointOnCanvas(this, e2)
                        if (!pointIntersection.isDummy()) {  // true => intersection found.
                            val point1CE2 = e2.getPoint1OnCanvas()
                            // Calculate the depth of both edges at intersection:
                            val xInter = pointIntersection.x;   val yInter = pointIntersection.y
                            val diffX1 = xInter - point1C.x;    val diffY1 = yInter - point1C.y
                            val diffX2 = xInter - point1CE2.x;  val diffY2 = yInter - point1CE2.y
                            val distanceIntersectionE1 = sqrt(diffX1 * diffX1 + diffY1 * diffY1)
                            val distanceIntersectionE2 = sqrt(diffX2 * diffX2 + diffY2 * diffY2)
                            val depthAtInterSectionE1 = getDepthAtDistanceFromPoint1(distanceIntersectionE1)
                            val depthAtInterSectionE2 = e2.getDepthAtDistanceFromPoint1(distanceIntersectionE2)
                            // Compare the depths to add the intersection distance if the other edge lies in front of this edge:
                            if (depthAtInterSectionE2 < depthAtInterSectionE1) positionsOfEdgeCrossingsTemp.add(distanceIntersectionE1)
                        }
                    }
                    positionsOfEdgeCrossings.clear()
                    if (positionsOfEdgeCrossingsTemp.size > 1) {
                        positionsOfEdgeCrossingsTemp.sort()  // Sort Float values ascending.
                        positionsOfEdgeCrossings.addAll(positionsOfEdgeCrossingsTemp)
                    }
                }

                /** Updates the array [edgeFragments]: Delivering a potentially changing visibility
                 *  info along the edge.
                 */
                private fun updateEdgeFragments() {
                    val edgeFragmentsTemp = mutableListOf<EdgeFragment>()
                    val lengthEdge = getProjectedOriginalLength()
                    val point1C = getPoint1OnCanvas();  val point2C = getPoint2OnCanvas()
                    val posOfPoints = positionsOfEdgeCrossings.toMutableList()
                    posOfPoints.add(lengthEdge)  // Add endpoint position - no matter if visible.
                    /* TODO delete when considered obsolete:
                    // Get edge's endpoints visibility:
                    var isPoint1Visible = true;   var isPoint2Visible = true
                    val depthPoint1 = getDepthOfPoint1();  val depthPoint2 = getDepthOfPoint2()
                    for (polygon in polygons.collection) {
                        if (isPoint1Visible) isPoint1Visible = polygon.isInFrontOfPoint(point1C, depthPoint1)
                        if (isPoint2Visible) isPoint2Visible = polygon.isInFrontOfPoint(point2C, depthPoint2)
                        if (!isPoint1Visible && !isPoint2Visible) break
                    }
                    */
                    // Step from point to point on edge - finding out visibility of the fragments between:
                    var posLastPoint = 0f  // = position of point1C.
                    var pointLast = point1C
                    var wasLastFragmentVisible = true
                    var countCenterPointOfEdgeEnclosedByPolygon = 0  // TODO delete when debugging done.
                    val hasOnlyOneFragment = posOfPoints.size == 1   //      -"-
                    for (posPoint in posOfPoints) {
                        val posMiddlePoint = posLastPoint + (posPoint - posLastPoint) / 2
                        val depthOfPoint = getDepthAtDistanceFromPoint1(posMiddlePoint)
                        val pointInMiddle = getPointAtDistanceFromPoint1(posMiddlePoint)
                        var isFragmentVisible = true
                        for (polygon in polygons.collection) {
                            val isEncasedByPolygon = polygon.isEncasingPoint(pointInMiddle, this)
                            if (isEncasedByPolygon && hasOnlyOneFragment) countCenterPointOfEdgeEnclosedByPolygon++
                            if (isEncasedByPolygon && polygon.isInFrontOfPoint(pointInMiddle, depthOfPoint)) {
                                isFragmentVisible = false
                                break
                            }
                        }
                        val point = if (posPoint == lengthEdge) point2C else getPointAtDistanceFromPoint1(posPoint)
                        if (edgeFragmentsTemp.isNotEmpty() && isFragmentVisible == wasLastFragmentVisible) {  // Prepare fragment-merger if demanded.
                            pointLast = edgeFragmentsTemp[edgeFragmentsTemp.lastIndex].point1
                            edgeFragmentsTemp.removeLast()
                        }
                        edgeFragmentsTemp.add(EdgeFragment(pointLast, point, isFragmentVisible))
                        posLastPoint = posPoint
                        pointLast = point
                        wasLastFragmentVisible = isFragmentVisible
                    }
                    /* TODO delete when debugging done:
                    if (countCenterPointOfEdgeEnclosedByPolygon > 0)
                        Log.d("updateEdgeFragments()", "Points along edge ${getCaption()} are enclosed by polygons $countCenterPointOfEdgeEnclosedByPolygon times in total.")
                    */
                    edgeFragments.clear()
                    edgeFragments.addAll(edgeFragmentsTemp)
                }

                private fun hasCorrespondingCanvasPoint1Data() = point1.indexOfPoint in polyhedronOnCanvas.polyhedronOnCanvasPointsF.indices
                private fun hasCorrespondingCanvasPoint2Data() = point2.indexOfPoint in polyhedronOnCanvas.polyhedronOnCanvasPointsF.indices

                /** Returns true if this edge's [point1] & [point2] both have corresponding
                 *  "canvas data" according to their properties .indexOfPoint
                 *  Returns always false for a "Dummy Edge".
                 */
                fun hasCorrespondingCanvasPointsData() = hasCorrespondingCanvasPoint1Data() && hasCorrespondingCanvasPoint2Data()

                /** Returns true if one or both of its points are "Dummy Points".
                 */
                fun isDummy() = point1.isDummy() || point2.isDummy()

                /** Returns the length of the edge for a 2D projection based on 3D original dimensions.
                 */
                fun getProjectedOriginalLength(): Float {
                    var length = 0f
                    if (hasCorrespondingCanvasPointsData()) {
                        val pointsOnCanvas = polyhedronOnCanvas.polyhedronOnCanvasPointsF
                        val p1OnCanvas = pointsOnCanvas[point1.indexOfPoint]
                        val p2OnCanvas = pointsOnCanvas[point2.indexOfPoint]
                        val diffX = p2OnCanvas.x - p1OnCanvas.x
                        val diffY = p2OnCanvas.y - p1OnCanvas.y
                        length = sqrt(diffX * diffX + diffY * diffY)
                    }
                    return length
                }

                fun getCanvasProjection(): EdgeOnCanvas {
                    val pointDummy = PointOnCanvasF(positiveInf, positiveInf)
                    var edgeOnCanvas = EdgeOnCanvas(pointDummy, pointDummy)
                    if (hasCorrespondingCanvasPointsData()) {
                        val pointsOnCanvas = polyhedronOnCanvas.polyhedronOnCanvasPointsF
                        val point1C = pointsOnCanvas[point1.indexOfPoint]
                        val point2C = pointsOnCanvas[point2.indexOfPoint]
                        edgeOnCanvas = EdgeOnCanvas(point1C, point2C)
                    }
                    return edgeOnCanvas
                }

                /** Returns the assigned depth to the 2D projection of point1 (that COULD be 0.0 as well).
                 *  Returns 0.0 (too) if the depth data is not available.
                 */
                fun getDepthOfPoint1() = if(hasCorrespondingCanvasPoint1Data())
                    polyhedronOnCanvas.polyhedronOnCanvasPointsDepth[point1.indexOfPoint] else 0.0

                /** Returns the assigned depth to the 2D projection of point2 (that COULD be 0.0 as well).
                 *  Returns 0.0 (too) if the depth data is not available.
                 */
                fun getDepthOfPoint2() = if(hasCorrespondingCanvasPoint2Data())
                    polyhedronOnCanvas.polyhedronOnCanvasPointsDepth[point2.indexOfPoint] else 0.0

                /** Returns the assigned depth to the 2D projected points (based on 3D original dimension)
                 *  in the [distance] from point1.
                 *  Returns 0.0 if the depth is zero at [distance] OR the calculation failed:
                 *  For example [distance] lies outside the range 0 to [getProjectedOriginalLength]
                 *  when [isExtrapolating] == false. Returns ALWAYS 0.0 if [distance] is infinite.
                 */
                fun getDepthAtDistanceFromPoint1(distance: Float, isExtrapolating: Boolean = false): Double {
                    var desiredDepth = 0.0
                    val lengthEdge = getProjectedOriginalLength()
                    if (distance in 0f..lengthEdge || distance != positiveInf && distance != negativeInf && isExtrapolating) {
                        val depthP1 = getDepthOfPoint1();  val depthP2 = getDepthOfPoint2()
                        desiredDepth = depthP1 + (depthP2 - depthP1) * distance / lengthEdge
                    }
                    return desiredDepth
                }

                /** Returns the interpolated depth of the specified 2D point [point2dOnEdge].
                 *  Returns 0.0 if the depth is zero at [point2dOnEdge] OR the calculation failed:
                 *  For example [point2dOnEdge] lies further away from point1 than point2 when
                 *  [isExtrapolating] == false.
                 *  Caution: It is NOT evaluated if [point2dOnEdge] is located along the edge at all
                 *  => in that case this method returns an invalid depth value!
                 */
                fun getDepthAtPoint(point2dOnEdge: PointOnCanvasF, isExtrapolating: Boolean = false): Double {
                    var desiredDepth = 0.0
                    if (!point2dOnEdge.isDummy() && hasCorrespondingCanvasPointsData()) {
                        val pointsOnCanvas = polyhedronOnCanvas.polyhedronOnCanvasPointsF
                        val point1C = pointsOnCanvas[point1.indexOfPoint]
                        val distance = point2dOnEdge.getDistance(point1C)
                        desiredDepth = getDepthAtDistanceFromPoint1(distance, isExtrapolating)
                    }
                    return desiredDepth
                }

                /** Returns a 2D projected point along the edge in distance [distance] from point1.
                 *  Returns PointOnCanvasF(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                 *  if specified [distance] is not located on the edge or no canvas points data
                 *  is available for the edge's endpoints.
                 */
                fun getPointAtDistanceFromPoint1(distance: Float): PointOnCanvasF {
                    var pointOnEdge = PointOnCanvasF(positiveInf, positiveInf)
                    val point1C = getPoint1OnCanvas();  val point2C = getPoint2OnCanvas()
                    val lengthEdge = getProjectedOriginalLength()
                    if (hasCorrespondingCanvasPointsData() && distance in 0f..lengthEdge) {
                        val fractionOfLen = distance / lengthEdge
                        val xNew = point1C.x + (point2C.x - point1C.x) * fractionOfLen
                        val yNew = point1C.y + (point2C.y - point1C.y) * fractionOfLen
                        pointOnEdge = PointOnCanvasF(xNew, yNew)
                    }
                    return pointOnEdge
                }

                /** Returns "first to find" [Point3D] of this [Edge] instance that is not part of
                 *  [listPoints3D]. Returns "Dummy" ( .isDummy() == true )
                 *  if [point1] as well as [point2] are part of [listPoints3D].
                 */
                fun getFirstPointNotInList(listPoints3D: List<Point3D>): Point3D {
                    var point3D = Point3D(positiveInf, positiveInf, positiveInf)
                    if (!listPoints3D.contains(point1)) point3D = point1
                    if (point3D.isDummy() && !listPoints3D.contains(point2)) point3D = point2
                    return point3D
                }

                /** Returns a copy of this [Edge] instance. The original points as well as its
                 *  (possibly existing [edgeFragments]) are copied by reference.
                 */
                fun getCopy(): Edge {
                    val edgeCopy = Edge(point1, point2)
                    edgeCopy.countPolygonsItIsPartOf = countPolygonsItIsPartOf
                    for (position in positionsOfEdgeCrossings) edgeCopy.positionsOfEdgeCrossings.add(position)
                    for (edgeFragment in edgeFragments) edgeCopy.edgeFragments.add(edgeFragment)
                    return edgeCopy
                }

                /** Returns true if the given point [point3D] is part of this edge (compared by reference!).
                 */
                fun contains(point3D: Point3D) = point3D == point1 || point3D == point2

                /** Returns true if another edge [other] contains the same 2 points (compared by
                 *  reference!) - their order being irrelevant.
                 *  Returns false if [other] is an invalid (and therefore not an equal)
                 *  object (not an instance of Edge) as well.
                 */
                override fun equals(other: Any?): Boolean {
                    return try {
                        val e2 = other as Edge
                        e2.point1 == point1 && e2.point2 == point2 || e2.point1 == point2 && e2.point2 == point1  //super.equals(other)
                    } catch (_: Exception) {
                        false
                    }
                }

                /** Returns true if another edge [edgeOther] consists of the same 2 points and
                 *  has exactly the same current sequence of visible/invisible fragments
                 *  according to [edgeFragments] vs. edgeOther.edgeFragments.
                 */
                fun hasSameFragmentVisibilitySequence(edgeOther: Edge): Boolean {
                    var isIdentical = false  // "identical by visibility!"
                    if (equals(edgeOther) && edgeFragments.size == edgeOther.edgeFragments.size) {
                        isIdentical = getFragmentVisibilityPattern() == edgeOther.getFragmentVisibilityPattern()
                    }
                    return isIdentical
                }

                override fun hashCode(): Int {
                    var result = point1.hashCode()
                    result = 31 * result + point2.hashCode()
                    return result
                }

                override fun toString() = getCaption()

                /** Returns "assembled caption" for the edge.
                 *  e.G.: "A-B" when [point1] Point3D's caption = "A" and the one of [point2] is "B".
                 */
                fun getCaption() = "${point1.caption}-${point2.caption}"

                /** Returns true if this edge's caption is specified by the given String [caption]:
                 *  Containing only the edge's points captions and separating them with "-".
                 *  The single captions' order within [caption] is ignored. Example: "A-B"
                 *  Returns always false as long as a single point of this edge has no caption
                 *  (empty String) - or if this edge is a dummy.
                 *  [ignoreCase] specifies if the comparison should be case-sensitive.
                 *  Spaces in [caption] are completely ignored.
                 *  Caution: Included point captions that are part of other (longer) point captions
                 *  may result in a wrong return of false!
                 */
                fun hasSpecifiedCaption(caption: String, ignoreCase: Boolean = false): Boolean {
                    var hasSpecifiedOne = false
                    var captionToWorkWith = caption
                    if (!isDummy() && point1.caption.isNotEmpty() && point2.caption.isNotEmpty()) {
                        val separator = "-"
                        captionToWorkWith = captionToWorkWith.replace(" ","")
                        if (captionToWorkWith.contains(point1.caption, ignoreCase)) {
                            captionToWorkWith = captionToWorkWith.replace(point1.caption,"", ignoreCase)
                            if (captionToWorkWith.contains(point2.caption, ignoreCase)) {
                                captionToWorkWith = captionToWorkWith.replace(point2.caption,"", ignoreCase)
                                if (captionToWorkWith == separator) hasSpecifiedOne = true
                            }
                        }
                    }
                    return hasSpecifiedOne
                }

                /** Returns the sequence of this edge's "fragments visibility" as a String pattern
                 *  according to [edgeFragments].
                 *  For example "-·-" for an edge with 3 fragments - the outer two currently visible
                 *  and the inner one not (the latter represented as a vertically centered dot).
                 *  Returns an empty String if no fragment data is available what supposed to be
                 *  always the case if this edge instance is a dummy.
                 */
                fun getFragmentVisibilityPattern(): String {
                    var patternVisibility = ""
                    if (!isDummy()) {
                        for (edgeFragment in edgeFragments) {
                            if (isInvisible) patternVisibility += "·"  // chg jg 2026-10-31
                            else if (edgeFragment.isVisible) patternVisibility += "-"
                            else patternVisibility += "·"
                        }
                    }
                    return patternVisibility
                }

                /** Contains the data for a "2D projected fragment" of the edge - its visibility included.
                 */
                inner class EdgeFragment(val point1: PointOnCanvasF, val point2: PointOnCanvasF, val isVisible: Boolean) {
                    operator fun component1() = point1
                    operator fun component2() = point2
                    operator fun component3() = isVisible
                }
            }  // inner class Edge(val point1: Point3D, val point2: Point3D)

            /** Contains data for an edge on canvas [point1]/[point2].
             */
            inner class EdgeOnCanvas(val point1: PointOnCanvasF, val point2: PointOnCanvasF) {
                operator fun component1() = point1
                operator fun component2() = point2
                fun getLength(): Float {
                    var length = 0f
                    if (!point1.isDummy() && !point2.isDummy()) length = point1.getDistance(point2)
                    return length
                }
                fun isDummy() = point1.isDummy() || point2.isDummy()
            }  // EdgeOnCanvas(val point1: PointOnCanvasF, val point2: PointOnCanvasF)

            /** Contains & manages polyhedron's edges: pairs of 2 points.
             */
            inner class Edges {
                val singleEdges = mutableListOf<Edge>()

                /** Clears all edges.
                 */
                fun clear() {
                    singleEdges.clear()
                }

                /** Updates the visibility data (edge.edgeFragment) of all current edges.
                 */
                fun updateVisibility() {
                    for (edge in singleEdges) edge.updateVisibility()
                }

                /** Returns true if the specified points [point1] and [point2] are already
                 *  existing within [singleEdges].
                 */
                fun isAlreadyExisting(point1: Point3D, point2: Point3D) = isAlreadyExisting(Edge(point1, point2))

                /** Returns true if the specified [edge] is already existing
                 *  (point-order independently) within [singleEdges].
                 */
                fun isAlreadyExisting(edge: Edge) = getIndexOfEdge(edge) >= 0

                /** Returns index of [edge] within [singleEdges] - comparing containing point3Ds
                 *  (order-independently). Returns -1 if not found.
                 */
                fun getIndexOfEdge(edge: Edge): Int {
                    val (point1, point2) = edge
                    var indexToReturn = -1
                    for ((index, singleEdge) in singleEdges.withIndex()) {
                        if (singleEdge.point1 == point1 && singleEdge.point2 == point2 || singleEdge.point1 == point2 && singleEdge.point2 == point1) {
                            indexToReturn = index
                            break
                        }
                    }
                    return indexToReturn
                }

                /** Returns the first found edge within [singleEdges] that consists of
                 *  [point1] & [point2] (their order being irrelevant).
                 *  Returns a dummy edge if no edge is matching. Always returns a dummy if
                 *  one or both of [point1] & [point2] are dummy points.
                 */
                fun getOneByItsPoints(point1: Point3D, point2: Point3D): Edge {
                    var edgeDesired = edgeDummy
                    if (!point1.isDummy() && !point2.isDummy()) {
                        val edgeSpecified = Edge(point1, point2)
                        for (edge in singleEdges) {
                            if (edge == edgeSpecified) {
                                edgeDesired = edge
                                break
                            }
                        }
                    }
                    return edgeDesired
                }

                /** Returns "assembled caption" for the given edge [edge] -
                 *  e.G.: "A-B" when first Point3D's caption = "A" and the second one is "B".
                 */
                fun getCaption(edge: Edge) = "${edge.point1.caption}-${edge.point2.caption}"

                /** Adds a new "pair of 3D points" if an edge with the specified points is not
                 *  already present (order independently!).
                 */
                fun add(point1: Point3D, point2: Point3D, isInvisible: Boolean = false) {
                    if (!isAlreadyExisting(point1, point2)) {
                        singleEdges.add(Edge(point1, point2))
                        polygons.collect()
                    }
                }

                /** Returns the number of edges the specified Point [point3D] is part of.
                 */
                fun getCountOfPointIsPartOf(point3D: Point3D): Int {
                    var count = 0
                    for (edge in singleEdges) if (edge.point1 == point3D || edge.point2 == point3D) count++
                    return count
                }

                /** Returns a copy of this [Edges] instance. Its contained (possibly existing)
                 *  [singleEdges] are copied "in the process" as well.
                 */
                fun getCopy(): Edges {
                    val edgesCopy = Edges()
                    for (singleEdge in singleEdges) edgesCopy.singleEdges.add(singleEdge.getCopy())
                    return edgesCopy
                }

                /** Returns true if the specified 3D point [point3D] isn't already connected to
                 *  every other point within [points] (by an edge).
                 *  Returns false: Otherwise or if the specified point isn't even part of [points].
                 */
                fun isAdditionalEdgeAvailableForPoint(point3D: Point3D) = points.contains(point3D) && getCountOfPointIsPartOf(point3D) < points.size - 1

                /** Returns true if [edge] exists in [singleEdges] and is currently
                 *  part of less than 2 polygons.
                 */
                fun isAdditionalPolygonMembershipPossible(edge: Edge) =
                    edge in singleEdges && edge.countPolygonsItIsPartOf < 2

                /** Sets all entries in (list of!) edge.countPolygonsItIsPartOf to 0.
                 */
                fun resetPolygonMemberships() {
                    for (edge in singleEdges) edge.countPolygonsItIsPartOf = 0
                }

                /** Updates (list of!) edge.countPolygonsItIsPartOf: incrementing this corresponding
                 *  numbers for all [singleEdges] that are members of [polygon].
                 */
                fun incrementPolygonMemberships(polygon: Polygon) {
                    for (edge in polygon.edges) edge.countPolygonsItIsPartOf++
                }

                /** Returns list of unique instances of Point3D (compared by reference - NOT content!)
                 *  from the given edges [containingEdges].
                 *  TODO delete in obsolete.
                 */
                fun getUniquePoints(containingEdges: List<Edge>): List<Point3D> {
                    val pointsSource = mutableListOf<Point3D>()
                    val pointsUnique = mutableListOf<Point3D>()
                    /** Returns true if [point3D] is not yet present within pointsUnique.
                     */
                    fun isPointMissing(point3D: Point3D): Boolean {
                        var isInList = false
                        for (point in pointsUnique)
                            if (point == point3D) {
                                isInList = true
                                break
                            }
                        return !isInList
                    }
                    for (edge in containingEdges) {
                        pointsSource.add(edge.point1);  pointsSource.add(edge.point2)
                    }
                    for (point in pointsSource) if (isPointMissing(point)) pointsUnique.add(point)
                    return pointsUnique.toList()
                }

                /** Returns true if (all of) the given points [points3D] are located on the same plane.
                 *  (Who do NOT need to be part of [singleEdges].)
                 *  Caution: Returns true as well if less than 4 distinct points are delivered
                 *  over those edges!
                 *  Nevertheless: Returns false when [points3D] is empty.
                 *  TODO test with Casper exercises.
                 */
                fun areOnSamePlane(points3D: List<Point3D>): Boolean {
                    var arePointsOnSamePlane = true
                    var isNonCoplanarPointFound = false
                    // Start to geometrically calculate if there are 4 or more distinct points:
                    // 1.) Use the first 3 points of points3D for creating the "plane equation":
                    when (val countPoints = points3D.size) {
                        0 -> arePointsOnSamePlane = false  // Zero given points return false!
                        in 1..3 -> arePointsOnSamePlane = true  // Less than 4 distinct points are ALWAYS on one plane.
                        else -> {
                            val p1 = points3D[0];  val p2 = points3D[1];  val p3 = points3D[2]
                            val a1 = p2.x - p1.x;  val b1 = p2.y - p1.y;  val c1 = p2.z - p1.z
                            val a2 = p3.x - p1.x;  val b2 = p3.y - p1.y;  val c2 = p3.z - p1.z
                            val a = b1 * c2 - b2 * c1;  val b = a2 * c1 - a1 * c2
                            val c = a1 * b2 - b1 * a2;  val d = - a * p1.x - b * p1.y - c * p1.z
                            fun isPointOnSamePlane(p: Point3D) = a * p.x + b * p.y + c * p.z + d == 0f
                            for (index in 3 until countPoints) {
                                val point = points3D[index]
                                if (!isPointOnSamePlane(point)) {
                                    isNonCoplanarPointFound = true
                                    break
                                }
                            }
                            if (isNonCoplanarPointFound) arePointsOnSamePlane = false
                        }
                    }
                    return arePointsOnSamePlane
                }

                /** Returns list of edges the given point [point3D] is part of.
                 *  Empty list if none.
                 */
                fun getAllPointIsPartOf(point3D: Point3D): List<Edge> {
                    val edges = mutableListOf<Edge>()
                    for (edge in singleEdges) {
                        val (point1, point2) = edge
                        if (point1 == point3D || point2 == point3D) edges.add(edge)
                    }
                    return edges.toList()
                }

                /** Returns an edge specified by its [caption] - returning empty one if not found.
                 *  For example: [caption] = "A-B" when "A" and "B" are the captions of the
                 *  its 2 single points. The "-" separators must be included. Spaces within
                 *  [caption] get completely ignored. Returns dummy edge as well if extra
                 *  characters are included.
                 *  With [ignoreCase] = true, the case sensitivity can be turned off - getting
                 *  the same result for the example above with [caption] = "a-B" and so on.
                 *  Caution: If there are point captions included for the wanted edge that are
                 *  part of other (longer) point captions, a dummy edge may get returned (falsely)!
                 */
                fun geEdgeByCaption(caption: String, ignoreCase: Boolean = false): Edge {
                    var edgeDesired = edgeDummy
                    for (edge in singleEdges) {
                        val isFound = edge.hasSpecifiedCaption(caption, ignoreCase)
                        if (isFound) {
                            edgeDesired = edge
                            break
                        }
                    }
                    return edgeDesired
                }
            }  // inner class Edges

            /** Delivers an instance that defines a polygon - all points supposed to be located
             *  on the same plane - forming a closed path:
             *  Three 3D points - e.g.: [pointsDefiningPlane] - defining the plane.
             *  Polygon's [edges] (containing the 3 points as well).
             *  Caution: A polyhedron could very well consist of more than 1 polygons on a certain plane.
             */
            inner class Polygon {
                val pointsDefiningPlane = mutableListOf<Point3D>()
                val edges = mutableListOf<Edge>()
                /** Polygon's current number of points / edges.
                 */
                var size: Int = 0
                    get() {
                        return edges.size
                    }
                    private set
                fun clear() {
                    pointsDefiningPlane.clear()
                    edges.clear()
                }
                fun isEmpty() = edges.isEmpty()

                /** Returns polygon's current path as a descriptive string.
                 *  Examples:  "empty!" for an empty path / "A-B; B-C; C-D; D-A" for a tetragon.
                 */
                fun getPointsPathDescription(): String {
                    var description = "empty!"
                    if (edges.size > 0) {
                        description = ""
                        for (edge in edges) {
                            val (point1, point2) = edge
                            if (description.isNotEmpty()) description += "; "
                            description += "${point1.caption}-${point2.caption}"
                        }
                    }
                    return description
                }

                /** Returns polygon's list of 3D corner points as a continuous chain.
                 * Returns empty list if this polygon is empty.
                 */
                fun getPointChain(): List<Point3D> {
                    val pointChain = mutableListOf<Point3D>()
                    for (edge in edges) {
                        if (pointChain.isEmpty()) {
                            pointChain.add(edge.point1)
                            pointChain.add(edge.point2)
                        } else if (pointChain.contains(edge.point1)) pointChain.add(edge.point2) else pointChain.add(edge.point1)
                    }
                    return pointChain
                }

                /** Returns corner point that follows [pointCurrent] - stepping through [edges] -
                 *  considering [doWrapAround]: Wrapping around or returning a Dummy Point if
                 *  after pointCurrent the end of [edges] is reached.
                 *  Returns a Dummy Point as well if this polygon is empty ( .isEmpty() == true ).
                 */
                fun getNextCornerPoint(pointCurrent: Point3D, doWrapAround: Boolean = false): Point3D {
                    var pointNext = pointDummy3D
                    if (edges.isNotEmpty()) {
                        val pointChain = getPointChain()
                        for ((index, point) in pointChain.withIndex()) {
                            if (pointCurrent == point) {
                                if (index < pointChain.size - 1) {
                                    pointNext = pointChain[index + 1]
                                } else if (index == pointChain.lastIndex && doWrapAround) {
                                    pointNext = pointChain.first()
                                } else pointNext = pointDummy3D
                            }
                        }
                    }
                    return pointNext
                }

                /** Returns polygon's current path as a caption.
                 *  Examples:  "" for an empty polygon / "A-B-C-D" for a tetragon.
                 */
                fun getCaption(): String {
                    var caption = ""
                    var point = if (edges.isEmpty()) pointDummy3D else edges.first().point1
                    while (!point.isDummy()) {
                        if (caption.isNotEmpty()) caption += "-"
                        caption += point.caption
                        point = getNextCornerPoint(point)
                    }
                    return caption
                }

                /** Returns coordinates of polygon's current points as a formatted String.
                 *  The points' order is like the one delivered by [getCaption()].
                 *  [digits] specifies the number of decimals for every coordinate.
                 *  Examples: (1)  "" for an empty polygon.
                 *  (2)  "[0.04]/[-1.10]-[-2.08]/[0.00]-[0.30]/[-3.99]-[0.44]/[-0.10]" for a tetragon
                 *  (with [digits] = 2).
                 */
                fun getFormattedCoordinates(digits: Int = 2): String {
                    var formattedCoordinates = ""
                    for (edge in edges) {
                        if (formattedCoordinates.isNotEmpty()) formattedCoordinates += "-"
                        val p1OnCanvas = edge.point1.getOnCanvas()
                        val formattedX = p1OnCanvas.getFormattedX(digits)
                        val formattedY = p1OnCanvas.getFormattedY(digits)
                        formattedCoordinates += "[$formattedX]/[$formattedY]"
                    }
                    return formattedCoordinates
                }

                /** Returns true if this polygon's caption is uniquely specified by given String
                 *  [caption]: Containing only the polygon's points captions and separating "-".
                 *  The single captions' order within [caption] is ignored. Example: "A-B-C-D"
                 *  Returns always false as long as a single point of this polygon has no caption
                 *  (empty String) - or if this polygon has no points at all.
                 *  [ignoreCase] specifies if the comparison should be case-sensitive.
                 *  Spaces in [caption] are completely ignored.
                 *  Caution: Included point captions that are part of other (longer) point captions
                 *  may result in a wrong return of false!
                 */
                fun hasSpecifiedCaption(caption: String, ignoreCase: Boolean = false): Boolean {
                    var hasSpecifiedOne = false
                    var captionToWorkWith = caption
                    if (size > 0) {
                        val separatorsOnly = "-".repeat(size - 1)
                        captionToWorkWith = captionToWorkWith.replace(" ","")
                        var pointNext = edges.first().point1
                        do {
                            if (captionToWorkWith.contains(pointNext.caption, ignoreCase)) {
                                captionToWorkWith = captionToWorkWith.replace(pointNext.caption,"", ignoreCase)
                                if (captionToWorkWith == separatorsOnly) {
                                    hasSpecifiedOne = true
                                    break
                                }
                            } else break
                            pointNext = getNextCornerPoint(pointNext)
                        } while (!pointNext.isDummy())
                    }
                    return hasSpecifiedOne
                }

                /** "Point within this polygon detection" (Ray Tracing approach):
                 *  Returns true if this polygon's "normed 2D projection on canvas" encases the
                 *  specified point [pointOnCanvas].
                 *  Returns false: Otherwise or if the specified [pointOnCanvas] is one of the
                 *  corner points of this polygon - if [pointOnCanvas] is a "dummy point" -
                 *  or if an [edgePointIsPartOf] is specified that is part of this polygon.
                 *  TODO evaluate if it (often?) falsely returns true or false.
                 */
                fun isEncasingPoint(pointOnCanvas: PointOnCanvasF, edgePointIsPartOf: Edge = edgeDummy): Boolean {
                    val (xP, yP) = pointOnCanvas
                    var countIntersections = 0  // count for Ray Tracing.
                    if (pointOnCanvas.isDummy() || edgePointIsPartOf in edges) return false
                    for (edge in edges) {
                        val point1C = edge.getPoint1OnCanvas();  val point2C = edge.getPoint2OnCanvas()
                        if (point1C == pointOnCanvas || point2C == pointOnCanvas) {
                            countIntersections = 0
                            break
                        }
                        val (x1, y1) = point1C;  val (x2, y2) = point2C
                        if ((yP < y1) != (yP < y2) && xP < x1 + (yP - y1) / (y2 - y1) * (x2 - x1)) {
                            countIntersections++
                        }
                    }
                    return countIntersections % 2 == 1  // Point within polygon if odd number of "ray" vs. edge intersections.
                }

                /** Returns true if this polygon's "normed 2D projection on canvas" is hiding
                 *  the specified point [pointOnCanvas] when its depth = [depthPoint].
                 *  Returns false: Otherwise or if the specified [pointOnCanvas] is one of the
                 *  corner points of this polygon.
                 *  FIXME ALWAYS returns true!
                 */
                fun isInFrontOfPoint(pointOnCanvas: PointOnCanvasF, depthPoint: Double): Boolean {
                    val pointsOnCanvas = polyhedronOnCanvas.polyhedronOnCanvasPointsF
                    var isInFront = false
                    var pointOfACornerC = PointOnCanvasF(positiveInf, positiveInf)
                    var indexCornerPoint = -1
                    if (!isEmpty()) {  // Get this polygon's first point of its first edge as a "Point On Canvas".
                        val pointOfACorner = edges[0].point1
                        indexCornerPoint = pointOfACorner.indexOfPoint
                        if (indexCornerPoint in pointsOnCanvas.indices)
                            pointOfACornerC = pointsOnCanvas[indexCornerPoint]  // TODO consider using corner point with an average distance from pointOnCanvas.
                    }
                    if (!pointOfACornerC.isDummy() && indexCornerPoint in pointsOnCanvas.indices) {
                        val edgeExtra = EdgeOnCanvas(pointOnCanvas, pointOfACornerC)
                        val pointOfACorner = points[indexCornerPoint]  // TODO adapt if "obtaining algorithm" of pointOfACornerC is changed.
                        for (edge in edges) {
                            if (edge.point1 == pointOfACorner || edge.point2 == pointOfACorner) continue
                            val pointIntersection = getIntersectionPointOnCanvas(edgeExtra, edge.getCanvasProjection(),true)
                            if (!pointIntersection.isDummy()) {
                                // Even a pointIntersection located outside polygon should return a valid depth:
                                val depthAtIntersection = edge.getDepthAtPoint(pointIntersection,true)
                                val lenExtendedExtraEdge = pointIntersection.getDistance(pointOfACornerC)
                                val distance = edgeExtra.getLength()
                                val depthCornerPoint = polyhedronOnCanvas.polyhedronOnCanvasPointsDepth[indexCornerPoint]
                                val depthPointOnPolygonPlane = distance / lenExtendedExtraEdge * (depthAtIntersection - depthCornerPoint)
                                isInFront = depthPointOnPolygonPlane < depthPoint
                                break
                            }
                        }
                    }
                    return isInFront
                }
            }  // inner class Polygon

            /** [collection] of polygons - capable of "doing the collecting" by analyzing
             *  the current edges.singleEdges: [collect]
             */
            inner class Polygons {
                val collection = mutableListOf<Polygon>()

                /** Clears all polygons: [collection].
                 */
                fun clear() {
                    collection.clear()
                }

                /** Returns true if the specified polygon [polygon] is already
                 *  existing in [collection]: Checking if every edge exists (by reference)
                 *  while having the same number of edges.
                 */
                fun isAlreadyExisting(polygon: Polygon): Boolean {
                    var isExisting = false
                    for (polygonFromCollection in collection) {
                        if (polygon.edges.size == polygonFromCollection.edges.size) {
                            var areTheSame = true
                            for (edge in polygon.edges) {
                                if (!polygonFromCollection.edges.contains(edge)) {
                                    areTheSame = false
                                    break
                                }
                            }
                            if (areTheSame) {
                                isExisting = true
                                break
                            }
                        }
                    }
                    return isExisting
                }

                /** Returns a polygon specified by its [caption] - returning empty one if not found.
                 *  For example: [caption] = "A-B-C-D" when "A" to "D" are the captions of the
                 *  Polygon's single points. The "-" separators must be included. Spaces within
                 *  [caption] get completely ignored. Returns empty polygon as well if extra
                 *  characters are included.
                 *  With [ignoreCase] = true, the case sensitivity can be turned off - getting
                 *  the same result for the example above with [caption] = "a-B-c-d" and so on.
                 *  Caution: If there are point captions included for the wanted polygon that are
                 *  part of other (longer) point captions, an empty polygon may get returned!
                 */
                fun getPolygonByCaption(caption: String, ignoreCase: Boolean = false): Polygon {
                    var polygonDesired = Polygon()
                    for (polygon in collection) {
                        val isFound = polygon.hasSpecifiedCaption(caption, ignoreCase)
                        if (isFound) {
                            polygonDesired = polygon
                            break
                        }
                    }
                    return polygonDesired
                }

                /** Start to (re-)collect all present polygons (perfectly defined by [edges]) -
                 *  (re-)filling [collection].
                 *  Updates & considers edges.countPolygonsTheEdgesArePartOf.
                 */
                fun collect() {
                    clear()
                    edges.resetPolygonMemberships()
                    // Loop through all currently defined edges - seeking out for (additional) polygons:
                    for (edge in edges.singleEdges) {
                        if (edge.countPolygonsItIsPartOf < 2) {  // Only make edges to the initial one that aren't already considered "part of 2 polygons".
                            val edgesFirstPointIsPartOf = edges.getAllPointIsPartOf(edge.point1).toMutableList()
                            for (edgePointIsPartOf in edgesFirstPointIsPartOf) {
                                val indexNext = edges.getIndexOfEdge(edgePointIsPartOf)  // TODO if "senseless extra edges" should be allowed for building polygons: Remove that.
                                if (edgePointIsPartOf != edge && edges.singleEdges[indexNext].countPolygonsItIsPartOf < 2) {
                                    val polygon = getPolygonForPlaneDefiningEdges(edge, edgePointIsPartOf)
                                    if (!polygon.isEmpty() && !isAlreadyExisting(polygon)) {
                                        collection.add(polygon)
                                        edges.incrementPolygonMemberships(polygon)
                                    }
                                }
                            }
                        }
                    }
                }

                /** Returns instance of polygon for the 2 given egdes [edge1] & [edge2].
                 * Returns EMPTY instance of polygon if no closed path (assembled out of [edges])
                 * got found on the defined plane: polygonReturned.isEmpty() == true
                 * ((Starting to collect edges from point [edge1] on.))
                 * FIXME make it works for "senseless extra edges": Right now it goes into an endless
                 *       loop (within the inner for loop) - causing an "out of memory error" e.g.
                 *       within handling the "senseless extra edge" that gets added to G17:  P-L
                 * FIXME consider that other polygons could be located on this plane as well!
                 *       => return List<Polygon>
                 */
                private fun getPolygonForPlaneDefiningEdges(edge1: Edge, edge2: Edge): Polygon {
                    var isPathContinuationPossible = true
                    var isPathContinuationFound: Boolean
                    val polygon = Polygon()
                    var edgePathCursor = edge1
                    var pointPathCursor = edge1.point1
                    val pointsPlaneDefining = mutableListOf(edge1.point1, edge1.point2)
                    if (!pointsPlaneDefining.contains(edge2.point1)) pointsPlaneDefining.add(edge2.point1)
                    else pointsPlaneDefining.add(edge2.point2)
                    do {  // Step through found, "chained" edges beginning with edge1.
                        val edgesCursorPointIsPartOf = edges.getAllPointIsPartOf(pointPathCursor).toMutableList()
                        if (edgesCursorPointIsPartOf.size > 1) {
                            isPathContinuationFound = false
                            for (edgePathCandidate in edgesCursorPointIsPartOf) {
                                if (edgePathCandidate != edgePathCursor) {
                                    var pointNewToPath = edgePathCandidate.point1
                                    if (pointNewToPath == pointPathCursor) pointNewToPath = edgePathCandidate.point2
                                    val points = pointsPlaneDefining.toMutableList()
                                    if (!points.contains(pointNewToPath)) points.add(pointNewToPath)  // New point could be one of the original "Plane Defining Points".
                                    if (edges.areOnSamePlane(points)) {
                                        polygon.edges.add(edgePathCandidate)
                                        isPathContinuationFound = true
                                        edgePathCursor = edgePathCandidate
                                        pointPathCursor = pointNewToPath
                                        break
                                    }
                                }
                            }
                            if (!isPathContinuationFound) isPathContinuationPossible = false
                        } else {
                            isPathContinuationPossible = false
                        }
                    } while (isPathContinuationPossible && edgePathCursor != edge1)
                    if (edgePathCursor != edge1) polygon.clear()
                    return polygon
                }
            }  // inner class Polygons

            /** Creates (normed) 2D canvas points from the 3D polyhedron points: [polyhedronOnCanvasPointsF]
             *  Just projecting the original values without any adaptation to a 2D canvas.
             *  PLUS: Separate axis points for shown axes (generalSettings.showAxisX..Z == true).
             *  [pointsCurrentlyOnCanvasF] on the other hand: Contains the absolute x/y-coordinates
             *  of the points within the canvas ImageView.
             */
            inner class PolyhedronOnCanvas {
                val polyhedronOnCanvasPointsF = mutableListOf<PointOnCanvasF>()  // Supposed to have the same index with the matching Point3D in points.
                val polyhedronOnCanvasPointsDepth = mutableListOf<Double>()      // Depth Factor for polyhedronOnCanvasPointsF.
                val pointsCurrentlyOnCanvasF = mutableListOf<PointOnCanvasF>()   // Absolute 2D coordinates in pixel (within canvas ImageView).
                // Negative & Positive endpoints of the axes. As soon as 1 point is present
                // the minimal length of an axis is "1 ruler unit (in the positive direction):
                val axisXOnCanvasPointsF = mutableListOf(PointOnCanvasF(0f, 0f), PointOnCanvasF(0f, 0f))
                val axisYOnCanvasPointsF = mutableListOf(PointOnCanvasF(0f, 0f), PointOnCanvasF(0f, 0f))
                val axisZOnCanvasPointsF = mutableListOf(PointOnCanvasF(0f, 0f), PointOnCanvasF(0f, 0f))
                // Outer bounds of current polyhedron (+ shown axis):
                var rectangleOfCanvas = RectF(negativeInf, negativeInf, positiveInf, positiveInf)

                fun reset() {
                    polyhedronOnCanvasPointsF.clear()
                    polyhedronOnCanvasPointsDepth.clear()
                    rectangleOfCanvas = RectF(negativeInf, negativeInf, positiveInf,positiveInf)
                    // All 4 "coordinate parts" == 0 mean "no axis existing":
                    axisXOnCanvasPointsF[0] = PointOnCanvasF(0f, 0f);  axisXOnCanvasPointsF[1] = PointOnCanvasF(0f, 0f)
                    axisYOnCanvasPointsF[0] = PointOnCanvasF(0f, 0f);  axisYOnCanvasPointsF[1] = PointOnCanvasF(0f, 0f)
                    axisZOnCanvasPointsF[0] = PointOnCanvasF(0f, 0f);  axisZOnCanvasPointsF[1] = PointOnCanvasF(0f, 0f)
                }

                fun update() {
                    var pointOnCanvasF: PointOnCanvasF
                    var depthFactor: Double
                    reset()
                    if (points.isNotEmpty()) {
                        pointOnCanvasF = points[0].projectedOntoCanvasF()
                        rectangleOfCanvas = RectF(pointOnCanvasF.x, pointOnCanvasF.y, pointOnCanvasF.x, pointOnCanvasF.y)
                        for (point3D in points) {
                            pointOnCanvasF = point3D.projectedOntoCanvasF()
                            depthFactor = generalSettings.depthFactor.getForPoint(point3D)
                            if (point3D.indexOfPoint >= 0) {
                                polyhedronOnCanvasPointsF.add(point3D.indexOfPoint, pointOnCanvasF)  // TODO evaluate if "indexOfPoint" actually works after polyhedronOnCanvasPointsF got cleared before this loop...
                                polyhedronOnCanvasPointsDepth.add(point3D.indexOfPoint, depthFactor)
                            } else {
                                polyhedronOnCanvasPointsF.add(pointOnCanvasF)
                                polyhedronOnCanvasPointsDepth.add(depthFactor)
                            }
                            updateRectangle(pointOnCanvasF.x, pointOnCanvasF.y)
                        }
                        // Add shown axes; axisInfo needs to be up to date to work (its .update() called before that):
                        if (generalSettings.showAxisX) {
                            if (axisInfo.fullRulerUnitsAxisX.minimum < 0) {
                                pointOnCanvasF = Point3D(axisInfo.fullRulerUnitsAxisX.minimum.toFloat(),0f,0f).projectedOntoCanvasF()
                                axisXOnCanvasPointsF[0] = pointOnCanvasF
                                updateRectangle(pointOnCanvasF.x, pointOnCanvasF.y)
                            }
                            if (axisInfo.fullRulerUnitsAxisX.maximum > 0 || axisInfo.fullRulerUnitsAxisX.minimum == 0 && axisInfo.fullRulerUnitsAxisX.maximum == 0) {
                                pointOnCanvasF = Point3D(axisInfo.fullRulerUnitsAxisX.maximum.toFloat(),0f,0f).projectedOntoCanvasF()
                                axisXOnCanvasPointsF[1] = pointOnCanvasF
                                updateRectangle(pointOnCanvasF.x, pointOnCanvasF.y)
                            }
                        }
                        if (generalSettings.showAxisY) {
                            if (axisInfo.fullRulerUnitsAxisY.minimum < 0) {
                                pointOnCanvasF = Point3D(0f, axisInfo.fullRulerUnitsAxisY.minimum.toFloat(), 0f).projectedOntoCanvasF()
                                axisYOnCanvasPointsF[0] = pointOnCanvasF
                                updateRectangle(pointOnCanvasF.x, pointOnCanvasF.y)
                            }
                            if (axisInfo.fullRulerUnitsAxisY.maximum > 0 || axisInfo.fullRulerUnitsAxisY.minimum == 0 && axisInfo.fullRulerUnitsAxisY.maximum == 0) {
                                pointOnCanvasF = Point3D(0f, axisInfo.fullRulerUnitsAxisY.maximum.toFloat(),0f).projectedOntoCanvasF()
                                axisYOnCanvasPointsF[1] = pointOnCanvasF
                                updateRectangle(pointOnCanvasF.x, pointOnCanvasF.y)
                            }
                        }
                        if (generalSettings.showAxisZ) {
                            if (axisInfo.fullRulerUnitsAxisZ.minimum < 0) {
                                pointOnCanvasF = Point3D(0f,0f, axisInfo.fullRulerUnitsAxisZ.minimum.toFloat()).projectedOntoCanvasF()
                                axisZOnCanvasPointsF[0] = pointOnCanvasF
                                updateRectangle(pointOnCanvasF.x, pointOnCanvasF.y)
                            }
                            if (axisInfo.fullRulerUnitsAxisZ.maximum > 0 || axisInfo.fullRulerUnitsAxisZ.minimum == 0 && axisInfo.fullRulerUnitsAxisZ.maximum == 0) {
                                pointOnCanvasF = Point3D(0f,0f, axisInfo.fullRulerUnitsAxisZ.maximum.toFloat()).projectedOntoCanvasF()
                                axisZOnCanvasPointsF[1] = pointOnCanvasF
                                updateRectangle(pointOnCanvasF.x, pointOnCanvasF.y)
                            }
                        }
                    }
                }

                /** Enhances the "rectangle of the canvas" if the delivered point
                 *  is located outside of the current rectangle.
                 */
                private fun updateRectangle(x: Float, y: Float) {
                    var (x1, y1) = rectangleOfCanvas.left to rectangleOfCanvas.top
                    var (x2, y2) = rectangleOfCanvas.right to rectangleOfCanvas.bottom
                    x1 = minOf(x1, x);  y1 = minOf(y1, y)
                    x2 = maxOf(x2, x);  y2 = maxOf(y2, y)
                    rectangleOfCanvas = RectF(x1, y1, x2, y2)
                }

                /** Returns Point3D instance that corresponds to the given PointOnCanvasF instance
                 *  [pointOnCanvasF] (within the [pointsCurrentlyOnCanvasF] list!).
                 *  Returns a "dummy Point3D" if none found.
                 */
                fun getCorrespondingPoint3D(pointOnCanvasF: PointOnCanvasF): Point3D {
                    var desiredIndexPoint2D = -1
                    var desiredPoint3D = pointDummy3D
                    // First (try to) get pointOnCanvasF's index within pointsCurrentlyOnCanvasF:
                    for ((indexPoint2D, point2D) in pointsCurrentlyOnCanvasF.withIndex()) {
                        if (point2D == pointOnCanvasF) {
                            desiredIndexPoint2D = indexPoint2D
                            break
                        }
                    }
                    if (desiredIndexPoint2D in 0 until pointsCurrentlyOnCanvasF.size) {
                        for (point3D in points) {
                            if (point3D.indexOfPoint == desiredIndexPoint2D) {
                                desiredPoint3D = point3D
                                break
                            }
                        }
                    }
                    return desiredPoint3D
                }

                /** Returns list of 3D points located within detectPointsWithinTapDiameter on canvas
                 *  with center [centerX] / [centerY].
                 *  Returns empty list if not a single point is present.
                 */
                fun getPoints3DWithinTapDiameter(centerX: Float, centerY: Float): List<Point3D> {
                    val pointsInDiameter3D = mutableListOf<Point3D>()
                    val pointTapped = PointOnCanvasF(centerX, centerY)
                    val tapRadius = generalSettings.detectPointsWithinTapDiameter / 2f
                    for (point2D in pointsCurrentlyOnCanvasF) {
                        if (point2D.getDistance(pointTapped) < tapRadius)
                            pointsInDiameter3D.add(getCorrespondingPoint3D(point2D))
                    }
                    return pointsInDiameter3D.toList()
                }
            }  // inner class PolyhedronOnCanvas

            /** Draws polyhedron & axis (if desired) onto given [canvas].
             *  TODO consider to center polyhedron (plus axes) on its canvas area.
             */
            fun drawOnCanvas(canvas: Canvas) {
                var marginLeftOfPolyhedron = generalSettings.axesMarginLeft
                var marginTopOfPolyhedron =  generalSettings.axesMarginTop
                var marginRightOfPolyhedron = generalSettings.axesMarginRight
                var marginBottomOfPolyhedron = generalSettings.axesMarginBottom
                val polyhedronMaxSpaceX: Int;  val polyhedronMaxSpaceY: Int  // Maximum available space for polyhedron on canvas.
                // val polyhedronWidth: Int;  val polyhedronHeight: Int  // TODO delete when considered obsolete.
                val sizeFactor: Float  // Factor indicating how many times the polyhedron is larger that calculated with "one ruler unit equals 1 pixel".
                val marginLeftOfOrigin: Float;  val marginTopOfOrigin: Float
                val marginLeft: Float;  val marginTop: Float  // Total margins of polyhedron's origin.
                val rectNormedCanvas = polyhedronOnCanvas.rectangleOfCanvas  //pointsNormedCanvas()
                if (generalSettings.showAxesIndicator) marginTopOfPolyhedron += axesIndicator.bottom.toInt()
                marginLeftOfPolyhedron += axisInfo.dimensionalClaimArrowArea.extraMarginLeft.toInt()
                marginTopOfPolyhedron += axisInfo.dimensionalClaimArrowArea.extraMarginTop.toInt()
                marginRightOfPolyhedron += axisInfo.dimensionalClaimArrowArea.extraMarginRight.toInt()
                marginBottomOfPolyhedron += axisInfo.dimensionalClaimArrowArea.extraMarginBottom.toInt()
                polyhedronMaxSpaceX = nCanvasWidthPixel - marginLeftOfPolyhedron - marginRightOfPolyhedron
                polyhedronMaxSpaceY = nCanvasHeightPixel - marginTopOfPolyhedron - marginBottomOfPolyhedron
                sizeFactor = minOf(polyhedronMaxSpaceX / rectNormedCanvas.width(), polyhedronMaxSpaceY / rectNormedCanvas.height())
                // polyhedronWidth = (rectNormedCanvas.width() * sizeFactor).toInt()
                // polyhedronHeight = (rectNormedCanvas.height() * sizeFactor).toInt()
                marginLeftOfOrigin = -rectNormedCanvas.left * sizeFactor
                marginTopOfOrigin = -rectNormedCanvas.top * sizeFactor
                marginLeft = marginLeftOfPolyhedron + marginLeftOfOrigin
                marginTop = marginTopOfPolyhedron + marginTopOfOrigin

                fun drawAxesOnCanvas() {
                    val lateralLen = generalSettings.lengthOfLateralDartPart.toDouble()
                    val arrowAreaLen = generalSettings.lengthOfAxisArrowArea.toDouble()
                    val path = Path()
                    val paintAxis = Paint()
                    paintAxis.isAntiAlias = true
                    paintAxis.style = Paint.Style.STROKE
                    paintAxis.strokeWidth = generalSettings.axesStrokeWidth
                    if (generalSettings.showAxisX) {
                        paintAxis.color = generalSettings.colorAxisX
                        if (axisInfo.fullRulerUnitsAxisX.minimum < 0) {
                            path.moveTo(marginLeft, marginTop)  // margin... serve as "axis origin coordinates" as well.
                            path.lineTo(marginLeft + polyhedronOnCanvas.axisXOnCanvasPointsF[0].x * sizeFactor, marginTop + polyhedronOnCanvas.axisXOnCanvasPointsF[0].y * sizeFactor)
                            canvas.drawPath(path, paintAxis)
                            path.reset()
                            // TODO draw "end dart" (plus line elongation) here!
                        }
                        if (axisInfo.fullRulerUnitsAxisX.maximum > 0 || axisInfo.fullRulerUnitsAxisX.minimum == 0 && axisInfo.fullRulerUnitsAxisX.maximum == 0) {
                            val endPointRulerPartX = marginLeft + polyhedronOnCanvas.axisXOnCanvasPointsF[1].x * sizeFactor
                            val endPointRulerPartY = marginTop + polyhedronOnCanvas.axisXOnCanvasPointsF[1].y * sizeFactor
                            path.moveTo(marginLeft, marginTop)
                            path.lineTo(endPointRulerPartX, endPointRulerPartY)
                            canvas.drawPath(path, paintAxis)
                            path.reset()
                            // Draw "dart part" of axis:
                            val vectorDartPartLeft = MyMath.Vector(endPointRulerPartX.toDouble(), endPointRulerPartY.toDouble())
                            vectorDartPartLeft.add(arrowAreaLen, generalSettings.viewAngleAxisX.toInt())
                            val endPointDartX = vectorDartPartLeft.get_dX().toFloat()
                            val endPointDartY = vectorDartPartLeft.get_dY().toFloat()
                            canvas.drawLine(endPointRulerPartX, endPointRulerPartY, endPointDartX, endPointDartY, paintAxis)  // = "dart's shaft"
                            val vectorDartPartRight = vectorDartPartLeft.copy()
                            vectorDartPartLeft.add(lateralLen,generalSettings.viewAngleAxisX.toInt() - 180 + generalSettings.angleDartPart / 2)
                            canvas.drawLine(endPointDartX, endPointDartY, vectorDartPartLeft.get_dX().toFloat(), vectorDartPartLeft.get_dY().toFloat(), paintAxis)
                            vectorDartPartRight.add(lateralLen,generalSettings.viewAngleAxisX.toInt() - 180 - generalSettings.angleDartPart / 2)
                            canvas.drawLine(endPointDartX, endPointDartY, vectorDartPartRight.get_dX().toFloat(), vectorDartPartRight.get_dY().toFloat(), paintAxis)
                        }
                    }
                    if (generalSettings.showAxisY) {
                        paintAxis.color = generalSettings.colorAxisY
                        if (axisInfo.fullRulerUnitsAxisY.minimum < 0) {
                            path.moveTo(marginLeft, marginTop)
                            path.lineTo(marginLeft + polyhedronOnCanvas.axisYOnCanvasPointsF[0].x * sizeFactor, marginTop + polyhedronOnCanvas.axisYOnCanvasPointsF[0].y * sizeFactor)
                            canvas.drawPath(path, paintAxis)
                            path.reset()
                            // TODO draw "end dart" (plus line elongation) here!
                        }
                        if (axisInfo.fullRulerUnitsAxisY.maximum > 0 || axisInfo.fullRulerUnitsAxisY.minimum == 0 && axisInfo.fullRulerUnitsAxisY.maximum == 0) {
                            val endPointRulerPartX = marginLeft + polyhedronOnCanvas.axisYOnCanvasPointsF[1].x * sizeFactor
                            val endPointRulerPartY = marginTop + polyhedronOnCanvas.axisYOnCanvasPointsF[1].y * sizeFactor
                            path.moveTo(marginLeft, marginTop)
                            path.lineTo(endPointRulerPartX, endPointRulerPartY)
                            canvas.drawPath(path, paintAxis)
                            path.reset()
                            // Draw "dart part" of axis:
                            val vectorDartPartLeft = MyMath.Vector(endPointRulerPartX.toDouble(), endPointRulerPartY.toDouble())
                            vectorDartPartLeft.add(arrowAreaLen, generalSettings.viewAngleAxisY.toInt())
                            val endPointDartX = vectorDartPartLeft.get_dX().toFloat()
                            val endPointDartY = vectorDartPartLeft.get_dY().toFloat()
                            canvas.drawLine(endPointRulerPartX, endPointRulerPartY, endPointDartX, endPointDartY, paintAxis)  // = "dart's shaft"
                            val vectorDartPartRight = vectorDartPartLeft.copy()
                            vectorDartPartLeft.add(lateralLen,generalSettings.viewAngleAxisY.toInt() - 180 + generalSettings.angleDartPart / 2)
                            canvas.drawLine(endPointDartX, endPointDartY, vectorDartPartLeft.get_dX().toFloat(), vectorDartPartLeft.get_dY().toFloat(), paintAxis)
                            vectorDartPartRight.add(lateralLen,generalSettings.viewAngleAxisY.toInt() - 180 - generalSettings.angleDartPart / 2)
                            canvas.drawLine(endPointDartX, endPointDartY, vectorDartPartRight.get_dX().toFloat(), vectorDartPartRight.get_dY().toFloat(), paintAxis)                        }
                    }
                    if (generalSettings.showAxisZ) {
                        paintAxis.color = generalSettings.colorAxisZ
                        if (axisInfo.fullRulerUnitsAxisZ.minimum < 0) {
                            path.moveTo(marginLeft, marginTop)
                            path.lineTo(marginLeft + polyhedronOnCanvas.axisZOnCanvasPointsF[0].x * sizeFactor, marginTop + polyhedronOnCanvas.axisZOnCanvasPointsF[0].y * sizeFactor)
                            canvas.drawPath(path, paintAxis)
                            path.reset()
                            // TODO draw "end dart" (plus line elongation) here!
                        }
                        if (axisInfo.fullRulerUnitsAxisZ.maximum > 0 || axisInfo.fullRulerUnitsAxisZ.minimum == 0 && axisInfo.fullRulerUnitsAxisZ.maximum == 0) {
                            val endPointRulerPartX = marginLeft + polyhedronOnCanvas.axisZOnCanvasPointsF[1].x * sizeFactor
                            val endPointRulerPartY = marginTop + polyhedronOnCanvas.axisZOnCanvasPointsF[1].y * sizeFactor
                            path.moveTo(marginLeft, marginTop)
                            path.lineTo(endPointRulerPartX, endPointRulerPartY)
                            canvas.drawPath(path, paintAxis)
                            path.reset()
                            // Draw "dart part" of axis:
                            val vectorDartPartLeft = MyMath.Vector(endPointRulerPartX.toDouble(), endPointRulerPartY.toDouble())
                            vectorDartPartLeft.add(arrowAreaLen, generalSettings.viewAngleAxisZ.toInt())
                            val endPointDartX = vectorDartPartLeft.get_dX().toFloat()
                            val endPointDartY = vectorDartPartLeft.get_dY().toFloat()
                            canvas.drawLine(endPointRulerPartX, endPointRulerPartY, endPointDartX, endPointDartY, paintAxis)  // = "dart's shaft"
                            val vectorDartPartRight = vectorDartPartLeft.copy()
                            vectorDartPartLeft.add(lateralLen,generalSettings.viewAngleAxisZ.toInt() - 180 + generalSettings.angleDartPart / 2)
                            canvas.drawLine(endPointDartX, endPointDartY, vectorDartPartLeft.get_dX().toFloat(), vectorDartPartLeft.get_dY().toFloat(), paintAxis)
                            vectorDartPartRight.add(lateralLen,generalSettings.viewAngleAxisZ.toInt() - 180 - generalSettings.angleDartPart / 2)
                            canvas.drawLine(endPointDartX, endPointDartY, vectorDartPartRight.get_dX().toFloat(), vectorDartPartRight.get_dY().toFloat(), paintAxis)
                        }
                    }
                }
                fun drawPointsOnCanvas() {
                    val paintPoint = Paint()
                    paintPoint.isAntiAlias = true
                    paintPoint.style = Paint.Style.STROKE
                    paintPoint.strokeWidth = generalSettings.axesStrokeWidth  // TODO experiment with that. If wanted: Add own generalSetting "pointStrength"
                    paintPoint.color = generalSettings.colorPoints
                    polyhedronOnCanvas.pointsCurrentlyOnCanvasF.clear()
                    for ((indexOfPoint, pointF) in polyhedronOnCanvas.polyhedronOnCanvasPointsF.withIndex()) {
                        val x = marginLeft + pointF.x * sizeFactor
                        val y = marginTop + pointF.y * sizeFactor
                        canvas.drawCircle(x, y,generalSettings.pointSize / 2, paintPoint)
                        polyhedronOnCanvas.pointsCurrentlyOnCanvasF.add(indexOfPoint, PointOnCanvasF(x, y))
                    }
                }
                fun drawEdgesOnCanvas() {
                    val paintSolid = generalSettings.paints.paintEdgeSolid
                    val paintDashed = generalSettings.paints.paintEdgeDashed
                    for (edge in edges.singleEdges) {
                        if (edge.edgeFragments.isNotEmpty()) {
                            for (edgeFragment in edge.edgeFragments) {
                                val (p1, p2) = edgeFragment
                                val paint = if (edgeFragment.isVisible) paintSolid else paintDashed
                                canvas.drawLine(marginLeft + p1.x * sizeFactor,marginTop + p1.y * sizeFactor,
                                    marginLeft + p2.x * sizeFactor,marginTop + p2.y * sizeFactor, paint)
                            }
                        } else {
                            val pF1 = polyhedronOnCanvas.polyhedronOnCanvasPointsF[edge.point1.indexOfPoint]
                            val pF2 = polyhedronOnCanvas.polyhedronOnCanvasPointsF[edge.point2.indexOfPoint]
                            canvas.drawLine(marginLeft + pF1.x * sizeFactor,marginTop + pF1.y * sizeFactor,
                                marginLeft + pF2.x * sizeFactor,marginTop + pF2.y * sizeFactor, paintSolid)
                        }
                    }
                }

                drawAxesOnCanvas()
                drawEdgesOnCanvas()
                drawPointsOnCanvas()
            }
        }  // class Polyhedron
    }  // UserInterface

    /** Load saved settings (or use app's default settings if not available yet).
     *  TODO use for implementing "restore preferences & design.
     */
    fun loadPreferences() {
        sharedPreferences?.let {
            // "Global Settings":
            val nIntervalMS = it.getInt("interval_ms", 4000)
            val bvMuted = it.getBoolean("muted", false)
            val bvShowPictureOverlay = it.getBoolean("show_picture_overlay", false)
            // Default Source Settings:
            val bvUseSubFolders = it.getBoolean("default__use_sub_folders", false)
            val bvShowWeatherDataIfAvailable = it.getBoolean("default__overlay_show_weather_data", false)
            // TODO: Make the yet unhandled settings savable too - loading them HERE (in a loop):
            //       while(it.contains("some_key_1"))
        }
    }

    /** TODO use for implementing "save preferences & design.
     */
    fun saveInt(nInt: Int) {
        sharedPreferences?.let {
            val preferencesEditor = it.edit()
            preferencesEditor.putInt("interval_ms", nInt)
            preferencesEditor.apply()
        }
    }

    /** TODO use for implementing "save preferences & design.
     */
    fun saveFlag(isTrue: Boolean) {
        sharedPreferences?.let {
            val preferencesEditor = it.edit()
            preferencesEditor.putBoolean("muted", isTrue)
            preferencesEditor.apply()
        }
    }
}  // class PolyhedronCanvas