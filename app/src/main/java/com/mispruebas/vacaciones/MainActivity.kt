package com.mispruebas.vacaciones

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.rememberImagePainter
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.io.File


enum class Pantalla {
    ListaLugares,
    DetallesLugar,
    Mapa
}

class AppVM : ViewModel() {
    val lugares = mutableStateListOf<Lugar>()
    var pantallaActual = mutableStateOf(Pantalla.ListaLugares)
    val lugarSeleccionado = mutableStateOf<Lugar?>(null)
    var valorDolar = mutableStateOf(0.0)

    init {
        obtenerValorDolar()
    }
    private fun obtenerValorDolar() {
        // Aquí debes configurar Retrofit para obtener el valor del dólar desde la API de mindicador.cl
        // Por ejemplo:
        val retrofit = Retrofit.Builder()
            .baseUrl("https://mindicador.cl/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(MindicadorService::class.java)
        service.getIndicadoresEconomicos().enqueue(object : Callback<IndicadoresResponse> {
            override fun onResponse(call: Call<IndicadoresResponse>, response: Response<IndicadoresResponse>) {
                response.body()?.let {
                    valorDolar.value = it.dolar.valor
                }
            }
            override fun onFailure(call: Call<IndicadoresResponse>, t: Throwable) {
                // Manejar error
            }
        })
    }
    fun agregarLugar(lugar: Lugar) {
        lugares.add(lugar)
    }
    fun actualizarLugar(lugarActualizado: Lugar) {
        // Encuentra el índice del lugar original basándote en el nombre y reemplázalo
        val index = lugares.indexOfFirst { it.nombre == lugarActualizado.nombre }
        if (index != -1) {
            lugares[index] = lugarActualizado
        }
    }

}

// Define las clases necesarias para parsear la respuesta de la API
data class IndicadoresResponse(
    val dolar: Indicador
)

data class Indicador(
    val valor: Double
)

interface MindicadorService {
    @GET("api")
    fun getIndicadoresEconomicos(): Call<IndicadoresResponse>

}

data class Lugar(
    val nombre: String,
    val ordenVisita: Int,
    val imagenReferencia: Uri,
    val latitud: Double,
    val longitud: Double,
    val costoAlojamientoCLP: Double,
    val comentarios: String,
    val fotos: List<Uri>
)

class MainActivity : ComponentActivity() {
    private val appVM: AppVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppPlanificadorVacaciones()
        }
    }
}

@Composable
fun AppPlanificadorVacaciones() {
    val appVM: AppVM = viewModel()
    when (appVM.pantallaActual.value) {
        Pantalla.ListaLugares -> PantallaListaLugares()
        Pantalla.DetallesLugar -> PantallaDetallesLugar()
        Pantalla.Mapa -> PantallaMapa()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaListaLugares() {
    val appVM: AppVM = viewModel()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(all = 16.dp)
    ) {
        LazyColumn(
            modifier = Modifier
                .weight(1f)  // Esta línea asegura que LazyColumn utilice todo el espacio disponible
        ) {
            items(appVM.lugares) { lugar ->

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Usa Coil para cargar la imagen de referencia
                    if (lugar.imagenReferencia.toString().isNotBlank()) {
                        Image(
                            painter = rememberImagePainter(lugar.imagenReferencia.toString()),
                            contentDescription = null,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable {
                                    appVM.lugarSeleccionado.value = lugar
                                    appVM.pantallaActual.value = Pantalla.DetallesLugar
                                }
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = lugar.nombre, style = MaterialTheme.typography.bodyMedium)
                        Text(text = "CLP: ${lugar.costoAlojamientoCLP}", style = MaterialTheme.typography.bodySmall)
                        Text(text = "USD: ${lugar.costoAlojamientoCLP / appVM.valorDolar.value}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }


        FloatingActionButton(
            onClick = {
                appVM.pantallaActual.value = Pantalla.DetallesLugar
            },
            modifier = Modifier.align(Alignment.End)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PantallaDetallesLugar() {
    val appVM: AppVM = viewModel()

    val lugar = appVM.lugarSeleccionado.value

    var nombre by remember { mutableStateOf(lugar?.nombre ?: "") }
    var ordenVisita by remember { mutableStateOf(lugar?.ordenVisita?.toString() ?: "") }
    var comentarios by remember { mutableStateOf(lugar?.comentarios ?: "") }
    var latitud by remember { mutableStateOf(lugar?.latitud?.toString() ?: "") }
    var longitud by remember { mutableStateOf(lugar?.longitud?.toString() ?: "") }
    var imagenReferenciaUrl by remember { mutableStateOf(lugar?.imagenReferencia?.toString() ?: "") }
    var precioAlojamiento by remember { mutableStateOf(lugar?.costoAlojamientoCLP?.toString() ?: "") }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        Text("Detalles del lugar", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = nombre,
            onValueChange = { nombre = it },
            label = { Text("Nombre") }
        )
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedTextField(
                value = latitud,
                onValueChange = { latitud = it },
                label = { Text("Latitud") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedTextField(
                value = longitud,
                onValueChange = { longitud = it },
                label = { Text("Longitud") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = {
                if (latitud.isNotBlank() && longitud.isNotBlank()) {
                    val lugarTemporal = Lugar(
                        nombre = "",
                        ordenVisita = 0,
                        imagenReferencia = Uri.EMPTY,
                        latitud = latitud.toDouble(),
                        longitud = longitud.toDouble(),
                        costoAlojamientoCLP = 0.0,
                        comentarios = "",
                        fotos = emptyList()
                    )
                    appVM.lugares.add(0, lugarTemporal)
                    appVM.pantallaActual.value = Pantalla.Mapa
                }
            }) {
                Icon(Icons.Default.LocationOn, contentDescription = "Ver en el mapa")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = ordenVisita,
            onValueChange = { ordenVisita = it },
            label = { Text("Orden de visita") },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = {
                // Handle done action
            })
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = imagenReferenciaUrl,
            onValueChange = { imagenReferenciaUrl = it },
            label = { Text("Imagen Referencia URL") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = precioAlojamiento,
            onValueChange = { precioAlojamiento = it },
            label = { Text("Precio Alojamiento (CLP)") },
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Number
            )
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Usa Coil para mostrar la imagen de referencia
        if (imagenReferenciaUrl.isNotBlank()) {
            Image(
                painter = rememberImagePainter(imagenReferenciaUrl),
                contentDescription = null,
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = comentarios,
            onValueChange = { comentarios = it },
            label = { Text("Comentarios") },
            maxLines = 3
        )
        if (imagenReferenciaUrl.isNotBlank()) {
            Image(
                painter = rememberImagePainter(File(imagenReferenciaUrl)), // Cambio aquí
                contentDescription = null,
                modifier = Modifier
                    .height(200.dp)
                    .fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            // Crear un nuevo objeto Lugar con la información proporcionada
            val nuevoLugar = Lugar(
                nombre = nombre,
                ordenVisita = ordenVisita.toIntOrNull() ?: 0,
                imagenReferencia = Uri.parse(imagenReferenciaUrl),
                latitud = latitud.toDoubleOrNull() ?: 0.0,
                longitud = longitud.toDoubleOrNull() ?: 0.0,
                costoAlojamientoCLP = precioAlojamiento.toDoubleOrNull() ?: 0.0,
                comentarios = comentarios,
                fotos = listOf(Uri.parse(imagenReferenciaUrl))  // Para simplificar, solo agregamos la imagen de referencia a la lista de fotos
            )
            if (lugar != null) {
                appVM.actualizarLugar(nuevoLugar)
            } else {
                appVM.agregarLugar(nuevoLugar)
            }

            // Volver a la pantalla anterior
            appVM.pantallaActual.value = Pantalla.ListaLugares
        }) {
            Text("Guardar")
        }
    }
}



@Composable
fun AndroidMapView(
    modifier: Modifier = Modifier,
    onMapViewCreated: (MapView) -> Unit
) {
    AndroidView(
        modifier = modifier,
        factory = {
            MapView(it).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setBuiltInZoomControls(true)
                setMultiTouchControls(true)
                onMapViewCreated(this)
            }
        }
    )
}

@Composable
fun PantallaMapa() {
    val appVM: AppVM = viewModel()

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidMapView(modifier = Modifier.fillMaxSize()) { mapView ->
            val ultimoLugar = appVM.lugares.firstOrNull()
            if (ultimoLugar != null) {
                val geoPoint = GeoPoint(ultimoLugar.latitud, ultimoLugar.longitud)
                mapView.controller.setCenter(geoPoint)
                val marker = Marker(mapView)
                marker.position = geoPoint
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                mapView.overlays.add(marker)
            }
            mapView.invalidate()
        }

        // Botón de volver en la esquina superior izquierda
        IconButton(
            onClick = {
                appVM.pantallaActual.value = Pantalla.DetallesLugar
            },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(Color.White, CircleShape)
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
        }
    }
}
