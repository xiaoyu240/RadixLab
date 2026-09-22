package com.radixlab.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.radixlab.app.data.model.ConversionRecord
import com.radixlab.app.data.model.RecordType
import com.radixlab.app.ui.theme.CardShape
import com.radixlab.app.ui.theme.Dimens
import com.radixlab.app.ui.theme.MonoLabel
import com.radixlab.app.ui.theme.MonoSmall
import com.radixlab.app.ui.theme.RadiusSmall
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")

private fun formatTime(timestamp: Long): String =
    Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()).format(TIME_FORMATTER)

/**
 * 单条历史记录。
 * 点击回填到转换页，右侧提供收藏与删除。圆角 20、1px 描边、等宽数值。
 */
@Composable
fun HistoryItem(
    record: ConversionRecord,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = Dimens.CardPadding,
                    top = Dimens.Space3,
                    bottom = Dimens.Space3,
                    end = Dimens.Space1
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimens.Space1)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TypeBadge(type = record.type)
                    Text(
                        text = record.expression,
                        style = MonoLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = Dimens.Space2)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = formatTime(record.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = record.input,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = record.output,
                    style = MonoSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier.size(Dimens.TouchTarget)
            ) {
                Icon(
                    imageVector = if (record.favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = if (record.favorite) "取消收藏" else "收藏",
                    tint = if (record.favorite) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(Dimens.IconMedium)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(Dimens.TouchTarget)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Delete,
                    contentDescription = "删除这条记录",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(Dimens.IconMedium)
                )
            }
        }
    }
}

/** 类型徽标：RADIX / IP，8dp 小圆角 + 品牌浅底 */
@Composable
private fun TypeBadge(type: RecordType) {
    val text = when (type) {
        RecordType.RADIX -> "RADIX"
        RecordType.IP -> "IP"
    }
    Text(
        text = text,
        style = MonoLabel,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(RadiusSmall)
            )
            .padding(
                start = Dimens.Space2,
                end = Dimens.Space2,
                top = Dimens.Space1,
                bottom = Dimens.Space1
            )
    )
}
