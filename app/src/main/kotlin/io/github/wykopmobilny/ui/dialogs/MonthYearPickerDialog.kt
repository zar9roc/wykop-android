package io.github.wykopmobilny.ui.dialogs

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.Window
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.setFragmentResult
import io.github.wykopmobilny.R
import io.github.wykopmobilny.databinding.YearMonthPickerBinding
import java.util.Calendar

class MonthYearPickerDialog : DialogFragment() {
    companion object {
        const val RESULT_CODE = 167

        // Klucz dla Fragment Result API - uzywany gdy dialog jest pokazywany bez
        // targetFragment (np. z poziomu aktywnosci, jak w widoku tagu).
        const val REQUEST_KEY = "MonthYearPickerDialog"
        const val EXTRA_YEAR = "EXTRA_YEAR"
        const val EXTRA_MONTH = "EXTRA_MONTH"

        // selectedMonth == WHOLE_YEAR -> wczesniej wybrano "Caly rok"; odtwarzamy
        // wskaznik na slot calorocznyzamiast na konkretny miesiac.
        const val WHOLE_YEAR = 13

        fun newInstance(
            selectedMonth: Int = 0,
            selectedYear: Int = 0,
        ): MonthYearPickerDialog {
            val intent = MonthYearPickerDialog()
            val arguments = Bundle()
            arguments.putInt(EXTRA_YEAR, selectedYear)
            arguments.putInt(EXTRA_MONTH, selectedMonth)
            intent.arguments = arguments
            return intent
        }
    }

    private val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
    private var yearSelection = currentYear
    private var selectedMonth = currentMonth - 1

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogBuilder = AlertDialog.Builder(context)
        val argumentYear = requireArguments().getInt(EXTRA_YEAR)
        val argumentMonth = requireArguments().getInt(EXTRA_MONTH)

        yearSelection = if (argumentYear == 0) currentYear else argumentYear
        selectedMonth = if (argumentMonth == 0) currentMonth else argumentMonth - 1
        // Odtworzenie wskaznikow z wczesniejszego wyboru: konkretny miesiac (1..12),
        // caly rok (WHOLE_YEAR) albo brak (0 -> domyslnie biezacy miesiac).
        val restoreWholeYear = argumentMonth == WHOLE_YEAR
        val restoredMonthIndex = if (argumentMonth in 1..12) argumentMonth - 1 else null
        val dialogView = YearMonthPickerBinding.inflate(layoutInflater)
        dialogBuilder.apply {
            setPositiveButton(android.R.string.ok) { _, _ ->
                val hasMonth = selectedMonth < 12
                val data = Intent()
                data.putExtra(EXTRA_YEAR, yearSelection)
                // Only add EXTRA_MONTH if a specific month was selected (not "Cały rok")
                if (hasMonth) {
                    data.putExtra(EXTRA_MONTH, selectedMonth + 1)
                }
                targetFragment?.onActivityResult(targetRequestCode, RESULT_CODE, data)
                // Rownolegle Fragment Result API - dziala takze bez targetFragment.
                val result = bundleOf(EXTRA_YEAR to yearSelection)
                if (hasMonth) {
                    result.putInt(EXTRA_MONTH, selectedMonth + 1)
                }
                setFragmentResult(REQUEST_KEY, result)
            }
            setView(dialogView.root)
        }
        val newDialog = dialogBuilder.create()
        newDialog.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialogView.apply {
            yearPicker.minValue = 2005
            yearPicker.maxValue = currentYear
            yearPicker.value = yearSelection
            setYear(this)
            // setYear konfiguruje zakres miesiaca (i dla biezacego roku/2005 nadpisuje
            // selectedMonth) - dopiero teraz mozemy narzucic odtworzony wybor.
            when {
                // Slot "Caly rok" to zawsze ostatnia (maks.) pozycja pickera dla roku.
                restoreWholeYear -> selectedMonth = monthPicker.maxValue
                restoredMonthIndex != null ->
                    selectedMonth = restoredMonthIndex.coerceIn(monthPicker.minValue, monthPicker.maxValue)
            }
            monthPicker.value = selectedMonth
            setTitleDate(this)
            yearPicker.setOnValueChangedListener { _, _, year ->
                yearSelection = year
                setYear(this)
            }

            monthPicker.setOnValueChangedListener { _, _, month ->
                selectedMonth = month
                setTitleDate(this)
            }
        }
        return newDialog
    }

    private fun setYear(binding: YearMonthPickerBinding) {
        binding.apply {
            when (yearSelection) {
                currentYear -> {
                    // For current year (2025), show only December + "Cały rok" option
                    if (currentMonth == 12) {
                        monthPicker.displayedValues = arrayOf(getMonthString(12), getString(R.string.whole_year))
                        monthPicker.minValue = 11
                        monthPicker.maxValue = 12
                        monthPicker.value = 11
                        selectedMonth = 11
                    } else {
                        // For future years, show available months + "Cały rok"
                        val months = (1..currentMonth).map { getMonthString(it) } + getString(R.string.whole_year)
                        monthPicker.displayedValues = months.toTypedArray()
                        monthPicker.minValue = 0
                        monthPicker.value = currentMonth - 1
                        monthPicker.maxValue = currentMonth
                        selectedMonth = currentMonth - 1
                    }
                }

                2005 -> {
                    // For 2005, only December is available (no "Cały rok" option)
                    monthPicker.minValue = 11
                    monthPicker.maxValue = 11
                    monthPicker.value = 11
                    monthPicker.displayedValues = arrayOf(getMonthString(12))
                    selectedMonth = 11
                }

                else -> {
                    // For all other years, show all 12 months + "Cały rok" option
                    val months = (1..12).map { getMonthString(it) } + getString(R.string.whole_year)
                    monthPicker.displayedValues = months.toTypedArray()
                    monthPicker.minValue = 0
                    monthPicker.maxValue = 12
                }
            }
            setTitleDate(this)
        }
    }

    private fun setTitleDate(view: YearMonthPickerBinding) {
        view.monthTextView.text =
            if (selectedMonth == 12) {
                getString(R.string.whole_year)
            } else {
                getMonthString(selectedMonth + 1)
            }
        view.yearTextView.text = yearSelection.toString()
    }

    private fun getMonthString(index: Int): String =
        getString(
            when (index) {
                1 -> R.string.january
                2 -> R.string.february
                3 -> R.string.march
                4 -> R.string.april
                5 -> R.string.may
                6 -> R.string.june
                7 -> R.string.july
                8 -> R.string.august
                9 -> R.string.september
                10 -> R.string.october
                11 -> R.string.novermber
                else -> R.string.december
            },
        )
}
