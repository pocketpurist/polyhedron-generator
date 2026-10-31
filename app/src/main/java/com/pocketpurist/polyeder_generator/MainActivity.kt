package com.pocketpurist.polyeder_generator

import android.app.AlertDialog
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.ImageSpan
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.marginLeft
import androidx.core.view.marginTop
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.snackbar.Snackbar
import com.pocketpurist.polyeder_generator.databinding.ActivityMainBinding

/***************************************************************************************************
 * Creating polyhedrons (within a skew coordinate system) by specifying points & edges
 * (and in future: face colors if desired).
 * Started on:  2023-10-23
 * as: Basic Views Activity
 *     Name:  Polyeder-Generator
 *     Package name:  com.pocketpurist.polyeder_generator
 *     Minimum SDK: API 16 (4.1)
 * Axis colors:  X: Red / Y: Green / Z: Blue
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * FIXME
 *  o) Search whole project for "in edges" & "in singleEdges": For occurrences where it is falsely
 *     assumed that the edges' points are sorted in a way so you could step through some edges to
 *     always get a "not already taken point" with picking point1.
 *     => Use Edge:getFirstPointNotInList(listPoints3D: List<-Point3D>): Point3D
 *     => and/or: Polygon:getPointAfter(point3D: Point3D): Point3D
 *  o) (possibly) isEncasingPoint and (certainly) isInFrontOfPoint failing.
 *     [Debugging - All edges of G17 without frontal intersection that are enclosed by at least 1 polygon]:
 *       Points along edge D-E are enclosed by polygons 1 times in total.
 *       Points along edge F-G are enclosed by polygons 1 times in total.
 *       Points along edge H-I are enclosed by polygons 1 times in total.
 *       Points along edge I-J are enclosed by polygons 1 times in total.
 *       Points along edge J-K are enclosed by polygons 1 times in total.
 *       Points along edge K-L are enclosed by polygons 2 times in total.
 *       Points along edge L-M are enclosed by polygons 1 times in total.
 *       Points along edge O-P are enclosed by polygons 2 times in total.
 *       Points along edge H-P are enclosed by polygons 1 times in total.
 *       Points along edge Q-R are enclosed by polygons 1 times in total.
 *       Points along edge R-S are enclosed by polygons 1 times in total.
 *       Points along edge H-R are enclosed by polygons 1 times in total.
 *       Points along edge O-T are enclosed by polygons 1 times in total.
 *       Points along edge B-I are enclosed by polygons 1 times in total.
 *       Points along edge C-J are enclosed by polygons 1 times in total.
 *       Points along edge D-K are enclosed by polygons 2 times in total.
 *       Points along edge E-L are enclosed by polygons 2 times in total.  !! HAS 1 frontal intersection !!
 *       Points along edge G-N are enclosed by polygons 1 times in total.
 *       -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -  -
 *       Coordinates of point G = 3,91/-0,83
 *       Coordinates of polygon E-L-M-F = [2,90]/[0,13]-[2,90]/[-1,87]-[4,85]/[-0,29]-[2,90]/[0,13]
 *       Point G is encased by polygon E-L-M-F:  true
 * TODO
 *  -> for README.md - simple demonstration polyhedron:
 *     1 / 2 / 1
 *     2 / 1 / 2
 *     3 / 1 / 1
 *     3 / 1 / 2
 *     2 / 2 / 2
 *     3 / 2 / 1
 *     0 / 1 / 0
 *     maybe the 8th edge:
 *     1 / 2 / 0
 *  -> Visibility of (parts of) edges:  Try "Point on canvas with depth info" approach.
 *  o) "(automatically) Reduce Edges" feature (considering to remove points and join edges afterwards!):
 *     Find out for every 2 (combinations of) polygons located on the same plane: if they share
 *     an edge => edge to delete.
 *     Consider to delete edges that are not integrated in a closed path (having one or both
 *     points open) too.
 *     Automated Point-Reduction: Points that are located on "one straight line" and either
 *     part of exactly 2 edges or "2 or less edges".
 *  o) Input dialogs/features:
 *     ·) Axes "compression factor". (Consider: Auto-Values.)
 *     ·) Face (polygon) colors selector.
 *  o) Consider "auto compression factor" according to the chosen (relations of) axis angles.
 *  o) Axis Angles Input: Consider showing a "3 axes indicator"
 *  o) Set of rules for "menu option items" enablement -
 *     e.g.: "add edge" only enabled when at least 2 points are available.
 *  o) Visibility of disabled menu items. For example greyed background.
 *  o) AddPointDialog(): implement "Point Caption".
 *  o) Edge input bar: Position correctly if lowest point is too low on screen for bar fitting below.
 *  o) Implement labeling (possibly headline) option for the current drawing.
 *  o) Draw/print:
 *     ·) Point captions.
 *     ·) Ruler lines ... with the maximum complexity of: An "axis cage" (see Raumgeometrie book).
 *  o) "Snapshot to clipboard" (Screenshot) feature: Via FAB.
 *  o) Implement polyhedron statistics (info page): Point & edge count.
 *  o) Storage data type: Consider established vector format.
 *  o) Consider a setting "showPointOverEdges": To turn off drawing points even though it is part
 *     of (at least) one edge.
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 * KNOW-HOW:
 * o) Pushed to GitHub by obeying the following instructions:
 *    https://www.youtube.com/watch?v=pwyu3BxtI2M
 * o) Always round down / up:  floor(dValue) / ceil(dValue)  // Should work with negative values too.
 * o) Show menu items with icon AND caption - see in code: menu.add( ... menuIconWithText(...) ...)
 * o) "Show only digits in virtual keyboard" for EditText views:
 *    android:digits="0123456789"
 *    android:inputType="number"
 * o) Hide Soft Keyboard:  Just disable all EditText views... (.isEnabled = false)
 * o) Handle <Enter> key on Soft Keyboard:
 *    editText.setonKeyListener( { _, keyCode, event ->
 *        if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {}  }
 * o) Handle "text change" for EditText views - use following listener:
 *    .addTextChangedListener(object : TextWatcher {
 *         override fun afterTextChanged(s: Editable) { ... } }
 * o) Implementing "custom input view" to register moving finger over it: Using ImageButton an
 *    checking for MotionEvent.ACTION_DOWN or MotionEvent.ACTION_MOVE within setOnTouchListener.
 ***************************************************************************************************/

var myActionBar: ActionBar? = null
var sharedPreferences: SharedPreferences? = null
var nCanvasWidthPixel = 0;  var nCanvasHeightPixel = 0
var pixelsPerDP = 2f
val userInterface = PolyhedronCanvas.UserInterface()

class MainActivity : AppCompatActivity() {
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    private lateinit var pointColorFlare: PointColorFlare
    private lateinit var addPointDialog: AddPointDialog
    private lateinit var setAxisAngles: SetAxisAngles
    private lateinit var addEdgeFeature: AddEdgeFeature
    //private val menuItemIdNewCanvas = 110
    //private val menuItemIdAddPoint = 120
    private val menuItemIdSetAxisAngles = 200
    private val menuItemIdSettings = 300
    private val menuItemIDs = mutableListOf<Int>() // List of all option menu item IDs created via menu_main.xml PLUS those in onCreateOptionsMenu(...)
    private val menuItems = mutableListOf<MenuItem>()  // List of all option menu items themseves.

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        myActionBar = supportActionBar

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        sharedPreferences = getSharedPreferences("PolyederGenerator", MODE_PRIVATE)

        // TODO implement screenshot:
        binding.fab.setOnClickListener {
            //Snackbar.make(view, "[Screenshot Feature noch nicht implementiert...]", Snackbar.LENGTH_LONG)
            //    .setAction("Action", null).show()
            val dFact = userInterface.generalSettings.depthFactor
            // =============== Debugging: Try to correct depth relation failure (stepping through some factors). TODO delete when debugging done. ===============
            // =============== Purpose: Search for a factor factorDepthX vs. Y (range) where fragment visibility transitions happen.              ===============
            val corrFactors = listOf(0.00001, 0.0001, 0.001, 0.01, 0.1, 0.4, 0.8, 1.0, 1.2, 1.4, 1.8, 2.0, 5.0, 10.0, 100.0, 1000.0, 10000.0, 100000.0, 1000000.0)
            for (corrFactor in corrFactors) {
                val edges = userInterface.polyhedron.edges
                val edgesRecent = edges.getCopy()
                val factorRecentCorrection = dFact.correctionFactorXvsY
                dFact.correctionFactorXvsY = corrFactor
                dFact.update()
                userInterface.polyhedron.polyhedronOnCanvas.update()  // To update points' depths.
                //Log.d("fab.onClickListener", "generalSettings.depthFactor.correctionFactorXvsY = ${corrFactor}")
                //Log.d("fab.onClickListener", "generalSettings.depthFactor.axisX / axisY / axisZ = ${dFact.axisX} / ${dFact.axisY} / ${dFact.axisZ}")

                //createG17Edges()
                createG13Edges()
                updateCanvas()
                // Comparison of "edge fragment visibility patterns" of edges vs. edgesRecent:
                for (edge in edges.singleEdges) {
                    val edgeRecently = edgesRecent.getOneByItsPoints(edge.point1, edge.point2)
                    if (!edgeRecently.isDummy() && !edge.hasSameFragmentVisibilitySequence(edgeRecently)) {
                        val patternRecently = edgeRecently.getFragmentVisibilityPattern()
                        val patternNow = edge.getFragmentVisibilityPattern()
                        //Log.d("fab.onClickListener", "change @ Fact $factorRecentCorrection -> $corrFactor: '${edge.getCaption()}' from '$patternRecently' -> '$patternNow'")
                    }
                }

                //testPointGisEncasedByPolygonEFML()
            }
        }

        pointColorFlare = PointColorFlare()
        addPointDialog = AddPointDialog()
        setAxisAngles = SetAxisAngles()
        addEdgeFeature = AddEdgeFeature()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)

        // Add menu items with icon AND caption:
        menu.add(0,menuItemIdSetAxisAngles,70, menuIconWithText(resources.getDrawable(R.drawable.symbol_3d_rotation), resources.getString(R.string.action_set_axes)))
        var menuItem: MenuItem = menu.add(0,menuItemIdSettings,99, menuIconWithText(resources.getDrawable(R.drawable.symbol_settings), resources.getString(R.string.action_settings)))
        menuItem.isEnabled = false
        // Add all of the desired(!) menu items IDs to a list:
        menuItemIDs.add(R.id.action_add_point)
        menuItemIDs.add(R.id.action_add_edge)
        menuItemIDs.add(R.id.action_new_canvas)
        menuItemIDs.add(menuItemIdSetAxisAngles)
        menuItemIDs.add(menuItemIdSettings)
        // Add all of the desired(!) menu items (themselves) to a list:
        for (itemIndex in 0 until menu.size()) {
            menuItem = menu.getItem(itemIndex)
            for (itemIdIndex in menuItemIDs) {
                if (menuItem.itemId == itemIdIndex) {
                    menuItems.add(menuItem)
                    break
                }
            }
        }
        //menu.getItem(R.id.action_add_point)
        //menuItem = menu.add(0,menuItemIdNewCanvas,1, menuIconWithText(resources.getDrawable(R.drawable.symbol_new), resources.getString(R.string.action_new_canvas)))
        //menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        //menuItem = menu.add(0,menuItemIdAddPoint,1, menuIconWithText(resources.getDrawable(R.drawable.symbol_add_point), resources.getString(R.string.action_add_point)))
        //menuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_add_point -> {
                addPointDialog.show()
                true
            }
            R.id.action_add_edge -> {
                addEdgeFeature.show()
                true
            }
            R.id.action_new_canvas -> {
                val builder = AlertDialog.Builder(this)
                builder.setCancelable(false)
                builder.setNegativeButton(R.string.command_no) { _, _ -> }
                builder.setTitle(resources.getString(R.string.question_dump_canvas))
                builder.setIcon(R.drawable.symbol_new)
                builder.setMessage(resources.getString(R.string.question_dump_canvas_starting_new))
                builder.setPositiveButton(R.string.command_yes) { _, _ ->
                    userInterface.polyhedron.reset()
                    updateCanvas()
                }
                val alert: AlertDialog = builder.create()
                alert.show()  // Show the dialog
                true
            }
            menuItemIdSetAxisAngles -> {
                setAxisAngles.show()
                true
            }
            menuItemIdSettings /* R.id.action_settings */ -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        acquireCanvasDimensionsAndUpdateIt()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    @Deprecated("Deprecated in Java")
    /** Handling "device's back button pressed": Immediately exit the app (or cancel a currently
     *  displayed input dialog).
     */
    override fun onBackPressed() {
        // super.onBackPressed()  // Commented => original functionality disabled.
        if (addPointDialog.isShown) {
            addPointDialog.hide()
        } else if (setAxisAngles.isShown) {
            setAxisAngles.hide()
        } else if (addEdgeFeature.isShown) {
            addEdgeFeature.hide()
        } else {
            if (userInterface.polyhedron.points.size == 0) {
                finish()
            } else {
                val builder = AlertDialog.Builder(this)
                builder.setCancelable(false)
                builder.setNegativeButton(R.string.command_no) { _, _ -> }
                builder.setTitle(resources.getString(R.string.question_dump_canvas))
                builder.setIcon(R.drawable.symbol_exit)
                builder.setMessage(resources.getString(R.string.question_exit_app_without_saving))
                builder.setPositiveButton(R.string.command_yes) { _, _ ->
                    finish()
                }
                val alert: AlertDialog = builder.create()
                alert.show()  // Show the dialog
            }
        }
    }

    private fun menuIconWithText(r: Drawable, title: String): CharSequence {
        r.setBounds(0, 0, r.intrinsicWidth, r.intrinsicHeight)
        val sb = SpannableString("    " + title)
        val imageSpan = ImageSpan(r, ImageSpan.ALIGN_BOTTOM)
        sb.setSpan(imageSpan, 0, 1, SpannableString.SPAN_EXCLUSIVE_EXCLUSIVE)
        return sb
    }

    /** Enable/disable all items of the options menu: According to [isToSetEnabled].
     */
    private fun menuItemsEnabled(isToSetEnabled: Boolean) {
        for (menuItem in menuItems) {
            if (menuItem.itemId != menuItemIdSettings) menuItem.isEnabled = isToSetEnabled  // Leave out the item "Settings"  // TODO include "settings" when needed!
        }
    }

    private fun acquireCanvasDimensionsAndUpdateIt() {
        /** Convert dp value into pixels. [dpValue]: Density-independent Pixels value.
         * Returns its value in pixels.
         */
        fun pixelValue(dpValue: Float) = resources.displayMetrics.density * dpValue

        val fragment = findViewById<FragmentContainerView>(R.id.nav_host_fragment_content_main)
        if (nCanvasHeightPixel <= 0 && fragment != null) {
            if (fragment.width > 0) nCanvasWidthPixel = fragment.width
            if (fragment.height > 0) nCanvasHeightPixel = fragment.height
            pixelsPerDP = pixelValue(1f)
            createTestPolyhedron()
            updateCanvas()
        }
    }

    private fun updateCanvas() {
        if (isCanvasAvailable()) {
            val bitmap = Bitmap.createBitmap(nCanvasWidthPixel, nCanvasHeightPixel, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            findViewById<ImageView>(R.id.img_view_canvas)?.setImageBitmap(bitmap)
            userInterface.update(canvas)

            //logFragmentCountOfEdgeAG()
        }
    }

    /** Controlling the "add point dialog".
     */
    private inner class AddPointDialog {
        var isShown = false
        var relLayoutPointInput: RelativeLayout?
        var editTextX: EditText?
        var editTextY: EditText?
        var editTextZ: EditText?
        val editTextsXYZ = mutableListOf<EditText?>()
        var editTextDummy: EditText?
        var buttonAdd: Button?
        var seekBar: SeekBar?
        var areListenersSet = false  // true if the needed view listeners are set.
        val maxCoordinateVal = userInterface.generalSettings.rangeCoordinates.last

        init {
            relLayoutPointInput = findViewById(R.id.rel_layout_point_input)
            editTextX = findViewById(R.id.edit_text_x_value)
            editTextY = findViewById(R.id.edit_text_y_value)
            editTextZ = findViewById(R.id.edit_text_z_value)
            editTextDummy = findViewById(R.id.edit_text_dummy)
            buttonAdd = findViewById(R.id.button_add_point)
            seekBar = findViewById(R.id.seek_bar_value)
            relLayoutPointInput?.visibility = View.GONE
        }

        private fun setListeners() {
            if (relLayoutPointInput == null) {
                relLayoutPointInput = findViewById(R.id.rel_layout_point_input)
                editTextX = findViewById(R.id.edit_text_x_value)
                editTextY = findViewById(R.id.edit_text_y_value)
                editTextZ = findViewById(R.id.edit_text_z_value)
                editTextDummy = findViewById(R.id.edit_text_dummy)
                buttonAdd = findViewById(R.id.button_add_point)
                seekBar = findViewById(R.id.seek_bar_value)
            }

            /** Returns true if the 3 views editTextX/Y/Z contain a valid 3D position
             *  for a point that isn't already created.
             */
            fun isAddablePointInEditTextsXYZ(): Boolean {
                var numberOfValidCoordinateValues = 0  // Counted if in 0 .. maxCoordinateVal
                var isAddPointPossible = false
                for (editText in editTextsXYZ) {
                    if (isContainingValidPointCoordinate(editText)) numberOfValidCoordinateValues++ else break
                }
                if (numberOfValidCoordinateValues == 3) {
                    val xValue = editTextX?.text.toString().toInt()
                    val yValue = editTextY?.text.toString().toInt()
                    val zValue = editTextZ?.text.toString().toInt()
                    isAddPointPossible = !userInterface.polyhedron.doesIntegerPointExist(xValue, yValue, zValue)
                }
                return isAddPointPossible
            }

            val lambdaEditTextOnFocusChange: (View, Boolean) -> Unit = { view, hasFocus ->
                if (hasFocus) {
                    if (!hasNotEmptyDigitsOnlyText(view as EditText)) {  // Set text to "0" if invalid: not "digits only text".
                        view.setText("0")
                        view.setSelection(view.length())
                        buttonAdd?.isEnabled = isAddablePointInEditTextsXYZ()
                    }
                    val value = view.text.toString().toInt()
                    seekBar?.progress = if (value in 0..maxCoordinateVal) value else maxCoordinateVal
                    seekBar?.visibility = View.VISIBLE
                } else {
                    seekBar?.visibility = View.GONE
                }
            }

            if (relLayoutPointInput != null) {
                relLayoutPointInput?.setOnTouchListener { _, _ -> true }  // Returning true prevents touch event for canvas "under the point input".

                editTextsXYZ.add(editTextX); editTextsXYZ.add(editTextY); editTextsXYZ.add(editTextZ)
                editTextX?.setOnFocusChangeListener(lambdaEditTextOnFocusChange)
                editTextY?.setOnFocusChangeListener(lambdaEditTextOnFocusChange)
                editTextZ?.setOnFocusChangeListener(lambdaEditTextOnFocusChange)

                editTextX?.setOnKeyListener { view, keyCode, event  ->
                    var booleanReturn = false
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        if (keyCode == KeyEvent.KEYCODE_ENTER) {
                            if (isContainingValidPointCoordinate(view as EditText)) {
                                editTextY?.setSelection(editTextY!!.length())
                                editTextY?.requestFocus()
                            }
                            booleanReturn = true
                        }
                    }
                    booleanReturn
                }
                editTextY?.setOnKeyListener { view, keyCode, event ->
                    var booleanReturn = false
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        if (keyCode == KeyEvent.KEYCODE_ENTER) {
                            if (isContainingValidPointCoordinate(view as EditText)) {
                                editTextZ?.setSelection(editTextZ!!.length())
                                editTextZ?.requestFocus()
                            }
                            booleanReturn = true
                        }
                    }
                    booleanReturn
                }
                editTextZ?.setOnKeyListener {view, keyCode, event ->
                    var booleanReturn = false
                    val gotAdded: Boolean
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        if (keyCode == KeyEvent.KEYCODE_ENTER) {
                            gotAdded = addIfValid(view)
                            if (gotAdded) hide()
                            booleanReturn = true
                        }
                    }
                    booleanReturn
                }

                editTextX?.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) { }
                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int ) { }
                    override fun afterTextChanged(s: Editable) {
                        buttonAdd?.isEnabled = isAddablePointInEditTextsXYZ()
                        if (isContainingValidPointCoordinate(editTextX)) seekBar?.progress = s.toString().toInt()
                    }
                })
                editTextY?.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) { }
                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int ) { }
                    override fun afterTextChanged(s: Editable) {
                        buttonAdd?.isEnabled = isAddablePointInEditTextsXYZ()
                        if (isContainingValidPointCoordinate(editTextY)) seekBar?.progress = s.toString().toInt()
                    }
                })
                editTextZ?.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) { }
                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int ) { }
                    override fun afterTextChanged(s: Editable) {
                        buttonAdd?.isEnabled = isAddablePointInEditTextsXYZ()
                        if (isContainingValidPointCoordinate(editTextZ)) seekBar?.progress = s.toString().toInt()
                    }
                })

                seekBar?.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener{
                    override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                        if (fromUser)
                            for (editText in editTextsXYZ) if (editText!!.hasFocus()) {
                                editText.setText(progress.toString())
                                editText.setSelection(editText.length())
                            }
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar) {}

                    override fun onStopTrackingTouch(seekBar: SeekBar) {
                        buttonAdd?.isEnabled = isAddablePointInEditTextsXYZ()
                    }
                })
                seekBar?.max = maxCoordinateVal

                buttonAdd?.setOnClickListener { view ->
                    addIfValid(view)
                    hide()
                }
                areListenersSet = true
            }
        }

        private fun setCanvasListener() {
            val imgViewCanvas = findViewById<ImageView>(R.id.img_view_canvas)
            imgViewCanvas?.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) hide()  // Abort point input.
                false
            }
        }

        /** Sets OnTouchListener of canvas ImageView to null.
         */
        private fun dumpCanvasListener() {
            val imgViewCanvas = findViewById<ImageView>(R.id.img_view_canvas)
            imgViewCanvas?.setOnTouchListener(null)
        }

        fun show() {
            if (!areListenersSet) setListeners()
            if (relLayoutPointInput != null) {
                val xValue = userInterface.polyhedron.lowestFreePositiveIntegerCoordinateX()
                val yValue = userInterface.polyhedron.lowestFreePositiveIntegerCoordinateY()
                val zValue = userInterface.polyhedron.lowestFreePositiveIntegerCoordinateZ()
                editTextViewsEnabled(true)
                editTextX?.setText(xValue.toString())
                editTextY?.setText(yValue.toString())
                editTextZ?.setText(zValue.toString())
                buttonAdd?.isEnabled = true
                editTextDummy?.requestFocus()
                seekBar?.visibility = View.GONE
                menuItemsEnabled(false)
                // Include new point's number in label "add point" (starting with "#1"):
                val txtViewCaptionAddPoint = findViewById<TextView>(R.id.txt_view_caption_add_point)
                if (txtViewCaptionAddPoint != null) {
                    var stringPoint = resources.getString(R.string.point)
                    var stringAddPoint = resources.getString(R.string.action_add_point)
                    val newPointNumber = userInterface.polyhedron.points.size + 1
                    stringAddPoint = stringAddPoint.replace(stringPoint, "$stringPoint #$newPointNumber")
                    stringPoint = stringPoint.lowercase()  // To cover "lower case languages" with next command:
                    stringAddPoint = stringAddPoint.replace(stringPoint, "$stringPoint #$newPointNumber")
                    txtViewCaptionAddPoint.text = stringAddPoint
                }
                setCanvasListener()
                relLayoutPointInput?.visibility = View.VISIBLE
                isShown = true
            }
        }

        fun hide() {
            isShown = false
            dumpCanvasListener()
            relLayoutPointInput?.visibility = View.GONE
            editTextViewsEnabled(false)  // For ensuring that no on screen keyboard stays visible.
            menuItemsEnabled(true)
        }

        /** Returns true if the given view [editText] contains a valid point coordinate.
         */
        private fun isContainingValidPointCoordinate(editText: EditText?): Boolean {
            var isValid = false
            if (editText != null) {
                val stringValue = editText.text.toString()
                isValid = hasNotEmptyDigitsOnlyText(editText) && stringValue.toInt() in 0..maxCoordinateVal
            }
            return isValid
        }

        /** Adds the current dialog's point entry if valid (and not already existing) to the polyhedron.
         *  Otherwise: Let an error message appear on bottom of the screen.
         *  Returns true if that currently entered point gets added.
         */
        private fun addIfValid(view: View): Boolean {
            var gotAdded = false
            var numberOfValidCoordinateValues = 0  // Counted if in 0 .. maxCoordinateVal
            for (editText in editTextsXYZ) {
                if (isContainingValidPointCoordinate(editText)) numberOfValidCoordinateValues++ else break
            }
            if (numberOfValidCoordinateValues == 3) {
                val xValue = editTextX?.text.toString().toInt()
                val yValue = editTextY?.text.toString().toInt()
                val zValue = editTextZ?.text.toString().toInt()
                if (userInterface.polyhedron.doesIntegerPointExist(xValue, yValue, zValue)) {
                    Snackbar.make(view, resources.getText(R.string.message_point_already_existing), Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show()
                } else {
                    userInterface.polyhedron.addPoint(xValue, yValue, zValue)
                    updateCanvas()
                    gotAdded = true
                }
            } else {
                Snackbar.make(view, resources.getText(R.string.message_point_input_invalid), Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
            return gotAdded
        }

        /** Enable/disable all EditText views: According to [isToSetEnabled].
         *  (If successfully disabled: Soft Keyboard gets hidden.)
         */
        private fun editTextViewsEnabled(isToSetEnabled: Boolean) {
            editTextDummy?.isEnabled = isToSetEnabled
            editTextX?.isEnabled = isToSetEnabled
            editTextY?.isEnabled = isToSetEnabled
            editTextZ?.isEnabled = isToSetEnabled
        }
    }  // private inner class AddPointDialog

    /** Controlling the "set axis angles dialog" (Axis angles input).
     */
    private inner class SetAxisAngles {
        val angleDefault = 0
        val rangeValidAngles = 0..359  // Range of valid integer angles.
        var isShown = false
        var relLayoutAxisAnglesInput: RelativeLayout?
        var editTextXAxisAngle: EditText?
        var editTextYAxisAngle: EditText?
        var editTextZAxisAngle: EditText?
        val editTextsXYZ = mutableListOf<EditText?>()
        var editTextDummy2: EditText?
        var buttonSetAxisAngles: Button?
        var areListenersSet = false  // true if the needed view listeners are set.
        val angleIndicator: AngleIndicator

        init {
            relLayoutAxisAnglesInput = findViewById(R.id.rel_layout_axis_angles_input)
            editTextXAxisAngle = findViewById(R.id.edit_text_x_axis_angle)
            editTextYAxisAngle = findViewById(R.id.edit_text_y_axis_angle)
            editTextZAxisAngle = findViewById(R.id.edit_text_z_axis_angle)
            editTextDummy2 = findViewById(R.id.edit_text_dummy_2)
            buttonSetAxisAngles = findViewById(R.id.button_set_axis_angles)
            angleIndicator = AngleIndicator()
            relLayoutAxisAnglesInput?.visibility = View.GONE
        }

        private fun setListeners() {
            if (relLayoutAxisAnglesInput == null) {
                relLayoutAxisAnglesInput = findViewById(R.id.rel_layout_axis_angles_input)
                editTextXAxisAngle = findViewById(R.id.edit_text_x_axis_angle)
                editTextYAxisAngle = findViewById(R.id.edit_text_y_axis_angle)
                editTextZAxisAngle = findViewById(R.id.edit_text_z_axis_angle)
                editTextDummy2 = findViewById(R.id.edit_text_dummy_2)
                buttonSetAxisAngles = findViewById(R.id.button_set_axis_angles)
            }

            /** Returns true if the 3 views editText[X/Y/Z]AxisAngle contain valid angles.
             */
            fun areValidAnglesInEditTextsXYZ(): Boolean {
                var numberOfValidAngleValues = 0  // Counted if in rangeValidAngles
                for (editText in editTextsXYZ) {
                    if (isContainingValidAngle(editText)) numberOfValidAngleValues++ else break
                }
                return numberOfValidAngleValues == 3
            }

            val lambdaEditTextOnFocusChange: (View, Boolean) -> Unit = { view, hasFocus ->
                if (hasFocus) {
                    if (!hasNotEmptyDigitsOnlyText(view as EditText)) {  // Set text to the default angle if invalid: not "digits only text".
                        view.setText("$angleDefault")
                        view.setSelection(view.length())
                        buttonSetAxisAngles?.isEnabled = areValidAnglesInEditTextsXYZ()
                    }
                    angleIndicator.show()
                } else {
                    angleIndicator.hide()
                }
            }

            if (relLayoutAxisAnglesInput != null) {
                relLayoutAxisAnglesInput?.setOnTouchListener { _, _ -> true }  // Returning true prevents touch event for canvas "under the point input".

                editTextsXYZ.add(editTextXAxisAngle); editTextsXYZ.add(editTextYAxisAngle); editTextsXYZ.add(editTextZAxisAngle)
                editTextXAxisAngle?.setOnFocusChangeListener(lambdaEditTextOnFocusChange)
                editTextYAxisAngle?.setOnFocusChangeListener(lambdaEditTextOnFocusChange)
                editTextZAxisAngle?.setOnFocusChangeListener(lambdaEditTextOnFocusChange)

                editTextXAxisAngle?.setOnKeyListener { view, keyCode, event  ->
                    var booleanReturn = false
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        if (keyCode == KeyEvent.KEYCODE_ENTER) {
                            if (isContainingValidAngle(view as EditText)) {
                                editTextYAxisAngle?.setSelection(editTextYAxisAngle!!.length())
                                editTextYAxisAngle?.requestFocus()
                            }
                            booleanReturn = true
                        }
                    }
                    booleanReturn
                }
                editTextYAxisAngle?.setOnKeyListener { view, keyCode, event ->
                    var booleanReturn = false
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        if (keyCode == KeyEvent.KEYCODE_ENTER) {
                            if (isContainingValidAngle(view as EditText)) {
                                editTextZAxisAngle?.setSelection(editTextZAxisAngle!!.length())
                                editTextZAxisAngle?.requestFocus()
                            }
                            booleanReturn = true
                        }
                    }
                    booleanReturn
                }
                editTextZAxisAngle?.setOnKeyListener {view, keyCode, event ->
                    var booleanReturn = false
                    val gotAdded: Boolean
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        if (keyCode == KeyEvent.KEYCODE_ENTER) {
                            gotAdded = setIfValid(view)
                            if (gotAdded) hide()
                            booleanReturn = true
                        }
                    }
                    booleanReturn
                }

                editTextXAxisAngle?.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) { }
                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int ) { }
                    override fun afterTextChanged(s: Editable) {
                        buttonSetAxisAngles?.isEnabled = areValidAnglesInEditTextsXYZ()
                        if (isContainingValidAngle(editTextXAxisAngle)) angleIndicator.update()
                    }
                })
                editTextYAxisAngle?.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) { }
                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int ) { }
                    override fun afterTextChanged(s: Editable) {
                        buttonSetAxisAngles?.isEnabled = areValidAnglesInEditTextsXYZ()
                        if (isContainingValidAngle(editTextYAxisAngle)) angleIndicator.update()
                    }
                })
                editTextZAxisAngle?.addTextChangedListener(object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) { }
                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int ) { }
                    override fun afterTextChanged(s: Editable) {
                        buttonSetAxisAngles?.isEnabled = areValidAnglesInEditTextsXYZ()
                        if (isContainingValidAngle(editTextZAxisAngle)) angleIndicator.update()
                    }
                })

                buttonSetAxisAngles?.setOnClickListener { view ->
                    setIfValid(view)
                    hide()
                }
                areListenersSet = true
            }
        }

        private fun setCanvasListener() {
            val imgViewCanvas = findViewById<ImageView>(R.id.img_view_canvas)
            imgViewCanvas?.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_DOWN) hide()  // Abort axis angles input.
                false
            }
        }

        /** Sets OnTouchListener of canvas ImageView to null.
         */
        private fun dumpCanvasListener() {
            val imgViewCanvas = findViewById<ImageView>(R.id.img_view_canvas)
            imgViewCanvas?.setOnTouchListener(null)
        }

        fun show() {
            if (!areListenersSet) setListeners()
            if (relLayoutAxisAnglesInput != null) {
                val xValue = userInterface.generalSettings.viewAngleAxisX.toInt()
                val yValue = userInterface.generalSettings.viewAngleAxisY.toInt()
                val zValue = userInterface.generalSettings.viewAngleAxisZ.toInt()
                editTextViewsEnabled(true)
                editTextXAxisAngle?.setText(xValue.toString())
                editTextYAxisAngle?.setText(yValue.toString())
                editTextZAxisAngle?.setText(zValue.toString())
                buttonSetAxisAngles?.isEnabled = true
                editTextDummy2?.requestFocus()
                angleIndicator.hide()
                menuItemsEnabled(false)
                setCanvasListener()
                relLayoutAxisAnglesInput?.visibility = View.VISIBLE
                isShown = true
            }
        }

        fun hide() {
            isShown = false
            dumpCanvasListener()
            relLayoutAxisAnglesInput?.visibility = View.GONE
            editTextViewsEnabled(false)  // For ensuring that no on screen keyboard stays visible.
            menuItemsEnabled(true)
        }

        /** Returns true if the given view [editText] contains a valid angle.
         */
        private fun isContainingValidAngle(editText: EditText?): Boolean {
            var isValid = false
            if (editText != null) {
                val stringValue = editText.text.toString()
                isValid = hasNotEmptyDigitsOnlyText(editText) && stringValue.toInt() in rangeValidAngles
            }
            return isValid
        }

        /** Set the current dialog's angle entries as the new x/y/z axis angles if valid.
         *  Otherwise: Let an error message appear on bottom of the screen.
         *  Returns true if set.
         */
        private fun setIfValid(view: View): Boolean {
            var gotAdded = false
            var numberOfValidAngleValues = 0  // Counted if in rangeValidAngles
            for (editText in editTextsXYZ) {
                if (isContainingValidAngle(editText)) numberOfValidAngleValues++ else break
            }
            if (numberOfValidAngleValues == 3) {
                val xValue = editTextXAxisAngle?.text.toString().toFloat()
                val yValue = editTextYAxisAngle?.text.toString().toFloat()
                val zValue = editTextZAxisAngle?.text.toString().toFloat()
                userInterface.generalSettings.viewAngleAxisX = xValue
                userInterface.generalSettings.viewAngleAxisY = yValue
                userInterface.generalSettings.viewAngleAxisZ = zValue
                userInterface.polyhedron.axisInfo.update()
                userInterface.polyhedron.polyhedronOnCanvas.update()
                userInterface.polyhedron.edges.updateVisibility()
                updateCanvas()
                gotAdded = true
            } else {  // FIXME replace with correct error message.
                Snackbar.make(view, resources.getText(R.string.message_point_input_invalid), Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
            }
            return gotAdded
        }

        /** Enable/disable all EditText views: According to [isToSetEnabled].
         *  (If successfully disabled: Soft Keyboard gets hidden.)
         */
        private fun editTextViewsEnabled(isToSetEnabled: Boolean) {
            editTextDummy2?.isEnabled = isToSetEnabled
            editTextXAxisAngle?.isEnabled = isToSetEnabled
            editTextYAxisAngle?.isEnabled = isToSetEnabled
            editTextZAxisAngle?.isEnabled = isToSetEnabled
        }

        /** Implements an indicator + graphical touch input "view" for angles.
         */
        private inner class AngleIndicator {
            var relLayoutAngleIndicator: RelativeLayout?
            var imgButtonAngleIndicator: ImageButton?
            var imgViewPointAngleIndicator: ImageView?
            var areListenersSet = false  // true if the needed view listeners are set.
            /** Center of Angle Indicator in pixel. [Float.NEGATIVE_INFINITY] if undefined.
             */
            var center: Float = Float.NEGATIVE_INFINITY
                get() = if (imgButtonAngleIndicator == null) Float.NEGATIVE_INFINITY else imgButtonAngleIndicator!!.layoutParams!!.width / 2f
                private set
            /** Radius of the "indicator point" in pixel. [Float.NEGATIVE_INFINITY] if undefined.
             */
            var radiusIndicatorPoint: Float = Float.NEGATIVE_INFINITY
                get() = if (imgViewPointAngleIndicator == null) Float.NEGATIVE_INFINITY else imgViewPointAngleIndicator!!.layoutParams!!.width / 2f
                private set
            /** Radius from indicator's center to center of "indicator point" in pixel. [Float.NEGATIVE_INFINITY] if undefined.
             */
            var radius: Float = Float.NEGATIVE_INFINITY
                get() = if (center > 0 && radiusIndicatorPoint > 0) center - radiusIndicatorPoint else Float.NEGATIVE_INFINITY
                private set
            init {
                relLayoutAngleIndicator = findViewById(R.id.rel_layout_angle_indicator)
                imgButtonAngleIndicator = findViewById(R.id.img_button_angle_indicator)
                imgViewPointAngleIndicator = findViewById(R.id.img_view_point_angle_indicator)
            }

            private fun setListeners() {
                relLayoutAngleIndicator = findViewById(R.id.rel_layout_angle_indicator)
                imgButtonAngleIndicator = findViewById(R.id.img_button_angle_indicator)
                imgViewPointAngleIndicator = findViewById(R.id.img_view_point_angle_indicator)

                if (imgButtonAngleIndicator != null) {
                    imgButtonAngleIndicator?.setOnTouchListener { _, event ->
                        when (event.action) {
                            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                                val xPosFromCenter = (event.getX() - center).toDouble()
                                val yPosFromCenter = (event.getY() - center).toDouble()
                                val vectorTouchPointFromCenter = MyMath.Vector(xPosFromCenter, yPosFromCenter)
                                val indexOfFocusedEditText = getIndexOfFocusedEditText()
                                if (indexOfFocusedEditText in 0 until editTextsXYZ.size) {
                                    val editText = editTextsXYZ[indexOfFocusedEditText]
                                    if (editText != null) {
                                        var angle = 360 - vectorTouchPointFromCenter.angle
                                        if (angle >= 360) angle -= 360
                                        editText.setText(angle.toString())
                                        update()
                                    }
                                }
                            }
                        }
                        false
                    }
                    areListenersSet = true
                }
            }

            /** Returns the index of the currently focused EditText view within editTextsXYZ.
             *  Returns -1 if none of the 3 views is the focused view.
             */
            private fun getIndexOfFocusedEditText(): Int {
                var indexCurrentlyFocused = -1
                for ((index, editText) in editTextsXYZ.withIndex()) {
                    if (editText != null && editText.isFocused) {
                        indexCurrentlyFocused = index
                        break
                    }
                }
                return indexCurrentlyFocused
            }

            /** Updates the indicator point according to the EditText view that is currently focused.
             */
            fun update() {
                var angleToTake = angleDefault
                val indexCurrentlyFocusedEditText = getIndexOfFocusedEditText()
                if (indexCurrentlyFocusedEditText in 0 until editTextsXYZ.size) {
                    val editText = editTextsXYZ[indexCurrentlyFocusedEditText]
                    if (editText != null && hasNotEmptyDigitsOnlyText(editText)) {
                        angleToTake = editText.text.toString().toInt()
                    }
                }
                val vectorIndicator = MyMath.Vector(radius.toDouble(), angleToTake)
                imgViewPointAngleIndicator?.x = (radius + vectorIndicator.get_dX()).toFloat()
                imgViewPointAngleIndicator?.y = (radius + vectorIndicator.get_dY()).toFloat()
            }

            fun show() {
                if (!areListenersSet) setListeners()
                if (relLayoutAngleIndicator != null) {
                    update()
                    relLayoutAngleIndicator?.visibility = View.VISIBLE
                }
            }

            fun hide() {
                relLayoutAngleIndicator?.visibility = View.GONE
            }
        }  // private inner class AngleIndicator
    }  // private inner class SetAxisAngles

    /** Controlling the "add edge feature".
     */
    private inner class AddEdgeFeature {
        var isShown = false
        val pointsOfEdge = mutableListOf<PolyhedronCanvas.UserInterface.Point3D>()
        var relLayoutEdgeInputInstruction: RelativeLayout?
        var linearLayoutEdgeInput: LinearLayout?
        var imgViewPoint: ImageView?
        var buttonPoint1: Button?
        var buttonPoint2: Button?
        var buttonPoint3: Button?
        val buttonsPoints = mutableListOf<Button?>()

        init {
            relLayoutEdgeInputInstruction = findViewById(R.id.rel_layout_edge_input_instruction)
            linearLayoutEdgeInput = findViewById(R.id.linear_layout_edge_input)
            imgViewPoint = findViewById(R.id.img_view_point)
            buttonPoint1 = findViewById(R.id.button_point1)
            buttonPoint2 = findViewById(R.id.button_point2)
            buttonPoint3 = findViewById(R.id.button_point3)
            relLayoutEdgeInputInstruction?.visibility = View.GONE
        }

        private fun initViewReferences() {
            if (relLayoutEdgeInputInstruction == null) {
                relLayoutEdgeInputInstruction = findViewById(R.id.rel_layout_edge_input_instruction)
                linearLayoutEdgeInput = findViewById(R.id.linear_layout_edge_input)
                imgViewPoint = findViewById(R.id.img_view_point)
                buttonPoint1 = findViewById(R.id.button_point1)
                buttonPoint2 = findViewById(R.id.button_point2)
                buttonPoint3 = findViewById(R.id.button_point3)
            }
            if (relLayoutEdgeInputInstruction != null) {
                buttonsPoints.add(buttonPoint1); buttonsPoints.add(buttonPoint2); buttonsPoints.add(buttonPoint3)
            }
        }

        private fun setCanvasListener() {
            val imgViewCanvas = findViewById<ImageView>(R.id.img_view_canvas)
            imgViewCanvas?.setOnTouchListener { view, event ->
                if (event.action == MotionEvent.ACTION_DOWN) {
                    /** TODO delete this sub function when debugging done.
                     */
                    fun showTapDiameterIndicator() {
                        val diameterTap = userInterface.generalSettings.detectPointsWithinTapDiameter
                        val radiusTap = diameterTap / 2f
                        val marginLeftImgViewCanvas = imgViewCanvas.marginLeft
                        val marginTopImgViewCanvas = imgViewCanvas.marginTop
                        imgViewPoint?.layoutParams?.width = diameterTap
                        imgViewPoint?.layoutParams?.height = diameterTap
                        imgViewPoint?.x = marginLeftImgViewCanvas + event.getX() - radiusTap
                        imgViewPoint?.y = marginTopImgViewCanvas + event.getY() - radiusTap
                        imgViewPoint?.visibility = View.VISIBLE
                    }

                    //showTapDiameterIndicator()
                    val poly = userInterface.polyhedron
                    val pointsWithinTapDiameter = poly.polyhedronOnCanvas.getPoints3DWithinTapDiameter(event.getX(), event.getY())
                    val pointsInTapDiameterToTake = mutableListOf<PolyhedronCanvas.UserInterface.Point3D>()
                    if (pointsOfEdge.isEmpty()) {  // Handle first point of new edge.
                        for (point in pointsWithinTapDiameter)
                            if (poly.edges.isAdditionalEdgeAvailableForPoint(point)) pointsInTapDiameterToTake.add(point)
                    } else if (pointsOfEdge.size == 1) {  // Handle second point.
                        for (point in pointsWithinTapDiameter) {
                            if (point != pointsOfEdge[0] && !poly.edges.isAlreadyExisting(pointsOfEdge[0], point))
                                pointsInTapDiameterToTake.add(point)  // Must not be 1st point nor already connected it.
                        }
                    }
                    if (pointsInTapDiameterToTake.isEmpty()) {
                        hide()
                        Snackbar.make(view, resources.getText(R.string.message_point_none_found), Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show()
                    } else if (pointsInTapDiameterToTake.size == 1) {
                        processValidPickedPoint(pointsInTapDiameterToTake[0])
                    } else {
                        setUpEdgeInput(pointsInTapDiameterToTake)
                    }
                }
                false
            }
        }

        /** Prepare & display "edge input" (point selector).
         */
        private fun setUpEdgeInput(pointsInTapDiameter: List<PolyhedronCanvas.UserInterface.Point3D>) {
            var isSetUp = false
            var lowestPointY = 0f  // Y coordinate of that point within pointsInTapDiameter that is located closest to bottom of canvas.
            var buttonPoint: Button?
            var captionButton: String
            val pointsCurrentlyOnCanvasF = userInterface.polyhedron.polyhedronOnCanvas.pointsCurrentlyOnCanvasF
            var point2D: PolyhedronCanvas.UserInterface.PointOnCanvasF
            // Hide unused "point select buttons" (minimum button count that makes sense is 2):
            for (indexButton in 2 until buttonsPoints.size) {
                buttonsPoints[indexButton]?.visibility = View.GONE
            }
            for ((index, point) in pointsInTapDiameter.withIndex()) {
                if (index < buttonsPoints.size && buttonsPoints[index] != null) {
                    if (point.indexOfPoint < pointsCurrentlyOnCanvasF.size) {
                        point2D = pointsCurrentlyOnCanvasF[point.indexOfPoint]
                        lowestPointY = maxOf(point2D.y, lowestPointY)
                    }
                    buttonPoint = buttonsPoints[index]
                    captionButton = point.caption
                    if (captionButton.isEmpty()) captionButton = "${resources.getString(R.string.point)} ${point.indexOfPoint + 1}"
                    buttonPoint?.text = captionButton
                    buttonPoint?.setOnClickListener { _ ->
                        linearLayoutEdgeInput?.visibility = View.GONE
                        processValidPickedPoint(point)
                    }
                    buttonPoint?.visibility = View.VISIBLE
                    if (index >= 1) isSetUp = true
                }
            }
            if (isSetUp) {
                linearLayoutEdgeInput?.y = lowestPointY + userInterface.generalSettings.pointSize
                linearLayoutEdgeInput?.visibility = View.VISIBLE
            }
        }

        /** Sets OnTouchListener of canvas ImageView to null.
         */
        private fun dumpCanvasListener() {
            val imgViewCanvas = findViewById<ImageView>(R.id.img_view_canvas)
            imgViewCanvas?.setOnTouchListener(null)
        }

        /** Activates the "add edge" feature. Only makes sense if at least 2 points are available
         *  that aren't already connected to every other point on canvas.
         */
        fun show() {
            if (buttonsPoints.isEmpty()) initViewReferences()
            if (buttonsPoints.isNotEmpty()) {
                pointsOfEdge.clear()
                setCanvasListener()
                menuItemsEnabled(false)
                relLayoutEdgeInputInstruction?.visibility = View.VISIBLE
                isShown = true
            }
        }

        fun hide() {
            isShown = false
            dumpCanvasListener()
            relLayoutEdgeInputInstruction?.visibility = View.GONE
            linearLayoutEdgeInput?.visibility = View.GONE
            menuItemsEnabled(true)
        }

        /** Handles valid "picked edge point": "Let it kind o blink", ...
         *  ...and: Create edge if it is the second picked point for the new edge.
         */
        private fun processValidPickedPoint(point: PolyhedronCanvas.UserInterface.Point3D) {
            val pointsOnCanvasF = userInterface.polyhedron.polyhedronOnCanvas.pointsCurrentlyOnCanvasF
            if (point.indexOfPoint in 0 until pointsOnCanvasF.size) {
                pointsOfEdge.add(point)
                val point2D = pointsOnCanvasF[point.indexOfPoint]
                val isPointStayingVisible = pointsOfEdge.size == 1
                pointColorFlare.execute(point2D.x.toInt(), point2D.y.toInt(), isPointStayingVisible)
            }
            if (pointsOfEdge.size == 2) {
                userInterface.polyhedron.edges.add(pointsOfEdge[0], pointsOfEdge[1])
                hide()
                updateCanvas()
            }
        }
    }  // private inner class AddEdgeFeature

    /** Creates a "color flare effect" for (over) a point on the canvas using the view imgViewPoint.
     */
    private inner class PointColorFlare {
        val pointDiameter = userInterface.generalSettings.pointSize
        val pointRadius = pointDiameter / 2
        val imgViewPoint: ImageView = findViewById(R.id.img_view_point)
        val imgViewCanvas: ImageView = findViewById(R.id.img_view_canvas)
        val marginLeftCanvas = imgViewCanvas.marginLeft
        val marginTopCanvas = imgViewCanvas.marginTop
        init {
            imgViewPoint.visibility = View.GONE
            imgViewPoint.layoutParams?.width = pointDiameter.toInt()
            imgViewPoint.layoutParams?.height = pointDiameter.toInt()
        }

        /** Execute the color flare effect for given coordinates [xImgViewCanvas] / [yImgViewCanvas]
         *  (measured from top left corner of [imgViewCanvas] in pixel).
         *  Keeps the point afterwards visible or not according to [isPointStayingVisible]:
         *  false => gone; true => marking point in different color.
         */
        fun execute(xImgViewCanvas: Int, yImgViewCanvas: Int, isPointStayingVisible: Boolean = false) {
            imgViewPoint.x = marginLeftCanvas + xImgViewCanvas - pointRadius
            imgViewPoint.y = marginTopCanvas + yImgViewCanvas- pointRadius
            imgViewPoint.setColorFilter(Color.BLACK, android.graphics.PorterDuff.Mode.SRC_IN)
            imgViewPoint.visibility = View.VISIBLE
            doViewColorFlare(imgViewPoint, 0xFFFF0000.toInt())
            // Time-delayed hiding of imgViewPoint:
            Handler(Looper.getMainLooper()).postDelayed( {
                if (isPointStayingVisible) imgViewPoint.setColorFilter(Color.BLUE, android.graphics.PorterDuff.Mode.SRC_IN)
                else imgViewPoint.visibility = View.GONE
            },1200)
        }
    }  // private inner class PointColorFlare

    /** Edges of "Example G17".
     *  Having the following 9 intersections:
     *  A-G x B-I / A-G x C-J / A-G x D-K / A-G x E-L / D-K x E-F / E-L x J-K / G-N x L-M / N-O x P-S / O-T x R-S
     *  TODO delete this method if it got obsolete.
     */

    /** Create points for a most simple polyhedron that is QUITE like the one in example G13
     *  (but shifted from a vertical edge along the Z axis to:  X = 1; Y = 1).
     */
    private fun createG13AlikePoints() {
        val exampleBody = userInterface.polyhedron
        userInterface.generalSettings.viewAngleAxisX = 211f  // 210f
        userInterface.generalSettings.viewAngleAxisY = 340f  // 330f
        userInterface.generalSettings.viewAngleAxisZ = 90f

        val factorAxesMax = 7.0f
        userInterface.generalSettings.factorAxisX = 3.8f / factorAxesMax
        userInterface.generalSettings.factorAxisY = 6.3f / factorAxesMax
        userInterface.generalSettings.factorAxisZ = 5.0f / factorAxesMax

        exampleBody.addPoint(1,1,0,"A")  // #0
        exampleBody.addPoint(2,4,0,"B")  // #1
        exampleBody.addPoint(4,2,0,"C")  // #2
        exampleBody.addPoint(1,1,4,"P")  // #3;  Maybe use: 0 | 0 | 3
        exampleBody.addPoint(2,4,3,"Q")  // #4
        exampleBody.addPoint(4,2,2,"R")  // #5
    }

    private fun createG13Edges() {
        val exampleBody = userInterface.polyhedron
        exampleBody.edges.add(exampleBody.points[0], exampleBody.points[1])  // A-B
        exampleBody.edges.add(exampleBody.points[0], exampleBody.points[2], true)  // A-C
        exampleBody.edges.add(exampleBody.points[1], exampleBody.points[2], true)  // B-C
        exampleBody.edges.add(exampleBody.points[3], exampleBody.points[4])  // P-Q
        exampleBody.edges.add(exampleBody.points[3], exampleBody.points[5])  // P-R
        exampleBody.edges.add(exampleBody.points[4], exampleBody.points[5])  // Q-R
        exampleBody.edges.add(exampleBody.points[0], exampleBody.points[3])  // A-P
        exampleBody.edges.add(exampleBody.points[1], exampleBody.points[4])  // B-Q
        exampleBody.edges.add(exampleBody.points[2], exampleBody.points[5])  // C-R
    }

    private fun createG17Edges() {
        val example = userInterface.polyhedron
        if (example.points.size == 20) {
            example.edges.clear()
        } else {  // Ensure that exactly the necessary count of points is available.
            userInterface.polyhedron.reset()
            createTestPolyhedron()
        }
        example.edges.add(example.points[0], example.points[1])  // A-B
        example.edges.add(example.points[1], example.points[2])  // B-C
        example.edges.add(example.points[2], example.points[3])  // C-D
        example.edges.add(example.points[3], example.points[4])  // D-E
        example.edges.add(example.points[4], example.points[5])  // E-F
        example.edges.add(example.points[5], example.points[6])  // F-G
        example.edges.add(example.points[0], example.points[6])  // A-G
        example.edges.add(example.points[7], example.points[8])  // H-I
        example.edges.add(example.points[8], example.points[9])  // I-J
        example.edges.add(example.points[9], example.points[10])  // J-K
        example.edges.add(example.points[10], example.points[11])  // K-L
        example.edges.add(example.points[11], example.points[12])  // L-M
        example.edges.add(example.points[12], example.points[13])  // M-N
        example.edges.add(example.points[13], example.points[14])  // N-O
        example.edges.add(example.points[14], example.points[15])  // O-P
        example.edges.add(example.points[7], example.points[15])  // H-P
        example.edges.add(example.points[16], example.points[17])  // Q-R
        example.edges.add(example.points[17], example.points[18])  // R-S
        example.edges.add(example.points[18], example.points[19])  // S-T
        example.edges.add(example.points[19], example.points[16])  // T-Q
        example.edges.add(example.points[0], example.points[16])  // A-Q
        example.edges.add(example.points[7], example.points[17])  // H-R
        example.edges.add(example.points[15], example.points[18])  // P-S
        example.edges.add(example.points[14], example.points[19])  // O-T
        example.edges.add(example.points[1], example.points[8])  // B-I
        example.edges.add(example.points[2], example.points[9])  // C-J
        example.edges.add(example.points[3], example.points[10])  // D-K
        example.edges.add(example.points[4], example.points[11])  // E-L
        example.edges.add(example.points[5], example.points[12])  // F-M
        example.edges.add(example.points[6], example.points[13])  // G-N
        example.edges.updateVisibility()
    }

    /** TODO delete when obsolete.
     */
    private fun createTestPolyhedron() {
        val exampleBody = userInterface.polyhedron
        val polyhedron2D = userInterface.polyhedron.polyhedronOnCanvas
        fun logPolyhedronOnCanvasPointsF() {
            for ((index, point) in polyhedron2D.polyhedronOnCanvasPointsF.withIndex()) Log.d("createTestPolyhedron()", "Punkt $index = ${point.x} / ${point.y}")
            Log.d("createTestPolyhedron()", "- - - - - - - -")
            val (x1, y1) = polyhedron2D.rectangleOfCanvas.left to polyhedron2D.rectangleOfCanvas.top
            val (x2, y2) = polyhedron2D.rectangleOfCanvas.right to polyhedron2D.rectangleOfCanvas.bottom
            Log.d("createTestPolyhedron()", "rectangleOfCanvas.left / top = $x1 / $y1")
            Log.d("createTestPolyhedron()", "rectangleOfCanvas.right / bottom = $x2 / $y2")
            Log.d("createTestPolyhedron()", "--------------------------------------------------------------")
        }

        /*
        // Test Points:
        //userInterface.generalSettings.viewAngleAxisX = 290f
        //userInterface.generalSettings.viewAngleAxisY = 50f
        //userInterface.generalSettings.viewAngleAxisZ = 90f
        exampleBody.addPoint(0,0,0,"A")  // test point.
        exampleBody.addPoint(1,3,0,"B")  // test point.
        exampleBody.addPoint(1,3,1,"X")  // test point.
        exampleBody.addPoint(3,1,0,"C")  // test point.
        exampleBody.addPoint(1,1,1,"D")  // test point.
        exampleBody.addPoint(2,2,2,"E")  // test point.
        exampleBody.addPoint(3,1,2,"F")  // test point.
        */


        /*
        // Example "G12":
        userInterface.generalSettings.viewAngleAxisX = 210f
        userInterface.generalSettings.viewAngleAxisY = 330f
        userInterface.generalSettings.viewAngleAxisZ = 90f
        exampleBody.addPoint(0,3,0,"A")  // #0
        exampleBody.addPoint(2,0,0,"B")  // #1
        exampleBody.addPoint(3,4,0,"C")  // #2
        exampleBody.addPoint(0,3,5,"P")  // #3
        exampleBody.addPoint(2,0,1,"Q")  // #4
        exampleBody.addPoint(3,4,3,"R")  // #5
        */

        /*
        // Example "G14":
        userInterface.generalSettings.viewAngleAxisX = 250f
        userInterface.generalSettings.viewAngleAxisY = 350f
        userInterface.generalSettings.viewAngleAxisZ = 90f
        exampleBody.addPoint(0,4,0,"A")  // #0
        exampleBody.addPoint(1,1,0,"B")  // #1
        exampleBody.addPoint(3,3,0,"C")  // #2
        exampleBody.addPoint(0,4,1,"P")  // #3
        exampleBody.addPoint(1,1,2,"Q")  // #4
        exampleBody.addPoint(3,3,5,"R")  // #5
        exampleBody.edges.add(exampleBody.points[0], exampleBody.points[1])  // A-B
        exampleBody.edges.add(exampleBody.points[0], exampleBody.points[2])  // A-C
        exampleBody.edges.add(exampleBody.points[1], exampleBody.points[2])  // B-C
        exampleBody.edges.add(exampleBody.points[3], exampleBody.points[4])  // P-Q
        exampleBody.edges.add(exampleBody.points[3], exampleBody.points[5])  // P-R
        exampleBody.edges.add(exampleBody.points[4], exampleBody.points[5])  // Q-R
        exampleBody.edges.add(exampleBody.points[0], exampleBody.points[3])  // A-P
        exampleBody.edges.add(exampleBody.points[1], exampleBody.points[4])  // B-Q
        exampleBody.edges.add(exampleBody.points[2], exampleBody.points[5])  // C-R
        */

        createG13AlikePoints()

        /*
        // Example "G17":
        userInterface.generalSettings.viewAngleAxisX = 226f  // Alternative view: 320f
        userInterface.generalSettings.viewAngleAxisY = 342f  //                    12f
        userInterface.generalSettings.viewAngleAxisZ = 90f
        val factorAxesMax = 7.0f
        userInterface.generalSettings.factorAxisX = 3.8f / factorAxesMax
        userInterface.generalSettings.factorAxisY = 6.3f / factorAxesMax  // Changed from 6.3 -> 7.0  =>  1 additional error: K-L drawn dashed.
        userInterface.generalSettings.factorAxisZ = 7.0f / factorAxesMax
        exampleBody.addPoint(0,0,0,"A")  // #00
        exampleBody.addPoint(2,0,0,"B")  // #01
        exampleBody.addPoint(3,1,0,"C")  // #02
        exampleBody.addPoint(3,2,0,"D")  // #03
        exampleBody.addPoint(2,2,0,"E")  // #04
        exampleBody.addPoint(2,4,0,"F")  // #05
        exampleBody.addPoint(0,4,0,"G")  // #06
        exampleBody.addPoint(1,0,2,"H")  // #07
        exampleBody.addPoint(2,0,2,"I")  // #08
        exampleBody.addPoint(3,1,2,"J")  // #09
        exampleBody.addPoint(3,2,2,"K")  // #10
        exampleBody.addPoint(2,2,2,"L")  // #11
        exampleBody.addPoint(2,4,2,"M")  // #12
        exampleBody.addPoint(0,4,2,"N")  // #13
        exampleBody.addPoint(0,2,2,"O")  // #14
        exampleBody.addPoint(1,2,2,"P")  // #15
        exampleBody.addPoint(0,0,5,"Q")  // #16
        exampleBody.addPoint(1,0,5,"R")  // #17
        exampleBody.addPoint(1,2,5,"S")  // #18
        exampleBody.addPoint(0,2,5,"T")  // #19
        */

        /*
        // Example "G18":
        userInterface.generalSettings.viewAngleAxisX = 202f
        userInterface.generalSettings.viewAngleAxisY = 345f
        userInterface.generalSettings.viewAngleAxisZ = 90f
        val factorAxesMax = 13.2f
        userInterface.generalSettings.factorAxisX = 10.8f / factorAxesMax
        userInterface.generalSettings.factorAxisY = 11.6f / factorAxesMax
        userInterface.generalSettings.factorAxisZ = 13.2f / factorAxesMax
        */
    }

    /** Debugging of Polygon.isEncasingPoint. TODO delete when debugging done.
     */
    private fun testPointGisEncasedByPolyognEFML() {
        val pointG = userInterface.polyhedron.getPointByCaption("G")
        val pointGonCanvas = pointG.getOnCanvas()
        val polygonEFML = userInterface.polyhedron.polygons.getPolygonByCaption("E-F-M-L")
        val isEncased = polygonEFML.isEncasingPoint(pointGonCanvas)
        Log.d("MainActivity.onCreate()", "Coordinates of point ${pointG.caption} = ${pointGonCanvas.getFormattedX()}/${pointGonCanvas.getFormattedY()}")
        Log.d("MainActivity.onCreate()", "Coordinates of polygon ${polygonEFML.getCaption()} = ${polygonEFML.getFormattedCoordinates()}")
        Log.d("MainActivity.onCreate()", "Point ${pointG.caption} is encased by polygon ${polygonEFML.getCaption()}:  $isEncased")
    }

    /** TODO delete when debugging done.
     */
    private fun logFragmentCountOfEdgeAG() {
        val edgeEG = userInterface.polyhedron.edges.geEdgeByCaption("A-G")
        Log.d("logFragmentCntOfEdgeAG", "fragment count of edge ${edgeEG.getCaption()} = ${edgeEG.edgeFragments.size}")
    }
}  // class MainActivity : AppCompatActivity()

fun isCanvasAvailable(): Boolean {
    return nCanvasWidthPixel > 0 && nCanvasHeightPixel > 0
}