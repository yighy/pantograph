package com.yighy.pantograph.drawing

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.yighy.pantograph.data.LayerEntity
import com.yighy.pantograph.ui.theme.MotionTokens
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** A collapsed row's height, and the gap between rows: the drag reorder counts in their sum. */
private val RowHeight = 56.dp
private val RowGap = 4.dp

/**
 * The layers, as a list under the top buttons: each one named, with its blend and opacity
 * said in words, the top of the stack first. The layer in hand opens up in place to its
 * settings, so there is no second panel to open and close beside this one.
 *
 * Long-press a row and drag to move it in the stack. While a row is being dragged the open
 * one folds shut, so every row is the same height and the drop lands where it looks like it
 * will.
 */
@Composable
fun LayersListPanel(viewModel: DrawingViewModel) {
    val layers by remember(viewModel) { viewModel.uiState.map { it.layers }.distinctUntilChanged() }.collectAsState(emptyList())
    val activeLayerId by remember(viewModel) { viewModel.uiState.map { it.activeLayerId }.distinctUntilChanged() }.collectAsState(-1L)
    val layerBitmaps by remember(viewModel) { viewModel.uiState.map { it.layerBitmaps }.distinctUntilChanged() }.collectAsState(emptyMap())
    val renderVersion by remember(viewModel) { viewModel.uiState.map { it.renderVersion }.distinctUntilChanged() }.collectAsState(0)

    var draggingId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var localOrder by remember { mutableStateOf<List<LayerEntity>?>(null) }
    // Gesture lambdas are cached by pointerInput: they read the current list through these,
    // or a reorder would leave them working on a stale one.
    val currentLayers by rememberUpdatedState(layers)
    val listState = rememberLazyListState()

    // Once the database catches up after a drop, the local order retires.
    LaunchedEffect(layers) {
        if (draggingId == null) localOrder = null
    }
    // The layer in hand in view when the selection moves.
    LaunchedEffect(activeLayerId) {
        val index = layers.reversed().indexOfFirst { it.id == activeLayerId }
        if (index >= 0) listState.animateScrollToItem(index)
    }

    Surface(
        modifier = Modifier.width(260.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Layers", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.addLayer("New Layer") }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add layer", tint = MaterialTheme.colorScheme.primary)
                }
            }

            // Top of the stack first, as it sits on the canvas.
            val displayLayers = localOrder ?: layers.reversed()
            Box {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.heightIn(max = 460.dp),
                    verticalArrangement = Arrangement.spacedBy(RowGap)
                ) {
                    items(displayLayers, key = { it.id }) { layer ->
                        val isActive = layer.id == activeLayerId
                        val isDragging = draggingId == layer.id
                        LayerRow(
                            viewModel = viewModel,
                            layer = layer,
                            layers = layers,
                            bitmap = layerBitmaps[layer.id],
                            renderVersion = renderVersion,
                            isActive = isActive,
                            expanded = isActive && draggingId == null,
                            modifier = Modifier
                                // The dragged one follows the finger; a placement animation would
                                // fight the compensated translation each time it changes slot.
                                // The others slide only during a drag, as the dragged row passes
                                // them. Anywhere else a slide is a row lagging behind a neighbour
                                // that is opening or closing - picking another layer had the new
                                // one grow over the rows around it before they moved aside.
                                .then(
                                    if (isDragging) Modifier
                                    else Modifier.animateItem(placementSpec = if (draggingId != null) spring() else null)
                                )
                                .zIndex(if (isDragging) 1f else 0f)
                                .graphicsLayer {
                                    translationY = if (isDragging) dragOffsetY else 0f
                                    val scale = if (isDragging) 1.04f else 1f
                                    scaleX = scale
                                    scaleY = scale
                                }
                                .pointerInput(layer.id) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = {
                                            localOrder = currentLayers.reversed()
                                            draggingId = layer.id
                                            dragOffsetY = 0f
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            dragOffsetY += dragAmount.y
                                            val slotPx = (RowHeight + RowGap).toPx()
                                            var moved = true
                                            while (moved) {
                                                moved = false
                                                val order = localOrder ?: break
                                                val idx = order.indexOfFirst { it.id == layer.id }
                                                if (idx < 0) break
                                                if (dragOffsetY > slotPx * 0.5f && idx < order.lastIndex) {
                                                    localOrder = order.toMutableList().apply { add(idx + 1, removeAt(idx)) }
                                                    dragOffsetY -= slotPx
                                                    moved = true
                                                } else if (dragOffsetY < -slotPx * 0.5f && idx > 0) {
                                                    localOrder = order.toMutableList().apply { add(idx - 1, removeAt(idx)) }
                                                    dragOffsetY += slotPx
                                                    moved = true
                                                }
                                            }
                                        },
                                        onDragEnd = {
                                            val stack = currentLayers
                                            val visualFrom = stack.reversed().indexOfFirst { it.id == layer.id }
                                            val visualTo = localOrder?.indexOfFirst { it.id == layer.id } ?: -1
                                            draggingId = null
                                            dragOffsetY = 0f
                                            if (visualFrom >= 0 && visualTo >= 0 && visualTo != visualFrom) {
                                                // The list shows the stack top first; the stack counts from the bottom.
                                                viewModel.reorderLayers(stack.size - 1 - visualFrom, stack.size - 1 - visualTo)
                                            } else {
                                                localOrder = null
                                            }
                                        },
                                        onDragCancel = {
                                            draggingId = null
                                            dragOffsetY = 0f
                                            localOrder = null
                                        }
                                    )
                                }
                        )
                    }
                }
                ScrollHint(listState.canScrollBackward, ScrollHintEdge.Top)
                ScrollHint(listState.canScrollForward, ScrollHintEdge.Bottom)
            }
        }
    }
}

/** One layer: thumbnail, name, how it is drawn, its eye - and, when it is the one in hand, its settings. */
@Composable
private fun LayerRow(
    viewModel: DrawingViewModel,
    layer: LayerEntity,
    layers: List<LayerEntity>,
    bitmap: android.graphics.Bitmap?,
    renderVersion: Int,
    isActive: Boolean,
    expanded: Boolean,
    modifier: Modifier = Modifier
) {
    var showRenameDialog by remember { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme
    val borderColor by animateColorAsState(
        if (isActive) colors.primary else Color.Transparent,
        animationSpec = MotionTokens.colorTransition,
        label = "layerRowBorder"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(if (isActive) colors.surfaceContainerHighest else Color.Transparent)
            .border(1.5.dp, borderColor, MaterialTheme.shapes.large)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(RowHeight)
                .pointerInput(layer.id, isActive) {
                    detectTapGestures(onTap = {
                        // The name of the layer in hand renames it; anywhere else picks the row.
                        if (isActive) showRenameDialog = true else viewModel.selectLayer(layer.id)
                    })
                }
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(Color.White)
                    .border(0.5.dp, colors.outlineVariant, MaterialTheme.shapes.extraSmall)
            ) {
                bitmap?.let {
                    // Keyed on renderVersion: the bitmap is drawn into in place, so the
                    // thumbnail has to be told to repaint.
                    key(renderVersion) {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                }
                if (!layer.isVisible) {
                    Box(Modifier.fillMaxSize().background(colors.inverseSurface.copy(alpha = 0.4f)))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        layer.name,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isActive) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Rename layer", modifier = Modifier.size(13.dp), tint = colors.onSurfaceVariant)
                    }
                    if (layer.isLocked) Icon(Icons.Rounded.Lock, contentDescription = "Locked", modifier = Modifier.size(13.dp), tint = colors.onSurfaceVariant)
                    if (layer.isAlphaLocked) Icon(Icons.Rounded.Opacity, contentDescription = "Alpha locked", modifier = Modifier.size(13.dp), tint = colors.onSurfaceVariant)
                }
                Text(
                    "${layer.blend.label} · ${(layer.opacity * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    maxLines = 1
                )
            }
            IconButton(onClick = { viewModel.toggleLayerVisibility(layer) }) {
                Icon(
                    if (layer.isVisible) Icons.Rounded.Visibility else Icons.Rounded.VisibilityOff,
                    contentDescription = if (layer.isVisible) "Hide ${layer.name}" else "Show ${layer.name}",
                    tint = colors.onSurfaceVariant
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(MotionTokens.panelTransition) + fadeIn(MotionTokens.expressiveEnter),
            exit = shrinkVertically(MotionTokens.panelTransition) + fadeOut(MotionTokens.expressiveExit)
        ) {
            LayerOptions(viewModel, layer, layers)
        }
    }

    if (showRenameDialog) {
        var renameText by remember { mutableStateOf(layer.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename layer", style = MaterialTheme.typography.titleMedium) },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text("Layer name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = renameText.isNotBlank(),
                    onClick = {
                        viewModel.renameLayer(layer, renameText)
                        showRenameDialog = false
                    }
                ) { Text("Rename") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("Cancel") }
            }
        )
    }
}

/** The settings of the layer in hand, under its row: opacity, blend, and what can be done to it. */
@Composable
private fun LayerOptions(viewModel: DrawingViewModel, layer: LayerEntity, layers: List<LayerEntity>) {
    var showBlends by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.padding(start = 10.dp, end = 6.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Slider(
                value = layer.opacity,
                onValueChange = { viewModel.setLayerOpacity(layer, it) },
                onValueChangeFinished = { viewModel.persistLayerOpacity(layer.id) },
                modifier = Modifier.weight(1f)
            )
            // How the layer is drawn rather than what is on it, so like opacity it stays
            // available on a locked layer.
            Box {
                TextButton(onClick = { showBlends = true }, contentPadding = PaddingValues(horizontal = 6.dp)) {
                    Text(layer.blend.label, style = MaterialTheme.typography.labelMedium)
                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = showBlends, onDismissRequest = { showBlends = false }) {
                    LayerBlend.entries.forEach { blend ->
                        DropdownMenuItem(
                            text = { Text(blend.label) },
                            onClick = {
                                viewModel.setLayerBlend(layer, blend)
                                showBlends = false
                            },
                            trailingIcon = if (blend == layer.blend) {
                                { Icon(Icons.Rounded.Check, contentDescription = "Current") }
                            } else null
                        )
                    }
                }
            }
        }

        // What leaves the pixels alone first, then what rewrites or removes them. Those are
        // left out while the layer is locked rather than shown and refused: a control that
        // does nothing reads as the app being broken.
        val index = layers.indexOf(layer)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            LayerAction(
                if (layer.isLocked) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                if (layer.isLocked) "Unlock layer" else "Lock layer",
                on = layer.isLocked
            ) { viewModel.toggleLayerLock(layer) }
            LayerAction(
                Icons.Rounded.Opacity,
                if (layer.isAlphaLocked) "Alpha lock on" else "Alpha lock off",
                on = layer.isAlphaLocked
            ) { viewModel.toggleLayerAlphaLock(layer) }
            LayerAction(Icons.Rounded.ContentCopy, "Duplicate layer") { viewModel.duplicateLayer(layer) }
            if (!layer.isLocked) {
                if (index > 0) LayerAction(Icons.Rounded.Merge, "Merge down") { viewModel.mergeDown(layer) }
                LayerAction(Icons.Rounded.CleaningServices, "Clear layer") { viewModel.clearLayer(layer.id) }
                if (layers.size > 1) LayerAction(Icons.Rounded.Delete, "Delete layer", danger = true) { viewModel.deleteLayer(layer) }
            }
        }
    }
}

/** One icon button of the layer's actions; [on] tints it as a toggle that is set. */
@Composable
private fun RowScope.LayerAction(
    icon: ImageVector,
    description: String,
    on: Boolean = false,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier.weight(1f).height(40.dp),
        shape = MaterialTheme.shapes.medium,
        color = when {
            on -> colors.primaryContainer
            danger -> colors.errorContainer.copy(alpha = 0.2f)
            else -> colors.surfaceContainerHigh
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                icon,
                contentDescription = description,
                modifier = Modifier.size(18.dp),
                tint = when {
                    on -> colors.onPrimaryContainer
                    danger -> colors.error
                    else -> colors.onSurfaceVariant
                }
            )
        }
    }
}
