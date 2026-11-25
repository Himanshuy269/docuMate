package com.example.documate.feature.home.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Compress
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Slideshow
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.documate.core.common.formatFileSize
import com.example.documate.core.common.formatTimestamp
import com.example.documate.core.domain.model.ConversionRecord
import com.example.documate.core.domain.model.DocumentTool
import com.example.documate.core.ui.AppViewModelProvider

@Composable
fun HomeScreen(
    onNavigateToTool: (DocumentTool) -> Unit,
    onNavigateToHistory: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Top App Bar Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "docu",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "MATE",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "A little less paperwork.",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Notification Button
                    Surface(
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(2.dp, RoundedCornerShape(16.dp), spotColor = Color(0x1A000000))
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { /* Notification handler */ },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Notifications",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                            // Subtle notification indicator dot
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .align(Alignment.TopEnd)
                                    .padding(top = 10.dp, end = 11.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape)
                            )
                        }
                    }
                }
            }

            // Hero Banner
            item {
                HeroBanner(
                    onCreatePdfClick = { onNavigateToTool(DocumentTool.IMAGES_TO_PDF) }
                )
            }

            // "All tools" Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "All tools",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "A tool for every task",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 6 Tools Grid (2 columns)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    // Row 1: Image to PDF & Add images
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            ToolCardItem(
                                title = "Image to PDF",
                                subtitle = "Turn photos into pages",
                                icon = Icons.Outlined.Image,
                                iconBgColor = Color(0xFFEEF2FF),
                                iconTint = Color(0xFF4F46E5),
                                onClick = { onNavigateToTool(DocumentTool.IMAGES_TO_PDF) }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            ToolCardItem(
                                title = "Add images",
                                subtitle = "To an existing PDF",
                                icon = Icons.Outlined.AddPhotoAlternate,
                                iconBgColor = Color(0xFFF3E8FF),
                                iconTint = Color(0xFF9333EA),
                                onClick = { onNavigateToTool(DocumentTool.ADD_IMAGES_TO_PDF) }
                            )
                        }
                    }

                    // Row 2: Excel to PDF & Word to PDF
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            ToolCardItem(
                                title = "Excel to PDF",
                                subtitle = "Spreadsheets, simplified",
                                icon = Icons.Outlined.TableChart,
                                iconBgColor = Color(0xFFECFDF5),
                                iconTint = Color(0xFF10B981),
                                onClick = { onNavigateToTool(DocumentTool.EXCEL_TO_PDF) }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            ToolCardItem(
                                title = "Word to PDF",
                                subtitle = "Keep every detail",
                                icon = Icons.Outlined.Description,
                                iconBgColor = Color(0xFFEFF6FF),
                                iconTint = Color(0xFF3B82F6),
                                onClick = { onNavigateToTool(DocumentTool.WORD_TO_PDF) }
                            )
                        }
                    }

                    // Row 3: PowerPoint to PDF & Compress PDF
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            ToolCardItem(
                                title = "PowerPoint to PDF",
                                subtitle = "Slides ready to share",
                                icon = Icons.Outlined.Slideshow,
                                iconBgColor = Color(0xFFFFF7ED),
                                iconTint = Color(0xFFF97316),
                                onClick = { onNavigateToTool(DocumentTool.POWERPOINT_TO_PDF) }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            ToolCardItem(
                                title = "Compress PDF",
                                subtitle = "Less size. Same quality.",
                                icon = Icons.Outlined.Compress,
                                iconBgColor = Color(0xFFF3E8FF),
                                iconTint = Color(0xFF8B5CF6),
                                onClick = { onNavigateToTool(DocumentTool.COMPRESS_PDF) }
                            )
                        }
                    }
                }
            }

            // "Recently opened" Section Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recently opened",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "View all",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onNavigateToHistory() }
                    )
                }
            }

            // Recently Opened Item Card
            item {
                if (uiState.recentConversions.isNotEmpty()) {
                    val firstRecord = uiState.recentConversions.first()
                    RecentlyOpenedCard(
                        record = firstRecord,
                        onClick = onNavigateToHistory
                    )
                } else {
                    // Default preview card matching UI design
                    DefaultRecentCard(onClick = onNavigateToHistory)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun HeroBanner(
    onCreatePdfClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 22.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1.3f)) {
                Text(
                    text = "Goodbye busywork.\nHello, done.",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 28.sp
                )
                Spacer(modifier = Modifier.height(18.dp))
                // White pill button
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { onCreatePdfClick() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Create a PDF",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Floating Layered Document Graphic
            Box(
                modifier = Modifier
                    .weight(0.9f)
                    .height(120.dp),
                contentAlignment = Alignment.Center
            ) {
                // Background subtle tilted card
                Box(
                    modifier = Modifier
                        .size(width = 82.dp, height = 104.dp)
                        .rotate(8f)
                        .background(Color(0x33FFFFFF), RoundedCornerShape(16.dp))
                )
                // Foreground white document card
                Surface(
                    modifier = Modifier
                        .size(width = 86.dp, height = 108.dp)
                        .shadow(6.dp, RoundedCornerShape(16.dp), spotColor = Color(0x33000000)),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "PDF",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        // Simulated document lines
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(5.dp)
                                .background(Color(0xFFE2E8F0), CircleShape)
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.85f)
                                .height(5.dp)
                                .background(Color(0xFFE2E8F0), CircleShape)
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(5.dp)
                                .background(Color(0xFFE2E8F0), CircleShape)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ToolCardItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = RoundedCornerShape(14.dp),
                color = iconBgColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RecentlyOpenedCard(
    record: ConversionRecord,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFEDE9FE)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "PDF",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.outputFileName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                val dateStr = formatTimestamp(record.timestampMillis)
                val sizeStr = if (record.outputSizeBytes > 0) {
                    formatFileSize(record.outputSizeBytes)
                } else "2.4 MB"
                Text(
                    text = "Today · $sizeStr",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = "Options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun DefaultRecentCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFEDE9FE)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "PDF",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Project proposal.pdf",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "Today · 2.4 MB",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = "Options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
