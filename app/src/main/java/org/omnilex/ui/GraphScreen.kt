package org.omnilex.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import org.omnilex.data.model.RelationshipType
import org.omnilex.data.repository.GraphEdge
import org.omnilex.data.repository.GraphNode
import org.omnilex.data.repository.LexicalGraph
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(
    entryId: String,
    viewModel: OmniLexViewModel,
    onBack: () -> Unit,
    onNodeClick: (String) -> Unit
) {
    val state by viewModel.graphState.collectAsState()

    LaunchedEffect(entryId) {
        viewModel.loadGraph(entryId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lexical Web") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val graphState = state) {
            is GraphState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is GraphState.Success -> GraphContent(graphState.graph, entryId, Modifier.padding(padding), onNodeClick = onNodeClick)
            is GraphState.Error -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text(graphState.message, color = MaterialTheme.colorScheme.error) }
            else -> {}
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun GraphContent(graph: LexicalGraph, centralId: String, modifier: Modifier = Modifier, isAnimated: Boolean = true, onNodeClick: (String) -> Unit) {
    var nodes by remember { 
        val initialNodes = graph.nodes.mapIndexed { index, node ->
            val isCentral = node.entryId == centralId
            if (isCentral) {
                NodeState(node, x = 0f, y = 0f)
            } else {
                // Adjustment 3: Circular Initial Placement
                val angle = (2 * PI * index / graph.nodes.size).toFloat()
                val radius = 300f
                NodeState(node, x = cos(angle) * radius, y = sin(angle) * radius)
            }
        }
        mutableStateOf(initialNodes) 
    }
    val edges = graph.edges
    
    // Simple force-directed physics loop
    LaunchedEffect(graph) {
        if (!isAnimated) return@LaunchedEffect
        val iterations = 150
        repeat(iterations) {
            val nextPositions = nodes.map { it.copy() }.toMutableList()
            // 1. Repulsion between all pairs
            for (j in nodes.indices) {
                for (k in nodes.indices) {
                    if (j == k) continue
                    val dx = nodes[j].x - nodes[k].x
                    val dy = nodes[j].y - nodes[k].y
                    val distSq = dx * dx + dy * dy + 0.01f
                    if (distSq < 160000) {
                        val force = 4500f / distSq // Increased repulsion
                        nextPositions[j].vx += dx * force
                        nextPositions[j].vy += dy * force
                    }
                }
            }
            // 2. Attraction along edges
            edges.forEach { edge ->
                val fromIdx = nodes.indexOfFirst { it.node.entryId == edge.fromId }
                val toIdx = nodes.indexOfFirst { it.node.entryId == edge.toId }
                if (fromIdx != -1 && toIdx != -1) {
                    val dx = nodes[toIdx].x - nodes[fromIdx].x
                    val dy = nodes[toIdx].y - nodes[fromIdx].y
                    val dist = sqrt(dx * dx + dy * dy)
                    // Attraction proportional to confidence (weight)
                    val isHomonym = edge.type == RelationshipType.HOMONYM
                    val desired = if (isHomonym) 120f else 250f * (1f - edge.confidence * 0.3f)
                    val force = (dist - desired) * (if (isHomonym) 0.15f else 0.08f)
                    val nx = dx / (dist + 0.01f)
                    val ny = dy / (dist + 0.01f)
                    nextPositions[fromIdx].vx += nx * force
                    nextPositions[fromIdx].vy += ny * force
                    nextPositions[toIdx].vx -= nx * force
                    nextPositions[toIdx].vy -= ny * force
                }
            }
            // 3. Center gravity (pull everything towards center)
            nextPositions.indices.forEach { idx ->
                nextPositions[idx].vx -= nextPositions[idx].x * 0.03f
                nextPositions[idx].vy -= nextPositions[idx].y * 0.03f
            }
            
            // Apply velocities and dampening
            nodes = nextPositions.map { 
                it.copy(
                    x = it.x + it.vx, 
                    y = it.y + it.vy, 
                    vx = it.vx * 0.35f, // Increased friction for stability
                    vy = it.vy * 0.35f
                ) 
            }
            delay(16.milliseconds)
        }
    }

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale *= zoomChange
        offset += offsetChange
    }

    val currentNodes by rememberUpdatedState(nodes)
    val currentScale by rememberUpdatedState(scale)
    val currentOffset by rememberUpdatedState(offset)

    Box(modifier = modifier
        .fillMaxSize()
        .transformable(state = transformState)
        .pointerInput(Unit) {
            detectTapGestures { tapOffset ->
                val canvasCenter = Offset(size.width / 2f, size.height / 2f)
                val relativeTap = (tapOffset - currentOffset - canvasCenter) / currentScale + canvasCenter
                val graphSpaceTap = relativeTap - canvasCenter

                currentNodes.find { state ->
                    val dist = sqrt((state.x - graphSpaceTap.x).pow(2) + (state.y - graphSpaceTap.y).pow(2))
                    // Hit radius is same as visual radius
                    val radius = if (state.node.entryId == centralId) 32.dp.toPx() else (24 + state.node.weight * 12).dp.toPx()
                    dist < radius + 10.dp.toPx()
                }?.let { onNodeClick(it.node.entryId) }
            }
        }
        .pointerInput(Unit) {
            detectDragGestures { _, dragAmount ->
                offset += dragAmount
            }
        }
    ) {
        val textMeasurer = rememberTextMeasurer()
        Canvas(Modifier.fillMaxSize().graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)) {
            val center = this.center
            
            // Draw Edges
            edges.forEach { edge ->
                val from = nodes.find { it.node.entryId == edge.fromId }
                val to = nodes.find { it.node.entryId == edge.toId }
                if (from != null && to != null) {
                    drawLine(
                        color = relationshipColor(edge.type).copy(alpha = 0.5f),
                        start = center + Offset(from.x, from.y),
                        end = center + Offset(to.x, to.y),
                        strokeWidth = (1 + edge.confidence * 3).dp.toPx(),
                        pathEffect = relationshipPathEffect(edge.type)
                    )
                }
            }

            // Draw Nodes
            nodes.forEach { state ->
                val pos = center + Offset(state.x, state.y)
                val isCentral = state.node.entryId == centralId
                
                // Variable Node Sizing based on weight (ranking)
                val baseRadius = if (isCentral) 32.dp else (24 + state.node.weight * 12).dp
                val radiusPx = baseRadius.toPx()
                
                // Color determined by relationship type, or static for central word
                val relType = if (isCentral) null else edges.find { it.toId == state.node.entryId || it.fromId == state.node.entryId }?.type
                val color = if (isCentral) Color(0xFF355C7D) else relType?.let { relationshipColor(it) } ?: Color(0xFFF67280)
                
                drawCircle(color, radius = radiusPx, center = pos)
                if (isCentral) {
                    drawCircle(Color.White, radius = radiusPx + 2.dp.toPx(), center = pos, style = Stroke(2.dp.toPx()))
                }

                val text = state.node.label
                val textLayout = textMeasurer.measure(
                    text = AnnotatedString(text),
                    style = TextStyle(color = Color.Black, fontSize = (10 + state.node.weight * 4).sp, fontWeight = FontWeight.Bold)
                )
                drawText(textLayout, topLeft = pos - Offset(textLayout.size.width / 2f, -(radiusPx + 8.dp.toPx())))
            }
        }
        
        Text("Pinch to zoom, drag to explore", Modifier.align(Alignment.BottomCenter).padding(16.dp), style = MaterialTheme.typography.labelSmall)
        
        // Legend
        LegendOverlay(Modifier.align(Alignment.TopEnd).padding(16.dp))
    }
}

@Composable
private fun LegendOverlay(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 4.dp
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Legend", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
            LegendItem(Color(0xFF4CAF50), "Synonyms")
            LegendItem(Color(0xFFF44336), "Antonyms")
            LegendItem(Color(0xFF2196F3), "Hierarchy")
            LegendItem(Color(0xFF9C27B0), "Part/Whole")
            LegendItem(Color(0xFFFF9800), "Associations")
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(12.dp).drawCircleAccent(color))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun Modifier.drawCircleAccent(color: Color) = this.then(Modifier.pointerInput(Unit) {}.drawBehind {
    drawCircle(color)
})

private fun relationshipColor(type: RelationshipType): Color = when(type) {
    RelationshipType.SYNONYM -> Color(0xFF4CAF50)
    RelationshipType.ANTONYM -> Color(0xFFF44336)
    RelationshipType.HYPERNYM, RelationshipType.HYPONYM -> Color(0xFF2196F3)
    RelationshipType.MERONYM, RelationshipType.HOLONYM -> Color(0xFF9C27B0)
    RelationshipType.RELATED_CONCEPT, RelationshipType.AMBIGUOUS_WITH -> Color(0xFFFF9800)
    RelationshipType.HOMONYM -> Color(0xFFFFD700) // Gold for homonym clusters
    RelationshipType.ETYMOLOGICAL_ORIGIN -> Color(0xFF9E9E9E)
    else -> Color(0xFF757575)
}

private fun relationshipPathEffect(type: RelationshipType): PathEffect? = when(type) {
    RelationshipType.HYPERNYM, RelationshipType.HYPONYM -> PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    RelationshipType.MERONYM, RelationshipType.HOLONYM -> PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
    else -> null
}

private data class NodeState(
    val node: GraphNode,
    var x: Float = (Random.nextFloat() - 0.5f) * 400f,
    var y: Float = (Random.nextFloat() - 0.5f) * 400f,
    var vx: Float = 0f,
    var vy: Float = 0f
)

private object Random {
    private var seed = 42
    fun nextFloat(): Float {
        seed = (seed * 1103515245 + 12345) and 0x7fffffff
        return seed.toFloat() / 0x7fffffff.toFloat()
    }
}

@Preview(showBackground = true)
@Composable
fun GraphPreview() {
    val sampleNodes = listOf(
        GraphNode("1", "Knowledge", 0.9f),
        GraphNode("2", "Wisdom", 0.8f),
        GraphNode("3", "Learning", 0.7f),
        GraphNode("4", "Experience", 0.6f)
    )
    val sampleEdges = listOf(
        GraphEdge("1", "2", RelationshipType.RELATED_CONCEPT, 0.9f),
        GraphEdge("1", "3", RelationshipType.RELATED_CONCEPT, 0.8f),
        GraphEdge("2", "4", RelationshipType.RELATED_CONCEPT, 0.7f)
    )
    MaterialTheme {
        GraphContent(LexicalGraph(sampleNodes, sampleEdges), "1", isAnimated = false, onNodeClick = {})
    }
}
