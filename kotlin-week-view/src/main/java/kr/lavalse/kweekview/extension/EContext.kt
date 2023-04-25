package kr.lavalse.kweekview.extension

import android.content.Context
import android.util.TypedValue

object EContext {
    fun Context.applyDimension(unit: Int, value: Float)
            = TypedValue.applyDimension(unit, value, resources.displayMetrics)

    fun Context.toSP(value: Float) = applyDimension(TypedValue.COMPLEX_UNIT_SP, value)
    fun Context.toSP(value: Int) = toSP(value.toFloat())

    fun Context.toDP(value: Float) = applyDimension(TypedValue.COMPLEX_UNIT_DIP, value)
    fun Context.toDP(value: Int) = toDP(value.toFloat())
}