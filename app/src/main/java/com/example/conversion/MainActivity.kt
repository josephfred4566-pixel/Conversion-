package com.example.conversion

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

private data class ConversionUnit(
    val label: String,
    val toBase: (Double) -> Double,
    val fromBase: (Double) -> Double
)

private data class ConversionCategory(
    val label: String,
    val units: List<ConversionUnit>
)

class MainActivity : Activity() {
    private lateinit var categorySpinner: Spinner
    private lateinit var fromSpinner: Spinner
    private lateinit var toSpinner: Spinner
    private lateinit var amountInput: EditText
    private lateinit var resultText: TextView
    private lateinit var resultCaption: TextView
    private var updatingUnits = false

    private val categories = listOf(
        ConversionCategory(
            "Length",
            listOf(
                unit("Meter (m)", 1.0),
                unit("Kilometer (km)", 1_000.0),
                unit("Centimeter (cm)", 0.01),
                unit("Millimeter (mm)", 0.001),
                unit("Mile (mi)", 1_609.344),
                unit("Foot (ft)", 0.3048),
                unit("Inch (in)", 0.0254)
            )
        ),
        ConversionCategory(
            "Mass",
            listOf(
                unit("Kilogram (kg)", 1.0),
                unit("Gram (g)", 0.001),
                unit("Milligram (mg)", 0.000001),
                unit("Pound (lb)", 0.45359237),
                unit("Ounce (oz)", 0.028349523125)
            )
        ),
        ConversionCategory(
            "Temperature",
            listOf(
                ConversionUnit("Celsius (°C)", { it }, { it }),
                ConversionUnit("Fahrenheit (°F)", { (it - 32.0) * 5.0 / 9.0 }, { it * 9.0 / 5.0 + 32.0 }),
                ConversionUnit("Kelvin (K)", { it - 273.15 }, { it + 273.15 })
            )
        ),
        ConversionCategory(
            "Volume",
            listOf(
                unit("Liter (L)", 1.0),
                unit("Milliliter (mL)", 0.001),
                unit("US gallon (gal)", 3.785411784),
                unit("US cup", 0.2365882365),
                unit("US fluid ounce (fl oz)", 0.0295735295625)
            )
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.rgb(246, 248, 252)
        window.navigationBarColor = Color.rgb(246, 248, 252)
        window.decorView.systemUiVisibility =
            android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        setContentView(createScreen())
    }

    private fun createScreen(): android.view.View {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(246, 248, 252))
            isFillViewport = true
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(28), dp(22), dp(28))
        }
        scroll.addView(content)

        addText(content, "Conversion", 30f, Color.rgb(26, 39, 59), bold = true)
        addText(content, "Quick, accurate conversions — works offline.", 15f, Color.rgb(105, 119, 139), bottom = 22)

        val categoryCard = card()
        addText(categoryCard, "CONVERSION TYPE", 12f, Color.rgb(105, 119, 139), bold = true, bottom = 8)
        categorySpinner = Spinner(this)
        categorySpinner.adapter = adapter(categories.map { it.label })
        categoryCard.addView(categorySpinner, matchWrap())
        content.addView(categoryCard, fullWidth(bottom = 14))

        val inputCard = card()
        addText(inputCard, "VALUE", 12f, Color.rgb(105, 119, 139), bold = true, bottom = 8)
        amountInput = EditText(this).apply {
            setText("1")
            textSize = 20f
            setTextColor(Color.rgb(26, 39, 59))
            setHintTextColor(Color.rgb(150, 160, 175))
            hint = "Enter a value"
            inputType = android.text.InputType.TYPE_CLASS_NUMBER or
                android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or
                android.text.InputType.TYPE_NUMBER_FLAG_SIGNED
            imeOptions = EditorInfo.IME_ACTION_DONE
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = roundedBackground(Color.rgb(247, 249, 252), 12)
        }
        inputCard.addView(amountInput, fullWidth(bottom = 18))

        addText(inputCard, "FROM", 12f, Color.rgb(105, 119, 139), bold = true, bottom = 4)
        fromSpinner = Spinner(this)
        inputCard.addView(fromSpinner, fullWidth(bottom = 12))

        addText(inputCard, "TO", 12f, Color.rgb(105, 119, 139), bold = true, bottom = 4)
        toSpinner = Spinner(this)
        inputCard.addView(toSpinner, fullWidth(bottom = 14))

        val buttonRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }
        val swapButton = Button(this).apply {
            text = "Swap units"
            isAllCaps = false
            setTextColor(Color.rgb(39, 93, 219))
            backgroundTintList = ColorStateList.valueOf(Color.rgb(230, 238, 255))
            setOnClickListener {
                val from = fromSpinner.selectedItemPosition
                val to = toSpinner.selectedItemPosition
                fromSpinner.setSelection(to)
                toSpinner.setSelection(from)
                convertValue()
            }
        }
        val convertButton = Button(this).apply {
            text = "Convert"
            isAllCaps = false
            setTextColor(Color.WHITE)
            backgroundTintList = ColorStateList.valueOf(Color.rgb(39, 93, 219))
            setOnClickListener {
                (getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                    ?.hideSoftInputFromWindow(amountInput.windowToken, 0)
                convertValue()
            }
        }
        buttonRow.addView(swapButton, LinearLayout.LayoutParams(0, dp(52), 1f).apply { marginEnd = dp(8) })
        buttonRow.addView(convertButton, LinearLayout.LayoutParams(0, dp(52), 1f).apply { marginStart = dp(8) })
        inputCard.addView(buttonRow, matchWrap())
        content.addView(inputCard, fullWidth(bottom = 14))

        val outputCard = card().apply { setBackgroundColor(Color.rgb(234, 242, 255)) }
        addText(outputCard, "RESULT", 12f, Color.rgb(74, 103, 157), bold = true, bottom = 8)
        resultCaption = addText(outputCard, "1 Meter (m) =", 14f, Color.rgb(74, 103, 157), bottom = 4)
        resultText = addText(outputCard, "1,000 Kilometer (km)", 23f, Color.rgb(31, 74, 155), bold = true)
        content.addView(outputCard, fullWidth(bottom = 14))

        addText(content, "Conversions are calculated on your device. No account or network connection is needed.", 13f, Color.rgb(105, 119, 139))

        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                refreshUnits()
            }
        }
        val unitListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (!updatingUnits) convertValue()
            }
        }
        fromSpinner.onItemSelectedListener = unitListener
        toSpinner.onItemSelectedListener = unitListener
        amountInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) = convertValue()
        })
        refreshUnits()
        return scroll
    }

    private fun refreshUnits() {
        if (!::fromSpinner.isInitialized || !::toSpinner.isInitialized) return
        val index = categorySpinner.selectedItemPosition.coerceIn(categories.indices)
        val units = categories[index].units
        updatingUnits = true
        fromSpinner.adapter = adapter(units.map { it.label })
        toSpinner.adapter = adapter(units.map { it.label })
        fromSpinner.setSelection(0, false)
        toSpinner.setSelection(if (units.size > 1) 1 else 0, false)
        updatingUnits = false
        convertValue()
    }

    private fun convertValue() {
        if (!::resultText.isInitialized || !::categorySpinner.isInitialized || !::fromSpinner.isInitialized || !::toSpinner.isInitialized) return
        val amount = amountInput.text?.toString()?.trim()?.toDoubleOrNull()
        if (amount == null) {
            resultCaption.text = "Enter a valid number to convert."
            resultText.text = "—"
            return
        }
        val category = categories[categorySpinner.selectedItemPosition.coerceIn(categories.indices)]
        val from = category.units[fromSpinner.selectedItemPosition.coerceIn(category.units.indices)]
        val to = category.units[toSpinner.selectedItemPosition.coerceIn(category.units.indices)]
        val converted = to.fromBase(from.toBase(amount))
        val formatter = DecimalFormat("#,##0.######", DecimalFormatSymbols.getInstance(Locale.getDefault()))
        resultCaption.text = "${formatter.format(amount)} ${from.label} ="
        resultText.text = "${formatter.format(converted)} ${to.label}"
    }

    private fun card(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
        background = roundedBackground(Color.WHITE, 18)
        elevation = dp(2).toFloat()
    }

    private fun addText(
        parent: LinearLayout,
        text: String,
        size: Float,
        color: Int,
        bold: Boolean = false,
        bottom: Int = 0
    ): TextView {
        val view = TextView(this).apply {
            this.text = text
            textSize = size
            setTextColor(color)
            if (bold) setTypeface(typeface, Typeface.BOLD)
        }
        parent.addView(view, fullWidth(bottom = bottom))
        return view
    }

    private fun adapter(items: List<String>): ArrayAdapter<String> =
        ArrayAdapter(this, android.R.layout.simple_spinner_item, items).also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

    private fun roundedBackground(color: Int, radiusDp: Int): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp).toFloat()
            setStroke(dp(1), Color.rgb(229, 234, 242))
        }

    private fun unit(label: String, baseUnitsPerUnit: Double) = ConversionUnit(
        label = label,
        toBase = { it * baseUnitsPerUnit },
        fromBase = { it / baseUnitsPerUnit }
    )

    private fun fullWidth(bottom: Int = 0) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { if (bottom > 0) bottomMargin = dp(bottom) }

    private fun matchWrap() = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
