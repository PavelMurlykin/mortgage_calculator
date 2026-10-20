package com.pamurlykin.mortgagecalculator.ui.screens

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import com.pamurlykin.mortgagecalculator.data.MortgageEntity
import com.pamurlykin.mortgagecalculator.ui.theme.MortgageCalculatorTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal suspend fun createCalculationShareIntent(host: ViewGroup, calculation: MortgageEntity): Intent {
    val bitmap = withContext(Dispatchers.Main.immediate) { renderCalculationBitmap(host, calculation) }
    try {
        return withContext(Dispatchers.IO) {
            val context = host.context
            val directory = File(context.cacheDir, "calculation-images").apply { check(isDirectory || mkdirs()) }
            val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
            directory.listFiles()?.filter { it.extension == "png" && it.lastModified() < cutoff }?.forEach { it.delete() }
            val file = File.createTempFile("calculation-${calculation.id}-", ".png", directory)
            file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.calculation-images", file)
            Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TITLE, calculation.title.ifBlank { "Ипотечный расчет" })
                clipData = ClipData.newUri(context.contentResolver, "Ипотечный расчет", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    } finally {
        bitmap.recycle()
    }
}

/** Render at a stable resolution, independently of scrolling, screen height and system font size. */
internal fun renderCalculationBitmap(host: ViewGroup, calculation: MortgageEntity): Bitmap {
    check(Looper.myLooper() == Looper.getMainLooper())
    val view = ComposeView(host.context).apply {
        alpha = 0f
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(density = 3f, fontScale = 1f)) {
                MortgageCalculatorTheme(darkTheme = false) {
                    Box(Modifier.background(listBackground()).padding(16.dp)) {
                        CalculationSummaryCard(calculation)
                    }
                }
            }
        }
    }
    try {
        // Attachment supplies the activity's lifecycle and saved-state owners. Rendering and
        // removal happen in one main-thread call; this invisible view never covers the list.
        host.addView(view, 0, ViewGroup.LayoutParams(1260, ViewGroup.LayoutParams.WRAP_CONTENT))
        view.measure(View.MeasureSpec.makeMeasureSpec(1260, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)
        return createBitmap(view.measuredWidth, view.measuredHeight).also {
            view.draw(Canvas(it))
        }
    } finally {
        view.disposeComposition()
        host.removeView(view)
    }
}
