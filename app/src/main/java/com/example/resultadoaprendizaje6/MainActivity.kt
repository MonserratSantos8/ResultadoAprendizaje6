package com.example.resultadoaprendizaje6

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.resultadoaprendizaje6.ui.theme.ResultadoAprendizaje6Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ResultadoAprendizaje6Theme {
                AppNotas()
            }
        }
    }
}

// Controla qué pantalla se muestra: la lista o el editor
@Composable
fun AppNotas() {
    // Lista de prueba que se puede modificar
    val notas = remember {
        mutableStateListOf("Comprar leche", "Estudiar Kotlin", "Llamar a mamá")
    }

    // null = estamos en la lista | -1 = nota nueva | 0,1,2... = posición de la nota a editar
    var notaEditando by remember { mutableStateOf<Int?>(null) }

    if (notaEditando == null) {
        // PANTALLA 1 (y 3): lista de notas
        PantallaNotas(
            notas = notas,
            onEditar = { posicion -> notaEditando = posicion },
            onNueva = { notaEditando = -1 }
        )
    } else {
        // PANTALLA 2: editor
        val posicion = notaEditando!!
        PantallaEditor(
            textoInicial = if (posicion >= 0) notas[posicion] else "",
            onGuardar = { texto ->
                if (texto.isNotBlank()) {
                    if (posicion >= 0) notas[posicion] = texto else notas.add(texto)
                }
                notaEditando = null
            },
            onDuplicar = { texto ->
                notas.add(texto)
                notaEditando = null
            },
            onEliminar = {
                if (posicion >= 0) notas.removeAt(posicion)
                notaEditando = null
            },
            onVolver = { notaEditando = null }
        )
    }
}

// PANTALLA 1: Lista de notas con menú contextual (se abre con long press)
// PANTALLA 3: Modo selección múltiple (barra superior contextual)
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PantallaNotas(
    notas: MutableList<String>,
    onEditar: (Int) -> Unit,
    onNueva: () -> Unit
) {
    val contexto = LocalContext.current

    // Dice si estamos en modo selección y cuáles notas están marcadas
    var modoSeleccion by remember { mutableStateOf(false) }
    val seleccionadas = remember { mutableStateListOf<Int>() }

    fun salirDeSeleccion() {
        seleccionadas.clear()
        modoSeleccion = false
    }

    // Si presionas "atrás" en modo selección, solo sale de la selección
    BackHandler(enabled = modoSeleccion) { salirDeSeleccion() }

    Scaffold(
        topBar = {
            if (modoSeleccion) {
                // Barra contextual: aparece solo mientras seleccionas
                TopAppBar(
                    title = { Text("${seleccionadas.size} seleccionadas") },
                    navigationIcon = {
                        IconButton(onClick = { salirDeSeleccion() }) {
                            Text("X")
                        }
                    },
                    actions = {
                        // Borra todas las notas marcadas (de atrás hacia adelante para no desordenar)
                        TextButton(onClick = {
                            seleccionadas.sortedDescending().forEach { notas.removeAt(it) }
                            salirDeSeleccion()
                        }) {
                            Text("Borrar")
                        }
                    }
                )
            } else {
                TopAppBar(title = { Text("Mis notas") })
            }
        },
        floatingActionButton = {
            if (!modoSeleccion) {
                FloatingActionButton(onClick = onNueva) {
                    Text("+")
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(modifier = Modifier.padding(innerPadding).fillMaxSize().padding(16.dp)) {
            itemsIndexed(notas) { posicion, nota ->
                // Controla si el menú está visible para ESTA nota
                var menuVisible by remember { mutableStateOf(false) }
                val estaSeleccionada = seleccionadas.contains(posicion)

                Box {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (estaSeleccionada)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .combinedClickable(
                                onClick = {
                                    if (modoSeleccion) {
                                        // En modo selección, tocar marca o desmarca la nota
                                        if (estaSeleccionada) seleccionadas.remove(posicion)
                                        else seleccionadas.add(posicion)
                                    } else {
                                        onEditar(posicion) // toque normal: abre el editor
                                    }
                                },
                                onLongClick = {
                                    if (!modoSeleccion) menuVisible = true // abre el menú contextual
                                }
                            )
                    ) {
                        Text(text = nota, modifier = Modifier.padding(20.dp))
                    }

                    // El menú contextual
                    DropdownMenu(
                        expanded = menuVisible,
                        onDismissRequest = { menuVisible = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Editar") },
                            onClick = {
                                menuVisible = false
                                onEditar(posicion)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Compartir") },
                            onClick = {
                                menuVisible = false
                                // Abre el selector de apps para compartir el texto
                                val intento = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, nota)
                                }
                                contexto.startActivity(Intent.createChooser(intento, "Compartir nota"))
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Seleccionar") },
                            onClick = {
                                menuVisible = false
                                modoSeleccion = true       // activa la barra contextual
                                seleccionadas.add(posicion)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar") },
                            onClick = {
                                notas.removeAt(posicion)
                                menuVisible = false
                            }
                        )
                    }
                }
            }
        }
    }
}

// PANTALLA 2: Editor de nota con menú de tres puntitos (PopupMenu)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaEditor(
    textoInicial: String,
    onGuardar: (String) -> Unit,
    onDuplicar: (String) -> Unit,
    onEliminar: () -> Unit,
    onVolver: () -> Unit
) {
    var texto by remember { mutableStateOf(textoInicial) }
    var menuVisible by remember { mutableStateOf(false) }

    BackHandler { onVolver() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Editar nota") },
                navigationIcon = {
                    IconButton(onClick = onVolver) {
                        Text("←")
                    }
                },
                actions = {
                    // Botón de los tres puntitos
                    IconButton(onClick = { menuVisible = true }) {
                        Text("⋮")
                    }
                    // Menú que sale pegado al botón
                    DropdownMenu(
                        expanded = menuVisible,
                        onDismissRequest = { menuVisible = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Limpiar texto") },
                            onClick = {
                                texto = ""
                                menuVisible = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicar") },
                            onClick = {
                                menuVisible = false
                                onDuplicar(texto)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Eliminar") },
                            onClick = {
                                menuVisible = false
                                onEliminar()
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            OutlinedTextField(
                value = texto,
                onValueChange = { texto = it },
                label = { Text("Escribe tu nota") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { onGuardar(texto) }) {
                Text("Guardar")
            }
        }
    }
}