package com.example.taaltrails

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable   // <-- the missing import
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.util.Calendar

// ---------- COLORS ----------
val Ink = Color(0xFF0F3D56)
val Teal = Color(0xFF1F7A8C)
val Gold = Color(0xFFD4A24C)
val Bg = Color(0xFFE8F0F2)
val Surface0 = Color(0xFFF6FAFB)
val Line = Color(0xFFDDE7EA)
val Muted = Color(0xFF6B7F88)

const val USER_NAME = "Traveler"
const val LOCATION_NAME = "Taal, Batangas"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Ink)) {
                TaalApp()
            }
        }
    }
}

@Composable
fun TaalApp() {
    var loggedIn by rememberSaveable { mutableStateOf(false) }
    if (!loggedIn) LoginScreen { loggedIn = true } else Dashboard { loggedIn = false }
}

// ---------- LOGIN ----------
@Composable
fun LoginScreen(onLogin: () -> Unit) {
    var user by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    Column(
        Modifier.fillMaxSize().background(Bg).padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("TaalTrails", fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Text("Your smart guide to Taal, Batangas", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(user, { user = it }, label = { Text("Username") },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(pass, { pass = it }, label = { Text("Password") },
            singleLine = true, visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onLogin,
            colors = ButtonDefaults.buttonColors(containerColor = Ink),
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text("Login") }
    }
}

// ---------- SMALL HELPERS ----------
fun fmt(n: Int) = "%,d".format(n)

@Composable
fun Thumb(p: Place, size: Dp) {
    val ctx = LocalContext.current
    // Drop a picture named like the place id (e.g. basilica.jpg) into res/drawable to show it.
    val resId = remember(p.id) { ctx.resources.getIdentifier(p.id, "drawable", ctx.packageName) }
    Box(
        Modifier.size(size).clip(RoundedCornerShape(12.dp)).background(Teal.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        if (resId != 0) {
            Image(painterResource(resId), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Text(p.emoji, fontSize = (size.value * 0.45f).sp)
        }
    }
}

@Composable
fun RoundBtn(
    onClick: () -> Unit,
    bg: Color = Color(0xFFF0F5F6),
    size: Dp = 34.dp,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier.size(size).clip(CircleShape).background(bg).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
fun CardBox(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Line),
    ) { content() }
}

fun openDirections(ctx: Context, p: Place, mode: String) {
    val uri = Uri.parse(
        "https://www.google.com/maps/dir/?api=1&destination=" +
                Uri.encode("${p.name}, Taal, Batangas") + "&travelmode=$mode"
    )
    ctx.startActivity(Intent(Intent.ACTION_VIEW, uri))
}

fun openTown(ctx: Context) {
    val uri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode("Taal, Batangas"))
    ctx.startActivity(Intent(Intent.ACTION_VIEW, uri))
}

// ---------- DASHBOARD ----------
@Composable
fun Dashboard(onLogout: () -> Unit) {
    var tab by remember { mutableIntStateOf(0) }
    var day by remember { mutableIntStateOf(0) }
    val favs = remember { mutableStateListOf<String>() }
    val days = remember { List(3) { mutableStateListOf<String>() } }
    val ctx = LocalContext.current
    val tripCount = days.sumOf { it.size }

    fun inTrip(id: String) = days.any { id in it }
    fun toast(msg: String) = android.widget.Toast.makeText(ctx, msg, android.widget.Toast.LENGTH_SHORT).show()
    fun toggleTrip(p: Place) {
        if (inTrip(p.id)) {
            days.forEach { it.remove(p.id) }
            toast("Removed from your trip")
        } else {
            days[day].add(p.id)
            toast("Added to Day ${day + 1}")
        }
    }

    Scaffold(
        containerColor = Bg,
        bottomBar = {
            NavigationBar(containerColor = Surface0) {
                val items = listOf("Explore" to "🧭", "My Trip" to "🗺️", "Guide" to "💬", "Map" to "📍")
                items.forEachIndexed { i, (label, emoji) ->
                    NavigationBarItem(
                        selected = tab == i,
                        onClick = { tab = i },
                        icon = {
                            BadgedBox(badge = {
                                if (i == 1 && tripCount > 0) Badge { Text("$tripCount") }
                            }) { Text(emoji, fontSize = 20.sp) }
                        },
                        label = { Text(label, fontSize = 11.sp) },
                    )
                }
            }
        },
    ) { pad ->
        Column(Modifier.padding(pad).fillMaxSize()) {
            Header(onLogout)
            when (tab) {
                0 -> ExploreTab(favs, ::inTrip, ::toggleTrip)
                1 -> TripTab(days, day, { day = it }, ::toggleTrip)
                2 -> GuideTab(::inTrip, ::toggleTrip)
                else -> MapTab()
            }
        }
    }
}

@Composable
fun Header(onLogout: () -> Unit) {
    val h = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greet = if (h < 12) "GOOD MORNING" else if (h < 18) "GOOD AFTERNOON" else "GOOD EVENING"
    Row(
        Modifier.fillMaxWidth().background(Surface0).padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(greet, fontSize = 10.sp, letterSpacing = 1.4.sp, fontWeight = FontWeight.Bold, color = Teal)
            Text("Hi, $USER_NAME", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Ink,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$LOCATION_NAME · Smart guide", fontSize = 12.sp, color = Muted)
        }
        OutlinedButton(
            onClick = onLogout,
            border = BorderStroke(1.dp, Line),
            shape = RoundedCornerShape(12.dp),
        ) { Text("Logout", color = Ink) }
    }
    HorizontalDivider(color = Line)
}

// ---------- EXPLORE ----------
@Composable
fun ExploreTab(favs: SnapshotStateList<String>, inTrip: (String) -> Boolean, toggleTrip: (Place) -> Unit) {
    var chip by remember { mutableIntStateOf(0) }
    var query by remember { mutableStateOf("") }
    val labels = listOf("Best Places", "Best Food", "Best Activities", "Favorites")
    val titles = listOf("BEST PLACES", "BEST FOOD", "BEST ACTIVITIES", "YOUR FAVORITES")

    val list = PLACES.filter {
        when (chip) {
            0 -> it.kind == Kind.PLACES
            1 -> it.kind == Kind.FOOD
            2 -> it.kind == Kind.ACTIVITIES
            else -> it.id in favs
        }
    }.filter {
        query.isBlank() || "${it.name} ${it.tag} ${it.desc}".lowercase().contains(query.trim().lowercase())
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query, onValueChange = { query = it },
            placeholder = { Text("Search…") },
            leadingIcon = { Icon(Icons.Default.Search, null) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(labels.size) { i ->
                val sel = chip == i
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp))
                        .background(if (sel) Ink else Color.White)
                        .border(1.dp, if (sel) Ink else Line, RoundedCornerShape(20.dp))
                        .clickable { chip = i; query = "" }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(labels[i], fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                        color = if (sel) Color.White else Ink)
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(titles[chip], fontSize = 11.sp, letterSpacing = 1.2.sp, fontWeight = FontWeight.Bold, color = Muted)
            Text("${list.size} results", fontSize = 11.sp, color = Muted)
        }
        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    if (chip == 3 && favs.isEmpty()) "Nothing saved yet. Tap the heart on any card."
                    else "No matches found. Try a different word or filter.",
                    color = Muted, fontSize = 13.sp, modifier = Modifier.padding(24.dp),
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(list, key = { it.id }) { p ->
                    val fav = p.id in favs
                    val added = inTrip(p.id)
                    CardBox {
                        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                            Thumb(p, 76.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(p.tag.uppercase(), fontSize = 10.sp, letterSpacing = 1.sp,
                                    fontWeight = FontWeight.Bold, color = Teal)
                                Text(p.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Ink,
                                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(p.desc, fontSize = 12.sp, color = Muted, maxLines = 2,
                                    overflow = TextOverflow.Ellipsis, lineHeight = 16.sp)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, null, tint = Gold, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(3.dp))
                                    Text("${p.rating}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.width(4.dp))
                                    Text("(${fmt(p.reviews)} reviews)", fontSize = 11.sp, color = Muted)
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                RoundBtn({ if (fav) favs.remove(p.id) else favs.add(p.id) }) {
                                    Icon(
                                        if (fav) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                        null, tint = if (fav) Gold else Muted, modifier = Modifier.size(18.dp),
                                    )
                                }
                                RoundBtn({ toggleTrip(p) }, bg = if (added) Ink else Color(0xFFF0F5F6)) {
                                    Icon(
                                        if (added) Icons.Default.Check else Icons.Default.Add,
                                        null, tint = if (added) Color.White else Muted, modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------- MY TRIP ----------
@Composable
fun TripTab(
    days: List<SnapshotStateList<String>>,
    day: Int,
    setDay: (Int) -> Unit,
    toggleTrip: (Place) -> Unit,
) {
    val all = days.flatten()
    val total = all.sumOf { placeById(it).cost }
    val planned = days.count { it.isNotEmpty() }
    val tripCount = all.size
    val dayList = days[day]

    fun dayHasFood() = dayList.any { placeById(it).kind == Kind.FOOD }
    fun suggestions(): List<Place> {
        val needsFood = !dayHasFood()
        return PLACES.filter { p -> days.none { p.id in it } }
            .sortedWith(
                compareByDescending<Place> { if (needsFood && it.kind == Kind.FOOD) 1 else 0 }
                    .thenByDescending { it.rating }
            ).take(3)
    }
    fun reason(p: Place) =
        if (!dayHasFood() && p.kind == Kind.FOOD) "Your plan needs a local food stop"
        else "Rated ${p.rating} by ${fmt(p.reviews)} visitors"

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("My Taal Trip", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Text("Build a day plan from anything you save. Times are suggestions based on the best visiting window.",
            fontSize = 12.sp, color = Muted, lineHeight = 17.sp)
        Spacer(Modifier.height(14.dp))

        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Ink).padding(16.dp)) {
            Text("ESTIMATED BUDGET", fontSize = 10.sp, letterSpacing = 1.3.sp,
                fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
            Text("₱${fmt(total)}", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Spacer(Modifier.height(8.dp))
            Text("$tripCount stop${if (tripCount == 1) "" else "s"}  ·  $planned day${if (planned == 1) "" else "s"} planned",
                fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
        }
        Spacer(Modifier.height(14.dp))

        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.White)
                .border(1.dp, Line, RoundedCornerShape(24.dp)).padding(4.dp),
        ) {
            for (i in 0 until 3) {
                val sel = day == i
                Box(
                    Modifier.weight(1f).clip(RoundedCornerShape(20.dp))
                        .background(if (sel) Ink else Color.Transparent)
                        .clickable { setDay(i) }.padding(vertical = 9.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Day ${i + 1}" + if (days[i].isNotEmpty()) " (${days[i].size})" else "",
                        fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = if (sel) Color.White else Ink)
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        if (dayList.isEmpty()) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface0)
                    .border(1.dp, Line, RoundedCornerShape(16.dp)).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Day ${day + 1} is empty", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Ink)
                Spacer(Modifier.height(4.dp))
                Text("Let the guide build a balanced day, or add stops yourself from Explore.",
                    fontSize = 12.sp, color = Muted)
                Spacer(Modifier.height(14.dp))
                Button(
                    onClick = { dayList.addAll(suggestions().map { it.id }) },
                    colors = ButtonDefaults.buttonColors(containerColor = Ink),
                ) { Text("Plan Day ${day + 1} for me") }
            }
        } else {
            dayList.forEachIndexed { i, id ->
                val p = placeById(id)
                CardBox(Modifier.padding(bottom = 10.dp)) {
                    Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Thumb(p, 52.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.tag.uppercase(), fontSize = 9.sp, letterSpacing = 1.sp,
                                fontWeight = FontWeight.Bold, color = Teal)
                            Text(p.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${p.price} · ${p.window}", fontSize = 11.sp, color = Muted,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(
                            onClick = { val t = dayList[i]; dayList[i] = dayList[i - 1]; dayList[i - 1] = t },
                            enabled = i > 0, modifier = Modifier.size(30.dp),
                        ) { Icon(Icons.Default.KeyboardArrowUp, null, tint = Muted) }
                        IconButton(
                            onClick = { val t = dayList[i]; dayList[i] = dayList[i + 1]; dayList[i + 1] = t },
                            enabled = i < dayList.size - 1, modifier = Modifier.size(30.dp),
                        ) { Icon(Icons.Default.KeyboardArrowDown, null, tint = Muted) }
                        IconButton(onClick = { toggleTrip(p) }, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Default.Delete, null, tint = Muted)
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { dayList.addAll(suggestions().take(2).map { it.id }) },
                    border = BorderStroke(1.dp, Line), shape = RoundedCornerShape(12.dp),
                ) { Text("Fill remaining gaps", color = Ink, fontSize = 12.sp) }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { dayList.clear() }) {
                    Text("Clear Day ${day + 1}", color = Muted, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        CardBox {
            Column(Modifier.padding(12.dp)) {
                Row {
                    Text("Smart picks for this trip", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink)
                    Spacer(Modifier.weight(1f))
                    Text("AUTO", fontSize = 10.sp, letterSpacing = 1.sp, color = Muted)
                }
                Spacer(Modifier.height(10.dp))
                val picks = suggestions()
                if (picks.isEmpty()) {
                    Text("Everything is already in your trip. Nice!", fontSize = 12.sp, color = Muted)
                } else picks.forEach { p ->
                    Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Thumb(p, 44.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(p.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink,
                                maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(reason(p), fontSize = 11.sp, color = Muted, maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                        }
                        RoundBtn({ toggleTrip(p) }, bg = Ink) {
                            Icon(Icons.Default.Add, null, tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

// ---------- GUIDE (AI CHAT) ----------
@Composable
fun GuideTab(inTrip: (String) -> Boolean, toggleTrip: (Place) -> Unit) {
    val msgs = remember {
        mutableStateListOf(
            ChatMsg(false, "Hi! I'm your Taal guide. Ask me anything: about Taal, where to eat, what to see, travel tips, or even general questions.")
        )
    }
    var input by remember { mutableStateOf("") }
    var thinking by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val starters = listOf(
        "I have 4 hours and love old churches", "Where should I eat?",
        "Tell me a fun fact about Taal", "How do I get to Taal?",
        "Is it safe? What about the volcano?", "What souvenirs should I buy?",
    )

    LaunchedEffect(msgs.size, thinking) {
        listState.animateScrollToItem((listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
    }

    fun send(text: String) {
        val t = text.trim()
        if (t.isEmpty() || thinking) return
        msgs.add(ChatMsg(true, t))
        input = ""
        thinking = true
        scope.launch {
            val result = if (Gemini.configured) Gemini.ask(msgs.toList())
            else Result.failure(Exception("no key"))
            val ai = result.getOrNull()
            val reply = if (ai != null) {
                ChatMsg(false, ai.trim(), Gemini.mentioned(ai))
            } else {
                val off = offlineReply(t, msgs.size)
                if (Gemini.configured)
                    ChatMsg(false,
                        "I couldn't reach the AI (${result.exceptionOrNull()?.message}), so here is an offline answer.\n\n${off.text}",
                        off.picks)
                else off
            }
            msgs.add(reply)
            thinking = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                CardBox {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Ink),
                            contentAlignment = Alignment.Center) { Text("🧭") }
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Taal Guide", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                            Text(if (Gemini.configured) "AI guide · ask me anything" else "Smart picks · offline",
                                fontSize = 11.sp, color = Muted)
                        }
                        Text(if (Gemini.configured) "● AI online" else "● Offline mode",
                            fontSize = 10.sp, color = Teal)
                    }
                }
            }
            items(msgs.size) { i ->
                val m = msgs[i]
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = if (m.me) Alignment.End else Alignment.Start,
                ) {
                    Box(
                        Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(16.dp))
                            .background(if (m.me) Ink else Color.White)
                            .then(if (m.me) Modifier else Modifier.border(1.dp, Line, RoundedCornerShape(16.dp)))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(m.text, fontSize = 13.sp, lineHeight = 18.sp, color = if (m.me) Color.White else Ink)
                    }
                    m.picks.forEach { p ->
                        Spacer(Modifier.height(8.dp))
                        CardBox(Modifier.widthIn(max = 300.dp)) {
                            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Thumb(p, 40.dp)
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(p.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Ink,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${p.window} · ${p.price}", fontSize = 10.sp, color = Muted,
                                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                val added = inTrip(p.id)
                                RoundBtn({ toggleTrip(p) }, bg = Ink, size = 30.dp) {
                                    Icon(if (added) Icons.Default.Check else Icons.Default.Add, null,
                                        tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
            if (thinking) {
                item {
                    Row(
                        Modifier.clip(RoundedCornerShape(16.dp)).background(Color.White)
                            .border(1.dp, Line, RoundedCornerShape(16.dp)).padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp, color = Teal)
                        Spacer(Modifier.width(10.dp))
                        Text("Taal Guide is thinking…", fontSize = 12.sp, color = Muted)
                    }
                }
            }
            if (msgs.size == 1) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        starters.forEach { s ->
                            Box(
                                Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White)
                                    .border(1.dp, Line, RoundedCornerShape(20.dp))
                                    .clickable { send(s) }.padding(horizontal = 14.dp, vertical = 8.dp)
                            ) { Text(s, fontSize = 12.sp, color = Ink) }
                        }
                    }
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().background(Surface0).padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = input, onValueChange = { input = it },
                placeholder = { Text("Ask anything…") },
                shape = RoundedCornerShape(14.dp), singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            FilledIconButton(
                onClick = { send(input) },
                enabled = !thinking,
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Ink),
            ) { Icon(Icons.AutoMirrored.Filled.Send, null, tint = Color.White) }
        }
    }
}

// ---------- MAP (LIVE LOCATION) ----------
@Composable
fun MapTab() {
    val ctx = LocalContext.current
    var location by remember { mutableStateOf<Location?>(null) }
    var mode by remember { mutableStateOf("driving") }
    var centerTick by remember { mutableIntStateOf(0) }
    var hasPerm by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        hasPerm = r[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                r[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }
    LaunchedEffect(Unit) {
        if (!hasPerm) launcher.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )
    }

    // Live location updates
    DisposableEffect(hasPerm) {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val listener = LocationListener { loc -> location = loc }
        if (hasPerm) {
            @SuppressLint("MissingPermission")
            fun start() {
                val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                    .filter { lm.isProviderEnabled(it) }
                providers.forEach { lm.requestLocationUpdates(it, 2000L, 5f, listener) }
                providers.mapNotNull { lm.getLastKnownLocation(it) }
                    .maxByOrNull { it.time }?.let { location = it }
            }
            start()
        }
        onDispose { lm.removeUpdates(listener) }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Taal on the map", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Text("See where you are in real time, then jump to turn-by-turn directions in Google Maps.",
            fontSize = 12.sp, color = Muted, lineHeight = 17.sp)
        Spacer(Modifier.height(12.dp))

        CardBox {
            Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (location != null) "Your live location" else "Your location",
                        fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink)
                    val l = location
                    Text(
                        if (l != null) "%.5f, %.5f · ±%d m".format(l.latitude, l.longitude, l.accuracy.toInt())
                        else if (!hasPerm) "Location permission is needed"
                        else "Locating…",
                        fontSize = 11.sp, color = Muted,
                    )
                }
                if (!hasPerm) TextButton(onClick = {
                    launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                }) { Text("Allow") }
            }
        }
        Spacer(Modifier.height(12.dp))

        Box(Modifier.fillMaxWidth().height(280.dp).clip(RoundedCornerShape(16.dp))) {
            LiveMap(location, centerTick)
            FloatingActionButton(
                onClick = { centerTick++ },
                containerColor = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(10.dp).size(44.dp),
            ) { Icon(Icons.Default.Place, null, tint = Ink) }
        }
        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { openTown(ctx) },
            colors = ButtonDefaults.buttonColors(containerColor = Ink),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(46.dp),
        ) { Text("Open Taal in Google Maps") }

        Spacer(Modifier.height(18.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("HERITAGE STOPS NEARBY", fontSize = 11.sp, letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Bold, color = Muted, modifier = Modifier.weight(1f))
            listOf("driving" to "Drive", "walking" to "Walk").forEach { (m, label) ->
                val sel = mode == m
                Box(
                    Modifier.padding(start = 6.dp).clip(RoundedCornerShape(16.dp))
                        .background(if (sel) Ink else Color.White)
                        .border(1.dp, if (sel) Ink else Line, RoundedCornerShape(16.dp))
                        .clickable { mode = m }.padding(horizontal = 12.dp, vertical = 6.dp)
                ) { Text(label, fontSize = 11.sp, color = if (sel) Color.White else Ink) }
            }
        }
        Spacer(Modifier.height(8.dp))
        PLACES.filter { it.kind == Kind.PLACES }.forEach { p ->
            CardBox(Modifier.padding(bottom = 10.dp)) {
                Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Thumb(p, 44.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Ink,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(p.address, fontSize = 11.sp, color = Muted, maxLines = 1,
                            overflow = TextOverflow.Ellipsis)
                    }
                    RoundBtn({ openDirections(ctx, p, mode) }, bg = Ink) {
                        Text("Go", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun LiveMap(location: Location?, centerTick: Int) {
    val ctx = LocalContext.current
    val mapView = remember {
        Configuration.getInstance().apply {
            userAgentValue = "TaalTrails/1.0 (student project; contact: linatocrodelito8@gmail.com)"
            val base = File(ctx.cacheDir, "osmdroid_v2")
            osmdroidBasePath = base
            osmdroidTileCache = File(base, "tiles")
        }
        MapView(ctx).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(15.0)
            controller.setCenter(GeoPoint(13.8820, 120.9230)) // Taal, until a fix arrives
        }
    }
    var lastTick by remember { mutableIntStateOf(0) }
    var firstFix by remember { mutableStateOf(true) }

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose { mapView.onPause(); mapView.onDetach() }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { mapView },
        update = { map ->
            map.overlays.clear()
            location?.let { loc ->
                val pt = GeoPoint(loc.latitude, loc.longitude)
                val marker = Marker(map).apply {
                    position = pt
                    title = "You are here"
                    setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                }
                map.overlays.add(marker)
                if (firstFix || centerTick != lastTick) {
                    map.controller.animateTo(pt)
                    firstFix = false
                    lastTick = centerTick
                }
            }
            map.invalidate()
        },
    )
}