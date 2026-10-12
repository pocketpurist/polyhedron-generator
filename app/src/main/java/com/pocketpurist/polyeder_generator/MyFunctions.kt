package com.pocketpurist.polyeder_generator

import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.EditText
import androidx.core.text.isDigitsOnly

/** Various useful functions.
 */

/** Generate "color flare effect" for the view [view]: From original color to desired end color
 * [colorEnd] and back (reversing the animation).
 */
fun doViewColorFlare(view: View, colorEnd: Int, durationMilliseconds: Long = 600) {
    val colorStart = 0xFFFFFFFF.toInt()
    // nStartColor = button.getSolidColor();
    val colorAnim: ValueAnimator = ObjectAnimator.ofInt(view, "backgroundColor", colorStart, colorEnd)
    colorAnim.duration = durationMilliseconds
    colorAnim.setEvaluator(ArgbEvaluator())
    colorAnim.repeatCount = 1
    colorAnim.repeatMode = ValueAnimator.REVERSE
    colorAnim.start()
}

/** Returns true if the content of the EditText view [editText] contains a valid number
 *  only consisting of digits (NOT being empty!).
 */
fun hasNotEmptyDigitsOnlyText(editText: EditText): Boolean {
    val string = editText.text.toString()
    return string.isNotEmpty() && string.isDigitsOnly()
}

/** Hides the back button of the ActionBar.
 */
fun hideActionBarBackButton() {
    myActionBar?.setHomeButtonEnabled(false)       // Disable the button
    myActionBar?.setDisplayHomeAsUpEnabled(false)  // Remove the left caret
    myActionBar?.setDisplayShowHomeEnabled(false)  // Remove the icon
}

/** TODO delete when debugging done.
 */
fun logEdgesUsageAndPolygons() {
    Handler(Looper.getMainLooper()).postDelayed({
        val ph = userInterface.polyhedron
        Log.d("logEdgesUsage&Polygons", "Polygons (${ph.polygons.collection.size}):")
        for (polygon in ph.polygons.collection) {
            var edgeCaptions = ""
            for (edge in polygon.edges) {
                if (edgeCaptions.isNotEmpty()) edgeCaptions += "; "
                edgeCaptions += ph.edges.getCaption(edge)
            }
            Log.d("logEdgesUsage&Polygons", "Polygon: $edgeCaptions")
        }
        Log.d("logEdgesUsage&Polygons", "- - - - - - - - - - - - - - - - - -")
        for ((index, edge) in ph.edges.singleEdges.withIndex()) {
            Log.d("logEdgesUsage&Polygons", "${ph.edges.getCaption(edge)} used in: ${ph.edges.singleEdges[index].countPolygonsItIsPartOf} polygons")
        }
        //logEdgesUsageAndPolygons()
    },1000*20)
}

/** Returns a Float formatted as a String with [digits] decimals.
 *  Returns its rounded integer value as a String for [digits] <= 0.
 */
fun Float.format(digits: Int = 2) = if (digits > 0) "%.${digits}f".format(this) else "${(this + 0.5f).toInt()}"