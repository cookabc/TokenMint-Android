package com.chuangcius.tokenmint.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.chuangcius.tokenmint.R
import com.chuangcius.tokenmint.data.model.Token
import com.chuangcius.tokenmint.service.TOTPService
import com.chuangcius.tokenmint.ui.theme.TokenMintAccent
import com.chuangcius.tokenmint.ui.theme.TokenMintSuccess
import com.chuangcius.tokenmint.ui.theme.TOTPCodeStyle
import kotlinx.coroutines.delay

/**
 * A single token row showing issuer, account, TOTP code with countdown ring,
 * copy-on-tap, and pin indicator.
 */
@Composable
fun TokenRow(
    token: Token,
    modifier: Modifier = Modifier
) {
    var code by remember { mutableStateOf("") }
    var remaining by remember { mutableIntStateOf(token.period) }
    var progress by remember { mutableFloatStateOf(0f) }
    var copied by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val view = LocalView.current
    val copyDesc = stringResource(R.string.cd_copy_code, token.issuer)

    // 1-second timer tick
    LaunchedEffect(token.id) {
        while (true) {
            val now = System.currentTimeMillis()
            code = TOTPService.generateCode(token, now)
            remaining = TOTPService.remainingSeconds(token.period, now)
            progress = TOTPService.progress(token.period, now)
            delay(1000)
        }
    }

    // Auto-hide "Copied!" after 2s
    LaunchedEffect(copied) {
        if (copied) {
            delay(2000)
            copied = false
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("TOTP", code))
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                copied = true
            }
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics { contentDescription = copyDesc },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: issuer + account
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (token.isPinned) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        tint = TokenMintAccent,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                Text(
                    text = token.issuer,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
            }
            if (token.account.isNotEmpty()) {
                Text(
                    text = token.account,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1
                )
            }
        }

        // Right: TOTP code + countdown
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End
        ) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = formatCode(code),
                    style = TOTPCodeStyle,
                    color = MaterialTheme.colorScheme.onSurface
                )
                AnimatedVisibility(
                    visible = copied,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    Text(
                        text = stringResource(R.string.copied),
                        style = MaterialTheme.typography.labelSmall,
                        color = TokenMintSuccess
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            CountdownRing(progress = progress, remaining = remaining)
        }
    }
}

/** Format TOTP code with space in the middle: "123 456" */
private fun formatCode(code: String): String {
    if (code.length < 6) return code
    val mid = code.length / 2
    return "${code.substring(0, mid)} ${code.substring(mid)}"
}
