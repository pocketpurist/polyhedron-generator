package com.pocketpurist.polyeder_generator

import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.atan2
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.sqrt

class MyMath {
    /** Vector handling in 2 forms: magnitude & angle / XY coordinates.
     * (The vector's origin always is x=0, y=0.)
     */
    class Vector {
        private val TO_RADIANS_FACTOR = Math.PI / 180.0

        /** Get vector's magnitude.
         * @return magnitude.
         */
        var magnitude = 0.0
            private set

        private var nAngle_Degrees = 0

        /** Get vector's angle in radians.
         * @return its angle as a double.
         */
        var angle_Radians = 0.0
            private set

        private var dX = 0.0
        private var dY = 0.0 // Coordinates of the vector's head.

        val nX: Int
            /** Get integer value of x dimension.
             *  @return rounded x value.
             */
            get() = (dX + 0.5).toInt()

        val nY: Int
            /** Get integer value of y dimension.
             *  @return rounded y value.
             */
            get() = (dY + 0.5).toInt()

        /** Initialize vector instance specifying its magnitude & angle.
         * @param dMagnitude: Vector's magnitude. Can be 0.
         * @param nAngle_Degrees: Angle in degrees. 0=east, 90=north, 180=west, 270=south etc.
         */
        constructor(dMagnitude: Double, nAngle_Degrees: Int) {
            magnitude = dMagnitude
            this.nAngle_Degrees = nAngle_Degrees
            if (nAngle_Degrees != 0) angle_Radians = -nAngle_Degrees * TO_RADIANS_FACTOR  // // Changed on 2023-11-23: MINUS nAngle_Degrees
            if (dMagnitude != 0.0) {
                dX = dMagnitude * cos(angle_Radians)
                dY = dMagnitude * sin(angle_Radians)
            }
        }

        /** Initialize vector instance specifying its (head's) XY coordinates.
         * @param dX: Vector's X coordinate.
         * @param dY: Vector's Y coordinate.
         */
        constructor(dX: Double, dY: Double) {
            setXY(dX, dY)
        }

        /** Initialize vector with given vector's parameters.
         * @param vector: Given one.
         */
        constructor(vector: Vector) {
            set(vector)
        }

        /** Add vector with angle in degrees.
         * @param dMagnitude: Magnitude aspect of vector to add.
         * @param nAngle_Degrees: Vector's angle aspect (in degrees) to add.
         */
        fun add(dMagnitude: Double, nAngle_Degrees: Int) {
            val dAngle_Rad = -nAngle_Degrees * TO_RADIANS_FACTOR  // Changed on 2023-11-03: MINUS nAngle_Degrees
            dX += dMagnitude * cos(dAngle_Rad)
            dY += dMagnitude * sin(dAngle_Rad)
            setMyMagnitude(dX, dY)
            setMyAngle(dX, dY)
        }

        /** Add vector over its xy coordinates.
         * @param dX: X coordinate of vector to add.
         * @param dY: Y coordinate of vector to add.
         */
        fun add(dX: Double, dY: Double) {
            this.dX += dX
            this.dY += dY
            setMyMagnitude(this.dX, this.dY)
            setMyAngle(this.dX, this.dY)
        }

        /** Add another vector.
         * @param vector: Vector to add.
         */
        fun add(vector: Vector) {
            add(vector.get_dX(), vector.get_dY())
        }

        /** Set vector's magnitude (and update it's x/y values) leaving its angle untouched.
         * @param nMagnitude: Vector's new magnitude (integer!).
         */
        fun setMagnitude(nMagnitude: Int) {
            magnitude = nMagnitude.toDouble()
            dX = magnitude * cos(angle_Radians)  // TODO evaluate if MINUS angle_Radians is needed here!
            dY = magnitude * sin(angle_Radians)  //         - " -
        }

        /** Set vector's magnitude (and update it's x/y values) leaving its angle untouched.
         * @param dMagnitude: Vector's new magnitude.
         */
        fun setMagnitude(dMagnitude: Double) {
            magnitude = dMagnitude
            dX = dMagnitude * cos(angle_Radians)  // TODO evaluate if MINUS angle_Radians is needed here!
            dY = dMagnitude * sin(angle_Radians)  //         - " -
        }

        /** Set vector by specifying its magnitude & angle.
         * @param dMagnitude: Vector's magnitude. Can be 0.
         * @param nAngle_Degrees: Angle in degrees. 0=east, 90=north, 180=west, 270=south etc.
         */
        fun setMagnitudeAndAngle(dMagnitude: Double, nAngle_Degrees: Int) {
            magnitude = dMagnitude
            this.nAngle_Degrees = nAngle_Degrees
            angle_Radians = nAngle_Degrees * TO_RADIANS_FACTOR
            if (dMagnitude == 0.0) {
                dY = 0.0
                dX = dY
            } else {
                dX = dMagnitude * cos(angle_Radians)  // TODO evaluate if MINUS angle_Radians is needed here!
                dY = dMagnitude * sin(angle_Radians)  //         - " -
            }
        }

        /** Set vector over the head's x/y value (vector's origin always = 0/0).
         * @param dX: Vector's new head X coordinate.
         * @param dY: Vector's new head Y coordinate.
         */
        fun setXY(dX: Double, dY: Double) {
            this.dX = dX
            this.dY = dY
            if (dX != 0.0 || dY != 0.0) {
                setMyMagnitude(dX, dY)
                setMyAngle(dX, dY)
            } else {
                magnitude = 0.0
                nAngle_Degrees = 0
                angle_Radians = 0.0
            }
        }

        /** Set vector to given vector's parameters.
         * @param vector: Given one.
         */
        fun set(vector: Vector) {
            setXY(vector.get_dX(), vector.get_dY())
        }

        /** Rotate the vector counterclockwise.
         * @param nAngle_Degrees: Angle to rotate the vector by in degrees.
         */
        fun rotate(nAngle_Degrees: Int) {
            rotate(nAngle_Degrees * TO_RADIANS_FACTOR)
        }

        /** Rotate the vector counterclockwise.
         * @param dAngle_Radians: Angle to rotate the vector by in radians.
         */
        fun rotate(dAngle_Radians: Double) {
            val dSin: Double
            val dCos: Double
            val dNewX: Double
            if (dAngle_Radians != 0.0) {
                dSin = sin(-dAngle_Radians)  // Changed on 2023-11-03: MINUS dAngle_Radians
                dCos = cos(-dAngle_Radians)  //   - " -
                dNewX = dX * dCos - dY * dSin
                dY = dX * dSin + dY * dCos
                dX = dNewX
                setMyMagnitude(dX, dY)
                setMyAngle(dX, dY)
            }
        }

        /** Invert vector: Set x dimension to its negative value (and update its magnitude & angle).
         */
        fun invertX() {
            dX = -dX
            setMyMagnitude(dX, dY)
            setMyAngle(dX, dY)
        }

        /** Invert vector: Set y dimension to its negative value (and update its magnitude & angle).
         */
        fun invertY() {
            dY = -dY
            setMyMagnitude(dX, dY)
            setMyAngle(dX, dY)
        }

        /** Get X coordinate.
         * @return x value.
         */
        fun get_dX(): Double {
            return dX
        }


        /** Get Y coordinate.
         * @return Y value.
         */
        fun get_dY(): Double {
            return dY
        }

        var angle: Int
            /** Get vector's angle in (full) degrees.
             * @return its angle: 0=east, 90=north, 180=west, 270=south etc.
             */
            get() = nAngle_Degrees
            /** Set vector's angle in degrees (and update it's x/y values).
             * Leaves its magnitude untouched.
             * @param nAngle_Degrees: Vector's new angle (integer!)
             */
            set(nAngle_Degrees) {
                this.nAngle_Degrees = nAngle_Degrees
                angle_Radians = nAngle_Degrees * TO_RADIANS_FACTOR
                dX = magnitude * cos(angle_Radians)  // TODO evaluate if MINUS angle_Radians is needed here!
                dY = magnitude * sin(angle_Radians)  //         - " -
            }

        /** Get angle difference between this vector and a second one in degrees.
         * @param nSecondAngle_Degrees: Second angle.
         * @return difference in degrees.
         */
        fun getAngleDifference(nSecondAngle_Degrees: Int): Int {
            val nSign: Int // 1 or -1
            var nAngleDiff = abs(nAngle_Degrees - nSecondAngle_Degrees) % 360
            nAngleDiff = if (nAngleDiff > 180) 360 - nAngleDiff else nAngleDiff
            nSign =
                if (nAngle_Degrees - nSecondAngle_Degrees >= 0 && nAngle_Degrees - nSecondAngle_Degrees <= 180 || nAngle_Degrees - nSecondAngle_Degrees <= -180 && nAngle_Degrees - nSecondAngle_Degrees >= -360) 1 else -1
            return nAngleDiff * nSign
        }

        /** Get angle difference between this vector and a second one in radiant.
         * @param vector2: Second vector.
         * @return difference in radiant.
         */
        fun getAngleDifference(vector2: Vector): Double {
            val dDot = dX * vector2.get_dX() + dY * vector2.get_dY() // Dot product.
            val dDet = dX * vector2.get_dY() - dY * vector2.get_dX() // Determinant.
            return atan2(dDet, dDot) // atan2(y, x) or atan2(sin, cos)
        }

        /** Copy this Vector instance.
         * @return copy of the vector.
         */
        fun copy(): Vector {
            return Vector(magnitude, -nAngle_Degrees)  // Changed on 2023-11-23: MINUS nAngle_Degrees  TODO evaluate WHY the negative sign.
        }

        private fun setMyMagnitude(dNewX: Double, dNewY: Double) {
            magnitude = sqrt(dNewX * dNewX + dNewY * dNewY)  // Use "pythagoras" to get the resulting magnitude.
        }

        private fun setMyAngle(dNewX: Double, dNewY: Double) {
            angle_Radians = atan2(dNewY, dNewX)
            nAngle_Degrees = toDegrees(angle_Radians)
        }

        /** Convert angle from radians to degrees.
         * @param dAngle: Angle in radians.
         * @return angle in degrees.
         */
        private fun toDegrees(dAngle: Double): Int {
            return (dAngle * 180.0 / Math.PI + 0.5).toInt() // "+ 0.5": turn the "int truncating" into an actual rounding.
        }
    }  // class Vector

    /** 3D Vector handling in 2 forms: magnitude & angles (polar) / XYZ coordinates (cartesian).
     * (The vector's origin always is x=0, y=0, z=0. The standard unit for angles is radian: Used
     *  if not specified otherwise.)
     */
    class Vector3D {
        var isUpdateInProgress = false
        /** Factor ("constant") to calculate radian out of degrees = Pi / 180
         */
        val degreesToRadian = Math.PI / 180.0

        /** Vector's x-coordinate.
         */
        var x: Double = 0.0
            set(x) {
                field = x
                updatePolarCoordinates()
            }
        /** Vector's y-coordinate.
         */
        var y: Double = 0.0
            set(y) {
                field = y
                updatePolarCoordinates()
            }
        /** Vector's z-coordinate.
         */
        var z: Double = 0.0
            set(z) {
                field = z
                updatePolarCoordinates()
            }

        /** Vector's length.
         */
        var magnitude: Double = 0.0
            set(magnitude) {
                field = magnitude
                updateCartesianCoordinates()
            }

        /** Vector's angle Φ (also: "azimuth") in radian from x-axis to vector projected
         *  onto the "x-y axes plane".
         *  Increasing this angle means: rotating the vector counter-clockwise.
         */
        var phi: Double = 0.0
            set (phi) {
                field = phi
                updateCartesianCoordinates()
            }
        /** Vector's angle (also: Θz or "elevation") in radian from z-axis to the vector itself.
         */
        var theta: Double = 0.0
            set (theta) {
                field = theta
                updateCartesianCoordinates()
            }

        fun getPhiInDegrees() = phi / degreesToRadian
        fun setPhiInDegrees(phiDegrees: Double) {
            isUpdateInProgress = true
            phi = phiDegrees * degreesToRadian
            isUpdateInProgress = false
            updateCartesianCoordinates()
        }

        fun getThetaInDegrees() = theta / degreesToRadian
        fun setThetaInDegrees(thetaDegrees: Double) {
            isUpdateInProgress = true
            theta = thetaDegrees * degreesToRadian
            isUpdateInProgress = false
            updateCartesianCoordinates()
        }

        /** Returns the angle between [x] and the vector's [magnitude] in degrees.
         */
        fun getAlphaInDegrees() = acos(x / magnitude) / degreesToRadian
        /** Rotates the vector relative to x-axis (angle alpha; on the plane built by
         *  x-axis & the vector) - given the relative angle in degrees: [angleDegrees].
         *  (Leaving vector's angle [theta] alone.)
         */
        fun setAlphaInDegrees(angleDegrees: Double) {
            setAlphaAndThetaInDegrees(angleDegrees,theta / degreesToRadian)
        }

        /** Rotates the vector relative to x-axis (angle alpha; on the plane built by
         *  x-axis & the vector) AND to z-axis at once - given the 2 relative angles in degrees:
         *  [angleAlphaDegrees] and [angleThetaDegrees].
         */
        fun setAlphaAndThetaInDegrees(angleAlphaDegrees: Double, angleThetaDegrees: Double) {
            isUpdateInProgress = true
            theta = angleThetaDegrees * degreesToRadian
            val alpha = angleAlphaDegrees * degreesToRadian
            val a = cos(alpha);  val c = cos(theta)
            val b = sqrt(1 - a * a - c * c)  // Expressed from: cos²(alpha) + cos²(beta) + cos²(gamma) = 1
            x = magnitude * a
            y = magnitude * b
            z = magnitude * c
            isUpdateInProgress = false
            updatePolarCoordinates()  // To update phi as well.
        }

        /** Initializes the 3D vector with cartesian coordinates [x]/[y]/[z].
         */
        constructor(x: Double, y: Double, z: Double) {
            isUpdateInProgress = true
            this.x = x;  this.y = y;  this.z = z
            isUpdateInProgress = false
            updatePolarCoordinates()
        }

        /** Initializes the 3D vector in polar form [magnitude]/[theta]/[phi]:
         *  The angles demanded IN DEGREES (as integers) though: [thetaDegrees]/[phiDegrees]
         */
        constructor(magnitude: Double, thetaDegrees: Int, phiDegrees: Int) {
            isUpdateInProgress = true
            this.magnitude = magnitude
            theta = thetaDegrees * degreesToRadian
            phi = phiDegrees * degreesToRadian
            isUpdateInProgress = false
            updateCartesianCoordinates()
        }

        /** Initializes the 3D vector by copying the given vector [vector3D].
         */
        constructor(vector3D: Vector3D) {
            isUpdateInProgress = true
            x = vector3D.x;  y = vector3D.y;  z = vector3D.z
            isUpdateInProgress = false
            updatePolarCoordinates()
        }

        /** Update vector's magnitude & the angles using its current [x]/[y]/[z].
         */
        private fun updatePolarCoordinates() {
            if (!isUpdateInProgress) {
                isUpdateInProgress = true
                magnitude = sqrt(x * x + y * y + z * z)
                phi = acos(x / sqrt(x * x + y * y))
                theta = acos(z / magnitude)
                isUpdateInProgress = false
            }
        }

        /** Update vector's [x]/[y]/[z] coordinates using its current [magnitude]/[phi]/[theta].
         */
        private fun updateCartesianCoordinates() {
            if (!isUpdateInProgress) {
                isUpdateInProgress = true
                x = magnitude * sin(theta) * cos(phi)
                y = magnitude * sin(theta) * sin(phi)
                z = magnitude * cos(theta)
                isUpdateInProgress = false
            }
        }

        /** Adds the given second vector [vector2nd] to the vector.
         */
        fun add(vector2nd: Vector3D) {
            isUpdateInProgress = true
            x += vector2nd.x
            y += vector2nd.y
            z += vector2nd.z
            isUpdateInProgress = false
            updatePolarCoordinates()
        }
    }  // class Vector3D
}  // class MyMath

/** Returns the sinus of the given angle [angleDegrees].
 *  Also usable for returning the depth of x- and y-Axes values
 *  with [angleDegrees] = viewAngleAxisX or viewAngleAxisY.
 */
fun sinDegrees(angleDegrees: Double) = sin(angleDegrees * Math.PI / 180.0)

/** Returns the cosine of the given angle [angleDegrees].
 *  Also usable for returning the depth of z-Axis values
 *  with [angleDegrees] = viewAngleAxisZ.
 */
fun cosDegrees(angleDegrees: Double) = cos(angleDegrees * Math.PI / 180.0)
