package com.tplatt.shadowshaper

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Reused by collection, deck, and in-battle card inspection. */
@Composable
internal fun ShadowKeywordDefinitions(
    keywords: List<ShadowKeywordGlossary.Entry>,
    accent: Color,
    foreground: Color,
    secondary: Color
) {
    Column(Modifier.fillMaxWidth()) {
        keywords.forEach { entry ->
            Spacer(Modifier.height(7.dp))
            Text(
                entry.name.uppercase(), color = accent,
                fontWeight = FontWeight.Bold, fontSize = 12.sp
            )
            Text(
                entry.definition, color = foreground,
                fontSize = 12.sp, lineHeight = 17.sp
            )
            if (!entry.recordedTooltip) {
                Text(
                    "Simulator mechanic explanation", color = secondary,
                    fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}
