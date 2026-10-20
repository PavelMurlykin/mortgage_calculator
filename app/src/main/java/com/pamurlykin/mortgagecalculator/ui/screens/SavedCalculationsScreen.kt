package com.pamurlykin.mortgagecalculator.ui.screens

import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.pamurlykin.mortgagecalculator.R
import com.pamurlykin.mortgagecalculator.data.MortgageEntity
import com.pamurlykin.mortgagecalculator.ui.MortgageViewModel
import com.pamurlykin.mortgagecalculator.util.MortgageCalculator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@Composable
fun SavedCalculationsScreen(mortgageViewModel: MortgageViewModel, navController: NavController) {
    val savedCalculations by mortgageViewModel.savedCalculations.collectAsState()
    val context = LocalContext.current
    val host = LocalView.current.rootView as ViewGroup
    val scope = rememberCoroutineScope()
    var sharing by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(listBackground()).padding(horizontal = 16.dp)) {
        Text("Список", fontSize = 32.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp, bottom = 20.dp))
        if (savedCalculations.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Нет сохраненных расчетов", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                items(savedCalculations, key = { it.id }) { calculation ->
                    CalculationCard(
                        calculation = calculation,
                        onEdit = {
                            mortgageViewModel.loadCalculation(calculation)
                            navController.navigate("calculation") {
                                popUpTo("calculation") { inclusive = true }
                            }
                        },
                        onDelete = { mortgageViewModel.deleteSavedCalculation(calculation.id) },
                        onTitleChange = { mortgageViewModel.updateCalculationTitle(calculation.id, it) },
                        sharingEnabled = !sharing,
                        onShare = { snapshot ->
                            sharing = true
                            scope.launch {
                                try {
                                    val intent = createCalculationShareIntent(host, snapshot)
                                    context.startActivity(android.content.Intent.createChooser(intent, "Поделиться расчетом"))
                                } catch (cancelled: CancellationException) {
                                    throw cancelled
                                } catch (_: Exception) {
                                    Toast.makeText(context, "Не удалось отправить изображение расчета", Toast.LENGTH_LONG).show()
                                } finally {
                                    sharing = false
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
internal fun CalculationCard(
    calculation: MortgageEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onTitleChange: (String) -> Unit,
    onShare: (MortgageEntity) -> Unit,
    sharingEnabled: Boolean = true
) {
    var editing by remember(calculation.id) { mutableStateOf(false) }
    var title by remember(calculation.title) {
        mutableStateOf(TextFieldValue(calculation.title, TextRange(calculation.title.length)))
    }
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val commitTitle = {
        editing = false
        onTitleChange(title.text)
        focusManager.clearFocus()
    }
    CalculationSummaryCard(
        calculation = calculation,
        title = {
            if (editing) {
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    textStyle = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { commitTitle() })
                )
                LaunchedEffect(Unit) { focus.requestFocus() }
            } else {
                CalculationTitle(calculation.title, Modifier.clickable { editing = true })
            }
        },
        actions = {
            Row {
                CardAction(Icons.Rounded.Share, "Поделиться расчетом", sharingEnabled) {
                    val snapshot = if (editing) calculation.copy(title = title.text, timestamp = System.currentTimeMillis()) else calculation
                    if (editing) commitTitle()
                    onShare(snapshot)
                }
                CardAction(Icons.Rounded.Edit, "Редактировать", onClick = onEdit)
                CardAction(Icons.Rounded.Delete, "Удалить", destructive = true, onClick = onDelete)
            }
        }
    )
}

/** Shared by the list and image export so the image includes the whole calculation. */
@Composable
internal fun CalculationSummaryCard(
    calculation: MortgageEntity,
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit = { CalculationTitle(calculation.title) },
    actions: (@Composable () -> Unit)? = null
) {
    val symbols = remember { DecimalFormatSymbols(Locale.getDefault()).apply { groupingSeparator = ' ' } }
    val money = remember { DecimalFormat("#,##0", symbols) }
    val rate = remember { DecimalFormat("0.##", symbols) }
    val finalValue = MortgageCalculator.calculateFinalPropertyValue(
        calculation.propertyValue, calculation.discountAmount, calculation.isMarkup, calculation.showDiscount)
    val term = calculation.termMonths.coerceAtLeast(0)
    val grace = if (calculation.showGracePeriod) calculation.gracePeriodMonths.coerceIn(0, term) else 0
    val payments = MortgageCalculator.calculatePayments(
        finalValue - calculation.downPayment, calculation.interestRate, term,
        calculation.isAnnuity, grace, calculation.graceInterestRate)
    val downPercent = if (finalValue > 0) calculation.downPayment / finalValue * 100 else 0.0
    val modifierPercent = if (calculation.propertyValue > 0) calculation.discountAmount / calculation.propertyValue * 100 else 0.0
    val hasModifier = calculation.showDiscount && calculation.discountAmount > 0
    val modifierLabel = when {
        !hasModifier -> "Без удорожания"
        calculation.isMarkup -> "С удорожанием ${rate.format(modifierPercent)} %"
        else -> "Со скидкой ${rate.format(modifierPercent)} %"
    }

    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
        ProvideTextStyle(MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.sp)) {
            BoxWithConstraints(Modifier.padding(14.dp)) {
                val stacked = maxWidth < 310.dp || LocalDensity.current.fontScale > 1.3f
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val heading: @Composable () -> Unit = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            title()
                            Text(updatedLabel(calculation.timestamp), fontSize = 12.sp, color = labelColor())
                        }
                    }
                    if (stacked && actions != null) {
                        heading()
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { actions() }
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) { heading() }
                            actions?.invoke()
                        }
                    }
                    MetricPair(stacked,
                        first = { MetricTile(Icons.Rounded.Home, "Стоимость объекта", "${money.format(finalValue)} ₽") },
                        second = { MetricTile(ImageVector.vectorResource(R.drawable.ic_coins), "Первый взнос", "${money.format(calculation.downPayment)} ₽") })
                    if (grace > 0) {
                        MetricPair(stacked,
                            first = {
                                PeriodPanel("Льготный период", "${money.format(payments.graceMonthlyPayment)} ₽", formatTerm(grace), "${rate.format(calculation.graceInterestRate)} %")
                            },
                            second = {
                                PeriodPanel("Основной период", "${money.format(payments.monthlyPayment)} ₽", formatTerm(term), "${rate.format(calculation.interestRate)} %")
                            })
                        if (grace == term) Text("После льготного периода платежей нет", fontSize = 12.sp, color = labelColor())
                    } else {
                        MetricTile(Icons.Rounded.AccountBalanceWallet, "Платеж", "${money.format(payments.monthlyPayment)} ₽")
                        MetricPair(stacked,
                            first = { MetricTile(Icons.Rounded.CalendarMonth, "Срок", formatTerm(term)) },
                            second = { MetricTile(Icons.Rounded.Percent, "Ставка", "${rate.format(calculation.interestRate)} %") })
                    }
                    SummaryBadges("Взнос ${rate.format(downPercent)} %", if (calculation.isAnnuity) "Аннуитет" else "Дифференцированный", modifierLabel, hasModifier)
                }
            }
        }
    }
}

@Composable
private fun CalculationTitle(title: String, modifier: Modifier = Modifier) {
    Text(title.ifBlank { "Сохраненный расчет" }, modifier, fontSize = 18.sp, lineHeight = 21.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun CardAction(icon: ImageVector, description: String, enabled: Boolean = true, destructive: Boolean = false, onClick: () -> Unit) {
    val tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    IconButton(onClick = onClick, enabled = enabled, modifier = Modifier.size(48.dp)) {
        Box(Modifier.size(40.dp).background(tint.copy(alpha = 0.06f), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(icon, description, tint = tint.copy(alpha = if (enabled) 1f else 0.4f), modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun MetricPair(stacked: Boolean, first: @Composable () -> Unit, second: @Composable () -> Unit) {
    if (stacked) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { first(); second() }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { first() }
            Box(Modifier.weight(1f)) { second() }
        }
    }
}

@Composable
private fun PeriodPanel(title: String, payment: String, term: String, rate: String) {
    Column(Modifier.fillMaxWidth().background(tileBrush(), RoundedCornerShape(14.dp)).padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(4.dp))
        MetricTile(Icons.Rounded.AccountBalanceWallet, "Платеж", payment, inset = true)
        MetricTile(Icons.Rounded.CalendarMonth, "Срок", term, inset = true)
        MetricTile(Icons.Rounded.Percent, "Ставка", rate, inset = true)
    }
}

@Composable
private fun MetricTile(icon: ImageVector, label: String, value: String, inset: Boolean = false) {
    BoxWithConstraints(Modifier.fillMaxWidth().background(
        if (inset) Brush.linearGradient(listOf(MaterialTheme.colorScheme.surface.copy(alpha = 0.65f), MaterialTheme.colorScheme.surface.copy(alpha = 0.35f))) else tileBrush(),
        RoundedCornerShape(13.dp)).padding(if (inset) 8.dp else 10.dp)) {
        val compact = maxWidth < 180.dp
        Row(Modifier.heightIn(min = if (inset) 40.dp else 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 16.dp)) {
            Box(Modifier.size(if (compact) 34.dp else 44.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.025f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(if (compact) 23.dp else 27.dp), tint = MaterialTheme.colorScheme.onSurface)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(label, fontSize = if (compact) 11.sp else 13.sp, color = labelColor(), lineHeight = 14.sp)
                FittingValue(value, if (compact) 17 else 20)
            }
        }
    }
}

@Composable
private fun FittingValue(value: String, preferredSize: Int) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints {
        val available = with(density) { maxWidth.toPx() }
        val size = (preferredSize downTo 12).firstOrNull { candidate ->
            measurer.measure(AnnotatedString(value), TextStyle(fontSize = candidate.sp, fontWeight = FontWeight.SemiBold)).size.width <= available
        } ?: 12
        Text(value, fontSize = size.sp, fontWeight = FontWeight.SemiBold, lineHeight = (size + 3).sp, letterSpacing = 0.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryBadges(downPayment: String, paymentType: String, modifier: String, hasModifier: Boolean) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 2.dp)) {
        val available = with(density) { (maxWidth - 124.dp).toPx() }
        val size = (11 downTo 9).firstOrNull { candidate ->
            listOf(downPayment, paymentType, modifier).sumOf { label ->
                measurer.measure(AnnotatedString(label), TextStyle(fontSize = candidate.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.sp)).size.width
            } <= available
        } ?: 11
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SummaryBadge(Icons.Rounded.PieChart, downPayment, size, Color(0xFF08794F), Color(0xFFE4FAEF))
            SummaryBadge(Icons.AutoMirrored.Rounded.ReceiptLong, paymentType, size, Color(0xFF0059B8), Color(0xFFEAF4FF))
            SummaryBadge(Icons.Rounded.BarChart, modifier, size,
                if (hasModifier) Color(0xFF7138AC) else Color(0xFF5C5A73),
                if (hasModifier) Color(0xFFF2E9FF) else Color(0xFFF0F0F8))
        }
    }
}

@Composable
private fun SummaryBadge(icon: ImageVector, label: String, fontSize: Int, foreground: Color, background: Color) {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    Row(Modifier.background(if (dark) foreground.copy(alpha = 0.24f) else background, RoundedCornerShape(12.dp))
        .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        val tint = if (dark) background else foreground
        Icon(icon, null, Modifier.size(16.dp), tint = tint)
        Text(label, fontSize = fontSize.sp, fontWeight = FontWeight.Medium, color = tint, letterSpacing = 0.sp)
    }
}

@Composable
private fun labelColor() = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) Color(0xFF73758F) else MaterialTheme.colorScheme.onSurfaceVariant

@Composable
internal fun listBackground() = if (MaterialTheme.colorScheme.surface.luminance() > 0.5f) Color(0xFFF3F7FD) else MaterialTheme.colorScheme.background

@Composable
private fun tileBrush(): Brush {
    val dark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
    return Brush.linearGradient(if (dark) listOf(Color(0xFF252C38), Color(0xFF202632)) else listOf(Color(0xFFF0F6FD), Color(0xFFF5F8FC)))
}

private fun updatedLabel(timestamp: Long): String {
    val day = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance()
    fun sameDay(other: Calendar) = day.get(Calendar.ERA) == other.get(Calendar.ERA) && day.get(Calendar.YEAR) == other.get(Calendar.YEAR) && day.get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
    if (sameDay(today)) return "Обновлено сегодня"
    today.add(Calendar.DAY_OF_YEAR, -1)
    if (sameDay(today)) return "Обновлено вчера"
    return "Обновлено ${SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(day.time)}"
}

private fun formatTerm(months: Int): String {
    val years = months / 12
    val remainder = months % 12
    return when {
        years == 0 -> "$remainder мес."
        remainder == 0 -> "$years ${formatYearsLabel(years)}"
        else -> "$years ${formatYearsLabel(years)} $remainder мес."
    }
}
