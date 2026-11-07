package com.animeav.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.animeav.tv.ui.theme.Background
import com.animeav.tv.ui.theme.TextSecondary

@Composable
fun AppTopBar(
    currentRoute: String,
    onNavigate: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Background)
            .focusGroup()
            .padding(horizontal = 40.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BrandLogo()
        Spacer(Modifier.width(36.dp))
        NavTab("Inicio", currentRoute == "home") { onNavigate("home") }
        NavTab("Catálogo", currentRoute == "catalog") { onNavigate("catalog") }
        NavTab("Buscar", currentRoute == "search") { onNavigate("search") }
        NavTab("Mi lista", currentRoute == "library") { onNavigate("library") }
        NavTab("Descargas", currentRoute == "downloads") { onNavigate("downloads") }
        NavTab("Ajustes", currentRoute == "settings") { onNavigate("settings") }
        Spacer(Modifier.weight(1f))
        Text(
            text = "Sub Español",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            maxLines = 1,
        )
    }
}
