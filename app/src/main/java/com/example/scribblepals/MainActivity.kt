package com.example.scribblepals

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.awaitEachGesture
import androidx.compose.ui.input.pointer.awaitFirstDown
import androidx.compose.ui.input.pointer.awaitPointerEvent
import androidx.compose.ui.input.pointer.consume
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScribblePalsApp()
        }
    }
}

private enum class GameMode { DRAW, PUZZLE }

private data class DrawStroke(
    val points: List<Offset>,
    val color: Color,
    val strokeWidth: Float
)

private data class PuzzlePrompt(
    val title: String,
    val encouragement: String,
    val hint: String,
    val ghost: (DrawScope) -> Unit
)

@Composable
private fun ScribblePalsApp() {
    MaterialTheme {
        var mode by remember { mutableStateOf(GameMode.PUZZLE) }
        var currentPromptIndex by remember { mutableStateOf(0) }
        val prompts = remember { defaultPrompts() }
        val strokes = remember { mutableStateListOf<DrawStroke>() }
        val currentPoints = remember { mutableStateListOf<Offset>() }
        var brushSize by remember { mutableFloatStateOf(32f) }
        var currentColor by remember { mutableStateOf(Color(0xFF1E293B)) }

        fun commitCurrentStroke() {
            if (currentPoints.size > 1) {
                strokes.add(
                    DrawStroke(
                        points = currentPoints.toList(),
                        color = currentColor,
                        strokeWidth = brushSize
                    )
                )
            }
            currentPoints.clear()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(text = "Scribble Pals") }
                )
            },
            containerColor = Color(0xFFFFF8EE)
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFE2B5)),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = when (mode) {
                                GameMode.DRAW -> "Let's make something magical!"
                                GameMode.PUZZLE -> prompts[currentPromptIndex].title
                            },
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = when (mode) {
                                GameMode.DRAW -> "Pick your favorite colors, swirl your brush, and see what pops into life!"
                                GameMode.PUZZLE -> prompts[currentPromptIndex].encouragement
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            ModeChip(
                                icon = Icons.Default.Brush,
                                label = "Drawing",
                                selected = mode == GameMode.DRAW,
                                onClick = { mode = GameMode.DRAW }
                            )
                            ModeChip(
                                icon = Icons.Default.AutoAwesome,
                                label = "Puzzle",
                                selected = mode == GameMode.PUZZLE,
                                onClick = { mode = GameMode.PUZZLE }
                            )
                            Button(onClick = {
                                strokes.clear()
                                currentPoints.clear()
                            }) {
                                Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear")
                                Spacer(Modifier.size(8.dp))
                                Text(text = "Clear")
                            }
                            Button(
                                onClick = {
                                    if (strokes.isNotEmpty()) {
                                        strokes.removeLast()
                                    }
                                }
                            ) {
                                Icon(imageVector = Icons.Default.Redo, contentDescription = "Undo")
                                Spacer(Modifier.size(8.dp))
                                Text(text = "Undo")
                            }
                        }
                    }
                }

                DrawingSurface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    strokes = strokes,
                    currentPoints = currentPoints,
                    currentColor = currentColor,
                    brushSize = brushSize,
                    ghostPrompt = prompts[currentPromptIndex].ghost.takeIf { mode == GameMode.PUZZLE },
                    onDragStart = { offset ->
                        currentPoints.clear()
                        currentPoints.add(offset)
                    },
                    onDrag = { change ->
                        change.consume()
                        currentPoints.add(change.position)
                    },
                    onDragEnd = {
                        commitCurrentStroke()
                    },
                    onTap = { _ ->
                        if (mode == GameMode.PUZZLE) {
                            // taps in puzzle mode don't start drawing to prevent accidental scribbles
                        }
                    }
                )

                if (mode == GameMode.PUZZLE) {
                    PromptFooter(
                        prompt = prompts[currentPromptIndex],
                        onNextPrompt = {
                            currentPromptIndex = (currentPromptIndex + 1) % prompts.size
                            strokes.clear()
                            currentPoints.clear()
                        }
                    )
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                }

                BrushControls(
                    brushSize = brushSize,
                    onBrushSizeChange = { brushSize = it },
                    selectedColor = currentColor,
                    onColorSelected = { currentColor = it }
                )
            }
        }
    }
}

@Composable
private fun ModeChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val background = if (selected) Color(0xFFFEF08A) else Color.White
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(containerColor = background)
    ) {
        Icon(imageVector = icon, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text(text = label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
private fun BrushControls(
    brushSize: Float,
    onBrushSizeChange: (Float) -> Unit,
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Pick a color", style = MaterialTheme.typography.titleMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(colorPalette) { color ->
                    ColorSwatch(color = color, selected = color == selectedColor) {
                        onColorSelected(color)
                    }
                }
            }
            Text(text = "Brush Size", style = MaterialTheme.typography.titleMedium)
            Slider(
                value = brushSize,
                onValueChange = onBrushSizeChange,
                valueRange = 12f..72f
            )
        }
    }
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    val borderWidth = if (selected) 6.dp else 2.dp
    Box(
        modifier = Modifier
            .size(56.dp)
            .background(color, CircleShape)
            .border(borderWidth, if (selected) Color.White else Color(0xFFCBD5F5), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Text("★", color = Color.White, fontSize = 18.sp)
        }
    }
}

@Composable
private fun PromptFooter(prompt: PuzzlePrompt, onNextPrompt: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFDBEAFE))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(text = "Can you draw this?", style = MaterialTheme.typography.titleMedium)
            Text(text = prompt.hint, fontSize = 18.sp)
            Button(onClick = onNextPrompt) {
                Text(text = "New Prompt")
            }
        }
    }
}

@Composable
private fun DrawingSurface(
    modifier: Modifier,
    strokes: List<DrawStroke>,
    currentPoints: List<Offset>,
    currentColor: Color,
    brushSize: Float,
    ghostPrompt: ((DrawScope) -> Unit)?,
    onDragStart: (Offset) -> Unit,
    onDrag: (PointerInputChange) -> Unit,
    onDragEnd: () -> Unit,
    onTap: (Offset) -> Unit
) {
    Box(
        modifier = modifier
            .background(Color(0xFFFFFFFF), shape = RoundedCornerShape(32.dp))
            .border(6.dp, Color(0xFFFB923C), RoundedCornerShape(32.dp))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFFFFFFF), shape = RoundedCornerShape(24.dp))
                .pointerInput(ghostPrompt, brushSize, currentColor) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val pointerId = down.id
                        var moved = false
                        onDragStart(down.position)
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId }
                            if (change == null) {
                                onDragEnd()
                                break
                            }
                            if (change.pressed) {
                                if (change.positionChange() != Offset.Zero) {
                                    moved = true
                                    onDrag(change)
                                }
                                change.consume()
                            } else {
                                if (!moved) {
                                    onTap(change.position)
                                }
                                onDragEnd()
                                break
                            }
                        }
                    }
                }
        ) {
            ghostPrompt?.invoke(this)
            strokes.forEach { stroke ->
                drawStroke(stroke)
            }
            if (currentPoints.isNotEmpty()) {
                drawStroke(
                    DrawStroke(
                        points = currentPoints,
                        color = currentColor,
                        strokeWidth = brushSize
                    )
                )
            }
        }
    }
}

private fun DrawScope.drawStroke(stroke: DrawStroke) {
    if (stroke.points.size < 2) return
    val path = Path().apply {
        moveTo(stroke.points.first().x, stroke.points.first().y)
        for (i in 1 until stroke.points.size) {
            lineTo(stroke.points[i].x, stroke.points[i].y)
        }
    }
    drawPath(
        path = path,
        color = stroke.color,
        style = Stroke(width = stroke.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    )
}

private val colorPalette = listOf(
    Color(0xFF1E293B),
    Color(0xFFEF4444),
    Color(0xFFF97316),
    Color(0xFFFACC15),
    Color(0xFF22C55E),
    Color(0xFF14B8A6),
    Color(0xFF3B82F6),
    Color(0xFFA855F7),
    Color(0xFFEC4899)
)

private fun defaultPrompts(): List<PuzzlePrompt> {
    return listOf(
        PuzzlePrompt(
            title = "Sunny Smiles",
            encouragement = "Can you make the sun shine bright with happy rays?",
            hint = "Draw a big circle. Add little lines all around to make the sunbeams!",
            ghost = { scope -> scope.drawSunGhost() }
        ),
        PuzzlePrompt(
            title = "Rocket Adventure",
            encouragement = "Help the rocket zoom to the stars!",
            hint = "Start with a tall triangle, add a rectangle body, and fiery zig-zags at the bottom.",
            ghost = { scope -> scope.drawRocketGhost() }
        ),
        PuzzlePrompt(
            title = "Friendly Dinosaur",
            encouragement = "This dino loves leafy snacks. Give it a big smile!",
            hint = "Begin with a long oval body, add a bumpy tail, and tiny triangles for plates.",
            ghost = { scope -> scope.drawDinoGhost() }
        ),
        PuzzlePrompt(
            title = "Cozy House",
            encouragement = "Someone wants to live here! Draw doors, windows, and maybe a chimney.",
            hint = "Make a square for the house, a triangle roof, then add your favorite decorations.",
            ghost = { scope -> scope.drawHouseGhost() }
        ),
        PuzzlePrompt(
            title = "Rainbow Dragon",
            encouragement = "Give the dragon colorful scales and a twirly tail!",
            hint = "Curve a long body, add wings, and top it off with bright rainbow spots.",
            ghost = { scope -> scope.drawDragonGhost() }
        )
    )
}

private fun DrawScope.drawSunGhost() {
    val radius = size.minDimension * 0.25f
    val center = Offset(size.width / 2f, size.height / 2f)
    drawCircle(
        color = Color(0x33F59E0B),
        radius = radius,
        center = center,
        style = Stroke(width = 12f, cap = StrokeCap.Round)
    )
    repeat(12) { index ->
        val angle = (Math.PI * 2 / 12 * index).toFloat()
        val start = Offset(
            x = center.x + radius * kotlin.math.cos(angle.toDouble()).toFloat(),
            y = center.y + radius * kotlin.math.sin(angle.toDouble()).toFloat()
        )
        val end = Offset(
            x = center.x + (radius + 60f) * kotlin.math.cos(angle.toDouble()).toFloat(),
            y = center.y + (radius + 60f) * kotlin.math.sin(angle.toDouble()).toFloat()
        )
        drawLine(color = Color(0x33F97316), start = start, end = end, strokeWidth = 12f, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawRocketGhost() {
    val width = size.minDimension * 0.35f
    val height = size.minDimension * 0.55f
    val center = Offset(size.width / 2f, size.height / 2f)
    val top = Offset(center.x, center.y - height / 2)
    val baseY = center.y + height / 2
    val leftX = center.x - width / 2
    val rightX = center.x + width / 2

    val nosePath = Path().apply {
        moveTo(center.x, top.y)
        lineTo(rightX, center.y - height * 0.25f)
        lineTo(leftX, center.y - height * 0.25f)
        close()
    }
    drawPath(nosePath, Color(0x3325A6F7), style = Stroke(width = 10f))
    drawRect(
        color = Color(0x3325A6F7),
        topLeft = Offset(leftX, center.y - height * 0.25f),
        size = androidx.compose.ui.geometry.Size(width, height * 0.5f),
        style = Stroke(width = 10f)
    )
    drawOval(
        color = Color(0x3325A6F7),
        topLeft = Offset(center.x - width * 0.2f, center.y - height * 0.05f),
        size = androidx.compose.ui.geometry.Size(width * 0.4f, height * 0.2f),
        style = Stroke(width = 10f)
    )
    drawLine(
        color = Color(0x33F97316),
        start = Offset(leftX, baseY),
        end = Offset(center.x - width * 0.4f, baseY + 80f),
        strokeWidth = 12f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0x33F97316),
        start = Offset(rightX, baseY),
        end = Offset(center.x + width * 0.4f, baseY + 80f),
        strokeWidth = 12f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawDinoGhost() {
    val bodyWidth = size.minDimension * 0.5f
    val bodyHeight = size.minDimension * 0.3f
    val center = Offset(size.width / 2f, size.height / 2f)
    val bodyTopLeft = Offset(center.x - bodyWidth / 2, center.y - bodyHeight / 2)
    drawRoundRect(
        color = Color(0x3322C55E),
        topLeft = bodyTopLeft,
        size = androidx.compose.ui.geometry.Size(bodyWidth, bodyHeight),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(120f, 120f),
        style = Stroke(width = 12f)
    )
    drawCircle(
        color = Color(0x3322C55E),
        radius = bodyHeight * 0.6f,
        center = Offset(center.x + bodyWidth / 2, center.y - bodyHeight * 0.3f),
        style = Stroke(width = 12f)
    )
    repeat(5) { index ->
        val startX = bodyTopLeft.x + index * (bodyWidth / 4f)
        drawLine(
            color = Color(0x33FACC15),
            start = Offset(startX, bodyTopLeft.y - 20f),
            end = Offset(startX + bodyWidth / 8f, bodyTopLeft.y - 60f),
            strokeWidth = 10f,
            cap = StrokeCap.Round
        )
    }
    drawLine(
        color = Color(0xFF22C55E).copy(alpha = 0.2f),
        start = Offset(bodyTopLeft.x + bodyWidth * 0.1f, bodyTopLeft.y + bodyHeight),
        end = Offset(bodyTopLeft.x + bodyWidth * 0.1f, bodyTopLeft.y + bodyHeight + 80f),
        strokeWidth = 12f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0xFF22C55E).copy(alpha = 0.2f),
        start = Offset(bodyTopLeft.x + bodyWidth * 0.5f, bodyTopLeft.y + bodyHeight),
        end = Offset(bodyTopLeft.x + bodyWidth * 0.5f, bodyTopLeft.y + bodyHeight + 80f),
        strokeWidth = 12f,
        cap = StrokeCap.Round
    )
}

private fun DrawScope.drawHouseGhost() {
    val houseWidth = size.minDimension * 0.5f
    val houseHeight = size.minDimension * 0.35f
    val center = Offset(size.width / 2f, size.height / 2f)
    val houseTopLeft = Offset(center.x - houseWidth / 2, center.y - houseHeight / 2)
    drawRect(
        color = Color(0x33F97316),
        topLeft = houseTopLeft,
        size = androidx.compose.ui.geometry.Size(houseWidth, houseHeight),
        style = Stroke(width = 12f)
    )
    val roofPath = Path().apply {
        moveTo(center.x, houseTopLeft.y - houseHeight * 0.6f)
        lineTo(houseTopLeft.x - houseWidth * 0.1f, houseTopLeft.y)
        lineTo(houseTopLeft.x + houseWidth + houseWidth * 0.1f, houseTopLeft.y)
        close()
    }
    drawPath(roofPath, Color(0x33F97316), style = Stroke(width = 12f))
    drawRect(
        color = Color(0x3325A6F7),
        topLeft = Offset(center.x - houseWidth * 0.15f, houseTopLeft.y + houseHeight * 0.45f),
        size = androidx.compose.ui.geometry.Size(houseWidth * 0.3f, houseHeight * 0.55f),
        style = Stroke(width = 10f)
    )
    drawRect(
        color = Color(0x3325A6F7),
        topLeft = Offset(houseTopLeft.x + houseWidth * 0.1f, houseTopLeft.y + houseHeight * 0.2f),
        size = androidx.compose.ui.geometry.Size(houseWidth * 0.2f, houseHeight * 0.2f),
        style = Stroke(width = 10f)
    )
    drawRect(
        color = Color(0x3325A6F7),
        topLeft = Offset(houseTopLeft.x + houseWidth * 0.7f, houseTopLeft.y + houseHeight * 0.2f),
        size = androidx.compose.ui.geometry.Size(houseWidth * 0.2f, houseHeight * 0.2f),
        style = Stroke(width = 10f)
    )
}

private fun DrawScope.drawDragonGhost() {
    val bodyPath = Path().apply {
        moveTo(size.width * 0.15f, size.height * 0.7f)
        quadraticBezierTo(
            size.width * 0.25f,
            size.height * 0.4f,
            size.width * 0.5f,
            size.height * 0.55f
        )
        quadraticBezierTo(
            size.width * 0.8f,
            size.height * 0.75f,
            size.width * 0.85f,
            size.height * 0.35f
        )
    }
    drawPath(bodyPath, Color(0x33EC4899), style = Stroke(width = 12f))
    drawCircle(
        color = Color(0x33EC4899),
        radius = min(size.width, size.height) * 0.08f,
        center = Offset(size.width * 0.85f, size.height * 0.3f),
        style = Stroke(width = 10f)
    )
    drawLine(
        color = Color(0x33EC4899),
        start = Offset(size.width * 0.45f, size.height * 0.45f),
        end = Offset(size.width * 0.35f, size.height * 0.25f),
        strokeWidth = 10f,
        cap = StrokeCap.Round
    )
    drawLine(
        color = Color(0x33EC4899),
        start = Offset(size.width * 0.55f, size.height * 0.5f),
        end = Offset(size.width * 0.65f, size.height * 0.3f),
        strokeWidth = 10f,
        cap = StrokeCap.Round
    )
    repeat(5) { index ->
        val x = size.width * (0.2f + index * 0.12f)
        drawCircle(
            color = Color(0x33FACC15),
            radius = 18f,
            center = Offset(x, size.height * 0.6f)
        )
    }
}
