package com.ssafy.dib.feature.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ssafy.dib.domain.product.ProductAttributeSpec
import com.ssafy.dib.ui.theme.WireframeColors as Colors
import java.time.LocalDate
import java.time.Year

internal fun productYearValid(value: String): Boolean =
    value.isBlank() || value.toIntOrNull() in 1900..2100

internal fun purchaseYearValid(value: String): Boolean =
    value.isBlank() || value.toIntOrNull() in 1900..Year.now().value

internal fun productAttributeError(spec: ProductAttributeSpec, value: String): String? {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return if (spec.required) "${spec.label} 항목을 확인해주세요" else null
    if (trimmed.length > 100) return "100자 이내로 입력해주세요"
    return when (spec.type) {
        "YEAR" -> if (!productYearValid(trimmed)) "1900~2100년 사이의 연도를 입력해주세요" else null
        "DATE" -> {
            val date = runCatching { LocalDate.parse(trimmed) }.getOrNull()
            if (date == null || date.isBefore(LocalDate.now())) "오늘 이후의 날짜를 YYYY-MM-DD 형식으로 입력해주세요" else null
        }
        "CONFIRM" -> if (trimmed != "true") "${spec.label} 항목을 확인해주세요" else null
        else -> null
    }
}

internal fun productAttributesValid(specs: List<ProductAttributeSpec>, values: Map<String, String>): Boolean =
    specs.all { productAttributeError(it, values[it.key].orEmpty()) == null }

internal fun normalizedProductAttributes(specs: List<ProductAttributeSpec>, values: Map<String, String>): Map<String, String> =
    specs.mapNotNull { spec -> values[spec.key]?.trim()?.takeIf(String::isNotEmpty)?.let { spec.key to it } }.toMap()

@Composable
internal fun CategoryProductFields(
    specs: List<ProductAttributeSpec>,
    values: Map<String, String>,
    onValueChange: (String, String) -> Unit,
    showRequiredErrors: Boolean
) {
    specs.forEach { spec ->
        val value = values[spec.key].orEmpty()
        val error = productAttributeError(spec, value)?.takeIf { showRequiredErrors || value.isNotBlank() }
        Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
            Text("${spec.label}${if (spec.required) " *" else " (선택)"}", color = Colors.Text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            if (spec.type == "CONFIRM") {
                Row(
                    Modifier.fillMaxWidth().clickable { onValueChange(spec.key, if (value == "true") "" else "true") },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = value == "true", onCheckedChange = { onValueChange(spec.key, if (it) "true" else "") })
                    Spacer(Modifier.width(8.dp))
                    Text("확인했어요", color = Colors.Text)
                }
            } else {
                OutlinedTextField(
                    value = value,
                    onValueChange = { input ->
                        val filtered = if (spec.type == "YEAR") input.filter(Char::isDigit).take(4) else input.take(100)
                        onValueChange(spec.key, filtered)
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    placeholder = { Text(spec.placeholder) },
                    singleLine = true,
                    isError = error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = if (spec.type == "YEAR") KeyboardType.Number else KeyboardType.Text)
                )
            }
            error?.let { Text(it, color = Colors.Urgent, fontSize = 11.sp) }
        }
    }
}

internal fun displayProductAttribute(spec: ProductAttributeSpec, value: String): String = when (spec.type) {
    "YEAR" -> "${value}년"
    "CONFIRM" -> if (value == "true") "확인됨" else "확인되지 않음"
    else -> value
}
